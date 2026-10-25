package arkive.admin.counsel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 상담 비밀번호 확인·수정/삭제 권한·답변 상태·첨부 3개 제한을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class CounselControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertCounsel(MockHttpSession session, String title, String writer, String password,
			String content) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_COUNSEL");
		MvcResult result = mockMvc.perform(post("/counsel/insertCounsel.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("writer", writer)
						.param("password", password)
						.param("content", content))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.counselId");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestAnswerList(MockHttpSession session, String writer, String status)
			throws Exception {
		MvcResult result = mockMvc.perform(get("/counsel/answerList")
						.session(session)
						.param("searchKeyword", writer)
						.param("status", status))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	@Test
	@Rollback
	void counselShouldCheckPasswordAndManageAnswers() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String suffix = String.valueOf(System.nanoTime());
		String writer = "ZZ상담작성자-" + suffix;
		String password = "pw1234";
		String counselId = insertCounsel(session, "상담 제목", writer, password,
				"<p>상담 내용</p><script>alert('x')</script>");

		String storedPassword = jdbcTemplate.queryForObject(
				"SELECT PASSWORD FROM COUNSEL WHERE COUNSEL_ID = ?", String.class, counselId);
		assertThat(storedPassword).matches("[0-9a-f]{64}").isNotEqualTo(password);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT STATUS FROM COUNSEL WHERE COUNSEL_ID = ?", String.class, counselId))
				.isEqualTo("접수대기");

		MvcResult detailResult = mockMvc.perform(get("/counsel/counselDetail")
						.session(session)
						.param("counselId", counselId))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("content"))).contains("<p>상담 내용</p>")
				.doesNotContain("<script>");

		mockMvc.perform(post("/counsel/checkPassword.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", "wrong"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호가 틀립니다."));
		mockMvc.perform(post("/counsel/checkPassword.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", password))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/counsel/updateCounsel.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", "wrong")
						.param("title", "수정 시도"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호가 틀립니다."));
		mockMvc.perform(post("/counsel/updateCounsel.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", password)
						.param("title", "수정된 상담 제목")
						.param("content", "<p>수정 내용</p>"))
				.andExpect(jsonPath("$.success").value(true));
		detailResult = mockMvc.perform(get("/counsel/counselDetail")
						.session(session)
						.param("counselId", counselId))
				.andReturn();
		detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("title"))).isEqualTo("수정된 상담 제목");
		assertThat(String.valueOf(detail.get("content"))).contains("수정 내용");

		mockMvc.perform(post("/counsel/updateCounselAnswer.json")
						.session(session)
						.param("counselId", counselId)
						.param("answer", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("답변을 입력하세요."));
		mockMvc.perform(post("/counsel/updateCounselAnswer.json")
						.session(session)
						.param("counselId", counselId)
						.param("answer", "답변 내용")
						.param("status", "이상한상태"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("진행상태는 접수대기·접수·완료 중 하나여야 합니다."));
		mockMvc.perform(post("/counsel/updateCounselAnswer.json")
						.session(session)
						.param("counselId", counselId)
						.param("answer", "<p>답변 내용</p>"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT STATUS FROM COUNSEL WHERE COUNSEL_ID = ?", String.class, counselId)).isEqualTo("완료");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT ANSWER_DT FROM COUNSEL WHERE COUNSEL_ID = ?", Object.class, counselId)).isNotNull();
		assertThat(requestAnswerList(session, writer, "완료")).hasSize(1);
		assertThat(requestAnswerList(session, writer, "접수")).isEmpty();

		mockMvc.perform(post("/counsel/updateCounselAnswer.json")
						.session(session)
						.param("counselId", counselId)
						.param("answer", "접수 답변")
						.param("status", "접수"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT STATUS FROM COUNSEL WHERE COUNSEL_ID = ?", String.class, counselId)).isEqualTo("접수");

		mockMvc.perform(post("/counsel/deleteCounsel.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", "wrong"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호가 틀립니다."));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM COUNSEL WHERE COUNSEL_ID = ?", Integer.class, counselId)).isEqualTo(1);
		mockMvc.perform(post("/counsel/deleteCounsel.json")
						.session(session)
						.param("counselId", counselId)
						.param("password", password))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM COUNSEL WHERE COUNSEL_ID = ?", Integer.class, counselId)).isZero();
	}

	@Test
	@Rollback
	void insertAndUploadShouldValidateRequiredValues() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/counsel/insertCounsel.json")
						.session(session)
						.param("title", "토큰 없는 상담")
						.param("writer", "작성자")
						.param("password", "pw"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_COUNSEL");
		mockMvc.perform(post("/counsel/insertCounsel.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("writer", "작성자")
						.param("password", "pw"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_COUNSEL");
		mockMvc.perform(post("/counsel/insertCounsel.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "작성자 없는 상담")
						.param("password", "pw"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("작성자를 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_COUNSEL");
		mockMvc.perform(post("/counsel/insertCounsel.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "비밀번호 없는 상담")
						.param("writer", "작성자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("비밀번호를 입력하세요."));

		mockMvc.perform(multipart("/counsel/counselFileUpload.json")
						.session(session))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("업로드할 파일이 없습니다."));

		MockMultipartFile f1 = new MockMultipartFile("file", "a.txt", "text/plain", "a".getBytes());
		MockMultipartFile f2 = new MockMultipartFile("file", "b.txt", "text/plain", "b".getBytes());
		MockMultipartFile f3 = new MockMultipartFile("file", "c.txt", "text/plain", "c".getBytes());
		MockMultipartFile f4 = new MockMultipartFile("file", "d.txt", "text/plain", "d".getBytes());
		mockMvc.perform(multipart("/counsel/counselFileUpload.json")
						.file(f1).file(f2).file(f3).file(f4)
						.session(session))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("첨부파일은 최대 3개까지 등록할 수 있습니다."));
	}
}
