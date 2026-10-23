package arkive.admin.board.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import arkive.admin.auth.service.PermissionService;
import arkive.admin.auth.web.LoginController;
import arkive.admin.board.service.BoardService;
import arkive.admin.board.service.BoardSseService;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.HtmlSanitizer;
import arkive.admin.mail.service.MailAsyncService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 게시판(CK/토스트 2종). 같은 BOARD 테이블을 BOARD_TYPE으로 분리한다.
 * 첨부는 기존 공통 업로드(tempFileUpload + saveTempFiles, BOARD_ 그룹)를 재사용한다.
 * 근거: eGov v5.0 cop 게시판(통합게시판) 목록·상세·등록 패턴. CK/토스트 듀얼 에디터 구성은 Arkive-local이다.
 */
@Tag(name = "게시판", description = "CK/토스트 목록·상세·등록·수정·삭제, 에디터 이미지 업로드")
@Controller
@RequestMapping("/board")
public class BoardController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "fileService")
	private FileService fileService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	@Resource
	private BoardSseService boardSseService;

	@Resource(name = "permissionService")
	private PermissionService permissionService;

	@Resource(name = "mailAsyncService")
	private MailAsyncService mailAsyncService;

	private CommUtil cmmUtil = new CommUtil();

	/**
	 * 세션에 본 글 ID 집합을 꺼낸다. 없으면 만들어 세션에 넣는다.
	 */
	@SuppressWarnings("unchecked")
	private Set<String> getViewedSet(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Object attr = session.getAttribute("viewedBoards");
		if (attr instanceof Set) {
			return (Set<String>) attr;
		}
		Set<String> viewed = new HashSet<>();
		session.setAttribute("viewedBoards", viewed);
		return viewed;
	}

	/**
	 * CK 오프셋 목록. 근거: eGov PaginationInfo 표준(페이지당 10건·블록 10개).
	 */
	private void putBoardList(String boardType, int pageUnit, int pageSize, HttpServletRequest request, ModelMap model) throws Exception {
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
		paginationInfo.setRecordCountPerPage(pageUnit);
		paginationInfo.setPageSize(pageSize);

		EgovMap param = new EgovMap();
		param.put("boardType", boardType);
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("boardType", boardType);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", boardService.selectBoardList(param));
		paginationInfo.setTotalRecordCount(boardService.selectBoardListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
	}

	@Operation(summary = "CK 게시판 목록 (전자정부 오프셋 페이징)")
	@RequestMapping(value = "/ckeditorList", method = { RequestMethod.GET, RequestMethod.POST })
	public String ckeditorList(HttpServletRequest request, ModelMap model) throws Exception {
		// CK 게시판 페이징: 페이지당 10건, 페이지 블록 10개
		putBoardList("CK", 10, 10, request, model);
		return "board/boardList";
	}

	@Operation(summary = "토스트 게시판 목록 껍데기 (목록은 toastList.json 커서 방식)")
	@RequestMapping(value = "/toastList", method = { RequestMethod.GET, RequestMethod.POST })
	public String toastList(HttpServletRequest request, ModelMap model) throws Exception {
		// 토스트는 커서(키셋)+더보기 방식이라 첫 화면은 껍데기만 내려보내고 목록은 toastList.json으로 채운다.
		model.put("boardType", "TOAST");
		model.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		return "board/boardList";
	}

	/**
	 * 토스트 커서 목록(JSON). cursorId(마지막 글 BOARD_ID) 다음 묶음을 pageSize만큼 돌려준다.
	 * limit = pageSize + 1 로 조회해 여분이 있으면 hasNext=true, nextCursorId는 마지막 실데이터 키다.
	 * 근거: 키셋 페이지네이션. 오프셋의 뒤쪽 페이지 느려짐·삽입 시 중복 문제를 피한다(Arkive-local 개선).
	 */
	@Operation(summary = "토스트 커서 목록 (cursorId 다음 묶음, hasNext/nextCursorId 반환)")
	@RequestMapping(value = "/toastList.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> toastListJson(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			int pageSize = 10;
			EgovMap param = new EgovMap();
			param.put("boardType", "TOAST");
			param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
			param.put("cursorId", cmmUtil.convertHtml(request, "cursorId"));
			param.put("limit", pageSize + 1);
			List<EgovMap> rows = boardService.selectBoardListByCursor(param);
			boolean hasNext = rows.size() > pageSize;
			if (hasNext) {
				rows = rows.subList(0, pageSize);
			}
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			List<Map<String, Object>> list = new ArrayList<>();
			for (EgovMap r : rows) {
				Map<String, Object> item = new HashMap<>();
				item.put("boardId", String.valueOf(r.get("boardId")));
				item.put("title", String.valueOf(r.get("title")));
				item.put("importantYn", "Y".equals(String.valueOf(r.get("importantYn"))) ? "Y" : "N");
				item.put("viewCnt", r.get("viewCnt"));
				Object dt = r.get("registDt");
				item.put("registDt", dt instanceof Date ? fmt.format((Date) dt) : "");
				list.add(item);
			}
			result.put("success", true);
			result.put("list", list);
			result.put("hasNext", hasNext);
			result.put("nextCursorId", list.isEmpty() ? "" : list.get(list.size() - 1).get("boardId"));
		} catch (Exception e) {
			logger.error("toastListJson failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "CK 글쓰기 화면 (boardId 있으면 수정)")
	@RequestMapping(value = "/ckeditorWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String ckeditorWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		if (boardId != null && !boardId.isEmpty()) {
			EgovMap detail = boardService.selectBoardDetail(boardId);
			model.put("detail", detail);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
			}
		}
		model.put("boardType", "CK");
		// 중요글 지정은 메일 알림을 유발하므로 관리자에게만 노출한다. 서버에서도 다시 검사한다.
		model.put("isAdmin", isAdmin(request));
		// 이중등록방지 토큰 발급. CK/TOAST 화면 동시 사용 대비 key 분리.
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_CK"));
		return "board/boardWriteCk";
	}

	@Operation(summary = "토스트 글쓰기 화면 (boardId 있으면 수정)")
	@RequestMapping(value = "/toastWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String toastWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		if (boardId != null && !boardId.isEmpty()) {
			EgovMap detail = boardService.selectBoardDetail(boardId);
			model.put("detail", detail);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
			}
		}
		model.put("boardType", "TOAST");
		// 중요글 지정은 메일 알림을 유발하므로 관리자에게만 노출한다. 서버에서도 다시 검사한다.
		model.put("isAdmin", isAdmin(request));
		// 이중등록방지 토큰 발급. CK/TOAST 화면 동시 사용 대비 key 분리.
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_TOAST"));
		return "board/boardWriteToast";
	}

	@Operation(summary = "게시글 상세 (조회수 증가, 이전글/다음글 포함). boardId 파라미터로 조회한다")
	@RequestMapping(value = "/detail", method = { RequestMethod.GET, RequestMethod.POST })
	public String detail(HttpServletRequest request, ModelMap model) throws Exception {
		// 근거: 동일 BOARD_TYPE 내 REGIST_DT·BOARD_ID 기준 이전/다음 1건. 조회수는 세션 집합으로 중복 방지한다.
		String boardId = cmmUtil.convertHtml(request, "boardId");
		// 조회수 중복 방지. 세션에 본 글 ID를 모아두고 처음 볼 때만 올린다.
		// 새로고침·뒤로가기로 뻥튀기되는 것을 막는다. (세션 만료되면 다시 센다)
		Set<String> viewed = getViewedSet(request);
		if (boardId != null && !boardId.isEmpty() && !viewed.contains(boardId)) {
			boardService.updateViewCnt(boardId);
			viewed.add(boardId);
		}
		EgovMap detail = boardService.selectBoardDetail(boardId);
		model.put("detail", detail);
		if (detail != null) {
			EgovMap neighborParam = new EgovMap();
			neighborParam.put("boardType", detail.get("boardType"));
			neighborParam.put("boardId", boardId);
			neighborParam.put("registDt", detail.get("registDt"));
			model.put("prev", boardService.selectPrevOne(neighborParam));
			model.put("next", boardService.selectNextOne(neighborParam));
			model.put("commentList", boardService.selectCommentList(boardId));
			model.put("commentCount", boardService.selectCommentCount(boardId));
		}
		if (detail != null && detail.get("atchFileGrpid") != null) {
			model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
		}
		return "board/boardDetail";
	}

	@Operation(summary = "게시글 등록 (제목 필수, 본문 HTML 그대로 저장)")
	@RequestMapping(value = "/insertBoard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertBoard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 제목은 HTML 이스케이프, 본문은 에디터 HTML 그대로 저장한다.
			// 운영 반영 전 lucy-xss 등 서버 sanitize를 추가한다.
			String boardType = cmmUtil.convertHtml(request, "boardType");
			if (!"CK".equals(boardType) && !"TOAST".equals(boardType)) {
				throw new IllegalArgumentException("boardType이 올바르지 않습니다.");
			}
			// 이중등록방지 검사. 글쓰기 화면 tokenKey와 쌍을 맞춘다.
			String tokenKey = "CK".equals(boardType) ? "BOARD_CK" : "BOARD_TOAST";
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken(tokenKey);
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("boardId", "BD" + CommUtil.getFileId());
			param.put("boardType", boardType);
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			param.put("title", title.trim());
			String importantYn = cmmUtil.convertHtml(request, "importantYn");
			if ("Y".equals(importantYn) && !isAdmin(request)) {
				throw new IllegalStateException("관리자만 중요글을 지정할 수 있습니다.");
			}
			param.put("importantYn", "Y".equals(importantYn) ? "Y" : "N");
			// XSS 정제. 에디터 HTML은 살리되 script·비디오외 iframe·이벤트 속성을 제거한다.
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			// 글 첨부는 사용하지 않는다. 에디터 본문 이미지는 editorImageUpload.json으로 별도 저장된다.
			// 빈 값이면 그룹을 만들지 않고 NULL로 둔다.
			if (grpid == null || grpid.isEmpty() || "undefined".equalsIgnoreCase(grpid) || "null".equalsIgnoreCase(grpid)) {
				grpid = null;
			} else if (!grpid.startsWith("BOARD_")) {
				grpid = "BOARD_" + grpid;
			}
			param.put("atchFileGrpid", grpid);
			param.put("register", "SYSTEM");
			boardService.insertBoard(param);
			result.put("success", true);
			result.put("boardId", param.get("boardId"));
			result.put("boardType", boardType);
			// 새 글 알림. 해당 게시판 메뉴를 조회할 수 있는 역할의 구독자에게만 서버에서 필터해 보낸다.
			try {
				String menuId = "CK".equals(boardType) ? "ME02030100" : "ME02030200";
				Set<String> targetRoles = permissionService.selectRoleIdsWithMenuRead(menuId);
				boardSseService.broadcast(boardType, String.valueOf(param.get("boardId")),
						String.valueOf(param.get("title")), targetRoles);
			} catch (Exception e) {
				// 알림 실패가 게시글 등록 실패로 번지지 않게 분리한다.
				logger.warn("board SSE broadcast skipped", e);
			}
			if ("Y".equals(importantYn)) {
				try {
					mailAsyncService.sendBoardNotification(String.valueOf(param.get("boardId")));
				} catch (Exception e) {
					logger.warn("board mail dispatch skipped", e);
				}
			}
		} catch (Exception e) {
			logger.error("insertBoard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "게시글 수정")
	@RequestMapping(value = "/updateBoard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateBoard(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			String boardId = cmmUtil.convertHtml(request, "boardId");
			EgovMap before = boardService.selectBoardDetail(boardId);
			if (before == null) {
				throw new IllegalArgumentException("게시글이 없습니다.");
			}
			param.put("boardId", boardId);
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			param.put("title", title.trim());
			// 중요글은 메일 알림을 유발하므로 관리자만 변경한다. 일반 사용자의 수정 요청은
			// 기존 값을 그대로 보존해 권한 상승을 막는다.
			String importantYn = isAdmin(request)
					? ("Y".equals(cmmUtil.convertHtml(request, "importantYn")) ? "Y" : "N")
					: ("Y".equals(String.valueOf(before.get("importantYn"))) ? "Y" : "N");
			param.put("importantYn", importantYn);
			// XSS 정제. 에디터 HTML은 살리되 script·비디오외 iframe·이벤트 속성을 제거한다.
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				grpid = before.get("atchFileGrpid") == null ? null
						: String.valueOf(before.get("atchFileGrpid"));
			}
			param.put("atchFileGrpid", grpid);
			param.put("updusr", "SYSTEM");
			boardService.updateBoard(param);
			if ("Y".equals(importantYn) && !"Y".equals(String.valueOf(before.get("importantYn")))) {
				try {
					mailAsyncService.sendBoardNotification(boardId);
				} catch (Exception e) {
					logger.warn("board mail dispatch skipped", e);
				}
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateBoard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "공지 고정/해제 (관리자, CK 공지사항만)")
	@RequestMapping(value = "/updateNotice.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateNotice(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 목록 버튼은 관리자 화면에서만 보이지만, 서버에서 권한을 다시 확인한다.
			if (!isAdmin(request)) {
				throw new IllegalStateException("관리자만 고정할 수 있습니다.");
			}
			String boardId = cmmUtil.convertHtml(request, "boardId");
			String noticeYn = cmmUtil.convertHtml(request, "noticeYn");
			if (boardId == null || boardId.isEmpty()) {
				throw new IllegalArgumentException("게시글 ID가 없습니다.");
			}
			if (!"Y".equals(noticeYn) && !"N".equals(noticeYn)) {
				throw new IllegalArgumentException("고정 값이 올바르지 않습니다.");
			}
			EgovMap detail = boardService.selectBoardDetail(boardId);
			if (detail == null) {
				throw new IllegalArgumentException("게시글이 없습니다.");
			}
			// 공지 고정은 CK(공지사항) 목록에만 적용한다. 자료실(TOAST)은 커서 목록이라 제외한다.
			if (!"CK".equals(String.valueOf(detail.get("boardType")))) {
				throw new IllegalArgumentException("공지사항만 고정할 수 있습니다.");
			}
			EgovMap param = new EgovMap();
			param.put("boardId", boardId);
			param.put("noticeYn", noticeYn);
			param.put("updusr", "SYSTEM");
			if (boardService.updateNoticeYn(param) == 0) {
				throw new IllegalStateException("고정 상태를 변경하지 못했습니다.");
			}
			if ("Y".equals(noticeYn)) {
				try {
					mailAsyncService.sendBoardNotification(boardId);
				} catch (Exception e) {
					logger.warn("board mail dispatch skipped", e);
				}
			}
			result.put("success", true);
			result.put("noticeYn", noticeYn);
		} catch (Exception e) {
			logger.error("updateNotice failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "게시글 삭제 (본문 에디터 이미지도 함께 삭제)")
	@RequestMapping(value = "/deleteBoard.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteBoard(@RequestParam Map<String, Object> map, HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String boardId = cmmUtil.convertHtml(request, "boardId");
			EgovMap detail = boardService.selectBoardDetail(boardId);
			if (detail != null && detail.get("content") != null) {
				// 본문 HTML 속 에디터 이미지(file/downloadFile.do?downloadFileId=...)도 함께 삭제한다.
				// 다른 글에서 같은 이미지를 참조하면 함께 안 보이게 되므로 복붙 공유는 운영상 자제한다.
				String content = String.valueOf(detail.get("content"));
				Matcher m = Pattern
						.compile("downloadFileId=([A-Za-z0-9_\\-]+)").matcher(content);
				Set<String> seen = new HashSet<>();
				while (m.find()) {
					if (seen.add(m.group(1))) {
						try {
							fileService.setDeleteFile(m.group(1));
						} catch (Exception ex) {
							logger.warn("editor image delete skipped. fileId={}", m.group(1));
						}
					}
				}
			}
			boardService.deleteBoard(boardId);
			// 게시글 첨부도 함께 삭제한다.
			if (detail != null && detail.get("atchFileGrpid") != null) {
				for (EgovMap f : fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid")))) {
					fileService.setDeleteFile(String.valueOf(f.get("fileId")));
				}
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteBoard failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 세션 로그인 사용자. 없으면 null.
	 */
	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	/**
	 * 관리자 세션 여부. 댓글 삭제 권한 판단에만 쓴다.
	 */
	private boolean isAdmin(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute(LoginController.ADMIN_KEY) != null;
	}

	@Operation(summary = "댓글 등록 (로그인 사용자, 1단)")
	@RequestMapping(value = "/insertComment.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertComment(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String boardId = cmmUtil.convertHtml(request, "boardId");
			if (boardId == null || boardId.isEmpty() || boardService.selectBoardDetail(boardId) == null) {
				throw new IllegalArgumentException("게시글이 없습니다.");
			}
			// 댓글은 평문으로 저장하고 화면에서 th:text로 이스케이프한다.
			String content = cmmUtil.convertHtml(request, "content");
			if (content == null || content.trim().isEmpty()) {
				throw new IllegalArgumentException("댓글 내용을 입력하세요.");
			}
			content = content.trim();
			if (content.length() > 1000) {
				throw new IllegalArgumentException("댓글은 1000자 이내로 입력하세요.");
			}
			String userId = String.valueOf(loginUser.get("userId"));
			String userNm = loginUser.get("userNm") == null ? userId : String.valueOf(loginUser.get("userNm"));
			EgovMap param = new EgovMap();
			param.put("commentId", "CM" + CommUtil.getFileId());
			param.put("boardId", boardId);
			param.put("content", content);
			param.put("register", userId);
			param.put("registerNm", userNm);
			boardService.insertComment(param);
			result.put("success", true);
			result.put("commentId", param.get("commentId"));
		} catch (Exception e) {
			logger.error("insertComment failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "댓글 삭제 (작성자 또는 관리자)")
	@RequestMapping(value = "/deleteComment.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteComment(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String commentId = cmmUtil.convertHtml(request, "commentId");
			if (commentId == null || commentId.isEmpty()) {
				throw new IllegalArgumentException("댓글 ID가 없습니다.");
			}
			EgovMap comment = boardService.selectCommentDetail(commentId);
			if (comment == null) {
				throw new IllegalArgumentException("댓글이 없습니다.");
			}
			boolean owner = String.valueOf(loginUser.get("userId")).equals(String.valueOf(comment.get("register")));
			if (!owner && !isAdmin(request)) {
				throw new IllegalStateException("삭제 권한이 없습니다.");
			}
			boardService.deleteComment(commentId);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteComment failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "새 글 알림 구독 (SSE, boardType별 30분 연결)")
	@RequestMapping(value = "/subscribe", method = RequestMethod.GET)
	@ResponseBody
	public SseEmitter subscribe(HttpServletRequest request) {
		String boardType = cmmUtil.convertHtml(request, "boardType");
		Set<String> roles = Collections.emptySet();
		EgovMap loginUser = getLoginUser(request);
		if (loginUser != null) {
			try {
				roles = new HashSet<>(
						permissionService.selectUserRoleIds(String.valueOf(loginUser.get("userId"))));
			} catch (Exception e) {
				logger.warn("board SSE role lookup failed", e);
			}
		}
		return boardSseService.subscribe(boardType, roles);
	}

	/**
	 * 에디터 본문 이미지 업로드(CK simpleUpload + 토스트 addImageBlobHook 공용).
	 * temp 저장 후 즉시 확정하고 인라인 표시 URL을 돌려준다.
	 */
	@Operation(summary = "에디터 본문 이미지 업로드 (upload 필드, {uploaded,url} 반환)")
	@RequestMapping(value = "/editorImageUpload.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> editorImageUpload(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			List<MultipartFile> files = new ArrayList<>();
			if (request instanceof MultipartHttpServletRequest multipart) {
				List<MultipartFile> upload = multipart.getFiles("upload");
				if (upload != null && !upload.isEmpty()) {
					files.addAll(upload);
				} else if (multipart.getMultiFileMap() != null) {
					multipart.getMultiFileMap().values().forEach(list -> {
						if (list != null) {
							files.addAll(list);
						}
					});
				}
			}
			if (files.isEmpty()) {
				throw new IllegalArgumentException("업로드할 파일이 없습니다.");
			}
			String groupId = "BOARD_EDITOR_" + UUID.randomUUID().toString();
			List<?> staged = fileService.setUploadFiles(files, groupId, "SYSTEM");
			List<Map<String, String>> stagedJson = new ArrayList<>();
			for (Object o : staged) {
				EgovMap m = (EgovMap) o;
				Map<String, String> item = new HashMap<>();
				item.put("fileId", String.valueOf(m.get("fileId")));
				item.put("fileName", String.valueOf(m.get("orgnlFileNm")));
				stagedJson.add(item);
			}
			List<EgovMap> saved = fileService.saveTempFiles(stagedJson, groupId, "SYSTEM");
			if (saved.isEmpty()) {
				throw new IllegalStateException("이미지 저장에 실패했습니다.");
			}
			String fileId = String.valueOf(saved.get(0).get("fileId"));
			String url = "/file/downloadFile.do?downloadFileId=" + fileId;
			// CKEditor5 simpleUpload 규격 + 토스트 공용 url
			result.put("uploaded", true);
			result.put("url", url);
			result.put("fileId", fileId);
		} catch (Exception e) {
			logger.error("editorImageUpload failed", e);
			result.put("uploaded", false);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
