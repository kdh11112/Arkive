package arkive.admin.leave.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 휴가 MyBatis 매퍼. SQL은 Leave_SQL.xml에 있다.
 */
@EgovMapper("leaveMapper")
public interface LeaveMapper {

	EgovMap selectBalance(EgovMap egovMap) throws Exception;

	List<EgovMap> selectMyLeaveList(EgovMap egovMap) throws Exception;

	List<EgovMap> selectLeaveList(EgovMap egovMap) throws Exception;

	int selectLeaveListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectBalanceList(EgovMap egovMap) throws Exception;

	int selectBalanceListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	EgovMap selectActiveUser(String userId) throws Exception;

	EgovMap selectLeaveDetail(String leaveId) throws Exception;

	List<String> selectHolidayDates(EgovMap egovMap) throws Exception;

	int countOverlappingLeave(EgovMap egovMap) throws Exception;

	double selectReservedDays(EgovMap egovMap) throws Exception;

	void insertLeave(EgovMap egovMap) throws Exception;

	int approveLeave(EgovMap egovMap) throws Exception;

	int rejectLeave(EgovMap egovMap) throws Exception;

	int cancelLeave(EgovMap egovMap) throws Exception;

	int increaseUsedDays(EgovMap egovMap) throws Exception;

	int decreaseUsedDays(EgovMap egovMap) throws Exception;

	int countBalance(EgovMap egovMap) throws Exception;

	void insertBalance(EgovMap egovMap) throws Exception;

	int updateBalance(EgovMap egovMap) throws Exception;
}
