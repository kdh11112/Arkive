package egovframework.example.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
@PropertySource("classpath:/application.properties")
public class EgovConfigDatasource implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EgovConfigDatasource.class);
	
	private Server hsqldbServer;
	
    private final Environment env;
    
    private String dbType;
    private String className;
    private String url;
    private String userName;
    private String password;
    private String hsqlDatabasePath;

    public EgovConfigDatasource(Environment env) {
        this.env = env;
    }
    
    @PostConstruct
    public void init() {
        dbType = env.getProperty("Globals.DbType");
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException("Globals.DbType 이 설정되지 않았거나 비어 있습니다.");
        }

        if (!"hsql".equalsIgnoreCase(dbType)) {
            className = env.getProperty("Globals." + dbType + ".DriverClassName");
            url = env.getProperty("Globals." + dbType + ".Url");
            userName = env.getProperty("Globals." + dbType + ".UserName");
            password = env.getProperty("Globals." + dbType + ".Password");

            if (className == null || url == null || userName == null) {
                throw new IllegalArgumentException("Globals." + dbType + " 관련 DB 연결 정보가 불완전합니다.");
            }
        }

        // dbType이 "hsql_server"인 경우에만 HSQLDB 서버를 직접 시작합니다.
        // 현재 Globals.DbType이 "hsql_server"이므로 이 블록이 실행됩니다.
        if ("hsql_server".equalsIgnoreCase(dbType)) {
            // 파일 DB는 서버를 재시작해도 디스크의 스키마와 데이터가 그대로 남습니다.
            String databasePath = env.getProperty("Globals.hsql_server.DatabasePath", "./data/ArkiveDB");
            java.io.File databaseFile = new java.io.File(databasePath).getAbsoluteFile();
            java.io.File databaseDirectory = databaseFile.getParentFile();
            if (databaseDirectory != null && !databaseDirectory.exists() && !databaseDirectory.mkdirs()) {
                throw new IllegalStateException("HSQLDB directory could not be created: " + databaseDirectory.getAbsolutePath());
            }
            hsqlDatabasePath = databaseFile.getAbsolutePath().replace('\\', '/');

            hsqldbServer = new Server();
            hsqldbServer.setLogWriter(null);
            hsqldbServer.setSilent(true);
            hsqldbServer.setDatabaseName(0, "ArkiveDB");
            hsqldbServer.setDatabasePath(0, hsqlDatabasePath);
            hsqldbServer.setPort(9001); // 서버 포트 설정
            hsqldbServer.start(); // HSQLDB 시작

            try {
                // 2초 정도 정지하여 서버가 완전하게 준비될 수 있는 시간을 줍니다.
                System.out.println("HSQLDB server starting, waiting for 2 seconds...");
                Thread.sleep(2000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                System.err.println("HSQLDB server startup delay interrupted.");
            }

            System.out.println("HSQLDB file server started at " + hsqlDatabasePath + " on port 9001");
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!"hsql_server".equalsIgnoreCase(dbType)) {
            return;
        }

        DataSource dataSource = dataSourceBasic();
        try (java.sql.Connection connection = dataSource.getConnection()) {
            // 최초 DB와 기존 DB 모두 같은 방식으로 이력을 만들고, 미적용 버전 SQL을 순서대로 반영합니다.
            createMigrationHistory(connection);
            applyPendingMigrations(connection);
        } finally {
            if (dataSource instanceof org.apache.commons.dbcp2.BasicDataSource basicDataSource) {
                basicDataSource.close();
            }
        }
    }

    private void executeSqlCommand(java.sql.Connection connection, String command) throws java.sql.SQLException {
        StringBuilder sql = new StringBuilder();
        for (String line : command.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        String trimmed = sql.toString().trim();
        if (!trimmed.isEmpty()) {
            try (java.sql.Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }

    private void createMigrationHistory(java.sql.Connection connection) throws java.sql.SQLException {
        // 적용된 버전과 파일명을 기록해 앱 재시작 때 같은 SQL을 다시 실행하지 않습니다.
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS SCHEMA_MIGRATION (VERSION VARCHAR(200) PRIMARY KEY, SCRIPT_NAME VARCHAR(255) NOT NULL, APPLIED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)");
        }
    }

    private void applyPendingMigrations(java.sql.Connection connection) throws Exception {
        // V숫자__설명.sql 파일을 숫자 순으로 찾아 개발 실행과 패키징된 실행 모두에서 읽습니다.
        java.util.List<MigrationScript> scripts = new java.util.ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        for (Resource resource : resolver.getResources("classpath*:db/V*__*.sql")) {
            String filename = resource.getFilename();
            if (filename != null && filename.matches("V[0-9]+__.+\\.sql")) {
                scripts.add(new MigrationScript(filename, () -> {
                    try (java.io.InputStream input = resource.getInputStream()) {
                        return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }));
            }
        }

        scripts.sort(java.util.Comparator.comparingLong(MigrationScript::versionNumber));
        java.util.Set<String> versions = new java.util.HashSet<>();
        for (MigrationScript script : scripts) {
            if (!versions.add(script.version())) {
                throw new IllegalStateException("Duplicate database migration version: " + script.version());
            }
            applyMigration(connection, script);
        }
    }

    private void applyMigration(java.sql.Connection connection, MigrationScript script) throws Exception {
        String version = script.version();
        try (java.sql.PreparedStatement query = connection.prepareStatement("SELECT 1 FROM SCHEMA_MIGRATION WHERE VERSION = ?")) {
            query.setString(1, version);
            try (java.sql.ResultSet result = query.executeQuery()) {
                if (result.next()) {
                    // 이미 적용 이력이 있으면 건너뜁니다.
                    return;
                }
            }
        }

        LOGGER.info("Applying database migration {}", script.name());
        // SQL 실행과 이력 기록을 한 트랜잭션으로 묶어 실패한 버전은 적용 완료로 남기지 않습니다.
        connection.setAutoCommit(false);
        try {
            for (String command : script.sql().split(";")) {
                executeSqlCommand(connection, command);
            }
            try (java.sql.PreparedStatement record = connection.prepareStatement("INSERT INTO SCHEMA_MIGRATION (VERSION, SCRIPT_NAME) VALUES (?, ?)")) {
                record.setString(1, version);
                record.setString(2, script.name());
                record.executeUpdate();
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private record MigrationScript(String name, SqlSupplier supplier) {
        String sql() throws Exception { return supplier.get(); }
        String version() { return name.substring(0, name.indexOf("__")); }
        long versionNumber() { return Long.parseLong(version().substring(1)); }
    }

    @FunctionalInterface
    private interface SqlSupplier {
        String get() throws Exception;
    }
    @PreDestroy
    public void stopHsqldbServer() {
        if (hsqldbServer != null) {
            hsqldbServer.stop();
            System.out.println("HSQLDB server stopped");
        }
    }

	/**
	 * @return [dataSource 설정] basicDataSource 설정
	 */
    private DataSource dataSourceBasic() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(className);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(userName);
        basicDataSource.setPassword(password);

        // --- DBCP2 커넥션 풀 추가 설정 (필수 사항, 확장 이슈 시 중요) ---
        basicDataSource.setInitialSize(5);        // 초기 커넥션 수
        basicDataSource.setMaxTotal(10);          // 최대 커넥션 수
        basicDataSource.setMaxIdle(5);            // 최대 유휴 커넥션 수
        basicDataSource.setMinIdle(2);            // 최소 유휴 커넥션 수
        basicDataSource.setMaxWaitMillis(10000);  // 커넥션 대기 시간 (밀리초)
        basicDataSource.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS"); // HSQLDB 유효성 쿼리
        basicDataSource.setTestOnBorrow(true);    // 커넥션 빌려올 시 유효성 검증
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setTestWhileIdle(true);   // 유휴 커넥션 유효성 검증
        basicDataSource.setTimeBetweenEvictionRunsMillis(60000); // 1분마다 유휴 커넥션 정리

        return basicDataSource;
    }

	/**
	 * @return [DataSource 설정]
	 */
    @Bean(name = {"dataSource", "egov.dataSource", "egovDataSource"})
    public DataSource dataSource() {
        // 이제 "hsql_server" 모드를 사용하기 위해, 내장형 HSQLDB 분기 제거
        if ("hsql_server".equalsIgnoreCase(dbType)) {
             System.out.println("Initializing BasicDataSource for HSQLDB Server (" + url + ")");
             return dataSourceBasic();
        } else {
             // 만약 "hsql_server"가 아니고 다른 외부 DB 연결 (mysql, oracle 등)이 설정되어 있다면 여기에 반환
             System.out.println("Initializing BasicDataSource for external DB (" + url + ")");
             return dataSourceBasic();
        }
    }

}
