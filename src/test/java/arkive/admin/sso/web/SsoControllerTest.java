package arkive.admin.sso.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;

/**
 * SSO가 기본 비활성일 때 로그인 시작·콜백이 외부로 나가지 않고 404로 차단되는지 확인한다.
 * 실제 SSO 서버 계약은 확인되지 않아 연동 흐름은 테스트하지 않는다.
 */
@ArkiveIntegrationTest
class SsoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void disabledSsoShouldReturnNotFound() throws Exception {
		mockMvc.perform(get("/sso/login"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/sso/callback").param("code", "test").param("state", "test"))
				.andExpect(status().isNotFound());
	}
}
