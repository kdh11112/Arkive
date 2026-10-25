package arkive.admin.chat.websocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.chat.service.ChatService;

/**
 * 채팅 WebSocket 핸들러. 단일 인스턴스 in-memory 브로드캐스트.
 *
 * <p>클라이언트가 보낸 사용자 ID·이름은 신뢰하지 않고 핸드셰이크에서 저장한
 * 세션 값만 사용한다. 메시지는 길이를 제한하고, 전송은 세션별 동시성 래퍼로
 * 직렬화한다. 다중 인스턴스로 확장하려면 Redis 등 공유 브로커가 필요하다.</p>
 */
@Component("chatWebSocketHandler")
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
	private static final int HISTORY_LIMIT = 100;
	private static final int MAX_MESSAGE = 1000;
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ChatService chatService;
	private final ObjectMapper objectMapper;
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
		this.chatService = chatService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024);
		sessions.put(session.getId(), safeSession);
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("type", "history");
			payload.put("messages", chatService.selectRecentMessages(HISTORY_LIMIT));
			safeSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
		} catch (Exception e) {
			logger.warn("chat history send failed. session={}", session.getId());
			closeQuietly(safeSession);
			sessions.remove(session.getId());
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		WebSocketSession target = sessions.get(session.getId());
		if (target == null) {
			return;
		}
		String userId = String.valueOf(session.getAttributes().get("chatUserId"));
		String userNm = String.valueOf(session.getAttributes().get("chatUserNm"));
		try {
			JsonNode node = objectMapper.readTree(message.getPayload());
			String text = node.path("message").asText("").trim();
			if (text.isEmpty()) {
				return;
			}
			if (text.length() > MAX_MESSAGE) {
				sendError(target, "메시지는 1000자 이내로 입력하세요.");
				return;
			}
			String messageId = chatService.insertMessage(userId, userNm, text);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("type", "message");
			payload.put("messageId", messageId);
			payload.put("userId", userId);
			payload.put("userNm", userNm);
			payload.put("message", text);
			payload.put("sentAt", LocalDateTime.now().format(TIME_FORMAT));
			broadcast(payload);
		} catch (IllegalArgumentException e) {
			sendError(target, e.getMessage());
		} catch (Exception e) {
			logger.warn("chat message failed. session={}", session.getId());
			sendError(target, "메시지를 보내지 못했습니다. 잠시 뒤 다시 시도하세요.");
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		logger.info("chat transport error. session={}", session.getId());
		sessions.remove(session.getId());
		closeQuietly(session);
	}

	private void broadcast(Map<String, Object> payload) {
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			logger.warn("chat payload serialization failed");
			return;
		}
		TextMessage textMessage = new TextMessage(json);
		for (WebSocketSession session : sessions.values()) {
			try {
				if (session.isOpen()) {
					session.sendMessage(textMessage);
				}
			} catch (Exception e) {
				sessions.remove(session.getId());
				closeQuietly(session);
			}
		}
	}

	private void sendError(WebSocketSession session, String message) {
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("type", "error");
			payload.put("message", message);
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
		} catch (Exception e) {
			logger.debug("chat error send failed. session={}", session.getId());
		}
	}

	private void closeQuietly(WebSocketSession session) {
		try {
			if (session.isOpen()) {
				session.close(CloseStatus.SERVER_ERROR);
			}
		} catch (Exception e) {
			logger.debug("chat close failed. session={}", session.getId());
		}
	}
}
