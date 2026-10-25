package arkive.admin.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import arkive.admin.batch.job.DailyStatsTasklet;
import jakarta.annotation.Resource;

/**
 * Spring Batch 인프라와 일일 집계 Job.
 *
 * <p>BATCH_* 메타데이터 테이블 없이 동작하도록 ResourcelessJobRepository를 쓴다.
 * 실행 이력이 남지 않으므로 재시작·중복 실행 관리는 화면에서 같은 날짜 재집계(멱등)로 처리한다.
 * 앱 시작 시 자동 실행은 spring.batch.job.enabled=false로 꺼져 있다.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfiguration {

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;

	@Bean
	@Override
	public JobRepository jobRepository() {
		return new ResourcelessJobRepository();
	}

	/** 기본 설정은 'transactionManager'라는 이름의 빈을 찾으므로 이 프로젝트의 txManager를 넘긴다. */
	@Override
	protected PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Bean
	public Job dailyStatsJob(JobRepository jobRepository, Step dailyStatsStep) {
		return new JobBuilder("dailyStatsJob", jobRepository)
				.start(dailyStatsStep)
				.build();
	}

	@Bean
	public Step dailyStatsStep(JobRepository jobRepository, DailyStatsTasklet dailyStatsTasklet) {
		return new StepBuilder("dailyStatsStep", jobRepository)
				.tasklet(dailyStatsTasklet, transactionManager)
				.build();
	}
}
