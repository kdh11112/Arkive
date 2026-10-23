package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.AuthService;
import arkive.admin.auth.service.impl.AuthMapper;
import arkive.support.ArkiveIntegrationTest;

/**
 * 비밀번호 찾기 메일 요청과 1회용 재설정 토큰 흐름을 검증한다.
 * 실제 SMTP는 테스트 설정에서 꺼져 있으므로 토큰 발급·소비·비밀번호 변경만 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class PasswordResetControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private void insertUser(String userId, String email) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", passwordEncoder.encode("OldPass123!"));
		user.put("userNm", "Reset Tester");
		user.put("email", email);
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}

	@Test
	void findPwShouldIssueTokenOnlyForMatchingUser() throws Exception {
		insertUser("findpw01", "findpw01@example.com");

		mockMvc.perform(post("/login/findPw.json")
				.param("userId", "findpw01")
				.param("userNm", "Reset Tester")
				.param("email", "wrong@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(authMapper.selectLatestPasswordResetDt("findpw01")).isNull();

		mockMvc.perform(post("/login/findPw.json")
				.param("userId", "findpw01")
				.param("userNm", "Reset Tester")
				.param("email", "FINDPW01@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value(
						"입력한 정보가 일치하면 비밀번호 재설정 링크를 이메일로 보냅니다."));

		assertThat(authMapper.selectLatestPasswordResetDt("findpw01")).isNotNull();
	}

	@Test
	void resetPwShouldChangePasswordAndConsumeToken() throws Exception {
		insertUser("resetpw01", "resetpw01@example.com");
		String token = authService.issuePasswordResetToken("resetpw01");
		assertThat(token).isNotBlank();

		mockMvc.perform(get("/login/resetPw").param("token", token))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("새 비밀번호")));

		mockMvc.perform(post("/login/resetPw.json")
				.param("token", token)
				.param("password", "NewPass123!")
				.param("passwordConfirm", "NewPass123!"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value(
						"비밀번호가 변경되었습니다. 새 비밀번호로 로그인하세요."));

		assertThat(authService.isValidPasswordResetToken(token)).isFalse();
		EgovMap user = authService.selectLoginUser("resetpw01");
		assertThat(passwordEncoder.matches("NewPass123!", String.valueOf(user.get("password")))).isTrue();
		assertThat(user.get("failedCnt")).isEqualTo(0);
		assertThat(user.get("lockYn")).isEqualTo("N");

		mockMvc.perform(post("/login/resetPw.json")
				.param("token", token)
				.param("password", "AnotherPass123!")
				.param("passwordConfirm", "AnotherPass123!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(
						"재설정 링크가 만료되었거나 이미 사용되었습니다."));
	}

	@Test
	void invalidTokenShouldShowExpiredPageAndRejectReset() throws Exception {
		mockMvc.perform(get("/login/resetPw").param("token", "invalid-token"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("만료되었거나 이미 사용")));

		mockMvc.perform(post("/login/resetPw.json")
				.param("token", "invalid-token")
				.param("password", "NewPass123!")
				.param("passwordConfirm", "NewPass123!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(
						"재설정 링크가 만료되었거나 이미 사용되었습니다."));
	}

	@Test
	void resetPwShouldValidateInputBeforeConsumingToken() throws Exception {
		insertUser("resetpw02", "resetpw02@example.com");
		String token = authService.issuePasswordResetToken("resetpw02");

		mockMvc.perform(post("/login/resetPw.json")
				.param("token", token)
				.param("password", "short")
				.param("passwordConfirm", "short"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 입력하세요."));

		mockMvc.perform(post("/login/resetPw.json")
				.param("token", token)
				.param("password", "NewPass123!")
				.param("passwordConfirm", "Different123!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호 확인이 일치하지 않습니다."));

		assertThat(authService.isValidPasswordResetToken(token)).isTrue();
	}

	@Test
	void tokenIssueShouldRespectCooldown() throws Exception {
		insertUser("resetpw03", "resetpw03@example.com");

		String first = authService.issuePasswordResetToken("resetpw03");
		String second = authService.issuePasswordResetToken("resetpw03");

		assertThat(first).isNotBlank();
		assertThat(second).isNull();
	}
}
