package arkive.admin.sms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import arkive.admin.comm.service.ExternalHttpClient;
import arkive.admin.sms.service.SmsSendResult;

/**
 * Aligo 어댑터의 설정 확인, 번호 검증, 응답 코드 해석, 오류 시 정보 비노출을 확인한다.
 * 실제 외부 API는 호출하지 않고 HTTP 클라이언트를 목으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class AligoSmsSenderTest {

	@Mock
	private ExternalHttpClient externalHttpClient;

	@InjectMocks
	private AligoSmsSender sender;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(sender, "enabled", true);
		ReflectionTestUtils.setField(sender, "baseUrl", "https://sms.example");
		ReflectionTestUtils.setField(sender, "id", "test-id");
		ReflectionTestUtils.setField(sender, "apiKey", "test-secret");
		ReflectionTestUtils.setField(sender, "sender", "02-1234-5678");
		ReflectionTestUtils.setField(sender, "testMode", false);
	}

	@Test
	void missingCredentialsShouldBeDisabledWithoutHttpCall() {
		ReflectionTestUtils.setField(sender, "apiKey", "");

		SmsSendResult result = sender.send("010-1234-5678", "제목", "내용");

		assertThat(sender.isReady()).isFalse();
		assertThat(result.success()).isFalse();
		assertThat(result.resultCode()).isEqualTo("DISABLED");
		verifyNoInteractions(externalHttpClient);
	}

	@Test
	void invalidRecipientShouldFailWithoutHttpCall() {
		SmsSendResult result = sender.send("02-123-4567", "제목", "내용");

		assertThat(result.success()).isFalse();
		assertThat(result.resultCode()).isEqualTo("INVALID");
		assertThat(result.message()).contains("휴대전화");
		verifyNoInteractions(externalHttpClient);
	}

	@Test
	void blankMessageShouldFailWithoutHttpCall() {
		SmsSendResult result = sender.send("010-1234-5678", "제목", " ");

		assertThat(result.success()).isFalse();
		assertThat(result.resultCode()).isEqualTo("INVALID");
		verifyNoInteractions(externalHttpClient);
	}

	@Test
	void successResponseShouldPostNormalizedForm() {
		when(externalHttpClient.postForm(eq("https://sms.example/send/"), any()))
				.thenReturn("{\"result_code\":\"1\",\"message\":\"OK\"}");

		SmsSendResult result = sender.send("010-1234-5678", "공지", "짧은 내용");

		assertThat(result.success()).isTrue();
		assertThat(result.provider()).isEqualTo("aligo");
		assertThat(result.resultCode()).isEqualTo("1");
		MultiValueMap<String, String> form = capturedForm();
		assertThat(form.getFirst("receiver")).isEqualTo("01012345678");
		assertThat(form.getFirst("sender")).isEqualTo("0212345678");
		assertThat(form.getFirst("msg_type")).isEqualTo("SMS");
		assertThat(form.getFirst("testmode_yn")).isEqualTo("N");
	}

	@Test
	void longMessageShouldUseAndTruncateLms() {
		when(externalHttpClient.postForm(anyString(), any()))
				.thenReturn("{\"result_code\":\"1\"}");
		String message = "가".repeat(700);

		SmsSendResult result = sender.send("010-1234-5678", "제목", message);

		assertThat(result.success()).isTrue();
		String sent = capturedForm().getFirst("msg");
		assertThat(capturedForm().getFirst("msg_type")).isEqualTo("LMS");
		assertThat(sent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1900);
	}

	@Test
	void providerFailureShouldReturnCodeAndMessage() {
		when(externalHttpClient.postForm(anyString(), any()))
				.thenReturn("{\"result_code\":\"-99\",\"message\":\"잔액 부족\"}");

		SmsSendResult result = sender.send("010-1234-5678", "제목", "내용");

		assertThat(result.success()).isFalse();
		assertThat(result.resultCode()).isEqualTo("-99");
		assertThat(result.message()).isEqualTo("잔액 부족");
	}

	@Test
	void httpFailureShouldReturnGenericMessageWithoutSecret() {
		when(externalHttpClient.postForm(anyString(), any()))
				.thenThrow(new RuntimeException("connection failed: test-secret"));

		SmsSendResult result = sender.send("010-1234-5678", "제목", "내용");

		assertThat(result.success()).isFalse();
		assertThat(result.resultCode()).isEqualTo("ERROR");
		assertThat(result.message()).doesNotContain("test-secret").doesNotContain("connection failed");
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> capturedForm() {
		ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
		verify(externalHttpClient).postForm(anyString(), captor.capture());
		return captor.getValue();
	}
}
