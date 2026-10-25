package arkive.admin.map.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import arkive.admin.map.service.impl.MapGuideMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 약도 등록·수정·삭제, 좌표 검증, 사용여부 필터를 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class MapControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MapGuideMapper mapGuideMapper;

	@Test
	void mapGuideShouldRoundTripAndFilterByUseYn() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(get("/map/mapWrite").session(session))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("doubleSubmitToken"))
				.andExpect(model().attribute("mapKeyConfigured", false));

		String mapId = insert(session, "본사 약도", "서울시 중구 세종대로 1", "37.5665", "126.9780", "1층 로비",
				"Y");

		EgovMap detail = mapGuideMapper.selectMapGuideDetail(mapId);
		assertThat(detail.get("title")).isEqualTo("본사 약도");
		assertThat(detail.get("useYn")).isEqualTo("Y");

		mockMvc.perform(get("/map/mapView").session(session).param("mapId", mapId))
				.andExpect(status().isOk())
				.andExpect(model().attribute("detail", hasEntry("title", "본사 약도")));
		@SuppressWarnings("unchecked")
		List<EgovMap> listed = (List<EgovMap>) mockMvc.perform(get("/map/mapList").session(session)
				.param("searchKeyword", "본사 약도"))
				.andExpect(status().isOk())
				.andReturn().getModelAndView().getModel().get("resultList");
		assertThat(listed).anyMatch(row -> mapId.equals(String.valueOf(row.get("mapId"))));

		mockMvc.perform(post("/map/updateMapGuide.json").session(session)
				.param("mapId", mapId)
				.param("title", "지사 약도")
				.param("address", "부산시 연제구 중앙대로 1001")
				.param("latitude", "35.1796")
				.param("longitude", "129.0756")
				.param("description", "2층")
				.param("useYn", "N"))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap updated = mapGuideMapper.selectMapGuideDetail(mapId);
		assertThat(updated.get("title")).isEqualTo("지사 약도");
		assertThat(String.valueOf(updated.get("useYn"))).isEqualTo("N");

		EgovMap param = new EgovMap();
		param.put("useYn", "Y");
		param.put("searchKeyword", "");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 10);
		List<EgovMap> enabled = mapGuideMapper.selectMapGuideList(param);
		assertThat(enabled).noneMatch(row -> mapId.equals(String.valueOf(row.get("mapId"))));

		mockMvc.perform(post("/map/deleteMapGuide.json").session(session).param("mapId", mapId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(mapGuideMapper.selectMapGuideDetail(mapId)).isNull();
		mockMvc.perform(get("/map/mapView").session(session).param("mapId", mapId))
				.andExpect(redirectedUrl("/map/mapList"));
	}

	@Test
	void mapGuideShouldValidateTokenTitleCoordinatesAndUseYn() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/map/insertMapGuide.json").session(session)
				.param("title", "토큰없음")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		expectInsertFailure(session, "", "", "", "", "", "Y", "약도명은 1~200자로 입력하세요.");
		expectInsertFailure(session, "가".repeat(201), "", "", "", "", "Y", "약도명은 1~200자로 입력하세요.");
		expectInsertFailure(session, "약도", "가".repeat(301), "", "", "", "Y", "주소는 300자 이내로 입력하세요.");
		expectInsertFailure(session, "약도", "", "", "", "가".repeat(2001), "Y", "설명은 2000자 이내로 입력하세요.");
		expectInsertFailure(session, "약도", "", "", "", "", "X", "사용여부가 올바르지 않습니다.");
		expectInsertFailure(session, "약도", "", "37.5", "", "", "Y", "위도와 경도는 함께 입력하세요.");
		expectInsertFailure(session, "약도", "", "abc", "127.0", "", "Y", "위도는 숫자로 입력하세요.");
		expectInsertFailure(session, "약도", "", "91", "127.0", "", "Y", "위도는 -90~90 사이로 입력하세요.");
		expectInsertFailure(session, "약도", "", "37.5", "181", "", "Y", "경도는 -180~180 사이로 입력하세요.");

		MockHttpSession other = TestSessions.admin("admin");
		mockMvc.perform(post("/map/updateMapGuide.json").session(other)
				.param("mapId", "NOPE")
				.param("title", "없는 약도")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("약도가 없습니다."));
		mockMvc.perform(post("/map/deleteMapGuide.json").session(other).param("mapId", "NOPE"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("약도가 없습니다."));
	}

	private void expectInsertFailure(MockHttpSession session, String title, String address, String latitude,
			String longitude, String description, String useYn, String message) throws Exception {
		mockMvc.perform(post("/map/insertMapGuide.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MAP_GUIDE"))
				.param("title", title)
				.param("address", address)
				.param("latitude", latitude)
				.param("longitude", longitude)
				.param("description", description)
				.param("useYn", useYn))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(message));
	}

	private String insert(MockHttpSession session, String title, String address, String latitude, String longitude,
			String description, String useYn) throws Exception {
		String body = mockMvc.perform(post("/map/insertMapGuide.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MAP_GUIDE"))
				.param("title", title)
				.param("address", address)
				.param("latitude", latitude)
				.param("longitude", longitude)
				.param("description", description)
				.param("useYn", useYn))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn().getResponse().getContentAsString();
		return com.jayway.jsonpath.JsonPath.read(body, "$.id");
	}
}
