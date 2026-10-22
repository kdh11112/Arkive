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
import arkive.admin.comm.web.CommUtil;
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

	private CommUtil cmmUtil = new CommUtil();

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
			EgovMap param = new EgovMap();
			param.put("userId", userId);
			param.put("password", passwordEncoder.encode(password));
			param.put("userNm", userNm.trim());
			param.put("email", cmmUtil.convertHtml(request, "email"));
			authService.insertUser(param);
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

	@Operation(summary = "비밀번호 찾기 (임시 비밀번호 1회 발급, 잠금도 함께 해제)")
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
			if (user == null
					|| !userNm.trim().equals(String.valueOf(user.get("userNm")))
					|| !email.trim().equalsIgnoreCase(String.valueOf(user.get("email")))) {
				throw new IllegalArgumentException("일치하는 회원 정보가 없습니다.");
			}
			// 영문·숫자 10자리 임시 비밀번호. 화면에 1회만 보여준다.
			String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
			java.security.SecureRandom random = new java.security.SecureRandom();
			StringBuilder tempPw = new StringBuilder();
			for (int i = 0; i < 10; i++) {
				tempPw.append(chars.charAt(random.nextInt(chars.length())));
			}
			EgovMap param = new EgovMap();
			param.put("userId", userId.trim());
			param.put("password", passwordEncoder.encode(tempPw.toString()));
			authService.updatePassword(param);
			authService.resetFailCount(userId.trim());
			result.put("success", true);
			result.put("tempPw", tempPw.toString());
		} catch (Exception e) {
			logger.error("findPw failed", e);
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
			session.invalidate();
		}
		return "redirect:/";
	}

	@Operation(summary = "JWT 데모 화면 (버튼으로 발급·호출)")
	@RequestMapping(value = "/apiDemo", method = { RequestMethod.GET, RequestMethod.POST })
	public String apiDemo() {
		return "login/apiDemo";
	}
}
