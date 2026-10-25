package arkive.admin.hr.web;

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
import arkive.admin.hr.service.HrService;
import arkive.admin.leave.service.LeaveService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * HR 세트. 기념일·경조사·포상 CRUD와 개인연차(휴가·당직 연계) 화면을 제공한다.
 * 메뉴 WRITE 권한은 MenuPermissionInterceptor가 서버에서 검사한다.
 */
@Tag(name = "HR 세트", description = "기념일·경조사·포상 관리, 개인연차 조회")
@Controller
@RequestMapping("/hr")
public class HrController {

	private static final Set<String> EVENT_TYPES = Set.of("WEDDING", "FUNERAL", "BIRTH", "ETC");
	private static final Set<String> REWARD_TYPES = Set.of("EXCELLENT", "TEAMWORK", "ETC");

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "hrService")
	private HrService hrService;

	@Resource(name = "leaveService")
	private LeaveService leaveService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "기념일 목록")
	@RequestMapping(value = "/anniversaryList", method = { RequestMethod.GET, RequestMethod.POST })
	public String anniversaryList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);

		String month = cmmUtil.convertHtml(request, "month");
		if (month == null) {
			month = String.format("%02d", LocalDate.now().getMonthValue());
		} else if (!month.matches("^(0[1-9]|1[0-2])$")) {
			month = "";
		}
		String useYn = cmmUtil.convertHtml(request, "useYn");
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			useYn = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("month", month);
		param.put("useYn", useYn);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("month", month);
		model.put("useYn", useYn);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", hrService.selectAnniversaryList(param));
		paginationInfo.setTotalRecordCount(hrService.selectAnniversaryListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "hr/anniversaryList";
	}

	@Operation(summary = "기념일 등록·수정 화면")
	@RequestMapping(value = "/anniversaryWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String anniversaryWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String annivId = cmmUtil.convertHtml(request, "annivId");
		if (annivId != null && !annivId.trim().isEmpty()) {
			EgovMap detail = hrService.selectAnniversaryDetail(annivId);
			if (detail == null) {
				throw new IllegalArgumentException("기념일이 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("userList", hrService.selectActiveUserList());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "HR_ANNIV"));
		return "hr/anniversaryWrite";
	}

	@Operation(summary = "기념일 등록")
	@RequestMapping(value = "/insertAnniversary.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertAnniversary(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "HR_ANNIV");
			EgovMap param = anniversaryParam(request);
			hrService.insertAnniversary(param);
			return param.get("annivId");
		});
	}

	@Operation(summary = "기념일 수정")
	@RequestMapping(value = "/updateAnniversary.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateAnniversary(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = anniversaryParam(request);
			param.put("annivId", cmmUtil.convertHtml(request, "annivId"));
			hrService.updateAnniversary(param);
			return param.get("annivId");
		});
	}

	@Operation(summary = "기념일 삭제")
	@RequestMapping(value = "/deleteAnniversary.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteAnniversary(HttpServletRequest request) {
		return execute(() -> {
			hrService.deleteAnniversary(cmmUtil.convertHtml(request, "annivId"));
			return null;
		});
	}

	@Operation(summary = "경조사 목록")
	@RequestMapping(value = "/eventList", method = { RequestMethod.GET, RequestMethod.POST })
	public String eventList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);

		String year = resolveYear(cmmUtil.convertHtml(request, "year"));
		String eventType = cmmUtil.convertHtml(request, "eventType");
		if (!EVENT_TYPES.contains(eventType)) {
			eventType = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("year", year);
		param.put("eventType", eventType);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("year", year);
		model.put("eventType", eventType);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", hrService.selectEventList(param));
		paginationInfo.setTotalRecordCount(hrService.selectEventListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "hr/eventList";
	}

	@Operation(summary = "경조사 등록·수정 화면")
	@RequestMapping(value = "/eventWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String eventWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String eventId = cmmUtil.convertHtml(request, "eventId");
		if (eventId != null && !eventId.trim().isEmpty()) {
			EgovMap detail = hrService.selectEventDetail(eventId);
			if (detail == null) {
				throw new IllegalArgumentException("경조사가 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("userList", hrService.selectActiveUserList());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "HR_EVENT"));
		return "hr/eventWrite";
	}

	@Operation(summary = "경조사 등록")
	@RequestMapping(value = "/insertEvent.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertEvent(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "HR_EVENT");
			EgovMap param = eventParam(request);
			hrService.insertEvent(param);
			return param.get("eventId");
		});
	}

	@Operation(summary = "경조사 수정")
	@RequestMapping(value = "/updateEvent.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateEvent(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = eventParam(request);
			param.put("eventId", cmmUtil.convertHtml(request, "eventId"));
			hrService.updateEvent(param);
			return param.get("eventId");
		});
	}

	@Operation(summary = "경조사 삭제")
	@RequestMapping(value = "/deleteEvent.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteEvent(HttpServletRequest request) {
		return execute(() -> {
			hrService.deleteEvent(cmmUtil.convertHtml(request, "eventId"));
			return null;
		});
	}

	@Operation(summary = "포상 목록")
	@RequestMapping(value = "/rewardList", method = { RequestMethod.GET, RequestMethod.POST })
	public String rewardList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);

		String year = resolveYear(cmmUtil.convertHtml(request, "year"));
		String rewardType = cmmUtil.convertHtml(request, "rewardType");
		if (!REWARD_TYPES.contains(rewardType)) {
			rewardType = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("year", year);
		param.put("rewardType", rewardType);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("year", year);
		model.put("rewardType", rewardType);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", hrService.selectRewardList(param));
		paginationInfo.setTotalRecordCount(hrService.selectRewardListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "hr/rewardList";
	}

	@Operation(summary = "포상 등록·수정 화면")
	@RequestMapping(value = "/rewardWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String rewardWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String rewardId = cmmUtil.convertHtml(request, "rewardId");
		if (rewardId != null && !rewardId.trim().isEmpty()) {
			EgovMap detail = hrService.selectRewardDetail(rewardId);
			if (detail == null) {
				throw new IllegalArgumentException("포상이 없습니다.");
			}
			model.put("detail", detail);
		}
		model.put("userList", hrService.selectActiveUserList());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "HR_REWARD"));
		return "hr/rewardWrite";
	}

	@Operation(summary = "포상 등록")
	@RequestMapping(value = "/insertReward.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertReward(HttpServletRequest request) {
		return execute(() -> {
			requireToken(request, "HR_REWARD");
			EgovMap param = rewardParam(request);
			hrService.insertReward(param);
			return param.get("rewardId");
		});
	}

	@Operation(summary = "포상 수정")
	@RequestMapping(value = "/updateReward.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateReward(HttpServletRequest request) {
		return execute(() -> {
			EgovMap param = rewardParam(request);
			param.put("rewardId", cmmUtil.convertHtml(request, "rewardId"));
			hrService.updateReward(param);
			return param.get("rewardId");
		});
	}

	@Operation(summary = "포상 삭제")
	@RequestMapping(value = "/deleteReward.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteReward(HttpServletRequest request) {
		return execute(() -> {
			hrService.deleteReward(cmmUtil.convertHtml(request, "rewardId"));
			return null;
		});
	}

	@Operation(summary = "개인연차 (사용자별 연차·휴가신청·당직)")
	@RequestMapping(value = "/personalLeave", method = { RequestMethod.GET, RequestMethod.POST })
	public String personalLeave(HttpServletRequest request, ModelMap model) throws Exception {
		String userId = cmmUtil.convertHtml(request, "userId");
		String leaveYear = resolveYear(cmmUtil.convertHtml(request, "leaveYear"));

		EgovMap user = hrService.selectUser(userId);
		model.put("userList", leaveService.selectActiveUserList());
		model.put("userId", userId == null ? "" : userId);
		model.put("leaveYear", leaveYear);
		if (user != null) {
			EgovMap leaveParam = new EgovMap();
			leaveParam.put("userId", String.valueOf(user.get("userId")));
			leaveParam.put("leaveYear", leaveYear);
			model.put("selectedUser", user);
			model.put("balance", leaveService.selectBalance(String.valueOf(user.get("userId")), leaveYear));
			model.put("leaveList", leaveService.selectMyLeaveList(leaveParam));

			EgovMap dutyParam = new EgovMap();
			dutyParam.put("year", leaveYear);
			dutyParam.put("userNm", String.valueOf(user.get("userNm")));
			model.put("dutyList", hrService.selectDutyListByUserName(dutyParam));
		}
		return "hr/personalLeave";
	}

	@Operation(summary = "개인연차 부여 (총 연차 저장)")
	@RequestMapping(value = "/savePersonalLeave.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> savePersonalLeave(HttpServletRequest request) {
		return execute(() -> {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
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
			if (hrService.selectUser(userId) == null) {
				throw new IllegalArgumentException("사용중인 사용자를 선택하세요.");
			}
			leaveService.saveBalance(userId, leaveYear, totalDays);
			return null;
		});
	}

	private EgovMap anniversaryParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("userId", cmmUtil.convertHtml(request, "userId"));
		param.put("annivType", cmmUtil.convertHtml(request, "annivType"));
		param.put("annivDe", cmmUtil.convertHtml(request, "annivDe"));
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("remark", cmmUtil.convertHtml(request, "remark"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		return param;
	}

	private EgovMap eventParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("userId", cmmUtil.convertHtml(request, "userId"));
		param.put("eventType", cmmUtil.convertHtml(request, "eventType"));
		param.put("eventDe", cmmUtil.convertHtml(request, "eventDe"));
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("amount", cmmUtil.convertHtml(request, "amount"));
		param.put("remark", cmmUtil.convertHtml(request, "remark"));
		return param;
	}

	private EgovMap rewardParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("userId", cmmUtil.convertHtml(request, "userId"));
		param.put("rewardType", cmmUtil.convertHtml(request, "rewardType"));
		param.put("rewardDe", cmmUtil.convertHtml(request, "rewardDe"));
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("amount", cmmUtil.convertHtml(request, "amount"));
		param.put("rewarder", cmmUtil.convertHtml(request, "rewarder"));
		param.put("remark", cmmUtil.convertHtml(request, "remark"));
		return param;
	}

	private Map<String, Object> execute(Action action) {
		Map<String, Object> result = new HashMap<>();
		try {
			Object data = action.run();
			result.put("success", true);
			if (data != null) {
				result.put("id", data);
			}
		} catch (Exception e) {
			logger.error("hr action failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private void requireToken(HttpServletRequest request, String key) {
		boolean valid;
		try {
			valid = EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			valid = false;
		}
		if (!valid) {
			throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
		}
	}

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private PaginationInfo pagination(int pageIndex) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		return paginationInfo;
	}

	private int pageIndex(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private String resolveYear(String value) {
		return value != null && value.matches("^[0-9]{4}$") ? value : String.valueOf(LocalDate.now().getYear());
	}

	@FunctionalInterface
	private interface Action {
		Object run() throws Exception;
	}
}
