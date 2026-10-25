package arkive.admin.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * ApplicationEvent 기반 감사 로그 저장 리스너.
 * 업무 서비스는 이벤트만 발행하고, 저장 방식은 이 리스너가 캡슐화한다.
 */
@Component
public class AuditLogEventListener {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogEventListener.class);

	@Resource(name = "auditLogService")
	private AuditLogService auditLogService;

	@EventListener
	public void onAuditEvent(AuditEvent event) {
		try {
			auditLogService.record(event);
		} catch (Exception e) {
			logger.warn("audit event handling skipped", e);
		}
	}
}
