package arkive.admin.sms.service;

/**
 * SMS 발송 제공자 추상화.
 *
 * <p>특정 업체(Aligo 등)에 직접 의존하지 않도록 Controller·업무 서비스는 이 인터페이스만 쓴다.
 * 대상 프로젝트에서 다른 제공자를 붙일 때 구현체만 교체한다.</p>
 */
public interface SmsSender {

	String provider();

	/**
	 * 자격증명·발신번호·사용 설정이 모두 준비됐는지 확인한다.
	 */
	boolean isReady();

	SmsSendResult send(String to, String title, String message);
}
