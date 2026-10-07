package arkive.admin.faq.web;

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

import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.HtmlSanitizer;
import arkive.admin.faq.service.FaqService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * FAQ관리. BOARD 테이블을 BOARD_TYPE='FAQ' 고정으로 재사용한다(신규 테이블 없음).
 * 참고: BLMS faqList(bbsId=04)+아코디언, pony notice 단순 CRUD.
 * 개선점: 에디터 의존성 없이 plain textarea, 조회수·이전글/다음글·첨부 제외, 이중등록방지+sanitize 적용.
 */
@Tag(name = "FAQ", description = "FAQ 목록·등록·수정·삭제")
@Controller
@RequestMapping("/faq")
public class FaqController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "faqService")
	private FaqService faqService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "FAQ 목록 (제목 검색 + 오프셋 10건 페이징, 아코디언)")
	@RequestMapping(value = "/faqList", method = { RequestMethod.GET, RequestMethod.POST })
	public String faqList(HttpServletRequest request, ModelMap model) throws Exception {
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
		model.put("resultList", faqService.selectFaqList(param));
		paginationInfo.setTotalRecordCount(faqService.selectFaqListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "faq/faqList";
	}

	@Operation(summary = "FAQ 글쓰기 화면 (boardId 있으면 수정)")
	@RequestMapping(value = "/faqWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String faqWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		if (boardId != null && !boardId.isEmpty()) {
			model.put("detail", faqService.selectFaqDetail(boardId));
		}
		// 이중등록방지 토큰 발급. FAQ 단독 key로 분리한다.
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_FAQ"));
		return "faq/faqWrite";
	}

	@Operation(summary = "FAQ 등록 (제목 필수)")
	@RequestMapping(value = "/insertFaq.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertFaq(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_FAQ");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("boardId", "BD" + CommUtil.getFileId());
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("register", "SYSTEM");
			faqService.insertFaq(param);
			result.put("success", true);
			result.put("boardId", param.get("boardId"));
		} catch (Exception e) {
			logger.error("insertFaq failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "FAQ 수정")
	@RequestMapping(value = "/updateFaq.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateFaq(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("boardId", cmmUtil.convertHtml(request, "boardId"));
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("updusr", "SYSTEM");
			faqService.updateFaq(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateFaq failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "FAQ 삭제")
	@RequestMapping(value = "/deleteFaq.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteFaq(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			faqService.deleteFaq(cmmUtil.convertHtml(request, "boardId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteFaq failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
