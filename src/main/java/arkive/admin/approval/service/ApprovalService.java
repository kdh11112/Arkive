package arkive.admin.approval.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 약식결재 서비스.
 *
 * <p>기안자는 임시저장(DRAFT) 상태에서만 수정·삭제·상신할 수 있고,
 * 상신(SUBMITTED) 문서는 지정 결재자만 승인·반려할 수 있다. 본인 결재는 허용하지 않는다.
 */
public interface ApprovalService {

	List<EgovMap> selectDocList(EgovMap egovMap) throws Exception;

	EgovMap selectDocDetail(String docId) throws Exception;

	List<EgovMap> selectHistory(String docId) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	void insertDraft(EgovMap egovMap) throws Exception;

	void updateDraft(EgovMap egovMap) throws Exception;

	void deleteDraft(String docId, String userId) throws Exception;

	void submit(String docId, String userId) throws Exception;

	void approve(String docId, String userId) throws Exception;

	void reject(String docId, String userId, String reason) throws Exception;

	void cancel(String docId, String userId) throws Exception;
}
