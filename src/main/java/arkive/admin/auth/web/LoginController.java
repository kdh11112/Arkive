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

	@Operation(summary = "관리자 로그인 화면 (일반 로그인과 진입·세션 분리)")
	@RequestMapping(value = "/adminLogin", method = { RequestMethod.GET, RequestMethod.POST })
	public String adminLogin(HttpServletRequest request, ModelMap model) {
		model.put("naverEnabled", isOauthEnabled(request, "naver"));
		model.put("kakaoEnabled", isOauthEnabled(request, "kakao"));
		return "login/adminLogin";
	}

	@Operation(summary = "관리자 로그인 처리 (ADMIN 세션 별도)")
	@RequestMapping(value = "/adminLogin.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> adminLoginAction(HttpServletRequest request) {
		Map<String, Object> result = loginAction(request);
		if (Boolean.TRUE.equals(result.get("success"))) {
			EgovMap adminUser = new EgovMap();
			adminUser.put("userId", ((EgovMap) request.getSession().getAttribute(SESSION_KEY)).get("userId"));
			adminUser.put("userNm", ((EgovMap) request.getSession().getAttribute(SESSION_KEY)).get("userNm"));
			request.getSession().setAttribute(ADMIN_KEY, adminUser);
		}
		return result;
	}

	@Operation(summary = "로그인 화면")
	@RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(HttpServletRequest request, ModelMap model) {
		model.put("naverEnabled", isOauthEnabled(request, "naver"));
		model.put("kakaoEnabled", isOauthEnabled(request, "kakao"));
		model.put("googleEnabled", isOauthEnabled(request, "google"));
		return "login/login";
	}

	private boolean isOauthEnabled(HttpServletRequest request, String provider) {
		try {
			org.springframework.web.context.WebApplicationContext ctx =
					org.springframework.web.context.support.WebApplicationContextUtils
							.getWebApplicationContext(request.getServletContext());
			String clientId = ctx.getEnvironment().getProperty("Globals." + provider + ".clientId");
			return clientId != null && !clientId.trim().isEmpty();
		} catch (Exception e) {
			return false;
		}
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
			EgovMap loginUser = new EgovMap();
			loginUser.put("userId", String.valueOf(user.get("userId")));
			loginUser.put("userNm", String.valueOf(user.get("userNm")));
			request.getSession().setAttribute(SESSION_KEY, loginUser);
			result.put("success", true);
			result.put("userNm", loginUser.get("userNm"));
		} catch (Exception e) {
			logger.error("login failed", e);
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
