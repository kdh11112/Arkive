package arkive.admin.stats.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 통계 화면의 기간 필터·감사 로그 집계와 엑셀 다운로드 형식을 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class StatsControllerTest {

	private static final String START = "2031-07-01";
	private static final String END = "2031-07-31";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void statsShouldAggregateAuditEventsAndDownloads() throws Exception {
		insertAudit("ST-1", "LOGIN_SUCCESS", null, "2031-07-10 09:00:00");
		insertAudit("ST-2", "MENU_ACCESS", null, "2031-07-10 10:00:00");
		insertAudit("ST-3", "PII_DOWNLOAD", "ADDRESS", "2031-07-11 10:00:00");
		insertAudit("ST-4", "PII_DOWNLOAD", "ADDRESS", "2031-07-12 10:00:00");

		MvcResult result = mockMvc.perform(get("/stats/statsList").session(TestSessions.admin("admin"))
				.param("startDate", START)
				.param("endDate", END))
				.andExpect(status().isOk())
				.andExpect(model().attribute("startDate", START))
				.andExpect(model().attribute("endDate", END))
				.andReturn();

		List<EgovMap> events = rows(result, "eventStats");
		assertThat(cnt(events, "eventType", "LOGIN_SUCCESS")).isEqualTo(1L);
		assertThat(cnt(events, "eventType", "MENU_ACCESS")).isEqualTo(1L);
		assertThat(cnt(events, "eventType", "PII_DOWNLOAD")).isEqualTo(2L);

		List<EgovMap> logins = rows(result, "dailyLoginStats");
		assertThat(logins).hasSize(1);
		assertThat(String.valueOf(logins.get(0).get("statDate"))).isEqualTo("2031-07-10");
		assertThat(toLong(logins.get(0).get("cnt"))).isEqualTo(1L);

		List<EgovMap> access = rows(result, "dailyAccessStats");
		assertThat(access).hasSize(3);
		assertThat(cnt(access, "statDate", "2031-07-10")).isEqualTo(1L);
		assertThat(cnt(access, "statDate", "2031-07-11")).isEqualTo(1L);
		assertThat(cnt(access, "statDate", "2031-07-12")).isEqualTo(1L);

		List<EgovMap> downloads = rows(result, "downloadStats");
		assertThat(cnt(downloads, "targetType", "ADDRESS")).isEqualTo(2L);
	}

	@Test
	void statsShouldSwapReversedDatesAndDownloadExcel() throws Exception {
		insertAudit("ST-5", "LOGIN_SUCCESS", null, "2031-07-20 09:00:00");
		insertAudit("ST-6", "PII_DOWNLOAD", "CARD", "2031-07-20 11:00:00");

		mockMvc.perform(get("/stats/statsList").session(TestSessions.admin("admin"))
				.param("startDate", END)
				.param("endDate", START))
				.andExpect(status().isOk())
				.andExpect(model().attribute("startDate", START))
				.andExpect(model().attribute("endDate", END));

		String fallbackStart = LocalDate.now().minusDays(6).toString();
		String fallbackEnd = LocalDate.now().toString();
		mockMvc.perform(get("/stats/statsList").session(TestSessions.admin("admin"))
				.param("startDate", "not-a-date")
				.param("endDate", ""))
				.andExpect(status().isOk())
				.andExpect(model().attribute("startDate", fallbackStart))
				.andExpect(model().attribute("endDate", fallbackEnd));

		MockHttpServletResponse response = mockMvc.perform(get("/stats/downloadStatsExcel.do")
				.session(TestSessions.admin("admin"))
				.param("startDate", START)
				.param("endDate", END))
				.andExpect(status().isOk())
				.andReturn().getResponse();
		assertThat(response.getContentType()).contains("application/vnd.ms-excel");
		assertThat(response.getHeader("Content-Disposition")).contains("attachment");

		HSSFWorkbook workbook = new HSSFWorkbook(
				new ByteArrayInputStream(response.getContentAsByteArray()));
		HSSFSheet sheet = workbook.getSheetAt(0);
		assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("구분");
		assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("항목");
		assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("건수");
		boolean loginRow = false;
		boolean cardRow = false;
		for (int i = 3; i <= sheet.getLastRowNum(); i++) {
			if (sheet.getRow(i) == null) {
				continue;
			}
			String category = sheet.getRow(i).getCell(0).getStringCellValue();
			String name = sheet.getRow(i).getCell(1).getStringCellValue();
			if ("이벤트".equals(category) && "LOGIN_SUCCESS".equals(name)) {
				loginRow = true;
			}
			if ("다운로드".equals(category) && "CARD".equals(name)) {
				cardRow = true;
			}
		}
		assertThat(loginRow).isTrue();
		assertThat(cardRow).isTrue();
	}

	private void insertAudit(String logId, String eventType, String targetType, String registDt) {
		jdbcTemplate.update("INSERT INTO AUDIT_LOG (LOG_ID, EVENT_TYPE, USER_ID, USER_NM, TARGET_TYPE, "
				+ "REQUEST_URI, HTTP_METHOD, CLIENT_IP, RESULT, REGIST_DT) "
				+ "VALUES (?, ?, 'stats-user', '통계사용자', ?, '/stats/test', 'GET', '127.0.0.1', 'SUCCESS', "
				+ "CAST(? AS TIMESTAMP))",
				logId, eventType, targetType, registDt);
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> rows(MvcResult result, String name) {
		return (List<EgovMap>) result.getModelAndView().getModel().get(name);
	}

	private long cnt(List<EgovMap> rows, String key, String value) {
		return rows.stream()
				.filter(row -> value.equals(String.valueOf(row.get(key))))
				.mapToLong(row -> toLong(row.get("cnt")))
				.sum();
	}

	private long toLong(Object value) {
		return value == null ? 0L : Long.parseLong(String.valueOf(value));
	}
}
