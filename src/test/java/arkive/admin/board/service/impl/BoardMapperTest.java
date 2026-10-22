package arkive.admin.board.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.support.ArkiveIntegrationTest;

/**
 * BoardMapper 통합 테스트. 실매퍼+실SQL+격리DB로 검증한다.
 * @Transactional + @Rollback이라 테스트 데이터가 남지 않는다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardMapperTest {

	@Autowired
	private BoardMapper boardMapper;

	@Test
	@Rollback
	void insertSelectDeleteShouldRoundTrip() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				"BDTEST00000000000001", "CK", "mapper-test", "<p>x</p>"));

		EgovMap param2 = new EgovMap();
		param2.put("boardType", "CK");
		param2.put("searchKeyword", "mapper-test");
		param2.put("firstIndex", 0);
		param2.put("recordCountPerPage", 10);
		List<EgovMap> list = boardMapper.selectBoardList(param2);
		assertThat(list).anyMatch(m -> "BDTEST00000000000001".equals(String.valueOf(m.get("boardId"))));

		boardMapper.deleteBoard("BDTEST00000000000001");
		assertThat(boardMapper.selectBoardDetail("BDTEST00000000000001")).isNull();
	}

	@Test
	@Rollback
	void nullContentShouldBeAllowed() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				"BDTEST00000000000002", "TOAST", "null-content", null));

		EgovMap detail = boardMapper.selectBoardDetail("BDTEST00000000000002");
		assertThat(detail).isNotNull();

		boardMapper.deleteBoard("BDTEST00000000000002");
	}

	@Test
	@Rollback
	void commentShouldRoundTripAndCascadeWithBoard() throws Exception {
		String boardId = "BDTEST00000000000003";
		boardMapper.insertBoard(BoardFixtures.board(
				boardId, "CK", "comment-board", "<p>body</p>"));

		boardMapper.insertComment(BoardFixtures.comment(
				"CMTEST00000000000001", boardId, "first comment"));

		assertThat(boardMapper.selectCommentCount(boardId)).isEqualTo(1);
		List<EgovMap> comments = boardMapper.selectCommentList(boardId);
		assertThat(comments).anyMatch(m -> "CMTEST00000000000001".equals(String.valueOf(m.get("commentId"))));

		EgovMap detail = boardMapper.selectCommentDetail("CMTEST00000000000001");
		assertThat(String.valueOf(detail.get("register"))).isEqualTo("TEST");

		boardMapper.deleteComment("CMTEST00000000000001");
		assertThat(boardMapper.selectCommentCount(boardId)).isZero();

		// 게시글 삭제 시 댓글도 함께 삭제되는지(FK ON DELETE CASCADE) 확인한다.
		boardMapper.insertComment(BoardFixtures.comment(
				"CMTEST00000000000002", boardId, "cascade comment"));

		boardMapper.deleteBoard(boardId);
		assertThat(boardMapper.selectCommentCount(boardId)).isZero();
	}
}
