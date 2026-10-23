package arkive.admin.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import arkive.support.ArkiveIntegrationTest;

/**
 * V5 권한 시드와 사용자별 메뉴 권한 집계를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class PermissionMapperTest {

	@Autowired
	private PermissionMapper permissionMapper;

	@Test
	void seededRolesShouldExist() throws Exception {
		List<EgovMap> roles = permissionMapper.selectRoleList();
		assertThat(roles).isNotEmpty();
		assertThat(roles).anySatisfy(role -> {
			assertThat(role.get("roleId")).isEqualTo("ROLE_ADMIN");
			assertThat(role.get("roleNm")).isEqualTo("관리자");
		});
		assertThat(roles).anySatisfy(role -> assertThat(role.get("roleId")).isEqualTo("ROLE_USER"));
	}

	@Test
	void defaultUserRoleShouldExcludeSystemAndUploadMenus() throws Exception {
		List<EgovMap> menus = permissionMapper.selectRoleMenuList("ROLE_USER");
		List<String> readableMenuIds = menus.stream()
				.filter(row -> "Y".equals(String.valueOf(row.get("readYn"))))
				.map(row -> String.valueOf(row.get("menuId")))
				.toList();

		assertThat(readableMenuIds).isNotEmpty();
		assertThat(readableMenuIds).noneMatch(id -> id.startsWith("ME0201") || id.startsWith("ME0202"));
		assertThat(readableMenuIds).contains("ME02030100");
	}

	@Test
	void adminShouldSeeAllMenusThroughRole() throws Exception {
		List<EgovMap> permissions = permissionMapper.selectUserMenuPermissions("admin");
		assertThat(permissions).isNotEmpty();
		assertThat(permissions).anySatisfy(row -> assertThat(row.get("menuId")).isEqualTo("ME02010500"));
		assertThat(permissions).anySatisfy(row -> assertThat(row.get("menuId")).isEqualTo("ME02030100"));
	}

	@Test
	void boardMenuReadRolesShouldIncludeAdminAndUser() throws Exception {
		assertThat(permissionMapper.selectRoleIdsWithMenuRead("ME02030100"))
				.contains("ROLE_ADMIN", "ROLE_USER");
	}
}
