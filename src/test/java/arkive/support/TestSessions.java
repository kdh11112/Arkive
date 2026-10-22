package arkive.support;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.mock.web.MockHttpSession;

import arkive.admin.auth.web.LoginController;

/**
 * 로그인 세션 테스트 헬퍼.
 * 대상 프로젝트로 복사할 때 이 파일의 세션 키와 사용자 형태만 바꾸면
 * Controller 테스트 여러 개를 한 번에 맞출 수 있다.
 */
public final class TestSessions {

	private TestSessions() {
	}

	public static MockHttpSession user(String userId) {
		return user(userId, userId + "-name");
	}

	public static MockHttpSession user(String userId, String userNm) {
		return session(userId, userNm, false);
	}

	public static MockHttpSession admin(String userId) {
		return admin(userId, userId + "-name");
	}

	public static MockHttpSession admin(String userId, String userNm) {
		return session(userId, userNm, true);
	}

	private static MockHttpSession session(String userId, String userNm, boolean admin) {
		MockHttpSession session = new MockHttpSession();
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("userNm", userNm);
		session.setAttribute(LoginController.SESSION_KEY, user);
		if (admin) {
			session.setAttribute(LoginController.ADMIN_KEY, user);
		}
		return session;
	}
}
