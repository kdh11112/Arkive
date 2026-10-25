package arkive.admin.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;

/**
 * 소셜 로그인 키가 없을 때 외부 제공자로 이동하지 않고 차단되는지 확인한다.
 * 실제 네이버·카카오·구글 토큰 교환은 계약 키가 없어 테스트하지 않는다.
 */
@ArkiveIntegrationTest
class OauthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unsupportedProviderShouldBeRejected() throws Exception {
		mockMvc.perform(get("/login/oauth/twitter"))
				.andExpect(status().is5xxServerError())
				.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void unconfiguredProviderShouldBeRejectedBeforeRedirect() throws Exception {
		mockMvc.perform(get("/login/oauth/naver"))
				.andExpect(status().is5xxServerError())
				.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void callbackWithInvalidStateShouldBeRejected() throws Exception {
		mockMvc.perform(get("/login/oauth/naverCallback")
				.param("state", "forged")
				.param("code", "code"))
				.andExpect(status().is5xxServerError())
				.andExpect(header().doesNotExist("Location"));
	}
}
