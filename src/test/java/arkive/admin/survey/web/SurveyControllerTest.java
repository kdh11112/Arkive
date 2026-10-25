package arkive.admin.survey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.survey.service.impl.SurveyMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 설문관리 세트의 화면 권한, 템플릿/설문/질문 CRUD, 공개 후 잠금,
 * 참여 응답 검증·중복 방지, 통계 집계를 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class SurveyControllerTest {

	private static final String START_DE = "2020-01-01";
	private static final String END_DE = "2099-12-31";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SurveyMapper surveyMapper;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Test
	void screensShouldFollowMenuPermission() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		mockMvc.perform(get("/survey/templateList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("설문템플릿")));
		mockMvc.perform(get("/survey/surveyList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("설문관리")));
		mockMvc.perform(get("/survey/participateList").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("설문참여")));
		mockMvc.perform(get("/survey/stats").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("설문 통계")));

		MockHttpSession user = userSession("survey-user");
		mockMvc.perform(get("/survey/participateList").session(user))
				.andExpect(status().isOk());
		mockMvc.perform(get("/survey/surveyList").session(user))
				.andExpect(status().isForbidden());
	}

	@Test
	void templateShouldFollowCrudAndBlockDeleteWhenUsed() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		MvcResult inserted = mockMvc.perform(post("/survey/insertTemplate.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "SURVEY_TEMPLATE"))
				.param("templateNm", "만족도 템플릿")
				.param("description", "만족도 조사 기본 설명")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		String templateId = idFrom(inserted);

		mockMvc.perform(post("/survey/updateTemplate.json").session(admin)
				.param("templateId", templateId)
				.param("templateNm", "만족도 템플릿(수정)")
				.param("description", "수정 설명")
				.param("useYn", "N"))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap template = surveyMapper.selectTemplateDetail(templateId);
		assertThat(template.get("templateNm")).isEqualTo("만족도 템플릿(수정)");
		assertThat(template.get("useYn")).isEqualTo("N");

		mockMvc.perform(post("/survey/updateTemplate.json").session(admin)
				.param("templateId", templateId)
				.param("templateNm", "만족도 템플릿(수정)")
				.param("description", "수정 설명")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/survey/insertTemplate.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "SURVEY_TEMPLATE"))
				.param("templateNm", " ")
				.param("description", "")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("템플릿명")));

		String surveyId = insertSurvey(admin, templateId);
		mockMvc.perform(post("/survey/deleteTemplate.json").session(admin).param("templateId", templateId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("설문")));
		assertThat(surveyMapper.selectTemplateDetail(templateId)).isNotNull();

		mockMvc.perform(post("/survey/deleteSurvey.json").session(admin).param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(post("/survey/deleteTemplate.json").session(admin).param("templateId", templateId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectTemplateDetail(templateId)).isNull();
	}

	@Test
	void surveyShouldOpenOnlyWithQuestionAndLockAfterOpen() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		String surveyId = insertSurvey(admin, "");

		mockMvc.perform(post("/survey/updateSurvey.json").session(admin)
				.param("surveyId", surveyId)
				.param("templateId", "")
				.param("title", "설문 수정")
				.param("description", "수정 설명")
				.param("startDe", START_DE)
				.param("endDe", END_DE)
				.param("anonymousYn", "Y"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectSurveyDetail(surveyId).get("title")).isEqualTo("설문 수정");

		mockMvc.perform(post("/survey/openSurvey.json").session(admin).param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("질문")));

		String questionId = insertChoiceQuestion(admin, surveyId, "SINGLE", "Y", List.of("매우 만족", "보통"));

		mockMvc.perform(post("/survey/openSurvey.json").session(admin).param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectSurveyDetail(surveyId).get("status")).isEqualTo("OPEN");

		mockMvc.perform(post("/survey/updateSurvey.json").session(admin)
				.param("surveyId", surveyId)
				.param("templateId", "")
				.param("title", "공개 후 수정")
				.param("description", "")
				.param("startDe", START_DE)
				.param("endDe", END_DE)
				.param("anonymousYn", "N"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("작성중")));

		mockMvc.perform(post("/survey/insertQuestion.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "SURVEY_QUESTION"))
				.param("surveyId", surveyId)
				.param("questionTxt", "공개 후 질문")
				.param("questionType", "TEXT")
				.param("requiredYn", "N"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("작성중")));

		mockMvc.perform(post("/survey/closeSurvey.json").session(admin).param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectSurveyDetail(surveyId).get("status")).isEqualTo("CLOSED");
	}

	@Test
	void questionShouldValidateTypesAndItems() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		String surveyId = insertSurvey(admin, "");

		mockMvc.perform(post("/survey/insertQuestion.json").session(admin)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(admin, "SURVEY_QUESTION"))
				.param("surveyId", surveyId)
				.param("questionTxt", "보기 부족")
				.param("questionType", "SINGLE")
				.param("requiredYn", "Y")
				.param("itemTxt", "하나만"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("2~10")));

		String questionId = insertChoiceQuestion(admin, surveyId, "MULTI", "N", List.of("A", "B", "C"));
		List<EgovMap> items = surveyMapper.selectItemList(questionId);
		assertThat(items).hasSize(3);

		mockMvc.perform(post("/survey/updateQuestion.json").session(admin)
				.param("questionId", questionId)
				.param("questionTxt", "복수선택(수정)")
				.param("questionType", "MULTI")
				.param("requiredYn", "N")
				.param("itemTxt", "A", "B"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectItemList(questionId)).hasSize(2);

		mockMvc.perform(post("/survey/updateQuestion.json").session(admin)
				.param("questionId", questionId)
				.param("questionTxt", "유형 오류")
				.param("questionType", "UNKNOWN")
				.param("requiredYn", "N"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("질문 유형")));

		mockMvc.perform(post("/survey/deleteQuestion.json").session(admin).param("questionId", questionId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(surveyMapper.selectQuestionDetail(questionId)).isNull();
	}

	@Test
	void participationShouldValidateAnswersAndPreventDuplicate() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		String surveyId = insertSurvey(admin, "");
		String singleId = insertChoiceQuestion(admin, surveyId, "SINGLE", "Y", List.of("찬성", "반대"));
		String multiId = insertChoiceQuestion(admin, surveyId, "MULTI", "N", List.of("기능", "디자인"));
		String textId = insertTextQuestion(admin, surveyId, "Y", "개선 의견");
		mockMvc.perform(post("/survey/openSurvey.json").session(admin).param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(true));

		MockHttpSession user = userSession("survey-taker");
		mockMvc.perform(get("/survey/participateList").session(user))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("설문 참여 테스트")));
		mockMvc.perform(get("/survey/surveyTake").session(user).param("surveyId", surveyId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("찬성")))
				.andExpect(content().string(containsString("개선 의견")));

		List<EgovMap> singleItems = surveyMapper.selectItemList(singleId);
		String first = String.valueOf(singleItems.get(0).get("itemId"));
		String second = String.valueOf(singleItems.get(1).get("itemId"));

		mockMvc.perform(post("/survey/submitSurvey.json").session(user)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(user, "SURVEY_TAKE"))
				.param("surveyId", surveyId)
				.param("answers_" + singleId, first)
				.param("answers_" + multiId, String.valueOf(surveyMapper.selectItemList(multiId).get(0).get("itemId"))))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("필수")));

		mockMvc.perform(post("/survey/submitSurvey.json").session(user)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(user, "SURVEY_TAKE"))
				.param("surveyId", surveyId)
				.param("answers_" + singleId, first, second)
				.param("answers_" + textId, "의견"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("1개만")));

		mockMvc.perform(post("/survey/submitSurvey.json").session(user)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(user, "SURVEY_TAKE"))
				.param("surveyId", surveyId)
				.param("answers_" + singleId, first)
				.param("answers_" + multiId,
						String.valueOf(surveyMapper.selectItemList(multiId).get(0).get("itemId")),
						String.valueOf(surveyMapper.selectItemList(multiId).get(1).get("itemId")))
				.param("answers_" + textId, "검색이 느립니다."))
				.andExpect(jsonPath("$.success").value(true));

		EgovMap countParam = new EgovMap();
		countParam.put("surveyId", surveyId);
		countParam.put("userId", "survey-taker");
		assertThat(surveyMapper.countParticipation(countParam)).isEqualTo(1);

		mockMvc.perform(get("/survey/surveyTake").session(user).param("surveyId", surveyId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("이미 참여한 설문")));

		mockMvc.perform(post("/survey/submitSurvey.json").session(user)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(user, "SURVEY_TAKE"))
				.param("surveyId", surveyId)
				.param("answers_" + singleId, first)
				.param("answers_" + textId, "중복"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("이미 참여")));

		mockMvc.perform(get("/survey/stats").session(admin).param("surveyId", surveyId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("참여자")))
				.andExpect(content().string(containsString("검색이 느립니다.")))
				.andExpect(content().string(containsString("100.0%")));
	}

	private MockHttpSession userSession(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", "encoded-test-password");
		user.put("userNm", userId + "-name");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);

		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", userId);
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		return TestSessions.user(userId, userId + "-name");
	}

	private String insertSurvey(MockHttpSession session, String templateId) throws Exception {
		MvcResult result = mockMvc.perform(post("/survey/insertSurvey.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "SURVEY"))
				.param("templateId", templateId)
				.param("title", "설문 참여 테스트")
				.param("description", "기본 설명")
				.param("startDe", START_DE)
				.param("endDe", END_DE)
				.param("anonymousYn", "N"))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return idFrom(result);
	}

	private String insertChoiceQuestion(MockHttpSession session, String surveyId, String type,
			String requiredYn, List<String> items) throws Exception {
		var request = post("/survey/insertQuestion.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "SURVEY_QUESTION"))
				.param("surveyId", surveyId)
				.param("questionTxt", "질문 " + type)
				.param("questionType", type)
				.param("requiredYn", requiredYn);
		for (String item : items) {
			request.param("itemTxt", item);
		}
		MvcResult result = mockMvc.perform(request)
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return idFrom(result);
	}

	private String insertTextQuestion(MockHttpSession session, String surveyId, String requiredYn,
			String questionTxt) throws Exception {
		MvcResult result = mockMvc.perform(post("/survey/insertQuestion.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "SURVEY_QUESTION"))
				.param("surveyId", surveyId)
				.param("questionTxt", questionTxt)
				.param("questionType", "TEXT")
				.param("requiredYn", requiredYn))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return idFrom(result);
	}

	private String idFrom(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		String marker = "\"id\":\"";
		int start = body.indexOf(marker);
		return body.substring(start + marker.length(), body.indexOf('"', start + marker.length()));
	}
}
