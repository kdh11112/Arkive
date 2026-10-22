package arkive.admin.board.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 게시글 댓글 JSON API의 인증·검증·삭제 권한을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardCommentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	private void insertBoard(String boardId) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				boardId, "CK", "comment-api-board", "<p>body</p>"));
	}

	@Test
	void unauthenticatedCommentShouldReturn401() throws Exception {
		mockMvc.perform(post("/board/insertComment.json")
				.param("boardId", "BDTEST00000000000010")
				.param("content", "hello"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@Rollback
	void commentShouldRequireExistingBoardAndContent() throws Exception {
		MockHttpSession session = TestSessions.user("commenter");

		mockMvc.perform(post("/board/insertComment.json")
				.session(session)
				.param("boardId", "NO-SUCH-BOARD")
				.param("content", "hello"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시글이 없습니다."));

		insertBoard("BDTEST00000000000010");
		mockMvc.perform(post("/board/insertComment.json")
				.session(session)
				.param("boardId", "BDTEST00000000000010")
				.param("content", "   "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("댓글 내용을 입력하세요."));

		mockMvc.perform(post("/board/insertComment.json")
				.session(session)
				.param("boardId", "BDTEST00000000000010")
				.param("content", "x".repeat(1001)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("댓글은 1000자 이내로 입력하세요."));
	}

	@Test
	@Rollback
	void onlyOwnerOrAdminCanDeleteComment() throws Exception {
		insertBoard("BDTEST00000000000011");
		MockHttpSession owner = TestSessions.user("commenter");

		mockMvc.perform(post("/board/insertComment.json")
				.session(owner)
				.param("boardId", "BDTEST00000000000011")
				.param("content", "hello"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.commentId").isNotEmpty());

		List<EgovMap> comments = boardMapper.selectCommentList("BDTEST00000000000011");
		String commentId = String.valueOf(comments.get(0).get("commentId"));

		// 다른 일반 사용자는 삭제할 수 없다.
		mockMvc.perform(post("/board/deleteComment.json")
				.session(TestSessions.user("other"))
				.param("commentId", commentId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("삭제 권한이 없습니다."));

		// 관리자는 작성자가 아니어도 삭제할 수 있다.
		mockMvc.perform(post("/board/deleteComment.json")
				.session(TestSessions.admin("admin"))
				.param("commentId", commentId))
				.andExpect(jsonPath("$.success").value(true));

		// 삭제 후에는 다시 삭제할 수 없다.
		mockMvc.perform(post("/board/deleteComment.json")
				.session(TestSessions.admin("admin"))
				.param("commentId", commentId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("댓글이 없습니다."));
	}
}
