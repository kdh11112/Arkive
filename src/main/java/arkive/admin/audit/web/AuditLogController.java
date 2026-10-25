package arkive.admin.audit.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.audit.service.AuditLogService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 로그관리. 로그인·메뉴·개인정보 열람·다운로드 감사 로그를 조회하고 엑셀로 내려받는다.
 */
@Tag(name = "로그관리", description = "감사 로그 조회·엑셀 다운로드")
@Controller
@RequestMapping("/system")
public class AuditLogController {

	private static final String[][] EXCEL_COLUMNS = {
			{ "이벤트", "eventType" }, { "사용자", "userId" }, { "이름", "userNm" },
			{ "메뉴", "menuId" }, { "대상", "targetType" }, { "대상ID", "targetId" },
			{ "요청", "requestUri" }, { "결과", "result" }, { "일시", "registDtText" } };

	@Resource(name = "auditLogService")
	private AuditLogService auditLogService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "감사 로그 목록 (기간·이벤트·키워드)")
	@RequestMapping(value = "/auditLogList", method = { RequestMethod.GET, RequestMethod.POST })
	public String auditLogList(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap param = searchParam(request);
		PaginationInfo paginationInfo = pagination(request, param, 20);
		model.put("resultList", auditLogService.selectAuditLogList(param));
		paginationInfo.setTotalRecordCount(auditLogService.selectAuditLogListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		model.put("pageIndex", paginationInfo.getCurrentPageNo());
		model.put("eventType", param.get("eventType"));
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("startDate", param.get("startDateText"));
		model.put("endDate", param.get("endDateText"));
		model.put("eventTypes", eventTypes());
		return "system/auditLogList";
	}

	@Operation(summary = "감사 로그 엑셀 다운로드")
	@RequestMapping(value = "/downloadAuditLogExcel.do", method = RequestMethod.GET)
	public void downloadAuditLogExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
		EgovMap param = searchParam(request);
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 5000);
		List<EgovMap> rows = auditLogService.selectAuditLogList(param);
		for (EgovMap row : rows) {
			row.put("registDtText", formatDate(row.get("registDt")));
			Object userNm = row.get("userNm");
			if (userNm != null) {
				row.put("userNm", arkive.admin.comm.web.MaskingUtil.mask("userNm", String.valueOf(userNm)));
			}
		}
		String[] header = new String[EXCEL_COLUMNS.length];
		String[] cols = new String[EXCEL_COLUMNS.length];
		for (int i = 0; i < EXCEL_COLUMNS.length; i++) {
			header[i] = EXCEL_COLUMNS[i][0];
			cols[i] = EXCEL_COLUMNS[i][1];
		}
		HSSFWorkbook wb = ExcelUtil.createWorkBook("감사로그", "감사 로그", header, cols, rows);
		String fileName = URLEncoder.encode("감사로그.xls", StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		wb.write(response.getOutputStream());
	}

	private EgovMap searchParam(HttpServletRequest request) {
		String start = cmmUtil.convertHtml(request, "startDate");
		String end = cmmUtil.convertHtml(request, "endDate");
		LocalDate startDate = parseDate(start, LocalDate.now().minusDays(6));
		LocalDate endDate = parseDate(end, LocalDate.now());
		if (endDate.isBefore(startDate)) {
			LocalDate tmp = startDate;
			startDate = endDate;
			endDate = tmp;
		}
		String eventType = cmmUtil.convertHtml(request, "eventType");
		EgovMap param = new EgovMap();
		param.put("eventType", eventType == null ? "" : eventType.trim());
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("startDate", Timestamp.valueOf(startDate.atStartOfDay()));
		param.put("endDate", Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
		param.put("startDateText", startDate.toString());
		param.put("endDateText", endDate.toString());
		return param;
	}

	private PaginationInfo pagination(HttpServletRequest request, EgovMap param, int pageUnit) {
		int pageIndex = 1;
		try {
			pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			if (pageIndex < 1) {
				pageIndex = 1;
			}
		} catch (NumberFormatException ignored) {
			pageIndex = 1;
		}
		PaginationInfo info = new PaginationInfo();
		info.setCurrentPageNo(pageIndex);
		info.setRecordCountPerPage(pageUnit);
		info.setPageSize(10);
		param.put("firstIndex", info.getFirstRecordIndex());
		param.put("recordCountPerPage", pageUnit);
		return info;
	}

	private LocalDate parseDate(String value, LocalDate fallback) {
		try {
			return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private String formatDate(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof java.util.Date date) {
			return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		}
		return String.valueOf(value);
	}

	private List<String> eventTypes() {
		List<String> types = new ArrayList<>();
		types.add("LOGIN_SUCCESS");
		types.add("LOGIN_FAIL");
		types.add("LOGOUT");
		types.add("API_LOGIN");
		types.add("API_LOGIN_FAIL");
		types.add("TOTP_CHALLENGE");
		types.add("TOTP_FAIL");
		types.add("TERMS_CONSENT");
		types.add("MENU_ACCESS");
		types.add("PII_VIEW");
		types.add("PII_DOWNLOAD");
		types.add("API_ACCESS");
		return types;
	}
}
