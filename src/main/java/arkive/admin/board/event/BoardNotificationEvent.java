package arkive.admin.board.event;

/**
 * 게시글 저장 뒤 채널별 알림을 분리하기 위한 이벤트.
 *
 * <p>Controller는 저장 결과만 발행하고, 실제 SSE·메일·SMS 발송은
 * {@link BoardNotificationEventListener}가 담당한다. 채널별 실패가 저장 로직에
 * 번지지 않도록 경계를 둔다.</p>
 */
public record BoardNotificationEvent(
		String boardId,
		String boardType,
		String title,
		boolean notifySse,
		boolean notifyMail,
		boolean notifySms) {

	/**
	 * SMS 채널 도입 전 호출부와의 호환 생성자. 메일과 같은 조건으로 SMS를 보낸다.
	 */
	public BoardNotificationEvent(String boardId, String boardType, String title, boolean notifySse, boolean notifyMail) {
		this(boardId, boardType, title, notifySse, notifyMail, notifyMail);
	}
}
