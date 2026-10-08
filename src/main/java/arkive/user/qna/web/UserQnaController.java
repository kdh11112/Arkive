package arkive.user.qna.web;

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
import arkive.admin.qna.service.QnaService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 외부 Q&A. 목록·상세·질문등록만 제공한다. 수정·삭제·답변은 내부(관리)에서 한다.
 */
@Tag(name = "사용자 Q&A", description = "Q&A 목록·상세·질문등록")
@Controller
@RequestMapping("/user/qna")
public class UserQnaController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "qnaService")
	private QnaService qnaService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "외부 Q&A 목록")
	@RequestMapping(value = "/qnaList", method = { RequestMethod.GET, RequestMethod.POST })
	public String qnaList(HttpServletRequest request, ModelMap model) throws Exception {
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
		model.put("resultList", qnaService.selectQnaList(param));
		paginationInfo.setTotalRecordCount(qnaService.selectQnaListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "user/qna/qnaList";
	}

	@Operation(summary = "외부 Q&A 상세 (답변 읽기 전용)")
	@RequestMapping(value = "/qnaDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String qnaDetail(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		model.put("detail", qnaService.selectQnaDetail(boardId));
		model.put("answer", qnaService.selectQnaAnswer(boardId));
		return "user/qna/qnaDetail";
	}

	@Operation(summary = "외부 Q&A 질문쓰기 화면")
	@RequestMapping(value = "/qnaWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String qnaWrite(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "USER_QNA"));
		return "user/qna/qnaWrite";
	}

	@Operation(summary = "외부 Q&A 질문 등록 (작성자 미입력 시 익명)")
	@RequestMapping(value = "/insertQna.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertQna(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("USER_QNA");
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
			String register = cmmUtil.convertHtml(request, "register");
			if (register == null || register.trim().isEmpty()) {
				register = "익명";
			}
			EgovMap param = new EgovMap();
			param.put("boardId", "BD" + CommUtil.getFileId());
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("register", register.trim());
			qnaService.insertQna(param);
			result.put("success", true);
			result.put("boardId", param.get("boardId"));
		} catch (Exception e) {
			logger.error("insertQna failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
