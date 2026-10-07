package arkive.admin.address.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.address.service.AddressService;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 주소록관리. 부서일정 팝업(부서·사용자 선택)의 기반 데이터로 쓴다.
 * 근거: eGov v5.0 cop 주소록관리. 타 프로젝트에 주소록 모듈이 없어(서베이) Arkive-local로 만들었다.
 */
@Tag(name = "주소록", description = "주소록 목록·등록·수정·삭제")
@Controller
@RequestMapping("/address")
public class AddressController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "addressService")
	private AddressService addressService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "주소록 목록 (이름·부서 검색 + 10건 페이징)")
	@RequestMapping(value = "/addressList", method = { RequestMethod.GET, RequestMethod.POST })
	public String addressList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = 1;
		try {
			pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			if (pageIndex < 1) {
				pageIndex = 1;
			}
		} catch (NumberFormatException e) {
			pageIndex = 1;
		}
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);

		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", addressService.selectAddressList(param));
		paginationInfo.setTotalRecordCount(addressService.selectAddressListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "address/addressList";
	}

	@Operation(summary = "주소록 쓰기 화면 (addrId 있으면 수정)")
	@RequestMapping(value = "/addressWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String addressWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String addrId = cmmUtil.convertHtml(request, "addrId");
		if (addrId != null && !addrId.isEmpty()) {
			model.put("detail", addressService.selectAddressDetail(addrId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_ADDRESS"));
		return "address/addressWrite";
	}

	@Operation(summary = "주소록 등록")
	@RequestMapping(value = "/insertAddress.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertAddress(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_ADDRESS");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String addrNm = cmmUtil.convertHtml(request, "addrNm");
			if (addrNm == null || addrNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("addrId", "AB" + CommUtil.getFileId());
			param.put("addrNm", addrNm.trim());
			param.put("deptNm", cmmUtil.convertHtml(request, "deptNm"));
			param.put("telNo", cmmUtil.convertHtml(request, "telNo"));
			param.put("email", cmmUtil.convertHtml(request, "email"));
			param.put("memo", cmmUtil.convertHtml(request, "memo"));
			param.put("register", "SYSTEM");
			addressService.insertAddress(param);
			result.put("success", true);
			result.put("addrId", param.get("addrId"));
		} catch (Exception e) {
			logger.error("insertAddress failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "주소록 수정")
	@RequestMapping(value = "/updateAddress.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateAddress(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("addrId", cmmUtil.convertHtml(request, "addrId"));
			String addrNm = cmmUtil.convertHtml(request, "addrNm");
			if (addrNm == null || addrNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			param.put("addrNm", addrNm.trim());
			param.put("deptNm", cmmUtil.convertHtml(request, "deptNm"));
			param.put("telNo", cmmUtil.convertHtml(request, "telNo"));
			param.put("email", cmmUtil.convertHtml(request, "email"));
			param.put("memo", cmmUtil.convertHtml(request, "memo"));
			param.put("updusr", "SYSTEM");
			addressService.updateAddress(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateAddress failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "주소록 삭제")
	@RequestMapping(value = "/deleteAddress.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteAddress(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			addressService.deleteAddress(cmmUtil.convertHtml(request, "addrId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteAddress failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
