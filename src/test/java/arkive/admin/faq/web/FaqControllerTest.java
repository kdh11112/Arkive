package arkive.admin.faq.web;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.comm.web.KoreanSearchUtil;
import arkive.admin.faq.service.FaqService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * FAQ가 BOARD_TYPE='FAQ'로 저장되고 이중등록·제목 검증·본문 정제·초성 갱신·삭제가 동작하는지 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class FaqControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FaqService faqService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertFaq(MockHttpSession session, String title, String content) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_FAQ");
		MvcResult result = mockMvc.perform(post("/faq/insertFaq.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", title)
						.param("content", content))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.boardId");
	}

	@Test
	@Rollback
	void faqShouldRoundTripWithChosungAndSanitizedContent() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String title = "자주 묻는 질문 테스트";
		String boardId = insertFaq(session, title, "<p>본문</p><script>alert('x')</script>");

		EgovMap detail = faqService.selectFaqDetail(boardId);
		assertThat(String.valueOf(detail.get("title"))).isEqualTo(title);
		assertThat(String.valueOf(detail.get("content"))).contains("<p>본문</p>").doesNotContain("<script>");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT BOARD_TYPE FROM BOARD WHERE BOARD_ID = ?", String.class, boardId)).isEqualTo("FAQ");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT SEARCH_CHOSUNG FROM BOARD WHERE BOARD_ID = ?", String.class, boardId))
				.isEqualTo(KoreanSearchUtil.toChosung(title));

		mockMvc.perform(post("/faq/updateFaq.json")
						.session(session)
						.param("boardId", boardId)
						.param("title", "수정된 FAQ 제목")
						.param("content", "<p>수정 본문</p>"))
				.andExpect(jsonPath("$.success").value(true));

		detail = faqService.selectFaqDetail(boardId);
		assertThat(String.valueOf(detail.get("title"))).isEqualTo("수정된 FAQ 제목");
		assertThat(String.valueOf(detail.get("content"))).contains("수정 본문");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT SEARCH_CHOSUNG FROM BOARD WHERE BOARD_ID = ?", String.class, boardId))
				.isEqualTo(KoreanSearchUtil.toChosung("수정된 FAQ 제목"));

		mockMvc.perform(get("/faq/faqList")
						.session(session)
						.param("searchKeyword", "수정된 FAQ 제목"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("수정된 FAQ 제목")));

		mockMvc.perform(post("/faq/deleteFaq.json")
						.session(session)
						.param("boardId", boardId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(faqService.selectFaqDetail(boardId)).isNull();
	}

	@Test
	@Rollback
	void insertShouldRejectDuplicateSubmitAndMissingTitle() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/faq/insertFaq.json")
						.session(session)
						.param("title", "토큰 없는 FAQ")
						.param("content", "본문"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_FAQ");
		mockMvc.perform(post("/faq/insertFaq.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("content", "본문"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("제목을 입력하세요."));

		String duplicateToken = EgovDoubleSubmitHelper.setToken(session, "BOARD_FAQ");
		mockMvc.perform(post("/faq/insertFaq.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, duplicateToken)
						.param("title", "첫 등록")
						.param("content", "본문"))
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(post("/faq/insertFaq.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, duplicateToken)
						.param("title", "두 번째 등록")
						.param("content", "본문"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));
	}
}
