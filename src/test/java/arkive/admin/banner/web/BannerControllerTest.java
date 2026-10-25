package arkive.admin.banner.web;

import static org.assertj.core.api.Assertions.assertThat;
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

import arkive.admin.banner.service.BannerService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 배너의 위치 정규화, 순서 파싱, 위치·게시기간·사용여부별 노출 필터를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class BannerControllerTest {

	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BannerService bannerService;

	private String ymd(int offsetDays) {
		return LocalDate.now().plusDays(offsetDays).format(YMD);
	}

	private String insertBanner(MockHttpSession session, String title, String position, String startYmd,
			String endYmd, String useYn, String ordr) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_BANNER");
		MockHttpServletRequestBuilder builder = post("/banner/insertBanner.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("title", title)
				.param("linkUrl", "https://example.com/" + title)
				.param("startYmd", startYmd)
				.param("endYmd", endYmd)
				.param("ordr", ordr);
		if (position != null) {
			builder.param("position", position);
		}
		if (useYn != null) {
			builder.param("useYn", useYn);
		}
		MvcResult result = mockMvc.perform(builder)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.bannerId");
	}

	@Test
	@Rollback
	void insertShouldNormalizePositionAndOrder() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/banner/insertBanner.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
								EgovDoubleSubmitHelper.setToken(session, "BOARD_BANNER"))
						.param("title", "형식 오류 배너")
						.param("startYmd", "2026-1-1")
						.param("endYmd", ymd(1)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시시작일을 YYYYMMDD 형식으로 입력하세요."));

		mockMvc.perform(post("/banner/insertBanner.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
								EgovDoubleSubmitHelper.setToken(session, "BOARD_BANNER"))
						.param("title", "기간 역전 배너")
						.param("startYmd", ymd(1))
						.param("endYmd", ymd(0)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("게시시작일이 종료일보다 늦을 수 없습니다."));

		String mainId = insertBanner(session, "메인 배너 테스트", "MAIN", ymd(-1), ymd(1), null, "2");
		EgovMap main = bannerService.selectBannerDetail(mainId);
		assertThat(String.valueOf(main.get("position"))).isEqualTo("MAIN");
		assertThat(String.valueOf(main.get("ordr"))).isEqualTo("2");

		// MAIN 외 위치는 BANNER로 모으고, 숫자가 아닌 순서는 0으로 처리한다.
		String unknownId = insertBanner(session, "위치 보정 배너", "SIDEBAR", ymd(-1), ymd(1), null, "abc");
		EgovMap unknown = bannerService.selectBannerDetail(unknownId);
		assertThat(String.valueOf(unknown.get("position"))).isEqualTo("BANNER");
		assertThat(String.valueOf(unknown.get("ordr"))).isEqualTo("0");
	}

	@Test
	@Rollback
	void activeBannerShouldFilterByPositionUseYnAndPeriod() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String mainActiveId = insertBanner(session, "메인 활성 테스트", "MAIN", ymd(-1), ymd(1), null, "1");
		String mainInactiveId = insertBanner(session, "메인 비활성 테스트", "MAIN", ymd(-1), ymd(1), "N", "1");
		String bannerActiveId = insertBanner(session, "배너 활성 테스트", "BANNER", ymd(-1), ymd(1), null, "1");
		String bannerSecondId = insertBanner(session, "배너 활성 둘째", "BANNER", ymd(-1), ymd(1), null, "5");
		String bannerExpiredId = insertBanner(session, "배너 종료 테스트", "BANNER", ymd(-5), ymd(-1), null, "1");

		mockMvc.perform(get("/banner/activeBanner.json").param("position", "MAIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.list[*].bannerId", hasItem(mainActiveId)))
				.andExpect(jsonPath("$.list[*].bannerId", not(hasItem(mainInactiveId))))
				.andExpect(jsonPath("$.list[*].bannerId", not(hasItem(bannerActiveId))));

		MvcResult defaultResult = mockMvc.perform(get("/banner/activeBanner.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.list[*].bannerId", hasItem(bannerActiveId)))
				.andExpect(jsonPath("$.list[*].bannerId", hasItem(bannerSecondId)))
				.andExpect(jsonPath("$.list[*].bannerId", not(hasItem(bannerExpiredId))))
				.andExpect(jsonPath("$.list[*].bannerId", not(hasItem(mainActiveId))))
				.andReturn();
		List<String> bannerIds = JsonPath.read(defaultResult.getResponse().getContentAsString(),
				"$.list[*].bannerId");
		assertThat(bannerIds.indexOf(bannerActiveId)).isLessThan(bannerIds.indexOf(bannerSecondId));

		mockMvc.perform(post("/banner/updateBanner.json")
						.session(session)
						.param("bannerId", bannerActiveId)
						.param("title", "배너 활성 테스트")
						.param("position", "BANNER")
						.param("startYmd", ymd(-1))
						.param("endYmd", ymd(1))
						.param("useYn", "N")
						.param("ordr", "1"))
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(get("/banner/activeBanner.json"))
				.andExpect(jsonPath("$.list[*].bannerId", not(hasItem(bannerActiveId))));

		mockMvc.perform(post("/banner/deleteBanner.json")
						.session(session)
						.param("bannerId", bannerSecondId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(bannerService.selectBannerDetail(bannerSecondId)).isNull();
	}
}
