package arkive.admin.message.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.message.MessageFixtures;
import arkive.admin.message.service.impl.MessageMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 쪽지 JSON API의 인증·검증·권한을 MockMvc로 확인한다.
 * 컨트롤러가 예외를 잡아 200 + success:false로 응답하는 현재 규약도 함께 고정한다.
 */
@ArkiveIntegrationTest
@Transactional
class MessageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MessageMapper messageMapper;

	@Test
	void unauthenticatedJsonShouldReturn401() throws Exception {
		mockMvc.perform(post("/message/insertMessage.json")
				.param("receiverId", "admin")
				.param("title", "title")
				.param("content", "content"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@Rollback
	void insertShouldRejectMissingTokenAndAcceptValidToken() throws Exception {
		MockHttpSession session = TestSessions.user("tester", "Tester");

		mockMvc.perform(post("/message/insertMessage.json")
				.session(session)
				.param("receiverId", "admin")
				.param("title", "title")
				.param("content", "content"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("중복 제출이 감지되었습니다. 보낸쪽지함에서 발송 여부를 확인하세요."));

		String token = EgovDoubleSubmitHelper.setToken(session, "MESSAGE");
		mockMvc.perform(post("/message/insertMessage.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("receiverId", "admin")
				.param("title", "title")
				.param("content", "content"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.msgId").isNotEmpty());
	}

	@Test
	@Rollback
	void validationShouldRejectSelfUnknownReceiverAndLongTitle() throws Exception {
		MockHttpSession session = TestSessions.user("tester", "Tester");

		mockMvc.perform(post("/message/insertMessage.json")
				.session(session)
				.param("receiverId", "tester")
				.param("title", "title")
				.param("content", "content"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("자신에게는 쪽지를 보낼 수 없습니다."));

		mockMvc.perform(post("/message/insertMessage.json")
				.session(session)
				.param("receiverId", "no-such-user")
				.param("title", "title")
				.param("content", "content"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("받는 사람을 찾을 수 없습니다."));

		mockMvc.perform(post("/message/insertMessage.json")
				.session(session)
				.param("receiverId", "admin")
				.param("title", "x".repeat(201))
				.param("content", "content"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목은 200자 이내로 입력하세요."));
	}

	@Test
	@Rollback
	void onlyOwnerOfEachFolderCanDelete() throws Exception {
		String msgId = "MSTEST00000000000010";
		messageMapper.insertMessage(MessageFixtures.message(
				msgId, "sender10", "admin", "delete-test"));

		// 발신자는 받은함에서 삭제할 수 없다.
		mockMvc.perform(post("/message/deleteMessage.json")
				.session(TestSessions.user("sender10", "Sender"))
				.param("msgId", msgId)
				.param("folder", "inbox"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("삭제 권한이 없습니다."));

		// 수신자는 받은함에서 삭제할 수 있다.
		mockMvc.perform(post("/message/deleteMessage.json")
				.session(TestSessions.admin("admin", "관리자"))
				.param("msgId", msgId)
				.param("folder", "inbox"))
				.andExpect(jsonPath("$.success").value(true));

		// 같은 함에서 다시 삭제하면 실패한다.
		mockMvc.perform(post("/message/deleteMessage.json")
				.session(TestSessions.admin("admin", "관리자"))
				.param("msgId", msgId)
				.param("folder", "inbox"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이미 삭제된 쪽지입니다."));
	}
}
