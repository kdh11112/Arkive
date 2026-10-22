package arkive.admin.message;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 쪽지 기능 테스트 데이터.
 * 공통 support가 아니라 쪽지 기능과 함께 복사한다.
 * 대상 프로젝트에서는 테이블·컬럼·PK 형식에 맞게 수정한다.
 */
public final class MessageFixtures {

	private MessageFixtures() {
	}

	public static EgovMap message(String msgId, String senderId, String receiverId, String title) {
		EgovMap message = new EgovMap();
		message.put("msgId", msgId);
		message.put("senderId", senderId);
		message.put("senderNm", senderId + "-name");
		message.put("receiverId", receiverId);
		message.put("receiverNm", receiverId + "-name");
		message.put("title", title);
		message.put("content", "content-" + msgId);
		return message;
	}
}
