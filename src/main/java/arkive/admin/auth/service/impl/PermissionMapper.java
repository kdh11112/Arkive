package arkive.admin.auth.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 역할·메뉴 권한 MyBatis 매퍼. SQL은 Permission_SQL.xml에 있다.
 */
@EgovMapper("permissionMapper")
public interface PermissionMapper {

	List<EgovMap> selectRoleList() throws Exception;

	EgovMap selectRoleDetail(String roleId) throws Exception;

	int countRoleId(String roleId) throws Exception;

	int countActiveUser(String userId) throws Exception;

	void insertRole(EgovMap egovMap) throws Exception;

	void updateRole(EgovMap egovMap) throws Exception;

	void deleteRole(String roleId) throws Exception;

	void deleteRoleMenus(String roleId) throws Exception;

	void deleteRoleUsers(String roleId) throws Exception;

	void insertRoleMenu(EgovMap egovMap) throws Exception;

	void insertRoleUser(EgovMap egovMap) throws Exception;

	List<EgovMap> selectRoleMenuList(String roleId) throws Exception;

	List<EgovMap> selectRoleUserList(String roleId) throws Exception;

	List<EgovMap> selectAllUserList() throws Exception;

	List<String> selectUserRoleIds(String userId) throws Exception;

	void deleteUserRoles(String userId) throws Exception;

	void insertUserRole(EgovMap egovMap) throws Exception;

	List<EgovMap> selectUserMenuPermissions(String userId) throws Exception;

	List<String> selectRoleIdsWithMenuRead(String menuId) throws Exception;

	/**
	 * 게시글 알림 대상: 활성·메일 수신 동의 사용자 중 해당 메뉴를 조회할 수 있는 사용자.
	 * 관리자(ADMIN_YN='Y')는 역할 매핑과 무관하게 포함한다.
	 */
	List<EgovMap> selectBoardMailRecipients(String menuId) throws Exception;
}
