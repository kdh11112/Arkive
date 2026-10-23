package arkive.admin.auth.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashSet;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 메뉴 권한이 화면 숨김만이 아니라 서버에서 강제되는지 확인한다.
 * FAQ 메뉴(ME02030300)를 읽기 전용 역할과 읽기·쓰기 역할로 나눠 호출한다.
 */
@ArkiveIntegrationTest
@Transactional
class MenuPermissionControllerTest {

	private static final String FAQ_MENU = "ME02030300";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PermissionMapper permissionMapper;

	@Test
	void readOnlyRoleShouldReadButNotWrite() throws Exception {
		grantRole("ROLE_TEST_FAQ_RO", "FAQ 조회 전용", new String[] { FAQ_MENU }, new String[0], "faq-viewer");

		mockMvc.perform(get("/faq/faqList").session(TestSessions.user("faq-viewer")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("FAQ관리")));

		mockMvc.perform(post("/faq/updateFaq.json")
				.session(TestSessions.user("faq-viewer"))
				.param("boardId", "NO-SUCH-FAQ"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	void writeRoleShouldPassInterceptor() throws Exception {
		grantRole("ROLE_TEST_FAQ_RW", "FAQ 편집", new String[] { FAQ_MENU }, new String[] { FAQ_MENU }, "faq-editor");

		// 인터셉터를 통과하면 컨트롤러 검증 메시지가 온다(403이 아님).
		mockMvc.perform(post("/faq/updateFaq.json")
				.session(TestSessions.user("faq-editor"))
				.param("boardId", "NO-SUCH-FAQ"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));
	}

	@Test
	void userWithoutRoleShouldBeDenied() throws Exception {
		mockMvc.perform(get("/faq/faqList").session(TestSessions.user("no-role-user")))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminShouldBypassMenuPermission() throws Exception {
		mockMvc.perform(get("/faq/faqList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk());
	}

	@Test
	void sidebarShouldShowOnlyAllowedMenu() throws Exception {
		grantRole("ROLE_TEST_FAQ_MENU", "FAQ 메뉴만", new String[] { FAQ_MENU }, new String[0], "faq-menu-viewer");

		mockMvc.perform(get("/faq/faqList").session(TestSessions.user("faq-menu-viewer")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("FAQ관리")))
				.andExpect(content().string(not(containsString("권한관리"))));
	}

	private void grantRole(String roleId, String roleNm, String[] readMenus, String[] writeMenus, String userId)
			throws Exception {
		EgovMap role = new EgovMap();
		role.put("roleId", roleId);
		role.put("roleNm", roleNm);
		role.put("description", "테스트 역할");
		role.put("useYn", "Y");
		permissionMapper.insertRole(role);

		Set<String> writes = new LinkedHashSet<>();
		for (String menuId : writeMenus) {
			writes.add(menuId);
		}
		Set<String> reads = new LinkedHashSet<>(writes);
		for (String menuId : readMenus) {
			reads.add(menuId);
		}
		for (String menuId : reads) {
			EgovMap menu = new EgovMap();
			menu.put("roleId", roleId);
			menu.put("menuId", menuId);
			menu.put("readYn", "Y");
			menu.put("writeYn", writes.contains(menuId) ? "Y" : "N");
			permissionMapper.insertRoleMenu(menu);
		}

		EgovMap userRole = new EgovMap();
		userRole.put("userId", userId);
		userRole.put("roleId", roleId);
		permissionMapper.insertUserRole(userRole);
	}
}
