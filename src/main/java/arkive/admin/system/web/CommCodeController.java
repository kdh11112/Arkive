package arkive.admin.system.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.system.service.CommCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 공통코드 관리. 마스터는 MASTR_CODE = DETAIL_CODE 인 행으로 표현한다.
 * 런타임 공용 조회(getCmmnCodeList.json)는 셀렉트박스 등에 쓴다.
 * 근거: eGov v5.0 sym 공통코드(공통분류·상세코드). 별도 마스터 테이블 없이 1테이블로 관리한다.
 */
@Controller
@Tag(name = "공통코드", description = "마스터·상세 코드 관리, 셀렉트박스 공용 조회")
@RequestMapping("/system")
public class CommCodeController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "commCodeService")
	private CommCodeService commCodeService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "공통코드 관리 화면")
	@RequestMapping(name = "공통코드 관리", value = "/commCodeList", method = RequestMethod.GET)
	public String commCodeList(HttpServletRequest request, ModelMap model) throws Exception {
		return "system/commCodeList";
	}

	@Operation(summary = "마스터 목록 조회")
	@RequestMapping(name = "마스터 목록", value = "/getMasterList.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getMasterList(HttpServletRequest request) throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		Map<String, Object> result = new HashMap<>();
		result.put("resultList", commCodeService.selectMasterList(param));
		return result;
	}

	@Operation(summary = "상세 목록 조회 (mastrCode 지정)")
	@RequestMapping(name = "상세 목록", value = "/getDetailList.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getDetailList(HttpServletRequest request) throws Exception {
		EgovMap param = new EgovMap();
		param.put("mastrCode", cmmUtil.convertHtml(request, "mastrCode"));
		Map<String, Object> result = new HashMap<>();
		result.put("resultList", commCodeService.selectDetailList(param));
		return result;
	}

	@Operation(summary = "셀렉트박스 공용 코드 조회 (사용중만, 순서대로)")
	@RequestMapping(name = "공용 코드 조회", value = "/getCmmnCodeList.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getCmmnCodeList(HttpServletRequest request) throws Exception {
		EgovMap param = new EgovMap();
		param.put("mastrCode", cmmUtil.convertHtml(request, "mastrCode"));
		List<EgovMap> rows = commCodeService.selectCmmnCodeList(param);
		Map<String, Object> result = new HashMap<>();
		result.put("resultList", rows);
		return result;
	}

	@Operation(summary = "코드 단건 조회 (수정 모달 채우기용)")
	@RequestMapping(name = "코드 단건 조회", value = "/getCodeInfo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getCodeInfo(HttpServletRequest request) throws Exception {
		EgovMap param = new EgovMap();
		param.put("mastrCode", cmmUtil.convertHtml(request, "mastrCode"));
		param.put("detailCode", cmmUtil.convertHtml(request, "detailCode"));
		Map<String, Object> result = new HashMap<>();
		result.put("result", commCodeService.selectCodeInfo(param));
		return result;
	}

	@Operation(summary = "코드 등록 (중복이면 fail 반환)")
	@RequestMapping(name = "코드 등록", value = "/setInsertCode.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setInsertCode(HttpServletRequest request) throws Exception {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = cmmUtil.makeRequestEgovMap(request);
			normalizeCodeParam(param, true);
			if (commCodeService.getChkCode(param) > 0) {
				throw new IllegalArgumentException("이미 등록된 코드입니다.");
			}
			param.put("register", "SYSTEM");
			commCodeService.insertCode(param);
			result.put("result", "success");
		} catch (Exception e) {
			logger.error("setInsertCode failed", e);
			result.put("result", "fail");
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "코드 수정")
	@RequestMapping(name = "코드 수정", value = "/setUpdateCode.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setUpdateCode(HttpServletRequest request) throws Exception {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = cmmUtil.makeRequestEgovMap(request);
			normalizeCodeParam(param, false);
			param.put("updusr", "SYSTEM");
			commCodeService.updateCode(param);
			result.put("result", "success");
		} catch (Exception e) {
			logger.error("setUpdateCode failed", e);
			result.put("result", "fail");
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "코드 삭제 (마스터 삭제는 상세 포함)")
	@RequestMapping(name = "코드 삭제", value = "/setDeleteCode.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> setDeleteCode(HttpServletRequest request) throws Exception {
		EgovMap param = new EgovMap();
		param.put("mastrCode", cmmUtil.convertHtml(request, "mastrCode"));
		param.put("detailCode", cmmUtil.convertHtml(request, "detailCode"));
		commCodeService.deleteCode(param);
		Map<String, Object> result = new HashMap<>();
		result.put("result", "success");
		return result;
	}

	/**
	 * 코드 파라미터 정규화. 마스터 등록이면 DETAIL_CODE에 MASTR_CODE를 넣는다.
	 * 숫자·빈값 기본값을 채운다.
	 */
	private void normalizeCodeParam(EgovMap param, boolean isInsert) throws Exception {
		String mastrCode = String.valueOf(param.get("mastrCode"));
		if (mastrCode == null || mastrCode.trim().isEmpty() || "null".equals(mastrCode)) {
			throw new IllegalArgumentException("마스터코드는 필수입니다.");
		}
		param.put("mastrCode", mastrCode.trim());
		String detailCode = String.valueOf(param.get("detailCode"));
		if ("true".equals(String.valueOf(param.get("isMaster")))) {
			param.put("detailCode", mastrCode.trim());
		} else {
			if (detailCode == null || detailCode.trim().isEmpty() || "null".equals(detailCode)) {
				throw new IllegalArgumentException("상세코드는 필수입니다.");
			}
			param.put("detailCode", detailCode.trim());
		}
		int ordr = 1;
		try {
			ordr = Integer.parseInt(String.valueOf(param.get("ordr")).trim());
		} catch (Exception e) {
			ordr = 1;
		}
		if (ordr < 1) {
			ordr = 1;
		}
		param.put("ordr", ordr);
		if (param.get("useYn") == null || String.valueOf(param.get("useYn")).trim().isEmpty()) {
			param.put("useYn", "Y");
		}
		for (String key : new String[] { "codeNm", "cdDc", "refrn1", "refrn2", "refrn3", "refrn4" }) {
			if (param.get(key) == null || "null".equals(String.valueOf(param.get(key)))) {
				param.put(key, "");
			}
		}
		if (isInsert && (param.get("codeNm") == null || String.valueOf(param.get("codeNm")).trim().isEmpty())) {
			throw new IllegalArgumentException("코드명은 필수입니다.");
		}
	}
}
