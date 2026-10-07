package arkive.admin.counsel.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 상담 MyBatis 매퍼. SQL은 Counsel_SQL.xml에 있다.
 */
@EgovMapper("counselMapper")
public interface CounselMapper {

	List<EgovMap> selectCounselList(EgovMap egovMap) throws Exception;

	int selectCounselListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectCounselDetail(String counselId) throws Exception;

	int checkCounselPassword(EgovMap egovMap) throws Exception;

	void insertCounsel(EgovMap egovMap) throws Exception;

	void updateCounsel(EgovMap egovMap) throws Exception;

	void deleteCounsel(String counselId) throws Exception;

	List<EgovMap> selectCounselAnswerList(EgovMap egovMap) throws Exception;

	int selectCounselAnswerListTotCnt(EgovMap egovMap) throws Exception;

	void updateCounselAnswer(EgovMap egovMap) throws Exception;
}
