package arkive.admin.search.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
 * 통합검색 화면의 로그인 요구, 결과 유형 표시, 본문 요약, 빈 검색어·무결과
 * 안내를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class SearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	private void insert(String boardId, String boardType, String title, String content) throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(boardId, boardType, title, content));
	}

	@Test
	void searchShouldRequireLogin() throws Exception {
		mockMvc.perform(get("/search").param("searchKeyword", "통합검색"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login/login"));
	}

	@Test
	@Rollback
	void searchShouldRenderMatchingResultsAndSnippet() throws Exception {
		insert("BDSRCH00000000000011", "CK", "통합검색 공지", "<p>공지 본문</p>");
		insert("BDSRCH00000000000012", "TOAST", "통합검색 자료", "<p>본문요약 테스트</p>");
		insert("BDSRCH00000000000013", "FAQ", "통합검색 FAQ", "<p>FAQ 본문</p>");
		insert("BDSRCH00000000000014", "QNA", "통합검색 질문", "<p>질문 본문</p>");
		insert("BDSRCH00000000000015", "CK", "제외 글", "<p>제외 본문</p>");
		insert("BDSRCH00000000000016", "CK", "본문만 일치", "<p>통합검색</p>");

		mockMvc.perform(get("/search")
						.session(TestSessions.user("tester"))
						.param("searchKeyword", "통합검색"))
				.andExpect(status().isOk())
				.andExpect(view().name("search/searchList"))
				.andExpect(model().attribute("searched", true))
				.andExpect(content().string(containsString("통합검색 공지")))
				.andExpect(content().string(containsString("통합검색 자료")))
				.andExpect(content().string(containsString("통합검색 FAQ")))
				.andExpect(content().string(containsString("통합검색 질문")))
				.andExpect(content().string(containsString("공지사항")))
				.andExpect(content().string(containsString("자료실")))
				.andExpect(content().string(containsString("Q&amp;A")))
				.andExpect(content().string(containsString("본문요약 테스트")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("제외 글"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("본문만 일치"))));
	}

	@Test
	void blankKeywordShouldShowPrompt() throws Exception {
		mockMvc.perform(get("/search").session(TestSessions.user("tester")))
				.andExpect(status().isOk())
				.andExpect(view().name("search/searchList"))
				.andExpect(model().attribute("searched", false))
				.andExpect(content().string(containsString("검색어를 입력하세요.")));
	}

	@Test
	void unknownKeywordShouldShowEmptyMessage() throws Exception {
		mockMvc.perform(get("/search")
						.session(TestSessions.user("tester"))
						.param("searchKeyword", "존재하지않는검색어"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("검색 결과가 없습니다.")));
	}
}
