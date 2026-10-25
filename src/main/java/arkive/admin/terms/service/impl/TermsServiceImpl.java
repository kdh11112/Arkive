package arkive.admin.terms.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.terms.service.TermsService;
import jakarta.annotation.Resource;

/**
 * 약관 서비스 구현. 새 버전이 등록되면 기존 동의와 별개로 미동의 상태가 된다.
 */
@Service("termsService")
public class TermsServiceImpl extends EgovAbstractServiceImpl implements TermsService {

	@Resource(name = "termsMapper")
	private TermsMapper termsMapper;

	@Override
	public List<EgovMap> selectCurrentTerms() throws Exception {
		List<EgovMap> terms = termsMapper.selectCurrentTerms();
		convertContent(terms);
		return terms;
	}

	@Override
	public List<EgovMap> selectPendingRequiredTerms(String userId) throws Exception {
		List<EgovMap> terms = termsMapper.selectPendingRequiredTerms(userId);
		convertContent(terms);
		return terms;
	}

	@Override
	public boolean hasRequiredConsents(String userId) throws Exception {
		return termsMapper.selectPendingRequiredTerms(userId).isEmpty();
	}

	@Override
	public List<EgovMap> selectConsentHistory(String userId) throws Exception {
		return termsMapper.selectConsentHistory(userId);
	}

	private void convertContent(List<EgovMap> terms) throws Exception {
		for (EgovMap term : terms) {
			Object content = term.get("content");
			if (content instanceof Clob clob) {
				try (Reader reader = clob.getCharacterStream()) {
					StringBuilder sb = new StringBuilder();
					char[] buffer = new char[8192];
					int length;
					while ((length = reader.read(buffer)) != -1) {
						sb.append(buffer, 0, length);
					}
					term.put("content", sb.toString());
				}
			}
		}
	}

	@Override
	@Transactional
	public void saveConsents(String userId, List<String> termsIds) throws Exception {
		if (userId == null || termsIds == null) {
			return;
		}
		for (String termsId : termsIds) {
			if (termsId == null || termsId.isBlank()) {
				continue;
			}
			// 현재 유효한 약관만 저장한다. 임의 ID로 과거·미사용 약관에 동의하게 하지 않는다.
			EgovMap term = termsMapper.selectCurrentTerm(termsId.trim());
			if (term == null) {
				continue;
			}
			EgovMap param = new EgovMap();
			param.put("userId", userId);
			param.put("termsId", termsId.trim());
			if (termsMapper.updateConsent(param) == 0) {
				try {
					termsMapper.insertConsent(param);
				} catch (DuplicateKeyException e) {
					// 동시 요청이 먼저 넣은 경우에도 최신 동의 시각으로 맞춘다.
					termsMapper.updateConsent(param);
				}
			}
		}
	}
}
