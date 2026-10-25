package arkive.admin.map.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.map.service.MapGuideService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 약도관리. 주소·좌표를 등록하고 카카오 지도로 미리보기를 제공한다.
 * 카카오 JavaScript 키가 없으면 지도 스크립트를 로드하지 않고 주소·좌표만 표시한다.
 */
@Tag(name = "약도관리", description = "약도 등록·조회·카카오 지도 표시")
@Controller
@RequestMapping("/map")
public class MapController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "mapGuideService")
	private MapGuideService mapGuideService;

	@Value("${Globals.kakao.mapKey:}")
	private String kakaoMapKey;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "약도 목록")
	@RequestMapping(value = "/mapList", method = { RequestMethod.GET, RequestMethod.POST })
	public String mapList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);
		String useYn = cmmUtil.convertHtml(request, "useYn");
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			useYn = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("useYn", useYn);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("useYn", useYn);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("mapKeyConfigured", !kakaoMapKey.isBlank());
		model.put("resultList", mapGuideService.selectMapGuideList(param));
		paginationInfo.setTotalRecordCount(mapGuideService.selectMapGuideListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "map/mapList";
	}

	@Operation(summary = "약도 등록·수정 화면")
	@RequestMapping(value = "/mapWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String mapWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String mapId = cmmUtil.convertHtml(request, "mapId");
		if (!mapId.trim().isEmpty()) {
			EgovMap detail = mapGuideService.selectMapGuideDetail(mapId);
			if (detail == null) {
				throw new IllegalArgumentException("약도가 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("kakaoMapKey", kakaoMapKey);
		model.put("mapKeyConfigured", !kakaoMapKey.isBlank());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "MAP_GUIDE"));
		return "map/mapWrite";
	}

	@Operation(summary = "약도 상세")
	@RequestMapping(value = "/mapView", method = { RequestMethod.GET, RequestMethod.POST })
	public String mapView(HttpServletRequest request, ModelMap model) throws Exception {
		String mapId = cmmUtil.convertHtml(request, "mapId");
		EgovMap detail = mapGuideService.selectMapGuideDetail(mapId);
		if (detail == null) {
			return "redirect:/map/mapList";
		}
		model.put("detail", detail);
		model.put("kakaoMapKey", kakaoMapKey);
		model.put("mapKeyConfigured", !kakaoMapKey.isBlank());
		return "map/mapView";
	}

	@Operation(summary = "약도 등록")
	@RequestMapping(value = "/insertMapGuide.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertMapGuide(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request);
			EgovMap param = mapParam(request);
			mapGuideService.insertMapGuide(param);
			return param.get("mapId");
		});
	}

	@Operation(summary = "약도 수정")
	@RequestMapping(value = "/updateMapGuide.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateMapGuide(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = mapParam(request);
			param.put("mapId", cmmUtil.convertHtml(request, "mapId"));
			mapGuideService.updateMapGuide(param);
			return param.get("mapId");
		});
	}

	@Operation(summary = "약도 삭제")
	@RequestMapping(value = "/deleteMapGuide.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteMapGuide(HttpServletRequest request) {
		return execute(() -> {
			mapGuideService.deleteMapGuide(cmmUtil.convertHtml(request, "mapId"));
			return null;
		});
	}

	private EgovMap mapParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("address", cmmUtil.convertHtml(request, "address"));
		param.put("latitude", cmmUtil.convertHtml(request, "latitude"));
		param.put("longitude", cmmUtil.convertHtml(request, "longitude"));
		param.put("description", cmmUtil.convertHtml(request, "description"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		return param;
	}

	private Map<String, Object> execute(Action action) {
		Map<String, Object> result = new HashMap<>();
		try {
			Object data = action.run();
			result.put("success", true);
			if (data != null) {
				result.put("id", data);
			}
		} catch (Exception e) {
			logger.error("map guide action failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private void requireToken(HttpServletRequest request) {
		boolean valid;
		try {
			valid = EgovDoubleSubmitHelper.checkAndSaveToken("MAP_GUIDE");
		} catch (RuntimeException e) {
			valid = false;
		}
		if (!valid) {
			throw new IllegalStateException("중복 제출이 감지되었습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
		}
	}

	private PaginationInfo pagination(int pageIndex) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		return paginationInfo;
	}

	private int pageIndex(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	@FunctionalInterface
	private interface Action {
		Object run() throws Exception;
	}
}
