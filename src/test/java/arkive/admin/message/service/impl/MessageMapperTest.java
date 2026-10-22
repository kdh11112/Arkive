package arkive.admin.message.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.message.MessageFixtures;
import arkive.support.ArkiveIntegrationTest;

/**
 * MessageMapper 통합 테스트. 받은/보낸함 분리, 읽음 처리, 함별 소프트 삭제,
 * 검색과 권한 조건(다른 사용자 ID)을 실제 SQL로 검증한다.
 */
@ArkiveIntegrationTest
@Transactional
class MessageMapperTest {

	@Autowired
	private MessageMapper messageMapper;

	private void insertMessage(String msgId, String senderId, String receiverId, String title) throws Exception {
		messageMapper.insertMessage(MessageFixtures.message(msgId, senderId, receiverId, title));
	}

	private EgovMap folder(String folder, String userId, String keyword) {
		EgovMap param = new EgovMap();
		param.put("folder", folder);
		param.put("userId", userId);
		param.put("searchKeyword", keyword);
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 10);
		return param;
	}

	@Test
	@Rollback
	void inboxAndSentShouldBeSeparatedAndSoftDeletedPerFolder() throws Exception {
		String msgId = "MSTEST00000000000001";
		insertMessage(msgId, "sender1", "receiver1", "hello");

		List<EgovMap> inbox = messageMapper.selectMessageList(folder("inbox", "receiver1", null));
		assertThat(inbox).anyMatch(m -> msgId.equals(String.valueOf(m.get("msgId"))));
		assertThat(messageMapper.selectMessageListTotCnt(folder("inbox", "receiver1", null))).isEqualTo(1);

		List<EgovMap> sent = messageMapper.selectMessageList(folder("sent", "sender1", null));
		assertThat(sent).anyMatch(m -> msgId.equals(String.valueOf(m.get("msgId"))));
		assertThat(messageMapper.selectMessageListTotCnt(folder("sent", "sender1", null))).isEqualTo(1);

		// 다른 사용자의 함에는 보이지 않는다.
		assertThat(messageMapper.selectMessageList(folder("inbox", "other", null))).isEmpty();

		// 수신자가 열면 읽음 처리된다. 두 번째 호출은 이미 Y라 0건이어도 상세는 Y다.
		EgovMap detail = messageMapper.selectMessageDetail(msgId);
		assertThat(String.valueOf(detail.get("readYn"))).isEqualTo("N");
		messageMapper.updateMessageRead(msgId);
		assertThat(String.valueOf(messageMapper.selectMessageDetail(msgId).get("readYn"))).isEqualTo("Y");

		EgovMap deleteReceiver = new EgovMap();
		deleteReceiver.put("msgId", msgId);
		deleteReceiver.put("userId", "receiver1");
		assertThat(messageMapper.deleteReceiverMessage(deleteReceiver)).isEqualTo(1);
		assertThat(messageMapper.selectMessageList(folder("inbox", "receiver1", null))).isEmpty();
		// 받은함에서 지워도 보낸함에는 남는다.
		assertThat(messageMapper.selectMessageList(folder("sent", "sender1", null)))
				.anyMatch(m -> msgId.equals(String.valueOf(m.get("msgId"))));
		// 같은 함에서 두 번 삭제하면 0건이다.
		assertThat(messageMapper.deleteReceiverMessage(deleteReceiver)).isZero();

		EgovMap deleteSender = new EgovMap();
		deleteSender.put("msgId", msgId);
		deleteSender.put("userId", "sender1");
		assertThat(messageMapper.deleteSenderMessage(deleteSender)).isEqualTo(1);
		assertThat(messageMapper.selectMessageList(folder("sent", "sender1", null))).isEmpty();

		// 소프트 삭제라 행 자체는 남는다. 보존·정리 정책은 별도 확인 대상이다.
		assertThat(messageMapper.selectMessageDetail(msgId)).isNotNull();
	}

	@Test
	@Rollback
	void searchAndWrongUserShouldNotLeakOrDelete() throws Exception {
		insertMessage("MSTEST00000000000002", "sender2", "receiver2", "alpha title");
		insertMessage("MSTEST00000000000003", "sender2", "receiver2", "beta title");

		List<EgovMap> searched = messageMapper.selectMessageList(folder("inbox", "receiver2", "alpha"));
		assertThat(searched).hasSize(1);
		assertThat(String.valueOf(searched.get(0).get("msgId"))).isEqualTo("MSTEST00000000000002");

		EgovMap wrongUser = new EgovMap();
		wrongUser.put("msgId", "MSTEST00000000000002");
		wrongUser.put("userId", "not-owner");
		assertThat(messageMapper.deleteReceiverMessage(wrongUser)).isZero();
		assertThat(messageMapper.deleteSenderMessage(wrongUser)).isZero();
		assertThat(messageMapper.selectMessageDetail("MSTEST00000000000002")).isNotNull();

		assertThat(messageMapper.selectUser("admin")).isNotNull();
		assertThat(String.valueOf(messageMapper.selectUser("admin").get("useYn"))).isEqualTo("Y");
		assertThat(messageMapper.selectUser("no-such-user")).isNull();
	}
}
