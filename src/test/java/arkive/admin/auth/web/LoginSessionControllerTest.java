package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import arkive.support.ArkiveIntegrationTest;

/**
 * 실제 로그인 API를 두 번 호출해 이전 세션이 만료되는지 확인한다.
 * 계정은 V1 seed의 개발용 admin을 사용한다.
 */
@ArkiveIntegrationTest
@Transactional
class LoginSessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoginSessionRegistry loginSessionRegistry;

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/login/login.json")
				.param("userId", "admin")
				.param("password", "Admin1234!"))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		return session;
	}

	@Test
	@Rollback
	void secondLoginShouldInvalidateFirstSession() throws Exception {
		MockHttpSession first = login();
		assertThat(first.isInvalid()).isFalse();

		MockHttpSession second = login();

		assertThat(second).isNotSameAs(first);
		assertThat(first.isInvalid()).isTrue();
		assertThat(second.isInvalid()).isFalse();
		assertThat(loginSessionRegistry.currentSession("admin")).isSameAs(second);

		// 공유 컨텍스트에 남지 않도록 정리한다.
		loginSessionRegistry.remove("admin", second);
	}
}
