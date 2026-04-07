package arkive.com.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;



public class DBJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Quartz Job 실행: " + System.currentTimeMillis());

        // JobDataMap 또는 SchedulerContext에서 ApplicationContext를 꺼내기
        ApplicationContext appContext;
        try {
            appContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
        } catch (Exception e) {
            throw new JobExecutionException("ApplicationContext를 얻지 못함", e);
        }

        Environment env = appContext.getEnvironment();

        String dbType = env.getProperty("Globals.DbType");
        if (dbType == null || dbType.isEmpty()) {
            throw new JobExecutionException("Globals.DbType 속성이 설정되지 않음");
        }

        String url = env.getProperty("Globals." + dbType + ".Url");
        String user = env.getProperty("Globals." + dbType + ".UserName");
        String password = env.getProperty("Globals." + dbType + ".Password");
        // 쓰기 가능한 경로
        String dumpFilePath = "/db/data.sql";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "SCRIPT '" + dumpFilePath.replace("\\", "/") + "'";
            stmt.execute(sql);

            System.out.println("데이터베이스 덤프 완료: " + dumpFilePath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new JobExecutionException("데이터베이스 덤프 실패", e);
        }
    }
}
