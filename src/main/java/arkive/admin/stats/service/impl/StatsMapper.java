package arkive.admin.stats.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 통계 집계 MyBatis 매퍼.
 */
@EgovMapper("statsMapper")
public interface StatsMapper {

	List<EgovMap> selectBoardStats() throws Exception;

	List<EgovMap> selectEventStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDailyLoginStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDailyAccessStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDownloadStats(EgovMap egovMap) throws Exception;
}
