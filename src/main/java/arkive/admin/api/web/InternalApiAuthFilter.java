package arkive.admin.api.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 내부 API 토큰 검사.
 *
 * <p>{@code /internal/**}는 세션·JWT와 분리된 서비스 토큰으로만 접근한다. 토큰이 설정되지
 * 않으면 기능을 닫고 503을 돌려준다. 토큰 비교는 길이·내용 차이를 노출하지 않도록
 * 상수 시간 비교를 사용한다.</p>
 */
public class InternalApiAuthFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Internal-Token";

	private final String configuredToken;

	public InternalApiAuthFilter(String configuredToken) {
		this.configuredToken = configuredToken;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (configuredToken == null || configuredToken.isBlank()) {
			write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"내부 API가 설정되지 않았습니다.");
			return;
		}
		String provided = request.getHeader(HEADER);
		if (!constantTimeEquals(configuredToken, provided)) {
			write(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 내부 API 토큰입니다.");
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean constantTimeEquals(String expected, String provided) {
		if (provided == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				provided.getBytes(StandardCharsets.UTF_8));
	}

	private void write(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write("{\"success\":false,\"apiVersion\":\"v1\",\"message\":\"" + message + "\"}");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;
		return !path.startsWith("/internal/");
	}
}
