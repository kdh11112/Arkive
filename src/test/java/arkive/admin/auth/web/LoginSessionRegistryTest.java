package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.http.HttpSessionEvent;

/**
 * 중복 로그인 방지 세션 레지스트리 단위 테스트.
 * 실제 컨테이너 없이 MockHttpSession으로 매핑·만료·정리 규칙을 확인한다.
 */
class LoginSessionRegistryTest {

	private final LoginSessionRegistry registry = new LoginSessionRegistry();

	@Test
	void sameUserNewSessionShouldInvalidatePrevious() {
		MockHttpSession first = new MockHttpSession();
		MockHttpSession second = new MockHttpSession();

		registry.register("user1", first);
		registry.register("user1", second);

		assertThat(first.isInvalid()).isTrue();
		assertThat(second.isInvalid()).isFalse();
		assertThat(registry.currentSession("user1")).isSameAs(second);
	}

	@Test
	void differentUsersShouldKeepTheirSessions() {
		MockHttpSession user1 = new MockHttpSession();
		MockHttpSession user2 = new MockHttpSession();

		registry.register("user1", user1);
		registry.register("user2", user2);

		assertThat(user1.isInvalid()).isFalse();
		assertThat(user2.isInvalid()).isFalse();
		assertThat(registry.currentSession("user1")).isSameAs(user1);
		assertThat(registry.currentSession("user2")).isSameAs(user2);
	}

	@Test
	void sameSessionShouldNotBeInvalidatedOnReregister() {
		MockHttpSession session = new MockHttpSession();

		registry.register("user1", session);
		registry.register("user1", session);

		assertThat(session.isInvalid()).isFalse();
		assertThat(registry.currentSession("user1")).isSameAs(session);
	}

	@Test
	void switchingUserOnSameSessionShouldDropPreviousMapping() {
		MockHttpSession session = new MockHttpSession();

		registry.register("user1", session);
		registry.register("user2", session);

		assertThat(session.isInvalid()).isFalse();
		assertThat(registry.currentSession("user1")).isNull();
		assertThat(registry.currentSession("user2")).isSameAs(session);
	}

	@Test
	void destroyedSessionShouldBeRemoved() {
		MockHttpSession session = new MockHttpSession();
		registry.register("user1", session);

		registry.sessionDestroyed(new HttpSessionEvent(session));

		assertThat(registry.currentSession("user1")).isNull();
	}
}
