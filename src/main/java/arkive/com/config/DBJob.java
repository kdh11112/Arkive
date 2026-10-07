package arkive.com.config;

import java.io.File;
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
        // 덤프 경로. 고정 절대경로(/db/data.sql)는 Windows에서 없어 10분마다 실패했다.
        // 기본값은 작업 디렉터리 아래 backup이며 application.properties에서 바꾼다.
        String dumpPath = env.getProperty("Globals.dumpPath", "./backup/ArkiveDB.sql");
        File dumpFile = new File(dumpPath).getAbsoluteFile();
        File parentDir = dumpFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            System.err.println("DB 덤프 건너뜀. 폴더를 만들 수 없음: " + parentDir.getAbsolutePath());
            return;
        }
        String dumpFilePath = dumpFile.getAbsolutePath().replace("\\", "/");
        // HSQLDB SCRIPT는 기존 파일을 덮어쓰지 않고 실패한다. 매번 새 덤프가 남게 먼저 지운다.
        if (dumpFile.exists() && !dumpFile.delete()) {
            System.err.println("DB 덤프 건너뜀. 기존 파일을 지울 수 없음: " + dumpFilePath);
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "SCRIPT '" + dumpFilePath.replace("'", "''") + "'";
            stmt.execute(sql);

            System.out.println("데이터베이스 덤프 완료: " + dumpFilePath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new JobExecutionException("데이터베이스 덤프 실패", e);
        }
    }
}
