package arkive.admin.terms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.terms.service.TermsService;
import arkive.support.ArkiveIntegrationTest;

/**
 * 현재 약관 조회, 필수 동의 판정, 동의 이력의 멱등 저장, 새 버전의 재동의 필요 여부를 검증한다.
 */
@ArkiveIntegrationTest
@Transactional
class TermsServiceTest {

	@Autowired
	private TermsService termsService;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private void insertUser(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", passwordEncoder.encode("TermsPass123!"));
		user.put("userNm", "Terms Tester");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);
	}

	private List<String> requiredIds(List<EgovMap> terms) {
		List<String> ids = new ArrayList<>();
		for (EgovMap term : terms) {
			if ("Y".equals(String.valueOf(term.get("requiredYn")))) {
				ids.add(String.valueOf(term.get("termsId")));
			}
		}
		return ids;
	}

	@Test
	void requiredTermsShouldBecomeSatisfiedOnlyAfterAllConsents() throws Exception {
		insertUser("terms01");
		List<EgovMap> current = termsService.selectCurrentTerms();
		List<String> required = requiredIds(current);
		assertThat(required).isNotEmpty();

		List<EgovMap> pendingBefore = termsService.selectPendingRequiredTerms("terms01");
		assertThat(requiredIds(pendingBefore)).containsExactlyInAnyOrderElementsOf(required);
		assertThat(termsService.hasRequiredConsents("terms01")).isFalse();

		termsService.saveConsents("terms01", required);

		assertThat(termsService.selectPendingRequiredTerms("terms01")).isEmpty();
		assertThat(termsService.hasRequiredConsents("terms01")).isTrue();
		assertThat(termsService.selectConsentHistory("terms01")).hasSize(required.size());
	}

	@Test
	void savingSameConsentTwiceShouldNotDuplicateHistory() throws Exception {
		insertUser("terms02");
		List<String> required = requiredIds(termsService.selectCurrentTerms());

		termsService.saveConsents("terms02", required);
		termsService.saveConsents("terms02", required);

		assertThat(termsService.selectConsentHistory("terms02")).hasSize(required.size());
	}

	@Test
	void unknownTermsIdShouldBeIgnored() throws Exception {
		insertUser("terms03");

		termsService.saveConsents("terms03", List.of("NOT_A_REAL_TERMS_ID"));

		assertThat(termsService.selectConsentHistory("terms03")).isEmpty();
		assertThat(termsService.hasRequiredConsents("terms03")).isFalse();
	}

	@Test
	void newRequiredVersionShouldRequireConsentAgain() throws Exception {
		insertUser("terms04");
		termsService.saveConsents("terms04", requiredIds(termsService.selectCurrentTerms()));

		String termsId = "T_SERVICE_TEST_" + UUID.randomUUID().toString().substring(0, 8);
		jdbcTemplate.update("""
				INSERT INTO TERMS (TERMS_ID, TERMS_TYPE, VERSION, TITLE, CONTENT, REQUIRED_YN, EFFECTIVE_DT, USE_YN)
				VALUES (?, 'SERVICE', '9.9-test', '테스트 새 버전', '테스트 본문', 'Y', CURRENT_DATE, 'Y')
				""", termsId);

		List<EgovMap> pending = termsService.selectPendingRequiredTerms("terms04");
		assertThat(requiredIds(pending)).containsExactly(termsId);
	}
}
