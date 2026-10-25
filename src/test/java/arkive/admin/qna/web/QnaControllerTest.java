package arkive.admin.qna.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.qna.service.QnaService;
import arkive.admin.qna.service.impl.QnaMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * Q&A 답변 권한 분리와 관리자 답변 저장을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class QnaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QnaMapper qnaMapper;

	@Autowired
	private QnaService qnaService;

	private void insert(String boardId) throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", boardId);
		param.put("title", "질문");
		param.put("content", "본문");
		param.put("register", "작성자");
		param.put("askType", "PUBLIC");
		qnaMapper.insertQna(param);
	}

	@Test
	@Rollback
	void adminDetailShouldExposeAnswerFormFlag() throws Exception {
		insert("BDQNA00000000000201");

		mockMvc.perform(get("/qna/qnaDetail").param("boardId", "BDQNA00000000000201")
						.session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("canAnswer", true));
	}

	@Test
	@Rollback
	void nonAdminShouldBeDeniedAnswerEndpoint() throws Exception {
		insert("BDQNA00000000000202");

		mockMvc.perform(post("/qna/saveAnswer.json")
						.session(TestSessions.user("tester"))
						.param("boardId", "BDQNA00000000000202")
						.param("answerContent", "답변"))
				.andExpect(status().isForbidden());
	}

	@Test
	@Rollback
	void adminWithTokenShouldSaveAnswer() throws Exception {
		insert("BDQNA00000000000203");
		MockHttpSession session = TestSessions.admin("admin");
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_QNA_ANS");

		mockMvc.perform(post("/qna/saveAnswer.json")
						.session(session)
						.param("boardId", "BDQNA00000000000203")
						.param("answerContent", "관리자 답변")
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token))
				.andExpect(status().isOk());

		assertThat(qnaService.selectQnaAnswer("BDQNA00000000000203")).isNotNull();
		assertThat(String.valueOf(qnaService.selectQnaAnswer("BDQNA00000000000203").get("answerContent")))
				.isEqualTo("관리자 답변");
	}
}
