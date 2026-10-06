package arkive.admin.system.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 공통코드 서비스. 마스터는 MASTR_CODE = DETAIL_CODE 인 행이다.
 */
public interface CommCodeService {

	List<EgovMap> selectMasterList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDetailList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectCmmnCodeList(EgovMap egovMap) throws Exception;

	EgovMap selectCodeInfo(EgovMap egovMap) throws Exception;

	int getChkCode(EgovMap egovMap) throws Exception;

	void insertCode(EgovMap egovMap) throws Exception;

	void updateCode(EgovMap egovMap) throws Exception;

	void deleteCode(EgovMap egovMap) throws Exception;
}
