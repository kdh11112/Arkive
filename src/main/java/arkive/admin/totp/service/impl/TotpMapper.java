package arkive.admin.totp.service.impl;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * TOTP·복구코드 MyBatis 매퍼.
 */
@EgovMapper("totpMapper")
public interface TotpMapper {

	EgovMap selectTotp(String userId) throws Exception;

	void insertTotp(EgovMap egovMap) throws Exception;

	void updateTotpSecret(EgovMap egovMap) throws Exception;

	void enableTotp(String userId) throws Exception;

	void deleteTotp(String userId) throws Exception;

	void deleteRecoveryCodes(String userId) throws Exception;

	void insertRecoveryCode(EgovMap egovMap) throws Exception;

	EgovMap selectRecoveryCode(String codeHash) throws Exception;

	int markRecoveryCodeUsed(String recoveryId) throws Exception;

	int countUnusedRecoveryCodes(String userId) throws Exception;
}
