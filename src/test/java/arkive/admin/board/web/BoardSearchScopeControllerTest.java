package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.BeforeEach;
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
 * 검색 범위(제목/내용/제목+내용)와 값 정규화(화이트리스트 폴백)를 실제 SQL과 화면 응답으로 확인한다.
 * 제목에만 있는 토큰과 본문에만 있는 토큰을 따로 넣어 범위가 섞이지 않는지 본다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardSearchScopeControllerTest {

	private static final String TITLE_TOKEN = "QZSEARCHTITLE";
	private static final String BODY_TOKEN = "QZSEARCHBODY";
	private static final String CK_ROW_ID = "BDSEARCHSCOPECK01";
	private static final String CK_BODY_ROW_ID = "BDSEARCHSCOPECK02";
	private static final String TOAST_ROW_ID = "BDSEARCHSCOPETO01";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	@BeforeEach
	void insertRows() throws Exception {
		// 제목에만 검색 토큰이 있는 글
		boardMapper.insertBoard(BoardFixtures.board(
				CK_ROW_ID, "CK", TITLE_TOKEN + " 공지", "<p>본문에는 토큰 없음</p>"));
		// 본문에만 검색 토큰이 있는 글
		boardMapper.insertBoard(BoardFixtures.board(
				CK_BODY_ROW_ID, "CK", "본문 검색용 공지", "<p>" + BODY_TOKEN + "</p>"));
		// 자료실(TOAST)도 같은 규칙을 쓰는지 확인할 글
		boardMapper.insertBoard(BoardFixtures.board(
				TOAST_ROW_ID, "TOAST", "자료실 " + TITLE_TOKEN, "<p>" + BODY_TOKEN + "</p>"));
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> resultList(MvcResult result) {
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	private Object modelValue(MvcResult result, String name) {
		return result.getModelAndView().getModel().get(name);
	}

	private MvcResult ckList(String condition, String keyword) throws Exception {
		return mockMvc.perform(get("/board/ckeditorList")
						.session(TestSessions.admin("admin"))
						.param("searchCondition", condition)
						.param("searchKeyword", keyword))
				.andExpect(status().isOk())
				.andReturn();
	}

	@Test
	@Rollback
	void ckContentSearchShouldMatchBodyOnly() throws Exception {
		MvcResult body = ckList("content", BODY_TOKEN);
		assertThat(resultList(body)).extracting(row -> row.get("boardId"))
				.containsExactly(CK_BODY_ROW_ID);
		assertThat(modelValue(body, "searchCondition")).isEqualTo("content");

		// 제목에만 있는 토큰은 내용 검색에서 걸리지 않아야 한다.
		assertThat(resultList(ckList("content", TITLE_TOKEN))).isEmpty();
	}

	@Test
	@Rollback
	void ckTitleSearchShouldMatchTitleOnly() throws Exception {
		MvcResult title = ckList("title", TITLE_TOKEN);
		assertThat(resultList(title)).extracting(row -> row.get("boardId"))
				.containsExactly(CK_ROW_ID);

		assertThat(resultList(ckList("title", BODY_TOKEN))).isEmpty();
	}

	@Test
	@Rollback
	void ckAllSearchShouldMatchTitleOrBody() throws Exception {
		MvcResult all = ckList("all", TITLE_TOKEN);
		assertThat(resultList(all)).extracting(row -> row.get("boardId"))
				.containsExactly(CK_ROW_ID);

		// 제목+내용이므로 본문 토큰도 같은 범위에서 잡힌다.
		assertThat(resultList(ckList("all", BODY_TOKEN))).extracting(row -> row.get("boardId"))
				.containsExactly(CK_BODY_ROW_ID);
	}

	@Test
	@Rollback
	void unknownConditionShouldFallBackToTitle() throws Exception {
		MvcResult injected = ckList("content' OR '1'='1", BODY_TOKEN);
		assertThat(modelValue(injected, "searchCondition")).isEqualTo("title");
		// 제목 기준으로 떨어지므로 본문에만 있는 토큰은 결과가 없다.
		assertThat(resultList(injected)).isEmpty();

		MvcResult empty = ckList("", TITLE_TOKEN);
		assertThat(modelValue(empty, "searchCondition")).isEqualTo("title");
		assertThat(resultList(empty)).extracting(row -> row.get("boardId"))
				.containsExactly(CK_ROW_ID);
	}

	@Test
	@Rollback
	void toastCursorShouldHonorContentSearch() throws Exception {
		mockMvc.perform(post("/board/toastList.json")
						.session(TestSessions.admin("admin"))
						.param("searchCondition", "content")
						.param("searchKeyword", BODY_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.list.length()").value(1))
				.andExpect(jsonPath("$.list[0].boardId").value(TOAST_ROW_ID));

		// 제목 검색어로는 자료실 본문이 걸리지 않는다.
		mockMvc.perform(post("/board/toastList.json")
						.session(TestSessions.admin("admin"))
						.param("searchCondition", "content")
						.param("searchKeyword", "자료실"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.list.length()").value(0));
	}

	@Test
	@Rollback
	void userNoticeShouldHonorContentSearch() throws Exception {
		MvcResult content = mockMvc.perform(get("/user/board/noticeList")
						.session(TestSessions.user("tester"))
						.param("searchCondition", "content")
						.param("searchKeyword", BODY_TOKEN))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(resultList(content)).extracting(row -> row.get("boardId"))
				.containsExactly(CK_BODY_ROW_ID);
		assertThat(modelValue(content, "searchCondition")).isEqualTo("content");

		MvcResult fallback = mockMvc.perform(get("/user/board/noticeList")
						.session(TestSessions.user("tester"))
						.param("searchCondition", "unknown")
						.param("searchKeyword", BODY_TOKEN))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(modelValue(fallback, "searchCondition")).isEqualTo("title");
		assertThat(resultList(fallback)).isEmpty();
	}

	@Test
	@Rollback
	void userArchiveShouldKeepWhitelistedCondition() throws Exception {
		MvcResult all = mockMvc.perform(get("/user/board/archiveList")
						.session(TestSessions.user("tester"))
						.param("searchCondition", "all")
						.param("searchKeyword", BODY_TOKEN))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(modelValue(all, "searchCondition")).isEqualTo("all");
		assertThat(modelValue(all, "searchKeyword")).isEqualTo(BODY_TOKEN);

		MvcResult fallback = mockMvc.perform(get("/user/board/archiveList")
						.session(TestSessions.user("tester"))
						.param("searchCondition", "CONTENT")
						.param("searchKeyword", BODY_TOKEN))
				.andExpect(status().isOk())
				.andReturn();
		// 대소문자가 다른 값은 화이트리스트에 없으므로 title로 떨어진다.
		assertThat(modelValue(fallback, "searchCondition")).isEqualTo("title");
	}
}
