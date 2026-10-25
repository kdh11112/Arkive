package arkive.admin.schedule.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.jayway.jsonpath.JsonPath;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 일정 기간 조회·유형 필터·날짜 검증·공휴일 MERGE/삭제를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class ScheduleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertSchedule(MockHttpSession session, String title, String startDt, String endDt,
			String scheduleType) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_SCHEDULE");
		MvcResult result = mockMvc.perform(post("/schedule/insertSchedule.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("content", "<p>일정 본문</p>")
						.param("startDt", startDt)
						.param("endDt", endDt)
						.param("scheduleType", scheduleType))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.scheduleId");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestScheduleList(MockHttpSession session, String view, String date,
			String scheduleType, String searchKeyword) throws Exception {
		MvcResult result = mockMvc.perform(get("/schedule/scheduleList")
						.session(session)
						.param("view", view)
						.param("date", date)
						.param("scheduleType", scheduleType)
						.param("searchKeyword", searchKeyword))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	@Test
	@Rollback
	void scheduleShouldRoundTripAndFilterByTypeAndDay() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String title = "ZZ일정-" + System.nanoTime();
		String scheduleId = insertSchedule(session, title, "2031-05-10T09:00", "2031-05-10T10:30", "PERSONAL");

		assertThat(jdbcTemplate.queryForObject(
				"SELECT START_DT FROM SCHEDULE WHERE SCHEDULE_ID = ?", String.class, scheduleId))
				.isEqualTo("2031-05-10 09:00");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT SCHEDULE_TYPE FROM SCHEDULE WHERE SCHEDULE_ID = ?", String.class, scheduleId))
				.isEqualTo("PERSONAL");

		insertSchedule(session, title + "-부서", "2031-05-10 09:00", "2031-05-10 10:30", "DEPT");

		List<EgovMap> personal = requestScheduleList(session, "day", "2031-05-10", "PERSONAL", title);
		assertThat(personal).hasSize(1);
		assertThat(String.valueOf(personal.get(0).get("scheduleId"))).isEqualTo(scheduleId);
		assertThat(String.valueOf(personal.get(0).get("scheduleType"))).isEqualTo("PERSONAL");

		List<EgovMap> all = requestScheduleList(session, "day", "2031-05-10", "", title);
		assertThat(all).hasSize(2);

		mockMvc.perform(post("/schedule/updateSchedule.json")
						.session(session)
						.param("scheduleId", scheduleId)
						.param("title", title + "-수정")
						.param("content", "<p>수정 본문</p>")
						.param("startDt", "2031-05-11T11:00")
						.param("endDt", "2031-05-11T12:00")
						.param("scheduleType", "TODO"))
				.andExpect(jsonPath("$.success").value(true));
		MvcResult detailResult = mockMvc.perform(get("/schedule/scheduleWrite")
						.session(session)
						.param("scheduleId", scheduleId))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("title"))).isEqualTo(title + "-수정");
		assertThat(String.valueOf(detail.get("content"))).contains("수정 본문");
		assertThat(String.valueOf(detail.get("startDt"))).isEqualTo("2031-05-11 11:00");

		mockMvc.perform(post("/schedule/deleteSchedule.json")
						.session(session)
						.param("scheduleId", scheduleId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM SCHEDULE WHERE SCHEDULE_ID = ?", Integer.class, scheduleId)).isZero();
	}

	@Test
	@Rollback
	void insertShouldValidateDateTimeAndNormalizeType() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/schedule/insertSchedule.json")
						.session(session)
						.param("title", "토큰 없는 일정")
						.param("startDt", "2031-05-10 09:00")
						.param("endDt", "2031-05-10 10:00"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_SCHEDULE");
		mockMvc.perform(post("/schedule/insertSchedule.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("startDt", "2031-05-10 09:00")
						.param("endDt", "2031-05-10 10:00"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_SCHEDULE");
		mockMvc.perform(post("/schedule/insertSchedule.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "형식 오류 일정")
						.param("startDt", "2031-05-10")
						.param("endDt", "2031-05-10 10:00"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("시작일시을 YYYY-MM-DD HH24:MI 형식으로 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_SCHEDULE");
		mockMvc.perform(post("/schedule/insertSchedule.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "역순 일정")
						.param("startDt", "2031-05-12 09:00")
						.param("endDt", "2031-05-11 09:00"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("시작일시가 종료일시보다 늦을 수 없습니다."));

		String scheduleId = insertSchedule(session, "ZZ유형정규화-" + System.nanoTime(),
				"2031-05-13 09:00", "2031-05-13 10:00", "TEAM");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT SCHEDULE_TYPE FROM SCHEDULE WHERE SCHEDULE_ID = ?", String.class, scheduleId))
				.isEqualTo("DEPT");
	}

	@SuppressWarnings("unchecked")
	@Test
	@Rollback
	void holidayShouldMergeByDateAndDelete() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String holidayDe = "20311225";

		mockMvc.perform(post("/schedule/insertHoliday.json")
						.session(session)
						.param("holidayDe", "2031-12-25")
						.param("holidayNm", "크리스마스"))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/schedule/insertHoliday.json")
						.session(session)
						.param("holidayDe", holidayDe)
						.param("holidayNm", "성탄절"))
				.andExpect(jsonPath("$.success").value(true));

		MvcResult listResult = mockMvc.perform(get("/schedule/holidayList")
						.session(session)
						.param("yyyymm", "203112"))
				.andExpect(status().isOk())
				.andReturn();
		List<EgovMap> holidays = (List<EgovMap>) listResult.getModelAndView().getModel().get("resultList");
		List<EgovMap> sameDay = holidays.stream()
				.filter(row -> holidayDe.equals(String.valueOf(row.get("holidayDe"))))
				.toList();
		assertThat(sameDay).hasSize(1);
		assertThat(String.valueOf(sameDay.get(0).get("holidayNm"))).isEqualTo("성탄절");

		mockMvc.perform(post("/schedule/insertHoliday.json")
						.session(session)
						.param("holidayDe", "20311232")
						.param("holidayNm", "없는 날짜"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("존재하지 않는 날짜입니다: 20311232"));

		mockMvc.perform(post("/schedule/insertHoliday.json")
						.session(session)
						.param("holidayDe", holidayDe)
						.param("holidayNm", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("공휴일 이름을 입력하세요."));

		mockMvc.perform(post("/schedule/deleteHoliday.json")
						.session(session)
						.param("holidayDe", holidayDe))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM HOLIDAY WHERE HOLIDAY_DE = ?", Integer.class, holidayDe)).isZero();
	}
}
