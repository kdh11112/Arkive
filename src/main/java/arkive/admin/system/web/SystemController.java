package arkive.admin.system.web;

import java.util.List;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.system.service.SystemService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
	
	@RequestMapping(name = "메뉴 관리", value = "/menuList")
	public String menuList(HttpServletRequest request, ModelMap model) throws Exception {
		return "system/menuList";
	}
	
	@RequestMapping(name = "메뉴 관리 조회", value = "/getMenuList.json")
	public String getMenuList(HttpServletRequest request, ModelMap model) throws Exception{
		EgovMap egovMap = new EgovMap();

		List<EgovMap> menuList = systemService.selectMenuList(egovMap);

		model.put("menuList", menuList);
		
		return "jsonView";
	}
	
	@RequestMapping(name = "하위메뉴 조회", value = "/getMenuDetailList.json")
	public String getMenuDetailList(HttpServletRequest request, ModelMap model) throws Exception {
		
		String menuId = cmmUtil.convertHtml(request, "menuId");
		EgovMap egovMap = new EgovMap();

		egovMap.put("menuId", menuId);
		List<EgovMap> menuListR = systemService.selectMenuDetailList(egovMap);

		model.put("menuList", menuListR);

		return "jsonView";
	}
	
	@RequestMapping(name = "메뉴 중복 조회", value = "/getChkMenuId.json")
	public String getChkMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		int result = systemService.getChkMenuId(paramMap);
		
		model.put("result", result);

		return "jsonView";
	}
	
	@RequestMapping(name = "하위메뉴 중복 조회", value = "/getChkUpMenuId.json")
	public String getChkUpMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		int result = systemService.getChkUpMenuId(paramMap);
		
		model.put("result", result);
			
		return "jsonView";
	}
	
	
}
