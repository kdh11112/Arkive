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

	int countContentRef(String fileId) throws Exception;

	/** 조회수 통계용 1건 기록. */
	void insertViewLog(EgovMap egovMap) throws Exception;

	/** 기준일 이후 일자별 조회수. */
	List<EgovMap> selectDailyViewStats(EgovMap egovMap) throws Exception;

	/** 게시글별 누적 조회수 상위 목록. */
	List<EgovMap> selectBoardViewRank(EgovMap egovMap) throws Exception;

	int updateNoticeYn(EgovMap egovMap) throws Exception;

	int claimBoardMail(String boardId) throws Exception;

	int claimBoardSms(String boardId) throws Exception;

	List<EgovMap> selectCommentList(String boardId) throws Exception;

	int selectCommentCount(String boardId) throws Exception;

	EgovMap selectCommentDetail(String commentId) throws Exception;

	void insertComment(EgovMap egovMap) throws Exception;

	void deleteComment(String commentId) throws Exception;
}
