package arkive.admin.system.service.impl;
import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.system.service.SystemService;
import jakarta.annotation.Resource;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

@Service("systemService")
public class SystemServiceImpl extends EgovAbstractServiceImpl implements SystemService {

	protected final Logger logger = Logger.getLogger(getClass());	
	
//	@Autowired
//	private CommUtil cmmUtil;
	
	@Resource(name = "systemMapper")
	private SystemMapper systemMapper;

	@Override
	public List<EgovMap> selectMenuList(EgovMap egovMap) throws Exception {
		return systemMapper.selectMenuList(egovMap);
	}

	@Override
	public List<EgovMap> selectMenuDetailList(EgovMap egovMap) throws Exception {
		return systemMapper.selectMenuDetailList(egovMap);
	}

	@Override
	public int getChkMenuId(EgovMap paramMap) throws Exception {
		int result = 0;
		
		String menuListXSS = (String) paramMap.get("menuList");
		
		menuListXSS = decXSS(menuListXSS);
		
		JSONArray jsonArr = new JSONArray(menuListXSS);
		
		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject jsonObj = jsonArr.getJSONObject(i);
			
			EgovMap egovMap = new EgovMap();
			
			egovMap.put("menuId", jsonObj.getString("menuId"));
			
			result += systemMapper.getChkMenuId(egovMap);
		}
		
		return result;
	}
	
	// XSS디코딩
	public static String decXSS(String value) {
		if (value == null || value.trim().equals("")) {
			return "";
		}
		
		String returnValue = value;
		
		returnValue = returnValue.replaceAll("&quot;", "\"");
		returnValue = returnValue.replaceAll("&lt;", "<");
		returnValue = returnValue.replaceAll("&gt;", ">");
		returnValue = returnValue.replaceAll("&#39;", "'");
		returnValue = returnValue.replaceAll("&amp;", "&");
	
		return returnValue;
	}

	@Override
	public int getChkUpMenuId(EgovMap paramMap) throws Exception {
		return systemMapper.getChkUpMenuId(paramMap);
	}
	
	
	

	
}
