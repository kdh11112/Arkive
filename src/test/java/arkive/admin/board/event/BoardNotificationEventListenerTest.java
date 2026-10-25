package arkive.admin.board.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import arkive.admin.auth.service.PermissionService;
import arkive.admin.board.service.BoardSseService;
import arkive.admin.mail.service.MailAsyncService;
import arkive.admin.sms.service.SmsAsyncService;

/**
 * 게시글 알림 이벤트가 SSE·메일·SMS 채널로만 분기되고, 한 채널의 실패가 다른 채널이나
 * 저장 요청으로 번지지 않는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class BoardNotificationEventListenerTest {

	@Mock
	private PermissionService permissionService;

	@Mock
	private BoardSseService boardSseService;

	@Mock
	private MailAsyncService mailAsyncService;

	@Mock
	private SmsAsyncService smsAsyncService;

	@InjectMocks
	private BoardNotificationEventListener listener;

	@Test
	void ckSseShouldUseNoticeMenuRoles() throws Exception {
		when(permissionService.selectRoleIdsWithMenuRead("ME02030100"))
				.thenReturn(Set.of("ROLE_USER", "ROLE_MANAGER"));

		listener.handleSse(event("CK", true, false));

		verify(boardSseService).broadcast("CK", "B1", "title", Set.of("ROLE_USER", "ROLE_MANAGER"));
		verifyNoInteractions(mailAsyncService);
	}

	@Test
	void toastSseShouldUseArchiveMenuRoles() throws Exception {
		when(permissionService.selectRoleIdsWithMenuRead("ME02030200"))
				.thenReturn(Set.of("ROLE_USER"));

		listener.handleSse(event("TOAST", true, false));

		verify(boardSseService).broadcast("TOAST", "B1", "title", Set.of("ROLE_USER"));
	}

	@Test
	void sseShouldBeSkippedWhenNotRequested() throws Exception {
		listener.handleSse(event("CK", false, true));

		verifyNoInteractions(permissionService, boardSseService);
		verifyNoInteractions(mailAsyncService);
	}

	@Test
	void sseFailureShouldNotEscape() throws Exception {
		when(permissionService.selectRoleIdsWithMenuRead("ME02030100"))
				.thenThrow(new RuntimeException("db down"));

		assertThatCode(() -> listener.handleSse(event("CK", true, false)))
				.doesNotThrowAnyException();

		verifyNoInteractions(boardSseService);
	}

	@Test
	void unknownBoardTypeShouldNotBroadcast() throws Exception {
		assertThatCode(() -> listener.handleSse(event("UNKNOWN", true, false)))
				.doesNotThrowAnyException();

		verifyNoInteractions(permissionService, boardSseService);
	}

	@Test
	void mailShouldDispatchOnlyWhenRequested() throws Exception {
		listener.handleMail(event("CK", true, true));

		verify(mailAsyncService).sendBoardNotification("B1");
		verifyNoInteractions(permissionService, boardSseService);
	}

	@Test
	void mailShouldBeSkippedWhenNotRequested() throws Exception {
		listener.handleMail(event("CK", false, false));

		verifyNoInteractions(mailAsyncService);
	}

	@Test
	void mailFailureShouldNotEscape() throws Exception {
		doThrow(new RuntimeException("smtp down"))
				.when(mailAsyncService).sendBoardNotification("B1");

		assertThatCode(() -> listener.handleMail(event("CK", false, true)))
				.doesNotThrowAnyException();

		verify(mailAsyncService).sendBoardNotification("B1");
	}

	@Test
	void sseAndMailRequestsShouldNotAffectEachOther() throws Exception {
		when(permissionService.selectRoleIdsWithMenuRead("ME02030100"))
				.thenReturn(Set.of("ROLE_USER"));

		listener.handleSse(event("CK", true, true));
		listener.handleMail(event("CK", true, true));

		verify(boardSseService).broadcast("CK", "B1", "title", Set.of("ROLE_USER"));
		verify(mailAsyncService).sendBoardNotification("B1");
	}

	@Test
	void smsShouldDispatchOnlyWhenRequested() throws Exception {
		listener.handleSms(event("CK", false, false, true));

		verify(smsAsyncService).sendBoardNotification("B1");
		verifyNoInteractions(permissionService, boardSseService, mailAsyncService);
	}

	@Test
	void smsShouldBeSkippedWhenNotRequested() throws Exception {
		listener.handleSms(event("CK", true, true, false));

		verifyNoInteractions(smsAsyncService);
	}

	@Test
	void smsFailureShouldNotEscape() throws Exception {
		doThrow(new RuntimeException("sms down"))
				.when(smsAsyncService).sendBoardNotification("B1");

		assertThatCode(() -> listener.handleSms(event("CK", false, false, true)))
				.doesNotThrowAnyException();

		verify(smsAsyncService).sendBoardNotification("B1");
	}

	@Test
	void mailFailureShouldNotBlockSms() throws Exception {
		doThrow(new RuntimeException("smtp down"))
				.when(mailAsyncService).sendBoardNotification("B1");

		listener.handleMail(event("CK", false, true, true));
		listener.handleSms(event("CK", false, true, true));

		verify(mailAsyncService).sendBoardNotification("B1");
		verify(smsAsyncService).sendBoardNotification("B1");
	}

	private BoardNotificationEvent event(String boardType, boolean notifySse, boolean notifyMail) {
		return new BoardNotificationEvent("B1", boardType, "title", notifySse, notifyMail);
	}

	private BoardNotificationEvent event(String boardType, boolean notifySse, boolean notifyMail, boolean notifySms) {
		return new BoardNotificationEvent("B1", boardType, "title", notifySse, notifyMail, notifySms);
	}
}
