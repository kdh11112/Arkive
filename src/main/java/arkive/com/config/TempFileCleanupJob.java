package arkive.com.config;

import java.io.File;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * temp 폴더의 만료 파일을 정리한다.
 * TUS 청크 이어받기를 위해 페이지를 나가도 temp를 남기므로,
 * 일정 시간이 지난 temp 파일과 TUS 길이 파일(.len)을 주기적으로 삭제한다.
 * 멀티파트·드롭존 staging 파일도 같은 폴더를 쓰므로 함께 정리된다.
 */
public class TempFileCleanupJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ApplicationContext appContext;
        try {
            appContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
        } catch (Exception e) {
            throw new JobExecutionException("ApplicationContext를 얻지 못함", e);
        }

        Environment env = appContext.getEnvironment();

        String tempPath = env.getProperty("Globals.FILE_TEMP_PATH");
        if (tempPath == null || tempPath.trim().isEmpty() || "99".equals(tempPath.trim())) {
            tempPath = System.getProperty("java.io.tmpdir") + File.separator + "arkive"
                    + File.separator + "temp";
        }

        int expireHours = 24;
        try {
            expireHours = Integer.parseInt(env.getProperty("Globals.TempFileExpireHours", "24").trim());
        } catch (NumberFormatException e) {
            expireHours = 24;
        }

        File dir = new File(tempPath);
        if (!dir.isDirectory()) {
            return;
        }

        long cutoff = System.currentTimeMillis() - (long) expireHours * 3600_000L;
        int deleted = 0;
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            // TUS fileId(20자리 숫자) 파일과 TUS 길이 파일(.len)만 정리 대상이다.
            if (!file.getName().matches("\\d{20}(\\.len)?")) {
                continue;
            }
            if (file.lastModified() < cutoff && file.delete()) {
                deleted++;
            }
        }

        System.out.println("TempFileCleanupJob 완료: " + deleted + "건 삭제 (기준 " + expireHours + "시간)");
    }
}
