package arkive.admin.audit.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 감사 로그 MyBatis 매퍼. SQL은 Audit_SQL.xml에 있다.
 */
@EgovMapper("auditLogMapper")
public interface AuditLogMapper {

	void insertAuditLog(EgovMap egovMap) throws Exception;

	List<EgovMap> selectAuditLogList(EgovMap egovMap) throws Exception;

	int selectAuditLogListTotCnt(EgovMap egovMap) throws Exception;

	int deleteAuditLogBefore(EgovMap egovMap) throws Exception;
}
