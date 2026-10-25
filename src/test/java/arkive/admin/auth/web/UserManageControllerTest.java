package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 사용자관리 화면 접근 제한, 등록 검증, 기본 역할, PII 암호화, 잠금해제·비밀번호 초기화를 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class UserManageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void userScreensShouldRequireAdminSession() throws Exception {
		mockMvc.perform(get("/auth/userList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("사용자")));
		mockMvc.perform(get("/auth/userWrite").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("doubleSubmitToken"));

		mockMvc.perform(get("/auth/userList").session(TestSessions.user("normal-user")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login/login"));
		mockMvc.perform(post("/auth/insertUser.json").session(TestSessions.user("normal-user")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void insertShouldValidateInputAndAssignDefaultRole() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/auth/insertUser.json").session(session))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		expectInsertFailure(session, "ab", "Password123!", "이름", "", "N", "", "N",
				"아이디는 영문·숫자 4~20자로 입력하세요.");
		expectInsertFailure(session, "manageuser1", "short", "이름", "", "N", "", "N",
				"비밀번호는 8자 이상 입력하세요.");
		expectInsertFailure(session, "manageuser1", "Password123!", "  ", "", "N", "", "N",
				"이름을 입력하세요.");
		expectInsertFailure(session, "manageuser1", "Password123!", "이름", "", "Y", "", "N",
				"메일 수신에 동의하려면 이메일을 입력하세요.");
		expectInsertFailure(session, "manageuser1", "Password123!", "이름", "bad@x", "N", "", "N",
				"이메일 형식이 올바르지 않습니다.");
		expectInsertFailure(session, "manageuser1", "Password123!", "이름", "", "N", "1234", "Y",
				"SMS 수신에 동의하려면 올바른 휴대전화 번호를 입력하세요.");

		insertUser(session, "manageuser1", "Password123!", "관리대상", "manage1@example.com", "N",
				"010-1234-5678", "Y");

		EgovMap login = authMapper.selectLoginUser("manageuser1");
		assertThat(String.valueOf(login.get("password"))).startsWith("$2");
		assertThat(String.valueOf(login.get("adminYn"))).isEqualTo("N");
		assertThat(String.valueOf(login.get("phone"))).startsWith("enc:v1:");
		assertThat(String.valueOf(login.get("phone"))).doesNotContain("01012345678");
		assertThat(permissionMapper.selectUserRoleIds("manageuser1")).containsExactly("ROLE_USER");

		mockMvc.perform(get("/auth/userWrite").session(session).param("userId", "manageuser1"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("detail",
						org.hamcrest.Matchers.hasEntry("phone", "01012345678")));

		mockMvc.perform(post("/auth/insertUser.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "BOARD_USER"))
				.param("userId", "manageuser1")
				.param("password", "Password123!")
				.param("userNm", "중복"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이미 등록된 아이디입니다."));
	}

	@Test
	void updateShouldChangeFlagsAndDeleteShouldRemoveRoles() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		insertUser(session, "manageuser2", "Password123!", "수정전", "before@example.com", "N",
				"010-1111-2222", "Y");

		mockMvc.perform(post("/auth/updateUser.json").session(session)
				.param("userId", "manageuser2")
				.param("userNm", "수정후")
				.param("email", "after@example.com")
				.param("mailYn", "N")
				.param("smsYn", "Y")
				.param("phone", "010-9999-8888")
				.param("useYn", "N")
				.param("adminYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap detail = authMapper.selectUserDetail("manageuser2");
		assertThat(detail.get("userNm")).isEqualTo("수정후");
		assertThat(detail.get("email")).isEqualTo("after@example.com");
		assertThat(String.valueOf(detail.get("useYn"))).isEqualTo("N");
		assertThat(String.valueOf(detail.get("adminYn"))).isEqualTo("Y");
		assertThat(String.valueOf(detail.get("phone"))).startsWith("enc:v1:");
		assertThat(authMapper.selectLoginUser("manageuser2")).isNull();

		mockMvc.perform(post("/auth/deleteUser.json").session(session).param("userId", "manageuser2"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(authMapper.selectUserDetail("manageuser2")).isNull();
		assertThat(permissionMapper.selectUserRoleIds("manageuser2")).isEmpty();
	}

	@Test
	void unlockAndResetPasswordShouldClearLockAndIssueRandomPassword() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		insertUser(session, "manageuser3", "Password123!", "잠금대상", "", "N", "", "N");
		for (int i = 0; i < 5; i++) {
			authMapper.addFailCount("manageuser3");
		}
		authMapper.lockUser("manageuser3");
		assertThat(String.valueOf(authMapper.selectLoginUser("manageuser3").get("lockYn"))).isEqualTo("Y");

		mockMvc.perform(post("/auth/unlockUser.json").session(session).param("userId", "manageuser3"))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap unlocked = authMapper.selectLoginUser("manageuser3");
		assertThat(String.valueOf(unlocked.get("lockYn"))).isEqualTo("N");
		assertThat(String.valueOf(unlocked.get("failedCnt"))).isEqualTo("0");

		authMapper.lockUser("manageuser3");
		mockMvc.perform(post("/auth/resetPassword.json").session(session).param("userId", "manageuser3"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.tempPassword").value(matchesPattern("[A-Za-z0-9]{10}")));

		EgovMap reset = authMapper.selectLoginUser("manageuser3");
		assertThat(String.valueOf(reset.get("lockYn"))).isEqualTo("N");
		assertThat(String.valueOf(reset.get("password"))).startsWith("$2");

		mockMvc.perform(post("/auth/resetTotp.json").session(session).param("userId", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("사용자 아이디가 필요합니다."));
		mockMvc.perform(post("/auth/resetTotp.json").session(session).param("userId", "manageuser3"))
				.andExpect(jsonPath("$.success").value(true));
	}

	private void expectInsertFailure(MockHttpSession session, String userId, String password, String userNm,
			String email, String mailYn, String phone, String smsYn, String message) throws Exception {
		mockMvc.perform(post("/auth/insertUser.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "BOARD_USER"))
				.param("userId", userId)
				.param("password", password)
				.param("userNm", userNm)
				.param("email", email)
				.param("mailYn", mailYn)
				.param("phone", phone)
				.param("smsYn", smsYn))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(message));
	}

	private void insertUser(MockHttpSession session, String userId, String password, String userNm, String email,
			String mailYn, String phone, String smsYn) throws Exception {
		mockMvc.perform(post("/auth/insertUser.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "BOARD_USER"))
				.param("userId", userId)
				.param("password", password)
				.param("userNm", userNm)
				.param("email", email)
				.param("mailYn", mailYn)
				.param("phone", phone)
				.param("smsYn", smsYn))
				.andExpect(jsonPath("$.success").value(true));
	}
}
