package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.BoardService;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 조회수 통계 화면. 최근 14일 막대 그래프와 그 아래 숫자 표, 게시글별 누적 순위를 그린다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardViewStatsControllerTest {

	private static final String BOARD_ID = "BDSTATS000000000001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private BoardService boardService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@Rollback
	void statsPageShouldShowChartAndNumberTable() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "통계 테스트 글", "<p>body</p>"));
		boardService.updateViewCnt(BOARD_ID);
		boardService.insertViewLog(BOARD_ID, "CK");
		boardService.insertViewLog(BOARD_ID, "CK");

		MvcResult result = mockMvc.perform(get("/board/viewStats").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("조회수 통계")))
				.andExpect(content().string(containsString("background: #2563eb")))
				.andExpect(content().string(containsString("합계(14일)")))
				.andExpect(content().string(containsString("게시글별 누적 조회수")))
				.andExpect(content().string(containsString("통계 테스트 글")))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<EgovMap> dailyStats = (List<EgovMap>) result.getModelAndView().getModel().get("dailyStats");
		assertThat(dailyStats).hasSize(14);
		EgovMap today = dailyStats.get(dailyStats.size() - 1);
		assertThat(((Number) today.get("count")).longValue()).isEqualTo(2L);
		assertThat(((Number) today.get("heightPct")).longValue()).isEqualTo(100L);
		assertThat(((Number) result.getModelAndView().getModel().get("dailyTotal")).longValue()).isEqualTo(2L);
	}

	@Test
	@Rollback
	void countedViewShouldLeaveOneLogRow() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(BOARD_ID, "CK", "통계 로그", "<p>body</p>"));

		MockHttpSession session = TestSessions.admin("logUser");
		mockMvc.perform(get("/board/detail").param("boardId", BOARD_ID).session(session))
				.andExpect(status().isOk());
		mockMvc.perform(get("/board/detail").param("boardId", BOARD_ID).session(session))
				.andExpect(status().isOk());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM BOARD_VIEW_LOG WHERE BOARD_ID = ?", Integer.class, BOARD_ID)).isEqualTo(1);
	}

	@Test
	@Rollback
	void emptyLogShouldStillRenderZeroBars() throws Exception {
		mockMvc.perform(get("/board/viewStats").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("height: 0%")))
				.andExpect(content().string(containsString("게시글이 없습니다.")));
	}
}
