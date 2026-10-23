package arkive.admin.comm.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 공통 오류 응답과 헬스 체크 설정을 확인한다.
 *
 * <p>미존재 URL은 Throwable → 500 매핑에 덮이지 않고 404로 응답해야 한다.
 * 메일 발송이 꺼진 기본 설정에서는 메일 헬스 인디케이터도 꺼져 전체 health가 UP이어야 한다.</p>
 */
@ArkiveIntegrationTest
class ErrorResponseTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unmappedPageShouldReturn404() throws Exception {
		mockMvc.perform(get("/asdf").session(TestSessions.admin("admin")))
				.andExpect(status().isNotFound())
				.andExpect(view().name("error/error"));
	}

	@Test
	void missingStaticResourceShouldReturn404() throws Exception {
		mockMvc.perform(get("/css/nope.css"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("error/error"));
	}

	@Test
	void healthShouldBeUpWhenMailIsDisabled() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.mail").doesNotExist());
	}
}
