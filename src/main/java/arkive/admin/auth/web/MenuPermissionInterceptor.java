package arkive.admin.auth.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import arkive.admin.auth.service.MenuPermission;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.board.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 메뉴 권한 서버 강제.
 *
 * <p>화면에서 메뉴를 숨기는 것만으로는 직접 URL 호출을 막을 수 없다.
 * 이 인터셉터가 요청을 컨트롤러→메뉴로 해석해 조회(READ)·쓰기(WRITE) 권한을 검사한다.
 * 컨트롤러마다 if문을 넣지 않고 매핑을 이 클래스 한 곳에 모은다.
 *
 * <p>정책:
 * <ul>
 *   <li>ADMIN_YN='Y' 슈퍼관리자는 통과한다.</li>
 *   <li>역할이 없는 사용자는 메뉴 권한이 없다(기본 거부).</li>
 *   <li>여러 역할은 메뉴별 OR로 합친다.</li>
 *   <li>GET/HEAD는 READ, 쓰기 성격의 POST는 READ+WRITE를 요구한다.</li>
 * </ul>
 *
 * <p>게시판은 URL만으로 CK/TOAST를 구분할 수 없는 경우가 있어
 * boardType 파라미터 또는 boardId의 실제 게시판 타입을 확인한다.
 */
public class MenuPermissionInterceptor implements HandlerInterceptor {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CK_MENU = "ME02030100";
	private static final String TOAST_MENU = "ME02030200";
	private static final String SCHEDULE_MENU = "ME02030800";
	private static final String HOLIDAY_MENU = "ME02030900";
	private static final String LEAVE_MY_MENU = "ME02090100";
	private static final String LEAVE_MANAGE_MENU = "ME02090200";
	private static final String LEAVE_BALANCE_MENU = "ME02090300";
	private static final String MEETING_ROOM_MENU = "ME02050100";
	private static final String MEETING_RESERVE_MENU = "ME02050200";
	private static final String APPROVAL_MENU = "ME02070100";
	private static final String HR_ANNIVERSARY_MENU = "ME02090400";
	private static final String HR_EVENT_MENU = "ME02090500";
	private static final String HR_REWARD_MENU = "ME02090600";
	private static final String HR_PERSONAL_LEAVE_MENU = "ME02090700";
	private static final String SURVEY_TEMPLATE_MENU = "ME02031600";
	private static final String SURVEY_MANAGE_MENU = "ME02031700";
	private static final String SURVEY_PARTICIPATE_MENU = "ME02031800";
	private static final String SURVEY_STATS_MENU = "ME02031900";
	private static final String LECTURE_MANAGE_MENU = "ME020A0100";
	private static final String LECTURE_MY_MENU = "ME020A0200";
	private static final String LECTURE_COMPLETE_MENU = "ME020A0300";
	private static final String MAP_GUIDE_MENU = "ME02040200";
	private static final String CHAT_MENU = "ME020B0100";
	private static final String BATCH_MENU = "ME02010800";

	/** 컨트롤러 → 메뉴 ID. 메뉴가 없는 컨트롤러(쪽지·파일·사용자포털 등)는 매핑하지 않는다. */
	private static final Map<String, String> CONTROLLER_MENU = Map.ofEntries(
			Map.entry("arkive.admin.faq.web.FaqController", "ME02030300"),
			Map.entry("arkive.admin.qna.web.QnaController", "ME02030400"),
			Map.entry("arkive.admin.poll.web.PollController", "ME02031200"),
			Map.entry("arkive.admin.counsel.web.CounselController", "ME02031500"),
			Map.entry("arkive.admin.popup.web.PopupController", "ME02030600"),
			Map.entry("arkive.admin.banner.web.BannerController", "ME02030700"),
			Map.entry("arkive.admin.schedule.web.ScheduleController", SCHEDULE_MENU),
			Map.entry("arkive.admin.duty.web.DutyController", "ME02031300"),
			Map.entry("arkive.admin.address.web.AddressController", "ME02031100"),
			Map.entry("arkive.admin.card.web.CardController", "ME02031400"),
			Map.entry("arkive.admin.category.web.CategoryController", "ME02031000"),
			Map.entry("arkive.admin.memo.web.MemoController", "ME02030500"),
			Map.entry("arkive.admin.system.web.SystemController", "ME02010100"),
			Map.entry("arkive.admin.system.web.CommCodeController", "ME02010200"),
			Map.entry("arkive.admin.auth.web.UserManageController", "ME02010400"),
			Map.entry("arkive.admin.auth.web.RoleManageController", "ME02010500"),
			Map.entry("arkive.admin.audit.web.AuditLogController", "ME02010600"),
			Map.entry("arkive.admin.stats.web.StatsController", "ME02010700"),
			Map.entry("arkive.admin.approval.web.ApprovalController", APPROVAL_MENU),
			Map.entry("arkive.admin.map.web.MapController", MAP_GUIDE_MENU),
			Map.entry("arkive.admin.chat.web.ChatController", CHAT_MENU),
			Map.entry("arkive.admin.batch.web.BatchController", BATCH_MENU));

	private final PermissionService permissionService;
	private final BoardService boardService;

	public MenuPermissionInterceptor(PermissionService permissionService, BoardService boardService) {
		this.permissionService = permissionService;
		this.boardService = boardService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}
		String path = path(request);
		if (isExempt(path)) {
			return true;
		}

		Target target = resolve(handlerMethod, request, path);
		if (target == null) {
			return true;
		}

		HttpSession session = request.getSession(false);
		if (session == null) {
			return true; // 로그인 검사는 AuthInterceptor가 담당한다.
		}
		if (session.getAttribute(LoginController.ADMIN_KEY) != null) {
			return true;
		}
		Object rawUser = session.getAttribute(LoginController.SESSION_KEY);
		if (!(rawUser instanceof EgovMap)) {
			return true; // 로그인 검사는 AuthInterceptor가 담당한다.
		}
		String userId = String.valueOf(((EgovMap) rawUser).get("userId"));
		Map<String, MenuPermission> permissions = permissionService.selectUserMenuPermissions(userId);
		for (String menuId : target.menuIds()) {
			MenuPermission permission = permissions.get(menuId);
			if (permission != null && permission.read() && (!target.write() || permission.write())) {
				return true;
			}
		}
		deny(request, response, target);
		return false;
	}

	private void deny(HttpServletRequest request, HttpServletResponse response, Target target) throws IOException {
		logger.info("menu permission denied. user={}, menu={}, write={}, uri={}",
				request.getSession(false) == null ? "" : request.getSession(false).getAttribute(LoginController.SESSION_KEY),
				target.menuIds(), target.write(), path(request));
		if (wantsJson(request)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"message\":\"권한이 없습니다.\"}");
			return;
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	private boolean wantsJson(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri != null && uri.endsWith(".json")) {
			return true;
		}
		if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
			return true;
		}
		String accept = request.getHeader("Accept");
		return accept != null && accept.contains("application/json");
	}

	/**
	 * 요청을 메뉴 ID 후보와 READ/WRITE로 해석한다.
	 * null이면 이 인터셉터가 관리하지 않는 요청이다.
	 */
	private Target resolve(HandlerMethod handler, HttpServletRequest request, String path) throws Exception {
		String className = handler.getBeanType().getName();
		if ("arkive.admin.board.web.BoardController".equals(className)) {
			return resolveBoard(handler, request, path);
		}
		if ("arkive.admin.leave.web.LeaveController".equals(className)) {
			return resolveLeave(handler, request, path);
		}
		if ("arkive.admin.meeting.web.MeetingController".equals(className)) {
			String lower = path.toLowerCase();
			String menuId = lower.contains("room") ? MEETING_ROOM_MENU : MEETING_RESERVE_MENU;
			return new Target(List.of(menuId), isWrite(handler, request));
		}
		if ("arkive.admin.hr.web.HrController".equals(className)) {
			String lower = path.toLowerCase();
			String menuId;
			if (lower.contains("anniversary")) {
				menuId = HR_ANNIVERSARY_MENU;
			} else if (lower.contains("event")) {
				menuId = HR_EVENT_MENU;
			} else if (lower.contains("reward")) {
				menuId = HR_REWARD_MENU;
			} else {
				menuId = HR_PERSONAL_LEAVE_MENU;
			}
			return new Target(List.of(menuId), isWrite(handler, request));
		}
		if ("arkive.admin.survey.web.SurveyController".equals(className)) {
			String lower = path.toLowerCase();
			String menuId;
			if (lower.contains("template")) {
				menuId = SURVEY_TEMPLATE_MENU;
			} else if (lower.contains("stats")) {
				menuId = SURVEY_STATS_MENU;
			} else if (lower.contains("surveylist") || lower.contains("surveywrite") || lower.contains("question")
					|| lower.contains("insertsurvey") || lower.contains("updatesurvey")
					|| lower.contains("deletesurvey") || lower.contains("opensurvey") || lower.contains("closesurvey")) {
				menuId = SURVEY_MANAGE_MENU;
			} else {
				menuId = SURVEY_PARTICIPATE_MENU;
			}
			return new Target(List.of(menuId), isWrite(handler, request));
		}
		if ("arkive.admin.lecture.web.LectureController".equals(className)) {
			String lower = path.toLowerCase();
			String menuId;
			if (lower.contains("mylecture") || lower.contains("view") || lower.contains("progress")
					|| lower.contains("stream")) {
				menuId = LECTURE_MY_MENU;
			} else if (lower.contains("completion")) {
				menuId = LECTURE_COMPLETE_MENU;
			} else {
				menuId = LECTURE_MANAGE_MENU;
			}
			return new Target(List.of(menuId), isWrite(handler, request));
		}
		String menuId = CONTROLLER_MENU.get(className);
		if (menuId == null) {
			return null;
		}
		if ("arkive.admin.schedule.web.ScheduleController".equals(className)
				&& (path.toLowerCase().contains("holiday")
						|| handler.getMethod().getName().toLowerCase().contains("holiday"))) {
			menuId = HOLIDAY_MENU;
		}
		return new Target(List.of(menuId), isWrite(handler, request));
	}

	/**
	 * 휴가관리는 한 컨트롤러가 내 휴가·승인·연차 화면을 모두 담당해 URL로 메뉴를 나눈다.
	 * 일반 역할은 "내 휴가"만 받고, 승인·연차 화면은 해당 메뉴 WRITE 권한이 필요하다.
	 */
	private Target resolveLeave(HandlerMethod handler, HttpServletRequest request, String path) {
		String lower = path.toLowerCase();
		String menuId;
		if (lower.contains("balance")) {
			menuId = LEAVE_BALANCE_MENU;
		} else if (lower.contains("leavelist") || lower.contains("approveleave") || lower.contains("rejectleave")) {
			menuId = LEAVE_MANAGE_MENU;
		} else {
			menuId = LEAVE_MY_MENU;
		}
		// 내 휴가 화면은 연도 검색에 POST를 쓰지만 조회 기능이므로 WRITE를 요구하지 않는다.
		boolean write = isWrite(handler, request) && !lower.contains("myleave");
		return new Target(List.of(menuId), write);
	}

	private Target resolveBoard(HandlerMethod handler, HttpServletRequest request, String path) throws Exception {
		if (path.endsWith("/subscribe")) {
			return null; // SSE 통로는 역할을 저장해 broadcast에서 필터한다.
		}
		boolean write = isWrite(handler, request);
		String lower = path.toLowerCase();
		String menuId = null;
		if (lower.contains("toast")) {
			menuId = TOAST_MENU;
		} else if (lower.contains("ckeditor")) {
			menuId = CK_MENU;
		}
		if (menuId == null) {
			String boardType = trimToEmpty(request.getParameter("boardType")).toUpperCase();
			if (!"CK".equals(boardType) && !"TOAST".equals(boardType)) {
				String boardId = trimToEmpty(request.getParameter("boardId"));
				if (!boardId.isEmpty()) {
					EgovMap detail = boardService.selectBoardDetail(boardId);
					boardType = detail == null ? "" : trimToEmpty(String.valueOf(detail.get("boardType"))).toUpperCase();
				}
			}
			if ("CK".equals(boardType)) {
				menuId = CK_MENU;
			} else if ("TOAST".equals(boardType)) {
				menuId = TOAST_MENU;
			}
		}
		if (menuId == null) {
			// 에디터 이미지 업로드처럼 CK/TOAST 공용 요청은 둘 중 하나의 쓰기 권한이면 허용한다.
			return new Target(List.of(CK_MENU, TOAST_MENU), write);
		}
		return new Target(List.of(menuId), write);
	}

	/**
	 * GET은 조회. POST라도 목록·상세·조회 성격의 핸들러는 조회로 본다.
	 * (화면 검색 폼이 POST를 쓰는 기존 방식을 유지한다)
	 */
	private boolean isWrite(HandlerMethod handler, HttpServletRequest request) {
		String method = request.getMethod();
		if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)
				&& !"PATCH".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
			return false;
		}
		String name = handler.getMethod().getName().toLowerCase();
		return !(name.startsWith("get") || name.startsWith("select") || name.startsWith("find")
				|| name.startsWith("active") || name.startsWith("can") || name.startsWith("download")
				|| name.contains("list") || name.contains("detail") || name.contains("view")
				|| name.contains("search") || name.contains("tree"));
	}

	private boolean isExempt(String path) {
		// 로그인 사용자 공통 기능(댓글·쪽지·다운로드·SSE)은 메뉴 CRUD 권한과 분리한다.
		return path.equals("/board/insertComment.json")
				|| path.equals("/board/deleteComment.json")
				|| path.equals("/board/updateNotice.json")
				|| path.equals("/board/subscribe")
				|| path.startsWith("/message/")
				|| path.startsWith("/file/")
				|| path.startsWith("/preview/")
				|| path.equals("/popup/activePopup.json")
				|| path.equals("/banner/activeBanner.json");
	}

	private String path(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		return uri != null && uri.startsWith(context) ? uri.substring(context.length()) : uri;
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	private record Target(List<String> menuIds, boolean write) {
	}
}
