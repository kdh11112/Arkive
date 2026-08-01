package arkive.admin.system.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

public interface SystemService {

	List<EgovMap> selectMenuList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectMenuDetailList(EgovMap egovMap) throws Exception;

	int getChkMenuId(EgovMap paramMap) throws Exception;

	int getChkUpMenuId(EgovMap paramMap) throws Exception;
	

}
