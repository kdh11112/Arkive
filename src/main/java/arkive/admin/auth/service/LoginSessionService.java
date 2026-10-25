package arkive.admin.auth.service;

import java.util.ArrayList;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import arkive.admin.audit.service.AuditEvent;
import arkive.admin.auth.web.LoginController;
import arkive.admin.auth.web.LoginSessionRegistry;
import arkive.admin.terms.service.TermsService;
import arkive.admin.terms.web.TermsInterceptor;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 로그인 확정과 TOTP 대기 상태를 한 곳에서 처리한다.
 *
 * <p>비밀번호 로그인, TOTP 확인, 소셜 로그인이 같은 세션 고정 방어·중복 로그인·
 * 필수 약관 확인 규칙을 사용하도록 공통화한다. 인증 수단별 Controller가 세션을
 * 직접 만들지 않게 한다.</p>
 */
@Service("loginSessionService")
public class LoginSessionService {

	public static final String TOTP_PENDING_KEY = "TOTP_PENDING_USER";

	@Resource(name = "loginSessionRegistry")
	private LoginSessionRegistry loginSessionRegistry;

	@Resource(name = "termsService")
	private TermsService termsService;

	@Resource
	private ApplicationEventPublisher auditEventPublisher;

	/**
	 * 인증이 끝난 사용자의 세션을 확정한다. 필수 약관 미동의 상태도 함께 계산한다.
	 *
	 * @return 관리자 여부
	 */
	public boolean establishSession(HttpServletRequest request, EgovMap user) throws Exception {
		return establishSession(request, user, "/login/login.json", "POST");
	}

	public boolean establishSession(HttpServletRequest request, EgovMap user, String requestUri, String httpMethod)
			throws Exception {
		request.getSession(true);
		// 세션 고정 공격 방어: 인증 성공 시 세션 ID를 새로 발급한다.
		request.changeSessionId();
		EgovMap loginUser = new EgovMap();
		loginUser.put("userId", String.valueOf(user.get("userId")));
		loginUser.put("userNm", String.valueOf(user.get("userNm")));
		request.getSession().setAttribute(LoginController.SESSION_KEY, loginUser);
		boolean isAdmin = "Y".equals(String.valueOf(user.get("adminYn")));
		if (isAdmin) {
			EgovMap adminUser = new EgovMap();
			adminUser.put("userId", String.valueOf(user.get("userId")));
			adminUser.put("userNm", String.valueOf(user.get("userNm")));
			request.getSession().setAttribute(LoginController.ADMIN_KEY, adminUser);
		}
		loginSessionRegistry.register(String.valueOf(user.get("userId")), request.getSession());
		refreshPendingTerms(request, String.valueOf(user.get("userId")));
		auditEventPublisher.publishEvent(AuditEvent.of("LOGIN_SUCCESS", String.valueOf(user.get("userId")),
				String.valueOf(user.get("userNm")), null, "USER", String.valueOf(user.get("userId")),
				requestUri, httpMethod, request.getRemoteAddr(), "SUCCESS", null));
		return isAdmin;
	}

	/**
	 * 비밀번호는 통과했지만 TOTP 확인이 남은 상태를 세션에 저장한다.
	 * 이 상태에서는 LOGIN_USER를 만들지 않아 보호된 화면에 접근할 수 없다.
	 */
	public void beginTotpChallenge(HttpServletRequest request, EgovMap user) {
		beginTotpChallenge(request, user, "/login/login.json", "POST");
	}

	public void beginTotpChallenge(HttpServletRequest request, EgovMap user, String requestUri, String httpMethod) {
		request.getSession(true);
		request.changeSessionId();
		EgovMap pending = new EgovMap();
		pending.put("userId", String.valueOf(user.get("userId")));
		pending.put("userNm", String.valueOf(user.get("userNm")));
		pending.put("adminYn", String.valueOf(user.get("adminYn")));
		request.getSession().setAttribute(TOTP_PENDING_KEY, pending);
		request.getSession().removeAttribute(LoginController.TOTP_FAIL_KEY);
		auditEventPublisher.publishEvent(AuditEvent.of("TOTP_CHALLENGE", String.valueOf(user.get("userId")),
				String.valueOf(user.get("userNm")), null, "USER", String.valueOf(user.get("userId")),
				requestUri, httpMethod, request.getRemoteAddr(), "SUCCESS", null));
	}

	public boolean hasPendingTotp(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute(TOTP_PENDING_KEY) != null;
	}

	private void refreshPendingTerms(HttpServletRequest request, String userId) throws Exception {
		List<EgovMap> pending = termsService.selectPendingRequiredTerms(userId);
		if (pending.isEmpty()) {
			request.getSession().removeAttribute(TermsInterceptor.PENDING_TERMS);
			return;
		}
		List<String> ids = new ArrayList<>();
		for (EgovMap term : pending) {
			ids.add(String.valueOf(term.get("termsId")));
		}
		request.getSession().setAttribute(TermsInterceptor.PENDING_TERMS, ids);
	}
}
