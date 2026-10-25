package arkive.admin.card.web;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 명함 공개/내 명함 탭·사용등록·본인 등록분 수정/삭제·개인정보 암호화를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class CardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertCard(MockHttpSession session, String cardNm, String companyNm, String register,
			String openYn, String telNo, String email) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_CARD");
		MockHttpServletRequestBuilder builder = post("/card/insertCard.json")
				.session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
				.param("cardNm", cardNm)
				.param("companyNm", companyNm)
				.param("deptNm", "테스트부서")
				.param("positionNm", "사원")
				.param("telNo", telNo)
				.param("email", email)
				.param("memo", "명함 메모")
				.param("register", register);
		if (openYn != null) {
			builder.param("openYn", openYn);
		}
		MvcResult result = mockMvc.perform(builder)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.cardId");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestList(MockHttpSession session, String tab, String userNm, String searchKeyword)
			throws Exception {
		MvcResult result = mockMvc.perform(get("/card/cardList")
						.session(session)
						.param("tab", tab)
						.param("userNm", userNm)
						.param("searchKeyword", searchKeyword))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	private EgovMap findById(List<EgovMap> rows, String cardId) {
		return rows.stream()
				.filter(row -> cardId.equals(String.valueOf(row.get("cardId"))))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@Rollback
	void cardShouldSeparatePublicAndMineAndTrackUsage() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String suffix = String.valueOf(System.nanoTime());
		String company = "ZZ회사-" + suffix;
		String owner = "ZZ등록자-" + suffix;
		String user = "ZZ사용자-" + suffix;
		String otherOwner = "ZZ타인-" + suffix;

		String publicId = insertCard(session, "공개명함", company, owner, null,
				"010-1234-5678", "card@example.com");
		String privateId = insertCard(session, "비공개명함", company, owner, "N",
				"010-2222-3333", "private@example.com");

		assertThat(jdbcTemplate.queryForObject(
				"SELECT OPEN_YN FROM BUSINESS_CARD WHERE CARD_ID = ?", String.class, publicId)).isEqualTo("Y");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT TEL_NO FROM BUSINESS_CARD WHERE CARD_ID = ?", String.class, publicId))
				.startsWith("enc:v1:").doesNotContain("010-1234-5678");

		List<EgovMap> publicRows = requestList(session, "public", user, company);
		assertThat(publicRows).hasSize(1);
		assertThat(String.valueOf(publicRows.get(0).get("cardId"))).isEqualTo(publicId);
		assertThat(String.valueOf(publicRows.get(0).get("usedYn"))).isEqualTo("N");
		assertThat(String.valueOf(publicRows.get(0).get("telNo"))).isEqualTo("010-****-5678");

		mockMvc.perform(post("/card/useCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("userNm", user))
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(post("/card/useCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("userNm", user))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM CARD_USER WHERE CARD_ID = ? AND USER_NM = ?",
				Integer.class, publicId, user)).isEqualTo(1);
		assertThat(String.valueOf(findById(requestList(session, "public", user, company), publicId)
				.get("usedYn"))).isEqualTo("Y");

		List<EgovMap> mineAsUser = requestList(session, "mine", user, company);
		assertThat(mineAsUser).hasSize(1);
		assertThat(String.valueOf(mineAsUser.get(0).get("cardId"))).isEqualTo(publicId);

		List<EgovMap> mineAsOwner = requestList(session, "mine", owner, company);
		assertThat(mineAsOwner).hasSize(2);
		assertThat(String.valueOf(findById(mineAsOwner, privateId).get("openYn"))).isEqualTo("N");

		mockMvc.perform(post("/card/cancelUseCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("userNm", user))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(String.valueOf(findById(requestList(session, "public", user, company), publicId)
				.get("usedYn"))).isEqualTo("N");
		assertThat(requestList(session, "mine", user, company)).isEmpty();

		mockMvc.perform(post("/card/updateCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("cardNm", "타인 수정 시도")
						.param("register", otherOwner))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("본인이 등록한 명함만 수정할 수 있습니다."));

		mockMvc.perform(post("/card/updateCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("cardNm", "수정 명함")
						.param("companyNm", company + "-수정")
						.param("telNo", "010-7777-6666")
						.param("email", "updated@example.com")
						.param("openYn", "N")
						.param("register", owner))
				.andExpect(jsonPath("$.success").value(true));
		MvcResult detailResult = mockMvc.perform(get("/card/cardWrite")
						.session(session)
						.param("cardId", publicId))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap detail = (EgovMap) detailResult.getModelAndView().getModel().get("detail");
		assertThat(String.valueOf(detail.get("cardNm"))).isEqualTo("수정 명함");
		assertThat(String.valueOf(detail.get("openYn"))).isEqualTo("N");
		assertThat(String.valueOf(detail.get("telNo"))).isEqualTo("010-7777-6666");
		assertThat(String.valueOf(detail.get("email"))).isEqualTo("updated@example.com");

		mockMvc.perform(post("/card/useCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("userNm", user));
		mockMvc.perform(post("/card/deleteCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("register", otherOwner))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("본인이 등록한 명함만 삭제할 수 있습니다."));
		mockMvc.perform(post("/card/deleteCard.json")
						.session(session)
						.param("cardId", publicId)
						.param("register", owner))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM BUSINESS_CARD WHERE CARD_ID = ?", Integer.class, publicId)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM CARD_USER WHERE CARD_ID = ?", Integer.class, publicId)).isZero();
	}

	@Test
	@Rollback
	void insertShouldValidateTokenAndRequiredFields() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String suffix = String.valueOf(System.nanoTime());

		mockMvc.perform(post("/card/insertCard.json")
						.session(session)
						.param("cardNm", "토큰 없는 명함")
						.param("register", "등록자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_CARD");
		mockMvc.perform(post("/card/insertCard.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("cardNm", " ")
						.param("register", "등록자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이름을 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_CARD");
		mockMvc.perform(post("/card/insertCard.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("cardNm", "등록자 없는 명함"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("등록자를 입력하세요."));

		mockMvc.perform(post("/card/useCard.json")
						.session(session)
						.param("cardId", "NC-NONE")
						.param("userNm", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("사용자 이름을 입력하세요."));

		String cardId = insertCard(session, "공개여부 기본값", "ZZ회사-" + suffix, "ZZ등록자-" + suffix, null,
				null, null);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT OPEN_YN FROM BUSINESS_CARD WHERE CARD_ID = ?", String.class, cardId)).isEqualTo("Y");

		mockMvc.perform(post("/card/updateCard.json")
						.session(session)
						.param("cardId", cardId)
						.param("cardNm", " ")
						.param("register", "ZZ등록자-" + suffix))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("이름을 입력하세요."));
	}
}
