package arkive.admin.qna.web;

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
 * Q&A관리. 질문(BOARD_TYPE='QNA') 등록과 답변(QNA_ANSWER) 등록을 분리한다.
 * 참고: BLMS qnaList(bbsId=03)+qnaDetail/qnaReg, pony QUESTION(questionMapper.xml)+ANSWER 분리.
 * 1단계(비로그인) 제약: 작성자는 입력값(기본 익명), 답변 권한 미분리(2단계 권한관리에서 분리 예정),
 * 공개여부·비밀번호 조건 제외(BLMS rlsYnCd·checkPwd.json 생략).
 */
@Tag(name = "Q&A", description = "Q&A 목록·질문 등록·수정·삭제, 관리자 답변 등록")
@Controller
@RequestMapping("/qna")
public class QnaController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "qnaService")
	private QnaService qnaService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "Q&A 목록 (제목 검색 + 오프셋 10건 페이징, 상태 표시)")
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
		return "qna/qnaList";
	}

	@Operation(summary = "Q&A 질문쓰기 화면 (boardId 있으면 수정)")
	@RequestMapping(value = "/qnaWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String qnaWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		if (boardId != null && !boardId.isEmpty()) {
			model.put("detail", qnaService.selectQnaDetail(boardId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_QNA"));
		return "qna/qnaWrite";
	}

	@Operation(summary = "Q&A 상세 (질문 + 답변, 답변 폼 포함)")
	@RequestMapping(value = "/qnaDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String qnaDetail(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		model.put("detail", qnaService.selectQnaDetail(boardId));
		model.put("answer", qnaService.selectQnaAnswer(boardId));
		model.put("answerToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_QNA_ANS"));
		return "qna/qnaDetail";
	}

	@Operation(summary = "Q&A 질문 등록 (제목 필수, 작성자 미입력 시 익명)")
	@RequestMapping(value = "/insertQna.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertQna(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_QNA");
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

	@Operation(summary = "Q&A 질문 수정")
	@RequestMapping(value = "/updateQna.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateQna(HttpServletRequest request) {
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
			qnaService.updateQna(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateQna failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "Q&A 질문 삭제 (답변 CASCADE)")
	@RequestMapping(value = "/deleteQna.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteQna(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			qnaService.deleteQna(cmmUtil.convertHtml(request, "boardId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteQna failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "Q&A 답변 저장 (등록·수정 겸용)")
	@RequestMapping(value = "/saveAnswer.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveAnswer(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_QNA_ANS");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 상세 화면을 새로고침하세요.");
			}
			String boardId = cmmUtil.convertHtml(request, "boardId");
			if (boardId == null || boardId.isEmpty()) {
				throw new IllegalArgumentException("질문 정보가 없습니다.");
			}
			String answer = request.getParameter("answerContent") == null ? "" : request.getParameter("answerContent");
			if (answer.trim().isEmpty()) {
				throw new IllegalArgumentException("답변을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("boardId", boardId);
			param.put("answerContent", htmlSanitizer.sanitizeBoardContent(answer));
			param.put("answerer", "SYSTEM");
			qnaService.saveQnaAnswer(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("saveAnswer failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
