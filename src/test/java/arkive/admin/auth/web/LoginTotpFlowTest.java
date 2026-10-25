package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.totp.service.TotpService;
import arkive.support.ArkiveIntegrationTest;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;

/**
 * TOTP가 켜진 사용자의 로그인 흐름을 확인한다.
 * 비밀번호만으로 세션을 만들지 않고, 5회 실패 시 대기 정보를 지우며, 성공 시에만 로그인 세션을 만든다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class LoginTotpFlowTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TotpService totpService;

	@Autowired
	private LoginSessionRegistry loginSessionRegistry;

	@Test
	void loginShouldRequireTotpAndLockAfterFiveFailures() throws Exception {
		String userId = "totpflow1";
		insertUser(userId);
		Map<String, Object> setup = totpService.beginSetup(userId);
		String secret = String.valueOf(setup.get("secret"));
		totpService.confirmSetup(userId, currentCode(secret));

		mockMvc.perform(post("/login/totpVerify.json").param("code", "!!!!!!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("2단계 인증 대기 정보가 없습니다. 다시 로그인하세요."));

		MockHttpSession session = login(userId);
		assertThat(session.getAttribute(LoginController.TOTP_PENDING_KEY)).isNotNull();
		assertThat(session.getAttribute(LoginController.SESSION_KEY)).isNull();

		for (int i = 1; i <= 4; i++) {
			mockMvc.perform(post("/login/totpVerify.json").session(session).param("code", "!!!!!!"))
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message")
							.value("인증 코드가 올바르지 않습니다. 남은 시도: " + (5 - i)));
		}
		mockMvc.perform(post("/login/totpVerify.json").session(session).param("code", "!!!!!!"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("2단계 인증을 5회 실패했습니다. 비밀번호부터 다시 로그인하세요."));
		assertThat(session.getAttribute(LoginController.TOTP_PENDING_KEY)).isNull();
		assertThat(session.getAttribute(LoginController.SESSION_KEY)).isNull();

		MockHttpSession retried = login(userId);
		mockMvc.perform(post("/login/totpVerify.json").session(retried)
				.param("code", currentCode(secret)))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.userNm").value("TOTP Tester"))
				.andExpect(jsonPath("$.isAdmin").value(false));
		assertThat(retried.getAttribute(LoginController.TOTP_PENDING_KEY)).isNull();
		assertThat(retried.getAttribute(LoginController.TOTP_FAIL_KEY)).isNull();
		Object loggedIn = retried.getAttribute(LoginController.SESSION_KEY);
		assertThat(loggedIn).isInstanceOf(EgovMap.class);
		assertThat(((EgovMap) loggedIn).get("userId")).isEqualTo(userId);
		loginSessionRegistry.remove(userId, retried);
	}

	private MockHttpSession login(String userId) throws Exception {
		MvcResult result = mockMvc.perform(post("/login/login.json")
				.param("userId", userId)
				.param("password", "TotpPass123!"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.totpRequired").value(true))
				.andExpect(jsonPath("$.message").value("2단계 인증 코드를 입력하세요."))
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private void insertUser(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", passwordEncoder.encode("TotpPass123!"));
		user.put("userNm", "TOTP Tester");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}

	private String currentCode(String secret) throws Exception {
		DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
		long timeStep = Math.floorDiv(System.currentTimeMillis() / 1000L, 30L);
		return generator.generate(secret, timeStep);
	}
}
