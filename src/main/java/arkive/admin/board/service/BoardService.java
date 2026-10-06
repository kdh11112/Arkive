package arkive.admin.board.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 게시판 서비스. BOARD_TYPE(CK/TOAST)으로 두 게시판을 분리 조회한다.
 * 목록은 오프셋 페이징(selectBoardList)과 커서 페이징(selectBoardListByCursor) 2종을 제공한다.
 */
public interface BoardService {

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
}
