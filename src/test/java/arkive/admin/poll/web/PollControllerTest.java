package arkive.admin.poll.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * POLL 보기 수 제한·기간/사용여부 검사·세션 중복투표·수정 시 보기 재등록·삭제 CASCADE를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class PollControllerTest {

	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertPoll(MockHttpSession session, String title, String startYmd, String endYmd,
			String useYn, String items) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		MockHttpServletRequestBuilder builder = post("/poll/insertPoll.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("title", title)
				.param("startYmd", startYmd)
				.param("endYmd", endYmd)
				.param("items", items);
		if (useYn != null) {
			builder.param("useYn", useYn);
		}
		MvcResult result = mockMvc.perform(builder)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.pollId");
	}

	private String itemId(String pollId, int ordr) {
		return jdbcTemplate.queryForObject(
				"SELECT ITEM_ID FROM POLL_ITEM WHERE POLL_ID = ? AND ORDR = ?", String.class, pollId, ordr);
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestItems(MockHttpSession session, String pollId) throws Exception {
		MvcResult result = mockMvc.perform(get("/poll/pollDetail")
						.session(session)
						.param("pollId", pollId))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("itemList");
	}

	@SuppressWarnings("unchecked")
	private EgovMap findPollRow(MockHttpSession session, String title, String pollId) throws Exception {
		MvcResult result = mockMvc.perform(get("/poll/pollList")
						.session(session)
						.param("searchKeyword", title))
				.andExpect(status().isOk())
				.andReturn();
		List<EgovMap> rows = (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
		return rows.stream()
				.filter(row -> pollId.equals(String.valueOf(row.get("pollId"))))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@Rollback
	void pollShouldVoteOncePerSessionAndResetItemsOnUpdate() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String title = "ZZ투표-" + System.nanoTime();
		LocalDate today = LocalDate.now();
		String start = today.minusDays(1).format(BASIC);
		String end = today.plusDays(1).format(BASIC);
		String pollId = insertPoll(session, title, start, end, "Y", "찬성\n반대\n기권");

		List<EgovMap> items = requestItems(session, pollId);
		assertThat(items).extracting(row -> String.valueOf(row.get("itemNm")))
				.containsExactly("찬성", "반대", "기권");

		mockMvc.perform(post("/poll/votePoll.json")
						.session(session)
						.param("pollId", pollId)
						.param("itemId", itemId(pollId, 1)))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT VOTE_CNT FROM POLL_ITEM WHERE POLL_ID = ? AND ORDR = 1", Integer.class, pollId))
				.isEqualTo(1);

		mockMvc.perform(post("/poll/votePoll.json")
						.session(session)
						.param("pollId", pollId)
						.param("itemId", itemId(pollId, 2)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이미 투표했습니다."));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COALESCE(SUM(VOTE_CNT), 0) FROM POLL_ITEM WHERE POLL_ID = ?", Integer.class, pollId))
				.isEqualTo(1);

		MockHttpSession otherSession = TestSessions.admin("voter2");
		mockMvc.perform(post("/poll/votePoll.json")
						.session(otherSession)
						.param("pollId", pollId)
						.param("itemId", itemId(pollId, 2)))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(Integer.parseInt(String.valueOf(findPollRow(session, title, pollId).get("totalVote"))))
				.isEqualTo(2);

		mockMvc.perform(post("/poll/updatePoll.json")
						.session(session)
						.param("pollId", pollId)
						.param("title", title + "-수정")
						.param("startYmd", start)
						.param("endYmd", end)
						.param("useYn", "Y")
						.param("items", "A안\nB안"))
				.andExpect(jsonPath("$.success").value(true));
		items = requestItems(session, pollId);
		assertThat(items).extracting(row -> String.valueOf(row.get("itemNm")))
				.containsExactly("A안", "B안");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COALESCE(SUM(VOTE_CNT), 0) FROM POLL_ITEM WHERE POLL_ID = ?", Integer.class, pollId))
				.isZero();

		mockMvc.perform(post("/poll/deletePoll.json")
						.session(session)
						.param("pollId", pollId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM POLL WHERE POLL_ID = ?", Integer.class, pollId)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM POLL_ITEM WHERE POLL_ID = ?", Integer.class, pollId)).isZero();
	}

	@Test
	@Rollback
	void pollShouldValidateItemsAndVotingPeriod() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		LocalDate today = LocalDate.now();
		String start = today.minusDays(1).format(BASIC);
		String end = today.plusDays(1).format(BASIC);
		String title = "ZZ투표검증-" + System.nanoTime();

		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param("title", title)
						.param("startYmd", start)
						.param("endYmd", end)
						.param("items", "A\nB"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("startYmd", start)
						.param("endYmd", end)
						.param("items", "A\nB"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("startYmd", "2026-1-1")
						.param("endYmd", end)
						.param("items", "A\nB"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("투표시작일을 YYYYMMDD 형식으로 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("startYmd", end)
						.param("endYmd", start)
						.param("items", "A\nB"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("투표시작일이 종료일보다 늦을 수 없습니다."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("startYmd", start)
						.param("endYmd", end)
						.param("items", "하나뿐"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("보기를 2개 이상 입력하세요."));

		StringBuilder tooMany = new StringBuilder();
		for (int i = 1; i <= 11; i++) {
			if (i > 1) {
				tooMany.append('\n');
			}
			tooMany.append("보기").append(i);
		}
		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POLL");
		mockMvc.perform(post("/poll/insertPoll.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("startYmd", start)
						.param("endYmd", end)
						.param("items", tooMany.toString()))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("보기는 최대 10개까지 입력하세요."));

		String inactiveId = insertPoll(session, title + "-미사용", start, end, "N", "A\nB");
		mockMvc.perform(post("/poll/votePoll.json")
						.session(session)
						.param("pollId", inactiveId)
						.param("itemId", itemId(inactiveId, 1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("진행 중인 투표가 아닙니다."));

		String expiredId = insertPoll(session, title + "-종료",
				today.minusDays(5).format(BASIC), today.minusDays(1).format(BASIC), "Y", "A\nB");
		mockMvc.perform(post("/poll/votePoll.json")
						.session(TestSessions.admin("voter3"))
						.param("pollId", expiredId)
						.param("itemId", itemId(expiredId, 1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("투표 기간이 아닙니다."));

		String futureId = insertPoll(session, title + "-예정",
				today.plusDays(1).format(BASIC), today.plusDays(2).format(BASIC), "Y", "A\nB");
		mockMvc.perform(post("/poll/votePoll.json")
						.session(TestSessions.admin("voter4"))
						.param("pollId", futureId)
						.param("itemId", itemId(futureId, 1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("투표 기간이 아닙니다."));

		mockMvc.perform(post("/poll/votePoll.json")
						.session(session)
						.param("pollId", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("투표 정보가 올바르지 않습니다."));

		mockMvc.perform(post("/poll/votePoll.json")
						.session(session)
						.param("pollId", "PL-NONE")
						.param("itemId", "PI-NONE"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("진행 중인 투표가 아닙니다."));
	}
}
