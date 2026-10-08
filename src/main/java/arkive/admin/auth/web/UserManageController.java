package arkive.admin.auth.web;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.service.AuthService;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 사용자관리. 등록·수정·삭제·잠금해제·비밀번호 초기화를 제공한다.
 * 초기화 비밀번호는 고정값을 쓰지 않고 매번 난수로 만들어 1회만 보여준다.
 */
@Tag(name = "사용자", description = "사용자 목록·등록·수정·삭제, 잠금해제·비밀번호 초기화")
@Controller
@RequestMapping("/auth")
public class UserManageController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "authService")
	private AuthService authService;

	@Resource
	private PasswordEncoder passwordEncoder;

	private CommUtil cmmUtil = new CommUtil();

	private static final String TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

	private String makeTempPassword() {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb.append(TEMP_CHARS.charAt(random.nextInt(TEMP_CHARS.length())));
		}
		return sb.toString();
	}

	@Operation(summary = "사용자 목록 (검색 + 10건 페이징)")
	@RequestMapping(value = "/userList", method = { RequestMethod.GET, RequestMethod.POST })
	public String userList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = 1;
		try {
			pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			if (pageIndex < 1) {
				pageIndex = 1;
			}
		} catch (NumberFormatException e) {
			pageIndex = 1;
		}
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);

		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", authService.selectUserList(param));
		paginationInfo.setTotalRecordCount(authService.selectUserListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "auth/userList";
	}

	@Operation(summary = "사용자 쓰기 화면 (userId 있으면 수정)")
	@RequestMapping(value = "/userWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String userWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String userId = cmmUtil.convertHtml(request, "userId");
		if (userId != null && !userId.isEmpty()) {
			model.put("detail", authService.selectUserDetail(userId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_USER"));
		return "auth/userWrite";
	}

	private void checkUserId(String userId) {
		if (userId == null || !userId.matches("^[A-Za-z0-9]{4,20}$")) {
			throw new IllegalArgumentException("아이디는 영문·숫자 4~20자로 입력하세요.");
		}
	}

	@Operation(summary = "사용자 등록")
	@RequestMapping(value = "/insertUser.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertUser(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_USER");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String userId = cmmUtil.convertHtml(request, "userId");
			String password = cmmUtil.convertHtml(request, "password");
			String userNm = cmmUtil.convertHtml(request, "userNm");
			checkUserId(userId);
			if (password == null || password.length() < 8) {
				throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
			}
			if (userNm == null || userNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			if (authService.selectLoginUser(userId) != null) {
				throw new IllegalArgumentException("이미 등록된 아이디입니다.");
			}
			EgovMap param = new EgovMap();
			param.put("userId", userId);
			param.put("password", passwordEncoder.encode(password));
			param.put("userNm", userNm.trim());
			param.put("email", cmmUtil.convertHtml(request, "email"));
			authService.insertUser(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("insertUser failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "사용자 수정")
	@RequestMapping(value = "/updateUser.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateUser(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("userId", cmmUtil.convertHtml(request, "userId"));
			String userNm = cmmUtil.convertHtml(request, "userNm");
			if (userNm == null || userNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			param.put("userNm", userNm.trim());
			param.put("email", cmmUtil.convertHtml(request, "email"));
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			authService.updateUser(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateUser failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "사용자 삭제")
	@RequestMapping(value = "/deleteUser.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteUser(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			authService.deleteUser(cmmUtil.convertHtml(request, "userId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteUser failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "잠금 해제 (실패 초기화 포함)")
	@RequestMapping(value = "/unlockUser.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> unlockUser(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			authService.resetFailCount(cmmUtil.convertHtml(request, "userId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("unlockUser failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "비밀번호 초기화 (난수 1회 발급, 잠금도 해제)")
	@RequestMapping(value = "/resetPassword.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> resetPassword(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userId = cmmUtil.convertHtml(request, "userId");
			String tempPassword = makeTempPassword();
			EgovMap param = new EgovMap();
			param.put("userId", userId);
			param.put("password", passwordEncoder.encode(tempPassword));
			authService.updatePassword(param);
			authService.resetFailCount(userId);
			result.put("success", true);
			result.put("tempPassword", tempPassword);
		} catch (Exception e) {
			logger.error("resetPassword failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
