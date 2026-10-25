package arkive.admin.board.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 게시판 MyBatis 매퍼. SQL은 Board_SQL.xml에 있다.
 */
@EgovMapper("boardMapper")
public interface BoardMapper {

	List<EgovMap> selectBoardList(EgovMap egovMap) throws Exception;

	int selectBoardListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectBoardListByCursor(EgovMap egovMap) throws Exception;

	EgovMap selectBoardDetail(String boardId) throws Exception;

	EgovMap selectPrevOne(EgovMap egovMap) throws Exception;

	EgovMap selectNextOne(EgovMap egovMap) throws Exception;

	void insertBoard(EgovMap egovMap) throws Exception;

	void updateBoard(EgovMap egovMap) throws Exception;

	void deleteBoard(String boardId) throws Exception;

	void updateViewCnt(String boardId) throws Exception;

	int updateNoticeYn(EgovMap egovMap) throws Exception;

	int claimBoardMail(String boardId) throws Exception;

	int claimBoardSms(String boardId) throws Exception;

	List<EgovMap> selectCommentList(String boardId) throws Exception;

	int selectCommentCount(String boardId) throws Exception;

	EgovMap selectCommentDetail(String commentId) throws Exception;

	void insertComment(EgovMap egovMap) throws Exception;

	void deleteComment(String commentId) throws Exception;
}
