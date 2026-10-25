package arkive.admin.sms.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * SMS 발송을 요청 스레드에서 분리한다. 외부 업체 지연·장애가 화면 응답을 막지 않게 한다.
 */
@Service("smsAsyncService")
public class SmsAsyncService {

	@Resource(name = "smsService")
	private SmsService smsService;

	@Async("mailTaskExecutor")
	public void sendBoardNotification(String boardId) {
		try {
			smsService.sendBoardNotification(boardId);
		} catch (Exception e) {
			// 비동기 채널 실패가 게시글 저장 결과에 영향을 주지 않는다.
		}
	}
}
