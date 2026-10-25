package arkive.com.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * SpringDoc이 실제 컨트롤러 경로를 API 문서로 노출하고 Swagger UI를 제공하는지 확인한다.
 * 문서 자체의 내용 정확성(스키마 설명)은 화면 확인 영역으로 남긴다.
 */
@ArkiveIntegrationTest
class OpenApiDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsShouldExposeRepresentativePaths() throws Exception {
		mockMvc.perform(get("/v3/api-docs").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Arkive API"))
				.andExpect(jsonPath("$.info.version").value("1.0.0"))
				.andExpect(jsonPath("$.paths['/board/toastList.json']").exists())
				.andExpect(jsonPath("$.paths['/board/insertBoard.json']").exists())
				.andExpect(jsonPath("$.paths['/file/downloadFile.do']").exists())
				.andExpect(jsonPath("$.paths['/system/file/saveTempFiles.json']").exists());
	}

	@Test
	void swaggerUiShouldBeServed() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}
}
