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
	public List<EgovMap> selectMenuList(EgovMap paramMap) throws Exception {
		return systemMapper.selectMenuList(paramMap);
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

	@Override
	public int setSaveMenu(EgovMap paramMap) throws Exception {
		int result = 0;
		
		String userId = (String) paramMap.get("userId");
		
		String menuListXSS = (String) paramMap.get("menuList");
		
		menuListXSS = decXSS(menuListXSS);
		
		JSONArray jsonArr = new JSONArray(menuListXSS);
		
		String upMenuId = "";
		
		if (jsonArr.length() > 0) {
		    JSONObject firstObj = jsonArr.getJSONObject(0);
		    upMenuId = firstObj.optString("upMenuId", "");
		}
		
		// 삭제 후 저장
		result += systemMapper.setDeleteMenu(upMenuId);
		
		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject jsonObj = jsonArr.getJSONObject(i);
			
			EgovMap egovMap = new EgovMap();
			
			egovMap.put("menuId", jsonObj.getString("menuId"));
			egovMap.put("menuNm", jsonObj.getString("menuNm"));
			if (!upMenuId.equals("ME00000000")) {
				egovMap.put("menuCours", jsonObj.getString("menuCours"));
			}
			egovMap.put("useYn", jsonObj.getString("useYn"));
			egovMap.put("grad", jsonObj.getString("grad"));
			egovMap.put("upMenuId", jsonObj.getString("upMenuId"));
			egovMap.put("ordr", jsonObj.getInt("ordr"));
			egovMap.put("userId", userId);
			
			result += systemMapper.setInsertMenu(egovMap);
		}
		
		return result;
	}

	@Override
	public List<EgovMap> getCodeInfoList(EgovMap egovMap) throws Exception {
		return  systemMapper.getCodeInfoList(egovMap);
	}

	@Override
	public List<EgovMap> getCodeDetailInfoList(EgovMap egovMap) throws Exception {
		return  systemMapper.getCodeDetailInfoList(egovMap);
	}

	@Override
	public int setCodeInfoList(EgovMap paramMap) throws Exception {
		int result = 0; 

		JSONArray jsonArray = new JSONArray((String) paramMap.get("codeList"));
		
		for (int i = 0; i < jsonArray.length(); i++) {
		
	        JSONObject rowObject = jsonArray.getJSONObject(i);

	        String crud = rowObject.getString("crud");
	        EgovMap paramList = new EgovMap();
	        if("C".equals(crud)) {
	        	paramList.clear();
				
				paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				paramList.put("useYn", rowObject.getString("useYn"));
				
				result += systemMapper.setInsertCodeInfoList(paramList);
	        }else if("U".equals(crud)){
	        	paramList.clear();
				
				paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				paramList.put("useAt", rowObject.getString("useAt"));
				
				result += systemMapper.setUpdateCodeInfoList(paramList);
	        }else {
	        	paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				
				result += systemMapper.setDeleteCodeInfoList(paramList);
	        }
			
		}

		return result;
	}

	@Override
	public int setCodeDetailInfoList(EgovMap paramMap) throws Exception {
		int result = 0; 
		//(String) 이 없으면 작동이 되지않음
		JSONArray jsonArray = new JSONArray((String) paramMap.get("codeDetailList"));

		for (int i = 0; i < jsonArray.length(); i++) {
		
	        JSONObject rowObject = jsonArray.getJSONObject(i);

	        String crud = rowObject.getString("crud");
	        EgovMap paramList = new EgovMap();
	        if("C".equals(crud)) {
	        	paramList.clear();
				
				paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				paramList.put("useAt", rowObject.getString("useAt"));
				paramList.put("refrn1", rowObject.optInt("refrn_1"));
				paramList.put("refrn2", rowObject.optString("refrn_2"));
				
				result += systemMapper.setInsertCodeInfoList(paramList);
	        }else if("U".equals(crud)){
	        	paramList.clear();
				
				paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				paramList.put("useAt", rowObject.getString("useAt"));
				paramList.put("refrn1", rowObject.optInt("refrn_1"));
				paramList.put("refrn2", rowObject.optString("refrn_2"));
				
				result += systemMapper.setUpdateCodeInfoList(paramList);
	        }else {
	        	paramList.clear();
				
				paramList.put("mastrCode", rowObject.getString("mastrCode"));
				paramList.put("code", rowObject.getString("code"));
				paramList.put("codeNm", rowObject.getString("codeNm"));
				paramList.put("ordr", rowObject.getInt("ordr"));
				
				result += systemMapper.setDeleteCodeInfoList(paramList);
	        }
			
		}

		return result;
	}
	
	
	

	
}
