package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.admin.comm.web.BoardViewTracker;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import jakarta.servlet.http.Cookie;

/**
 * 조회수 집계 정책: 게시글 1건은 브라우저(세션·쿠키)당 24시간에 한 번만 센다.
 * 24시간이 지난 뒤 다시 보면 다시 1회 추가된다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardViewCountDailyWindowTest {

	private static final String BOARD_ID = "BDVIEWWINDOW000001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@Rollback
	void sameSessionShouldCountOnlyOnce() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));
		MockHttpSession session = TestSessions.admin("viewer");

		view(session, null);
		view(session, null);
		view(session, null);

		assertThat(viewCount()).isEqualTo(1);
	}

	@Test
	@Rollback
	void shouldCountAgainAfterTwentyFourHoursInSameSession() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));
		MockHttpSession session = TestSessions.admin("viewer");

		// 23시간 전에 봤으면 아직 집계하지 않는다.
		markSession(session, 23L * 60L * 60L * 1000L);
		view(session, null);
		assertThat(viewCount()).isEqualTo(0);

		// 25시간 전으로 바꾸면 24시간이 지나 다시 집계한다.
		markSession(session, 25L * 60L * 60L * 1000L);
		view(session, null);
		assertThat(viewCount()).isEqualTo(1);
	}

	@Test
	@Rollback
	void cookieShouldBlockRecountWhenSessionIsNew() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));

		// 브라우저를 닫아 세션이 새로 생겨도 24시간 안이면 쿠키로 막는다.
		view(TestSessions.admin("cookieViewer"), new Cookie(BoardViewTracker.COOKIE_PREFIX + BOARD_ID,
				String.valueOf(System.currentTimeMillis() - 1000L)));
		assertThat(viewCount()).isEqualTo(0);
	}

	@Test
	@Rollback
	void expiredCookieShouldCountAgain() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));

		view(TestSessions.admin("cookieViewer"), new Cookie(BoardViewTracker.COOKIE_PREFIX + BOARD_ID,
				String.valueOf(System.currentTimeMillis() - 25L * 60L * 60L * 1000L)));
		assertThat(viewCount()).isEqualTo(1);
	}

	@Test
	@Rollback
	void userNoticeDetailShouldFollowTheSameRule() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(get("/user/board/noticeDetail").param("boardId", BOARD_ID).session(session))
				.andExpect(status().isOk());
		mockMvc.perform(get("/user/board/noticeDetail").param("boardId", BOARD_ID).session(session))
				.andExpect(status().isOk());

		assertThat(viewCount()).isEqualTo(1);
	}

	@Test
	@Rollback
	void shouldDropDailyCookieForBrowserSideExpiry() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "조회수 정책", "<p>body</p>"));

		MvcResult result = view(TestSessions.admin("viewer"), null);

		assertThat(result.getResponse().getHeader("Set-Cookie"))
				.contains(BoardViewTracker.COOKIE_PREFIX + BOARD_ID)
				.contains("Max-Age=86400")
				.contains("HttpOnly");
	}

	private MvcResult view(MockHttpSession session, Cookie cookie) throws Exception {
		var request = get("/board/detail").param("boardId", BOARD_ID);
		if (session != null) {
			request.session(session);
		}
		if (cookie != null) {
			request.cookie(cookie);
		}
		return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
	}

	private void markSession(MockHttpSession session, long ageMs) {
		Map<String, Long> times = new HashMap<>();
		times.put(BOARD_ID, System.currentTimeMillis() - ageMs);
		session.setAttribute(BoardViewTracker.SESSION_KEY, times);
	}

	private int viewCount() {
		return jdbcTemplate.queryForObject("SELECT VIEW_CNT FROM BOARD WHERE BOARD_ID = ?", Integer.class, BOARD_ID);
	}
}
