package arkive.admin.board.event;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import arkive.admin.auth.service.PermissionService;
import arkive.admin.board.service.BoardSseService;
import arkive.admin.mail.service.MailAsyncService;
import jakarta.annotation.Resource;

/**
 * 게시글 알림 이벤트를 채널별로 처리한다.
 *
 * <p>각 채널은 자기 예외를 삼켜 본문 저장이나 다른 채널 발송에 영향을 주지 않는다.
 * 트랜잭션이 있으면 커밋 이후에 실행하고, 현재처럼 개별 Mapper 커밋 구조에서는
 * {@code fallbackExecution=true}로 즉시 실행한다.</p>
 */
@Component
public class BoardNotificationEventListener {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "permissionService")
	private PermissionService permissionService;

	@Resource
	private BoardSseService boardSseService;

	@Resource(name = "mailAsyncService")
	private MailAsyncService mailAsyncService;

	@TransactionalEventListener(fallbackExecution = true)
	public void handleSse(BoardNotificationEvent event) {
		if (!event.notifySse()) {
			return;
		}
		try {
			String menuId = menuId(event.boardType());
			Set<String> targetRoles = permissionService.selectRoleIdsWithMenuRead(menuId);
			boardSseService.broadcast(event.boardType(), event.boardId(), event.title(), targetRoles);
		} catch (Exception e) {
			// 알림 실패가 게시글 저장 실패로 번지지 않게 분리한다.
			logger.warn("board SSE broadcast skipped", e);
		}
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void handleMail(BoardNotificationEvent event) {
		if (!event.notifyMail()) {
			return;
		}
		try {
			mailAsyncService.sendBoardNotification(event.boardId());
		} catch (Exception e) {
			// 메일 발송 실패가 게시글 저장 실패로 번지지 않게 분리한다.
			logger.warn("board mail dispatch skipped", e);
		}
	}

	private String menuId(String boardType) {
		if ("CK".equals(boardType)) {
			return "ME02030100";
		}
		if ("TOAST".equals(boardType)) {
			return "ME02030200";
		}
		throw new IllegalArgumentException("지원하지 않는 게시판 타입입니다: " + boardType);
	}
}
