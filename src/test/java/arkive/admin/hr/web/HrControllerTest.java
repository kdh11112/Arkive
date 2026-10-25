package arkive.admin.hr.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.duty.service.DutyService;
import arkive.admin.hr.service.impl.HrMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * HR 세트의 화면 권한, 기념일·경조사·포상 CRUD 검증, 개인연차의 휴가·당직 연계를 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class HrControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private HrMapper hrMapper;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private DutyService dutyService;

	@Test
	void hrScreensShouldRequireAdminMenuPermission() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		mockMvc.perform(get("/hr/anniversaryList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("기념일관리")));
		mockMvc.perform(get("/hr/eventList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("경조사관리")));
		mockMvc.perform(get("/hr/rewardList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("포상관리")));
		mockMvc.perform(get("/hr/personalLeave").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("개인연차")));

		mockMvc.perform(get("/hr/anniversaryList").session(TestSessions.user("hr-outsider")))
				.andExpect(status().isForbidden());
	}

	@Test
	void anniversaryShouldFollowCrudAndMonthDayValidation() throws Exception {
		insertUser("hr-anniv", "기념일대상");
		MockHttpSession admin = TestSessions.admin("admin");

		mockMvc.perform(post("/hr/insertAnniversary.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_ANNIV"))
				.param("userId", "hr-anniv")
				.param("annivType", "BIRTH")
				.param("annivDe", "12-31")
				.param("title", "생일")
				.param("remark", "테스트")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap param = new EgovMap();
		param.put("month", "12");
		param.put("useYn", "Y");
		param.put("searchKeyword", "기념일대상");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 10);
		EgovMap row = hrMapper.selectAnniversaryList(param).get(0);
		String annivId = String.valueOf(row.get("annivId"));
		assertThat(row.get("title")).isEqualTo("생일");

		mockMvc.perform(post("/hr/updateAnniversary.json").session(admin)
				.param("annivId", annivId)
				.param("userId", "hr-anniv")
				.param("annivType", "ETC")
				.param("annivDe", "11-30")
				.param("title", "기념일 수정")
				.param("remark", "")
				.param("useYn", "N"))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap updated = hrMapper.selectAnniversaryDetail(annivId);
		assertThat(updated.get("annivDe")).isEqualTo("11-30");
		assertThat(updated.get("useYn")).isEqualTo("N");

		mockMvc.perform(post("/hr/insertAnniversary.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_ANNIV"))
				.param("userId", "hr-anniv")
				.param("annivType", "BIRTH")
				.param("annivDe", "13-01")
				.param("title", "잘못된 날짜")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("MM-DD")));

		mockMvc.perform(post("/hr/deleteAnniversary.json").session(admin).param("annivId", annivId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(hrMapper.selectAnniversaryDetail(annivId)).isNull();
	}

	@Test
	void eventAndRewardShouldValidateAmountAndDate() throws Exception {
		insertUser("hr-event", "경조대상");
		MockHttpSession admin = TestSessions.admin("admin");

		mockMvc.perform(post("/hr/insertEvent.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_EVENT"))
				.param("userId", "hr-event")
				.param("eventType", "WEDDING")
				.param("eventDe", "2031-05-10")
				.param("title", "결혼")
				.param("amount", "100000")
				.param("remark", "")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap eventParam = new EgovMap();
		eventParam.put("year", "2031");
		eventParam.put("eventType", "");
		eventParam.put("searchKeyword", "경조대상");
		eventParam.put("firstIndex", 0);
		eventParam.put("recordCountPerPage", 10);
		String eventId = String.valueOf(hrMapper.selectEventList(eventParam).get(0).get("eventId"));

		mockMvc.perform(post("/hr/insertEvent.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_EVENT"))
				.param("userId", "hr-event")
				.param("eventType", "WEDDING")
				.param("eventDe", "2031-02-30")
				.param("title", "잘못된 날짜")
				.param("amount", "100000")
				.param("remark", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("YYYY-MM-DD")));

		mockMvc.perform(post("/hr/insertEvent.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_EVENT"))
				.param("userId", "hr-event")
				.param("eventType", "WEDDING")
				.param("eventDe", "2031-05-11")
				.param("title", "잘못된 금액")
				.param("amount", "-1")
				.param("remark", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("금액")));

		mockMvc.perform(post("/hr/deleteEvent.json").session(admin).param("eventId", eventId))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/hr/insertReward.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "HR_REWARD"))
				.param("userId", "hr-event")
				.param("rewardType", "EXCELLENT")
				.param("rewardDe", "2031-12-20")
				.param("title", "우수사원")
				.param("amount", "500000")
				.param("rewarder", "대표")
				.param("remark", ""))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap rewardParam = new EgovMap();
		rewardParam.put("year", "2031");
		rewardParam.put("rewardType", "EXCELLENT");
		rewardParam.put("searchKeyword", "경조대상");
		rewardParam.put("firstIndex", 0);
		rewardParam.put("recordCountPerPage", 10);
		String rewardId = String.valueOf(hrMapper.selectRewardList(rewardParam).get(0).get("rewardId"));

		mockMvc.perform(post("/hr/updateReward.json").session(admin)
				.param("rewardId", rewardId)
				.param("userId", "hr-event")
				.param("rewardType", "UNKNOWN")
				.param("rewardDe", "2031-12-20")
				.param("title", "우수사원")
				.param("amount", "500000")
				.param("rewarder", "대표"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("포상 구분")));

		mockMvc.perform(post("/hr/deleteReward.json").session(admin).param("rewardId", rewardId))
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void personalLeaveShouldShowBalanceLeaveAndDuty() throws Exception {
		insertUser("hr-personal", "인사테스터");
		MockHttpSession admin = TestSessions.admin("admin");

		EgovMap duty = new EgovMap();
		duty.put("dutyId", "DUTY-HR-TEST");
		duty.put("dutyDe", "20310115");
		duty.put("dutyUser", "인사테스터");
		duty.put("note", "개인연차 연계 확인");
		duty.put("register", "admin");
		dutyService.insertDuty(duty);

		mockMvc.perform(post("/hr/savePersonalLeave.json").session(admin)
				.param("userId", "hr-personal")
				.param("leaveYear", "2031")
				.param("totalDays", "12"))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/hr/personalLeave").session(admin)
				.param("userId", "hr-personal")
				.param("leaveYear", "2031"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("인사테스터")))
				.andExpect(content().string(containsString("12.0일")))
				.andExpect(content().string(containsString("20310115")));

		mockMvc.perform(post("/hr/savePersonalLeave.json").session(admin)
				.param("userId", "hr-personal")
				.param("leaveYear", "2031")
				.param("totalDays", "abc"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("숫자")));
	}

	private void insertUser(String userId, String userNm) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", "encoded-test-password");
		user.put("userNm", userNm);
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}
}
