package arkive.admin.sms.service;

/**
 * 업무 알림 SMS 발송.
 *
 * <p>수신 동의(SMS_YN='Y')와 휴대전화가 있는 사용자에게만 보낸다. 자격증명이 없으면
 * 아무 것도 하지 않는다. 외부 장애가 게시글 저장을 막지 않도록 비동기 채널에서 호출한다.</p>
 */
public interface SmsService {

	boolean isReady();

	/**
	 * @return 실제 발송을 시도한 수신자 수
	 */
	int sendBoardNotification(String boardId) throws Exception;
}
