package arkive.admin.survey.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 설문관리 세트 MyBatis 매퍼. SQL은 Survey_SQL.xml에 있다.
 */
@EgovMapper("surveyMapper")
public interface SurveyMapper {

	List<EgovMap> selectTemplateList(EgovMap egovMap) throws Exception;

	int selectTemplateListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectActiveTemplateList() throws Exception;

	EgovMap selectTemplateDetail(String templateId) throws Exception;

	int countSurveyByTemplate(String templateId) throws Exception;

	void insertTemplate(EgovMap egovMap) throws Exception;

	int updateTemplate(EgovMap egovMap) throws Exception;

	int deleteTemplate(String templateId) throws Exception;

	List<EgovMap> selectSurveyList(EgovMap egovMap) throws Exception;

	int selectSurveyListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectParticipableSurveyList(EgovMap egovMap) throws Exception;

	EgovMap selectSurveyDetail(String surveyId) throws Exception;

	void insertSurvey(EgovMap egovMap) throws Exception;

	int updateSurvey(EgovMap egovMap) throws Exception;

	int deleteSurvey(String surveyId) throws Exception;

	int openSurvey(String surveyId) throws Exception;

	int closeSurvey(String surveyId) throws Exception;

	List<EgovMap> selectQuestionList(String surveyId) throws Exception;

	EgovMap selectQuestionDetail(String questionId) throws Exception;

	int selectNextQuestionOrd(String surveyId) throws Exception;

	void insertQuestion(EgovMap egovMap) throws Exception;

	int updateQuestion(EgovMap egovMap) throws Exception;

	int deleteQuestion(String questionId) throws Exception;

	List<EgovMap> selectItemList(String questionId) throws Exception;

	void insertItem(EgovMap egovMap) throws Exception;

	int deleteItems(String questionId) throws Exception;

	int countParticipation(EgovMap egovMap) throws Exception;

	int selectParticipationCount(String surveyId) throws Exception;

	void insertParticipation(EgovMap egovMap) throws Exception;

	void insertAnswer(EgovMap egovMap) throws Exception;

	List<EgovMap> selectItemStats(String surveyId) throws Exception;

	List<EgovMap> selectTextAnswers(String surveyId) throws Exception;
}
