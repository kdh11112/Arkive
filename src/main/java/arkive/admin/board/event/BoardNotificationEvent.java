package arkive.admin.board.event;

/**
 * 게시글 저장 뒤 채널별 알림을 분리하기 위한 이벤트.
 *
 * <p>Controller는 저장 결과만 발행하고, 실제 SSE·메일 발송은
 * {@link BoardNotificationEventListener}가 담당한다. 이후 SMS 등 채널을
 * 추가할 때 Controller의 저장 로직을 다시 바꾸지 않기 위한 경계다.</p>
 */
public record BoardNotificationEvent(
		String boardId,
		String boardType,
		String title,
		boolean notifySse,
		boolean notifyMail) {
}
