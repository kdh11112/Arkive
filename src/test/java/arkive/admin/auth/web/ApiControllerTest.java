package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.support.ArkiveIntegrationTest;

/**
 * JWT 발급·검증, /api/** 접근 제한, 실패 5회 잠금을 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class ApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	@Test
	void apiLoginShouldIssueJwtAndProtectApiPaths() throws Exception {
		insertUser("apiuser1");

		String body = mockMvc.perform(post("/api/login.json")
				.param("userId", "apiuser1")
				.param("password", "ApiPass123!"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.apiVersion").value("v1"))
				.andExpect(jsonPath("$.userNm").value("API 사용자"))
				.andReturn().getResponse().getContentAsString();
		String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");
		assertThat(token).isNotBlank();
		assertThat(jwtUtil.parseToken(token).getPayload().getSubject()).isEqualTo("apiuser1");

		mockMvc.perform(get("/api/noticeList.json"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("토큰이 없습니다."));
		mockMvc.perform(get("/api/noticeList.json").header("Authorization", "Bearer not-a-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));

		mockMvc.perform(get("/api/noticeList.json").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.apiVersion").value("v1"))
				.andExpect(jsonPath("$.list").isArray());
		mockMvc.perform(get("/api/v1/noticeList.json").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void apiLoginShouldLockAfterFiveFailures() throws Exception {
		insertUser("apiuser2");

		for (int i = 0; i < 4; i++) {
			mockMvc.perform(post("/api/login.json")
					.param("userId", "apiuser2")
					.param("password", "wrong"))
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
		}
		mockMvc.perform(post("/api/login.json")
				.param("userId", "apiuser2")
				.param("password", "wrong"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("5회 실패로 잠겼습니다. 관리자에게 문의하세요."));

		EgovMap user = authMapper.selectLoginUser("apiuser2");
		assertThat(String.valueOf(user.get("lockYn"))).isEqualTo("Y");
		mockMvc.perform(post("/api/login.json")
				.param("userId", "apiuser2")
				.param("password", "ApiPass123!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("잠긴 계정입니다. 관리자에게 문의하세요."));

		mockMvc.perform(post("/api/login.json").param("userId", "").param("password", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("아이디와 비밀번호를 입력하세요."));
	}

	private void insertUser(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", passwordEncoder.encode("ApiPass123!"));
		user.put("userNm", "API 사용자");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}
}
