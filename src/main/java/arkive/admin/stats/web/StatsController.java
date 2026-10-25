package arkive.admin.stats.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.ExcelUtil;
import arkive.admin.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 통계. 게시판 건수, 감사 이벤트, 일별 로그인·접속, 다운로드 현황을 집계한다.
 *
 * <p>접속 통계는 AUDIT_LOG의 요청 이벤트를 기준으로 한다. 개인정보 원문은 집계하지 않는다.</p>
 */
@Tag(name = "통계", description = "게시판·접속·다운로드 통계")
@Controller
@RequestMapping("/stats")
public class StatsController {

	@Resource(name = "statsService")
	private StatsService statsService;

	private final CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "통계 화면 (기간별)")
	@RequestMapping(value = "/statsList", method = { RequestMethod.GET, RequestMethod.POST })
	public String statsList(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap param = rangeParam(request);
		model.put("boardStats", statsService.selectBoardStats());
		model.put("eventStats", statsService.selectEventStats(param));
		model.put("dailyLoginStats", statsService.selectDailyLoginStats(param));
		model.put("dailyAccessStats", statsService.selectDailyAccessStats(param));
		model.put("downloadStats", statsService.selectDownloadStats(param));
		model.put("startDate", param.get("startDateText"));
		model.put("endDate", param.get("endDateText"));
		return "stats/statsList";
	}

	@Operation(summary = "통계 엑셀 다운로드")
	@RequestMapping(value = "/downloadStatsExcel.do", method = RequestMethod.GET)
	public void downloadStatsExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
		EgovMap param = rangeParam(request);
		List<EgovMap> rows = new ArrayList<>();
		for (EgovMap board : statsService.selectBoardStats()) {
			rows.add(row("게시판", board.get("boardType"), board.get("cnt")));
		}
		for (EgovMap event : statsService.selectEventStats(param)) {
			rows.add(row("이벤트", event.get("eventType"), event.get("cnt")));
		}
		for (EgovMap download : statsService.selectDownloadStats(param)) {
			rows.add(row("다운로드", download.get("targetType"), download.get("cnt")));
		}
		for (EgovMap daily : statsService.selectDailyAccessStats(param)) {
			rows.add(row("일별접속", daily.get("statDate"), daily.get("cnt")));
		}
		HSSFWorkbook wb = ExcelUtil.createWorkBook("통계", "통계",
				new String[] { "구분", "항목", "건수" },
				new String[] { "category", "name", "cnt" }, rows);
		String fileName = URLEncoder.encode("통계.xls", StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		wb.write(response.getOutputStream());
	}

	private EgovMap row(String category, Object name, Object cnt) {
		EgovMap row = new EgovMap();
		row.put("category", category);
		row.put("name", name == null ? "" : String.valueOf(name));
		row.put("cnt", cnt == null ? 0 : cnt);
		return row;
	}

	private EgovMap rangeParam(HttpServletRequest request) {
		String start = cmmUtil.convertHtml(request, "startDate");
		String end = cmmUtil.convertHtml(request, "endDate");
		LocalDate startDate = parseDate(start, LocalDate.now().minusDays(6));
		LocalDate endDate = parseDate(end, LocalDate.now());
		if (endDate.isBefore(startDate)) {
			LocalDate tmp = startDate;
			startDate = endDate;
			endDate = tmp;
		}
		EgovMap param = new EgovMap();
		param.put("startDate", Timestamp.valueOf(startDate.atStartOfDay()));
		param.put("endDate", Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
		param.put("startDateText", startDate.toString());
		param.put("endDateText", endDate.toString());
		return param;
	}

	private LocalDate parseDate(String value, LocalDate fallback) {
		try {
			return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}
}
