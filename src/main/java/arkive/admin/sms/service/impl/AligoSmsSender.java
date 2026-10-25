package arkive.admin.sms.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.comm.service.ExternalHttpClient;
import arkive.admin.comm.web.PhoneNumberUtil;
import arkive.admin.sms.service.SmsSendResult;
import arkive.admin.sms.service.SmsSender;
import jakarta.annotation.Resource;

/**
 * Aligo SMS/LMS 어댑터.
 *
 * <p>{@code arkive.sms.enabled=false}가 기본이고, 아이디·API 키·발신번호가 모두 있어야
 * 발송한다. 자격증명은 환경변수로 주입하며 저장소에 커밋하지 않는다.</p>
 *
 * <p>제공자 응답 원문과 수신번호는 로그에 남기지 않는다. 인증키가 쿼리로 노출되지 않도록
 * form POST를 사용한다.</p>
 */
@Service("smsSender")
public class AligoSmsSender implements SmsSender {

	private static final String PROVIDER = "aligo";

	@Value("${arkive.sms.enabled:false}")
	private boolean enabled;

	@Value("${arkive.sms.aligo.url:https://sipapis.aligo.in}")
	private String baseUrl;

	@Value("${arkive.sms.aligo.id:}")
	private String id;

	@Value("${arkive.sms.aligo.api-key:}")
	private String apiKey;

	@Value("${arkive.sms.aligo.sender:}")
	private String sender;

	@Value("${arkive.sms.aligo.test-mode:false}")
	private boolean testMode;

	@Resource(name = "externalHttpClient")
	private ExternalHttpClient externalHttpClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String provider() {
		return PROVIDER;
	}

	@Override
	public boolean isReady() {
		return enabled && !id.isBlank() && !apiKey.isBlank() && !sender.isBlank();
	}

	@Override
	public SmsSendResult send(String to, String title, String message) {
		if (!isReady()) {
			return SmsSendResult.disabled(PROVIDER);
		}
		String receiver = PhoneNumberUtil.normalizeMobile(to);
		if (receiver == null) {
			return SmsSendResult.invalid(PROVIDER, "휴대전화 번호가 올바르지 않습니다.");
		}
		if (message == null || message.isBlank()) {
			return SmsSendResult.invalid(PROVIDER, "발송할 내용이 없습니다.");
		}
		try {
			String safeMessage = truncateUtf8(message.trim(), 1900);
			String safeTitle = truncateUtf8(title == null ? "" : title.trim(), 40);
			String msgType = safeMessage.getBytes(StandardCharsets.UTF_8).length <= 90 ? "SMS" : "LMS";
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("key", apiKey);
			form.add("user_id", id);
			form.add("sender", PhoneNumberUtil.normalize(sender));
			form.add("receiver", receiver);
			form.add("msg", safeMessage);
			form.add("msg_type", msgType);
			form.add("title", safeTitle);
			form.add("testmode_yn", testMode ? "Y" : "N");
			String response = externalHttpClient.postForm(trimSlash(baseUrl) + "/send/", form);
			JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
			String resultCode = node.path("result_code").asText(node.path("resultCode").asText(""));
			if ("1".equals(resultCode)) {
				return SmsSendResult.success(PROVIDER, resultCode);
			}
			String providerMessage = node.path("message").asText("SMS 발송에 실패했습니다.");
			return SmsSendResult.fail(PROVIDER, resultCode.isEmpty() ? "UNKNOWN" : resultCode, providerMessage);
		} catch (Exception e) {
			// 외부 응답 본문·키는 남기지 않는다.
			return SmsSendResult.fail(PROVIDER, "ERROR", "SMS 발송 중 오류가 발생했습니다.");
		}
	}

	private String trimSlash(String value) {
		if (value == null || value.isBlank()) {
			return "https://sipapis.aligo.in";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private String truncateUtf8(String value, int maxBytes) {
		if (value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
			return value;
		}
		StringBuilder sb = new StringBuilder();
		int bytes = 0;
		for (int i = 0; i < value.length(); i++) {
			String ch = String.valueOf(value.charAt(i));
			int length = ch.getBytes(StandardCharsets.UTF_8).length;
			if (bytes + length > maxBytes) {
				break;
			}
			sb.append(ch);
			bytes += length;
		}
		return sb.toString();
	}
}
