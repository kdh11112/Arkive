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
 * 공지 고정(NOTICE_YN) SQL 검증. 실매퍼+실SQL+격리DB로 확인한다.
 * CK 목록에서 고정 글이 먼저 나오는지, CK가 아닌 글은 변경되지 않는지 본다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardNoticeMapperTest {

	@Autowired
	private BoardMapper boardMapper;

	private List<EgovMap> selectCkByTitle(String keyword) throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardType", "CK");
		param.put("searchKeyword", keyword);
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 10);
		return boardMapper.selectBoardList(param);
	}

	@Test
	@Rollback
	void pinnedNoticeShouldSortFirstAndToggle() throws Exception {
		String normalId = "BDNOTICE00000000001";
		String pinnedId = "BDNOTICE00000000002";
		boardMapper.insertBoard(BoardFixtures.board(normalId, "CK", "notice-sort-normal", "<p>n</p>"));
		boardMapper.insertBoard(BoardFixtures.board(pinnedId, "CK", "notice-sort-pinned", "<p>p</p>"));

		EgovMap param = new EgovMap();
		param.put("boardId", pinnedId);
		param.put("noticeYn", "Y");
		param.put("updusr", "TEST");
		assertThat(boardMapper.updateNoticeYn(param)).isEqualTo(1);

		EgovMap detail = boardMapper.selectBoardDetail(pinnedId);
		assertThat(String.valueOf(detail.get("noticeYn"))).isEqualTo("Y");

		List<EgovMap> list = selectCkByTitle("notice-sort");
		assertThat(list).hasSize(2);
		assertThat(String.valueOf(list.get(0).get("boardId"))).isEqualTo(pinnedId);
		assertThat(String.valueOf(list.get(0).get("noticeYn"))).isEqualTo("Y");
		assertThat(String.valueOf(list.get(1).get("noticeYn"))).isEqualTo("N");

		// 해제하면 고정 표시가 사라진다.
		param.put("noticeYn", "N");
		assertThat(boardMapper.updateNoticeYn(param)).isEqualTo(1);
		assertThat(String.valueOf(boardMapper.selectBoardDetail(pinnedId).get("noticeYn"))).isEqualTo("N");
		assertThat(selectCkByTitle("notice-sort"))
				.allMatch(m -> "N".equals(String.valueOf(m.get("noticeYn"))));
	}

	@Test
	@Rollback
	void noticeUpdateShouldRejectNonCkBoard() throws Exception {
		String toastId = "BDNOTICE00000000003";
		boardMapper.insertBoard(BoardFixtures.board(toastId, "TOAST", "notice-sort-toast", "<p>t</p>"));

		EgovMap param = new EgovMap();
		param.put("boardId", toastId);
		param.put("noticeYn", "Y");
		param.put("updusr", "TEST");
		assertThat(boardMapper.updateNoticeYn(param)).isZero();
		assertThat(String.valueOf(boardMapper.selectBoardDetail(toastId).get("noticeYn"))).isEqualTo("N");
	}
}
