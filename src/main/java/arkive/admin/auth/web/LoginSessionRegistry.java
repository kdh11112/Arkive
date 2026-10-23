package arkive.admin.auth.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * 중복 로그인 방지용 세션 레지스트리.
 *
 * <p>사용자별 최신 세션 1개만 유지한다. 같은 사용자가 새로 로그인하면 이전 세션을
 * 만료시켜 먼저 로그인한 브라우저를 로그아웃 상태로 만든다(새 로그인 우선 정책).
 * 세션 만료·로그아웃 시 {@link HttpSessionListener} 콜백으로 매핑을 제거한다.</p>
 *
 * <p>이 구현은 단일 애플리케이션 인스턴스 기준이다. 다중 인스턴스로 확장할 때는
 * 세션 저장소(예: Spring Session)와 공유 레지스트리 또는 스티키 세션이 필요하다.</p>
 */
@Component("loginSessionRegistry")
public class LoginSessionRegistry implements HttpSessionListener {

	private static final Logger logger = LoggerFactory.getLogger(LoginSessionRegistry.class);

	private final Map<String, HttpSession> sessionsByUser = new ConcurrentHashMap<>();

	/**
	 * 사용자 세션을 등록하고, 같은 사용자의 이전 세션이 있으면 만료시킨다.
	 * 같은 세션으로 다른 사용자가 로그인한 경우 이전 사용자 매핑도 함께 지운다.
	 */
	public void register(String userId, HttpSession session) {
		if (userId == null || userId.isEmpty() || session == null) {
			return;
		}
		// 한 브라우저 세션에서 계정을 바꿔 로그인하면 이전 사용자 매핑이 남지 않게 한다.
		sessionsByUser.entrySet().removeIf(entry ->
				!userId.equals(entry.getKey()) && entry.getValue() == session);

		HttpSession previous = sessionsByUser.put(userId, session);
		boolean sameSession = previous == session;
		if (previous != null && !sameSession) {
			try {
				sameSession = previous.getId().equals(session.getId());
			} catch (IllegalStateException e) {
				sameSession = false;
			}
		}
		if (previous != null && !sameSession) {
			try {
				previous.invalidate();
			} catch (IllegalStateException e) {
				// 이미 만료된 세션이다. 매핑은 새 세션으로 교체됐으므로 그대로 둔다.
				logger.debug("previous session already invalidated: {}", userId);
			}
		}
	}

	/** 로그아웃 시 현재 세션 매핑을 제거한다. */
	public void remove(String userId, HttpSession session) {
		if (userId == null || session == null) {
			return;
		}
		sessionsByUser.remove(userId, session);
	}

	/** 현재 로그인 세션 조회. 테스트·진단용이며 없으면 null. */
	public HttpSession currentSession(String userId) {
		return userId == null ? null : sessionsByUser.get(userId);
	}

	/**
	 * 비밀번호 재설정 등 보안 이벤트 시 해당 사용자의 현재 세션을 즉시 만료한다.
	 * 세션이 이미 만료됐거나 없으면 조용히 넘어간다.
	 */
	public void invalidate(String userId) {
		if (userId == null || userId.isEmpty()) {
			return;
		}
		HttpSession session = sessionsByUser.remove(userId);
		if (session == null) {
			return;
		}
		try {
			session.invalidate();
		} catch (IllegalStateException e) {
			logger.debug("session already invalidated: {}", userId);
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession destroyed = se.getSession();
		if (destroyed == null) {
			return;
		}
		// getId()는 만료된 세션에서 예외를 던질 수 있으므로 객체 동일성으로 제거한다.
		sessionsByUser.entrySet().removeIf(entry -> entry.getValue() == destroyed);
	}
}
