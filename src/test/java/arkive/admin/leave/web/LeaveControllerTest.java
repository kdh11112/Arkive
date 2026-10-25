package arkive.admin.leave.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.leave.service.impl.LeaveMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 휴가 신청·승인·반려·취소와 연차 차감/환원을 확인한다.
 * 테스트 연도(2030)를 따로 두어 현재 연도 시드 데이터와 섞이지 않게 한다.
 */
@ArkiveIntegrationTest
@Transactional
class LeaveControllerTest {

	private static final String YEAR = "2030";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LeaveMapper leaveMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@BeforeEach
	void grantAdminBalance() throws Exception {
		EgovMap param = balanceParam("admin");
		param.put("totalDays", new BigDecimal("10.0"));
		if (leaveMapper.countBalance(param) == 0) {
			leaveMapper.insertBalance(param);
		}
	}

	@Test
	void adminShouldRenderLeaveScreens() throws Exception {
		mockMvc.perform(get("/leave/myLeave").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("내 휴가")));
		mockMvc.perform(get("/leave/leaveList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("휴가관리")));
		mockMvc.perform(get("/leave/balanceList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("연차관리")));
	}

	@Test
	void myLeaveRoleShouldSeeOwnScreenButNotManageScreens() throws Exception {
		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", "leave-user");
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		MockHttpSession session = TestSessions.user("leave-user", "휴가사용자");

		mockMvc.perform(get("/leave/myLeave").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("내 휴가")));
		mockMvc.perform(get("/leave/leaveList").session(session))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/leave/balanceList").session(session))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/leave/approveLeave.json").session(session).param("leaveId", "LVX"))
				.andExpect(status().isForbidden());
	}

	@Test
	void insertShouldRequireTokenAndNotDeductBeforeApproval() throws Exception {
		MockHttpSession session = TestSessions.admin("admin", "관리자");

		mockMvc.perform(post("/leave/insertLeave.json").session(session)
				.param("leaveType", "FULL")
				.param("startDe", "2030-01-07")
				.param("endDe", "2030-01-08")
				.param("reason", "개인 사유"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "LEAVE");
		mockMvc.perform(post("/leave/insertLeave.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("leaveType", "FULL")
				.param("startDe", "2030-01-07")
				.param("endDe", "2030-01-08")
				.param("reason", "개인 사유"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.leaveId").isNotEmpty());

		assertThat(number(selectBalance("admin"), "usedDays")).isEqualTo(0.0);
		List<EgovMap> list = leaveMapper.selectMyLeaveList(myListParam("admin"));
		assertThat(list).anySatisfy(row -> {
			assertThat(row.get("leaveType")).isEqualTo("FULL");
			assertThat(row.get("status")).isEqualTo("REQUESTED");
			assertThat(number(row, "days")).isEqualTo(2.0);
		});
	}

	@Test
	void approveShouldDeductAndCancelShouldRestore() throws Exception {
		String leaveId = insertLeave("2030-01-07", "2030-01-08");

		mockMvc.perform(post("/leave/approveLeave.json")
				.session(TestSessions.admin("admin"))
				.param("leaveId", leaveId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(number(selectBalance("admin"), "usedDays")).isEqualTo(2.0);

		mockMvc.perform(post("/leave/cancelLeave.json")
				.session(TestSessions.admin("admin"))
				.param("leaveId", leaveId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(number(selectBalance("admin"), "usedDays")).isEqualTo(0.0);
	}

	@Test
	void rejectShouldKeepBalanceAndStoreReason() throws Exception {
		String leaveId = insertLeave("2030-01-14", "2030-01-15");

		mockMvc.perform(post("/leave/rejectLeave.json")
				.session(TestSessions.admin("admin"))
				.param("leaveId", leaveId)
				.param("rejectReason", "업무 일정으로 반려"))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap detail = leaveMapper.selectLeaveDetail(leaveId);
		assertThat(detail.get("status")).isEqualTo("REJECTED");
		assertThat(detail.get("rejectReason")).isEqualTo("업무 일정으로 반려");
		assertThat(number(selectBalance("admin"), "usedDays")).isEqualTo(0.0);
	}

	@Test
	void duplicatePeriodAndOverBalanceShouldBeRejected() throws Exception {
		insertLeave("2030-02-04", "2030-02-05");

		MockHttpSession duplicateSession = TestSessions.admin("admin");
		mockMvc.perform(post("/leave/insertLeave.json").session(duplicateSession)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(duplicateSession, "LEAVE"))
				.param("leaveType", "FULL")
				.param("startDe", "2030-02-05")
				.param("endDe", "2030-02-06")
				.param("reason", "겹치는 신청"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("같은 기간")));

		MockHttpSession overSession = TestSessions.admin("admin");
		mockMvc.perform(post("/leave/insertLeave.json").session(overSession)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(overSession, "LEAVE"))
				.param("leaveType", "FULL")
				.param("startDe", "2030-03-04")
				.param("endDe", "2030-03-14")
				.param("reason", "잔여 초과"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("잔여 연차가 부족")));
	}

	@Test
	void saveBalanceShouldRejectBelowUsedDays() throws Exception {
		saveBalance("2031", "3.0");
		String leaveId = insertLeave("2031-03-03", "2031-03-04", "2031");
		mockMvc.perform(post("/leave/approveLeave.json")
				.session(TestSessions.admin("admin"))
				.param("leaveId", leaveId))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/leave/saveBalance.json")
				.session(TestSessions.admin("admin"))
				.param("userId", "admin")
				.param("leaveYear", "2031")
				.param("totalDays", "1.0"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("사용한 연차보다 적게")));

		saveBalance("2031", "20.0");
		assertThat(number(selectBalance("admin", "2031"), "totalDays")).isEqualTo(20.0);
		assertThat(number(selectBalance("admin", "2031"), "usedDays")).isEqualTo(2.0);
	}

	private String insertLeave(String startDe, String endDe) throws Exception {
		return insertLeave(startDe, endDe, YEAR);
	}

	private String insertLeave(String startDe, String endDe, String year) throws Exception {
		if (!YEAR.equals(year)) {
			EgovMap param = balanceParam("admin");
			param.put("leaveYear", year);
			param.put("totalDays", new BigDecimal("10.0"));
			if (leaveMapper.countBalance(param) == 0) {
				leaveMapper.insertBalance(param);
			}
		}
		MockHttpSession session = TestSessions.admin("admin");
		MvcResult result = mockMvc.perform(post("/leave/insertLeave.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "LEAVE"))
				.param("leaveType", "FULL")
				.param("startDe", startDe)
				.param("endDe", endDe)
				.param("reason", "테스트 휴가"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.leaveId");
	}

	private void saveBalance(String year, String totalDays) throws Exception {
		mockMvc.perform(post("/leave/saveBalance.json")
				.session(TestSessions.admin("admin"))
				.param("userId", "admin")
				.param("leaveYear", year)
				.param("totalDays", totalDays))
				.andExpect(jsonPath("$.success").value(true));
	}

	private EgovMap selectBalance(String userId) throws Exception {
		return selectBalance(userId, YEAR);
	}

	private EgovMap selectBalance(String userId, String year) throws Exception {
		return leaveMapper.selectBalance(balanceParam(userId, year));
	}

	private EgovMap balanceParam(String userId) {
		return balanceParam(userId, YEAR);
	}

	private EgovMap balanceParam(String userId, String year) {
		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("leaveYear", year);
		return param;
	}

	private EgovMap myListParam(String userId) {
		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("leaveYear", YEAR);
		return param;
	}

	private double number(EgovMap map, String key) {
		Object value = map == null ? null : map.get(key);
		return value instanceof Number ? ((Number) value).doubleValue() : 0d;
	}
}
