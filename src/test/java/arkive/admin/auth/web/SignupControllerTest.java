package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.support.ArkiveIntegrationTest;

/**
 * 회원가입(필수 약관·일반 회원 고정·PII 암호화), 아이디 찾기 마스킹, 비밀번호 찾기 토큰 발급을 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class SignupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void publicMemberScreensShouldRender() throws Exception {
		mockMvc.perform(get("/login/signup"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("termsList"))
				.andExpect(content().string(containsString("회원가입")));
		mockMvc.perform(get("/login/findId"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("아이디 찾기")));
		mockMvc.perform(get("/login/findPw"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("비밀번호 찾기")));
	}

	@Test
	@SuppressWarnings("unchecked")
	void signupShouldCreateNormalUserWithConsentsAndEncryptedPhone() throws Exception {
		List<String> requiredTerms = requiredTermIds();
		assertThat(requiredTerms).isNotEmpty();

		signup("signup01", "Password123!", "가입자", "signup01@example.com", "N", "010-2222-3333", "Y",
				requiredTerms);

		EgovMap login = authMapper.selectLoginUser("signup01");
		assertThat(login).isNotNull();
		assertThat(String.valueOf(login.get("adminYn"))).isEqualTo("N");
		assertThat(String.valueOf(login.get("password"))).startsWith("$2");
		assertThat(String.valueOf(login.get("phone"))).startsWith("enc:v1:");
		assertThat(String.valueOf(login.get("phone"))).doesNotContain("010-2222-3333");
		assertThat(permissionMapper.selectUserRoleIds("signup01")).containsExactly("ROLE_USER");
		Integer consents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM USER_TERMS_CONSENT WHERE USER_ID = ? AND CONSENT_YN = 'Y'",
				Integer.class, "signup01");
		assertThat(consents).isEqualTo(requiredTerms.size());
	}

	@Test
	void signupShouldValidateRequiredValues() throws Exception {
		List<String> requiredTerms = requiredTermIds();

		expectSignupFailure("ab", "Password123!", "이름", "", "N", "", "N", requiredTerms,
				"아이디는 영문·숫자 4~20자로 입력하세요.");
		expectSignupFailure("signup02", "short", "이름", "", "N", "", "N", requiredTerms,
				"비밀번호는 8자 이상 입력하세요.");
		expectSignupFailure("signup02", "Password123!", " ", "", "N", "", "N", requiredTerms,
				"이름을 입력하세요.");
		expectSignupFailure("signup02", "Password123!", "이름", "", "N", "", "N", List.of(),
				"필수 약관에 모두 동의해야 가입할 수 있습니다.");
		expectSignupFailure("signup02", "Password123!", "이름", "", "Y", "", "N", requiredTerms,
				"메일 수신에 동의하려면 이메일을 입력하세요.");
		expectSignupFailure("signup02", "Password123!", "이름", "bad@x", "N", "", "N", requiredTerms,
				"이메일 형식이 올바르지 않습니다.");
		expectSignupFailure("signup02", "Password123!", "이름", "", "N", "12", "Y", requiredTerms,
				"SMS 수신에 동의하려면 올바른 휴대전화 번호를 입력하세요.");

		signup("signup02", "Password123!", "가입자2", "", "N", "", "N", requiredTerms);
		expectSignupFailure("signup02", "Password123!", "가입자2", "", "N", "", "N", requiredTerms,
				"이미 등록된 아이디입니다.");
	}

	@Test
	void findIdShouldMaskUserIdAndRejectUnknownMember() throws Exception {
		signup("signup03", "Password123!", "아이디찾기", "find@example.com", "N", "", "N", requiredTermIds());

		mockMvc.perform(post("/login/findId.json")
				.param("userNm", "아이디찾기")
				.param("email", "find@example.com"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.userId").value("sig****"));

		mockMvc.perform(post("/login/findId.json")
				.param("userNm", "아이디찾기")
				.param("email", "other@example.com"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("일치하는 회원 정보가 없습니다."));

		mockMvc.perform(post("/login/findId.json").param("userNm", "").param("email", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이름과 이메일을 입력하세요."));
	}

	@Test
	void findPwShouldIssueTokenOnlyWhenIdentityMatches() throws Exception {
		signup("signup04", "Password123!", "비밀번호찾기", "findpw@example.com", "N", "", "N", requiredTermIds());

		mockMvc.perform(post("/login/findPw.json")
				.param("userId", "signup04")
				.param("userNm", "비밀번호찾기")
				.param("email", "wrong@example.com"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value(containsString("이메일로 보냅니다")));
		assertThat(authMapper.selectLatestPasswordResetDt("signup04")).isNull();

		mockMvc.perform(post("/login/findPw.json")
				.param("userId", "signup04")
				.param("userNm", "비밀번호찾기")
				.param("email", "findpw@example.com"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(authMapper.selectLatestPasswordResetDt("signup04")).isNotNull();

		mockMvc.perform(post("/login/findPw.json")
				.param("userId", "")
				.param("userNm", "")
				.param("email", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("아이디·이름·이메일을 입력하세요."));
	}

	@SuppressWarnings("unchecked")
	private List<String> requiredTermIds() throws Exception {
		MvcResult screen = mockMvc.perform(get("/login/signup"))
				.andExpect(status().isOk())
				.andReturn();
		List<EgovMap> terms = (List<EgovMap>) screen.getModelAndView().getModel().get("termsList");
		List<String> ids = new ArrayList<>();
		for (EgovMap term : terms) {
			if ("Y".equals(String.valueOf(term.get("requiredYn")))) {
				ids.add(String.valueOf(term.get("termsId")));
			}
		}
		return ids;
	}

	private void expectSignupFailure(String userId, String password, String userNm, String email, String mailYn,
			String phone, String smsYn, List<String> termIds, String message) throws Exception {
		var request = post("/login/signup.json")
				.param("userId", userId)
				.param("password", password)
				.param("userNm", userNm)
				.param("email", email)
				.param("mailYn", mailYn)
				.param("phone", phone)
				.param("smsYn", smsYn);
		if (!termIds.isEmpty()) {
			request.param("termsIds", termIds.toArray(String[]::new));
		}
		mockMvc.perform(request)
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(message));
	}

	private void signup(String userId, String password, String userNm, String email, String mailYn, String phone,
			String smsYn, List<String> termIds) throws Exception {
		mockMvc.perform(post("/login/signup.json")
				.param("userId", userId)
				.param("password", password)
				.param("userNm", userNm)
				.param("email", email)
				.param("mailYn", mailYn)
				.param("phone", phone)
				.param("smsYn", smsYn)
				.param("termsIds", termIds.toArray(String[]::new)))
				.andExpect(jsonPath("$.success").value(true));
	}
}
