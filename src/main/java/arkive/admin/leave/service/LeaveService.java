package arkive.admin.leave.service;

import java.math.BigDecimal;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 휴가 서비스.
 *
 * <p>신청(REQUESTED)은 잔여 연차를 예약하지 않고, 승인 시점에 차감한다.
 * 신청 가능 여부는 총 연차 - 사용 - 신청중 일수로 검사해 중복 초과 신청을 막는다.
 * 승인 취소 시 사용일수를 되돌린다.
 */
public interface LeaveService {

	EgovMap selectBalance(String userId, String leaveYear) throws Exception;

	List<EgovMap> selectMyLeaveList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectLeaveList(EgovMap egovMap) throws Exception;

	int selectLeaveListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectBalanceList(EgovMap egovMap) throws Exception;

	int selectBalanceListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	EgovMap selectLeaveDetail(String leaveId) throws Exception;

	void insertLeave(EgovMap egovMap) throws Exception;

	void approveLeave(String leaveId, String approverId) throws Exception;

	void rejectLeave(String leaveId, String approverId, String rejectReason) throws Exception;

	void cancelLeave(String leaveId, String userId) throws Exception;

	void saveBalance(String userId, String leaveYear, BigDecimal totalDays) throws Exception;
}
