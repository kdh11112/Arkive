package arkive.admin.auth.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.service.PermissionService;
import arkive.admin.comm.web.CommUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 권한관리. 역할을 만들고 메뉴별 조회·쓰기와 사용자 배정을 관리한다.
 *
 * <p>화면에서 숨기는 것과 별개로 MenuPermissionInterceptor가 서버에서 다시 검사한다.
 * 컨트롤러마다 권한 분기를 넣지 않고, 컨트롤러→메뉴 매핑은 인터셉터 한 곳에 모은다.
 */
@Tag(name = "권한관리", description = "역할·메뉴 권한·사용자 역할 배정")
@Controller
@RequestMapping("/auth")
public class RoleManageController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String ROLE_USER = "ROLE_USER";

	@Resource(name = "permissionService")
	private PermissionService permissionService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "역할 목록")
	@RequestMapping(value = "/roleList", method = { RequestMethod.GET, RequestMethod.POST })
	public String roleList(ModelMap model) throws Exception {
		model.put("resultList", permissionService.selectRoleList());
		return "auth/roleList";
	}

	@Operation(summary = "역할 등록·수정 화면 (메뉴 권한·사용자 배정 포함)")
	@RequestMapping(value = "/roleWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String roleWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String roleId = cmmUtil.convertHtml(request, "roleId");
		if (roleId == null) {
			roleId = "";
		}
		EgovMap detail = null;
		if (!roleId.trim().isEmpty()) {
			detail = permissionService.selectRoleDetail(roleId);
			if (detail == null) {
				throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
			}
		}
		Set<String> selectedUserIds = new LinkedHashSet<>();
		for (EgovMap user : permissionService.selectRoleUserList(roleId)) {
			selectedUserIds.add(String.valueOf(user.get("userId")));
		}
		model.put("detail", detail);
		model.put("roleId", roleId.trim());
		model.put("menuList", permissionService.selectRoleMenuList(roleId));
		model.put("userList", permissionService.selectAllUserList());
		model.put("selectedUserIds", selectedUserIds);
		model.put("systemRole", ROLE_ADMIN.equals(roleId) || ROLE_USER.equals(roleId));
		return "auth/roleWrite";
	}

	@Operation(summary = "역할 저장 (메뉴·사용자 매핑 전체 교체)")
	@RequestMapping(value = "/saveRole.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveRole(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String mode = cmmUtil.convertHtml(request, "mode");
			boolean insert = !"update".equals(mode);
			String roleId = cmmUtil.convertHtml(request, "roleId");
			EgovMap role = new EgovMap();
			role.put("roleId", roleId);
			role.put("roleNm", cmmUtil.convertHtml(request, "roleNm"));
			role.put("description", cmmUtil.convertHtml(request, "description"));
			role.put("useYn", "N".equals(cmmUtil.convertHtml(request, "useYn")) ? "N" : "Y");

			String savedRoleId = permissionService.saveRole(insert, role,
					toList(request.getParameterValues("readMenuIds")),
					toList(request.getParameterValues("writeMenuIds")),
					toList(request.getParameterValues("userIds")));
			result.put("success", true);
			result.put("roleId", savedRoleId);
		} catch (Exception e) {
			logger.error("saveRole failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "역할 삭제 (기본 역할 제외)")
	@RequestMapping(value = "/deleteRole.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteRole(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			permissionService.deleteRole(cmmUtil.convertHtml(request, "roleId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteRole failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private List<String> toList(String[] values) {
		List<String> result = new ArrayList<>();
		if (values == null) {
			return result;
		}
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				result.add(value.trim());
			}
		}
		return result;
	}
}
