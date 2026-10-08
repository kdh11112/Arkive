package arkive.admin.auth.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 로그인 검사. 공개 경로 외에는 세션 LOGIN_USER를 요구한다.
 * JSON(.json)은 401 JSON, 화면은 /login/login으로 보낸다.
 * EgovConfigWeb에서 빈으로 등록한다.
 */
public class AuthInterceptor implements HandlerInterceptor {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final Set<String> ALLOW_PREFIX = new HashSet<>(Arrays.asList(
			"/login/", "/user/", "/api/", "/css/", "/js/", "/images/", "/img/", "/vendor/", "/fonts/",
			"/portal/", "/swagger-ui/", "/v3/api-docs/", "/actuator/"));

	private static final Set<String> ALLOW_EXACT = new HashSet<>(Arrays.asList(
			"/", "/error", "/favicon.ico",
			"/board/toastList.json",
			"/popup/activePopup.json",
			"/banner/activeBanner.json",
			"/file/downloadFile.do",
			"/file/downloadFile2.do",
			"/preview/fileView"));

	private static final Set<String> ADMIN_PREFIX = new HashSet<>(Arrays.asList(
			"/system/", "/auth/"));

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;
		if (isPublic(path)) {
			return true;
		}
		HttpSession session = request.getSession(false);
		String needKey = LoginController.SESSION_KEY;
		for (String prefix : ADMIN_PREFIX) {
			if (path.startsWith(prefix)) {
				needKey = LoginController.ADMIN_KEY;
				break;
			}
		}
		if (session != null && session.getAttribute(needKey) != null) {
			return true;
		}
		if (path.endsWith(".json")) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
			return false;
		}
		response.sendRedirect(context + "/login/login");
		return false;
	}

	private boolean isPublic(String path) {
		if (ALLOW_EXACT.contains(path)) {
			return true;
		}
		for (String prefix : ALLOW_PREFIX) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		int dot = path.lastIndexOf('.');
		if (dot >= 0) {
			String ext = path.substring(dot + 1).toLowerCase();
			if ("css".equals(ext) || "js".equals(ext) || "png".equals(ext) || "jpg".equals(ext)
					|| "jpeg".equals(ext) || "gif".equals(ext) || "bmp".equals(ext) || "svg".equals(ext)
					|| "woff".equals(ext) || "woff2".equals(ext) || "ico".equals(ext) || "map".equals(ext)) {
				return true;
			}
		}
		return false;
	}
}
