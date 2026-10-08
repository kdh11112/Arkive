package arkive.admin.auth.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.auth.service.AuthService;
import arkive.admin.comm.web.CommUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 소셜 로그인(네이버·카카오). Spring Security 없이 표준 code flow를 직접 수행한다.
 * 회원 매핑: 연계 USER가 있으면 세션 로그인, 없으면 기존 ID 연결 또는 신규 생성 후 연계한다.
 * 클라이언트 키·콜백 URL은 Globals에서 읽는다. 미발급이면 버튼이 숨는다.
 */
@Tag(name = "소셜로그인", description = "네이버·카카오 code flow + 회원 매핑")
@Controller
@RequestMapping("/login/oauth")
public class OauthController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "authService")
	private AuthService authService;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Resource
	private Environment env;

	private CommUtil cmmUtil = new CommUtil();

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String SESSION_STATE = "OAUTH_STATE";
	private static final String SESSION_PROFILE = "OAUTH_PROFILE";

	private String prop(String key) {
		String value = env.getProperty(key);
		return value == null ? "" : value.trim();
	}

	private boolean isEnabled(String provider) {
		return !prop("Globals." + provider + ".clientId").isEmpty();
	}

	private boolean isSupported(String provider) {
		return "naver".equals(provider) || "kakao".equals(provider) || "google".equals(provider);
	}

	@Operation(summary = "소셜 로그인 시작 (제공자 authorize로 이동)")
	@RequestMapping(value = "/{provider}", method = { RequestMethod.GET, RequestMethod.POST })
	public String start(@PathVariable("provider") String provider, HttpServletRequest request) throws Exception {
		if (!isSupported(provider)) {
			throw new IllegalArgumentException("지원하지 않는 제공자입니다.");
		}
		if (!isEnabled(provider)) {
			throw new IllegalStateException("발급되지 않은 제공자입니다. 키를 먼저 등록하세요.");
		}
		String state = UUID.randomUUID().toString();
		request.getSession().setAttribute(SESSION_STATE + "_" + provider, state);
		String callback = prop("Globals." + provider + ".callbackUrl");
		String clientId = prop("Globals." + provider + ".clientId");
		String authorizeUrl;
		if ("naver".equals(provider)) {
			authorizeUrl = "https://nid.naver.com/oauth2.0/authorize?response_type=code"
					+ "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
					+ "&redirect_uri=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
					+ "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
		} else if ("kakao".equals(provider)) {
			authorizeUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code"
					+ "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
					+ "&redirect_uri=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
					+ "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
		} else {
			authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code"
					+ "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
					+ "&redirect_uri=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
					+ "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
					+ "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
		}
		return "redirect:" + authorizeUrl;
	}

	@Operation(summary = "소셜 콜백 (code 교환·프로필 조회·매핑 분기)")
	@RequestMapping(value = "/{provider}Callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(@PathVariable("provider") String provider, HttpServletRequest request, ModelMap model)
			throws Exception {
		if (!isSupported(provider)) {
			throw new IllegalArgumentException("지원하지 않는 제공자입니다.");
		}
		String state = cmmUtil.convertHtml(request, "state");
		String sessionState = String.valueOf(request.getSession().getAttribute(SESSION_STATE + "_" + provider));
		request.getSession().removeAttribute(SESSION_STATE + "_" + provider);
		if (state == null || !state.equals(sessionState)) {
			throw new IllegalStateException("비정상적인 접근입니다.");
		}
		String code = cmmUtil.convertHtml(request, "code");
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException("인증 코드가 없습니다.");
		}
		String accessToken = exchangeToken(provider, code, state);
		EgovMap profile = fetchProfile(provider, accessToken);
		String providerId = String.valueOf(profile.get("providerId"));

		EgovMap linkParam = new EgovMap();
		linkParam.put("provider", provider);
		linkParam.put("providerId", providerId);
		EgovMap link = authService.selectOauthLink(linkParam);
		if (link != null) {
			EgovMap user = authService.selectLoginUser(String.valueOf(link.get("userId")));
			if (user == null) {
				throw new IllegalStateException("연계된 계정을 찾을 수 없습니다. 관리자에게 문의하세요.");
			}
			EgovMap loginUser = new EgovMap();
			loginUser.put("userId", String.valueOf(user.get("userId")));
			loginUser.put("userNm", String.valueOf(user.get("userNm")));
			request.getSession().setAttribute(LoginController.SESSION_KEY, loginUser);
			return "redirect:/dashboard";
		}
		profile.put("provider", provider);
		profile.put("providerId", providerId);
		request.getSession().setAttribute(SESSION_PROFILE, profile);
		model.put("provider", provider);
		model.put("email", profile.get("email"));
		model.put("nickname", profile.get("nickname"));
		model.put("naverEnabled", isEnabled("naver"));
		model.put("kakaoEnabled", isEnabled("kakao"));
		model.put("googleEnabled", isEnabled("google"));
		return "login/oauthLink";
	}

	private String exchangeToken(String provider, String code, String state) throws Exception {
		String tokenUrl = "naver".equals(provider)
				? "https://nid.naver.com/oauth2.0/token"
				: "kakao".equals(provider)
						? "https://kauth.kakao.com/oauth/token"
						: "https://oauth2.googleapis.com/token";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", prop("Globals." + provider + ".clientId"));
		body.add("client_secret", prop("Globals." + provider + ".clientSecret"));
		body.add("redirect_uri", prop("Globals." + provider + ".callbackUrl"));
		body.add("code", code);
		body.add("state", state);
		ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl,
				new HttpEntity<>(body, headers), String.class);
		JsonNode node = objectMapper.readTree(response.getBody());
		if (!node.hasNonNull("access_token")) {
			throw new IllegalStateException("토큰 발급에 실패했습니다.");
		}
		return node.get("access_token").asText();
	}

	private EgovMap fetchProfile(String provider, String accessToken) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		String profileUrl = "naver".equals(provider)
				? "https://openapi.naver.com/v1/nid/me"
				: "kakao".equals(provider)
						? "https://kapi.kakao.com/v2/user/me"
						: "https://www.googleapis.com/oauth2/v3/userinfo";
		ResponseEntity<String> response = restTemplate.exchange(profileUrl, HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		JsonNode root = objectMapper.readTree(response.getBody());
		EgovMap profile = new EgovMap();
		if ("naver".equals(provider)) {
			JsonNode r = root.path("response");
			profile.put("providerId", r.path("id").asText(""));
			profile.put("email", r.path("email").asText(""));
			profile.put("nickname", r.path("nickname").asText(""));
		} else if ("kakao".equals(provider)) {
			profile.put("providerId", root.path("id").asText(""));
			profile.put("email", root.path("kakao_account").path("email").asText(""));
			profile.put("nickname", root.path("properties").path("nickname").asText(""));
		} else {
			profile.put("providerId", root.path("sub").asText(""));
			profile.put("email", root.path("email").asText(""));
			profile.put("nickname", root.path("name").asText(""));
		}
		if (String.valueOf(profile.get("providerId")).isEmpty()) {
			throw new IllegalStateException("프로필 조회에 실패했습니다.");
		}
		return profile;
	}

	@Operation(summary = "소셜·내부 계정 연결 (기존 연결 또는 신규 생성)")
	@RequestMapping(value = "/link.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> linkAccount(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			Object sessionProfile = request.getSession().getAttribute(SESSION_PROFILE);
			if (!(sessionProfile instanceof EgovMap)) {
				throw new IllegalStateException("소셜 인증 정보가 없습니다. 다시 시도하세요.");
			}
			EgovMap profile = (EgovMap) sessionProfile;
			String provider = String.valueOf(profile.get("provider"));
			String providerId = String.valueOf(profile.get("providerId"));
			String mode = cmmUtil.convertHtml(request, "mode");
			String userId;
			if ("create".equals(mode)) {
				String userNm = cmmUtil.convertHtml(request, "userNm");
				String password = cmmUtil.convertHtml(request, "password");
				if (userNm == null || userNm.trim().isEmpty()) {
					throw new IllegalArgumentException("이름을 입력하세요.");
				}
				if (password == null || password.length() < 8) {
					throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
				}
				String email = String.valueOf(profile.get("email"));
				userId = ("sns_" + provider + "_" + providerId).toLowerCase();
				if (userId.length() > 20) {
					userId = userId.substring(0, 20);
				}
				if (authService.selectLoginUser(userId) != null) {
					throw new IllegalStateException("이미 연결된 계정입니다.");
				}
				EgovMap param = new EgovMap();
				param.put("userId", userId);
				param.put("password", passwordEncoder.encode(password));
				param.put("userNm", userNm.trim());
				param.put("email", email);
				authService.insertUser(param);
			} else if ("link".equals(mode)) {
				userId = cmmUtil.convertHtml(request, "userId");
				String password = cmmUtil.convertHtml(request, "password");
				EgovMap user = authService.selectLoginUser(userId);
				if (user == null || !passwordEncoder.matches(password == null ? "" : password,
						String.valueOf(user.get("password")))) {
					throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
				}
				userId = String.valueOf(user.get("userId"));
			} else {
				throw new IllegalArgumentException("연결 방식이 올바르지 않습니다.");
			}
			EgovMap linkParam = new EgovMap();
			linkParam.put("provider", provider);
			linkParam.put("providerId", providerId);
			linkParam.put("userId", userId);
			try {
				authService.insertOauthLink(linkParam);
			} catch (Exception e) {
				throw new IllegalStateException("이미 연결된 소셜 계정입니다.");
			}
			EgovMap user = authService.selectLoginUser(userId);
			EgovMap loginUser = new EgovMap();
			loginUser.put("userId", String.valueOf(user.get("userId")));
			loginUser.put("userNm", String.valueOf(user.get("userNm")));
			request.getSession().setAttribute(LoginController.SESSION_KEY, loginUser);
			request.getSession().removeAttribute(SESSION_PROFILE);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("linkAccount failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
