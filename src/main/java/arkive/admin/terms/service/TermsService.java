package arkive.admin.terms.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 버전별 약관 조회와 사용자 동의 저장.
 */
public interface TermsService {

	List<EgovMap> selectCurrentTerms() throws Exception;

	List<EgovMap> selectPendingRequiredTerms(String userId) throws Exception;

	boolean hasRequiredConsents(String userId) throws Exception;

	List<EgovMap> selectConsentHistory(String userId) throws Exception;

	/**
	 * 전달된 약관 ID 중 현재 유효한 약관만 동의로 저장한다.
	 * 필수 약관 누락 검사는 호출자(가입·동의 화면)가 한다.
	 */
	void saveConsents(String userId, List<String> termsIds) throws Exception;
}
