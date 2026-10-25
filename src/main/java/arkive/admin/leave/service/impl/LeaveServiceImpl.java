package arkive.admin.leave.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.leave.service.LeaveDaysCalculator;
import arkive.admin.leave.service.LeaveService;
import jakarta.annotation.Resource;

/**
 * 휴가 서비스 구현.
 *
 * <p>휴가 기간·일수·연차 검증은 서비스에서 수행한다. 승인·취소로 연차가 바뀌는
 * 구간은 트랜잭션으로 묶고, 잔여 연차 조건을 UPDATE에 함께 넣어 동시 승인에도
 * 음수 잔여가 생기지 않게 한다.
 */
@Service("leaveService")
public class LeaveServiceImpl extends EgovAbstractServiceImpl implements LeaveService {

	private static final String STATUS_REQUESTED = "REQUESTED";
	private static final String STATUS_APPROVED = "APPROVED";

	@Resource(name = "leaveMapper")
	private LeaveMapper leaveMapper;

	@Override
	public EgovMap selectBalance(String userId, String leaveYear) throws Exception {
		if (isBlank(userId) || !isYear(leaveYear)) {
			return null;
		}
		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("leaveYear", leaveYear);
		return leaveMapper.selectBalance(param);
	}

	@Override
	public List<EgovMap> selectMyLeaveList(EgovMap egovMap) throws Exception {
		return leaveMapper.selectMyLeaveList(egovMap);
	}

	@Override
	public List<EgovMap> selectLeaveList(EgovMap egovMap) throws Exception {
		return leaveMapper.selectLeaveList(egovMap);
	}

	@Override
	public int selectLeaveListTotCnt(EgovMap egovMap) throws Exception {
		return leaveMapper.selectLeaveListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectBalanceList(EgovMap egovMap) throws Exception {
		return leaveMapper.selectBalanceList(egovMap);
	}

	@Override
	public int selectBalanceListTotCnt(EgovMap egovMap) throws Exception {
		return leaveMapper.selectBalanceListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectActiveUserList() throws Exception {
		return leaveMapper.selectActiveUserList();
	}

	@Override
	public EgovMap selectLeaveDetail(String leaveId) throws Exception {
		if (isBlank(leaveId)) {
			return null;
		}
		return leaveMapper.selectLeaveDetail(leaveId.trim());
	}

	@Override
	@Transactional
	public void insertLeave(EgovMap egovMap) throws Exception {
		String userId = trim(egovMap.get("userId"));
		String leaveType = trim(egovMap.get("leaveType"));
		String startDe = trim(egovMap.get("startDe"));
		String endDe = trim(egovMap.get("endDe"));
		String reason = trim(egovMap.get("reason"));
		if (userId.isEmpty()) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		if (reason.isEmpty()) {
			throw new IllegalArgumentException("휴가 사유를 입력하세요.");
		}
		if (reason.length() > 500) {
			throw new IllegalArgumentException("휴가 사유는 500자 이내로 입력하세요.");
		}

		LocalDate start = LeaveDaysCalculator.parseDate(startDe, "시작일");
		LocalDate end = LeaveDaysCalculator.parseDate(endDe, "종료일");
		Set<String> holidayDates = new HashSet<>(leaveMapper.selectHolidayDates(holidayParam(start, end)));
		double days = LeaveDaysCalculator.calculate(leaveType, startDe, endDe, holidayDates);
		String leaveYear = startDe.substring(0, 4);

		EgovMap overlapParam = new EgovMap();
		overlapParam.put("userId", userId);
		overlapParam.put("startDe", startDe);
		overlapParam.put("endDe", endDe);
		if (leaveMapper.countOverlappingLeave(overlapParam) > 0) {
			throw new IllegalStateException("같은 기간에 이미 신청했거나 승인된 휴가가 있습니다.");
		}

		EgovMap balance = selectBalance(userId, leaveYear);
		if (balance == null) {
			throw new IllegalStateException("연차 정보가 없습니다. 관리자에게 연차 부여를 요청하세요.");
		}
		EgovMap reservedParam = new EgovMap();
		reservedParam.put("userId", userId);
		reservedParam.put("leaveYear", leaveYear);
		double available = toDouble(balance.get("totalDays")) - toDouble(balance.get("usedDays"))
				- leaveMapper.selectReservedDays(reservedParam);
		if (available < days) {
			throw new IllegalStateException("잔여 연차가 부족합니다. (신청 " + formatDays(days)
					+ "일, 사용 가능 " + formatDays(available) + "일)");
		}

		egovMap.put("leaveId", "LV" + CommUtil.getFileId());
		egovMap.put("days", BigDecimal.valueOf(days));
		egovMap.put("status", STATUS_REQUESTED);
		leaveMapper.insertLeave(egovMap);
	}

	@Override
	@Transactional
	public void approveLeave(String leaveId, String approverId) throws Exception {
		EgovMap detail = requireRequested(leaveId, "승인");
		String leaveYear = trim(detail.get("startDe")).substring(0, 4);
		double days = toDouble(detail.get("days"));

		EgovMap balanceParam = new EgovMap();
		balanceParam.put("userId", trim(detail.get("userId")));
		balanceParam.put("leaveYear", leaveYear);
		balanceParam.put("days", BigDecimal.valueOf(days));
		// 잔여 연차가 부족하면 0건 갱신되어 승인 전체가 롤백된다.
		if (leaveMapper.increaseUsedDays(balanceParam) == 0) {
			throw new IllegalStateException("잔여 연차가 부족하여 승인할 수 없습니다.");
		}

		EgovMap approveParam = new EgovMap();
		approveParam.put("leaveId", leaveId.trim());
		approveParam.put("approverId", approverId);
		if (leaveMapper.approveLeave(approveParam) == 0) {
			throw new IllegalStateException("이미 처리된 휴가입니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void rejectLeave(String leaveId, String approverId, String rejectReason) throws Exception {
		String reason = rejectReason == null ? "" : rejectReason.trim();
		if (reason.isEmpty()) {
			throw new IllegalArgumentException("반려 사유를 입력하세요.");
		}
		if (reason.length() > 500) {
			throw new IllegalArgumentException("반려 사유는 500자 이내로 입력하세요.");
		}
		requireRequested(leaveId, "반려");

		EgovMap rejectParam = new EgovMap();
		rejectParam.put("leaveId", leaveId.trim());
		rejectParam.put("approverId", approverId);
		rejectParam.put("rejectReason", reason);
		if (leaveMapper.rejectLeave(rejectParam) == 0) {
			throw new IllegalStateException("이미 처리된 휴가입니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void cancelLeave(String leaveId, String userId) throws Exception {
		EgovMap detail = selectLeaveDetail(leaveId);
		if (detail == null) {
			throw new IllegalArgumentException("휴가 신청이 없습니다.");
		}
		if (!userId.equals(trim(detail.get("userId")))) {
			throw new IllegalStateException("본인의 휴가만 취소할 수 있습니다.");
		}
		String status = trim(detail.get("status"));
		if (!STATUS_REQUESTED.equals(status) && !STATUS_APPROVED.equals(status)) {
			throw new IllegalStateException("대기 또는 승인된 휴가만 취소할 수 있습니다.");
		}

		if (STATUS_APPROVED.equals(status)) {
			EgovMap balanceParam = new EgovMap();
			balanceParam.put("userId", userId);
			balanceParam.put("leaveYear", trim(detail.get("startDe")).substring(0, 4));
			balanceParam.put("days", BigDecimal.valueOf(toDouble(detail.get("days"))));
			if (leaveMapper.decreaseUsedDays(balanceParam) == 0) {
				throw new IllegalStateException("연차 정보를 찾을 수 없어 취소할 수 없습니다.");
			}
		}

		EgovMap cancelParam = new EgovMap();
		cancelParam.put("leaveId", leaveId.trim());
		cancelParam.put("userId", userId);
		cancelParam.put("status", status);
		if (leaveMapper.cancelLeave(cancelParam) == 0) {
			throw new IllegalStateException("이미 처리된 휴가입니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void saveBalance(String userId, String leaveYear, BigDecimal totalDays) throws Exception {
		if (isBlank(userId)) {
			throw new IllegalArgumentException("사용자를 선택하세요.");
		}
		if (!isYear(leaveYear)) {
			throw new IllegalArgumentException("연도는 YYYY 형식으로 입력하세요.");
		}
		if (totalDays == null) {
			throw new IllegalArgumentException("연차 일수를 입력하세요.");
		}
		BigDecimal normalized = totalDays.stripTrailingZeros();
		if (normalized.scale() > 1) {
			throw new IllegalArgumentException("연차 일수는 소수점 한 자리까지 입력하세요.");
		}
		if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(new BigDecimal("999.9")) > 0) {
			throw new IllegalArgumentException("연차 일수는 0 이상 999.9 이하로 입력하세요.");
		}
		if (leaveMapper.selectActiveUser(userId.trim()) == null) {
			throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
		}

		EgovMap param = new EgovMap();
		param.put("userId", userId.trim());
		param.put("leaveYear", leaveYear);
		param.put("totalDays", totalDays);
		if (leaveMapper.countBalance(param) > 0) {
			if (leaveMapper.updateBalance(param) == 0) {
				throw new IllegalStateException("이미 사용한 연차보다 적게 부여할 수 없습니다.");
			}
		} else {
			leaveMapper.insertBalance(param);
		}
	}

	private EgovMap requireRequested(String leaveId, String action) throws Exception {
		EgovMap detail = selectLeaveDetail(leaveId);
		if (detail == null) {
			throw new IllegalArgumentException("휴가 신청이 없습니다.");
		}
		if (!STATUS_REQUESTED.equals(trim(detail.get("status")))) {
			throw new IllegalStateException("대기 상태의 휴가만 " + action + "할 수 있습니다.");
		}
		return detail;
	}

	private EgovMap holidayParam(LocalDate start, LocalDate end) {
		EgovMap param = new EgovMap();
		param.put("fromDe", start.format(DateTimeFormatter.BASIC_ISO_DATE));
		param.put("toDe", end.format(DateTimeFormatter.BASIC_ISO_DATE));
		return param;
	}

	private boolean isYear(String value) {
		return value != null && value.matches("^[0-9]{4}$");
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private double toDouble(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return value == null ? 0d : Double.parseDouble(String.valueOf(value));
	}

	private String formatDays(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}
}
