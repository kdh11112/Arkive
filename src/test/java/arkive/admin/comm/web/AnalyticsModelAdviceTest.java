package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * GA4·Clarity 설정의 기본 비활성, ID 형식 검증, 동의 설정 전달을 확인한다.
 */
class AnalyticsModelAdviceTest {

	@Test
	void analyticsShouldBeDisabledByDefault() {
		AnalyticsModelAdvice advice = advice(false, "", "", true, "1");

		assertThat(advice.analyticsEnabled()).isFalse();
		Map<String, Object> config = advice.analyticsConfig();
		assertThat(config).containsEntry("ga4Id", "").containsEntry("clarityId", "")
				.containsEntry("requireConsent", true).containsEntry("consentVersion", "1");
	}

	@Test
	void invalidIdsShouldNotBeExposedOrEnabled() {
		AnalyticsModelAdvice advice = advice(true, "G-ABC123\"><script>", "bad id", true, "1");

		assertThat(advice.analyticsEnabled()).isFalse();
		assertThat(advice.analyticsConfig()).containsEntry("ga4Id", "").containsEntry("clarityId", "");
	}

	@Test
	void validIdsShouldEnableAnalyticsAndPassConsentPolicy() {
		AnalyticsModelAdvice advice = advice(true, "G-ABC123", "clarity_01", false, "2");

		assertThat(advice.analyticsEnabled()).isTrue();
		assertThat(advice.analyticsConfig())
				.containsEntry("ga4Id", "G-ABC123")
				.containsEntry("clarityId", "clarity_01")
				.containsEntry("requireConsent", false)
				.containsEntry("consentVersion", "2");
	}

	@Test
	void onlyOneValidIdShouldStillEnableAnalytics() {
		AnalyticsModelAdvice advice = advice(true, "", "clarity01", true, "1");

		assertThat(advice.analyticsEnabled()).isTrue();
	}

	@Test
	void invalidConsentVersionShouldFallBackToDefault() {
		AnalyticsModelAdvice advice = advice(true, "G-ABC123", "", true, "bad version!");

		assertThat(advice.analyticsConfig()).containsEntry("consentVersion", "1");
	}

	private AnalyticsModelAdvice advice(boolean enabled, String ga4Id, String clarityId,
			boolean requireConsent, String consentVersion) {
		AnalyticsModelAdvice advice = new AnalyticsModelAdvice();
		ReflectionTestUtils.setField(advice, "enabled", enabled);
		ReflectionTestUtils.setField(advice, "ga4Id", ga4Id);
		ReflectionTestUtils.setField(advice, "clarityId", clarityId);
		ReflectionTestUtils.setField(advice, "requireConsent", requireConsent);
		ReflectionTestUtils.setField(advice, "consentVersion", consentVersion);
		return advice;
	}
}
