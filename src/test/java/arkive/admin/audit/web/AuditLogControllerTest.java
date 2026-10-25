package arkive.admin.audit.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 로그관리가 관리자에게만 열리고 일반 사용자는 거부되는지 확인한다.
 */
@ArkiveIntegrationTest
class AuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminShouldOpenAuditLogList() throws Exception {
		mockMvc.perform(get("/system/auditLogList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(view().name("system/auditLogList"));
	}

	@Test
	void normalUserShouldBeRedirectedToLogin() throws Exception {
		// /system은 기존 ADMIN_YN 슈퍼관리자 관문이 먼저 적용된다.
		mockMvc.perform(get("/system/auditLogList").session(TestSessions.user("tester")))
				.andExpect(status().is3xxRedirection());
	}
}
