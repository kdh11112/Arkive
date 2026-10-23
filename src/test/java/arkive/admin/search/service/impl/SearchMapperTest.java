package arkive.admin.search.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;

/**
 * 통합검색 SQL이 BOARD_TYPE별 제목을 LIKE로 찾고, 페이징·건수를 맞게
 * 반환하는지 실매퍼+실SQL로 확인한다. 본문만 일치하는 글은 제외되는지도 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class SearchMapperTest {

	@Autowired
	private SearchMapper searchMapper;

	@Autowired
	private BoardMapper boardMapper;

	private void insert(String boardId, String boardType, String title, String content) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(boardId, boardType, title, content));
	}

	private EgovMap param(String keyword, int firstIndex, int recordCountPerPage) {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", keyword);
		param.put("firstIndex", firstIndex);
		param.put("recordCountPerPage", recordCountPerPage);
		return param;
	}

	@Test
	@Rollback
	void searchShouldMatchTitleAcrossBoardTypesAndIgnoreContentOnlyMatch() throws Exception {
		insert("BDSRCH00000000000001", "CK", "통합검색 공지", "<p>공지 본문</p>");
		insert("BDSRCH00000000000002", "TOAST", "통합검색 자료", "<p>자료 본문</p>");
		insert("BDSRCH00000000000003", "FAQ", "통합검색 FAQ", "<p>FAQ 본문</p>");
		insert("BDSRCH00000000000004", "QNA", "통합검색 질문", "<p>질문 본문</p>");
		insert("BDSRCH00000000000009", "CK", "본문만 일치", "<p>통합검색</p>");

		List<EgovMap> list = searchMapper.selectSearchList(param("통합검색", 0, 10));

		assertThat(list).extracting(row -> String.valueOf(row.get("boardType")))
				.contains("CK", "TOAST", "FAQ", "QNA");
		assertThat(list).extracting(row -> String.valueOf(row.get("boardId")))
				.doesNotContain("BDSRCH00000000000009");
		assertThat(searchMapper.selectSearchListTotCnt(param("통합검색", 0, 10)))
				.isGreaterThanOrEqualTo(4);
	}

	@Test
	@Rollback
	void unknownKeywordShouldReturnZero() throws Exception {
		insert("BDSRCH00000000000005", "CK", "다른 글", "<p>다른 본문</p>");

		assertThat(searchMapper.selectSearchList(param("존재하지않는검색어", 0, 10))).isEmpty();
		assertThat(searchMapper.selectSearchListTotCnt(param("존재하지않는검색어", 0, 10))).isZero();
	}

	@Test
	@Rollback
	void pagingShouldUseLimitAndOffset() throws Exception {
		insert("BDSRCH00000000000006", "CK", "페이징 검색 1", "<p>본문</p>");
		insert("BDSRCH00000000000007", "TOAST", "페이징 검색 2", "<p>본문</p>");
		insert("BDSRCH00000000000008", "FAQ", "페이징 검색 3", "<p>본문</p>");

		List<EgovMap> firstPage = searchMapper.selectSearchList(param("페이징 검색", 0, 2));
		List<EgovMap> secondPage = searchMapper.selectSearchList(param("페이징 검색", 2, 2));

		assertThat(firstPage).hasSize(2);
		assertThat(secondPage).hasSize(1);
		assertThat(searchMapper.selectSearchListTotCnt(param("페이징 검색", 0, 2)))
				.isGreaterThanOrEqualTo(3);
	}
}
