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

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return schedulerFactoryBean -> {
            schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        };
    }

    @Bean
    public JobDetail DBJobDetail() {
        return JobBuilder.newJob(DBJob.class)
            .withIdentity("DBJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger sampleJobTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(DBJobDetail())
            .withIdentity("DBTrigger")
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")) // 10분마다 실행
            .build();
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
