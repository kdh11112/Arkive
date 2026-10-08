package arkive.com.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import arkive.admin.auth.web.JwtAuthFilter;
import arkive.admin.auth.web.JwtUtil;

/**
 * /api/** JWT 필터 등록. 화면(세션)과 인증 체계를 분리한다.
 * 등록 역할: 이 설정이 있어야 필터가 돈다. 없으면 /api가 무방비다.
 */
@Configuration
public class JwtConfig {

	@Bean
	public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtUtil jwtUtil) {
		// 필터 등록 그릇이다. 필터 객체를 담아 서블릿 컨테이너에 붙인다.
		FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
		// 검사 주체. JwtUtil을 주입받아 쓴다.
		registration.setFilter(new JwtAuthFilter(jwtUtil));
		// /api/ 밑에만 이 필터를 태운다. 화면에는 안 건다.
		registration.addUrlPatterns("/api/*");
		// 필터 순서. 낮을수록 먼저 돈다.
		registration.setOrder(1);
		return registration;
	}
}
