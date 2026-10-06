package arkive.admin.board.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
 * 목록 화면이 GET /board/subscribe?boardType=CK 으로 구독하면
 * 여기서 SseEmitter(30분짜리 통로) 하나를 만들어 게시판별로 보관한다.
 * insertBoard 성공 뒤 broadcast()가 그 통로들로 {boardId,title}을 쏜다.
 * 끊긴 통로는 다음 방송 때 정리한다(보내다 IOException 나면 제거).
 *
 * 주의: 통로는 메모리에만 있다. 서버를 재시작하면 구독이 끊기고
 * 브라우저가 자동 재구독한다. 다중 서버(스케일아웃)면 Redis Pub/Sub 등으로
 * 바꿔야 한다. (참고용이라 여기까지만 둔다)
 */
@Service
public class BoardSseService {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/** 통로 유지 시간. 30분 지나면 브라우저가 자동 재구독한다. */
	private static final long TIMEOUT_MS = 30L * 60 * 1000;

	/** 게시판 타입별 구독 통로 목록. 멀티스레드에서 같이 쓰므로 동시성 컬렉션이다. */
	private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

	/**
	 * 구독 통로를 하나 만든다. 컨트롤러가 그대로 반환하면 스프링이 연결을 열어 둔다.
	 */
	public SseEmitter subscribe(String boardType) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
		String key = normalize(boardType);
		emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
		// 정상 종료·시간초과·에러 시 목록에서 뺀다. 안 빼면 죽은 통로가 쌓인다.
		emitter.onCompletion(() -> remove(key, emitter));
		emitter.onTimeout(() -> remove(key, emitter));
		emitter.onError(e -> remove(key, emitter));
		return emitter;
	}

	/**
	 * 새 글을 같은 게시판 구독자들에게 뿌린다.
	 * 죽은 통로는 보내다 실패하면 그 자리에서 제거한다.
	 */
	public void broadcast(String boardType, String boardId, String title) {
		List<SseEmitter> list = emitters.get(normalize(boardType));
		if (list == null || list.isEmpty()) {
			return;
		}
		for (SseEmitter emitter : list) {
			try {
				// 브라우저 EventSource.onmessage(e)에서 e.data로 받는 JSON이다.
				emitter.send(SseEmitter.event()
						.name("new-post")
						.data(Map.of("boardId", boardId, "title", title)));
			} catch (IOException e) {
				remove(normalize(boardType), emitter);
			}
		}
	}

	private void remove(String key, SseEmitter emitter) {
		List<SseEmitter> list = emitters.get(key);
		if (list != null) {
			list.remove(emitter);
		}
	}

	private String normalize(String boardType) {
		return boardType == null ? "" : boardType.trim().toUpperCase();
	}
}
