package arkive.admin.duty.web;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.ExcelUtil;
import arkive.admin.duty.service.DutyService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 당직관리(단독). 엑셀 다운로드·양식·일괄등록을 같은 Controller에 둔다.
 * 참고: gcsms DutyScheduleController(dutyScheduleListSch/setDutyScheduleReg/downloadExcel)
 * + ExcelUtil.createWorkBook + test/ExcelRead 읽기 패턴. 승인연동 제외.
 */
@Tag(name = "당직", description = "당직 월 목록·등록·수정·삭제, 엑셀 다운로드·일괄등록")
@Controller
@RequestMapping("/duty")
public class DutyController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "dutyService")
	private DutyService dutyService;

	private CommUtil cmmUtil = new CommUtil();

	/** 엑셀 일괄등록 고정 형식: 날짜(YYYYMMDD) | 당직자 | 비고 */
	private static final String[] EXCEL_HEADER = { "날짜(YYYYMMDD)", "당직자", "비고" };
	private static final String[] EXCEL_COLS = { "dutyDe", "dutyUser", "note" };

	@Operation(summary = "당직 월 목록")
	@RequestMapping(value = "/dutyList", method = { RequestMethod.GET, RequestMethod.POST })
	public String dutyList(HttpServletRequest request, ModelMap model) throws Exception {
		String yyyymm = cmmUtil.convertHtml(request, "yyyymm");
		if (yyyymm != null) {
			yyyymm = yyyymm.replace("-", "");
		}
		if (yyyymm == null || !yyyymm.matches("^[12][0-9]{5}$")) {
			yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		}
		EgovMap param = new EgovMap();
		param.put("yyyymm", yyyymm);
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		model.put("yyyymm", yyyymm);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("resultList", dutyService.selectDutyList(param));
		return "duty/dutyList";
	}

	@Operation(summary = "당직 쓰기 화면 (dutyId 있으면 수정)")
	@RequestMapping(value = "/dutyWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String dutyWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String dutyId = cmmUtil.convertHtml(request, "dutyId");
		if (dutyId != null && !dutyId.isEmpty()) {
			model.put("detail", dutyService.selectDutyDetail(dutyId));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_DUTY"));
		return "duty/dutyWrite";
	}

	private void checkDuty(EgovMap param) {
		String dutyDe = String.valueOf(param.get("dutyDe"));
		if (dutyDe != null) {
			dutyDe = dutyDe.replace("-", "");
			param.put("dutyDe", dutyDe);
		}
		String dutyUser = String.valueOf(param.get("dutyUser"));
		if (dutyDe == null || !dutyDe.matches(ExcelUtil.YYYYMMDD_PATTERN)) {
			throw new IllegalArgumentException("당직 날짜를 YYYYMMDD 형식으로 입력하세요.");
		}
		if (dutyUser == null || dutyUser.trim().isEmpty()) {
			throw new IllegalArgumentException("당직자를 입력하세요.");
		}
	}

	@Operation(summary = "당직 등록")
	@RequestMapping(value = "/insertDuty.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertDuty(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_DUTY");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("dutyId", "DT" + CommUtil.getFileId());
			param.put("dutyDe", cmmUtil.convertHtml(request, "dutyDe"));
			param.put("dutyUser", cmmUtil.convertHtml(request, "dutyUser"));
			param.put("note", cmmUtil.convertHtml(request, "note"));
			checkDuty(param);
			param.put("register", "SYSTEM");
			dutyService.insertDuty(param);
			result.put("success", true);
			result.put("dutyId", param.get("dutyId"));
		} catch (Exception e) {
			logger.error("insertDuty failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "당직 수정")
	@RequestMapping(value = "/updateDuty.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateDuty(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("dutyId", cmmUtil.convertHtml(request, "dutyId"));
			param.put("dutyDe", cmmUtil.convertHtml(request, "dutyDe"));
			param.put("dutyUser", cmmUtil.convertHtml(request, "dutyUser"));
			param.put("note", cmmUtil.convertHtml(request, "note"));
			checkDuty(param);
			param.put("updusr", "SYSTEM");
			dutyService.updateDuty(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateDuty failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "당직 삭제")
	@RequestMapping(value = "/deleteDuty.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteDuty(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			dutyService.deleteDuty(cmmUtil.convertHtml(request, "dutyId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteDuty failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "당직 월 엑셀 다운로드 (목록 그대로)")
	@RequestMapping(value = "/downloadDutyExcel.do", method = RequestMethod.GET)
	public void downloadDutyExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String yyyymm = cmmUtil.convertHtml(request, "yyyymm");
		if (yyyymm != null) {
			yyyymm = yyyymm.replace("-", "");
		}
		if (yyyymm == null || !yyyymm.matches("^[12][0-9]{5}$")) {
			yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		}
		EgovMap param = new EgovMap();
		param.put("yyyymm", yyyymm);
		param.put("searchKeyword", "");
		List<EgovMap> list = dutyService.selectDutyList(param);

		HSSFWorkbook wb = ExcelUtil.createWorkBook("당직", yyyymm + " 당직표", EXCEL_HEADER, EXCEL_COLS, list);
		String fileName = URLEncoder.encode(yyyymm + "_당직표.xls", StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		wb.write(response.getOutputStream());
		// pom의 POI 3.8 HSSFWorkbook에는 close()가 없어 호출하지 않는다.
	}

	@Operation(summary = "당직 일괄등록 양식 다운로드 (헤더만)")
	@RequestMapping(value = "/downloadDutyTemplate.do", method = RequestMethod.GET)
	public void downloadDutyTemplate(HttpServletResponse response) throws Exception {
		HSSFWorkbook wb = ExcelUtil.createWorkBook("당직양식", "당직 일괄등록 양식", EXCEL_HEADER, EXCEL_COLS, new ArrayList<EgovMap>());
		String fileName = URLEncoder.encode("당직_일괄등록_양식.xls", StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		wb.write(response.getOutputStream());
		// pom의 POI 3.8 HSSFWorkbook에는 close()가 없어 호출하지 않는다.
	}

	/** ExcelUtil.getCellResult는 빈 칸에서 null을 돌려준다. NPE 방지로 감싼다. */
	private String cellStr(Row row, int idx) {
		String v = ExcelUtil.getCellResult(row, idx);
		return v == null ? "" : v.trim();
	}

	@Operation(summary = "당직 엑셀 일괄등록 (양식 고정 형식, 행별 검증)")
	@RequestMapping(value = "/uploadDutyExcel.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> uploadDutyExcel(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		int okCnt = 0;
		List<String> errors = new ArrayList<>();
		try {
			MultipartFile file = null;
			if (request instanceof MultipartHttpServletRequest multipart) {
				file = multipart.getFile("file");
			}
			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("업로드할 엑셀 파일이 없습니다.");
			}
			// pom의 POI 3.8 Workbook은 AutoCloseable이 아니라 try-with-resources를 쓰지 않는다.
			// ByteArrayInputStream은 close가 no-op이라 별도 종료 처리가 필요 없다.
			Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()));
			Sheet sheet = wb.getSheetAt(0);
			int lastRow = sheet.getLastRowNum();
			// 대용량 메모리 주의: 1000행까지만 처리한다.
			if (lastRow > 1000) {
				throw new IllegalArgumentException("한 번에 1000행까지만 등록할 수 있습니다.");
			}
			for (int i = 1; i <= lastRow; i++) {
				Row row = sheet.getRow(i);
				if (row == null) {
					continue;
				}
				String dutyDe = cellStr(row, 0).replace("-", "");
				String dutyUser = cellStr(row, 1);
				String note = cellStr(row, 2);
				if (dutyDe.isEmpty() && dutyUser.isEmpty()) {
					continue;
				}
				try {
					EgovMap param = new EgovMap();
					param.put("dutyId", "DT" + CommUtil.getFileId());
					param.put("dutyDe", dutyDe);
					param.put("dutyUser", dutyUser);
					param.put("note", note);
					param.put("register", "SYSTEM");
					checkDuty(param);
					dutyService.insertDuty(param);
					okCnt++;
				} catch (Exception e) {
					// 한 행 실패가 전체를 깨뜨리지 않게 행별로 수집한다.
					String msg = e.getMessage() == null ? e.toString() : e.getMessage();
					errors.add((i + 1) + "행: " + msg);
				}
			}
			result.put("success", true);
			result.put("okCnt", okCnt);
			result.put("failCnt", errors.size());
			result.put("errors", errors);
		} catch (Exception e) {
			logger.error("uploadDutyExcel failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
			result.put("okCnt", okCnt);
			result.put("errors", errors);
		}
		return result;
	}
}
