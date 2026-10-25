package arkive.admin.survey.service;

import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 설문관리 세트 서비스. 템플릿·설문·질문·항목·참여·통계를 한 서비스에서 다룬다.
 *
 * <p>질문은 설문이 작성(DRAFT) 상태일 때만 수정할 수 있고, 공개(OPEN) 후에는
 * 참여·통계의 기준이 흔들리지 않도록 변경을 막는다. 참여는 설문 1건당 사용자 1회다.</p>
 */
public interface SurveyService {

	List<EgovMap> selectTemplateList(EgovMap egovMap) throws Exception;

	int selectTemplateListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectActiveTemplateList() throws Exception;

	EgovMap selectTemplateDetail(String templateId) throws Exception;

	void insertTemplate(EgovMap egovMap) throws Exception;

	void updateTemplate(EgovMap egovMap) throws Exception;

	void deleteTemplate(String templateId) throws Exception;

	List<EgovMap> selectSurveyList(EgovMap egovMap) throws Exception;

	int selectSurveyListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectParticipableSurveyList(EgovMap egovMap) throws Exception;

	EgovMap selectSurveyDetail(String surveyId) throws Exception;

	EgovMap selectSurveyForTake(String surveyId, String userId) throws Exception;

	void insertSurvey(EgovMap egovMap) throws Exception;

	void updateSurvey(EgovMap egovMap) throws Exception;

	void deleteSurvey(String surveyId) throws Exception;

	void openSurvey(String surveyId) throws Exception;

	void closeSurvey(String surveyId) throws Exception;

	List<EgovMap> selectQuestionList(String surveyId) throws Exception;

	EgovMap selectQuestionDetail(String questionId) throws Exception;

	void insertQuestion(EgovMap egovMap) throws Exception;

	void updateQuestion(EgovMap egovMap) throws Exception;

	void deleteQuestion(String questionId) throws Exception;

	void submitSurvey(String surveyId, String userId, Map<String, List<String>> answers) throws Exception;

	boolean hasParticipated(String surveyId, String userId) throws Exception;

	int selectParticipationCount(String surveyId) throws Exception;

	List<EgovMap> selectItemStats(String surveyId) throws Exception;

	List<EgovMap> selectTextAnswers(String surveyId) throws Exception;
}
