package arkive.com.config;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.JobBuilder;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // TempFileCleanupJob이 스케줄러 컨텍스트에서 ApplicationContext를 꺼내므로 이 연결은 유지한다.
    // DB 백업 작업은 예약하지 않는다. PostgreSQL 백업은 pg_dump 등 별도 도구로 수행한다.
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return schedulerFactoryBean -> {
            schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        };
    }

    @Bean
    public JobDetail tempCleanupJobDetail() {
        return JobBuilder.newJob(TempFileCleanupJob.class)
            .withIdentity("TempCleanupJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger tempCleanupTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(tempCleanupJobDetail())
            .withIdentity("TempCleanupTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?")) // 매일 03시 실행
            .build();
    }
}
