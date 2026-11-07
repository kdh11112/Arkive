package arkive.admin.board.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * 게시판 목록·상세 마크업의 링크/접근성 회귀를 막는다.
 * - TOAST 목록 행은 javascript:void(0) 대신 실제 href를 쓴다(새 탭·링크 복사 가능)
 * - 목록 고정 버튼은 화면 문구와 별개로 접근성 이름을 가진다
 * - 댓글 입력란은 name과 접근성 이름을 가진다
 */
@ArkiveIntegrationTest
@Transactional
class BoardMarkupAccessibilityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	@Test
	@Rollback
	void ckListPinButtonShouldHaveAccessibleName() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board("BDMARKUP01", "CK", "마크업 테스트", "<p>본문</p>"));

		mockMvc.perform(get("/board/ckeditorList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("aria-label=\"공지로 고정\"")));
	}

	@Test
	void toastListShouldBuildRealDetailHref() throws Exception {
		mockMvc.perform(get("/board/toastList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("href=\"/board/detail?boardId=")))
				.andExpect(content().string(not(containsString("javascript:void(0)"))))
				.andExpect(content().string(not(containsString("onclick=\\\"toastGoDetail"))));
	}

	@Test
	@Rollback
	void commentTextareaShouldHaveNameAndAccessibleName() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board("BDMARKUP02", "CK", "댓글 마크업", "<p>본문</p>"));

		mockMvc.perform(get("/board/detail").param("boardId", "BDMARKUP02")
						.session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"commentContent\"")))
				.andExpect(content().string(containsString("aria-label=\"댓글 내용\"")));
	}
}
