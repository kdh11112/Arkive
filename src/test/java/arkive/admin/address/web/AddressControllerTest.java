package arkive.admin.address.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 주소록 CRUD·이름/부서 검색·연락처 암호화·목록 마스킹을 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class AddressControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertAddress(MockHttpSession session, String addrNm, String deptNm, String telNo,
			String email) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_ADDRESS");
		MvcResult result = mockMvc.perform(post("/address/insertAddress.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("addrNm", addrNm)
						.param("deptNm", deptNm)
						.param("telNo", telNo)
						.param("email", email)
						.param("memo", "메모"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.addrId");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestList(MockHttpSession session, String searchKeyword) throws Exception {
		MvcResult result = mockMvc.perform(get("/address/addressList")
						.session(session)
						.param("searchKeyword", searchKeyword))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	@Test
	@Rollback
	void addressShouldEncryptAndMaskWhileDetailStaysPlain() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String suffix = String.valueOf(System.nanoTime());
		String addrNm = "ZZ주소록-" + suffix;
		String deptNm = "ZZ부서-" + suffix;
		String addrId = insertAddress(session, addrNm, deptNm, "010-1234-5678", "testuser@example.com");

		String storedTel = jdbcTemplate.queryForObject(
				"SELECT TEL_NO FROM ADDRESS_BOOK WHERE ADDR_ID = ?", String.class, addrId);
		String storedEmail = jdbcTemplate.queryForObject(
				"SELECT EMAIL FROM ADDRESS_BOOK WHERE ADDR_ID = ?", String.class, addrId);
		assertThat(storedTel).startsWith("enc:v1:").doesNotContain("010-1234-5678");
		assertThat(storedEmail).startsWith("enc:v1:").doesNotContain("testuser@example.com");

		MvcResult detailResult = mockMvc.perform(get("/address/addressWrite")
						.session(session)
						.param("addrId", addrId))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("telNo"))).isEqualTo("010-1234-5678");
		assertThat(String.valueOf(detail.get("email"))).isEqualTo("testuser@example.com");

		List<EgovMap> rows = requestList(session, addrNm);
		assertThat(rows).hasSize(1);
		assertThat(String.valueOf(rows.get(0).get("addrNm"))).isEqualTo(addrNm);
		assertThat(String.valueOf(rows.get(0).get("telNo"))).isEqualTo("010-****-5678");
		assertThat(String.valueOf(rows.get(0).get("email"))).isEqualTo("te***@example.com");

		assertThat(requestList(session, deptNm)).hasSize(1);

		mockMvc.perform(post("/address/updateAddress.json")
						.session(session)
						.param("addrId", addrId)
						.param("addrNm", addrNm + "-수정")
						.param("deptNm", deptNm)
						.param("telNo", "010-9999-8888")
						.param("email", "changed@example.com")
						.param("memo", "수정 메모"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT TEL_NO FROM ADDRESS_BOOK WHERE ADDR_ID = ?", String.class, addrId))
				.startsWith("enc:v1:").doesNotContain("010-9999-8888");
		detailResult = mockMvc.perform(get("/address/addressWrite")
						.session(session)
						.param("addrId", addrId))
				.andReturn();
		detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("addrNm"))).isEqualTo(addrNm + "-수정");
		assertThat(String.valueOf(detail.get("telNo"))).isEqualTo("010-9999-8888");
		assertThat(String.valueOf(detail.get("email"))).isEqualTo("changed@example.com");

		mockMvc.perform(post("/address/deleteAddress.json")
						.session(session)
						.param("addrId", addrId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(requestList(session, addrNm)).isEmpty();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM ADDRESS_BOOK WHERE ADDR_ID = ?", Integer.class, addrId)).isZero();
	}

	@Test
	@Rollback
	void insertAndUpdateShouldValidateTokenAndName() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/address/insertAddress.json")
						.session(session)
						.param("addrNm", "토큰 없는 주소록"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_ADDRESS");
		mockMvc.perform(post("/address/insertAddress.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("addrNm", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이름을 입력하세요."));

		mockMvc.perform(post("/address/updateAddress.json")
						.session(session)
						.param("addrId", "AB-NONE")
						.param("addrNm", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이름을 입력하세요."));
	}
}
