package arkive.admin.system.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 공통코드 마스터·상세 CRUD, 중복 검사, 마스터 삭제 시 상세 연쇄 삭제를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class CommCodeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private MockHttpSession admin() {
		return TestSessions.admin("admin");
	}

	private void insertMaster(MockHttpSession session, String mastrCode, String codeNm) throws Exception {
		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", mastrCode)
						.param("isMaster", "true")
						.param("codeNm", codeNm)
						.param("ordr", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("success"));
	}

	private void insertDetail(MockHttpSession session, String mastrCode, String detailCode, String codeNm,
			String ordr, String useYn) throws Exception {
		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", mastrCode)
						.param("detailCode", detailCode)
						.param("codeNm", codeNm)
						.param("ordr", ordr)
						.param("useYn", useYn))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value("success"));
	}

	@Test
	@Rollback
	void masterAndDetailShouldBeManagedAndCascadeDeleted() throws Exception {
		MockHttpSession session = admin();
		insertMaster(session, "ZZTEST01", "테스트 분류");
		insertDetail(session, "ZZTEST01", "Z01", "첫째", "1", "Y");
		insertDetail(session, "ZZTEST01", "Z02", "둘째", "2", "N");

		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST01")
						.param("detailCode", "Z01")
						.param("codeNm", "중복"))
				.andExpect(jsonPath("$.result").value("fail"))
				.andExpect(jsonPath("$.message").value(containsString("이미 등록된 코드")));

		mockMvc.perform(post("/system/getMasterList.json")
						.session(session)
						.param("searchKeyword", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(1))
				.andExpect(jsonPath("$.resultList[0].detailCode").value("ZZTEST01"))
				.andExpect(jsonPath("$.resultList[0].codeNm").value("테스트 분류"));

		mockMvc.perform(post("/system/getDetailList.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(2))
				.andExpect(jsonPath("$.resultList[0].detailCode").value("Z01"));

		mockMvc.perform(post("/system/getCmmnCodeList.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(1))
				.andExpect(jsonPath("$.resultList[0].detailCode").value("Z01"))
				.andExpect(jsonPath("$.resultList[0].codeNm").value("첫째"));

		mockMvc.perform(post("/system/getCodeInfo.json")
						.session(session)
						.param("mastrCode", "ZZTEST01")
						.param("detailCode", "Z01"))
				.andExpect(jsonPath("$.result.codeNm").value("첫째"));

		mockMvc.perform(post("/system/setUpdateCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST01")
						.param("detailCode", "Z01")
						.param("codeNm", "수정된 첫째")
						.param("ordr", "3")
						.param("useYn", "N"))
				.andExpect(jsonPath("$.result").value("success"));

		// 사용중인 코드만 내려주므로 수정으로 USE_YN=N이 되면 공용 조회에서 빠진다.
		mockMvc.perform(post("/system/getCmmnCodeList.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(0));

		mockMvc.perform(post("/system/setDeleteCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST01")
						.param("detailCode", "Z01"))
				.andExpect(jsonPath("$.result").value("success"));
		mockMvc.perform(post("/system/getDetailList.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(1));

		// 마스터 삭제는 DETAIL_CODE 조건 없이 호출해 하위 상세까지 지운다.
		mockMvc.perform(post("/system/setDeleteCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.result").value("success"));
		mockMvc.perform(post("/system/getMasterList.json")
						.session(session)
						.param("searchKeyword", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(0));
		mockMvc.perform(post("/system/getDetailList.json")
						.session(session)
						.param("mastrCode", "ZZTEST01"))
				.andExpect(jsonPath("$.resultList.length()").value(0));
	}

	@Test
	@Rollback
	void insertShouldValidateRequiredCodeFields() throws Exception {
		MockHttpSession session = admin();

		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("isMaster", "true")
						.param("codeNm", "분류"))
				.andExpect(jsonPath("$.result").value("fail"))
				.andExpect(jsonPath("$.message").value("마스터코드는 필수입니다."));

		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST02")
						.param("isMaster", "true"))
				.andExpect(jsonPath("$.result").value("fail"))
				.andExpect(jsonPath("$.message").value("코드명은 필수입니다."));

		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST02")
						.param("codeNm", "항목"))
				.andExpect(jsonPath("$.result").value("fail"))
				.andExpect(jsonPath("$.message").value("상세코드는 필수입니다."));

		insertMaster(session, "ZZTEST02", "분류2");
		mockMvc.perform(post("/system/setInsertCode.json")
						.session(session)
						.param("mastrCode", "ZZTEST02")
						.param("isMaster", "true")
						.param("codeNm", "분류2"))
				.andExpect(jsonPath("$.result").value("fail"))
				.andExpect(jsonPath("$.message").value("이미 등록된 코드입니다."));
	}
}
