package arkive.admin.chat.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import arkive.admin.chat.websocket.ChatHandshakeInterceptor;
import arkive.admin.chat.websocket.ChatWebSocketHandler;
import jakarta.websocket.server.ServerContainer;

/**
 * 채팅 WebSocket 등록. Origin과 메뉴 권한 검사는 핸드셰이크 인터셉터가 담당한다.
 */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

	private final ChatWebSocketHandler chatWebSocketHandler;
	private final ChatHandshakeInterceptor chatHandshakeInterceptor;

	public ChatWebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
			ChatHandshakeInterceptor chatHandshakeInterceptor) {
		this.chatWebSocketHandler = chatWebSocketHandler;
		this.chatHandshakeInterceptor = chatHandshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(chatWebSocketHandler, "/ws/chat")
				.addInterceptors(chatHandshakeInterceptor);
	}

	/**
	 * 실제 서블릿 컨테이너가 준비된 경우에만 WebSocket 버퍼·유휴 한도를 적용한다.
	 * ServletServerContainerFactoryBean은 ServerContainer 속성이 없는 테스트 mock 컨텍스트에서 기동을 막는다.
	 */
	@Bean
	public ServletContextInitializer webSocketContainerInitializer() {
		return servletContext -> {
			Object container = servletContext.getAttribute(ServerContainer.class.getName());
			if (container instanceof ServerContainer serverContainer) {
				serverContainer.setDefaultMaxTextMessageBufferSize(16 * 1024);
				serverContainer.setDefaultMaxBinaryMessageBufferSize(16 * 1024);
				serverContainer.setDefaultMaxSessionIdleTimeout(30L * 60 * 1000);
			}
		};
	}
}
