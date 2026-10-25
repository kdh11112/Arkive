package arkive.admin.audit.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 감사 로그 보관 정책 실행기.
 *
 * <p>기본값은 0일(자동 삭제 안 함)이다. 실제 보관 기간은 개인정보 처리방침·법령 검토 후
 * {@code arkive.audit.retention-days}로 설정한다. 운영에서 값을 정하지 않은 상태로
 * 데이터를 임의 삭제하지 않는다.</p>
 */
@Component
public class AuditRetentionJob {

	private static final Logger logger = LoggerFactory.getLogger(AuditRetentionJob.class);

	@Value("${arkive.audit.retention-days:0}")
	private int retentionDays;

	@Resource(name = "auditLogService")
	private AuditLogService auditLogService;

	@Scheduled(cron = "${arkive.audit.retention-cron:0 0 4 * * *}")
	public void purge() {
		if (retentionDays <= 0) {
			return;
		}
		try {
			Date before = Date.from(LocalDate.now().minusDays(retentionDays)
					.atStartOfDay(ZoneId.systemDefault()).toInstant());
			int deleted = auditLogService.purgeBefore(before);
			logger.info("audit log purge completed. days={}, deleted={}", retentionDays, deleted);
		} catch (Exception e) {
			logger.warn("audit log purge failed. days={}", retentionDays, e);
		}
	}
}
