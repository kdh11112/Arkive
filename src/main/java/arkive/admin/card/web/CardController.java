package arkive.admin.card.web;

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

import arkive.admin.card.service.CardService;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 명함관리. 공개 명함 목록·사용등록 + 내 명함 목록·등록·수정을 제공한다.
 * 근거: eGov v5.0 cop 명함관리(CMTNNCRD+COMTNNCRDUSER, 공개여부·사용등록).
 * 1단계(비로그인) 제약: 사용자는 작성자명 문자열로 식별한다.
 */
@Tag(name = "명함", description = "명함 목록·사용등록, 내 명함 등록·수정·삭제")
@Controller
@RequestMapping("/card")
public class CardController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "cardService")
	private CardService cardService;

	private CommUtil cmmUtil = new CommUtil();

	private void putPage(EgovMap param, HttpServletRequest request, int pageIndex) {
		param.put("firstIndex", (pageIndex - 1) * 10);
		param.put("recordCountPerPage", 10);
	}

	private PaginationInfo buildPage(int pageIndex, int total) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		paginationInfo.setTotalRecordCount(total);
		return paginationInfo;
	}

	private int parsePage(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	@Operation(summary = "명함 목록 (공개 탭 + 내 명함 탭)")
	@RequestMapping(value = "/cardList", method = { RequestMethod.GET, RequestMethod.POST })
	public String cardList(HttpServletRequest request, ModelMap model) throws Exception {
		String tab = cmmUtil.convertHtml(request, "tab");
		if (!"mine".equals(tab)) {
			tab = "public";
		}
		int pageIndex = parsePage(request);
		String userNm = cmmUtil.convertHtml(request, "userNm");
		if (userNm == null) {
			userNm = "";
		}
		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("userNm", userNm);
		putPage(param, request, pageIndex);
		if ("mine".equals(tab)) {
			model.put("resultList", cardService.selectMyCardList(param));
			model.put("paginationInfo", buildPage(pageIndex, cardService.selectMyCardListTotCnt(param)));
		} else {
			model.put("resultList", cardService.selectPublicCardList(param));
			model.put("paginationInfo", buildPage(pageIndex, cardService.selectPublicCardListTotCnt(param)));
		}
		model.put("tab", tab);
		model.put("userNm", userNm);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		return "card/cardList";
	}

	@Operation(summary = "명함 쓰기 화면 (cardId 있으면 수정)")
	@RequestMapping(value = "/cardWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String cardWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String cardId = cmmUtil.convertHtml(request, "cardId");
		if (cardId != null && !cardId.isEmpty()) {
			model.put("detail", cardService.selectCardDetail(cardId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_CARD"));
		return "card/cardWrite";
	}

	@Operation(summary = "명함 등록")
	@RequestMapping(value = "/insertCard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertCard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_CARD");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String cardNm = cmmUtil.convertHtml(request, "cardNm");
			String register = cmmUtil.convertHtml(request, "register");
			if (cardNm == null || cardNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			if (register == null || register.trim().isEmpty()) {
				throw new IllegalArgumentException("등록자를 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("cardId", "NC" + CommUtil.getFileId());
			param.put("cardNm", cardNm.trim());
			param.put("companyNm", cmmUtil.convertHtml(request, "companyNm"));
			param.put("deptNm", cmmUtil.convertHtml(request, "deptNm"));
			param.put("positionNm", cmmUtil.convertHtml(request, "positionNm"));
			param.put("telNo", cmmUtil.convertHtml(request, "telNo"));
			param.put("email", cmmUtil.convertHtml(request, "email"));
			String openYn = cmmUtil.convertHtml(request, "openYn");
			param.put("openYn", "N".equals(openYn) ? "N" : "Y");
			param.put("memo", cmmUtil.convertHtml(request, "memo"));
			param.put("register", register.trim());
			cardService.insertCard(param);
			result.put("success", true);
			result.put("cardId", param.get("cardId"));
		} catch (Exception e) {
			logger.error("insertCard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "명함 수정 (본인 등록분만)")
	@RequestMapping(value = "/updateCard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateCard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("cardId", cmmUtil.convertHtml(request, "cardId"));
			String cardNm = cmmUtil.convertHtml(request, "cardNm");
			if (cardNm == null || cardNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			param.put("cardNm", cardNm.trim());
			param.put("companyNm", cmmUtil.convertHtml(request, "companyNm"));
			param.put("deptNm", cmmUtil.convertHtml(request, "deptNm"));
			param.put("positionNm", cmmUtil.convertHtml(request, "positionNm"));
			param.put("telNo", cmmUtil.convertHtml(request, "telNo"));
			param.put("email", cmmUtil.convertHtml(request, "email"));
			String openYn = cmmUtil.convertHtml(request, "openYn");
			param.put("openYn", "N".equals(openYn) ? "N" : "Y");
			param.put("memo", cmmUtil.convertHtml(request, "memo"));
			param.put("register", cmmUtil.convertHtml(request, "register"));
			param.put("updusr", param.get("register"));
			cardService.updateCard(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateCard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "명함 삭제 (본인 등록분만)")
	@RequestMapping(value = "/deleteCard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteCard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("cardId", cmmUtil.convertHtml(request, "cardId"));
			param.put("register", cmmUtil.convertHtml(request, "register"));
			cardService.deleteCard(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteCard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "명함 사용등록 (타인 공개 명함을 내 명함으로)")
	@RequestMapping(value = "/useCard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> useCard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userNm = cmmUtil.convertHtml(request, "userNm");
			if (userNm == null || userNm.trim().isEmpty()) {
				throw new IllegalArgumentException("사용자 이름을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("cardId", cmmUtil.convertHtml(request, "cardId"));
			param.put("userNm", userNm.trim());
			cardService.useCard(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("useCard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "명함 사용등록 취소")
	@RequestMapping(value = "/cancelUseCard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelUseCard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("cardId", cmmUtil.convertHtml(request, "cardId"));
			param.put("userNm", cmmUtil.convertHtml(request, "userNm"));
			cardService.cancelUseCard(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("cancelUseCard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
