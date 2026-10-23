package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.event.BoardNotificationEvent;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 게시글 저장 API가 직접 SSE·메일을 호출하지 않고 알림 이벤트만 발행하는지 확인한다.
 * 실제 채널 동작은 BoardNotificationEventListenerTest에서 검증한다.
 */
@ArkiveIntegrationTest
@Transactional
@RecordApplicationEvents
class BoardNotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private ApplicationEvents events;

	private void insertBoard(String boardId, String boardType) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				boardId, boardType, "notification-api-" + boardId, "<p>body</p>"));
	}

	@Test
	void insertBoardShouldPublishSseAndMailEvents() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_TOAST");

		mockMvc.perform(post("/board/insertBoard.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("boardType", "TOAST")
				.param("title", "important notice")
				.param("content", "<p>body</p>")
				.param("importantYn", "Y"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.boardType").value("TOAST"));

		List<BoardNotificationEvent> published = events.stream(BoardNotificationEvent.class).toList();
		assertThat(published).hasSize(1);
		BoardNotificationEvent event = published.get(0);
		assertThat(event.boardType()).isEqualTo("TOAST");
		assertThat(event.title()).isEqualTo("important notice");
		assertThat(event.notifySse()).isTrue();
		assertThat(event.notifyMail()).isTrue();
	}

	@Test
	void pinningNoticeShouldPublishMailOnlyEvent() throws Exception {
		insertBoard("BDNOTICEEVENT0001", "CK");

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDNOTICEEVENT0001")
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		List<BoardNotificationEvent> published = events.stream(BoardNotificationEvent.class).toList();
		assertThat(published).hasSize(1);
		BoardNotificationEvent event = published.get(0);
		assertThat(event.boardId()).isEqualTo("BDNOTICEEVENT0001");
		assertThat(event.boardType()).isEqualTo("CK");
		assertThat(event.notifySse()).isFalse();
		assertThat(event.notifyMail()).isTrue();
	}

	@Test
	void updatingToImportantShouldRequestMailOnlyOnTransition() throws Exception {
		insertBoard("BDEVENTBOARD00001", "CK");
		MockHttpSession admin = TestSessions.admin("admin");

		mockMvc.perform(post("/board/updateBoard.json")
				.session(admin)
				.param("boardId", "BDEVENTBOARD00001")
				.param("title", "updated title")
				.param("content", "<p>updated</p>")
				.param("importantYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		List<BoardNotificationEvent> first = events.stream(BoardNotificationEvent.class).toList();
		assertThat(first).hasSize(1);
		assertThat(first.get(0).notifySse()).isFalse();
		assertThat(first.get(0).notifyMail()).isTrue();

		events.clear();

		mockMvc.perform(post("/board/updateBoard.json")
				.session(admin)
				.param("boardId", "BDEVENTBOARD00001")
				.param("title", "updated title again")
				.param("content", "<p>updated again</p>")
				.param("importantYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		List<BoardNotificationEvent> second = events.stream(BoardNotificationEvent.class).toList();
		assertThat(second).hasSize(1);
		assertThat(second.get(0).notifySse()).isFalse();
		assertThat(second.get(0).notifyMail()).isFalse();
	}

	@Test
	void normalUpdateShouldNotRequestMail() throws Exception {
		insertBoard("BDEVENTBOARD00002", "CK");

		mockMvc.perform(post("/board/updateBoard.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDEVENTBOARD00002")
				.param("title", "normal title")
				.param("content", "<p>normal</p>")
				.param("importantYn", "N"))
				.andExpect(jsonPath("$.success").value(true));

		List<BoardNotificationEvent> published = events.stream(BoardNotificationEvent.class).toList();
		assertThat(published).hasSize(1);
		assertThat(published.get(0).notifySse()).isFalse();
		assertThat(published.get(0).notifyMail()).isFalse();
	}
}
