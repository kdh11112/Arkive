package arkive.admin.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 브로커가 게시판 타입·ALL 통로·역할 필터를 지켜 발송하는지 확인한다.
 * 실제 연결은 브라우저 EventSource가 열지만, 서버 발송 대상 선택은 여기서 검증한다.
 */
class BoardSseServiceTest {

	private final BoardSseService boardSseService = new BoardSseService();

	@SuppressWarnings("unchecked")
	private Set<DataWithMediaType> sent(SseEmitter emitter) {
		return (Set<DataWithMediaType>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
	}

	private boolean hasEvent(SseEmitter emitter, String boardId) {
		return sent(emitter).stream()
				.map(DataWithMediaType::getData)
				.anyMatch(data -> data instanceof Map<?, ?> map && boardId.equals(map.get("boardId")));
	}

	private boolean hasEventName(SseEmitter emitter) {
		return sent(emitter).stream()
				.map(DataWithMediaType::getData)
				.anyMatch(data -> data instanceof String text && text.startsWith("event:new-post"));
	}

	@Test
	void broadcastShouldReachMatchingRolesOnTypeAndAllChannels() {
		SseEmitter ckUser = boardSseService.subscribe("ck", Set.of("ROLE_USER"));
		SseEmitter allUser = boardSseService.subscribe("ALL", Set.of("ROLE_USER"));
		SseEmitter ckManager = boardSseService.subscribe("CK", Set.of("ROLE_MANAGER"));

		boardSseService.broadcast("CK", "B1", "title", Set.of("ROLE_USER"));

		assertThat(hasEvent(ckUser, "B1")).isTrue();
		assertThat(hasEventName(ckUser)).isTrue();
		assertThat(hasEvent(allUser, "B1")).isTrue();
		assertThat(sent(ckManager)).isEmpty();
	}

	@Test
	void broadcastShouldNotCrossBoardTypes() {
		SseEmitter toastUser = boardSseService.subscribe("TOAST", Set.of("ROLE_USER"));

		boardSseService.broadcast("CK", "B1", "title", Set.of("ROLE_USER"));

		assertThat(sent(toastUser)).isEmpty();
	}

	@Test
	void emptyOrMissingTargetRolesShouldNotSend() {
		SseEmitter user = boardSseService.subscribe("CK", Set.of("ROLE_USER"));

		boardSseService.broadcast("CK", "B1", "title", Set.of());
		boardSseService.broadcast("CK", "B2", "title", null);

		assertThat(sent(user)).isEmpty();
	}

	@Test
	void subscriptionWithoutRolesShouldNotReceiveRoleTargetedBroadcast() {
		SseEmitter anonymous = boardSseService.subscribe("CK", null);

		boardSseService.broadcast("CK", "B1", "title", Set.of("ROLE_USER"));

		assertThat(sent(anonymous)).isEmpty();
	}

	@Test
	void broadcastWithoutSubscribersShouldNotFail() {
		assertThatCode(() -> boardSseService.broadcast("CK", "B1", "title", Set.of("ROLE_USER")))
				.doesNotThrowAnyException();
	}
}
