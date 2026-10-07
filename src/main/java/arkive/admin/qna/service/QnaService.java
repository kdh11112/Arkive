package arkive.admin.qna.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * Q&A 서비스. 질문 등록(일반)과 답변 등록(관리자) 분리는 Controller에서 나눈다.
 */
public interface QnaService {

	List<EgovMap> selectQnaList(EgovMap egovMap) throws Exception;

	int selectQnaListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectQnaDetail(String boardId) throws Exception;

	EgovMap selectQnaAnswer(String boardId) throws Exception;

	void insertQna(EgovMap egovMap) throws Exception;

	void updateQna(EgovMap egovMap) throws Exception;

	void deleteQna(String boardId) throws Exception;

	void saveQnaAnswer(EgovMap egovMap) throws Exception;
}
