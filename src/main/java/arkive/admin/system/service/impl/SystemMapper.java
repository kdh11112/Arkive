package arkive.admin.system.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

@EgovMapper("systemMapper")
public interface SystemMapper {

	List<EgovMap> selectMenuList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectMenuDetailList(EgovMap egovMap) throws Exception;

	int getChkMenuId(EgovMap egovMap) throws Exception;

	int getChkUpMenuId(EgovMap paramMap) throws Exception;
	
	
}
