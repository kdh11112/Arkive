package arkive.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Arkive 테스트 전용 격리 PostgreSQL 초기화기.
 *
 * <p>Spring 컨텍스트가 DataSource 빈을 만들기 전에 프로세스 안에서 로컬 PostgreSQL 17.12를
 * 한 번만 띄우고, {@code db/postgres/schema.sql} → {@code db/postgres/seed.sql}을 새 DB에 적용한다.
 * 테스트는 운영 Supabase나 사용자가 쓰는 기존 PostgreSQL에 접속하지 않는다.
 *
 * <p>인스턴스는 JVM(정확히는 테스트 실행 JVM) 안에서 공유한다. 모든 {@link ArkiveIntegrationTest}
 * 테스트가 같은 Spring 컨텍스트와 같은 DB를 쓰도록 static 인스턴스를 thread-safe하게 한 번만 만든다.
 *
 * <p>경로 격리 근거(embedded-postgres 2.1.0 {@code EmbeddedPostgres} 소스 확인):
 * <ul>
 *   <li>{@code builder()}는 부모 디렉터리를 {@code ot.epg.working-dir}(없으면
 *       {@code java.io.tmpdir/embedded-pg})에서 읽고, 생성자에서 그 부모 디렉터리의 '오래된
 *       데이터 디렉터리 정리'를 수행한다. 따라서 이 값은 우리 전용 하위 디렉터리로만 지정한다.</li>
 *   <li>네이티브 바이너리 추출 위치는 {@code setOverrideWorkingDirectory}가 지정한 디렉터리
 *       아래 {@code PG-<md5>}다.</li>
 *   <li>데이터 디렉터리는 {@code setDataDirectory}로 명시하지 않으면 OS 임시 디렉터리에
 *       만들어지므로 반드시 명시한다.</li>
 * </ul>
 * 세 경로는 모두 {@code target/test-postgres/<고유 UUID>} 아래에만 만든다.
 * 라이브러리가 이 JVM 밖의 임시 PG를 정리하거나 기존 데이터 경로를 재사용하지 않는다.
 */
public class PostgresTestDatabaseInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresTestDatabaseInitializer.class);

    /** 테스트가 붙는 Datasource 키. 운영 값과 섞이지 않게 항상 이 로컬 값으로 덮어쓴다. */
    private static final String TEST_DB_TYPE = "postgresql";
    private static final String TEST_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    private static final String TEST_DB_USER = "postgres";
    /** embedded-postgres의 initdb가 {@code -A trust}로 초기화하므로 실제 인증에 쓰이지 않는 자리표시자다. */
    private static final String TEST_DB_PASSWORD = "postgres";
    private static final String TEST_DATABASE = "postgres";
    /** 실제 Supabase와 같은 PG 주 버전. BOM(embedded-postgres-binaries-bom)과 함께 올린다. */
    private static final int EXPECTED_POSTGRES_MAJOR = 17;
    /** 테스트 세션 시간대. 서버 전역 설정은 UTC로 두고 접속마다 이 값으로 맞춘다. */
    private static final String TEST_TIME_ZONE = "Asia/Seoul";
    private static final String SERVER_TIME_ZONE = "UTC";
    private static final String SET_TIME_ZONE_SQL = "SET TIME ZONE '" + TEST_TIME_ZONE + "'";

    private static final String WORKING_DIR_PROPERTY = "ot.epg.working-dir";
    private static final String PROPERTY_SOURCE_NAME = "arkivePostgresTestDatabase";

    private static final Object START_LOCK = new Object();
    private static EmbeddedPostgres postgres;
    private static String jdbcUrl;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        // PG를 띄우기 전에도 운영 URL이 보이지 않도록 로컬 고정값을 먼저 1순위로 등록한다.
        // 실제 접속 URL은 포트가 정해진 뒤 같은 이름의 property source를 교체해 넣는다.
        registerOverrides(environment, overrides(null));

        String url = startSharedDatabase();
        registerOverrides(environment, overrides(url));
        LOG.info("격리 PostgreSQL 테스트 DB 사용 준비 완료: dbType={}, timeZone={}", TEST_DB_TYPE, TEST_TIME_ZONE);
    }

    /** 사용자 환경변수·시스템 properties·application.properties의 운영 값보다 우선하도록 맨 앞에 둔다. */
    private void registerOverrides(ConfigurableEnvironment environment, Map<String, Object> overrides) {
        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, overrides);
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().replace(PROPERTY_SOURCE_NAME, source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
    }

    /**
     * 수동 DataSource 구성({@code EgovConfigDatasource})과 후속 Boot 자동 구성이 같은 로컬 DB를
     * 가리키도록 두 방식의 키를 모두 덮어쓴다. {@code spring.sql.init.mode=never}로 Boot의
     * 자동 스크립트 실행이 스키마·시드를 두 번 적용하지 않게 한다.
     */
    private Map<String, Object> overrides(String url) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("Globals.DbType", TEST_DB_TYPE);
        overrides.put("Globals.postgresql.DriverClassName", TEST_DRIVER_CLASS_NAME);
        overrides.put("Globals.postgresql.UserName", TEST_DB_USER);
        overrides.put("Globals.postgresql.Password", TEST_DB_PASSWORD);
        overrides.put("Globals.postgresql.TimeZone", TEST_TIME_ZONE);

        overrides.put("spring.datasource.driver-class-name", TEST_DRIVER_CLASS_NAME);
        overrides.put("spring.datasource.username", TEST_DB_USER);
        overrides.put("spring.datasource.password", TEST_DB_PASSWORD);
        overrides.put("spring.datasource.hikari.connection-init-sql", SET_TIME_ZONE_SQL);
        overrides.put("spring.sql.init.mode", "never");

        if (url != null) {
            overrides.put("Globals.postgresql.Url", url);
            overrides.put("spring.datasource.url", url);
        }
        return overrides;
    }

    private String startSharedDatabase() {
        synchronized (START_LOCK) {
            if (jdbcUrl != null) {
                return jdbcUrl;
            }

            Path root = Paths.get("target", "test-postgres", UUID.randomUUID().toString())
                    .toAbsolutePath().normalize();
            Path binariesDirectory = root.resolve("binaries");
            Path workingDirectory = root.resolve("work");
            Path dataDirectory = root.resolve("data");

            EmbeddedPostgres instance = null;
            try {
                Files.createDirectories(workingDirectory);
                Files.createDirectories(dataDirectory);

                EmbeddedPostgres.Builder builder = isolatedBuilder(workingDirectory);
                instance = builder
                        // 0이면 빈 포트를 자동 선택한다. 고정 포트로 다른 프로세스와 충돌하지 않는다.
                        .setPort(0)
                        .setOverrideWorkingDirectory(binariesDirectory.toFile())
                        .setDataDirectory(dataDirectory.toFile())
                        .setCleanDataDirectory(true)
                        // 루프백에서만 접속을 받는다. 외부 네트워크에 노출하지 않는다.
                        .setServerConfig("listen_addresses", "127.0.0.1")
                        // 서버 기본 시간대는 UTC로 유지한다. 앱 세션은 접속 후 SET TIME ZONE으로 맞춘다.
                        .setServerConfig("timezone", SERVER_TIME_ZONE)
                        // 최초 실행은 바이너리 추출+initdb까지 포함하므로 기본 10초보다 넉넉히 준다.
                        .setPGStartupWait(Duration.ofSeconds(120))
                        .start();

                // 실제 접속은 localhost 대신 127.0.0.1을 써서 IPv6 loopback 해석에 기대지 않는다.
                String url = "jdbc:postgresql://127.0.0.1:" + instance.getPort() + "/" + TEST_DATABASE;

                String version = readAndValidateServer(instance.getPort(), dataDirectory);
                applySchemaAndSeed(url);

                registerShutdownHook(instance, dataDirectory);
                postgres = instance;
                jdbcUrl = url;

                LOG.info("격리 PostgreSQL 테스트 DB 기동: url={}, port={}, serverVersion={}",
                        url, instance.getPort(), version);
                LOG.info("격리 PostgreSQL 테스트 산출물 경로(테스트 종료 시 정리): root={}, dataDirectory={}, binariesDirectory={}",
                        root, dataDirectory, binariesDirectory);
                return jdbcUrl;
            } catch (Exception e) {
                // 생성된 인스턴스가 있으면 postmaster를 즉시 내린다. 실패 시에도 잔여 프로세스를 남기지 않는다.
                closeQuietly(instance);
                throw new IllegalStateException(
                        "격리 PostgreSQL 테스트 DB 준비에 실패했습니다. root=" + root
                                + " (Docker·로컬 PostgreSQL 설치 없이 embedded-postgres 바이너리를 사용합니다.)",
                        e);
            }
        }
    }

    /**
     * {@code builder()}가 부모 디렉터리를 읽는 시점에만 {@code ot.epg.working-dir}을 우리 전용
     * 경로로 바꾸고 곧바로 원래 값으로 되돌린다. 이렇게 해야 라이브러리의 오래된 데이터 디렉터리
     * 정리가 {@code target/test-postgres/<UUID>/work} 밖(예: 다른 사람의 OS 임시 PG)을 건드리지 않는다.
     */
    private EmbeddedPostgres.Builder isolatedBuilder(Path workingDirectory) {
        String previous = System.getProperty(WORKING_DIR_PROPERTY);
        System.setProperty(WORKING_DIR_PROPERTY, workingDirectory.toString());
        try {
            return EmbeddedPostgres.builder();
        } finally {
            if (previous == null) {
                System.clearProperty(WORKING_DIR_PROPERTY);
            } else {
                System.setProperty(WORKING_DIR_PROPERTY, previous);
            }
        }
    }

    /**
     * PG 주 버전과 서버 기본 시간대를 확인한다.
     *
     * <p>세션의 {@code current_setting('timezone')}는 진단 근거로 쓰지 않는다. pgjdbc는 접속 시 JVM
     * 기본 시간대를 startup 파라미터(TimeZone)로 보내므로 세션 값이 항상 클라이언트 값이 되고
     * ({@code pg_settings.source=client}로 확인), 서버 기본값이 가려진다. 서버 기본값은 postmaster가
     * 실제로 받은 옵션을 기록한 {@code postmaster.opts}로 확인한다.
     */
    private String readAndValidateServer(int port, Path dataDirectory) throws SQLException, IOException {
        String url = "jdbc:postgresql://127.0.0.1:" + port + "/" + TEST_DATABASE;
        try (Connection connection = DriverManager.getConnection(url, TEST_DB_USER, TEST_DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT current_setting('server_version_num'), version()")) {
            if (!resultSet.next()) {
                throw new IllegalStateException("PostgreSQL 버전을 조회하지 못했습니다.");
            }
            int versionNumber = resultSet.getInt(1);
            String version = resultSet.getString(2);
            int major = versionNumber / 10000;
            if (major != EXPECTED_POSTGRES_MAJOR) {
                throw new IllegalStateException("PostgreSQL 주 버전이 " + EXPECTED_POSTGRES_MAJOR + "이 아닙니다. version="
                        + version + ", server_version_num=" + versionNumber);
            }

            Path postmasterOptions = dataDirectory.resolve("postmaster.opts");
            String options = Files.exists(postmasterOptions)
                    ? Files.readString(postmasterOptions, StandardCharsets.UTF_8)
                    : "";
            boolean serverDefaultUtc = options.contains("timezone=" + SERVER_TIME_ZONE);
            LOG.info("격리 PostgreSQL 서버 기본 시간대: {} (postmaster.opts 근거={}). 앱 세션은 connection-init-sql로 '{}'를 쓴다.",
                    SERVER_TIME_ZONE, serverDefaultUtc, TEST_TIME_ZONE);
            if (!serverDefaultUtc) {
                LOG.warn("격리 PostgreSQL 서버 기본 시간대가 {}가 아닙니다. 서버 기동 옵션을 확인하세요: {}",
                        SERVER_TIME_ZONE, postmasterOptions);
            }
            return version;
        }
    }

    /**
     * 새 로컬 DB에 스키마·시드를 최초 한 번만 적용한다. 스크립트 오류는 ScriptUtils가 예외로
     * 전파하며 여기서 삼키지 않는다(부분 적용 상태로 테스트를 시작하지 않는다).
     */
    private void applySchemaAndSeed(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, TEST_DB_USER, TEST_DB_PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                // 서버 전역은 UTC, 이 세션만 KST로 맞춘다. 앱 커넥션도 connection-init-sql로 같은 값을 쓴다.
                statement.execute(SET_TIME_ZONE_SQL);
            }
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource("db/postgres/schema.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource("db/postgres/seed.sql"), StandardCharsets.UTF_8));
        }
        LOG.info("격리 PostgreSQL 테스트 DB에 db/postgres/schema.sql, db/postgres/seed.sql을 적용했습니다.");
    }

    private void registerShutdownHook(EmbeddedPostgres instance, Path dataDirectory) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("격리 PostgreSQL 테스트 DB 종료: dataDirectory={}, port={}", dataDirectory, instance.getPort());
            closeQuietly(instance);
        }, "arkive-test-postgres-shutdown"));
    }

    private static void closeQuietly(EmbeddedPostgres instance) {
        if (instance == null) {
            return;
        }
        try {
            instance.close();
        } catch (IOException | RuntimeException e) {
            LOG.warn("격리 PostgreSQL 테스트 DB 종료 중 오류", e);
        }
    }
}
