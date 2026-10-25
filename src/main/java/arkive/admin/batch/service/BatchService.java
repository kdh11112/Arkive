package arkive.admin.batch.service;

import java.time.LocalDate;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 일일 통계 배치.
 */
public interface BatchService {

	EgovMap runDailyStats(LocalDate targetDate) throws Exception;

	List<EgovMap> selectDailyStats(LocalDate statDate) throws Exception;
}
