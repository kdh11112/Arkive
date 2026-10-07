package arkive.admin.counsel.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 상담 서비스.
 */
public interface CounselService {

	List<EgovMap> selectCounselList(EgovMap egovMap) throws Exception;

	int selectCounselListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectCounselDetail(String counselId) throws Exception;

	boolean checkCounselPassword(EgovMap egovMap) throws Exception;

	void insertCounsel(EgovMap egovMap) throws Exception;

	void updateCounsel(EgovMap egovMap) throws Exception;

	void deleteCounsel(String counselId) throws Exception;

	List<EgovMap> selectCounselAnswerList(EgovMap egovMap) throws Exception;

	int selectCounselAnswerListTotCnt(EgovMap egovMap) throws Exception;

	void updateCounselAnswer(EgovMap egovMap) throws Exception;
}
