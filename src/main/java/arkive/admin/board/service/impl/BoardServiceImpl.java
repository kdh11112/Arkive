package arkive.admin.board.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.board.service.BoardService;
import jakarta.annotation.Resource;

/**
 * 게시판 서비스 구현. 상세 조회 시 HSQLDB CLOB을 문자열로 변환한다.
 */
@Service("boardService")
public class BoardServiceImpl extends EgovAbstractServiceImpl implements BoardService {

	@Resource(name = "boardMapper")
	private BoardMapper boardMapper;

	@Override
	public List<EgovMap> selectBoardList(EgovMap egovMap) throws Exception {
		return boardMapper.selectBoardList(egovMap);
	}

	@Override
	public int selectBoardListTotCnt(EgovMap egovMap) throws Exception {
		return boardMapper.selectBoardListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectBoardListByCursor(EgovMap egovMap) throws Exception {
		return boardMapper.selectBoardListByCursor(egovMap);
	}

	@Override
	public EgovMap selectBoardDetail(String boardId) throws Exception {
		EgovMap detail = boardMapper.selectBoardDetail(boardId);
		if (detail != null) {
			// HSQLDB CLOB은 EgovMap에 JDBCClob 객체로 담기므로 문자열로 변환한다. (DB 이식성 유지)
			Object content = detail.get("content");
			if (content instanceof java.sql.Clob clob) {
				try (java.io.Reader reader = clob.getCharacterStream()) {
					StringBuilder sb = new StringBuilder();
					char[] buf = new char[8192];
					int len;
					while ((len = reader.read(buf)) != -1) {
						sb.append(buf, 0, len);
					}
					detail.put("content", sb.toString());
				}
			}
		}
		return detail;
	}

	@Override
	public void insertBoard(EgovMap egovMap) throws Exception {
		boardMapper.insertBoard(egovMap);
	}

	@Override
	public void updateBoard(EgovMap egovMap) throws Exception {
		boardMapper.updateBoard(egovMap);
	}

	@Override
	public void deleteBoard(String boardId) throws Exception {
		boardMapper.deleteBoard(boardId);
	}

	@Override
	public void updateViewCnt(String boardId) throws Exception {
		boardMapper.updateViewCnt(boardId);
	}

	@Override
	public EgovMap selectPrevOne(EgovMap egovMap) throws Exception {
		return boardMapper.selectPrevOne(egovMap);
	}

	@Override
	public EgovMap selectNextOne(EgovMap egovMap) throws Exception {
		return boardMapper.selectNextOne(egovMap);
	}
}
