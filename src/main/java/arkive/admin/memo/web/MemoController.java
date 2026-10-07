package arkive.admin.memo.web;

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
import arkive.admin.memo.service.MemoService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 메모보고. 보고자 등록 → 대상자 확인/의견. 상태 전이는 SQL 조건으로 강제한다.
 * 근거: eGov v5.0 cop 메모보고(보고자-대상자 상태 전이). 상태값은 Arkive-local(보고/확인/의견)이다.
 */
@Tag(name = "메모보고", description = "메모보고 목록·등록·수정·삭제, 확인·의견 처리")
@Controller
@RequestMapping("/memo")
public class MemoController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "memoService")
	private MemoService memoService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "메모보고 목록 (검색 + 상태 필터 + 10건 페이징)")
	@RequestMapping(value = "/memoList", method = { RequestMethod.GET, RequestMethod.POST })
	public String memoList(HttpServletRequest request, ModelMap model) throws Exception {
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
		param.put("status", cmmUtil.convertHtml(request, "status"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("status", param.get("status"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", memoService.selectMemoList(param));
		paginationInfo.setTotalRecordCount(memoService.selectMemoListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "memo/memoList";
	}

	@Operation(summary = "메모보고 쓰기 화면 (reportId 있으면 수정)")
	@RequestMapping(value = "/memoWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String memoWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String reportId = cmmUtil.convertHtml(request, "reportId");
		if (reportId != null && !reportId.isEmpty()) {
			model.put("detail", memoService.selectMemoDetail(reportId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_MEMO"));
		return "memo/memoWrite";
	}

	@Operation(summary = "메모보고 상세 (확인·의견 버튼 포함)")
	@RequestMapping(value = "/memoDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String memoDetail(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("detail", memoService.selectMemoDetail(cmmUtil.convertHtml(request, "reportId")));
		return "memo/memoDetail";
	}

	@Operation(summary = "메모보고 등록")
	@RequestMapping(value = "/insertMemo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertMemo(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_MEMO");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String title = cmmUtil.convertHtml(request, "title");
			String reporter = cmmUtil.convertHtml(request, "reporter");
			String receiver = cmmUtil.convertHtml(request, "receiver");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			if (reporter == null || reporter.trim().isEmpty() || receiver == null || receiver.trim().isEmpty()) {
				throw new IllegalArgumentException("보고자와 대상자를 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("reportId", "MR" + CommUtil.getFileId());
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("reporter", reporter.trim());
			param.put("receiver", receiver.trim());
			memoService.insertMemo(param);
			result.put("success", true);
			result.put("reportId", param.get("reportId"));
		} catch (Exception e) {
			logger.error("insertMemo failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "메모보고 수정 (보고 상태에서만)")
	@RequestMapping(value = "/updateMemo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateMemo(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("reportId", cmmUtil.convertHtml(request, "reportId"));
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			memoService.updateMemo(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateMemo failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "메모보고 삭제")
	@RequestMapping(value = "/deleteMemo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteMemo(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			memoService.deleteMemo(cmmUtil.convertHtml(request, "reportId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteMemo failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "메모보고 확인 처리")
	@RequestMapping(value = "/confirmMemo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> confirmMemo(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			memoService.confirmMemo(cmmUtil.convertHtml(request, "reportId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("confirmMemo failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "메모보고 의견 등록")
	@RequestMapping(value = "/opinionMemo.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> opinionMemo(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String opinion = request.getParameter("opinion") == null ? "" : request.getParameter("opinion");
			if (opinion.trim().isEmpty()) {
				throw new IllegalArgumentException("의견을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("reportId", cmmUtil.convertHtml(request, "reportId"));
			param.put("opinion", htmlSanitizer.sanitizeBoardContent(opinion));
			memoService.opinionMemo(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("opinionMemo failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
