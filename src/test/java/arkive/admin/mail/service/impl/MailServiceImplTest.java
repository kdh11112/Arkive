package arkive.admin.mail.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.board.service.impl.BoardMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * 메일 발송 조건과 1회 발송 규칙을 SMTP 연결 없이 검증한다.
 * {@link JavaMailSender}와 Mapper를 목으로 대체하고, 실제 MimeMessage의 수신자·제목·본문만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private AuthMapper authMapper;

	@Mock
	private BoardMapper boardMapper;

	@Mock
	private PermissionMapper permissionMapper;

	@InjectMocks
	private MailServiceImpl mailService;

	private final List<MimeMessage> sent = new ArrayList<>();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(mailService, "enabled", true);
		ReflectionTestUtils.setField(mailService, "from", "noreply@example.com");
		ReflectionTestUtils.setField(mailService, "baseUrl", "http://localhost:8080/");
		sent.clear();
	}

	private void stubCreateMessage() throws Exception {
		when(mailSender.createMimeMessage())
				.thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}

	private void stubSender() throws Exception {
		stubCreateMessage();
		doAnswer(invocation -> {
			sent.add(invocation.getArgument(0));
			return null;
		}).when(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void disabledMailShouldNotQueryUser() throws Exception {
		ReflectionTestUtils.setField(mailService, "enabled", false);

		mailService.sendWelcome("user1");

		verifyNoInteractions(authMapper, mailSender);
	}

	@Test
	void emptyFromShouldNotQueryUser() throws Exception {
		ReflectionTestUtils.setField(mailService, "from", " ");

		mailService.sendWelcome("user1");

		verifyNoInteractions(authMapper, mailSender);
	}

	@Test
	void welcomeShouldSkipUserWithoutConsent() throws Exception {
		when(authMapper.selectUserDetail("user1"))
				.thenReturn(user("user1@example.com", "N", "Y", "User One"));

		mailService.sendWelcome("user1");

		verifyNoInteractions(mailSender);
	}

	@Test
	void welcomeShouldSendHtmlMailForConsentedUser() throws Exception {
		stubSender();
		when(authMapper.selectUserDetail("user1"))
				.thenReturn(user("user1@example.com", "Y", "Y", "<b>User One</b>"));

		mailService.sendWelcome("user1");

		assertThat(sent).hasSize(1);
		MimeMessage message = sent.get(0);
		assertThat(message.getSubject()).isEqualTo("[Arkive] 가입을 환영합니다");
		assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user1@example.com");
		assertThat(body(message))
				.contains("&lt;b&gt;User One&lt;/b&gt;")
				.contains("http://localhost:8080/login/login");
	}

	@Test
	void passwordResetShouldIgnoreMarketingConsent() throws Exception {
		stubSender();
		when(authMapper.selectUserDetail("user1"))
				.thenReturn(user("user1@example.com", "N", "Y", "User One"));

		mailService.sendPasswordReset("user1", "a+b/c");

		assertThat(sent).hasSize(1);
		assertThat(sent.get(0).getSubject()).isEqualTo("[Arkive] 비밀번호 재설정 안내");
		assertThat(body(sent.get(0)))
				.contains("http://localhost:8080/login/resetPw?token=a%2Bb%2Fc")
				.contains("30분 동안 한 번만");
	}

	@Test
	void boardNotificationShouldSendOnlyValidRecipients() throws Exception {
		stubSender();
		when(boardMapper.selectBoardDetail("B1"))
				.thenReturn(board("Y", "N", "CK", "<script>alert(1)</script>"));
		when(boardMapper.claimBoardMail("B1")).thenReturn(1);
		when(permissionMapper.selectBoardMailRecipients("ME02030100"))
				.thenReturn(List.of(recipient("one@example.com"), recipient("not-an-email")));

		mailService.sendBoardNotification("B1");

		verify(boardMapper).claimBoardMail("B1");
		assertThat(sent).hasSize(1);
		assertThat(sent.get(0).getSubject()).isEqualTo("[Arkive 공지] <script>alert(1)</script>");
		assertThat(body(sent.get(0)))
				.contains("&lt;script&gt;alert(1)&lt;/script&gt;")
				.contains("http://localhost:8080/user/board/noticeDetail?boardId=B1");
	}

	@Test
	void boardNotificationShouldSkipNormalBoard() throws Exception {
		when(boardMapper.selectBoardDetail("B2")).thenReturn(board("N", "N", "CK", "normal"));

		mailService.sendBoardNotification("B2");

		verify(boardMapper, never()).claimBoardMail("B2");
		verifyNoInteractions(mailSender);
	}

	@Test
	void boardNotificationShouldNotSendTwice() throws Exception {
		when(boardMapper.selectBoardDetail("B3")).thenReturn(board("Y", "N", "CK", "notice"));
		when(boardMapper.claimBoardMail("B3")).thenReturn(0);

		mailService.sendBoardNotification("B3");

		verify(permissionMapper, never()).selectBoardMailRecipients(any());
		verifyNoInteractions(mailSender);
	}

	@Test
	void boardNotificationShouldClaimButSkipWithoutRecipients() throws Exception {
		when(boardMapper.selectBoardDetail("B4")).thenReturn(board("N", "Y", "TOAST", "important"));
		when(boardMapper.claimBoardMail("B4")).thenReturn(1);
		when(permissionMapper.selectBoardMailRecipients("ME02030200")).thenReturn(List.of());

		mailService.sendBoardNotification("B4");

		verify(boardMapper).claimBoardMail("B4");
		verifyNoInteractions(mailSender);
	}

	@Test
	void sendFailureShouldNotBubbleUp() throws Exception {
		stubCreateMessage();
		doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));
		when(authMapper.selectUserDetail("user1"))
				.thenReturn(user("user1@example.com", "Y", "Y", "User One"));

		assertThatCode(() -> mailService.sendWelcome("user1")).doesNotThrowAnyException();
		assertThat(sent).isEmpty();
	}

	private EgovMap user(String email, String mailYn, String useYn, String userNm) {
		EgovMap user = new EgovMap();
		user.put("email", email);
		user.put("mailYn", mailYn);
		user.put("useYn", useYn);
		user.put("userNm", userNm);
		return user;
	}

	private EgovMap board(String noticeYn, String importantYn, String boardType, String title) {
		EgovMap board = new EgovMap();
		board.put("noticeYn", noticeYn);
		board.put("importantYn", importantYn);
		board.put("boardType", boardType);
		board.put("title", title);
		return board;
	}

	private EgovMap recipient(String email) {
		EgovMap recipient = new EgovMap();
		recipient.put("email", email);
		return recipient;
	}

	private String body(MimeMessage message) throws Exception {
		Object content = message.getContent();
		return content == null ? "" : content.toString();
	}
}
