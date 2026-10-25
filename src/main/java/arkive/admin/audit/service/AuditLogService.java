package arkive.admin.audit.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 감사 로그 조회·보관 서비스.
 */
public interface AuditLogService {

	void record(AuditEvent event);

	List<EgovMap> selectAuditLogList(EgovMap egovMap) throws Exception;

	int selectAuditLogListTotCnt(EgovMap egovMap) throws Exception;

	int purgeBefore(java.util.Date before) throws Exception;
}
