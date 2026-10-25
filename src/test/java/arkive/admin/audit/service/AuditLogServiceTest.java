package arkive.admin.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.stats.service.StatsService;
import arkive.support.ArkiveIntegrationTest;

/**
 * 감사 이벤트 저장·조회와 통계 집계를 실제 SQL로 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class AuditLogServiceTest {

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private StatsService statsService;

	private EgovMap range() {
		EgovMap param = new EgovMap();
		param.put("startDate", Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
		param.put("endDate", Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
		return param;
	}

	@Test
	@Rollback
	void auditEventsShouldBeStoredAndAggregated() throws Exception {
		auditLogService.record(AuditEvent.access("MENU_ACCESS", "auditUser", "감사사용자", "ME02010700",
				null, null, "/stats/statsList", "GET", "127.0.0.1"));
		auditLogService.record(AuditEvent.access("PII_DOWNLOAD", "auditUser", "감사사용자", "ME02031300",
				"DUTY", "202610", "/duty/downloadDutyExcel.do", "GET", "127.0.0.1"));

		EgovMap param = new EgovMap();
		param.put("searchKeyword", "auditUser");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 20);
		assertThat(auditLogService.selectAuditLogList(param)).hasSizeGreaterThanOrEqualTo(2);
		assertThat(auditLogService.selectAuditLogListTotCnt(param)).isGreaterThanOrEqualTo(2);

		EgovMap range = range();
		assertThat(statsService.selectEventStats(range)).extracting(row -> String.valueOf(row.get("eventType")))
				.contains("MENU_ACCESS", "PII_DOWNLOAD");
		assertThat(statsService.selectDailyAccessStats(range)).isNotEmpty();
		assertThat(statsService.selectDownloadStats(range)).extracting(row -> String.valueOf(row.get("targetType")))
				.contains("DUTY");
	}

	@Test
	@Rollback
	void purgeShouldDeleteOnlyBeforeBoundary() throws Exception {
		auditLogService.record(AuditEvent.access("MENU_ACCESS", "purgeUser", "정리사용자", null,
				null, null, "/stats/statsList", "GET", "127.0.0.1"));
		EgovMap param = new EgovMap();
		param.put("searchKeyword", "purgeUser");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 20);
		assertThat(auditLogService.selectAuditLogList(param)).isNotEmpty();

		auditLogService.purgeBefore(Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay()));

		assertThat(auditLogService.selectAuditLogList(param)).isEmpty();
	}
}
