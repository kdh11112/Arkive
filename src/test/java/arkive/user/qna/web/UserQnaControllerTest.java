package arkive.user.qna.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import arkive.admin.qna.service.QnaService;
import arkive.admin.qna.service.impl.QnaMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 외부 Q&A가 비로그인에게 INTERNAL 글을 숨기고, 내부 상세는 로그인으로 유도하는지 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class UserQnaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QnaMapper qnaMapper;

	@Autowired
	private QnaService qnaService;

	private void insert(String boardId, String title, String askType) throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", boardId);
		param.put("title", title);
		param.put("content", "본문");
		param.put("register", "작성자");
		param.put("askType", askType);
		qnaMapper.insertQna(param);
	}

	@Test
	@Rollback
	void anonymousListShouldHideInternalQuestions() throws Exception {
		insert("BDQNA00000000000101", "공개 질문", "PUBLIC");
		insert("BDQNA00000000000102", "내부 비밀 질문", "INTERNAL");

		mockMvc.perform(get("/user/qna/qnaList"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("공개 질문")))
				.andExpect(content().string(not(containsString("내부 비밀 질문"))));
	}

	@Test
	@Rollback
	void loggedInListShouldShowInternalQuestions() throws Exception {
		insert("BDQNA00000000000103", "내부 질문2", "INTERNAL");

		mockMvc.perform(get("/user/qna/qnaList").session(TestSessions.user("tester")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("내부 질문2")));
	}

	@Test
	@Rollback
	void anonymousInternalDetailShouldRedirectToLogin() throws Exception {
		insert("BDQNA00000000000104", "내부 상세", "INTERNAL");

		mockMvc.perform(get("/user/qna/qnaDetail").param("boardId", "BDQNA00000000000104"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", startsWith("/login/login?returnUrl=")));
	}

	@Test
	@Rollback
	void loggedInInternalDetailShouldRender() throws Exception {
		insert("BDQNA00000000000105", "내부 상세2", "INTERNAL");

		mockMvc.perform(get("/user/qna/qnaDetail").param("boardId", "BDQNA00000000000105")
						.session(TestSessions.user("tester")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("내부 상세2")));
	}

	@Test
	@Rollback
	void anonymousInsertShouldStorePublicSanitizedQuestionAndRejectDuplicateToken() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String token = EgovDoubleSubmitHelper.setToken(session, "USER_QNA");

		MvcResult result = mockMvc.perform(post("/user/qna/insertQna.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "외부 질문")
						.param("content", "<p>질문 본문</p><script>alert(1)</script>"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		String boardId = JsonPath.read(result.getResponse().getContentAsString(), "$.boardId");

		EgovMap detail = qnaService.selectQnaDetail(boardId);
		assertThat(String.valueOf(detail.get("askType"))).isEqualTo("PUBLIC");
		assertThat(String.valueOf(detail.get("register"))).isEqualTo("익명");
		assertThat(String.valueOf(detail.get("content")))
				.contains("<p>질문 본문</p>")
				.doesNotContain("<script>");

		mockMvc.perform(post("/user/qna/insertQna.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "중복 질문")
						.param("content", "<p>dup</p>"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message", containsString("중복")));
	}

	@Test
	void writeScreenShouldIssueDoubleSubmitToken() throws Exception {
		MvcResult result = mockMvc.perform(get("/user/qna/qnaWrite"))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getModelAndView().getModel().get("doubleSubmitToken")).isNotNull();
	}
}
