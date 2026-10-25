package arkive.admin.search.event;

/**
 * 게시글 변경 후 검색 색인을 갱신하기 위한 이벤트.
 */
public record SearchIndexEvent(String boardId, boolean delete) {

	public static SearchIndexEvent index(String boardId) {
		return new SearchIndexEvent(boardId, false);
	}

	public static SearchIndexEvent delete(String boardId) {
		return new SearchIndexEvent(boardId, true);
	}
}
