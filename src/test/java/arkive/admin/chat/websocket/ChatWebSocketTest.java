package arkive.admin.chat.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.auth.service.MenuPermission;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.chat.service.ChatService;
import arkive.support.TestSessions;

/**
 * 채팅 WebSocket의 핸드셰이크 인증·권한·Origin 검사와 메시지 저장·브로드캐스트를
 * 실제 웹소켓 서버 없이 단위 테스트로 확인한다.
 */
class ChatWebSocketTest {

	private static final String CHAT_MENU = "ME020B0100";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void handshakeShouldRequireSessionAndChatWritePermission() throws Exception {
		PermissionService permissionService = mock(PermissionService.class);
		ChatHandshakeInterceptor interceptor = new ChatHandshakeInterceptor(permissionService);

		assertThat(handshake(interceptor, new MockHttpServletRequest(), new HashMap<>())).isFalse();

		MockHttpServletRequest noPermission = requestWithSession(TestSessions.user("chat01", "채팅 사용자"));
		when(permissionService.selectUserMenuPermissions("chat01")).thenReturn(Map.of());
		assertThat(handshake(interceptor, noPermission, new HashMap<>())).isFalse();

		MockHttpServletRequest readOnly = requestWithSession(TestSessions.user("chat01", "채팅 사용자"));
		when(permissionService.selectUserMenuPermissions("chat01"))
				.thenReturn(Map.of(CHAT_MENU, new MenuPermission(true, false)));
		assertThat(handshake(interceptor, readOnly, new HashMap<>())).isFalse();

		MockHttpServletRequest allowed = requestWithSession(TestSessions.user("chat01", "채팅 사용자"));
		when(permissionService.selectUserMenuPermissions("chat01"))
				.thenReturn(Map.of(CHAT_MENU, new MenuPermission(true, true)));
		Map<String, Object> attributes = new HashMap<>();
		assertThat(handshake(interceptor, allowed, attributes)).isTrue();
		assertThat(attributes).containsEntry("chatUserId", "chat01")
				.containsEntry("chatUserNm", "채팅 사용자");

		MockHttpServletRequest admin = requestWithSession(TestSessions.admin("admin", "관리자"));
		assertThat(handshake(interceptor, admin, new HashMap<>())).isTrue();
	}

	@Test
	void handshakeShouldRejectCrossOriginAndAllowConfiguredOrigin() throws Exception {
		PermissionService permissionService = mock(PermissionService.class);
		ChatHandshakeInterceptor interceptor = new ChatHandshakeInterceptor(permissionService);
		MockHttpSession session = TestSessions.admin("admin", "관리자");

		MockHttpServletRequest sameOrigin = requestWithSession(session);
		sameOrigin.setServerName("localhost");
		sameOrigin.setServerPort(8080);
		sameOrigin.addHeader("Origin", "http://localhost:8080");
		assertThat(handshake(interceptor, sameOrigin, new HashMap<>())).isTrue();

		MockHttpServletRequest otherHost = requestWithSession(session);
		otherHost.setServerName("localhost");
		otherHost.setServerPort(8080);
		otherHost.addHeader("Origin", "http://evil.example:8080");
		assertThat(handshake(interceptor, otherHost, new HashMap<>())).isFalse();

		MockHttpServletRequest otherPort = requestWithSession(session);
		otherPort.setServerName("localhost");
		otherPort.setServerPort(8080);
		otherPort.addHeader("Origin", "http://localhost:9090");
		assertThat(handshake(interceptor, otherPort, new HashMap<>())).isFalse();

		ReflectionTestUtils.setField(interceptor, "allowedOrigins", "https://chat.example");
		MockHttpServletRequest configured = requestWithSession(session);
		configured.addHeader("Origin", "https://chat.example");
		assertThat(handshake(interceptor, configured, new HashMap<>())).isTrue();
	}

	@Test
	void connectionShouldSendRecentHistory() throws Exception {
		ChatService chatService = mock(ChatService.class);
		EgovMap recent = new EgovMap();
		recent.put("message", "이전 메시지");
		when(chatService.selectRecentMessages(100)).thenReturn(List.of(recent));
		ChatWebSocketHandler handler = new ChatWebSocketHandler(chatService, objectMapper);

		WebSocketSession session = session("s1", "chat01", "채팅 사용자");
		handler.afterConnectionEstablished(session);

		JsonNode payload = sentPayload(session);
		assertThat(payload.path("type").asText()).isEqualTo("history");
		assertThat(payload.path("messages").get(0).path("message").asText()).isEqualTo("이전 메시지");
	}

	@Test
	void messageShouldBeSavedWithSessionIdentityAndBroadcast() throws Exception {
		ChatService chatService = mock(ChatService.class);
		when(chatService.insertMessage("chat01", "채팅 사용자", "안녕하세요")).thenReturn("CHAT-1");
		ChatWebSocketHandler handler = new ChatWebSocketHandler(chatService, objectMapper);

		WebSocketSession first = session("s1", "chat01", "채팅 사용자");
		WebSocketSession second = session("s2", "chat02", "다른 사용자");
		handler.afterConnectionEstablished(first);
		handler.afterConnectionEstablished(second);

		handler.handleTextMessage(first, new TextMessage("{\"message\":\"안녕하세요\"}"));

		verify(chatService).insertMessage("chat01", "채팅 사용자", "안녕하세요");
		for (WebSocketSession session : List.of(first, second)) {
			List<TextMessage> messages = sentMessages(session);
			JsonNode payload = objectMapper.readTree(messages.get(messages.size() - 1).getPayload());
			assertThat(payload.path("type").asText()).isEqualTo("message");
			assertThat(payload.path("messageId").asText()).isEqualTo("CHAT-1");
			assertThat(payload.path("userId").asText()).isEqualTo("chat01");
			assertThat(payload.path("message").asText()).isEqualTo("안녕하세요");
			assertThat(payload.path("sentAt").asText()).isNotBlank();
		}
	}

	@Test
	void blankAndTooLongMessagesShouldNotBeSaved() throws Exception {
		ChatService chatService = mock(ChatService.class);
		ChatWebSocketHandler handler = new ChatWebSocketHandler(chatService, objectMapper);
		WebSocketSession session = session("s1", "chat01", "채팅 사용자");
		handler.afterConnectionEstablished(session);

		handler.handleTextMessage(session, new TextMessage("{\"message\":\"   \"}"));
		verify(chatService, never()).insertMessage(any(), any(), any());

		handler.handleTextMessage(session, new TextMessage(
				objectMapper.writeValueAsString(Map.of("message", "a".repeat(1001)))));
		verify(chatService, never()).insertMessage(any(), any(), any());
		JsonNode error = sentPayload(session);
		assertThat(error.path("type").asText()).isEqualTo("error");
		assertThat(error.path("message").asText()).contains("1000자");
	}

	@Test
	void serviceFailureShouldSendErrorToSenderOnly() throws Exception {
		ChatService chatService = mock(ChatService.class);
		when(chatService.insertMessage(any(), any(), any())).thenThrow(new IllegalArgumentException("저장 실패"));
		ChatWebSocketHandler handler = new ChatWebSocketHandler(chatService, objectMapper);
		WebSocketSession sender = session("s1", "chat01", "채팅 사용자");
		WebSocketSession other = session("s2", "chat02", "다른 사용자");
		handler.afterConnectionEstablished(sender);
		handler.afterConnectionEstablished(other);
		clearInvocations(sender, other);

		handler.handleTextMessage(sender, new TextMessage("{\"message\":\"실패\"}"));

		JsonNode error = sentPayload(sender);
		assertThat(error.path("type").asText()).isEqualTo("error");
		assertThat(error.path("message").asText()).isEqualTo("저장 실패");
		verify(other, never()).sendMessage(any());
	}

	private boolean handshake(ChatHandshakeInterceptor interceptor, MockHttpServletRequest request,
			Map<String, Object> attributes) throws Exception {
		return interceptor.beforeHandshake(new ServletServerHttpRequest(request),
				new ServletServerHttpResponse(new MockHttpServletResponse()), mock(WebSocketHandler.class),
				attributes);
	}

	private MockHttpServletRequest requestWithSession(MockHttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		return request;
	}

	private WebSocketSession session(String id, String userId, String userNm) throws Exception {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		when(session.isOpen()).thenReturn(true);
		when(session.getAttributes()).thenReturn(Map.of("chatUserId", userId, "chatUserNm", userNm));
		return session;
	}

	@SuppressWarnings("unchecked")
	private List<TextMessage> sentMessages(WebSocketSession session) throws Exception {
		ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
		verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());
		return (List<TextMessage>) (List<?>) captor.getAllValues();
	}

	private JsonNode sentPayload(WebSocketSession session) throws Exception {
		List<TextMessage> messages = sentMessages(session);
		return objectMapper.readTree(messages.get(messages.size() - 1).getPayload());
	}
}
