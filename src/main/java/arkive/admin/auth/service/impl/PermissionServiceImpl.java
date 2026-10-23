package arkive.admin.auth.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.MenuPermission;
import arkive.admin.auth.service.PermissionService;
import jakarta.annotation.Resource;

/**
 * 역할·메뉴 권한 서비스 구현.
 *
 * <p>권한은 역할 단위로만 부여한다. 사용자별 예외 권한을 두지 않아
 * "이 사용자만 특별히" 같은 추적이 어려운 예외를 만들지 않는다.
 */
@Service("permissionService")
public class PermissionServiceImpl extends EgovAbstractServiceImpl implements PermissionService {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String ROLE_USER = "ROLE_USER";

	@Resource(name = "permissionMapper")
	private PermissionMapper permissionMapper;

	@Override
	public List<EgovMap> selectRoleList() throws Exception {
		return permissionMapper.selectRoleList();
	}

	@Override
	public EgovMap selectRoleDetail(String roleId) throws Exception {
		if (isBlank(roleId)) {
			return null;
		}
		return permissionMapper.selectRoleDetail(roleId.trim());
	}

	@Override
	public List<EgovMap> selectRoleMenuList(String roleId) throws Exception {
		return permissionMapper.selectRoleMenuList(isBlank(roleId) ? "" : roleId.trim());
	}

	@Override
	public List<EgovMap> selectRoleUserList(String roleId) throws Exception {
		return permissionMapper.selectRoleUserList(isBlank(roleId) ? "" : roleId.trim());
	}

	@Override
	public List<EgovMap> selectAllUserList() throws Exception {
		return permissionMapper.selectAllUserList();
	}

	@Override
	@Transactional
	public String saveRole(boolean insert, EgovMap role, List<String> readMenuIds, List<String> writeMenuIds,
			List<String> userIds) throws Exception {
		if (role == null) {
			throw new IllegalArgumentException("역할 정보가 없습니다.");
		}
		String roleNm = trim(String.valueOf(role.get("roleNm") == null ? "" : role.get("roleNm")));
		if (roleNm.isEmpty()) {
			throw new IllegalArgumentException("역할명을 입력하세요.");
		}
		if (roleNm.length() > 100) {
			throw new IllegalArgumentException("역할명은 100자 이내로 입력하세요.");
		}
		String description = trim(String.valueOf(role.get("description") == null ? "" : role.get("description")));
		if (description.length() > 500) {
			throw new IllegalArgumentException("설명은 500자 이내로 입력하세요.");
		}
		String useYn = "N".equals(String.valueOf(role.get("useYn"))) ? "N" : "Y";

		Set<String> userIdSet = normalizeIds(userIds);
		for (String userId : userIdSet) {
			if (permissionMapper.countActiveUser(userId) == 0) {
				throw new IllegalArgumentException("존재하지 않거나 사용 중지된 사용자입니다: " + userId);
			}
		}

		String roleId = trim(String.valueOf(role.get("roleId") == null ? "" : role.get("roleId")));
		if (insert) {
			roleId = makeRoleId();
		} else {
			if (roleId.isEmpty()) {
				throw new IllegalArgumentException("수정할 역할 ID가 없습니다.");
			}
			if (permissionMapper.selectRoleDetail(roleId) == null) {
				throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
			}
			if (ROLE_ADMIN.equals(roleId) || ROLE_USER.equals(roleId)) {
				// 시스템 역할도 이름·설명·사용여부는 바꿀 수 있지만 ID는 고정이다.
				roleId = roleId.trim();
			}
		}

		EgovMap roleParam = new EgovMap();
		roleParam.put("roleId", roleId);
		roleParam.put("roleNm", roleNm);
		roleParam.put("description", description);
		roleParam.put("useYn", useYn);
		if (insert) {
			permissionMapper.insertRole(roleParam);
		} else {
			permissionMapper.updateRole(roleParam);
		}

		// 메뉴 권한은 전체 교체한다. WRITE는 READ를 포함하도록 보정한다.
		Set<String> writeSet = normalizeIds(writeMenuIds);
		Set<String> readSet = normalizeIds(readMenuIds);
		readSet.addAll(writeSet);

		permissionMapper.deleteRoleMenus(roleId);
		for (String menuId : readSet) {
			EgovMap menuParam = new EgovMap();
			menuParam.put("roleId", roleId);
			menuParam.put("menuId", menuId);
			menuParam.put("readYn", "Y");
			menuParam.put("writeYn", writeSet.contains(menuId) ? "Y" : "N");
			permissionMapper.insertRoleMenu(menuParam);
		}

		// 사용자 매핑도 전체 교체한다.
		permissionMapper.deleteRoleUsers(roleId);
		for (String userId : userIdSet) {
			EgovMap userParam = new EgovMap();
			userParam.put("roleId", roleId);
			userParam.put("userId", userId);
			permissionMapper.insertRoleUser(userParam);
		}
		return roleId;
	}

	@Override
	@Transactional
	public void deleteRole(String roleId) throws Exception {
		if (isBlank(roleId)) {
			throw new IllegalArgumentException("삭제할 역할 ID가 없습니다.");
		}
		String id = roleId.trim();
		if (ROLE_ADMIN.equals(id) || ROLE_USER.equals(id)) {
			throw new IllegalArgumentException("기본 역할은 삭제할 수 없습니다.");
		}
		if (permissionMapper.selectRoleDetail(id) == null) {
			throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
		}
		permissionMapper.deleteRoleMenus(id);
		permissionMapper.deleteRoleUsers(id);
		permissionMapper.deleteRole(id);
	}

	@Override
	public List<String> selectUserRoleIds(String userId) throws Exception {
		if (isBlank(userId)) {
			return new ArrayList<>();
		}
		List<String> roles = permissionMapper.selectUserRoleIds(userId.trim());
		return roles == null ? new ArrayList<>() : roles;
	}

	@Override
	@Transactional
	public void saveUserRoles(String userId, List<String> roleIds) throws Exception {
		if (isBlank(userId)) {
			throw new IllegalArgumentException("사용자 ID가 없습니다.");
		}
		String id = userId.trim();
		if (permissionMapper.countActiveUser(id) == 0) {
			throw new IllegalArgumentException("존재하지 않거나 사용 중지된 사용자입니다: " + id);
		}
		Set<String> ids = normalizeIds(roleIds);
		for (String roleId : ids) {
			if (permissionMapper.selectRoleDetail(roleId) == null) {
				throw new IllegalArgumentException("존재하지 않는 역할입니다: " + roleId);
			}
		}
		permissionMapper.deleteUserRoles(id);
		for (String roleId : ids) {
			EgovMap param = new EgovMap();
			param.put("userId", id);
			param.put("roleId", roleId);
			permissionMapper.insertUserRole(param);
		}
	}

	@Override
	@Transactional
	public void assignDefaultRole(String userId) throws Exception {
		if (isBlank(userId)) {
			return;
		}
		if (selectUserRoleIds(userId).isEmpty()) {
			List<String> roles = new ArrayList<>();
			roles.add(ROLE_USER);
			saveUserRoles(userId, roles);
		}
	}

	@Override
	public Map<String, MenuPermission> selectUserMenuPermissions(String userId) throws Exception {
		Map<String, MenuPermission> result = new LinkedHashMap<>();
		if (isBlank(userId)) {
			return result;
		}
		List<EgovMap> rows = permissionMapper.selectUserMenuPermissions(userId.trim());
		if (rows == null) {
			return result;
		}
		for (EgovMap row : rows) {
			Object rawMenuId = row.get("menuId");
			if (rawMenuId == null) {
				continue;
			}
			String menuId = String.valueOf(rawMenuId);
			boolean read = "Y".equals(String.valueOf(row.get("readYn")));
			boolean write = "Y".equals(String.valueOf(row.get("writeYn")));
			result.put(menuId, new MenuPermission(read, write));
		}
		return result;
	}

	@Override
	public Set<String> selectRoleIdsWithMenuRead(String menuId) throws Exception {
		Set<String> result = new LinkedHashSet<>();
		if (isBlank(menuId)) {
			return result;
		}
		List<String> rows = permissionMapper.selectRoleIdsWithMenuRead(menuId.trim());
		if (rows != null) {
			result.addAll(rows);
		}
		return result;
	}

	private String makeRoleId() throws Exception {
		for (int i = 0; i < 5; i++) {
			String roleId = "ROLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
			if (permissionMapper.countRoleId(roleId) == 0) {
				return roleId;
			}
		}
		throw new IllegalStateException("역할 ID를 생성하지 못했습니다. 다시 시도하세요.");
	}

	private Set<String> normalizeIds(List<String> ids) {
		Set<String> result = new LinkedHashSet<>();
		if (ids == null) {
			return result;
		}
		for (String id : ids) {
			if (id != null && !id.trim().isEmpty()) {
				result.add(id.trim());
			}
		}
		return result;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
