package arkive.admin.board.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	@Test
	@Rollback
	void prevNextShouldStayWithinBoardTypeAndFollowRegistDt() throws Exception {
		String older = "BDNEIGHBOR00000001";
		String middle = "BDNEIGHBOR00000002";
		String newer = "BDNEIGHBOR00000003";
		String toast = "BDNEIGHBOR00000004";
		boardMapper.insertBoard(BoardFixtures.board(older, "CK", "neighbor-older", "<p>1</p>"));
		boardMapper.insertBoard(BoardFixtures.board(middle, "CK", "neighbor-middle", "<p>2</p>"));
		boardMapper.insertBoard(BoardFixtures.board(newer, "CK", "neighbor-newer", "<p>3</p>"));
		boardMapper.insertBoard(BoardFixtures.board(toast, "TOAST", "neighbor-toast", "<p>4</p>"));

		// 기존 데이터와 섞이지 않도록 충분히 먼 미래 시각으로 고정한다.
		setRegistDt(older, LocalDateTime.of(2099, 1, 1, 0, 0));
		setRegistDt(middle, LocalDateTime.of(2099, 1, 2, 0, 0));
		setRegistDt(toast, LocalDateTime.of(2099, 1, 2, 12, 0));
		setRegistDt(newer, LocalDateTime.of(2099, 1, 3, 0, 0));

		EgovMap detail = boardMapper.selectBoardDetail(middle);
		EgovMap param = new EgovMap();
		param.put("boardType", "CK");
		param.put("boardId", middle);
		param.put("registDt", detail.get("registDt"));

		assertThat(String.valueOf(boardMapper.selectPrevOne(param).get("boardId"))).isEqualTo(older);
		assertThat(String.valueOf(boardMapper.selectNextOne(param).get("boardId"))).isEqualTo(newer);
	}

	private void setRegistDt(String boardId, LocalDateTime registDt) {
		jdbcTemplate.update("UPDATE BOARD SET REGIST_DT = ? WHERE BOARD_ID = ?",
				Timestamp.valueOf(registDt), boardId);
	}
}
