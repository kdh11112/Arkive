package egovframework.example.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hsqldb.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class EgovConfigDatasource {
	
	private Server hsqldbServer;
	
    private final Environment env;
    
    private String dbType;
    private String className;
    private String url;
    private String userName;
    private String password;

    public EgovConfigDatasource(Environment env) {
        this.env = env;
    }
    
    @PostConstruct
    public void init() {
        dbType = env.getProperty("Globals.DbType");
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException("Globals.DbType 속성이 설정되지 않았거나 비어 있습니다.");
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

        // dbType이 "hsql_server"일 때만 HSQLDB 서버를 시작합니다.
        // 현재 Globals.DbType이 "hsql_server"이므로 이 블록이 실행됩니다.
        if ("hsql_server".equalsIgnoreCase(dbType)) {
            hsqldbServer = new Server();
            hsqldbServer.setLogWriter(null);
            hsqldbServer.setSilent(true);
            hsqldbServer.setDatabaseName(0, "ArkiveDB");
            hsqldbServer.setDatabasePath(0, "mem:ArkiveDB"); // 인메모리 모드
            hsqldbServer.setPort(9001); // 서버 포트 설정
            hsqldbServer.start(); // HSQLDB 시작

            try {
                // 2초 정도 대기하여 서버가 완전히 준비될 시간을 줍니다.
                System.out.println("HSQLDB server starting, waiting for 2 seconds...");
                Thread.sleep(2000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                System.err.println("HSQLDB server startup delay interrupted.");
            }

            System.out.println("HSQLDB server (in-memory mode) started on port 9001 for Globals.DbType: " + dbType);
        }
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

        // --- DBCP2 커넥션 풀 추가 설정 (선택 사항, 성능 튜닝에 중요) ---
        basicDataSource.setInitialSize(5);        // 초기 커넥션 수
        basicDataSource.setMaxTotal(10);          // 최대 커넥션 수
        basicDataSource.setMaxIdle(5);            // 최대 유휴 커넥션 수
        basicDataSource.setMinIdle(2);            // 최소 유휴 커넥션 수
        basicDataSource.setMaxWaitMillis(10000);  // 커넥션 얻을 때까지 대기 시간 (밀리초)
        basicDataSource.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS"); // HSQLDB 유효성 쿼리
        basicDataSource.setTestOnBorrow(true);    // 커넥션 대여 시 유효성 검사
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setTestWhileIdle(true);   // 유휴 커넥션 유효성 검사
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
             // 만약 "hsql_server"가 아닌 다른 외부 DB 타입 (mysql, oracle 등)이 설정되었다면 여기가 반환
             System.out.println("Initializing BasicDataSource for external DB (" + url + ")");
             return dataSourceBasic();
        }
    }

}
