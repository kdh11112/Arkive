package arkive.admin.terms.web;

import java.util.List;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 필수 약관 미동의 사용자를 동의 화면으로 유도한다.
 *
 * <p>로그인 시 {@link #PENDING_TERMS}에 미동의 필수 약관 ID를 저장한다. 화면 요청은
 * 동의 화면으로 보내고, JSON 요청은 409로 거절해 화면 일부 기능만 동작하는 상태를 막는다.</p>
 */
public class TermsInterceptor implements HandlerInterceptor {

	public static final String PENDING_TERMS = "PENDING_TERMS";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String path = path(request);
		if (isAllowed(path)) {
			return true;
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			return true;
		}
		Object pending = session.getAttribute(PENDING_TERMS);
		if (!(pending instanceof List<?> list) || list.isEmpty()) {
			return true;
		}
		if (path.endsWith(".json")) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(
					"{\"success\":false,\"termsRequired\":true,\"message\":\"필수 약관에 동의해야 계속할 수 있습니다.\"}");
			return false;
		}
		response.sendRedirect(request.getContextPath() + "/login/termsAgree");
		return false;
	}

	private boolean isAllowed(String path) {
		return path.startsWith("/login/") || path.startsWith("/css/") || path.startsWith("/js/")
				|| path.startsWith("/images/") || path.startsWith("/img/") || path.startsWith("/vendor/")
				|| path.startsWith("/fonts/") || path.startsWith("/error") || path.startsWith("/terms")
				|| path.startsWith("/api/") || path.startsWith("/internal/")
				|| path.startsWith("/sso/") || path.startsWith("/ws/")
				|| path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/")
				|| path.startsWith("/actuator/") || path.equals("/favicon.ico");
	}

	private String path(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		return uri != null && uri.startsWith(context) ? uri.substring(context.length()) : uri;
	}
}
