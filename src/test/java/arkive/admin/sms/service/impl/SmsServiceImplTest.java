package arkive.admin.sms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.board.service.BoardService;
import arkive.admin.comm.service.PiiFieldService;
import arkive.admin.sms.service.SmsSendResult;
import arkive.admin.sms.service.SmsSender;

/**
 * 게시판 SMS 알림의 1회 발송 선점, 메뉴 권한 수신자 조회, 번호 오류 집계를 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

	@Mock
	private SmsSender smsSender;

	@Mock
	private BoardService boardService;

	@Mock
	private PermissionMapper permissionMapper;

	@Mock
	private PiiFieldService piiFieldService;

	@InjectMocks
	private SmsServiceImpl smsService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(smsService, "titlePrefix", "[Arkive] ");
	}

	@Test
	void disabledSenderShouldNotQueryBoard() throws Exception {
		when(smsSender.isReady()).thenReturn(false);

		assertThat(smsService.isReady()).isFalse();
		assertThat(smsService.sendBoardNotification("B1")).isZero();
		verifyNoInteractions(boardService, permissionMapper, piiFieldService);
	}

	@Test
	void missingBoardOrUnsupportedTypeShouldNotClaim() throws Exception {
		when(smsSender.isReady()).thenReturn(true);
		when(boardService.selectBoardDetail("NONE")).thenReturn(null);
		EgovMap faq = new EgovMap();
		faq.put("boardType", "FAQ");
		faq.put("title", "FAQ 글");
		when(boardService.selectBoardDetail("FAQ-1")).thenReturn(faq);

		assertThat(smsService.sendBoardNotification("NONE")).isZero();
		assertThat(smsService.sendBoardNotification("FAQ-1")).isZero();
		verify(boardService, never()).claimBoardSms(anyString());
	}

	@Test
	void alreadyClaimedBoardShouldNotSend() throws Exception {
		when(smsSender.isReady()).thenReturn(true);
		when(boardService.selectBoardDetail("B2")).thenReturn(board("CK", "이미 발송"));
		when(boardService.claimBoardSms("B2")).thenReturn(0);

		assertThat(smsService.sendBoardNotification("B2")).isZero();
		verifyNoInteractions(permissionMapper, piiFieldService);
	}

	@Test
	void shouldSendToValidRecipientsAndSkipInvalidPhone() throws Exception {
		when(smsSender.isReady()).thenReturn(true);
		when(boardService.selectBoardDetail("B3")).thenReturn(board("CK", "새 공지"));
		when(boardService.claimBoardSms("B3")).thenReturn(1);
		when(permissionMapper.selectBoardSmsRecipients("ME02030100"))
				.thenReturn(List.of(recipient("010-1111-2222"), recipient(null), recipient("02-123-4567")));
		when(smsSender.send("01011112222", "새 공지", "[Arkive] 새 공지"))
				.thenReturn(SmsSendResult.success("aligo", "1"));

		int sent = smsService.sendBoardNotification("B3");

		assertThat(sent).isEqualTo(1);
		verify(piiFieldService, org.mockito.Mockito.times(3)).decryptUserPhone(any(EgovMap.class));
		verify(smsSender).send("01011112222", "새 공지", "[Arkive] 새 공지");
	}

	@Test
	void toastBoardShouldUseToastMenu() throws Exception {
		when(smsSender.isReady()).thenReturn(true);
		when(boardService.selectBoardDetail("B4")).thenReturn(board("TOAST", "토스트 공지"));
		when(boardService.claimBoardSms("B4")).thenReturn(1);
		when(permissionMapper.selectBoardSmsRecipients("ME02030200")).thenReturn(List.of());

		assertThat(smsService.sendBoardNotification("B4")).isZero();
		verify(permissionMapper).selectBoardSmsRecipients("ME02030200");
		verify(smsSender, never()).send(anyString(), anyString(), anyString());
	}

	private EgovMap board(String boardType, String title) {
		EgovMap detail = new EgovMap();
		detail.put("boardType", boardType);
		detail.put("title", title);
		return detail;
	}

	private EgovMap recipient(String phone) {
		EgovMap row = new EgovMap();
		row.put("phone", phone);
		return row;
	}
}
