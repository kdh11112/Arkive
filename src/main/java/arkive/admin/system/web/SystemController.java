package arkive.admin.system.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;


import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.system.service.SystemService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/system")
public class SystemController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;
	
	@Resource(name = "systemService")
	private SystemService systemService;
	
	public static int pageUnit = 10;
	public static int pageSize = 5;
	
	private CommUtil cmmUtil = new CommUtil();
	
	@RequestMapping(name = "메뉴 관리 페이지 이동", value = "/menuList")
	public String menuList(HttpServletRequest request, ModelMap model) throws Exception {
		return "system/menuList";
	}
	
	@RequestMapping(name = "메뉴 관리 조회", value = "/getMenuList.json")
	public String getMenuList(HttpServletRequest request, ModelMap model) throws Exception{

		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		log.info("--------------------------> /getMenuList.json");
		log.info("getMenuList ----------------------------- type=[{}]", new Object[] { paramMap });

		List<EgovMap> result = systemService.selectMenuList(paramMap);

		model.put("result", result);
		
		return "jsonView";
	}
	
	@RequestMapping(name = "하위메뉴 조회", value = "/getMenuDetailList.json")
	public String getMenuDetailList(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		log.info("--------------------------> /getMenuDetailList.json");
		log.info("getMenuDetailList ----------------------------- type=[{}]", new Object[] { paramMap });
		
		List<EgovMap> result = systemService.selectMenuDetailList(paramMap);

		model.put("result", result);

		return "jsonView";
	}
	
	@RequestMapping(name = "메뉴 중복 조회", value = "/getChkMenuId.json")
	public String getChkMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		log.info("--------------------------> /getChkMenuId.json");
		log.info("getChkMenuId ----------------------------- type=[{}]", new Object[] { paramMap });
		
		int result = systemService.getChkMenuId(paramMap);
		
		model.put("result", result);

		return "jsonView";
	}
	
	@RequestMapping(name = "하위메뉴 중복 조회", value = "/getChkUpMenuId.json")
	public String getChkUpMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		log.info("--------------------------> /getChkUpMenuId.json");
		log.info("getChkUpMenuId ----------------------------- type=[{}]", new Object[] { paramMap });
		
		int result = systemService.getChkUpMenuId(paramMap);
		
		model.put("result", result);
			
		return "jsonView";
	}
	
	@RequestMapping(name = "메뉴 저장", value = "/setMenuSave.json")
	public String setMenuSave(HttpServletRequest request, ModelMap model) throws Exception {

//		HttpSession session = request.getSession();
//		LoginVO loginVo = (LoginVO) session.getAttribute("USER");
//		String userId = loginVo.getUserId();

		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		paramMap.put("userId", "userId");
		
		log.info("--------------------------> /setMenuSave.json");
		log.info("setMenuSave ----------------------------- type=[{}]", new Object[] { paramMap });

		try {
			
			int	result = systemService.setSaveMenu(paramMap);

			model.put("result", result);
		} catch (DataAccessException e) {
			logger.debug("DataAccessException");
		} catch (RuntimeException e) {
			logger.debug("RuntimeException");
		} catch (IOException e) {
			logger.debug("IOException");
		}

		return "jsonView";
	}
	
	@RequestMapping(name = "코드 관리 페이지 이동", value = "/codeList")
	public String codeList(HttpServletRequest request , ModelMap model , HttpSession session){
		return "system/codeList";
	}
	
	@RequestMapping(name = "그룹코드 목록 조회", value = "/getCodeInfoList.json")
    public String getCodeInfoList(@RequestParam("srchCodeNm") String codeNm ,@RequestParam("srchUseAt") String useAt , ModelMap model)  throws Exception {
		 EgovMap egovMap = new EgovMap();
		 egovMap.put("codeNm", codeNm);
		 egovMap.put("useAt", useAt);
		
		log.info("--------------------------> /getCodeInfoList.json");
		log.info("getCodeInfoList ----------------------------- type=[{}]", new Object[] { egovMap });
		
		List<EgovMap> result = systemService.getCodeInfoList(egovMap);
		
		model.put("result", result);

		return "jsonView";
	}
	
	@RequestMapping(name = "상세코드 목록 조회", value = "/getCodeDetailInfoList.json")
	public String getCodeDetailInfoList(@RequestParam("mastrCode") String mastrCode , ModelMap model) throws Exception {
		EgovMap egovMap = new EgovMap();
		egovMap.put("mastrCode", mastrCode);
		
		log.info("--------------------------> /getCodeDetailInfoList.json");
		log.info("getCodeDetailInfoList ----------------------------- type=[{}]", new Object[] { egovMap });
		
		List<EgovMap> result = systemService.getCodeDetailInfoList(egovMap);
		
		model.put("result", result);
		
		return "jsonView";
	}
	
    @RequestMapping(name = "그룹코드 저장", value = "/setCodeInfoList.json")
    public String setCodeInfoList(HttpServletRequest request , ModelMap model) throws Exception {
    	EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
    	
    	log.info("--------------------------> /setCodeInfoList.json");
    	log.info("setCodeInfoList ----------------------------- type=[{}]", new Object[] { paramMap });
    	
    	int result = systemService.setCodeInfoList(paramMap);
    	
    	model.put("result", result);
    	
    	return "jsonView";
    }
    
    @RequestMapping(name = "상세코드 저장", value = "/setCodeDetailInfoList.json")
    public String setCodeDetailInfoList(HttpServletRequest request , ModelMap model, HttpSession session) throws Exception {
    	EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
//    	LoginVO loginVo = (LoginVO) session.getAttribute("USER");
//		String userId = loginVo.getUserId();
    	
    	paramMap.put("userId", "userId");
    	
    	log.info("--------------------------> /setCodeDetailInfoList.json");
    	log.info("setCodeDetailInfoList ----------------------------- type=[{}]", new Object[] { paramMap });
    	
    	int result = systemService.setCodeDetailInfoList(paramMap);
    	
    	model.put("result", result);
    	
    	return "jsonView";
    }
	
	
}
