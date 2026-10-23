package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 공지 고정 JSON API의 인증·관리자 권한·대상 검증을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardNoticeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	private void insertBoard(String boardId, String boardType) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				boardId, boardType, "notice-api-" + boardId, "<p>body</p>"));
	}

	@Test
	void unauthenticatedNoticeUpdateShouldReturn401() throws Exception {
		mockMvc.perform(post("/board/updateNotice.json")
				.param("boardId", "BDNOTICE00000000010")
				.param("noticeYn", "Y"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@Rollback
	void onlyAdminCanPinNotice() throws Exception {
		insertBoard("BDNOTICE00000000010", "CK");

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.user("normal"))
				.param("boardId", "BDNOTICE00000000010")
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("관리자만 고정할 수 있습니다."));

		EgovMap detail = boardMapper.selectBoardDetail("BDNOTICE00000000010");
		assertThatNoticeYn(detail, "N");
	}

	@Test
	@Rollback
	void adminCanPinOnlyCkBoard() throws Exception {
		insertBoard("BDNOTICE00000000011", "CK");
		insertBoard("BDNOTICE00000000012", "TOAST");

		// 대상·값 검증
		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시글 ID가 없습니다."));

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDNOTICE00000000011")
				.param("noticeYn", "X"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("고정 값이 올바르지 않습니다."));

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "NO-SUCH-BOARD")
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시글이 없습니다."));

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDNOTICE00000000012")
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("공지사항만 고정할 수 있습니다."));

		// CK는 고정/해제 가능
		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDNOTICE00000000011")
				.param("noticeYn", "Y"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.noticeYn").value("Y"));
		assertThatNoticeYn(boardMapper.selectBoardDetail("BDNOTICE00000000011"), "Y");

		mockMvc.perform(post("/board/updateNotice.json")
				.session(TestSessions.admin("admin"))
				.param("boardId", "BDNOTICE00000000011")
				.param("noticeYn", "N"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.noticeYn").value("N"));
		assertThatNoticeYn(boardMapper.selectBoardDetail("BDNOTICE00000000011"), "N");
	}

	@Test
	@Rollback
	void adminListShouldRenderNoticeColumnAndBadge() throws Exception {
		insertBoard("BDNOTICE00000000013", "CK");

		// Thymeleaf 렌더링까지 확인한다. 템플릿 식이 깨지면 여기서 실패한다.
		mockMvc.perform(get("/board/ckeditorList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("fnToggleNotice")));
	}

	private void assertThatNoticeYn(EgovMap detail, String expected) {
		assertThat(String.valueOf(detail.get("noticeYn"))).isEqualTo(expected);
	}
}
