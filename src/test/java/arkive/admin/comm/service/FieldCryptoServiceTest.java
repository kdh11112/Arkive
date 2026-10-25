package arkive.admin.comm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * AES-GCM 필드 암호화의 왕복, 평문 호환, 키 미설정·오류 처리를 확인한다.
 */
class FieldCryptoServiceTest {

	private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

	@Test
	void encryptShouldProduceVersionedCiphertextAndRoundTrip() {
		FieldCryptoService service = new FieldCryptoService(KEY);

		String encrypted = service.encrypt("010-1234-5678");

		assertThat(encrypted).startsWith("enc:v1:").doesNotContain("010-1234-5678");
		assertThat(service.decrypt(encrypted)).isEqualTo("010-1234-5678");
		assertThat(service.encrypt("010-1234-5678")).isNotEqualTo(encrypted);
	}

	@Test
	void decryptShouldReturnPlaintextWithoutPrefixUnchanged() {
		FieldCryptoService service = new FieldCryptoService(KEY);

		assertThat(service.decrypt("plain@example.com")).isEqualTo("plain@example.com");
		assertThat(service.decrypt(null)).isNull();
	}

	@Test
	void missingKeyShouldRejectEncryptButStillReadPlaintext() {
		FieldCryptoService service = new FieldCryptoService("");

		assertThat(service.isAvailable()).isFalse();
		assertThat(service.decrypt("010-1234-5678")).isEqualTo("010-1234-5678");
		assertThatThrownBy(() -> service.encrypt("010-1234-5678"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ARKIVE_FIELD_KEY");
	}

	@Test
	void differentKeyShouldFailDecrypt() {
		String encrypted = new FieldCryptoService(KEY).encrypt("secret");
		FieldCryptoService other = new FieldCryptoService(
				Base64.getEncoder().encodeToString("another-32-byte-key-for-testing!!".getBytes()));

		assertThatThrownBy(() -> other.decrypt(encrypted))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("복호화");
	}

	@Test
	void malformedCiphertextShouldFailWithoutLeakingValue() {
		FieldCryptoService service = new FieldCryptoService(KEY);

		assertThatThrownBy(() -> service.decrypt("enc:v1:not-base64!"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("복호화")
				.hasMessageNotContaining("not-base64");
	}

	@Test
	void nonStandardKeyLengthShouldBeDerivedAndRemainUsable() {
		FieldCryptoService service = new FieldCryptoService("short-key");

		assertThat(service.isAvailable()).isTrue();
		assertThat(service.decrypt(service.encrypt("value"))).isEqualTo("value");
	}
}
