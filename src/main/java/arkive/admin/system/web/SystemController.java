package arkive.admin.system.web;

import java.util.List;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@Controller
@Tag(name = "시스템 메뉴", description = "좌측 메뉴 트리·상세·등록·수정·삭제")
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
	
	@Operation(summary = "메뉴 관리 화면")
	@RequestMapping(name = "메뉴 관리", value = "/menuList", method = RequestMethod.GET)
	public String menuList(HttpServletRequest request, ModelMap model) throws Exception {
		return "system/menuList";
	}
	
	@Operation(summary = "메뉴 트리 조회 (jstree용)")
	@RequestMapping(name = "메뉴 관리 조회", value = "/getMenuList.json", method = RequestMethod.GET)
	public String getMenuList(HttpServletRequest request, ModelMap model) throws Exception{
		EgovMap egovMap = new EgovMap();

		List<EgovMap> menuList = systemService.selectMenuList(egovMap);

		model.put("menuList", menuList);
		
		return "jsonView";
	}
	
	@Operation(summary = "하위메뉴 목록 조회")
	@RequestMapping(name = "하위메뉴 조회", value = "/getMenuDetailList.json", method = RequestMethod.POST)
	public String getMenuDetailList(HttpServletRequest request, ModelMap model) throws Exception {
		
		String menuId = cmmUtil.convertHtml(request, "menuId");
		EgovMap egovMap = new EgovMap();

		egovMap.put("menuId", menuId);
		List<EgovMap> menuListR = systemService.selectMenuDetailList(egovMap);

		model.put("menuList", menuListR);

		return "jsonView";
	}
	
	@Operation(summary = "메뉴 ID 중복 확인")
	@RequestMapping(name = "메뉴 중복 조회", value = "/getChkMenuId.json", method = RequestMethod.POST)
	public String getChkMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		int result = systemService.getChkMenuId(paramMap);
		
		model.put("result", result);

		return "jsonView";
	}
	
	@Operation(summary = "하위메뉴 존재 확인")
	@RequestMapping(name = "하위메뉴 중복 조회", value = "/getChkUpMenuId.json", method = RequestMethod.POST)
	public String getChkUpMenuId(HttpServletRequest request, ModelMap model) throws Exception {
		
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		
		int result = systemService.getChkUpMenuId(paramMap);
		
		model.put("result", result);
			
		return "jsonView";
	}

	@Operation(summary = "메뉴 등록")
	@RequestMapping(name = "메뉴 등록", value = "/setInsertMenu.json", method = RequestMethod.POST)
	public String setInsertMenu(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		systemService.setInsertMenu(paramMap);
		model.put("result", "success");
		return "jsonView";
	}

	@Operation(summary = "메뉴 수정")
	@RequestMapping(name = "메뉴 수정", value = "/setUpdateMenu.json", method = RequestMethod.POST)
	public String setUpdateMenu(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		systemService.setUpdateMenu(paramMap);
		model.put("result", "success");
		return "jsonView";
	}

	@Operation(summary = "메뉴 삭제")
	@RequestMapping(name = "메뉴 삭제", value = "/setDeleteMenu.json", method = RequestMethod.POST)
	public String setDeleteMenu(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap paramMap = cmmUtil.makeRequestEgovMap(request);
		systemService.setDeleteMenu(paramMap);
		model.put("result", "success");
		return "jsonView";
	}


	
}
