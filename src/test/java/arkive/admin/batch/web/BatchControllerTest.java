package arkive.admin.batch.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 일일 통계 배치의 집계 결과와 같은 날짜 재실행 멱등성을 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class BatchControllerTest {

	private static final String STAT_DATE = "2020-01-02";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void dailyStatsShouldAggregateAndBeIdempotent() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		insertAudit("BATCH-1", "LOGIN_SUCCESS", STAT_DATE + " 09:00:00");
		insertAudit("BATCH-2", "MENU_ACCESS", STAT_DATE + " 10:00:00");

		run(session, STAT_DATE)
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.targetDate").value(STAT_DATE));
		assertThat(statValue("LOGIN")).isEqualTo(1L);
		assertThat(statValue("ACCESS")).isEqualTo(1L);

		insertAudit("BATCH-3", "LOGIN_SUCCESS", STAT_DATE + " 11:00:00");
		run(session, STAT_DATE)
				.andExpect(jsonPath("$.success").value(true));
		assertThat(statValue("LOGIN")).isEqualTo(2L);
		Integer loginRows = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM STAT_DAILY WHERE STAT_DATE = CAST(? AS DATE) AND STAT_TYPE = 'LOGIN'",
				Integer.class, STAT_DATE);
		assertThat(loginRows).isEqualTo(1);

		MvcResult list = mockMvc.perform(get("/batch/batchList").session(session).param("statDate", STAT_DATE))
				.andExpect(status().isOk())
				.andExpect(model().attribute("statDate", STAT_DATE))
				.andExpect(model().attributeExists("doubleSubmitToken"))
				.andReturn();
		@SuppressWarnings("unchecked")
		List<EgovMap> stats = (List<EgovMap>) list.getModelAndView().getModel().get("resultList");
		assertThat(stats).anyMatch(row -> "LOGIN".equals(String.valueOf(row.get("statType")))
				&& Long.parseLong(String.valueOf(row.get("statValue"))) == 2L);
	}

	@Test
	void batchShouldValidateTokenAndDate() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/batch/runDailyStats.json").session(session).param("statDate", STAT_DATE))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		expectRunFailure(session, "", "집계 날짜를 입력하세요.");
		expectRunFailure(session, "2020/01/02", "집계 날짜 형식이 올바르지 않습니다.");
		expectRunFailure(session, LocalDate.now().plusDays(1).toString(), "미래 날짜는 집계할 수 없습니다.");

		String yesterday = LocalDate.now().minusDays(1).toString();
		mockMvc.perform(get("/batch/batchList").session(session))
				.andExpect(status().isOk())
				.andExpect(model().attribute("statDate", yesterday));
		mockMvc.perform(get("/batch/batchList").session(session).param("statDate", "2020/01/02"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("statDate", yesterday));
	}

	private org.springframework.test.web.servlet.ResultActions run(MockHttpSession session, String statDate)
			throws Exception {
		return mockMvc.perform(post("/batch/runDailyStats.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "BATCH_STATS"))
				.param("statDate", statDate));
	}

	private void expectRunFailure(MockHttpSession session, String statDate, String message) throws Exception {
		run(session, statDate)
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(message));
	}

	private void insertAudit(String logId, String eventType, String registDt) {
		jdbcTemplate.update("INSERT INTO AUDIT_LOG (LOG_ID, EVENT_TYPE, USER_ID, USER_NM, TARGET_TYPE, "
				+ "REQUEST_URI, HTTP_METHOD, CLIENT_IP, RESULT, REGIST_DT) "
				+ "VALUES (?, ?, 'batch-user', '배치사용자', NULL, '/batch/test', 'GET', '127.0.0.1', "
				+ "'SUCCESS', CAST(? AS TIMESTAMP))",
				logId, eventType, registDt);
	}

	private long statValue(String statType) {
		Long value = jdbcTemplate.queryForObject(
				"SELECT STAT_VALUE FROM STAT_DAILY WHERE STAT_DATE = CAST(? AS DATE) AND STAT_TYPE = ? "
						+ "AND STAT_KEY = 'TOTAL'",
				Long.class, STAT_DATE, statType);
		return value == null ? 0L : value;
	}
}
