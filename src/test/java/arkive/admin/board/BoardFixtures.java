package arkive.admin.board;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 게시판 기능 테스트 데이터.
 * 공통 support가 아니라 게시판 기능과 함께 복사한다.
 * 대상 프로젝트에서는 테이블·컬럼·PK 형식에 맞게 수정한다.
 */
public final class BoardFixtures {

	private BoardFixtures() {
	}

	public static EgovMap board(String boardId, String boardType, String title, String content) {
		EgovMap board = new EgovMap();
		board.put("boardId", boardId);
		board.put("boardType", boardType);
		board.put("title", title);
		board.put("content", content);
		board.put("atchFileGrpid", null);
		board.put("register", "TEST");
		return board;
	}

	public static EgovMap comment(String commentId, String boardId, String content) {
		EgovMap comment = new EgovMap();
		comment.put("commentId", commentId);
		comment.put("boardId", boardId);
		comment.put("content", content);
		comment.put("register", "TEST");
		comment.put("registerNm", "Tester");
		return comment;
	}
}
