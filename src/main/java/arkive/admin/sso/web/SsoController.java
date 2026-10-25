package arkive.admin.sso.web;

import java.util.Locale;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.auth.service.AuthService;
import arkive.admin.auth.service.LoginSessionService;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.comm.service.ExternalHttpClient;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.totp.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 외부 SSO 연계. OAuth2 authorization code flow를 가정한 샘플 구현이다.
 *
 * <p>외부 SSO 서버의 실제 계약(토큰 발급·사용자 정보 필드·글로벌 로그아웃)은
 * 확인되지 않았다. URL과 사용자 정보 필드는 설정값으로 분리했고, 기본값은
 * 비활성화다. 연계 계정은 OAUTH_LINK(PROVIDER='sso')에 저장하며, 기본 정책은
 * 이미 연결된 계정만 로그인시키고 자동 가입은 하지 않는다.</p>
 */
@Tag(name = "SSO연계", description = "외부 SSO authorization code flow")
@Controller
@RequestMapping("/sso")
public class SsoController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SESSION_STATE = "SSO_STATE";
	private static final String PROVIDER = "sso";

	@Value("${arkive.sso.enabled:false}")
	private boolean enabled;

	@Value("${arkive.sso.authorize-url:}")
	private String authorizeUrl;

	@Value("${arkive.sso.token-url:}")
	private String tokenUrl;

	@Value("${arkive.sso.user-info-url:}")
	private String userInfoUrl;

	@Value("${arkive.sso.client-id:}")
	private String clientId;

	@Value("${arkive.sso.client-secret:}")
	private String clientSecret;

	@Value("${arkive.sso.callback-url:http://localhost:8080/sso/callback}")
	private String callbackUrl;

	@Value("${arkive.sso.scope:}")
	private String scope;

	@Value("${arkive.sso.user-id-field:sub}")
	private String userIdField;

	@Value("${arkive.sso.user-name-field:name}")
	private String userNameField;

	@Value("${arkive.sso.user-email-field:email}")
	private String userEmailField;

	@Value("${arkive.sso.auto-create:false}")
	private boolean autoCreate;

	@Value("${arkive.sso.user-prefix:sso_}")
	private String userPrefix;

	@Resource(name = "authService")
	private AuthService authService;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Resource(name = "permissionService")
	private PermissionService permissionService;

	@Resource(name = "loginSessionService")
	private LoginSessionService loginSessionService;

	@Resource(name = "totpService")
	private TotpService totpService;

	@Resource(name = "externalHttpClient")
	private ExternalHttpClient externalHttpClient;

	private CommUtil cmmUtil = new CommUtil();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Operation(summary = "SSO 로그인 시작")
	@RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(HttpServletRequest request, HttpServletResponse response) {
		if (!available(response)) {
			return null;
		}
		String state = UUID.randomUUID().toString();
		request.getSession(true).setAttribute(SESSION_STATE, state);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizeUrl)
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", callbackUrl)
				.queryParam("state", state);
		if (!scope.isBlank()) {
			builder.queryParam("scope", scope.trim());
		}
		return "redirect:" + builder.build().encode().toUriString();
	}

	@Operation(summary = "SSO 콜백 (코드 교환·사용자 연결·세션 로그인)")
	@RequestMapping(value = "/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
		if (!available(response)) {
			return null;
		}
		try {
			String state = cmmUtil.convertHtml(request, "state");
			String savedState = String.valueOf(request.getSession().getAttribute(SESSION_STATE));
			request.getSession().removeAttribute(SESSION_STATE);
			if (state == null || !state.equals(savedState)) {
				throw new IllegalStateException("비정상적인 접근입니다. SSO 로그인을 다시 시도하세요.");
			}
			String code = cmmUtil.convertHtml(request, "code");
			if (code == null || code.trim().isEmpty()) {
				throw new IllegalArgumentException("인증 코드가 없습니다.");
			}

			String accessToken = exchangeToken(code);
			EgovMap profile = fetchProfile(accessToken);
			String externalId = trim(profile.get("providerId"));
			if (externalId.isEmpty()) {
				throw new IllegalStateException("SSO 사용자 식별자를 확인하지 못했습니다.");
			}

			EgovMap linkParam = new EgovMap();
			linkParam.put("provider", PROVIDER);
			linkParam.put("providerId", externalId);
			EgovMap link = authService.selectOauthLink(linkParam);
			String userId;
			if (link != null) {
				userId = trim(link.get("userId"));
			} else if (autoCreate) {
				userId = createLinkedUser(externalId, profile);
			} else {
				throw new IllegalStateException("연계된 계정이 없습니다. 관리자에게 계정 연결을 요청하세요.");
			}

			EgovMap user = authService.selectLoginUser(userId);
			if (user == null) {
				throw new IllegalStateException("연계된 계정을 찾을 수 없습니다. 관리자에게 문의하세요.");
			}
			permissionService.assignDefaultRole(userId);
			if (totpService.isEnabled(userId)) {
				loginSessionService.beginTotpChallenge(request, user, request.getRequestURI(), request.getMethod());
				return "redirect:/login/login?totp=1";
			}
			boolean isAdmin = loginSessionService.establishSession(request, user,
					request.getRequestURI(), request.getMethod());
			return isAdmin ? "redirect:/dashboard" : "redirect:/user/userMain";
		} catch (IllegalArgumentException | IllegalStateException e) {
			logger.warn("sso callback rejected: {}", e.getMessage());
			model.put("errorMessage", e.getMessage());
			return "sso/ssoError";
		} catch (Exception e) {
			logger.error("sso callback failed", e);
			model.put("errorMessage", "SSO 인증 처리 중 오류가 발생했습니다. 잠시 뒤 다시 시도하세요.");
			return "sso/ssoError";
		}
	}

	private boolean available(HttpServletResponse response) {
		if (!enabled) {
			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (Exception e) {
				logger.warn("sso disabled response failed");
			}
			return false;
		}
		if (authorizeUrl.isBlank() || tokenUrl.isBlank() || userInfoUrl.isBlank()
				|| clientId.isBlank() || clientSecret.isBlank()) {
			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (Exception e) {
				logger.warn("sso misconfigured response failed");
			}
			return false;
		}
		return true;
	}

	private String exchangeToken(String code) throws Exception {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", callbackUrl);
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		String tokenResponse = externalHttpClient.postForm(tokenUrl, form);
		JsonNode node = objectMapper.readTree(tokenResponse);
		if (!node.hasNonNull("access_token")) {
			throw new IllegalStateException("SSO 토큰 발급에 실패했습니다.");
		}
		return node.get("access_token").asText();
	}

	private EgovMap fetchProfile(String accessToken) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		String body = externalHttpClient.get(userInfoUrl, headers);
		JsonNode node = objectMapper.readTree(body);
		EgovMap profile = new EgovMap();
		profile.put("providerId", node.path(userIdField).asText(""));
		profile.put("userNm", node.path(userNameField).asText(""));
		profile.put("email", node.path(userEmailField).asText(""));
		return profile;
	}

	private String createLinkedUser(String externalId, EgovMap profile) throws Exception {
		String userId = buildUserId(externalId);
		if (authService.selectLoginUser(userId) != null) {
			throw new IllegalStateException("SSO 사용자 ID가 기존 계정과 충돌합니다. 관리자에게 계정 연결을 요청하세요.");
		}
		String userNm = trim(profile.get("userNm"));
		if (userNm.isEmpty()) {
			userNm = externalId;
		}
		if (userNm.length() > 100) {
			userNm = userNm.substring(0, 100);
		}
		String email = trim(profile.get("email"));
		if (email.isEmpty() || email.length() > 100 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			email = null;
		}

		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("password", passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
		param.put("userNm", userNm);
		param.put("email", email);
		param.put("mailYn", "N");
		param.put("phone", null);
		param.put("smsYn", "N");
		authService.insertUser(param);
		permissionService.assignDefaultRole(userId);

		EgovMap linkParam = new EgovMap();
		linkParam.put("provider", PROVIDER);
		linkParam.put("providerId", externalId);
		linkParam.put("userId", userId);
		authService.insertOauthLink(linkParam);
		return userId;
	}

	private String buildUserId(String externalId) {
		String prefix = userPrefix == null ? "" : userPrefix.trim().toLowerCase(Locale.ROOT);
		String value = (prefix + externalId).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
		if (value.length() > 20) {
			value = value.substring(0, 20);
		}
		if (value.isEmpty()) {
			throw new IllegalStateException("SSO 사용자 ID를 만들 수 없습니다. 관리자에게 문의하세요.");
		}
		return value;
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
