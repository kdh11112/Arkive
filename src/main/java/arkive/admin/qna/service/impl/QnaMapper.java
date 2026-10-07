package arkive.admin.qna.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * Q&A MyBatis 매퍼. SQL은 Qna_SQL.xml에 있다.
 * 질문은 BOARD(BOARD_TYPE='QNA'), 답변은 QNA_ANSWER(1:1)다.
 */
@EgovMapper("qnaMapper")
public interface QnaMapper {

	List<EgovMap> selectQnaList(EgovMap egovMap) throws Exception;

	int selectQnaListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectQnaDetail(String boardId) throws Exception;

	EgovMap selectQnaAnswer(String boardId) throws Exception;

	void insertQna(EgovMap egovMap) throws Exception;

	void updateQna(EgovMap egovMap) throws Exception;

	void deleteQna(String boardId) throws Exception;

	void saveQnaAnswer(EgovMap egovMap) throws Exception;
}
