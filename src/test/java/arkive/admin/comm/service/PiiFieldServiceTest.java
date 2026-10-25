package arkive.admin.comm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 주소록·명함의 연락처/이메일과 사용자 휴대전화 암호화 대상을 확인한다.
 */
class PiiFieldServiceTest {

	private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

	private PiiFieldService piiFieldService;

	@BeforeEach
	void setUp() {
		piiFieldService = new PiiFieldService();
		ReflectionTestUtils.setField(piiFieldService, "fieldCryptoService", new FieldCryptoService(KEY));
	}

	@Test
	void encryptShouldProtectContactAndEmailButKeepName() {
		EgovMap row = row();

		piiFieldService.encrypt(row);

		assertThat(String.valueOf(row.get("telNo"))).startsWith("enc:v1:").doesNotContain("010-1234-5678");
		assertThat(String.valueOf(row.get("email"))).startsWith("enc:v1:").doesNotContain("user@example.com");
		assertThat(row.get("name")).isEqualTo("홍길동");
	}

	@Test
	void decryptShouldRestoreEncryptedFields() {
		EgovMap row = row();
		piiFieldService.encrypt(row);

		piiFieldService.decrypt(row);

		assertThat(row.get("telNo")).isEqualTo("010-1234-5678");
		assertThat(row.get("email")).isEqualTo("user@example.com");
	}

	@Test
	void encryptShouldNotDoubleEncryptAlreadyEncryptedValue() {
		EgovMap row = row();
		piiFieldService.encrypt(row);
		String encryptedTel = String.valueOf(row.get("telNo"));

		piiFieldService.encrypt(row);

		assertThat(row.get("telNo")).isEqualTo(encryptedTel);
		assertThat(new FieldCryptoService(KEY).decrypt(encryptedTel)).isEqualTo("010-1234-5678");
	}

	@Test
	void userPhoneOnlyShouldNotEncryptEmail() {
		EgovMap row = new EgovMap();
		row.put("phone", "010-9999-8888");
		row.put("email", "keep@example.com");

		piiFieldService.encryptUserPhone(row);

		assertThat(String.valueOf(row.get("phone"))).startsWith("enc:v1:");
		assertThat(row.get("email")).isEqualTo("keep@example.com");

		piiFieldService.decryptUserPhone(row);
		assertThat(row.get("phone")).isEqualTo("010-9999-8888");
	}

	@Test
	void missingKeyShouldRejectNewPiiButReadPlaintext() {
		PiiFieldService withoutKey = new PiiFieldService();
		ReflectionTestUtils.setField(withoutKey, "fieldCryptoService", new FieldCryptoService(""));

		assertThatThrownBy(() -> withoutKey.encrypt(row())).isInstanceOf(IllegalStateException.class);
		EgovMap plain = row();
		withoutKey.decrypt(plain);
		assertThat(plain.get("telNo")).isEqualTo("010-1234-5678");
	}

	@Test
	void nullRowAndFieldsShouldBeIgnored() {
		piiFieldService.encrypt(null);
		piiFieldService.decrypt((EgovMap) null);

		EgovMap row = new EgovMap();
		row.put("telNo", null);
		row.put("email", "");
		piiFieldService.encrypt(row);

		assertThat(row.get("telNo")).isNull();
		assertThat(row.get("email")).isEqualTo("");
	}

	private EgovMap row() {
		EgovMap row = new EgovMap();
		row.put("name", "홍길동");
		row.put("telNo", "010-1234-5678");
		row.put("email", "user@example.com");
		return row;
	}
}
