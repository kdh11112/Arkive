package arkive.admin.terms.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import arkive.admin.auth.web.LoginController;

/**
 * 필수 약관 미동의 세션의 화면 이동과 JSON 차단을 검증한다.
 */
class TermsInterceptorTest {

	private final TermsInterceptor interceptor = new TermsInterceptor();

	private MockHttpSession pendingSession() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(LoginController.SESSION_KEY,
				new org.egovframe.rte.psl.dataaccess.util.EgovMap());
		session.setAttribute(TermsInterceptor.PENDING_TERMS, List.of("T_SERVICE_1"));
		return session;
	}

	@Test
	void pageRequestShouldRedirectToTermsAgree() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
		request.setSession(pendingSession());
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(request, response, new Object());

		assertThat(allowed).isFalse();
		assertThat(response.getRedirectedUrl()).isEqualTo("/login/termsAgree");
	}

	@Test
	void jsonRequestShouldReturnConflict() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/board/insertBoard.json");
		request.setSession(pendingSession());
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(request, response, new Object());

		assertThat(allowed).isFalse();
		assertThat(response.getStatus()).isEqualTo(409);
		assertThat(response.getContentAsString()).contains("\"termsRequired\":true");
	}

	@Test
	void termsAgreeAndLoginPagesShouldBeAllowed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/termsAgree");
		request.setSession(pendingSession());
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
	}

	@Test
	void sessionWithoutPendingTermsShouldPass() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
		request.setSession(new MockHttpSession());
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
	}
}
