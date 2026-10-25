package arkive.admin.survey.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.survey.service.SurveyService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 설문관리 세트. 템플릿·설문·질문/보기 관리, 사용자 참여, 통계 화면을 제공한다.
 * 관리 화면과 참여 화면은 메뉴 권한으로 분리하며 MenuPermissionInterceptor가 서버에서 검사한다.
 */
@Tag(name = "설문관리", description = "템플릿·설문·질문·보기·참여·통계")
@Controller
@RequestMapping("/survey")
public class SurveyController {

	private static final Set<String> SURVEY_STATUSES = Set.of("DRAFT", "OPEN", "CLOSED");

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "surveyService")
	private SurveyService surveyService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "설문 템플릿 목록")
	@RequestMapping(value = "/templateList", method = { RequestMethod.GET, RequestMethod.POST })
	public String templateList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);
		String useYn = cmmUtil.convertHtml(request, "useYn");
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			useYn = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("useYn", useYn);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("useYn", useYn);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", surveyService.selectTemplateList(param));
		paginationInfo.setTotalRecordCount(surveyService.selectTemplateListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "survey/templateList";
	}

	@Operation(summary = "설문 템플릿 등록·수정 화면")
	@RequestMapping(value = "/templateWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String templateWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String templateId = cmmUtil.convertHtml(request, "templateId");
		if (!templateId.trim().isEmpty()) {
			EgovMap detail = surveyService.selectTemplateDetail(templateId);
			if (detail == null) {
				throw new IllegalArgumentException("템플릿이 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "SURVEY_TEMPLATE"));
		return "survey/templateWrite";
	}

	@Operation(summary = "설문 템플릿 등록")
	@RequestMapping(value = "/insertTemplate.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertTemplate(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "SURVEY_TEMPLATE");
			EgovMap param = templateParam(request);
			surveyService.insertTemplate(param);
			return param.get("templateId");
		});
	}

	@Operation(summary = "설문 템플릿 수정")
	@RequestMapping(value = "/updateTemplate.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateTemplate(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = templateParam(request);
			param.put("templateId", cmmUtil.convertHtml(request, "templateId"));
			surveyService.updateTemplate(param);
			return param.get("templateId");
		});
	}

	@Operation(summary = "설문 템플릿 삭제")
	@RequestMapping(value = "/deleteTemplate.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteTemplate(HttpServletRequest request) {
		return execute(() -> {
			surveyService.deleteTemplate(cmmUtil.convertHtml(request, "templateId"));
			return null;
		});
	}

	@Operation(summary = "설문 목록")
	@RequestMapping(value = "/surveyList", method = { RequestMethod.GET, RequestMethod.POST })
	public String surveyList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);
		String status = cmmUtil.convertHtml(request, "status");
		if (!SURVEY_STATUSES.contains(status)) {
			status = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("status", status);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("status", status);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", surveyService.selectSurveyList(param));
		paginationInfo.setTotalRecordCount(surveyService.selectSurveyListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "survey/surveyList";
	}

	@Operation(summary = "설문 등록·수정 화면")
	@RequestMapping(value = "/surveyWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String surveyWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String surveyId = cmmUtil.convertHtml(request, "surveyId");
		if (!surveyId.trim().isEmpty()) {
			EgovMap detail = surveyService.selectSurveyDetail(surveyId);
			if (detail == null) {
				throw new IllegalArgumentException("설문이 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("templateList", surveyService.selectActiveTemplateList());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "SURVEY"));
		return "survey/surveyWrite";
	}

	@Operation(summary = "설문 등록")
	@RequestMapping(value = "/insertSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertSurvey(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "SURVEY");
			EgovMap param = surveyParam(request);
			surveyService.insertSurvey(param);
			return param.get("surveyId");
		});
	}

	@Operation(summary = "설문 수정 (작성중)")
	@RequestMapping(value = "/updateSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateSurvey(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = surveyParam(request);
			param.put("surveyId", cmmUtil.convertHtml(request, "surveyId"));
			surveyService.updateSurvey(param);
			return param.get("surveyId");
		});
	}

	@Operation(summary = "설문 삭제 (작성중)")
	@RequestMapping(value = "/deleteSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteSurvey(HttpServletRequest request) {
		return execute(() -> {
			surveyService.deleteSurvey(cmmUtil.convertHtml(request, "surveyId"));
			return null;
		});
	}

	@Operation(summary = "설문 공개 (작성중 → 공개)")
	@RequestMapping(value = "/openSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> openSurvey(HttpServletRequest request) {
		return execute(() -> {
			surveyService.openSurvey(cmmUtil.convertHtml(request, "surveyId"));
			return null;
		});
	}

	@Operation(summary = "설문 마감 (공개 → 마감)")
	@RequestMapping(value = "/closeSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> closeSurvey(HttpServletRequest request) {
		return execute(() -> {
			surveyService.closeSurvey(cmmUtil.convertHtml(request, "surveyId"));
			return null;
		});
	}

	@Operation(summary = "질문·보기 목록")
	@RequestMapping(value = "/questionList", method = { RequestMethod.GET, RequestMethod.POST })
	public String questionList(HttpServletRequest request, ModelMap model) throws Exception {
		String surveyId = cmmUtil.convertHtml(request, "surveyId");
		EgovMap survey = surveyService.selectSurveyDetail(surveyId);
		if (survey == null) {
			return "redirect:/survey/surveyList";
		}
		model.put("survey", survey);
		model.put("resultList", surveyService.selectQuestionList(surveyId));
		return "survey/questionList";
	}

	@Operation(summary = "질문·보기 등록·수정 화면")
	@RequestMapping(value = "/questionWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String questionWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String surveyId = cmmUtil.convertHtml(request, "surveyId");
		String questionId = cmmUtil.convertHtml(request, "questionId");
		if (!questionId.trim().isEmpty()) {
			EgovMap detail = surveyService.selectQuestionDetail(questionId);
			if (detail == null) {
				throw new IllegalArgumentException("질문이 없습니다.");
			}
			surveyId = String.valueOf(detail.get("surveyId"));
			detail.put("items", surveyService.selectQuestionList(surveyId).stream()
					.filter(q -> questionId.equals(String.valueOf(q.get("questionId"))))
					.findFirst().map(q -> q.get("items")).orElse(List.of()));
			model.put("detail", detail);
		}
		EgovMap survey = surveyService.selectSurveyDetail(surveyId);
		if (survey == null) {
			return "redirect:/survey/surveyList";
		}
		model.put("survey", survey);
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "SURVEY_QUESTION"));
		return "survey/questionWrite";
	}

	@Operation(summary = "질문·보기 등록")
	@RequestMapping(value = "/insertQuestion.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertQuestion(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "SURVEY_QUESTION");
			EgovMap param = questionParam(request);
			param.put("surveyId", cmmUtil.convertHtml(request, "surveyId"));
			surveyService.insertQuestion(param);
			return param.get("questionId");
		});
	}

	@Operation(summary = "질문·보기 수정")
	@RequestMapping(value = "/updateQuestion.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateQuestion(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = questionParam(request);
			param.put("questionId", cmmUtil.convertHtml(request, "questionId"));
			surveyService.updateQuestion(param);
			return param.get("questionId");
		});
	}

	@Operation(summary = "질문·보기 삭제")
	@RequestMapping(value = "/deleteQuestion.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteQuestion(HttpServletRequest request) {
		return execute(() -> {
			surveyService.deleteQuestion(cmmUtil.convertHtml(request, "questionId"));
			return null;
		});
	}

	@Operation(summary = "참여 가능한 설문 목록")
	@RequestMapping(value = "/participateList", method = { RequestMethod.GET, RequestMethod.POST })
	public String participateList(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		EgovMap param = new EgovMap();
		param.put("userId", String.valueOf(loginUser.get("userId")));
		param.put("today", LocalDate.now().toString());
		model.put("resultList", surveyService.selectParticipableSurveyList(param));
		return "survey/participateList";
	}

	@Operation(summary = "설문 응답 화면")
	@RequestMapping(value = "/surveyTake", method = { RequestMethod.GET, RequestMethod.POST })
	public String surveyTake(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String surveyId = cmmUtil.convertHtml(request, "surveyId");
		try {
			model.put("survey", surveyService.selectSurveyForTake(surveyId,
					String.valueOf(loginUser.get("userId"))));
			model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "SURVEY_TAKE"));
		} catch (Exception e) {
			model.put("errorMessage", e.getMessage());
			model.put("surveyId", surveyId);
		}
		return "survey/surveyTake";
	}

	@Operation(summary = "설문 응답 제출")
	@RequestMapping(value = "/submitSurvey.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> submitSurvey(HttpServletRequest request) {
		return execute(() -> {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			requireToken(request, "SURVEY_TAKE");
			String surveyId = cmmUtil.convertHtml(request, "surveyId");
			Map<String, List<String>> answers = new LinkedHashMap<>();
			for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("answers_")) {
					String questionId = key.substring("answers_".length());
					List<String> values = new ArrayList<>();
					for (String value : entry.getValue()) {
						values.add(value);
					}
					answers.put(questionId, values);
				}
			}
			surveyService.submitSurvey(surveyId, String.valueOf(loginUser.get("userId")), answers);
			return null;
		});
	}

	@Operation(summary = "설문 통계")
	@RequestMapping(value = "/stats", method = { RequestMethod.GET, RequestMethod.POST })
	public String stats(HttpServletRequest request, ModelMap model) throws Exception {
		String surveyId = cmmUtil.convertHtml(request, "surveyId");
		EgovMap listParam = new EgovMap();
		listParam.put("status", "");
		listParam.put("searchKeyword", "");
		listParam.put("firstIndex", 0);
		listParam.put("recordCountPerPage", 200);
		model.put("surveyList", surveyService.selectSurveyList(listParam));
		model.put("surveyId", surveyId);
		EgovMap survey = surveyService.selectSurveyDetail(surveyId);
		if (survey != null) {
			model.put("survey", survey);
			model.put("participationCount", surveyService.selectParticipationCount(surveyId));
			model.put("statGroups", groupItemStats(surveyService.selectItemStats(surveyId)));
			model.put("textAnswers", surveyService.selectTextAnswers(surveyId));
		}
		return "survey/stats";
	}

	private List<EgovMap> groupItemStats(List<EgovMap> rows) {
		List<EgovMap> groups = new ArrayList<>();
		EgovMap current = null;
		for (EgovMap row : rows) {
			String questionId = String.valueOf(row.get("questionId"));
			if (current == null || !questionId.equals(String.valueOf(current.get("questionId")))) {
				current = new EgovMap();
				current.put("questionId", row.get("questionId"));
				current.put("questionNo", row.get("questionNo"));
				current.put("questionTxt", row.get("questionTxt"));
				current.put("questionType", row.get("questionType"));
				current.put("items", new ArrayList<EgovMap>());
				groups.add(current);
			}
			@SuppressWarnings("unchecked")
			List<EgovMap> items = (List<EgovMap>) current.get("items");
			items.add(row);
		}
		return groups;
	}

	private EgovMap templateParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("templateNm", cmmUtil.convertHtml(request, "templateNm"));
		param.put("description", cmmUtil.convertHtml(request, "description"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		return param;
	}

	private EgovMap surveyParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("templateId", cmmUtil.convertHtml(request, "templateId"));
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("description", cmmUtil.convertHtml(request, "description"));
		param.put("startDe", cmmUtil.convertHtml(request, "startDe"));
		param.put("endDe", cmmUtil.convertHtml(request, "endDe"));
		param.put("anonymousYn", cmmUtil.convertHtml(request, "anonymousYn"));
		return param;
	}

	private EgovMap questionParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("questionTxt", cmmUtil.convertHtml(request, "questionTxt"));
		param.put("questionType", cmmUtil.convertHtml(request, "questionType"));
		param.put("requiredYn", cmmUtil.convertHtml(request, "requiredYn"));
		List<String> items = new ArrayList<>();
		String[] values = request.getParameterValues("itemTxt");
		if (values != null) {
			for (String value : values) {
				items.add(value);
			}
		}
		param.put("items", items);
		return param;
	}

	private Map<String, Object> execute(Action action) {
		Map<String, Object> result = new HashMap<>();
		try {
			Object data = action.run();
			result.put("success", true);
			if (data != null) {
				result.put("id", data);
			}
		} catch (Exception e) {
			logger.error("survey action failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private void requireToken(HttpServletRequest request, String key) {
		boolean valid;
		try {
			valid = EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			valid = false;
		}
		if (!valid) {
			throw new IllegalStateException("중복 제출이 감지되었습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
		}
	}

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private PaginationInfo pagination(int pageIndex) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		return paginationInfo;
	}

	private int pageIndex(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	@FunctionalInterface
	private interface Action {
		Object run() throws Exception;
	}
}
