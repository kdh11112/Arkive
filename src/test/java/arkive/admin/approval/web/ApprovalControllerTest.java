package arkive.admin.approval.web;

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
import org.springframework.test.web.servlet.MvcResult;

import arkive.admin.approval.service.impl.ApprovalMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 약식결재의 상태 전이, 결재자 지정, 기안자 본인 제한을 확인한다.
 * 결재자 검증은 실제 APP_USER에 존재하는 admin 계정을 사용한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class ApprovalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApprovalMapper approvalMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Test
	void approvalScreensShouldFollowMenuPermission() throws Exception {
		mockMvc.perform(get("/approval/approvalList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("약식결재")));

		mockMvc.perform(get("/approval/approvalList").session(userSession("approval-user")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("기안 작성")));

		mockMvc.perform(get("/approval/approvalList").session(TestSessions.user("no-role-user")))
				.andExpect(status().isForbidden());
	}

	@Test
	void draftShouldBeSubmittedAndApprovedWithHistory() throws Exception {
		MockHttpSession drafter = userSession("approval-drafter");
		String docId = insertDraft(drafter, "출장 신청", "admin");

		mockMvc.perform(post("/approval/submitApproval.json").session(drafter).param("docId", docId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(approvalMapper.selectDocDetail(docId).get("status")).isEqualTo("SUBMITTED");

		mockMvc.perform(get("/approval/approvalView").session(drafter).param("docId", docId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("출장 신청")));

		mockMvc.perform(post("/approval/approveApproval.json").session(TestSessions.admin("admin"))
				.param("docId", docId))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap detail = approvalMapper.selectDocDetail(docId);
		assertThat(detail.get("status")).isEqualTo("APPROVED");
		assertThat(detail.get("approvedDt")).isNotNull();
		assertThat(approvalMapper.selectHistory(docId))
				.extracting(row -> row.get("action"))
				.containsExactly("CREATE", "SUBMIT", "APPROVE");
	}

	@Test
	void onlyDesignatedApproverShouldApproveOrReject() throws Exception {
		MockHttpSession drafter = userSession("approval-drafter2");
		String docId = insertDraft(drafter, "비품 구입", "admin");
		mockMvc.perform(post("/approval/submitApproval.json").session(drafter).param("docId", docId))
				.andExpect(jsonPath("$.success").value(true));

		MockHttpSession other = userSession("approval-other");
		mockMvc.perform(post("/approval/approveApproval.json").session(other).param("docId", docId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("지정된 결재자")));

		mockMvc.perform(post("/approval/rejectApproval.json").session(TestSessions.admin("admin"))
				.param("docId", docId)
				.param("rejectReason", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("반려 사유")));

		mockMvc.perform(post("/approval/rejectApproval.json").session(TestSessions.admin("admin"))
				.param("docId", docId)
				.param("rejectReason", "예산 초과"))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap detail = approvalMapper.selectDocDetail(docId);
		assertThat(detail.get("status")).isEqualTo("REJECTED");
		assertThat(detail.get("rejectReason")).isEqualTo("예산 초과");
	}

	@Test
	void onlyDrafterShouldEditDeleteSubmitOrCancel() throws Exception {
		MockHttpSession drafter = userSession("approval-drafter3");
		String docId = insertDraft(drafter, "연차 신청", "admin");
		MockHttpSession other = userSession("approval-other2");

		mockMvc.perform(post("/approval/updateDraft.json").session(other)
				.param("docId", docId)
				.param("docType", "GENERAL")
				.param("title", "가로채기")
				.param("content", "")
				.param("approverId", "admin"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("기안자만")));
		mockMvc.perform(post("/approval/submitApproval.json").session(other).param("docId", docId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("기안자만")));
		mockMvc.perform(post("/approval/deleteDraft.json").session(other).param("docId", docId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("기안자만")));

		mockMvc.perform(post("/approval/updateDraft.json").session(drafter)
				.param("docId", docId)
				.param("docType", "LEAVE")
				.param("title", "연차 신청(수정)")
				.param("content", "5월 2일")
				.param("approverId", "admin"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(approvalMapper.selectDocDetail(docId).get("title")).isEqualTo("연차 신청(수정)");

		mockMvc.perform(post("/approval/submitApproval.json").session(drafter).param("docId", docId))
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(post("/approval/updateDraft.json").session(drafter)
				.param("docId", docId)
				.param("docType", "LEAVE")
				.param("title", "상신 후 수정")
				.param("content", "")
				.param("approverId", "admin"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("임시저장 문서만")));

		mockMvc.perform(post("/approval/cancelApproval.json").session(drafter).param("docId", docId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(approvalMapper.selectDocDetail(docId).get("status")).isEqualTo("CANCELLED");
	}

	@Test
	void nonParticipantShouldNotViewDetailOrUseAllScope() throws Exception {
		MockHttpSession drafter = userSession("approval-drafter4");
		String docId = insertDraft(drafter, "비공개 기안", "admin");

		MockHttpSession outsider = userSession("approval-outsider");
		mockMvc.perform(get("/approval/approvalView").session(outsider).param("docId", docId))
				.andExpect(status().isForbidden());

		MvcResult result = mockMvc.perform(get("/approval/approvalList").session(outsider)
				.param("scope", "all"))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(result.getModelAndView().getModel().get("scope")).isEqualTo("my");
	}

	private MockHttpSession userSession(String userId) throws Exception {
		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", userId);
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		return TestSessions.user(userId, userId + "-name");
	}

	private String insertDraft(MockHttpSession session, String title, String approverId) throws Exception {
		MvcResult result = mockMvc.perform(post("/approval/insertDraft.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "APPROVAL"))
				.param("docType", "GENERAL")
				.param("title", title)
				.param("content", "테스트 내용")
				.param("approverId", approverId))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		String body = result.getResponse().getContentAsString();
		String marker = "\"docId\":\"";
		int start = body.indexOf(marker);
		return body.substring(start + marker.length(), body.indexOf('"', start + marker.length()));
	}
}
