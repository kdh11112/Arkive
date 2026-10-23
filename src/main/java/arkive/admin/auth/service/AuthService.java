package arkive.admin.auth.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 로그인·사용자 서비스. 실패 잠금은 Service에서 건수로 판단한다.
 */
public interface AuthService {

	EgovMap selectLoginUser(String userId) throws Exception;

	List<EgovMap> selectUserList(EgovMap egovMap) throws Exception;

	int selectUserListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectUserDetail(String userId) throws Exception;

	EgovMap selectFindUserId(EgovMap egovMap) throws Exception;

	void insertUser(EgovMap egovMap) throws Exception;

	void updateUser(EgovMap egovMap) throws Exception;

	void deleteUser(String userId) throws Exception;

	void addFailCount(String userId) throws Exception;

	void lockUser(String userId) throws Exception;

	void resetFailCount(String userId) throws Exception;

	void updatePassword(EgovMap egovMap) throws Exception;

	EgovMap selectOauthLink(EgovMap egovMap) throws Exception;

	void insertOauthLink(EgovMap egovMap) throws Exception;

	/**
	 * 비밀번호 재설정 원문 토큰을 발급한다. 같은 사용자에게 60초 안에 다시 요청하면
	 * null을 반환해 메일 폭주를 막는다.
	 */
	String issuePasswordResetToken(String userId) throws Exception;

	boolean isValidPasswordResetToken(String rawToken) throws Exception;

	/**
	 * 토큰을 1회 소비하고 비밀번호를 변경한다. 성공하면 사용자 ID를 반환한다.
	 */
	String resetPasswordByToken(String rawToken, String encodedPassword) throws Exception;
}
