package arkive.admin.totp.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.totp.service.TotpService;
import arkive.support.ArkiveIntegrationTest;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;

/**
 * TOTP 비밀키 암호화, 설정 확인, 복구코드 1회 사용, 관리자 초기화를 검증한다.
 */
@ArkiveIntegrationTest
@Transactional
class TotpServiceTest {

	@Autowired
	private TotpService totpService;

	@Autowired
	private TotpMapper totpMapper;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private void insertUser(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", passwordEncoder.encode("TotpPass123!"));
		user.put("userNm", "TOTP Tester");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}

	private String currentCode(String secret) throws Exception {
		DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
		long timeStep = Math.floorDiv(System.currentTimeMillis() / 1000L, 30L);
		return generator.generate(secret, timeStep);
	}

	@Test
	void setupShouldEncryptSecretAndEnableAfterCodeConfirmation() throws Exception {
		insertUser("totp01");

		Map<String, Object> setup = totpService.beginSetup("totp01");
		String secret = String.valueOf(setup.get("secret"));
		assertThat(secret).isNotBlank();
		assertThat(String.valueOf(setup.get("qrImage"))).startsWith("data:image/png;base64,");
		assertThat(totpService.isEnabled("totp01")).isFalse();

		EgovMap stored = totpMapper.selectTotp("totp01");
		assertThat(String.valueOf(stored.get("secretEnc"))).startsWith("enc:v1:").doesNotContain(secret);

		List<String> recoveryCodes = totpService.confirmSetup("totp01", currentCode(secret));
		assertThat(recoveryCodes).hasSize(8);
		assertThat(totpService.isEnabled("totp01")).isTrue();
		assertThat(totpService.verify("totp01", currentCode(secret))).isTrue();
		assertThat(totpService.verify("totp01", "!!!!!!")).isFalse();
		assertThat(totpService.countUnusedRecoveryCodes("totp01")).isEqualTo(8);
	}

	@Test
	void recoveryCodeShouldBeUsableOnlyOnce() throws Exception {
		insertUser("totp02");
		Map<String, Object> setup = totpService.beginSetup("totp02");
		String secret = String.valueOf(setup.get("secret"));
		List<String> recoveryCodes = totpService.confirmSetup("totp02", currentCode(secret));

		assertThat(totpService.verifyRecovery("totp02", recoveryCodes.get(0))).isTrue();
		assertThat(totpService.verifyRecovery("totp02", recoveryCodes.get(0))).isFalse();
		assertThat(totpService.countUnusedRecoveryCodes("totp02")).isEqualTo(7);
	}

	@Test
	void resetShouldRemoveTotpAndRecoveryCodes() throws Exception {
		insertUser("totp03");
		Map<String, Object> setup = totpService.beginSetup("totp03");
		String secret = String.valueOf(setup.get("secret"));
		totpService.confirmSetup("totp03", currentCode(secret));

		totpService.reset("totp03");

		assertThat(totpService.isEnabled("totp03")).isFalse();
		assertThat(totpMapper.selectTotp("totp03")).isNull();
		assertThat(totpService.countUnusedRecoveryCodes("totp03")).isZero();
	}

	@Test
	void confirmShouldRejectWrongCode() throws Exception {
		insertUser("totp04");
		totpService.beginSetup("totp04");

		assertThatThrownBy(() -> totpService.confirmSetup("totp04", "!!!!!!"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("인증 코드");
		assertThat(totpService.isEnabled("totp04")).isFalse();
	}
}
