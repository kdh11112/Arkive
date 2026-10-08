package arkive.admin.auth.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * /api/** JWT 검사. Authorization: Bearer 토큰을 검증해 API_USER를 request에 둔다.
 * 없거나 틀리면 401 JSON으로 끝낸다.
 *
 * 문지기 역할:
 * - 서블릿 필터라 인터셉터보다 먼저 실행된다.
 * - /api/login.json만 예외다(토큰 받으러 오는 길이라).
 * - 통과하면 request에 아이디(API_USER)를 적어둔다. 컨트롤러는 세션 없이 이걸 쓴다.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String API_USER_KEY = "API_USER";

	private final JwtUtil jwtUtil;

	public JwtAuthFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		// 컨텍스트 경로를 뗀 순수 경로로 판단한다.
		String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;
		// 로그인 발급은 토큰 없이 통과시킨다.
		if ("/api/login.json".equals(path)) {
			chain.doFilter(request, response);
			return;
		}
		// 헤더 모양: Authorization: Bearer 토큰. 없으면 여기서 끝낸다.
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			unauthorized(response, "토큰이 없습니다.");
			return;
		}
		// "Bearer " 7글자를 뗀 나머지가 토큰이다.
		String token = header.substring(7).trim();
		try {
			// 서명·만료 검증. 실패하면 아래 catch로 간다.
			Jws<Claims> jws = jwtUtil.parseToken(token);
			// 컨트롤러가 쓸 아이디를 request에 적어둔다. 세션은 안 쓴다.
			request.setAttribute(API_USER_KEY, jws.getPayload().getSubject());
			// 다음 필터·컨트롤러로 넘긴다.
			chain.doFilter(request, response);
		} catch (Exception e) {
			// 만료·위조 모두 같은 메시지다. 무엇이 틀렸는지 드러내지 않는다.
			logger.warn("jwt rejected: {}", e.getMessage());
			unauthorized(response, "유효하지 않은 토큰입니다.");
		}
	}

	private void unauthorized(HttpServletResponse response, String message) throws IOException {
		// 401 + JSON. 화면 리다이렉트는 안 한다(API라서).
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;
		// /api/ 아니면 이 필터는 건너뛴다. 화면 요청에는 관여 안 한다.
		return !path.startsWith("/api/");
	}
}
