package arkive.admin.comm.web;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * 분석 도구 설정을 화면 모델에 안전하게 전달한다.
 *
 * <p>GA4·Clarity는 ID가 있고 {@code arkive.analytics.enabled=true}일 때만 활성화한다.
 * 기본값은 꺼짐이며, 동의 정책이 없으면 스크립트를 로드하지 않는다. ID 형식을 검증해
 * 설정값을 통한 스크립트 주입을 막는다.</p>
 */
@Hidden
@ControllerAdvice
public class AnalyticsModelAdvice {

	private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_-]{4,64}$");

	@Value("${arkive.analytics.enabled:false}")
	private boolean enabled;

	@Value("${arkive.analytics.ga4-id:}")
	private String ga4Id;

	@Value("${arkive.analytics.clarity-id:}")
	private String clarityId;

	@Value("${arkive.analytics.require-consent:true}")
	private boolean requireConsent;

	@Value("${arkive.analytics.consent-version:1}")
	private String consentVersion;

	@ModelAttribute("analyticsEnabled")
	public boolean analyticsEnabled() {
		return enabled && (!safe(ga4Id).isEmpty() || !safe(clarityId).isEmpty());
	}

	@ModelAttribute("analyticsConfig")
	public Map<String, Object> analyticsConfig() {
		return Map.of(
				"ga4Id", safe(ga4Id),
				"clarityId", safe(clarityId),
				"requireConsent", requireConsent,
				"consentVersion", safeConsentVersion());
	}

	private String safe(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		return SAFE_ID.matcher(trimmed).matches() ? trimmed : "";
	}

	private String safeConsentVersion() {
		if (consentVersion == null) {
			return "1";
		}
		String trimmed = consentVersion.trim();
		return trimmed.matches("^[A-Za-z0-9._-]{1,20}$") ? trimmed : "1";
	}
}
