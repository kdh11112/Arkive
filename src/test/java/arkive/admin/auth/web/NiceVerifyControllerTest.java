package arkive.admin.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;

/**
 * NICE 본인인증의 상태 조회·초기화와 미계약 상태 차단을 확인한다.
 * 실제 CheckPlus 계약·복호화는 확인되지 않아 테스트하지 않는다.
 */
@ArkiveIntegrationTest
class NiceVerifyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void statusShouldReportNotVerifiedAndClearShouldSucceed() throws Exception {
		mockMvc.perform(get("/login/nice/status.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.verified").value(false));

		mockMvc.perform(post("/login/nice/clear.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void startShouldBeBlockedWhenSiteCodeIsMissing() throws Exception {
		mockMvc.perform(get("/login/nice/start"))
				.andExpect(status().is5xxServerError())
				.andExpect(header().doesNotExist("Location"));
	}
}
