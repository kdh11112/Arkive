package arkive.admin.approval.service.impl;

import java.util.List;
import java.util.Set;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.approval.service.ApprovalService;
import arkive.admin.comm.web.CommUtil;
import jakarta.annotation.Resource;

/**
 * 약식결재 서비스 구현.
 */
@Service("approvalService")
public class ApprovalServiceImpl extends EgovAbstractServiceImpl implements ApprovalService {

	private static final Set<String> DOC_TYPES = Set.of("GENERAL", "LEAVE", "EXPENSE", "ETC");
	private static final String DRAFT = "DRAFT";
	private static final String SUBMITTED = "SUBMITTED";

	@Resource(name = "approvalMapper")
	private ApprovalMapper approvalMapper;

	@Override
	public List<EgovMap> selectDocList(EgovMap egovMap) throws Exception {
		return approvalMapper.selectDocList(egovMap);
	}

	@Override
	public EgovMap selectDocDetail(String docId) throws Exception {
		return isBlank(docId) ? null : approvalMapper.selectDocDetail(docId.trim());
	}

	@Override
	public List<EgovMap> selectHistory(String docId) throws Exception {
		return isBlank(docId) ? List.of() : approvalMapper.selectHistory(docId.trim());
	}

	@Override
	public List<EgovMap> selectActiveUserList() throws Exception {
		return approvalMapper.selectActiveUserList();
	}

	@Override
	@Transactional
	public void insertDraft(EgovMap egovMap) throws Exception {
		applyValidatedFields(egovMap);
		String userId = trim(egovMap.get("drafterId"));
		egovMap.put("docId", "AP" + CommUtil.getFileId());
		egovMap.put("status", DRAFT);
		approvalMapper.insertDoc(egovMap);
		insertHistory(String.valueOf(egovMap.get("docId")), "CREATE", userId, "기안 작성");
	}

	@Override
	@Transactional
	public void updateDraft(EgovMap egovMap) throws Exception {
		applyValidatedFields(egovMap);
		String docId = trim(egovMap.get("docId"));
		String userId = trim(egovMap.get("drafterId"));
		requireOwnedDraft(docId, userId, "수정");
		if (approvalMapper.updateDoc(egovMap) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
		insertHistory(docId, "UPDATE", userId, "기안 수정");
	}

	@Override
	@Transactional
	public void deleteDraft(String docId, String userId) throws Exception {
		requireOwnedDraft(docId, userId, "삭제");
		EgovMap param = new EgovMap();
		param.put("docId", docId.trim());
		param.put("drafterId", userId);
		approvalMapper.deleteHistory(docId.trim());
		if (approvalMapper.deleteDoc(param) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void submit(String docId, String userId) throws Exception {
		requireOwnedDraft(docId, userId, "상신");
		EgovMap param = new EgovMap();
		param.put("docId", docId.trim());
		param.put("drafterId", userId);
		if (approvalMapper.submitDoc(param) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
		insertHistory(docId, "SUBMIT", userId, "결재 상신");
	}

	@Override
	@Transactional
	public void approve(String docId, String userId) throws Exception {
		requireApproverTarget(docId, userId, "승인");
		EgovMap param = new EgovMap();
		param.put("docId", docId.trim());
		param.put("approverId", userId);
		if (approvalMapper.approveDoc(param) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
		insertHistory(docId, "APPROVE", userId, "승인");
	}

	@Override
	@Transactional
	public void reject(String docId, String userId, String reason) throws Exception {
		String trimmed = reason == null ? "" : reason.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("반려 사유를 입력하세요.");
		}
		if (trimmed.length() > 500) {
			throw new IllegalArgumentException("반려 사유는 500자 이내로 입력하세요.");
		}
		requireApproverTarget(docId, userId, "반려");
		EgovMap param = new EgovMap();
		param.put("docId", docId.trim());
		param.put("approverId", userId);
		param.put("rejectReason", trimmed);
		if (approvalMapper.rejectDoc(param) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
		insertHistory(docId, "REJECT", userId, trimmed);
	}

	@Override
	@Transactional
	public void cancel(String docId, String userId) throws Exception {
		EgovMap detail = selectDocDetail(docId);
		if (detail == null) {
			throw new IllegalArgumentException("문서가 없습니다.");
		}
		if (!userId.equals(trim(detail.get("drafterId")))) {
			throw new IllegalStateException("기안자만 상신을 취소할 수 있습니다.");
		}
		if (!SUBMITTED.equals(trim(detail.get("status")))) {
			throw new IllegalStateException("상신된 문서만 취소할 수 있습니다.");
		}
		EgovMap param = new EgovMap();
		param.put("docId", docId.trim());
		param.put("drafterId", userId);
		if (approvalMapper.cancelDoc(param) == 0) {
			throw new IllegalStateException("이미 처리된 문서입니다. 새로고침 후 다시 시도하세요.");
		}
		insertHistory(docId, "CANCEL", userId, "상신 취소");
	}

	private EgovMap requireOwnedDraft(String docId, String userId, String action) throws Exception {
		EgovMap detail = selectDocDetail(docId);
		if (detail == null) {
			throw new IllegalArgumentException("문서가 없습니다.");
		}
		if (!userId.equals(trim(detail.get("drafterId")))) {
			throw new IllegalStateException("기안자만 " + action + "할 수 있습니다.");
		}
		if (!DRAFT.equals(trim(detail.get("status")))) {
			throw new IllegalStateException("임시저장 문서만 " + action + "할 수 있습니다.");
		}
		return detail;
	}

	private void requireApproverTarget(String docId, String userId, String action) throws Exception {
		EgovMap detail = selectDocDetail(docId);
		if (detail == null) {
			throw new IllegalArgumentException("문서가 없습니다.");
		}
		if (!userId.equals(trim(detail.get("approverId")))) {
			throw new IllegalStateException("지정된 결재자만 " + action + "할 수 있습니다.");
		}
		if (!SUBMITTED.equals(trim(detail.get("status")))) {
			throw new IllegalStateException("상신된 문서만 " + action + "할 수 있습니다.");
		}
	}

	private void applyValidatedFields(EgovMap egovMap) throws Exception {
		String docType = trim(egovMap.get("docType"));
		String title = trim(egovMap.get("title"));
		String content = trim(egovMap.get("content"));
		String drafterId = trim(egovMap.get("drafterId"));
		String approverId = trim(egovMap.get("approverId"));

		if (drafterId.isEmpty()) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		if (!DOC_TYPES.contains(docType)) {
			throw new IllegalArgumentException("문서구분이 올바르지 않습니다.");
		}
		if (title.isEmpty()) {
			throw new IllegalArgumentException("제목을 입력하세요.");
		}
		if (title.length() > 200) {
			throw new IllegalArgumentException("제목은 200자 이내로 입력하세요.");
		}
		if (content.length() > 4000) {
			throw new IllegalArgumentException("내용은 4000자 이내로 입력하세요.");
		}
		if (approverId.isEmpty()) {
			throw new IllegalArgumentException("결재자를 선택하세요.");
		}
		if (approverId.equals(drafterId)) {
			throw new IllegalArgumentException("본인을 결재자로 지정할 수 없습니다.");
		}
		if (approvalMapper.selectActiveUser(approverId) == null) {
			throw new IllegalArgumentException("사용중인 결재자를 선택하세요.");
		}
		egovMap.put("docType", docType);
		egovMap.put("title", title);
		egovMap.put("content", content);
		egovMap.put("drafterId", drafterId);
		egovMap.put("approverId", approverId);
	}

	private void insertHistory(String docId, String action, String actorId, String note) throws Exception {
		EgovMap history = new EgovMap();
		history.put("historyId", "AH" + CommUtil.getFileId());
		history.put("docId", docId);
		history.put("action", action);
		history.put("actorId", actorId);
		history.put("note", note);
		approvalMapper.insertHistory(history);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
