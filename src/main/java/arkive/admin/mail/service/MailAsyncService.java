package arkive.admin.mail.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 메일 발송을 요청 스레드에서 분리한다. SMTP 지연·장애가 화면 응답을 막지 않게 한다.
 * 실제 발송 여부는 MailService가 arkive.mail.enabled와 수신 동의를 다시 검사한다.
 */
@Service("mailAsyncService")
public class MailAsyncService {

	@Resource(name = "mailService")
	private MailService mailService;

	@Async("mailTaskExecutor")
	public void sendWelcome(String userId) {
		mailService.sendWelcome(userId);
	}

	@Async("mailTaskExecutor")
	public void sendPasswordReset(String userId, String rawToken) {
		mailService.sendPasswordReset(userId, rawToken);
	}

	@Async("mailTaskExecutor")
	public void sendBoardNotification(String boardId) {
		mailService.sendBoardNotification(boardId);
	}
}
