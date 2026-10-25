package arkive.admin.batch.service.impl;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 일일 통계 집계 본체.
 *
 * <p>같은 날짜를 다시 실행해도 결과가 같도록 기존 행을 지우고 새로 넣는다.
 * 호출한 TaskletStep의 트랜잭션 안에서 실행되므로 일부 집계가 실패하면 전부 롤백된다.</p>
 */
@Component
public class DailyStatsAggregator {

	@Resource(name = "batchMapper")
	private BatchMapper batchMapper;

	public void aggregate(LocalDate statDate) throws Exception {
		EgovMap param = new EgovMap();
		param.put("statDate", Date.valueOf(statDate));
		param.put("startDate", Timestamp.valueOf(statDate.atStartOfDay()));
		param.put("endDate", Timestamp.valueOf(statDate.plusDays(1).atStartOfDay()));
		batchMapper.deleteDailyStats(param);
		batchMapper.insertBoardStats(param);
		batchMapper.insertLoginStats(param);
		batchMapper.insertAccessStats(param);
		batchMapper.insertLectureCompleteStats(param);
		batchMapper.insertSurveyParticipationStats(param);
	}
}
