package arkive.admin.memo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.memo.service.MemoService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 메모보고의 등록 검증, 보고→확인→의견 상태 전이와 상태별 수정 제한을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class MemoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemoService memoService;

	private String insertMemo(MockHttpSession session, String title, String content, String reporter,
			String receiver) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_MEMO");
		MvcResult result = mockMvc.perform(post("/memo/insertMemo.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("content", content)
						.param("reporter", reporter)
						.param("receiver", receiver))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.reportId");
	}

	@Test
	@Rollback
	void statusTransitionsShouldRestrictUpdate() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String reportId = insertMemo(session, "메모 상태 전이 테스트",
				"<p>내용</p><script>bad()</script>", "김보고", "이대상");

		EgovMap detail = memoService.selectMemoDetail(reportId);
		assertThat(String.valueOf(detail.get("status"))).isEqualTo("보고");
		assertThat(String.valueOf(detail.get("content"))).contains("<p>내용</p>").doesNotContain("<script>");

		mockMvc.perform(post("/memo/updateMemo.json")
						.session(session)
						.param("reportId", reportId)
						.param("title", "수정 메모")
						.param("content", "수정 내용"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(String.valueOf(memoService.selectMemoDetail(reportId).get("title"))).isEqualTo("수정 메모");

		mockMvc.perform(post("/memo/confirmMemo.json")
						.session(session)
						.param("reportId", reportId))
				.andExpect(jsonPath("$.success").value(true));
		detail = memoService.selectMemoDetail(reportId);
		assertThat(String.valueOf(detail.get("status"))).isEqualTo("확인");
		assertThat(detail.get("confirmDt")).isNotNull();

		mockMvc.perform(post("/memo/updateMemo.json")
						.session(session)
						.param("reportId", reportId)
						.param("title", "확인 후 수정")
						.param("content", "수정 내용"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("확인·의견 이후에는 수정할 수 없습니다."));

		mockMvc.perform(post("/memo/opinionMemo.json")
						.session(session)
						.param("reportId", reportId)
						.param("opinion", "   "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("의견을 입력하세요."));

		mockMvc.perform(post("/memo/opinionMemo.json")
						.session(session)
						.param("reportId", reportId)
						.param("opinion", "<p>검토 의견</p>"))
				.andExpect(jsonPath("$.success").value(true));
		detail = memoService.selectMemoDetail(reportId);
		assertThat(String.valueOf(detail.get("status"))).isEqualTo("의견");
		assertThat(String.valueOf(detail.get("opinion"))).contains("검토 의견");

		// 의견 상태에서도 대상자 확인으로 다시 전이할 수 있다.
		mockMvc.perform(post("/memo/confirmMemo.json")
						.session(session)
						.param("reportId", reportId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(String.valueOf(memoService.selectMemoDetail(reportId).get("status"))).isEqualTo("확인");

		mockMvc.perform(post("/memo/deleteMemo.json")
						.session(session)
						.param("reportId", reportId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(memoService.selectMemoDetail(reportId)).isNull();

		EgovMap param = new EgovMap();
		param.put("searchKeyword", "수정 메모");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 10);
		assertThat(memoService.selectMemoList(param)).isEmpty();
	}

	@Test
	@Rollback
	void insertShouldRequireTokenAndMandatoryFields() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/memo/insertMemo.json")
						.session(session)
						.param("title", "토큰 없는 메모")
						.param("reporter", "김보고")
						.param("receiver", "이대상"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_MEMO");
		mockMvc.perform(post("/memo/insertMemo.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("reporter", "김보고")
						.param("receiver", "이대상"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));

		String missingReceiverToken = EgovDoubleSubmitHelper.setToken(session, "BOARD_MEMO");
		mockMvc.perform(post("/memo/insertMemo.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, missingReceiverToken)
						.param("title", "대상자 누락")
						.param("reporter", "김보고"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("보고자와 대상자를 입력하세요."));
	}
}
