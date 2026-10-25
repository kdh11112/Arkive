package arkive.admin.batch.service.impl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import arkive.admin.batch.service.BatchService;
import jakarta.annotation.Resource;

/**
 * 일일 통계 배치 실행과 집계 결과 조회.
 *
 * <p>ResourcelessJobRepository는 실행 이력을 저장하지 않고 스레드 안전하지 않으므로
 * 한 인스턴스 안에서 실행을 직렬화한다. 다중 인스턴스에서는 공유 스케줄러·잠금 정책이 필요하다.</p>
 */
@Service("batchService")
public class BatchServiceImpl extends EgovAbstractServiceImpl implements BatchService {

	@Resource(name = "dailyStatsJob")
	private Job dailyStatsJob;

	@Resource(name = "jobLauncher")
	private JobLauncher jobLauncher;

	@Resource(name = "batchMapper")
	private BatchMapper batchMapper;

	@Override
	public synchronized EgovMap runDailyStats(LocalDate targetDate) throws Exception {
		if (targetDate == null) {
			throw new IllegalArgumentException("집계 날짜를 입력하세요.");
		}
		if (targetDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("미래 날짜는 집계할 수 없습니다.");
		}
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("targetDate", targetDate.toString())
				.addLong("runTime", System.currentTimeMillis())
				.toJobParameters();
		JobExecution execution = jobLauncher.run(dailyStatsJob, jobParameters);

		EgovMap result = new EgovMap();
		result.put("targetDate", targetDate.toString());
		result.put("status", execution.getStatus().name());
		result.put("exitCode", execution.getExitStatus().getExitCode());
		if (execution.getStatus() != BatchStatus.COMPLETED) {
			String message = "배치가 정상 완료되지 않았습니다.";
			List<Throwable> failures = execution.getAllFailureExceptions();
			if (!failures.isEmpty() && failures.get(0).getMessage() != null) {
				message = failures.get(0).getMessage();
			}
			result.put("message", message);
		}
		return result;
	}

	@Override
	public List<EgovMap> selectDailyStats(LocalDate statDate) throws Exception {
		EgovMap param = new EgovMap();
		param.put("statDate", Date.valueOf(statDate));
		return batchMapper.selectDailyStats(param);
	}
}
