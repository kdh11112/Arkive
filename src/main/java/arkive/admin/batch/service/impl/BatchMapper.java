package arkive.admin.batch.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 일일 통계 집계 MyBatis 매퍼.
 */
@EgovMapper("batchMapper")
public interface BatchMapper {

	int deleteDailyStats(EgovMap egovMap) throws Exception;

	int insertBoardStats(EgovMap egovMap) throws Exception;

	int insertLoginStats(EgovMap egovMap) throws Exception;

	int insertAccessStats(EgovMap egovMap) throws Exception;

	int insertLectureCompleteStats(EgovMap egovMap) throws Exception;

	int insertSurveyParticipationStats(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDailyStats(EgovMap egovMap) throws Exception;
}
