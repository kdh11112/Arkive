package arkive.admin.system.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 공통코드 MyBatis 매퍼. SQL은 CommCode_SQL.xml에 있다.
 */
@EgovMapper("commCodeMapper")
public interface CommCodeMapper {

	List<EgovMap> selectMasterList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectDetailList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectCmmnCodeList(EgovMap egovMap) throws Exception;

	EgovMap selectCodeInfo(EgovMap egovMap) throws Exception;

	int getChkCode(EgovMap egovMap) throws Exception;

	void insertCode(EgovMap egovMap) throws Exception;

	void updateCode(EgovMap egovMap) throws Exception;

	void deleteCode(EgovMap egovMap) throws Exception;
}
