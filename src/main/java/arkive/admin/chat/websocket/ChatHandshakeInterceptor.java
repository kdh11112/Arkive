package arkive.admin.chat.websocket;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import arkive.admin.auth.service.MenuPermission;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.auth.web.LoginController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 채팅 WebSocket 핸드셰이크 검사.
 *
 * <p>세션 로그인 사용자만 연결을 허용하고, 관리자가 아니면 채팅 메뉴의
 * READ·WRITE 권한을 서버에서 확인한다. 브라우저의 교차 사이트 WebSocket
 * 공격을 막기 위해 Origin을 검사한다. 허용 목록이 비어 있으면 같은 호스트만 허용한다.</p>
 */
@Component("chatHandshakeInterceptor")
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(ChatHandshakeInterceptor.class);
	private static final String CHAT_MENU = "ME020B0100";

	@Value("${arkive.chat.allowed-origins:}")
	private String allowedOrigins;

	private final PermissionService permissionService;

	public ChatHandshakeInterceptor(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return false;
		}
		HttpServletRequest httpRequest = servletRequest.getServletRequest();
		if (!originAllowed(httpRequest)) {
			logger.warn("chat handshake denied: origin={}", httpRequest.getHeader("Origin"));
			return false;
		}
		HttpSession session = httpRequest.getSession(false);
		if (session == null) {
			return false;
		}
		Object rawUser = session.getAttribute(LoginController.SESSION_KEY);
		if (!(rawUser instanceof EgovMap)) {
			return false;
		}
		String userId = String.valueOf(((EgovMap) rawUser).get("userId"));
		String userNm = String.valueOf(((EgovMap) rawUser).get("userNm"));
		boolean admin = session.getAttribute(LoginController.ADMIN_KEY) != null;
		if (!admin) {
			MenuPermission permission = permissionService.selectUserMenuPermissions(userId).get(CHAT_MENU);
			if (permission == null || !permission.read() || !permission.write()) {
				logger.info("chat handshake denied: user={}, menu permission missing", userId);
				return false;
			}
		}
		attributes.put("chatUserId", userId);
		attributes.put("chatUserNm", userNm);
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}

	private boolean originAllowed(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		if (origin == null || origin.isBlank()) {
			// 브라우저가 아닌 클라이언트는 Origin을 보내지 않는다. 세션 쿠키 인증으로 제한한다.
			return true;
		}
		Set<String> configured = new HashSet<>();
		if (allowedOrigins != null) {
			Arrays.stream(allowedOrigins.split(","))
					.map(String::trim)
					.filter(value -> !value.isEmpty())
					.forEach(configured::add);
		}
		if (!configured.isEmpty()) {
			return configured.contains(origin);
		}
		try {
			URI uri = URI.create(origin);
			int expectedPort = request.getServerPort();
			int originPort = uri.getPort() >= 0 ? uri.getPort()
					: ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
			return request.getServerName().equalsIgnoreCase(uri.getHost()) && expectedPort == originPort;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
