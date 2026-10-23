package arkive.admin.mail.service;

/**
 * 메일 발송. 호출자는 {@link MailAsyncService}를 통해 비동기로 실행한다.
 * 실제 SMTP 설정이 없거나 arkive.mail.enabled=false이면 아무 것도 보내지 않는다.
 */
public interface MailService {

	void sendWelcome(String userId);

	void sendPasswordReset(String userId, String rawToken);

	void sendBoardNotification(String boardId);
}
