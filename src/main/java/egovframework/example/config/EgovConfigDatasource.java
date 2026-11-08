package egovframework.example.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hsqldb.Server;
import org.hsqldb.server.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
// @PropertySource를 쓰지 않는다. Boot가 application.properties를
// main+test 합쳐서 Environment에 넣는데, @PropertySource는 중복 파일 중
// 첫 번째 것만 읽어서 테스트 오버라이드가 묻힌다.
public class EgovConfigDatasource {

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

        // 로컬 데모의 hsql_server 모드에서만 DB 서버를 직접 시작한다.
        // 기존 DB를 사용하는 이식 대상에는 서버 시작·데모 초기화를 가져가지 않는다.
        if ("hsql_server".equalsIgnoreCase(dbType)) {
            // 파일 DB는 서버를 재시작해도 디스크의 스키마와 데이터가 그대로 남습니다.
            String databasePath = env.getProperty("Globals.hsql_server.DatabasePath", "./data/ArkiveDB");
            java.io.File databaseFile = new java.io.File(databasePath).getAbsoluteFile();
            java.io.File databaseDirectory = databaseFile.getParentFile();
            if (databaseDirectory != null && !databaseDirectory.exists() && !databaseDirectory.mkdirs()) {
                throw new IllegalStateException("HSQLDB directory could not be created: " + databaseDirectory.getAbsolutePath());
            }
            hsqlDatabasePath = databaseFile.getAbsolutePath().replace('\\', '/');

            int port = env.getProperty("Globals.hsql_server.Port", Integer.class, 9001);
            hsqldbServer = new Server();
            hsqldbServer.setLogWriter(null);
            hsqldbServer.setSilent(true);
            hsqldbServer.setDatabaseName(0, "ArkiveDB");
            hsqldbServer.setDatabasePath(0, hsqlDatabasePath);
            hsqldbServer.setPort(port); // 서버 포트 설정 (Url의 포트와 일치해야 함)
            hsqldbServer.start(); // start()는 서버의 시작 결과가 정해질 때까지 기다린다.
            if (hsqldbServer.getState() != ServerConstants.SERVER_STATE_ONLINE) {
                Throwable cause = hsqldbServer.getServerError();
                hsqldbServer.stop();
                throw new IllegalStateException("HSQLDB server could not start on port " + port, cause);
            }

            LOGGER.info("HSQLDB file server started at {} on port {}", hsqlDatabasePath, port);
        }
    }

    @PreDestroy
    public void stopHsqldbServer() {
        if (hsqldbServer != null) {
            hsqldbServer.stop();
            LOGGER.info("HSQLDB server stopped");
        }
    }

	/**
	 * @return [dataSource 설정] basicDataSource 설정
	 */
    private BasicDataSource dataSourceBasic() {
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
    @Bean(name = {"dataSource", "egov.dataSource", "egovDataSource"}, destroyMethod = "close")
    public DataSource dataSource() throws Exception {
        BasicDataSource dataSource = dataSourceBasic();
        try {
            if ("hsql_server".equalsIgnoreCase(dbType)) {
                // Mapper·웹 서버·스케줄러가 DB를 쓰기 전에 최초 초기화/기존 DB 확인을 마친다.
                // 일반 재시작에는 schema.sql·seed.sql을 실행하지 않는다.
                try (java.sql.Connection connection = dataSource.getConnection()) {
                    new HsqlDemoDatabaseInitializer().initialize(connection, hsqlDatabasePath);
                }
            }
            return dataSource;
        } catch (Exception e) {
            try {
                dataSource.close();
            } catch (java.sql.SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

}
