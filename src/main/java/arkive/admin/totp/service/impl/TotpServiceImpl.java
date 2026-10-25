package arkive.admin.totp.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.service.FieldCryptoService;
import arkive.admin.totp.service.TotpService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.annotation.Resource;

/**
 * TOTP 서비스 구현.
 *
 * <p>비밀키는 {@link FieldCryptoService}로 암호화해 저장한다. 복구코드는 SHA-256 해시만
 * 저장하고 발급 직후 한 번만 화면에 보여준다. 관리자 초기화는 행을 삭제해 다음 로그인부터
 * TOTP 없이 비밀번호로 인증하게 한다.</p>
 */
@Service("totpService")
public class TotpServiceImpl extends EgovAbstractServiceImpl implements TotpService {

	private static final int RECOVERY_CODE_COUNT = 8;

	@Resource(name = "totpMapper")
	private TotpMapper totpMapper;

	@Resource(name = "fieldCryptoService")
	private FieldCryptoService fieldCryptoService;

	@Value("${arkive.totp.issuer:Arkive}")
	private String issuer;

	private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
	private final CodeVerifier codeVerifier = buildVerifier();
	private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();

	private CodeVerifier buildVerifier() {
		DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
		verifier.setAllowedTimePeriodDiscrepancy(1);
		return verifier;
	}

	@Override
	public boolean isAvailable() {
		return fieldCryptoService.isAvailable();
	}

	@Override
	public boolean isEnabled(String userId) throws Exception {
		EgovMap row = totpMapper.selectTotp(userId);
		return row != null && "Y".equals(String.valueOf(row.get("enabledYn")));
	}

	@Override
	@Transactional
	public Map<String, Object> beginSetup(String userId) throws Exception {
		if (!isAvailable()) {
			throw new IllegalStateException("2단계 인증을 사용하려면 ARKIVE_FIELD_KEY가 설정되어야 합니다.");
		}
		EgovMap existing = totpMapper.selectTotp(userId);
		if (existing != null && "Y".equals(String.valueOf(existing.get("enabledYn")))) {
			throw new IllegalStateException("이미 2단계 인증이 설정되어 있습니다. 해제 후 다시 설정하세요.");
		}
		String secret = new DefaultSecretGenerator().generate();
		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("secretEnc", fieldCryptoService.encrypt(secret));
		if (existing == null) {
			totpMapper.insertTotp(param);
		} else {
			totpMapper.updateTotpSecret(param);
		}
		QrData qrData = new QrData.Builder()
				.label(userId)
				.secret(secret)
				.issuer(issuer)
				.algorithm(HashingAlgorithm.SHA1)
				.digits(6)
				.period(30)
				.build();
		ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
		qrGenerator.setImageSize(240);
		String qrImage = "data:" + qrGenerator.getImageMimeType() + ";base64,"
				+ Base64.getEncoder().encodeToString(qrGenerator.generate(qrData));
		Map<String, Object> result = new HashMap<>();
		result.put("secret", secret);
		result.put("otpauthUri", qrData.getUri());
		result.put("qrImage", qrImage);
		return result;
	}

	@Override
	@Transactional
	public List<String> confirmSetup(String userId, String code) throws Exception {
		EgovMap row = totpMapper.selectTotp(userId);
		if (row == null) {
			throw new IllegalStateException("2단계 인증 설정을 먼저 시작하세요.");
		}
		String secret = fieldCryptoService.decrypt(String.valueOf(row.get("secretEnc")));
		if (!codeVerifier.isValidCode(secret, normalizeCode(code))) {
			throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
		}
		totpMapper.enableTotp(userId);
		totpMapper.deleteRecoveryCodes(userId);
		String[] codes = recoveryCodeGenerator.generateCodes(RECOVERY_CODE_COUNT);
		List<String> plainCodes = new ArrayList<>();
		for (String recoveryCode : codes) {
			EgovMap recovery = new EgovMap();
			recovery.put("recoveryId", UUID.randomUUID().toString());
			recovery.put("userId", userId);
			recovery.put("codeHash", sha256Hex(recoveryCode));
			totpMapper.insertRecoveryCode(recovery);
			plainCodes.add(recoveryCode);
		}
		return plainCodes;
	}

	@Override
	public boolean verify(String userId, String code) throws Exception {
		EgovMap row = totpMapper.selectTotp(userId);
		if (row == null || !"Y".equals(String.valueOf(row.get("enabledYn")))) {
			return false;
		}
		String secret = fieldCryptoService.decrypt(String.valueOf(row.get("secretEnc")));
		return codeVerifier.isValidCode(secret, normalizeCode(code));
	}

	@Override
	@Transactional
	public boolean verifyRecovery(String userId, String code) throws Exception {
		String normalized = normalizeCode(code);
		if (normalized.isEmpty()) {
			return false;
		}
		EgovMap row = totpMapper.selectRecoveryCode(sha256Hex(normalized));
		if (row == null || !userId.equals(String.valueOf(row.get("userId")))) {
			return false;
		}
		return totpMapper.markRecoveryCodeUsed(String.valueOf(row.get("recoveryId"))) == 1;
	}

	@Override
	public int countUnusedRecoveryCodes(String userId) throws Exception {
		return totpMapper.countUnusedRecoveryCodes(userId);
	}

	@Override
	@Transactional
	public void reset(String userId) throws Exception {
		totpMapper.deleteRecoveryCodes(userId);
		totpMapper.deleteTotp(userId);
	}

	private String normalizeCode(String code) {
		return code == null ? "" : code.replaceAll("\\s", "");
	}

	private String sha256Hex(String value) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
	}
}
