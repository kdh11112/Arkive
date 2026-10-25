package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * CK 오프셋 페이징과 TOAST 커서 페이징의 페이지 분할·hasNext·nextCursorId를
 * 실제 SQL과 화면 응답으로 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardPagingControllerTest {

	private static final String CK_KEYWORD = "오프셋페이징";
	private static final String TOAST_KEYWORD = "커서페이징";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	private String ckId(int index) {
		return String.format("BDPAGECK%012d", index);
	}

	private String toastId(int index) {
		return String.format("BDPAGETOAST%012d", index);
	}

	private void insert(String boardId, String boardType, String keyword) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				boardId, boardType, keyword + " " + boardId, "<p>body</p>"));
	}

	private void insertTwelve(String boardType, String keyword) throws Exception {
		for (int i = 1; i <= 12; i++) {
			insert("CK".equals(boardType) ? ckId(i) : toastId(i), boardType, keyword);
		}
	}

	@SuppressWarnings("unchecked")
	private List<org.egovframe.rte.psl.dataaccess.util.EgovMap> resultList(MvcResult result) {
		return (List<org.egovframe.rte.psl.dataaccess.util.EgovMap>)
				result.getModelAndView().getModel().get("resultList");
	}

	@Test
	@Rollback
	void ckOffsetPagingShouldSplitPagesWithoutOverlap() throws Exception {
		insertTwelve("CK", CK_KEYWORD);

		MvcResult first = mockMvc.perform(get("/board/ckeditorList")
						.session(TestSessions.admin("admin"))
						.param("searchKeyword", CK_KEYWORD)
						.param("pageIndex", "1"))
				.andExpect(status().isOk())
				.andReturn();
		MvcResult second = mockMvc.perform(get("/board/ckeditorList")
						.session(TestSessions.admin("admin"))
						.param("searchKeyword", CK_KEYWORD)
						.param("pageIndex", "2"))
				.andExpect(status().isOk())
				.andReturn();

		List<org.egovframe.rte.psl.dataaccess.util.EgovMap> firstPage = resultList(first);
		List<org.egovframe.rte.psl.dataaccess.util.EgovMap> secondPage = resultList(second);
		assertThat(firstPage).hasSize(10);
		assertThat(secondPage).hasSize(2);

		PaginationInfo paginationInfo = (PaginationInfo) first.getModelAndView().getModel()
				.get("paginationInfo");
		assertThat(paginationInfo.getTotalRecordCount()).isEqualTo(12);
		assertThat(paginationInfo.getCurrentPageNo()).isEqualTo(1);

		List<Object> firstIds = firstPage.stream().map(row -> row.get("boardId")).toList();
		List<Object> secondIds = secondPage.stream().map(row -> row.get("boardId")).toList();
		assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
	}

	@Test
	@Rollback
	void toastCursorPagingShouldReturnHasNextAndNextCursor() throws Exception {
		insertTwelve("TOAST", TOAST_KEYWORD);

		mockMvc.perform(post("/board/toastList.json")
						.session(TestSessions.admin("admin"))
						.param("searchKeyword", TOAST_KEYWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.list.length()").value(10))
				.andExpect(jsonPath("$.list[0].boardId").value(toastId(12)))
				.andExpect(jsonPath("$.list[9].boardId").value(toastId(3)))
				.andExpect(jsonPath("$.hasNext").value(true))
				.andExpect(jsonPath("$.nextCursorId").value(toastId(3)));

		mockMvc.perform(post("/board/toastList.json")
						.session(TestSessions.admin("admin"))
						.param("searchKeyword", TOAST_KEYWORD)
						.param("cursorId", toastId(3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.list.length()").value(2))
				.andExpect(jsonPath("$.list[0].boardId").value(toastId(2)))
				.andExpect(jsonPath("$.list[1].boardId").value(toastId(1)))
				.andExpect(jsonPath("$.hasNext").value(false))
				.andExpect(jsonPath("$.nextCursorId").value(toastId(1)));
	}
}
