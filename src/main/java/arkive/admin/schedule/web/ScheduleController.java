package arkive.admin.schedule.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.schedule.service.ScheduleService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 일정관리(부서/개인/일지/할일) + 공휴일관리. 월/주/일은 기간 조건만 다른 단일 쿼리로 조회한다.
 * 근거: eGov v5.0 cop 부서일정·일정·일지·메모할일 + sym 공휴일(달력).
 * 타 프로젝트에 범용 일정 모듈이 없어(서베이) Arkive-local로 만들었다.
 * 날짜는 DB 함수 없이 문자열(YYYY-MM-DD, YYYY-MM-DD HH24:MI)로 통일해 DB 이식을 단순화했다.
 */
@Tag(name = "일정", description = "일정 월/주/일 조회·등록·수정·삭제, 공휴일 관리")
@Controller
@RequestMapping("/schedule")
public class ScheduleController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "scheduleService")
	private ScheduleService scheduleService;

	private CommUtil cmmUtil = new CommUtil();

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

	private LocalDate parseDate(String date) {
		try {
			return LocalDate.parse(date, DAY);
		} catch (Exception e) {
			return LocalDate.now();
		}
	}

	private void checkDateTime(String dt, String label) {
		if (dt == null || !dt.matches("^[12][0-9]{3}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]) ([01][0-9]|2[0-3]):[0-5][0-9]$")) {
			throw new IllegalArgumentException(label + "을 YYYY-MM-DD HH24:MI 형식으로 입력하세요.");
		}
	}

	private boolean overlapsDay(String startDt, String endDt, String day) {
		String s = startDt.length() >= 10 ? startDt.substring(0, 10) : startDt;
		String e = endDt.length() >= 10 ? endDt.substring(0, 10) : endDt;
		return s.compareTo(day) <= 0 && e.compareTo(day) >= 0;
	}

	@Operation(summary = "일정 목록 (month/week/day 보기, 달력 그리드 포함)")
	@RequestMapping(value = "/scheduleList", method = { RequestMethod.GET, RequestMethod.POST })
	public String scheduleList(HttpServletRequest request, ModelMap model) throws Exception {
		String view = cmmUtil.convertHtml(request, "view");
		if (!"week".equals(view) && !"day".equals(view)) {
			view = "month";
		}
		LocalDate base = parseDate(cmmUtil.convertHtml(request, "date"));
		String scheduleType = cmmUtil.convertHtml(request, "scheduleType");
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		String fromDt;
		String toDt;
		if ("day".equals(view)) {
			fromDt = base.format(DAY) + " 00:00";
			toDt = base.plusDays(1).format(DAY) + " 00:00";
		} else if ("week".equals(view)) {
			LocalDate sunday = base.with(DayOfWeek.SUNDAY);
			fromDt = sunday.format(DAY) + " 00:00";
			toDt = sunday.plusDays(7).format(DAY) + " 00:00";
		} else {
			LocalDate first = base.withDayOfMonth(1);
			fromDt = first.format(DAY) + " 00:00";
			toDt = first.plusMonths(1).format(DAY) + " 00:00";
		}

		EgovMap param = new EgovMap();
		param.put("fromDt", fromDt);
		param.put("toDt", toDt);
		param.put("scheduleType", scheduleType);
		param.put("searchKeyword", searchKeyword);
		List<EgovMap> rows = scheduleService.selectScheduleList(param);

		// 공휴일 맵 (해당 월). 달력에 표시한다.
		String yyyymm = base.format(MONTH);
		Map<String, String> holidayMap = new LinkedHashMap<>();
		for (EgovMap h : scheduleService.selectHolidayList(yyyymm)) {
			holidayMap.put(String.valueOf(h.get("holidayDe")), String.valueOf(h.get("holidayNm")));
		}

		// 월 달력 그리드 (일요일 시작 6주). 각 칸: 날짜·당월여부·일정·공휴일.
		List<List<Map<String, Object>>> weeks = new ArrayList<>();
		if ("month".equals(view)) {
			LocalDate first = base.withDayOfMonth(1);
			LocalDate cursor = first.with(DayOfWeek.SUNDAY);
			for (int w = 0; w < 6; w++) {
				List<Map<String, Object>> week = new ArrayList<>();
				for (int d = 0; d < 7; d++) {
					String day = cursor.format(DAY);
					List<EgovMap> dayRows = new ArrayList<>();
					for (EgovMap r : rows) {
						if (overlapsDay(String.valueOf(r.get("startDt")), String.valueOf(r.get("endDt")), day)) {
							dayRows.add(r);
						}
					}
					Map<String, Object> cell = new HashMap<>();
					cell.put("day", day);
					cell.put("dayNo", cursor.getDayOfMonth());
					cell.put("inMonth", cursor.getMonth() == base.getMonth());
					cell.put("rows", dayRows);
					cell.put("holiday", holidayMap.get(day.substring(0, 4) + day.substring(5, 7) + day.substring(8, 10)));
					week.add(cell);
					cursor = cursor.plusDays(1);
				}
				weeks.add(week);
			}
		}

		model.put("view", view);
		model.put("date", base.format(DAY));
		model.put("yyyymm", yyyymm);
		model.put("scheduleType", scheduleType);
		model.put("searchKeyword", searchKeyword);
		model.put("resultList", rows);
		model.put("weeks", weeks);
		model.put("holidayMap", holidayMap);
		return "schedule/scheduleList";
	}

	@Operation(summary = "일정 쓰기 화면 (scheduleId 있으면 수정)")
	@RequestMapping(value = "/scheduleWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String scheduleWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String scheduleId = cmmUtil.convertHtml(request, "scheduleId");
		if (scheduleId != null && !scheduleId.isEmpty()) {
			model.put("detail", scheduleService.selectScheduleDetail(scheduleId));
		} else {
			String date = cmmUtil.convertHtml(request, "date");
			model.put("defaultDate", date != null && date.matches("^[12][0-9]{3}-[0-9]{2}-[0-9]{2}$") ? date : LocalDate.now().format(DAY));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_SCHEDULE"));
		return "schedule/scheduleWrite";
	}

	@Operation(summary = "일정 등록")
	@RequestMapping(value = "/insertSchedule.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertSchedule(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_SCHEDULE");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			EgovMap param = collectScheduleParam(request, false);
			param.put("scheduleId", "SC" + CommUtil.getFileId());
			param.put("register", "SYSTEM");
			scheduleService.insertSchedule(param);
			result.put("success", true);
			result.put("scheduleId", param.get("scheduleId"));
		} catch (Exception e) {
			logger.error("insertSchedule failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "일정 수정")
	@RequestMapping(value = "/updateSchedule.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateSchedule(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = collectScheduleParam(request, true);
			param.put("updusr", "SYSTEM");
			scheduleService.updateSchedule(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateSchedule failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private EgovMap collectScheduleParam(HttpServletRequest request, boolean needId) {
		String title = cmmUtil.convertHtml(request, "title");
		String startDt = cmmUtil.convertHtml(request, "startDt");
		String endDt = cmmUtil.convertHtml(request, "endDt");
		// datetime-local 입력(YYYY-MM-DDTHH:MI)은 공백 구분으로 되돌린다.
		if (startDt != null) {
			startDt = startDt.replace("T", " ");
		}
		if (endDt != null) {
			endDt = endDt.replace("T", " ");
		}
		String scheduleType = cmmUtil.convertHtml(request, "scheduleType");
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("제목을 입력하세요.");
		}
		checkDateTime(startDt, "시작일시");
		checkDateTime(endDt, "종료일시");
		if (startDt.compareTo(endDt) > 0) {
			throw new IllegalArgumentException("시작일시가 종료일시보다 늦을 수 없습니다.");
		}
		if (!"PERSONAL".equals(scheduleType) && !"DAILY".equals(scheduleType) && !"TODO".equals(scheduleType)) {
			scheduleType = "DEPT";
		}
		EgovMap param = new EgovMap();
		if (needId) {
			param.put("scheduleId", cmmUtil.convertHtml(request, "scheduleId"));
		}
		param.put("title", title.trim());
		String content = request.getParameter("content") == null ? "" : request.getParameter("content");
		param.put("content", content);
		param.put("startDt", startDt);
		param.put("endDt", endDt);
		param.put("scheduleType", scheduleType);
		return param;
	}

	@Operation(summary = "일정 삭제")
	@RequestMapping(value = "/deleteSchedule.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteSchedule(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			scheduleService.deleteSchedule(cmmUtil.convertHtml(request, "scheduleId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteSchedule failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "공휴일 목록 (yyyymm 6자리)")
	@RequestMapping(value = "/holidayList", method = { RequestMethod.GET, RequestMethod.POST })
	public String holidayList(HttpServletRequest request, ModelMap model) throws Exception {
		String yyyymm = cmmUtil.convertHtml(request, "yyyymm");
		if (yyyymm != null) {
			yyyymm = yyyymm.replace("-", "");
		}
		if (yyyymm == null || !yyyymm.matches("^[12][0-9]{5}$")) {
			yyyymm = LocalDate.now().format(MONTH);
		}
		model.put("yyyymm", yyyymm);
		model.put("resultList", scheduleService.selectHolidayList(yyyymm));
		return "schedule/holidayList";
	}

	@Operation(summary = "공휴일 등록 (같은 날짜 재등록 시 이름 갱신)")
	@RequestMapping(value = "/insertHoliday.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertHoliday(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String holidayDe = cmmUtil.convertHtml(request, "holidayDe");
			if (holidayDe != null) {
				holidayDe = holidayDe.replace("-", "");
			}
			String holidayNm = cmmUtil.convertHtml(request, "holidayNm");
			if (holidayDe == null || !holidayDe.matches("^[12][0-9]{3}[01][0-9][0-3][0-9]$")) {
				throw new IllegalArgumentException("공휴일 날짜를 YYYYMMDD 형식으로 입력하세요.");
			}
			if (holidayNm == null || holidayNm.trim().isEmpty()) {
				throw new IllegalArgumentException("공휴일 이름을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("holidayDe", holidayDe);
			param.put("holidayNm", holidayNm.trim());
			scheduleService.insertHoliday(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("insertHoliday failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "공휴일 삭제")
	@RequestMapping(value = "/deleteHoliday.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteHoliday(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			scheduleService.deleteHoliday(cmmUtil.convertHtml(request, "holidayDe"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteHoliday failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
