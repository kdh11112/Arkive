package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import arkive.support.TestSessions;

/**
 * 로그인 인터셉터 단위 테스트. 공개 경로, JSON 401, 화면 redirect, 관리자 경로를 확인한다.
 */
class AuthInterceptorTest {

	private final AuthInterceptor interceptor = new AuthInterceptor();

	private MockHttpServletRequest request(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/app");
		request.setRequestURI("/app" + uri);
		return request;
	}

	@Test
	void publicPathsShouldPassWithoutLogin() throws Exception {
		assertThat(interceptor.preHandle(request("/login/login"), new MockHttpServletResponse(), new Object())).isTrue();
		assertThat(interceptor.preHandle(request("/css/app.css"), new MockHttpServletResponse(), new Object())).isTrue();
		assertThat(interceptor.preHandle(request("/images/logo.png"), new MockHttpServletResponse(), new Object())).isTrue();
		assertThat(interceptor.preHandle(request("/board/toastList.json"), new MockHttpServletResponse(), new Object())).isTrue();
	}

	@Test
	void htmlShouldRedirectToLoginWithoutSession() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(request("/board/list"), response, new Object());

		assertThat(allowed).isFalse();
		assertThat(response.getRedirectedUrl()).isEqualTo("/app/login/login");
	}

	@Test
	void jsonShouldReturn401WithoutSession() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(request("/message/insertMessage.json"), response, new Object());

		assertThat(allowed).isFalse();
		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("\"success\":false");
	}

	@Test
	void loginSessionShouldAllowGeneralPath() throws Exception {
		MockHttpServletRequest request = request("/board/list");
		request.setSession(TestSessions.user("user1"));

		assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
	}

	@Test
	void adminPathShouldRequireAdminSession() throws Exception {
		MockHttpServletRequest general = request("/system/menu");
		general.setSession(TestSessions.user("user1"));
		MockHttpServletResponse redirect = new MockHttpServletResponse();

		assertThat(interceptor.preHandle(general, redirect, new Object())).isFalse();
		assertThat(redirect.getRedirectedUrl()).isEqualTo("/app/login/login");

		MockHttpServletRequest admin = request("/system/menu");
		admin.setSession(TestSessions.admin("admin"));
		assertThat(interceptor.preHandle(admin, new MockHttpServletResponse(), new Object())).isTrue();
	}
}
