package arkive.admin.stats.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 게시판·접속 현황 통계.
 */
public interface StatsService {

	List<EgovMap> selectBoardStats() throws Exception;

	List<EgovMap> selectEventStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDailyLoginStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDailyAccessStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDownloadStats(EgovMap egovMap) throws Exception;
}
