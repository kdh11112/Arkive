package arkive.admin.auth.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 역할·메뉴 권한 서비스.
 *
 * <p>ADMIN_YN='Y'인 슈퍼관리자는 이 서비스의 권한 계산과 무관하게 통과한다.
 * 일반 사용자는 부여된 역할의 메뉴 권한을 합집합(OR)으로 판단한다.
 */
public interface PermissionService {

	List<EgovMap> selectRoleList() throws Exception;

	EgovMap selectRoleDetail(String roleId) throws Exception;

	List<EgovMap> selectRoleMenuList(String roleId) throws Exception;

	List<EgovMap> selectRoleUserList(String roleId) throws Exception;

	List<EgovMap> selectAllUserList() throws Exception;

	/**
	 * 역할을 등록/수정하고 메뉴·사용자 매핑을 한 트랜잭션으로 교체한다.
	 *
	 * @param insert true면 roleId를 새로 만들고, false면 role.roleId를 수정한다.
	 * @return 저장된 roleId
	 */
	String saveRole(boolean insert, EgovMap role, List<String> readMenuIds, List<String> writeMenuIds,
			List<String> userIds) throws Exception;

	void deleteRole(String roleId) throws Exception;

	List<String> selectUserRoleIds(String userId) throws Exception;

	void saveUserRoles(String userId, List<String> roleIds) throws Exception;

	/** 역할이 하나도 없을 때만 ROLE_USER를 부여한다. */
	void assignDefaultRole(String userId) throws Exception;

	/** 사용자의 메뉴별 유효 권한. 역할이 없으면 빈 맵이다. */
	Map<String, MenuPermission> selectUserMenuPermissions(String userId) throws Exception;

	/** 해당 메뉴를 조회할 수 있는 역할 ID 목록(SSE 대상 필터). */
	Set<String> selectRoleIdsWithMenuRead(String menuId) throws Exception;
}
