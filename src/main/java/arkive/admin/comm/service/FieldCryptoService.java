package arkive.admin.comm.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * DB 필드 암호화. AES-GCM(12바이트 IV, 128비트 태그)을 사용한다.
 *
 * <p>키는 {@code ARKIVE_FIELD_KEY} 환경변수로 주입한다. 키가 없으면 암호화가 필요한 기능
 * (TOTP, 개인정보 필드)은 저장을 거부하거나 기존 평문을 그대로 읽는다. 운영 키를 저장소에
 * 커밋하지 않는다.</p>
 *
 * <p>출력 형식은 {@code enc:v1:base64(iv+ciphertext+tag)}이다. 접두사가 없는 값은
 * 이 기능 도입 전의 평문으로 보고 그대로 반환해 기존 데이터를 읽을 수 있게 한다.</p>
 */
@Service("fieldCryptoService")
public class FieldCryptoService {

	private static final Logger logger = LoggerFactory.getLogger(FieldCryptoService.class);
	private static final String PREFIX = "enc:v1:";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKeySpec key;

	public FieldCryptoService(@Value("${arkive.crypto.field-key:}") String configuredKey) {
		this.key = buildKey(configuredKey);
		if (this.key == null) {
			logger.warn("arkive.crypto.field-key is not configured. Encrypted fields and TOTP setup are disabled.");
		}
	}

	public boolean isAvailable() {
		return key != null;
	}

	public String encrypt(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		if (!isAvailable()) {
			throw new IllegalStateException("필드 암호화 키(ARKIVE_FIELD_KEY)가 설정되지 않았습니다.");
		}
		try {
			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			byte[] combined = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
			return PREFIX + Base64.getEncoder().encodeToString(combined);
		} catch (Exception e) {
			throw new IllegalStateException("필드 암호화에 실패했습니다.", e);
		}
	}

	public String decrypt(String stored) {
		if (stored == null || !stored.startsWith(PREFIX)) {
			return stored;
		}
		if (!isAvailable()) {
			throw new IllegalStateException("필드 암호화 키(ARKIVE_FIELD_KEY)가 설정되지 않았습니다.");
		}
		try {
			byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
			if (combined.length <= IV_BYTES) {
				throw new IllegalArgumentException("암호문 형식이 올바르지 않습니다.");
			}
			byte[] iv = new byte[IV_BYTES];
			System.arraycopy(combined, 0, iv, 0, IV_BYTES);
			byte[] encrypted = new byte[combined.length - IV_BYTES];
			System.arraycopy(combined, IV_BYTES, encrypted, 0, encrypted.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("필드 복호화에 실패했습니다. 키가 변경되지 않았는지 확인하세요.", e);
		}
	}

	private SecretKeySpec buildKey(String configuredKey) {
		if (configuredKey == null || configuredKey.trim().isEmpty()) {
			return null;
		}
		byte[] raw;
		try {
			raw = Base64.getDecoder().decode(configuredKey.trim());
		} catch (IllegalArgumentException e) {
			raw = configuredKey.trim().getBytes(StandardCharsets.UTF_8);
		}
		if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
			raw = sha256(raw);
		}
		return new SecretKeySpec(raw, "AES");
	}

	private byte[] sha256(byte[] value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value);
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
		}
	}
}
