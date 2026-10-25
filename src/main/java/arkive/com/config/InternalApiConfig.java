package arkive.com.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import arkive.admin.api.web.InternalApiAuthFilter;

/**
 * 내부 API 토큰 필터 등록.
 *
 * <p>토큰이 비어 있으면 모든 {@code /internal/**} 요청을 503으로 닫는다. 대상 프로젝트에서
 * 게이트웨이·서비스 토큰 정책이 정해지면 이 필터를 그 인증 방식으로 교체한다.</p>
 */
@Configuration
public class InternalApiConfig {

	@Bean
	public FilterRegistrationBean<InternalApiAuthFilter> internalApiAuthFilter(
			@Value("${arkive.api.internal-token:}") String internalToken) {
		FilterRegistrationBean<InternalApiAuthFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new InternalApiAuthFilter(internalToken));
		registration.addUrlPatterns("/internal/*");
		registration.setOrder(1);
		return registration;
	}
}
