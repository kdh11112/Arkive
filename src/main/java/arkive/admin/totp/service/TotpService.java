package arkive.admin.totp.service;

import java.util.List;
import java.util.Map;

/**
 * TOTP 2단계 인증. 설정 시작·확인, 로그인 검증, 관리자 초기화를 제공한다.
 */
public interface TotpService {

	boolean isAvailable();

	boolean isEnabled(String userId) throws Exception;

	Map<String, Object> beginSetup(String userId) throws Exception;

	List<String> confirmSetup(String userId, String code) throws Exception;

	boolean verify(String userId, String code) throws Exception;

	boolean verifyRecovery(String userId, String code) throws Exception;

	int countUnusedRecoveryCodes(String userId) throws Exception;

	void reset(String userId) throws Exception;
}
