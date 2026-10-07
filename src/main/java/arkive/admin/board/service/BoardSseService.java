package arkive.admin.board.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 새 글 알림(SSE) 브로커.
 *
 * SSE(Server-Sent Events)란:
 * 서버가 브라우저에 한 방향으로 밀어넣는 기술이다. WebSocket과 달리
 * 별도 설치 없이 HTTP 하나로 되며, 브라우저 EventSource가 끊기면
 * 알아서 다시 붙는다. 알림·진행률 같은 단방향에 쓴다.
 *
 * 동작:
 * 전역 알림은 GET /board/subscribe?boardType=ALL 로 통로 1개만 연다.
 * insertBoard 성공 뒤 broadcast()가 해당 게시판 + ALL 통로로 {boardId,title,boardType}을 쏜다.
 * 끊긴 통로는 다음 방송 때 정리한다(보내다 IOException 나면 제거).
 *
 * 권한별 발송(2단계 이후): 구독 때 역할도 함께 저장한다.
 * broadcast(..., targetRoles)는 역할이 겹치는 통로에만 쏜다. 클라이언트에서 숨기는 게 아니라 서버에서 안 쏜다.
 * 1단계(비로그인)에는 역할이 없어 전체 발송만 동작한다. 역할 필터는 로그인·권한관리 이후 활성화한다.
 *
 * 주의: 통로는 메모리에만 있다. 서버를 재시작하면 구독이 끊기고
 * 브라우저가 자동 재구독한다. 다중 서버(스케일아웃)면 Redis Pub/Sub 등으로
 * 바꿔야 한다. (참고용이라 여기까지만 둔다)
 *
 * 근거: Spring SseEmitter 표준. 별도 의존성 없이 새 글 알림을 목록에 밀어넣는다.
 */
@Service
public class BoardSseService {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/** 통로 유지 시간. 30분 지나면 브라우저가 자동 재구독한다. */
	private static final long TIMEOUT_MS = 30L * 60 * 1000;

	/** 게시판 타입별 구독 통로 목록. 멀티스레드에서 같이 쓰므로 동시성 컬렉션이다. */
	private final Map<String, List<Subscription>> emitters = new ConcurrentHashMap<>();

	/** 구독 1건. roles가 비면 전체 공개 채널이다. */
	private static final class Subscription {
		final SseEmitter emitter;
		final Set<String> roles;

		Subscription(SseEmitter emitter, Set<String> roles) {
			this.emitter = emitter;
			this.roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
		}
	}

	/**
	 * 구독 통로를 하나 만든다. 컨트롤러가 그대로 반환하면 스프링이 연결을 열어 둔다.
	 */
	public SseEmitter subscribe(String boardType) {
		return subscribe(boardType, Collections.emptySet());
	}

	/**
	 * 역할 포함 구독. 2단계 로그인 이후 Controller에서 세션 역할을 넘긴다.
	 * 1단계에는 빈 집합으로 호출한다(전체 공개와 동일).
	 */
	public SseEmitter subscribe(String boardType, Set<String> roles) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
		String key = normalize(boardType);
		Subscription sub = new Subscription(emitter, roles);
		emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(sub);
		// 정상 종료·시간초과·에러 시 목록에서 뺀다. 안 빼면 죽은 통로가 쌓인다.
		emitter.onCompletion(() -> remove(key, sub));
		emitter.onTimeout(() -> remove(key, sub));
		emitter.onError(e -> remove(key, sub));
		return emitter;
	}

	/**
	 * 새 글을 해당 게시판 + 전체(ALL) 구독자들에게 뿌린다.
	 * 죽은 통로는 보내다 실패하면 그 자리에서 제거한다.
	 */
	public void broadcast(String boardType, String boardId, String title) {
		String key = normalize(boardType);
		sendTo(key, key, boardId, title, null);
		if (!"ALL".equals(key)) {
			sendTo("ALL", key, boardId, title, null);
		}
	}

	/**
	 * 역할 지정 발송. targetRoles와 겹치는 역할의 통로에만 쏜다.
	 * 2단계 로그인·권한관리 이후 사용한다. 1단계에는 호출하지 않는다.
	 */
	public void broadcast(String boardType, String boardId, String title, Set<String> targetRoles) {
		String key = normalize(boardType);
		sendTo(key, key, boardId, title, targetRoles);
		if (!"ALL".equals(key)) {
			sendTo("ALL", key, boardId, title, targetRoles);
		}
	}

	private void sendTo(String listKey, String boardType, String boardId, String title, Set<String> targetRoles) {
		List<Subscription> list = emitters.get(listKey);
		if (list == null || list.isEmpty()) {
			return;
		}
		for (Subscription sub : list) {
			if (targetRoles != null && !targetRoles.isEmpty()
					&& sub.roles.stream().noneMatch(targetRoles::contains)) {
				continue;
			}
			try {
				// 브라우저 EventSource.onmessage(e)에서 e.data로 받는 JSON이다.
				sub.emitter.send(SseEmitter.event()
						.name("new-post")
						.data(Map.of("boardId", boardId, "title", title, "boardType", boardType)));
			} catch (IOException e) {
				remove(listKey, sub);
			}
		}
	}

	private void remove(String key, Subscription sub) {
		List<Subscription> list = emitters.get(key);
		if (list != null) {
			list.remove(sub);
		}
	}

	private String normalize(String boardType) {
		return boardType == null ? "" : boardType.trim().toUpperCase();
	}
}
