package arkive.admin.leave.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.leave.service.LeaveDaysCalculator;
import arkive.admin.leave.service.LeaveService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 휴가관리.
 *
 * <p>내 휴가 화면은 로그인 사용자 본인 것만 조회·신청·취소한다.
 * 휴가관리(승인·반려)와 연차관리(부여)는 메뉴 WRITE 권한이 있는 사용자만
 * 호출할 수 있도록 MenuPermissionInterceptor가 서버에서 검사한다.
 * 승인 연계는 별도 결재 시스템 없이 이 화면에서 처리한다(약식결재).
 */
@Tag(name = "휴가관리", description = "내 휴가 신청·취소, 휴가 승인·반려, 연차 부여")
@Controller
@RequestMapping("/leave")
public class LeaveController {

	private static final Set<String> STATUSES = Set.of("REQUESTED", "APPROVED", "REJECTED", "CANCELLED");

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "leaveService")
	private LeaveService leaveService;

	private CommUtil cmmUtil = new CommUtil();

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	@Operation(summary = "내 휴가 (연차 현황·본인 신청 목록)")
	@RequestMapping(value = "/myLeave", method = { RequestMethod.GET, RequestMethod.POST })
	public String myLeave(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String userId = String.valueOf(loginUser.get("userId"));
		String leaveYear = resolveYear(cmmUtil.convertHtml(request, "leaveYear"), true);

		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("leaveYear", leaveYear);
		model.put("leaveYear", leaveYear);
		model.put("balance", leaveService.selectBalance(userId, leaveYear));
		model.put("resultList", leaveService.selectMyLeaveList(param));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "LEAVE"));
		return "leave/myLeave";
	}

	@Operation(summary = "휴가관리 (전체 신청 목록·승인/반려)")
	@RequestMapping(value = "/leaveList", method = { RequestMethod.GET, RequestMethod.POST })
	public String leaveList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);

		String leaveYear = resolveYear(cmmUtil.convertHtml(request, "leaveYear"), false);
		String status = cmmUtil.convertHtml(request, "status");
		if (!STATUSES.contains(status)) {
			status = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("leaveYear", leaveYear);
		param.put("status", status);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("leaveYear", leaveYear);
		model.put("status", status);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", leaveService.selectLeaveList(param));
		paginationInfo.setTotalRecordCount(leaveService.selectLeaveListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "leave/leaveList";
	}

	@Operation(summary = "연차관리 (사용자별 부여·사용 현황)")
	@RequestMapping(value = "/balanceList", method = { RequestMethod.GET, RequestMethod.POST })
	public String balanceList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);

		String leaveYear = resolveYear(cmmUtil.convertHtml(request, "leaveYear"), true);
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("leaveYear", leaveYear);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("leaveYear", leaveYear);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", leaveService.selectBalanceList(param));
		paginationInfo.setTotalRecordCount(leaveService.selectBalanceListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		model.put("userList", leaveService.selectActiveUserList());
		return "leave/balanceList";
	}

	@Operation(summary = "휴가 신청")
	@RequestMapping(value = "/insertLeave.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertLeave(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String leaveType = cmmUtil.convertHtml(request, "leaveType");
			String startDe = cmmUtil.convertHtml(request, "startDe");
			String endDe = cmmUtil.convertHtml(request, "endDe");
			String reason = cmmUtil.convertHtml(request, "reason");
			if (!LeaveDaysCalculator.isValidType(leaveType)) {
				throw new IllegalArgumentException("휴가 유형이 올바르지 않습니다.");
			}
			if (!isDate(startDe) || !isDate(endDe)) {
				throw new IllegalArgumentException("휴가 기간을 YYYY-MM-DD 형식으로 입력하세요.");
			}
			if (reason == null || reason.trim().isEmpty()) {
				throw new IllegalArgumentException("휴가 사유를 입력하세요.");
			}
			if (reason.length() > 500) {
				throw new IllegalArgumentException("휴가 사유는 500자 이내로 입력하세요.");
			}
			// 입력 형식이 맞은 뒤 토큰을 소모한다. 형식 오류로는 토큰을 잃지 않게 한다.
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("LEAVE");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 내 휴가 목록에서 신청 여부를 확인하세요.");
			}

			EgovMap param = new EgovMap();
			param.put("userId", String.valueOf(loginUser.get("userId")));
			param.put("leaveType", leaveType.trim());
			param.put("startDe", startDe.trim());
			param.put("endDe", endDe.trim());
			param.put("reason", reason.trim());
			leaveService.insertLeave(param);
			result.put("success", true);
			result.put("leaveId", param.get("leaveId"));
		} catch (Exception e) {
			logger.error("insertLeave failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "휴가 취소 (본인, 대기/승인 건)")
	@RequestMapping(value = "/cancelLeave.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelLeave(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String leaveId = cmmUtil.convertHtml(request, "leaveId");
			if (leaveId == null || leaveId.trim().isEmpty()) {
				throw new IllegalArgumentException("휴가 ID가 없습니다.");
			}
			leaveService.cancelLeave(leaveId.trim(), String.valueOf(loginUser.get("userId")));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("cancelLeave failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "휴가 승인 (잔여 연차 차감)")
	@RequestMapping(value = "/approveLeave.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> approveLeave(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String leaveId = cmmUtil.convertHtml(request, "leaveId");
			if (leaveId == null || leaveId.trim().isEmpty()) {
				throw new IllegalArgumentException("휴가 ID가 없습니다.");
			}
			leaveService.approveLeave(leaveId.trim(), String.valueOf(loginUser.get("userId")));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("approveLeave failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "휴가 반려")
	@RequestMapping(value = "/rejectLeave.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> rejectLeave(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String leaveId = cmmUtil.convertHtml(request, "leaveId");
			String rejectReason = cmmUtil.convertHtml(request, "rejectReason");
			if (leaveId == null || leaveId.trim().isEmpty()) {
				throw new IllegalArgumentException("휴가 ID가 없습니다.");
			}
			leaveService.rejectLeave(leaveId.trim(), String.valueOf(loginUser.get("userId")), rejectReason);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("rejectLeave failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "연차 부여 (사용자·연도별 총 연차 저장)")
	@RequestMapping(value = "/saveBalance.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveBalance(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (getLoginUser(request) == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String userId = cmmUtil.convertHtml(request, "userId");
			String leaveYear = cmmUtil.convertHtml(request, "leaveYear");
			String totalDaysText = cmmUtil.convertHtml(request, "totalDays");
			if (totalDaysText == null || totalDaysText.trim().isEmpty()) {
				throw new IllegalArgumentException("연차 일수를 입력하세요.");
			}
			BigDecimal totalDays;
			try {
				totalDays = new BigDecimal(totalDaysText.trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("연차 일수를 숫자로 입력하세요.");
			}
			leaveService.saveBalance(userId, leaveYear, totalDays);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("saveBalance failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private int pageIndex(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private String resolveYear(String value, boolean defaultCurrent) {
		if (value != null && value.matches("^[0-9]{4}$")) {
			return value;
		}
		return defaultCurrent ? String.valueOf(LocalDate.now().getYear()) : "";
	}

	private boolean isDate(String value) {
		return value != null && value.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
	}
}
