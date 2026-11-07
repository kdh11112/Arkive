package arkive.admin.board.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 글쓰기 화면의 임시저장 동작을 화면 산출물로 고정한다.
 * 편집이 없어도 10초마다 저장하던 타이머와 로드 시 confirm 프롬프트를 없애고
 * 배너(불러오기/무시)로 바꾼 상태를 확인한다.
 */
@ArkiveIntegrationTest
class BoardWriteDraftTemplateTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void ckWriteShouldUseDraftBannerInsteadOfTimerAndConfirm() throws Exception {
		mockMvc.perform(get("/board/ckeditorWrite").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"draftNotice\"")))
				.andExpect(content().string(containsString("id=\"draftLoad\"")))
				.andExpect(content().string(containsString("id=\"draftDiscard\"")))
				.andExpect(content().string(containsString("scheduleSaveDraft")))
				// 주기적 자동 저장과 로드 시 confirm은 제거됐다
				.andExpect(content().string(not(containsString("setInterval(saveDraft"))))
				.andExpect(content().string(not(containsString("confirm('작성 중 임시저장본"))));
	}

	@Test
	void toastWriteShouldUseDraftBannerInsteadOfTimerAndConfirm() throws Exception {
		mockMvc.perform(get("/board/toastWrite").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"draftNotice\"")))
				.andExpect(content().string(containsString("id=\"draftLoad\"")))
				.andExpect(content().string(containsString("id=\"draftDiscard\"")))
				.andExpect(content().string(containsString("scheduleSaveDraft")))
				.andExpect(content().string(not(containsString("setInterval(saveDraft"))))
				.andExpect(content().string(not(containsString("confirm('작성 중 임시저장본"))));
	}
}
