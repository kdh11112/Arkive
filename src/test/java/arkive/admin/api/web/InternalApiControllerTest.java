package arkive.admin.api.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;

/**
 * 내부 API의 서비스 토큰 검사와 공지 응답(PII 미포함)을 확인한다.
 * 테스트 토큰은 application-test.properties의 arkive.api.internal-token 값이다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class InternalApiControllerTest {

	private static final String TOKEN = "test-internal-token";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void requestWithoutValidTokenShouldBeRejected() throws Exception {
		mockMvc.perform(get("/internal/v1/health"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("유효하지 않은 내부 API 토큰입니다."));
		mockMvc.perform(get("/internal/v1/health").header(InternalApiAuthFilter.HEADER, "wrong-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthShouldReportUpWithValidToken() throws Exception {
		mockMvc.perform(get("/internal/v1/health").header(InternalApiAuthFilter.HEADER, TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.apiVersion").value("v1"))
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void noticesShouldReturnLatestCkWithoutPersonalFields() throws Exception {
		jdbcTemplate.update("INSERT INTO BOARD (BOARD_ID, BOARD_TYPE, TITLE, CONTENT, VIEW_CNT, "
				+ "REGIST_DT, REGISTER, NOTICE_YN) VALUES ('INT-NOTICE-1', 'CK', '내부 API 공지', '본문', 0, "
				+ "CAST('2099-01-01 00:00:00' AS TIMESTAMP), 'admin', 'Y')");

		mockMvc.perform(get("/internal/v1/notices").header(InternalApiAuthFilter.HEADER, TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.list[0].title").value("내부 API 공지"))
				.andExpect(jsonPath("$.list[0].boardType").value("CK"))
				.andExpect(jsonPath("$.list[0].email").doesNotExist())
				.andExpect(jsonPath("$.list[0].phone").doesNotExist())
				.andExpect(jsonPath("$.list[0].password").doesNotExist())
				.andExpect(content().string(not(containsString("enc:v1:"))));
	}
}
