package arkive.admin.auth.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.service.AuthService;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.mail.service.MailAsyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 로그인·로그아웃. 비밀번호는 bcrypt로만 비교한다.
 * 실패 5회 누적 시 잠그고, 성공하면 초기화한다.
 */
@Tag(name = "로그인", description = "세션 로그인·로그아웃")
@Controller
@RequestMapping("/login")
public class LoginController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String SESSION_KEY = "LOGIN_USER";

	public static final String ADMIN_KEY = "ADMIN_USER";

	private static final int MAX_FAIL = 5;

	@Resource(name = "authService")
	private AuthService authService;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Resource(name = "loginSessionRegistry")
	private LoginSessionRegistry loginSessionRegistry;

	@Resource(name = "permissionService")
	private PermissionService permissionService;

	@Resource(name = "mailAsyncService")
	private MailAsyncService mailAsyncService;

	private CommUtil cmmUtil = new CommUtil();

	private static final java.util.regex.Pattern EMAIL_PATTERN =
			java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	@Operation(summary = "로그인 화면")
	@RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(HttpServletRequest request, ModelMap model) {
		return "login/login";
	}

	@Operation(summary = "로그인 처리 (실패 누적·잠금 포함)")
	@RequestMapping(value = "/login.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> loginAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userId = cmmUtil.convertHtml(request, "userId");
			String password = cmmUtil.convertHtml(request, "password");
			if (userId == null || userId.trim().isEmpty() || password == null || password.isEmpty()) {
				throw new IllegalArgumentException("아이디와 비밀번호를 입력하세요.");
			}
			EgovMap user = authService.selectLoginUser(userId.trim());
			// 아이디·비밀번호 중 무엇이 틀렸는지 드러내지 않는다.
			if (user == null || !"N".equals(String.valueOf(user.get("lockYn")))) {
				if (user != null) {
					throw new IllegalStateException("잠긴 계정입니다. 관리자에게 문의하세요.");
				}
				throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
			}
			if (!passwordEncoder.matches(password, String.valueOf(user.get("password")))) {
				authService.addFailCount(userId.trim());
				EgovMap retry = authService.selectLoginUser(userId.trim());
				int failed = 0;
				try {
					failed = Integer.parseInt(String.valueOf(retry.get("failedCnt")));
				} catch (NumberFormatException e) {
					failed = MAX_FAIL;
				}
				if (failed >= MAX_FAIL) {
					authService.lockUser(userId.trim());
					throw new IllegalStateException("5회 실패로 잠겼습니다. 관리자에게 문의하세요.");
				}
				throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
			}
			authService.resetFailCount(userId.trim());
			// 세션 고정 공격 방어: 인증 성공 시 세션 ID를 새로 발급한다.
			request.getSession(true);
			request.changeSessionId();
			EgovMap loginUser = new EgovMap();
			loginUser.put("userId", String.valueOf(user.get("userId")));
			loginUser.put("userNm", String.valueOf(user.get("userNm")));
			request.getSession().setAttribute(SESSION_KEY, loginUser);
			// 관리자면 ADMIN 세션도 함께 넣어 /system·/auth 관문을 통과시킨다.
			boolean isAdmin = "Y".equals(String.valueOf(user.get("adminYn")));
			if (isAdmin) {
				EgovMap adminUser = new EgovMap();
				adminUser.put("userId", String.valueOf(user.get("userId")));
				adminUser.put("userNm", String.valueOf(user.get("userNm")));
				request.getSession().setAttribute(ADMIN_KEY, adminUser);
			}
			// 중복 로그인 방지: 같은 사용자의 이전 세션을 만료시킨다.
			loginSessionRegistry.register(String.valueOf(user.get("userId")), request.getSession());
			result.put("success", true);
			result.put("userNm", loginUser.get("userNm"));
			result.put("isAdmin", isAdmin);
		} catch (Exception e) {
			logger.error("login failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "회원가입 화면")
	@RequestMapping(value = "/signup", method = { RequestMethod.GET, RequestMethod.POST })
	public String signup() {
		return "login/signup";
	}

	@Operation(summary = "회원가입 처리 (일반 회원으로만 가입, ADMIN_YN 강제 N)")
	@RequestMapping(value = "/signup.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> signupAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userId = cmmUtil.convertHtml(request, "userId");
			String password = cmmUtil.convertHtml(request, "password");
			String userNm = cmmUtil.convertHtml(request, "userNm");
			if (userId == null || !userId.matches("^[A-Za-z0-9]{4,20}$")) {
				throw new IllegalArgumentException("아이디는 영문·숫자 4~20자로 입력하세요.");
			}
			if (password == null || password.length() < 8) {
				throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
			}
			if (userNm == null || userNm.trim().isEmpty()) {
				throw new IllegalArgumentException("이름을 입력하세요.");
			}
			if (authService.selectLoginUser(userId) != null) {
				throw new IllegalArgumentException("이미 등록된 아이디입니다.");
			}
			String email = trim(cmmUtil.convertHtml(request, "email"));
			String mailYn = "Y".equals(cmmUtil.convertHtml(request, "mailYn")) ? "Y" : "N";
			if ("Y".equals(mailYn) && (email == null || email.isEmpty())) {
				throw new IllegalArgumentException("메일 수신에 동의하려면 이메일을 입력하세요.");
			}
			if (email != null && !email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
				throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
			}
			if (email != null && email.length() > 100) {
				throw new IllegalArgumentException("이메일은 100자 이내로 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("userId", userId);
			param.put("password", passwordEncoder.encode(password));
			param.put("userNm", userNm.trim());
			param.put("email", email);
			param.put("mailYn", mailYn);
			authService.insertUser(param);
			// 신규 가입자는 기본 역할을 받아야 권한관리 대상이 된다.
			permissionService.assignDefaultRole(userId);
			if ("Y".equals(mailYn)) {
				mailAsyncService.sendWelcome(userId);
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("signup failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "아이디 찾기 화면")
	@RequestMapping(value = "/findId", method = { RequestMethod.GET, RequestMethod.POST })
	public String findId() {
		return "login/findId";
	}

	@Operation(summary = "아이디 찾기 (이름+이메일, 마스킹 반환)")
	@RequestMapping(value = "/findId.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> findIdAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userNm = cmmUtil.convertHtml(request, "userNm");
			String email = cmmUtil.convertHtml(request, "email");
			if (userNm == null || userNm.trim().isEmpty() || email == null || email.trim().isEmpty()) {
				throw new IllegalArgumentException("이름과 이메일을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("userNm", userNm.trim());
			param.put("email", email.trim());
			EgovMap found = authService.selectFindUserId(param);
			if (found == null) {
				throw new IllegalArgumentException("일치하는 회원 정보가 없습니다.");
			}
			String userId = String.valueOf(found.get("userId"));
			// 앞 3자만 보여주고 나머지는 가린다.
			String masked = userId.length() <= 3
					? userId.charAt(0) + "**"
					: userId.substring(0, 3) + "****";
			result.put("success", true);
			result.put("userId", masked);
		} catch (Exception e) {
			logger.error("findId failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "비밀번호 찾기 화면")
	@RequestMapping(value = "/findPw", method = { RequestMethod.GET, RequestMethod.POST })
	public String findPw() {
		return "login/findPw";
	}

	@Operation(summary = "비밀번호 재설정 메일 요청 (가입 정보가 일치할 때만 1회용 링크 발급)")
	@RequestMapping(value = "/findPw.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> findPwAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String userId = cmmUtil.convertHtml(request, "userId");
			String userNm = cmmUtil.convertHtml(request, "userNm");
			String email = cmmUtil.convertHtml(request, "email");
			if (userId == null || userId.trim().isEmpty()
					|| userNm == null || userNm.trim().isEmpty()
					|| email == null || email.trim().isEmpty()) {
				throw new IllegalArgumentException("아이디·이름·이메일을 입력하세요.");
			}
			EgovMap user = authService.selectLoginUser(userId.trim());
			// 가입 여부·이메일 존재 여부를 응답으로 노출하지 않는다.
			if (user != null
					&& userNm.trim().equals(String.valueOf(user.get("userNm")))
					&& email.trim().equalsIgnoreCase(String.valueOf(user.get("email")))) {
				String token = authService.issuePasswordResetToken(userId.trim());
				if (token != null) {
					mailAsyncService.sendPasswordReset(userId.trim(), token);
				}
			}
			result.put("success", true);
			result.put("message", "입력한 정보가 일치하면 비밀번호 재설정 링크를 이메일로 보냅니다.");
		} catch (Exception e) {
			logger.error("findPw failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "비밀번호 재설정 화면 (메일 링크의 token 사용)")
	@RequestMapping(value = "/resetPw", method = { RequestMethod.GET, RequestMethod.POST })
	public String resetPw(HttpServletRequest request, ModelMap model) throws Exception {
		String token = trim(cmmUtil.convertHtml(request, "token"));
		model.put("token", token == null ? "" : token);
		model.put("valid", token != null && authService.isValidPasswordResetToken(token));
		return "login/resetPw";
	}

	@Operation(summary = "비밀번호 재설정 처리 (1회용 token, 성공 시 기존 세션 만료)")
	@RequestMapping(value = "/resetPw.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> resetPwAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String token = trim(cmmUtil.convertHtml(request, "token"));
			String password = cmmUtil.convertHtml(request, "password");
			String passwordConfirm = cmmUtil.convertHtml(request, "passwordConfirm");
			if (token == null || token.isEmpty()) {
				throw new IllegalArgumentException("재설정 링크가 올바르지 않습니다.");
			}
			if (password == null || password.length() < 8) {
				throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
			}
			if (!password.equals(passwordConfirm)) {
				throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
			}
			String userId = authService.resetPasswordByToken(token, passwordEncoder.encode(password));
			// 비밀번호가 바뀌면 기존 로그인 세션을 즉시 끊는다.
			loginSessionRegistry.invalidate(userId);
			result.put("success", true);
			result.put("message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인하세요.");
		} catch (Exception e) {
			logger.error("resetPw failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "로그아웃")
	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object loginUser = session.getAttribute(SESSION_KEY);
			if (loginUser instanceof EgovMap) {
				loginSessionRegistry.remove(String.valueOf(((EgovMap) loginUser).get("userId")), session);
			}
			session.invalidate();
		}
		return "redirect:/";
	}

	@Operation(summary = "JWT 데모 화면 (버튼으로 발급·호출)")
	@RequestMapping(value = "/apiDemo", method = { RequestMethod.GET, RequestMethod.POST })
	public String apiDemo() {
		return "login/apiDemo";
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}
}
