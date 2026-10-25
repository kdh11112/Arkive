package arkive.admin.audit.service.impl;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import arkive.admin.audit.service.AuditEvent;
import arkive.admin.audit.service.AuditLogService;
import jakarta.annotation.Resource;

/**
 * 감사 로그 저장·조회 구현. 저장 실패는 업무 흐름을 깨지 않는다.
 */
@Service("auditLogService")
public class AuditLogServiceImpl extends EgovAbstractServiceImpl implements AuditLogService {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);

	@Resource(name = "auditLogMapper")
	private AuditLogMapper auditLogMapper;

	@Override
	public void record(AuditEvent event) {
		if (event == null || event.eventType() == null || event.eventType().isEmpty()) {
			return;
		}
		try {
			EgovMap param = new EgovMap();
			param.put("logId", "AL" + UUID.randomUUID().toString().replace("-", "").substring(0, 30));
			param.put("eventType", truncate(event.eventType(), 30));
			param.put("userId", truncate(event.userId(), 50));
			param.put("userNm", truncate(event.userNm(), 100));
			param.put("menuId", truncate(event.menuId(), 20));
			param.put("targetType", truncate(event.targetType(), 50));
			param.put("targetId", truncate(event.targetId(), 100));
			param.put("requestUri", truncate(event.requestUri(), 500));
			param.put("httpMethod", truncate(event.httpMethod(), 10));
			param.put("clientIp", truncate(event.clientIp(), 64));
			param.put("result", event.result() == null ? "SUCCESS" : truncate(event.result(), 20));
			param.put("detail", truncate(event.detail(), 1000));
			auditLogMapper.insertAuditLog(param);
		} catch (Exception e) {
			// 감사 로그 장애가 로그인·업무 요청을 실패시키면 안 된다.
			logger.warn("audit log write skipped. type={}", event.eventType(), e);
		}
	}

	@Override
	public List<EgovMap> selectAuditLogList(EgovMap egovMap) throws Exception {
		return auditLogMapper.selectAuditLogList(egovMap);
	}

	@Override
	public int selectAuditLogListTotCnt(EgovMap egovMap) throws Exception {
		return auditLogMapper.selectAuditLogListTotCnt(egovMap);
	}

	@Override
	public int purgeBefore(Date before) throws Exception {
		EgovMap param = new EgovMap();
		param.put("before", before);
		return auditLogMapper.deleteAuditLogBefore(param);
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
