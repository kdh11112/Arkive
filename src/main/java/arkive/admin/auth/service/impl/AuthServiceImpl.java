package arkive.admin.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.AuthService;
import arkive.admin.comm.service.PiiFieldService;
import jakarta.annotation.Resource;

/**
 * 로그인·사용자 서비스 구현.
 */
@Service("authService")
public class AuthServiceImpl extends EgovAbstractServiceImpl implements AuthService {

	/** 재설정 링크 유효시간(30분)과 재발송 최소 간격(60초). */
	private static final long RESET_TOKEN_VALID_MILLIS = 30L * 60L * 1000L;
	private static final long RESET_TOKEN_COOLDOWN_MILLIS = 60L * 1000L;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Resource(name = "authMapper")
	private AuthMapper authMapper;

	@Resource(name = "permissionMapper")
	private PermissionMapper permissionMapper;

	@Resource(name = "piiFieldService")
	private PiiFieldService piiFieldService;

	@Override
	public EgovMap selectLoginUser(String userId) throws Exception {
		return authMapper.selectLoginUser(userId);
	}

	@Override
	public List<EgovMap> selectUserList(EgovMap egovMap) throws Exception {
		return authMapper.selectUserList(egovMap);
	}

	@Override
	public int selectUserListTotCnt(EgovMap egovMap) throws Exception {
		return authMapper.selectUserListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectUserDetail(String userId) throws Exception {
		EgovMap user = authMapper.selectUserDetail(userId);
		piiFieldService.decryptUserPhone(user);
		return user;
	}

	public EgovMap selectFindUserId(EgovMap egovMap) throws Exception {
		return authMapper.selectFindUserId(egovMap);
	}

	@Override
	public void insertUser(EgovMap egovMap) throws Exception {
		piiFieldService.encryptUserPhone(egovMap);
		authMapper.insertUser(egovMap);
	}

	@Override
	public void updateUser(EgovMap egovMap) throws Exception {
		piiFieldService.encryptUserPhone(egovMap);
		authMapper.updateUser(egovMap);
	}

	@Override
	@Transactional
	public void deleteUser(String userId) throws Exception {
		// 사용자 행만 지우면 역할 매핑이 남아 같은 ID 재가입 시 이전 권한을 물려받는다.
		permissionMapper.deleteUserRoles(userId);
		authMapper.deleteUser(userId);
	}

	@Override
	public void addFailCount(String userId) throws Exception {
		authMapper.addFailCount(userId);
	}

	@Override
	public void lockUser(String userId) throws Exception {
		authMapper.lockUser(userId);
	}

	@Override
	public void resetFailCount(String userId) throws Exception {
		authMapper.resetFailCount(userId);
	}

	@Override
	public void updatePassword(EgovMap egovMap) throws Exception {
		authMapper.updatePassword(egovMap);
	}

	@Override
	public EgovMap selectOauthLink(EgovMap egovMap) throws Exception {
		return authMapper.selectOauthLink(egovMap);
	}

	@Override
	public void insertOauthLink(EgovMap egovMap) throws Exception {
		authMapper.insertOauthLink(egovMap);
	}

	@Override
	public String issuePasswordResetToken(String userId) throws Exception {
		Timestamp latest = authMapper.selectLatestPasswordResetDt(userId);
		long now = System.currentTimeMillis();
		if (latest != null && latest.getTime() > now - RESET_TOKEN_COOLDOWN_MILLIS) {
			// 같은 사용자에게 연속 요청이 와도 기존 링크를 유지하고 메일만 다시 보내지 않는다.
			return null;
		}
		byte[] randomBytes = new byte[32];
		SECURE_RANDOM.nextBytes(randomBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
		EgovMap param = new EgovMap();
		param.put("tokenHash", sha256Hex(rawToken));
		param.put("userId", userId);
		param.put("expiresDt", new Timestamp(now + RESET_TOKEN_VALID_MILLIS));
		// 이전 미사용 링크는 즉시 무효화한다.
		authMapper.deleteUnusedPasswordResetTokens(userId);
		authMapper.insertPasswordResetToken(param);
		return rawToken;
	}

	@Override
	public boolean isValidPasswordResetToken(String rawToken) throws Exception {
		if (rawToken == null || rawToken.isEmpty()) {
			return false;
		}
		return authMapper.selectValidPasswordResetToken(sha256Hex(rawToken)) != null;
	}

	@Override
	@Transactional
	public String resetPasswordByToken(String rawToken, String encodedPassword) throws Exception {
		if (rawToken == null || rawToken.isEmpty() || encodedPassword == null || encodedPassword.isEmpty()) {
			throw new IllegalArgumentException("재설정 링크가 올바르지 않습니다.");
		}
		String tokenHash = sha256Hex(rawToken);
		EgovMap token = authMapper.selectValidPasswordResetToken(tokenHash);
		if (token == null) {
			throw new IllegalArgumentException("재설정 링크가 만료되었거나 이미 사용되었습니다.");
		}
		// 동시에 같은 링크를 사용하는 요청 중 하나만 성공하게 한다.
		if (authMapper.markPasswordResetTokenUsed(tokenHash) == 0) {
			throw new IllegalArgumentException("재설정 링크가 만료되었거나 이미 사용되었습니다.");
		}
		String userId = String.valueOf(token.get("userId"));
		EgovMap user = authMapper.selectUserDetail(userId);
		if (user == null || !"Y".equals(String.valueOf(user.get("useYn")))) {
			throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
		}
		EgovMap passwordParam = new EgovMap();
		passwordParam.put("userId", userId);
		passwordParam.put("password", encodedPassword);
		authMapper.updatePassword(passwordParam);
		authMapper.resetFailCount(userId);
		return userId;
	}

	private String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
		}
	}
}
