package arkive.admin.popup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.popup.service.PopupService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 팝업의 게시기간 검증, 사용여부·게시기간 필터, 수정·삭제를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class PopupControllerTest {

	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PopupService popupService;

	private String ymd(int offsetDays) {
		return LocalDate.now().plusDays(offsetDays).format(YMD);
	}

	private String insertPopup(MockHttpSession session, String title, String startYmd, String endYmd,
			String useYn) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POPUP");
		MockHttpServletRequestBuilder builder = post("/popup/insertPopup.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("title", title)
				.param("content", "<p>팝업 내용</p>")
				.param("startYmd", startYmd)
				.param("endYmd", endYmd);
		if (useYn != null) {
			builder.param("useYn", useYn);
		}
		MvcResult result = mockMvc.perform(builder)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.popupId");
	}

	@Test
	@Rollback
	void insertShouldValidateDateRangeAndToken() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/popup/insertPopup.json")
						.session(session)
						.param("title", "토큰 없는 팝업")
						.param("startYmd", ymd(0))
						.param("endYmd", ymd(1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_POPUP");
		mockMvc.perform(post("/popup/insertPopup.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("title", "형식 오류 팝업")
						.param("startYmd", "20261301")
						.param("endYmd", ymd(1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("게시시작일")));

		String reversedToken = EgovDoubleSubmitHelper.setToken(session, "BOARD_POPUP");
		mockMvc.perform(post("/popup/insertPopup.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, reversedToken)
						.param("title", "기간 역전 팝업")
						.param("startYmd", ymd(1))
						.param("endYmd", ymd(0)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시시작일이 종료일보다 늦을 수 없습니다."));
	}

	@Test
	@Rollback
	void activePopupShouldFollowUseYnAndPublishPeriod() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String activeId = insertPopup(session, "팝업 활성 테스트", ymd(-1), ymd(1), null);
		String inactiveId = insertPopup(session, "팝업 비활성 테스트", ymd(-1), ymd(1), "N");
		String futureId = insertPopup(session, "팝업 예정 테스트", ymd(2), ymd(3), "Y");

		// activePopup.json은 비로그인 공개 경로다.
		MvcResult activeResult = mockMvc.perform(get("/popup/activePopup.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.list[*].popupId", hasItem(activeId)))
				.andExpect(jsonPath("$.list[*].popupId", not(hasItem(inactiveId))))
				.andExpect(jsonPath("$.list[*].popupId", not(hasItem(futureId))))
				.andReturn();
		// CLOB은 문자열로 변환되어야 한다. JDBC 객체가 그대로 직렬화되면 응답이 깨진다.
		List<String> contents = JsonPath.read(activeResult.getResponse().getContentAsString(),
				"$.list[?(@.popupId == '" + activeId + "')].content");
		assertThat(contents).containsExactly("<p>팝업 내용</p>");

		mockMvc.perform(post("/popup/updatePopup.json")
						.session(session)
						.param("popupId", activeId)
						.param("title", "팝업 활성 테스트")
						.param("content", "<p>수정 내용</p>")
						.param("startYmd", ymd(-1))
						.param("endYmd", ymd(1))
						.param("useYn", "N"))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/popup/activePopup.json"))
				.andExpect(jsonPath("$.list[*].popupId", not(hasItem(activeId))));

		EgovMap detail = popupService.selectPopupDetail(activeId);
		assertThat(String.valueOf(detail.get("content"))).contains("수정 내용");

		mockMvc.perform(post("/popup/deletePopup.json")
						.session(session)
						.param("popupId", activeId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(popupService.selectPopupDetail(activeId)).isNull();
	}
}
