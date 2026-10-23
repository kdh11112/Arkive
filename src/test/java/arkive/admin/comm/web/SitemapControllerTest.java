package arkive.admin.comm.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 사이트맵이 로그인 사용자에게만 열리고, MenuInterceptor가 만든 권한 필터 메뉴를
 * 그대로 사용하는지 확인한다.
 */
@ArkiveIntegrationTest
class SitemapControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sitemapShouldRequireLogin() throws Exception {
		mockMvc.perform(get("/sitemap"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login/login"));
	}

	@Test
	void adminSitemapShouldRenderMenuTree() throws Exception {
		mockMvc.perform(get("/sitemap").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(view().name("comm/sitemap"))
				.andExpect(model().attributeExists("menuList"))
				.andExpect(content().string(containsString("사이트맵")))
				.andExpect(content().string(containsString("공지사항관리")));
	}

	@Test
	void userWithoutRolesShouldSeeEmptySitemap() throws Exception {
		mockMvc.perform(get("/sitemap").session(TestSessions.user("no-role-user")))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("menuList"))
				.andExpect(content().string(containsString("표시할 메뉴가 없습니다.")));
	}
}
