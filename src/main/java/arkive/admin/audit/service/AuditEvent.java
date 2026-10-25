package arkive.admin.audit.service;

/**
 * 감사 로그 이벤트.
 *
 * <p>업무 코드는 이 객체를 발행하기만 하고, 저장은 {@code AuditLogEventListener}가 맡는다.
 * 비밀번호·주민번호·게시글 본문 같은 원문은 detail에 넣지 않는다.</p>
 */
public record AuditEvent(
		String eventType,
		String userId,
		String userNm,
		String menuId,
		String targetType,
		String targetId,
		String requestUri,
		String httpMethod,
		String clientIp,
		String result,
		String detail) {

	public static AuditEvent loginSuccess(String userId, String userNm, String clientIp) {
		return new AuditEvent("LOGIN_SUCCESS", userId, userNm, null, "USER", userId, "/login/login.json",
				"POST", clientIp, "SUCCESS", null);
	}

	public static AuditEvent loginFail(String userId, String clientIp, String detail) {
		return new AuditEvent("LOGIN_FAIL", userId, null, null, "USER", userId, "/login/login.json",
				"POST", clientIp, "FAIL", detail);
	}

	public static AuditEvent logout(String userId, String userNm, String clientIp) {
		return new AuditEvent("LOGOUT", userId, userNm, null, "USER", userId, "/login/logout",
				"GET", clientIp, "SUCCESS", null);
	}

	public static AuditEvent apiLogin(String userId, String userNm, String clientIp) {
		return new AuditEvent("API_LOGIN", userId, userNm, null, "USER", userId, "/api/login.json",
				"POST", clientIp, "SUCCESS", null);
	}

	public static AuditEvent access(String eventType, String userId, String userNm, String menuId,
			String targetType, String targetId, String requestUri, String httpMethod, String clientIp) {
		return new AuditEvent(eventType, userId, userNm, menuId, targetType, targetId, requestUri,
				httpMethod, clientIp, "SUCCESS", null);
	}

	public static AuditEvent of(String eventType, String userId, String userNm, String menuId,
			String targetType, String targetId, String requestUri, String httpMethod, String clientIp,
			String result, String detail) {
		return new AuditEvent(eventType, userId, userNm, menuId, targetType, targetId, requestUri,
				httpMethod, clientIp, result, detail);
	}
}
