package arkive.admin.terms.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 약관·동의 MyBatis 매퍼.
 */
@EgovMapper("termsMapper")
public interface TermsMapper {

	List<EgovMap> selectCurrentTerms() throws Exception;

	List<EgovMap> selectPendingRequiredTerms(String userId) throws Exception;

	EgovMap selectCurrentTerm(String termsId) throws Exception;

	int updateConsent(EgovMap egovMap) throws Exception;

	int insertConsent(EgovMap egovMap) throws Exception;

	List<EgovMap> selectConsentHistory(String userId) throws Exception;
}
