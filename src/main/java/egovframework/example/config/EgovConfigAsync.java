package egovframework.example.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 메일 발송 전용 비동기 실행기.
 *
 * <p>메일은 요청 스레드에서 SMTP 응답을 기다리지 않는다. DB 트랜잭션이 끝난 뒤
 * 발송되도록 Controller에서 커밋 이후 호출하는 구조를 전제로 한다. 현재 프로젝트의
 * Mapper 호출은 개별 커밋이므로, 메일 서비스는 저장된 사용자/게시글을 다시 조회해
 * 최소한의 정보만 사용한다.</p>
 */
@Configuration
@EnableAsync
public class EgovConfigAsync implements AsyncConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(EgovConfigAsync.class);

	@Bean(name = "mailTaskExecutor")
	public ThreadPoolTaskExecutor mailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("arkive-mail-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return mailTaskExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new AsyncUncaughtExceptionHandler() {
			@Override
			public void handleUncaughtException(Throwable ex, Method method, Object... params) {
				// 수신자 주소·토큰 같은 민감값은 로그에 남기지 않는다.
				logger.error("async mail task failed: {}", method == null ? "unknown" : method.getName(), ex);
			}
		};
	}
}
