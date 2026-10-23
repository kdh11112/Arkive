package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.auth.service.AuthService;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 권한관리 화면과 역할 저장 API를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class RoleManageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PermissionMapper permissionMapper;

	@Autowired
	private AuthService authService;

	@Test
	void adminShouldRenderRoleList() throws Exception {
		mockMvc.perform(get("/auth/roleList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("권한관리")))
				.andExpect(content().string(containsString("ROLE_ADMIN")));
	}

	@Test
	void normalUserShouldNotEnterRoleList() throws Exception {
		mockMvc.perform(get("/auth/roleList").session(TestSessions.user("normal")))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void adminShouldCreateRoleWithMenusAndUsers() throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/saveRole.json")
				.session(TestSessions.admin("admin"))
				.param("mode", "insert")
				.param("roleNm", "FAQ 조회 전용")
				.param("description", "테스트 역할")
				.param("useYn", "Y")
				.param("readMenuIds", "ME02030300")
				.param("writeMenuIds", "ME02030600")
				.param("userIds", "admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.roleId").isNotEmpty())
				.andReturn();

		String roleId = JsonPath.read(result.getResponse().getContentAsString(), "$.roleId");
		EgovMap detail = permissionMapper.selectRoleDetail(roleId);
		assertThat(detail).isNotNull();
		assertThat(detail.get("roleNm")).isEqualTo("FAQ 조회 전용");

		List<EgovMap> menus = permissionMapper.selectRoleMenuList(roleId);
		EgovMap faq = findMenu(menus, "ME02030300");
		assertThat(faq.get("readYn")).isEqualTo("Y");
		assertThat(faq.get("writeYn")).isEqualTo("N");

		// 쓰기만 보내도 조회는 자동으로 함께 부여된다.
		EgovMap popup = findMenu(menus, "ME02030600");
		assertThat(popup.get("readYn")).isEqualTo("Y");
		assertThat(popup.get("writeYn")).isEqualTo("Y");

		assertThat(permissionMapper.selectRoleUserList(roleId))
				.anySatisfy(user -> assertThat(user.get("userId")).isEqualTo("admin"));
	}

	@Test
	void roleNameShouldBeRequired() throws Exception {
		mockMvc.perform(post("/auth/saveRole.json")
				.session(TestSessions.admin("admin"))
				.param("mode", "insert")
				.param("roleNm", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("역할명을 입력하세요."));
	}

	@Test
	void roleShouldNotBeSavedWithUnknownUser() throws Exception {
		mockMvc.perform(post("/auth/saveRole.json")
				.session(TestSessions.admin("admin"))
				.param("mode", "insert")
				.param("roleNm", "잘못된 사용자 역할")
				.param("userIds", "no-such-user"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("존재하지 않거나 사용 중지된 사용자입니다: no-such-user"));

		assertThat(permissionMapper.selectRoleList())
				.noneMatch(role -> "잘못된 사용자 역할".equals(String.valueOf(role.get("roleNm"))));
	}

	@Test
	void deletingUserShouldRemoveRoleMappings() throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", "perm-delete-user");
		user.put("password", "not-used");
		user.put("userNm", "삭제대상");
		user.put("email", null);
		authService.insertUser(user);

		EgovMap userRole = new EgovMap();
		userRole.put("userId", "perm-delete-user");
		userRole.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(userRole);

		mockMvc.perform(post("/auth/deleteUser.json")
				.session(TestSessions.admin("admin"))
				.param("userId", "perm-delete-user"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(authService.selectLoginUser("perm-delete-user")).isNull();
		assertThat(permissionMapper.selectUserRoleIds("perm-delete-user")).isEmpty();
	}

	@Test
	void systemRoleShouldNotBeDeleted() throws Exception {
		mockMvc.perform(post("/auth/deleteRole.json")
				.session(TestSessions.admin("admin"))
				.param("roleId", "ROLE_ADMIN"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("기본 역할은 삭제할 수 없습니다."));
	}

	private EgovMap findMenu(List<EgovMap> menus, String menuId) {
		return menus.stream()
				.filter(menu -> menuId.equals(String.valueOf(menu.get("menuId"))))
				.findFirst()
				.orElseThrow();
	}
}
