package arkive.admin.audit.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import arkive.admin.audit.service.AuditEvent;
import arkive.admin.auth.web.JwtAuthFilter;
import arkive.admin.auth.web.LoginController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 로그인 이후 메뉴·개인정보 열람·다운로드 접근을 감사 이벤트로 발행한다.
 *
 * <p>DB 저장은 리스너가 담당하고, 이 인터셉터는 요청 경로와 대상 식별자만 남긴다.
 * 쿼리스트링·본문·비밀번호는 저장하지 않는다.</p>
 */
public class AuditInterceptor implements HandlerInterceptor {

	private static final Map<String, String> MENU_BY_PREFIX = new LinkedHashMap<>();
	private static final Map<String, String> TARGET_BY_PREFIX = new LinkedHashMap<>();

	static {
		MENU_BY_PREFIX.put("/system/auditLogList", "ME02010600");
		MENU_BY_PREFIX.put("/stats/", "ME02010700");
		MENU_BY_PREFIX.put("/auth/user", "ME02010400");
		MENU_BY_PREFIX.put("/auth/role", "ME02010500");
		MENU_BY_PREFIX.put("/qna/", "ME02030400");
		MENU_BY_PREFIX.put("/faq/", "ME02030300");
		MENU_BY_PREFIX.put("/board/", "ME02030100");
		MENU_BY_PREFIX.put("/popup/", "ME02030600");
		MENU_BY_PREFIX.put("/banner/", "ME02030700");
		MENU_BY_PREFIX.put("/schedule/", "ME02030800");
		MENU_BY_PREFIX.put("/duty/", "ME02031300");
		MENU_BY_PREFIX.put("/address/", "ME02031100");
		MENU_BY_PREFIX.put("/card/", "ME02031400");
		MENU_BY_PREFIX.put("/counsel/", "ME02031500");
		MENU_BY_PREFIX.put("/memo/", "ME02030500");
		MENU_BY_PREFIX.put("/poll/", "ME02031200");

		TARGET_BY_PREFIX.put("/auth/user", "APP_USER");
		TARGET_BY_PREFIX.put("/auth/role", "APP_ROLE");
		TARGET_BY_PREFIX.put("/address/", "ADDRESS_BOOK");
		TARGET_BY_PREFIX.put("/card/", "BUSINESS_CARD");
		TARGET_BY_PREFIX.put("/counsel/", "COUNSEL");
		TARGET_BY_PREFIX.put("/memo/", "MEMO");
		TARGET_BY_PREFIX.put("/message/", "MESSAGE");
		TARGET_BY_PREFIX.put("/qna/", "QNA");
		TARGET_BY_PREFIX.put("/user/qna/", "QNA");
		TARGET_BY_PREFIX.put("/duty/", "DUTY");
		TARGET_BY_PREFIX.put("/search", "SEARCH");
		TARGET_BY_PREFIX.put("/file/", "FILE");
	}

	private final ApplicationEventPublisher eventPublisher;

	public AuditInterceptor(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		String path = path(request);
		if (isExcluded(path)) {
			return true;
		}
		HttpSession session = request.getSession(false);
		EgovMap loginUser = session == null ? null : asMap(session.getAttribute(LoginController.SESSION_KEY));
		String userId = loginUser == null ? null : string(loginUser.get("userId"));
		String userNm = loginUser == null ? null : string(loginUser.get("userNm"));
		if (userId == null) {
			Object apiUser = request.getAttribute(JwtAuthFilter.API_USER_KEY);
			userId = apiUser == null ? null : String.valueOf(apiUser);
		}
		String eventType = eventType(path);
		// 로그인 전 공개 화면은 감사 대상이 아니다. 단, 파일 다운로드와 API는 대상 식별자를 남긴다.
		if (userId == null && !"PII_DOWNLOAD".equals(eventType) && !"API_ACCESS".equals(eventType)) {
			return true;
		}
		try {
			String targetType = targetType(path);
			String targetId = targetId(request, path);
			eventPublisher.publishEvent(AuditEvent.access(eventType, userId, userNm, menuId(path), targetType,
					targetId, path, request.getMethod(), request.getRemoteAddr()));
		} catch (Exception ignored) {
			// 감사 로그 발행 실패가 요청을 막지 않는다.
		}
		return true;
	}

	private String eventType(String path) {
		if (path.startsWith("/api/")) {
			return "API_ACCESS";
		}
		if (path.contains("download") || path.endsWith(".do") && path.contains("excel")) {
			return "PII_DOWNLOAD";
		}
		if (targetType(path) != null) {
			return "PII_VIEW";
		}
		return "MENU_ACCESS";
	}

	private String menuId(String path) {
		for (Map.Entry<String, String> entry : MENU_BY_PREFIX.entrySet()) {
			if (path.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	private String targetType(String path) {
		for (Map.Entry<String, String> entry : TARGET_BY_PREFIX.entrySet()) {
			if (path.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	private String targetId(HttpServletRequest request, String path) {
		String[] keys = { "boardId", "userId", "fileId", "downloadFileId", "dutyId", "messageId", "commentId",
				"roleId" };
		for (String key : keys) {
			String value = request.getParameter(key);
			if (value != null && !value.trim().isEmpty()) {
				return value.trim().length() > 100 ? value.trim().substring(0, 100) : value.trim();
			}
		}
		return null;
	}

	private boolean isExcluded(String path) {
		return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
				|| path.startsWith("/img/") || path.startsWith("/vendor/") || path.startsWith("/fonts/")
				|| path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/")
				|| path.startsWith("/actuator/") || path.equals("/favicon.ico") || path.equals("/error")
				|| path.equals("/board/subscribe") || path.startsWith("/login/");
	}

	private EgovMap asMap(Object value) {
		return value instanceof EgovMap map ? map : null;
	}

	private String string(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private String path(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		return uri != null && uri.startsWith(context) ? uri.substring(context.length()) : uri;
	}
}
