package arkive.admin.approval.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 약식결재 MyBatis 매퍼. SQL은 Approval_SQL.xml에 있다.
 */
@EgovMapper("approvalMapper")
public interface ApprovalMapper {

	List<EgovMap> selectDocList(EgovMap egovMap) throws Exception;

	EgovMap selectDocDetail(String docId) throws Exception;

	List<EgovMap> selectHistory(String docId) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	EgovMap selectActiveUser(String userId) throws Exception;

	void insertDoc(EgovMap egovMap) throws Exception;

	int updateDoc(EgovMap egovMap) throws Exception;

	int deleteDoc(EgovMap egovMap) throws Exception;

	int deleteHistory(String docId) throws Exception;

	int submitDoc(EgovMap egovMap) throws Exception;

	int approveDoc(EgovMap egovMap) throws Exception;

	int rejectDoc(EgovMap egovMap) throws Exception;

	int cancelDoc(EgovMap egovMap) throws Exception;

	void insertHistory(EgovMap egovMap) throws Exception;
}
