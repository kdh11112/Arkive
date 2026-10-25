package arkive.admin.survey.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.survey.service.SurveyService;
import jakarta.annotation.Resource;

/**
 * 설문관리 세트 서비스 구현.
 *
 * <p>문항 수·보기 수 제한(2~10개)과 주관식 2000자 제한은 샘플 정책이다.
 * 공개(OPEN) 뒤에는 질문·보기를 바꿀 수 없고, 참여는 사용자당 1회만 허용한다.</p>
 */
@Service("surveyService")
public class SurveyServiceImpl extends EgovAbstractServiceImpl implements SurveyService {

	private static final String DRAFT = "DRAFT";
	private static final String OPEN = "OPEN";
	private static final String CLOSED = "CLOSED";
	private static final Set<String> QUESTION_TYPES = Set.of("SINGLE", "MULTI", "TEXT");
	private static final int MIN_ITEMS = 2;
	private static final int MAX_ITEMS = 10;
	private static final int MAX_TEXT_ANSWER = 2000;

	@Resource(name = "surveyMapper")
	private SurveyMapper surveyMapper;

	@Override
	public List<EgovMap> selectTemplateList(EgovMap egovMap) throws Exception {
		return surveyMapper.selectTemplateList(egovMap);
	}

	@Override
	public int selectTemplateListTotCnt(EgovMap egovMap) throws Exception {
		return surveyMapper.selectTemplateListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectActiveTemplateList() throws Exception {
		return surveyMapper.selectActiveTemplateList();
	}

	@Override
	public EgovMap selectTemplateDetail(String templateId) throws Exception {
		return isBlank(templateId) ? null : surveyMapper.selectTemplateDetail(templateId.trim());
	}

	@Override
	@Transactional
	public void insertTemplate(EgovMap egovMap) throws Exception {
		validateTemplate(egovMap);
		egovMap.put("templateId", "TP" + CommUtil.getFileId());
		surveyMapper.insertTemplate(egovMap);
	}

	@Override
	@Transactional
	public void updateTemplate(EgovMap egovMap) throws Exception {
		validateTemplate(egovMap);
		String templateId = trim(egovMap.get("templateId"));
		requireExisting(selectTemplateDetail(templateId), "템플릿");
		egovMap.put("templateId", templateId);
		if (surveyMapper.updateTemplate(egovMap) == 0) {
			throw new IllegalStateException("템플릿이 없습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteTemplate(String templateId) throws Exception {
		requireExisting(selectTemplateDetail(templateId), "템플릿");
		if (surveyMapper.countSurveyByTemplate(templateId.trim()) > 0) {
			throw new IllegalStateException("이 템플릿을 사용하는 설문이 있어 삭제할 수 없습니다.");
		}
		surveyMapper.deleteTemplate(templateId.trim());
	}

	@Override
	public List<EgovMap> selectSurveyList(EgovMap egovMap) throws Exception {
		return surveyMapper.selectSurveyList(egovMap);
	}

	@Override
	public int selectSurveyListTotCnt(EgovMap egovMap) throws Exception {
		return surveyMapper.selectSurveyListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectParticipableSurveyList(EgovMap egovMap) throws Exception {
		return surveyMapper.selectParticipableSurveyList(egovMap);
	}

	@Override
	public EgovMap selectSurveyDetail(String surveyId) throws Exception {
		return isBlank(surveyId) ? null : surveyMapper.selectSurveyDetail(surveyId.trim());
	}

	@Override
	public EgovMap selectSurveyForTake(String surveyId, String userId) throws Exception {
		EgovMap survey = selectSurveyDetail(surveyId);
		if (survey == null) {
			throw new IllegalArgumentException("설문이 없습니다.");
		}
		if (!OPEN.equals(trim(survey.get("status")))) {
			throw new IllegalStateException("진행중인 설문이 아닙니다.");
		}
		LocalDate today = LocalDate.now();
		LocalDate start = parseDate(trim(survey.get("startDe")));
		LocalDate end = parseDate(trim(survey.get("endDe")));
		if (today.isBefore(start) || today.isAfter(end)) {
			throw new IllegalStateException("설문 참여 기간이 아닙니다.");
		}
		EgovMap countParam = new EgovMap();
		countParam.put("surveyId", trim(surveyId));
		countParam.put("userId", userId);
		if (surveyMapper.countParticipation(countParam) > 0) {
			throw new IllegalStateException("이미 참여한 설문입니다.");
		}
		survey.put("questions", loadQuestions(trim(surveyId)));
		return survey;
	}

	@Override
	@Transactional
	public void insertSurvey(EgovMap egovMap) throws Exception {
		validateSurvey(egovMap);
		egovMap.put("surveyId", "SV" + CommUtil.getFileId());
		egovMap.put("status", DRAFT);
		surveyMapper.insertSurvey(egovMap);
	}

	@Override
	@Transactional
	public void updateSurvey(EgovMap egovMap) throws Exception {
		validateSurvey(egovMap);
		String surveyId = trim(egovMap.get("surveyId"));
		EgovMap survey = requireDraft(surveyId, "수정");
		egovMap.put("surveyId", surveyId);
		if (surveyMapper.updateSurvey(egovMap) == 0) {
			throw new IllegalStateException("작성중인 설문만 수정할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteSurvey(String surveyId) throws Exception {
		requireDraft(surveyId, "삭제");
		surveyMapper.deleteSurvey(surveyId.trim());
	}

	@Override
	@Transactional
	public void openSurvey(String surveyId) throws Exception {
		requireDraft(surveyId, "공개");
		if (surveyMapper.selectQuestionList(surveyId.trim()).isEmpty()) {
			throw new IllegalStateException("질문을 1개 이상 등록한 뒤 공개하세요.");
		}
		if (surveyMapper.openSurvey(surveyId.trim()) == 0) {
			throw new IllegalStateException("작성중인 설문만 공개할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void closeSurvey(String surveyId) throws Exception {
		EgovMap survey = selectSurveyDetail(surveyId);
		if (survey == null) {
			throw new IllegalArgumentException("설문이 없습니다.");
		}
		if (!OPEN.equals(trim(survey.get("status")))) {
			throw new IllegalStateException("공개중인 설문만 마감할 수 있습니다.");
		}
		if (surveyMapper.closeSurvey(surveyId.trim()) == 0) {
			throw new IllegalStateException("공개중인 설문만 마감할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	public List<EgovMap> selectQuestionList(String surveyId) throws Exception {
		if (isBlank(surveyId)) {
			return List.of();
		}
		return loadQuestions(surveyId.trim());
	}

	@Override
	public EgovMap selectQuestionDetail(String questionId) throws Exception {
		return isBlank(questionId) ? null : surveyMapper.selectQuestionDetail(questionId.trim());
	}

	@Override
	@Transactional
	public void insertQuestion(EgovMap egovMap) throws Exception {
		String surveyId = trim(egovMap.get("surveyId"));
		requireDraft(surveyId, "질문 등록");
		ValidatedQuestion question = validateQuestion(egovMap);
		egovMap.put("questionId", "SQ" + CommUtil.getFileId());
		egovMap.put("surveyId", surveyId);
		egovMap.put("ordr", surveyMapper.selectNextQuestionOrd(surveyId));
		surveyMapper.insertQuestion(egovMap);
		insertItems(String.valueOf(egovMap.get("questionId")), question);
	}

	@Override
	@Transactional
	public void updateQuestion(EgovMap egovMap) throws Exception {
		String questionId = trim(egovMap.get("questionId"));
		EgovMap existing = selectQuestionDetail(questionId);
		requireExisting(existing, "질문");
		requireDraft(trim(existing.get("surveyId")), "질문 수정");
		ValidatedQuestion question = validateQuestion(egovMap);
		egovMap.put("questionId", questionId);
		if (surveyMapper.updateQuestion(egovMap) == 0) {
			throw new IllegalStateException("질문이 없습니다. 새로고침 후 다시 시도하세요.");
		}
		surveyMapper.deleteItems(questionId);
		insertItems(questionId, question);
	}

	@Override
	@Transactional
	public void deleteQuestion(String questionId) throws Exception {
		EgovMap existing = selectQuestionDetail(questionId);
		requireExisting(existing, "질문");
		requireDraft(trim(existing.get("surveyId")), "질문 삭제");
		surveyMapper.deleteQuestion(questionId.trim());
	}

	@Override
	@Transactional
	public void submitSurvey(String surveyId, String userId, Map<String, List<String>> answers) throws Exception {
		EgovMap survey = selectSurveyForTake(surveyId, userId);
		String trimmedSurveyId = trim(surveyId);
		List<EgovMap> questions = castQuestions(survey.get("questions"));

		String participationId = "SP" + CommUtil.getFileId();
		List<EgovMap> answerRows = new ArrayList<>();
		for (EgovMap question : questions) {
			String questionId = trim(question.get("questionId"));
			String type = trim(question.get("questionType"));
			boolean required = "Y".equals(trim(question.get("requiredYn")));
			List<String> raw = answers == null ? List.of() : answers.getOrDefault(questionId, List.of());
			List<String> values = raw.stream().filter(v -> v != null && !v.trim().isEmpty()).map(String::trim).toList();

			if (values.isEmpty()) {
				if (required) {
					throw new IllegalArgumentException("필수 질문에 응답하세요: " + trim(question.get("questionTxt")));
				}
				continue;
			}
			if ("TEXT".equals(type)) {
				String text = String.join("\n", values);
				if (text.length() > MAX_TEXT_ANSWER) {
					throw new IllegalArgumentException("주관식 응답은 2000자 이내로 입력하세요.");
				}
				EgovMap answer = answerParam(participationId, questionId);
				answer.put("answerTxt", text);
				answerRows.add(answer);
				continue;
			}

			Set<String> itemIds = new LinkedHashSet<>(values);
			if ("SINGLE".equals(type) && itemIds.size() != 1) {
				throw new IllegalArgumentException("단일선택 질문은 1개만 선택하세요: " + trim(question.get("questionTxt")));
			}
			List<EgovMap> items = castItems(question.get("items"));
			Set<String> allowed = new LinkedHashSet<>();
			for (EgovMap item : items) {
				allowed.add(trim(item.get("itemId")));
			}
			for (String itemId : itemIds) {
				if (!allowed.contains(itemId)) {
					throw new IllegalArgumentException("질문에 없는 보기가 포함되어 있습니다.");
				}
				EgovMap answer = answerParam(participationId, questionId);
				answer.put("itemId", itemId);
				answerRows.add(answer);
			}
		}

		// 응답 FK가 참여를 참조하므로 참여를 먼저 넣는다. 검증 실패는 위에서 모두 끝났다.
		EgovMap participation = new EgovMap();
		participation.put("participationId", participationId);
		participation.put("surveyId", trimmedSurveyId);
		participation.put("userId", userId);
		surveyMapper.insertParticipation(participation);
		for (EgovMap answer : answerRows) {
			surveyMapper.insertAnswer(answer);
		}
	}

	@Override
	public boolean hasParticipated(String surveyId, String userId) throws Exception {
		if (isBlank(surveyId) || isBlank(userId)) {
			return false;
		}
		EgovMap param = new EgovMap();
		param.put("surveyId", surveyId.trim());
		param.put("userId", userId);
		return surveyMapper.countParticipation(param) > 0;
	}

	@Override
	public int selectParticipationCount(String surveyId) throws Exception {
		return isBlank(surveyId) ? 0 : surveyMapper.selectParticipationCount(surveyId.trim());
	}

	@Override
	public List<EgovMap> selectItemStats(String surveyId) throws Exception {
		if (isBlank(surveyId)) {
			return List.of();
		}
		int participants = surveyMapper.selectParticipationCount(surveyId.trim());
		List<EgovMap> stats = surveyMapper.selectItemStats(surveyId.trim());
		for (EgovMap row : stats) {
			int count = toInt(row.get("answerCnt"));
			row.put("rate", participants == 0 ? 0 : Math.round(count * 1000.0 / participants) / 10.0);
		}
		return stats;
	}

	@Override
	public List<EgovMap> selectTextAnswers(String surveyId) throws Exception {
		return isBlank(surveyId) ? List.of() : surveyMapper.selectTextAnswers(surveyId.trim());
	}

	private List<EgovMap> loadQuestions(String surveyId) throws Exception {
		List<EgovMap> questions = surveyMapper.selectQuestionList(surveyId);
		for (EgovMap question : questions) {
			question.put("items", surveyMapper.selectItemList(trim(question.get("questionId"))));
		}
		return questions;
	}

	private void insertItems(String questionId, ValidatedQuestion question) throws Exception {
		if (question.items() == null) {
			return;
		}
		int ordr = 1;
		for (String text : question.items()) {
			EgovMap item = new EgovMap();
			item.put("itemId", "SI" + CommUtil.getFileId());
			item.put("questionId", questionId);
			item.put("itemTxt", text);
			item.put("ordr", ordr++);
			surveyMapper.insertItem(item);
		}
	}

	private EgovMap answerParam(String participationId, String questionId) {
		EgovMap answer = new EgovMap();
		answer.put("answerId", "SA" + CommUtil.getFileId());
		answer.put("participationId", participationId);
		answer.put("questionId", questionId);
		return answer;
	}

	private EgovMap requireDraft(String surveyId, String action) throws Exception {
		EgovMap survey = selectSurveyDetail(surveyId);
		requireExisting(survey, "설문");
		if (!DRAFT.equals(trim(survey.get("status")))) {
			throw new IllegalStateException("작성중인 설문만 " + action + "할 수 있습니다.");
		}
		return survey;
	}

	private void validateTemplate(EgovMap egovMap) {
		String templateNm = trim(egovMap.get("templateNm"));
		String description = trim(egovMap.get("description"));
		String useYn = trim(egovMap.get("useYn"));
		if (templateNm.isEmpty() || templateNm.length() > 200) {
			throw new IllegalArgumentException("템플릿명은 1~200자로 입력하세요.");
		}
		if (description.length() > 1000) {
			throw new IllegalArgumentException("설명은 1000자 이내로 입력하세요.");
		}
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			throw new IllegalArgumentException("사용여부가 올바르지 않습니다.");
		}
		egovMap.put("templateNm", templateNm);
		egovMap.put("description", description);
		egovMap.put("useYn", useYn);
	}

	private void validateSurvey(EgovMap egovMap) throws Exception {
		String templateId = trim(egovMap.get("templateId"));
		String title = trim(egovMap.get("title"));
		String description = trim(egovMap.get("description"));
		String startDe = trim(egovMap.get("startDe"));
		String endDe = trim(egovMap.get("endDe"));
		String anonymousYn = trim(egovMap.get("anonymousYn"));

		if (title.isEmpty() || title.length() > 200) {
			throw new IllegalArgumentException("설문 제목은 1~200자로 입력하세요.");
		}
		if (description.length() > 2000) {
			throw new IllegalArgumentException("설명은 2000자 이내로 입력하세요.");
		}
		LocalDate start = parseDate(startDe);
		LocalDate end = parseDate(endDe);
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
		}
		if (!"Y".equals(anonymousYn) && !"N".equals(anonymousYn)) {
			throw new IllegalArgumentException("익명 여부가 올바르지 않습니다.");
		}
		if (!templateId.isEmpty()) {
			EgovMap template = selectTemplateDetail(templateId);
			if (template == null || !"Y".equals(trim(template.get("useYn")))) {
				throw new IllegalArgumentException("사용중인 템플릿을 선택하세요.");
			}
		}
		egovMap.put("templateId", templateId);
		egovMap.put("title", title);
		egovMap.put("description", description);
		egovMap.put("startDe", startDe);
		egovMap.put("endDe", endDe);
		egovMap.put("anonymousYn", anonymousYn);
	}

	private ValidatedQuestion validateQuestion(EgovMap egovMap) {
		String questionTxt = trim(egovMap.get("questionTxt"));
		String questionType = trim(egovMap.get("questionType"));
		String requiredYn = trim(egovMap.get("requiredYn"));

		if (questionTxt.isEmpty() || questionTxt.length() > 1000) {
			throw new IllegalArgumentException("질문은 1~1000자로 입력하세요.");
		}
		if (!QUESTION_TYPES.contains(questionType)) {
			throw new IllegalArgumentException("질문 유형이 올바르지 않습니다.");
		}
		if (!"Y".equals(requiredYn) && !"N".equals(requiredYn)) {
			throw new IllegalArgumentException("필수 여부가 올바르지 않습니다.");
		}

		List<String> items = null;
		if (!"TEXT".equals(questionType)) {
			Object raw = egovMap.get("items");
			List<String> candidates = new ArrayList<>();
			if (raw instanceof List<?> list) {
				for (Object value : list) {
					String text = value == null ? "" : String.valueOf(value).trim();
					if (!text.isEmpty()) {
						candidates.add(text);
					}
				}
			}
			if (candidates.size() < MIN_ITEMS || candidates.size() > MAX_ITEMS) {
				throw new IllegalArgumentException("객관식 보기는 " + MIN_ITEMS + "~" + MAX_ITEMS + "개 입력하세요.");
			}
			for (String text : candidates) {
				if (text.length() > 500) {
					throw new IllegalArgumentException("보기는 500자 이내로 입력하세요.");
				}
			}
			items = candidates;
		}
		egovMap.put("questionTxt", questionTxt);
		egovMap.put("questionType", questionType);
		egovMap.put("requiredYn", requiredYn);
		return new ValidatedQuestion(items);
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> castQuestions(Object value) {
		return value instanceof List<?> ? (List<EgovMap>) value : List.of();
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> castItems(Object value) {
		return value instanceof List<?> ? (List<EgovMap>) value : List.of();
	}

	private LocalDate parseDate(String value) {
		if (value == null || !value.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
			throw new IllegalArgumentException("날짜는 YYYY-MM-DD 형식으로 입력하세요.");
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("날짜는 YYYY-MM-DD 형식으로 입력하세요.");
		}
	}

	private int toInt(Object value) {
		return value instanceof Number number ? number.intValue() : 0;
	}

	private void requireExisting(EgovMap detail, String label) {
		if (detail == null) {
			throw new IllegalArgumentException(label + "이(가) 없습니다.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private record ValidatedQuestion(List<String> items) {
	}
}
