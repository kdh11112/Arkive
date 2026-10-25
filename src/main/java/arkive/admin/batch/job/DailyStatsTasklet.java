package arkive.admin.batch.job;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import arkive.admin.batch.service.impl.DailyStatsAggregator;
import jakarta.annotation.Resource;

/**
 * dailyStatsJob의 유일한 Step. Job 파라미터 targetDate(YYYY-MM-DD)를 집계한다.
 */
@Component
public class DailyStatsTasklet implements Tasklet {

	@Resource(name = "dailyStatsAggregator")
	private DailyStatsAggregator dailyStatsAggregator;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		String targetDate = chunkContext.getStepContext().getStepExecution()
				.getJobParameters().getString("targetDate");
		if (targetDate == null || targetDate.isBlank()) {
			throw new IllegalArgumentException("배치 파라미터 targetDate가 없습니다.");
		}
		dailyStatsAggregator.aggregate(LocalDate.parse(targetDate.trim()));
		return RepeatStatus.FINISHED;
	}
}
