package arkive.admin.duty.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.comm.web.MaskingUtil;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 당직 CRUD·월/이름 검색·엑셀 양식/다운로드 마스킹·일괄등록 행별 오류를 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class DutyControllerTest {

	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertDuty(MockHttpSession session, String dutyDe, String dutyUser, String note) throws Exception {
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_DUTY");
		MvcResult result = mockMvc.perform(post("/duty/insertDuty.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("dutyDe", dutyDe)
						.param("dutyUser", dutyUser)
						.param("note", note))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.dutyId");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> requestList(MockHttpSession session, String yyyymm, String searchKeyword)
			throws Exception {
		MvcResult result = mockMvc.perform(get("/duty/dutyList")
						.session(session)
						.param("yyyymm", yyyymm)
						.param("searchKeyword", searchKeyword))
				.andExpect(status().isOk())
				.andReturn();
		return (List<EgovMap>) result.getModelAndView().getModel().get("resultList");
	}

	private byte[] workbookBytes(String[]... rows) throws Exception {
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet("당직");
		Row title = sheet.createRow(0);
		title.createCell(0).setCellValue("당직 일괄등록");
		sheet.createRow(1);
		Row header = sheet.createRow(2);
		header.createCell(0).setCellValue("날짜(YYYYMMDD)");
		header.createCell(1).setCellValue("당직자");
		header.createCell(2).setCellValue("비고");
		for (int i = 0; i < rows.length; i++) {
			Row row = sheet.createRow(3 + i);
			for (int j = 0; j < rows[i].length; j++) {
				if (rows[i][j] != null) {
					row.createCell(j).setCellValue(rows[i][j]);
				}
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wb.write(out);
		return out.toByteArray();
	}

	@Test
	@Rollback
	void dutyShouldRoundTripAndMaskExcelDownload() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String today = LocalDate.now().format(BASIC);
		String yyyymm = LocalDate.now().format(MONTH);
		String note = "ZZ당직메모-" + System.nanoTime();
		String dutyUser = "테스트당직자";
		String dutyId = insertDuty(session, today, dutyUser, note);

		List<EgovMap> rows = requestList(session, yyyymm, dutyUser);
		assertThat(rows).extracting(row -> String.valueOf(row.get("dutyId"))).contains(dutyId);

		MvcResult download = mockMvc.perform(get("/duty/downloadDutyExcel.do")
						.session(session)
						.param("yyyymm", yyyymm))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(download.getResponse().getContentType()).startsWith("application/vnd.ms-excel");
		assertThat(download.getResponse().getHeader("Content-Disposition")).contains("attachment");

		Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(download.getResponse().getContentAsByteArray()));
		Sheet sheet = wb.getSheetAt(0);
		assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).startsWith("날짜");
		boolean found = false;
		for (int i = 3; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null || row.getCell(0) == null || row.getCell(2) == null) {
				continue;
			}
			if (today.equals(row.getCell(0).getStringCellValue())
					&& note.equals(row.getCell(2).getStringCellValue())) {
				assertThat(row.getCell(1).getStringCellValue()).isEqualTo(MaskingUtil.maskName(dutyUser));
				found = true;
			}
		}
		assertThat(found).isTrue();

		mockMvc.perform(post("/duty/updateDuty.json")
						.session(session)
						.param("dutyId", dutyId)
						.param("dutyDe", "2031-06-02")
						.param("dutyUser", dutyUser + "-수정")
						.param("note", note + "-수정"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT DUTY_DE FROM DUTY WHERE DUTY_ID = ?", String.class, dutyId)).isEqualTo("20310602");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT DUTY_USER FROM DUTY WHERE DUTY_ID = ?", String.class, dutyId))
				.isEqualTo(dutyUser + "-수정");

		mockMvc.perform(post("/duty/deleteDuty.json")
						.session(session)
						.param("dutyId", dutyId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM DUTY WHERE DUTY_ID = ?", Integer.class, dutyId)).isZero();
	}

	@Test
	@Rollback
	void insertShouldValidateTokenAndDate() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		mockMvc.perform(post("/duty/insertDuty.json")
						.session(session)
						.param("dutyDe", "20310601")
						.param("dutyUser", "당직자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("중복 제출")));

		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_DUTY");
		mockMvc.perform(post("/duty/insertDuty.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("dutyDe", "2026-1-1")
						.param("dutyUser", "당직자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("당직 날짜를 YYYYMMDD 형식으로 입력하세요."));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_DUTY");
		mockMvc.perform(post("/duty/insertDuty.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("dutyDe", "20310230")
						.param("dutyUser", "당직자"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("존재하지 않는 날짜입니다: 20310230"));

		token = EgovDoubleSubmitHelper.setToken(session, "BOARD_DUTY");
		mockMvc.perform(post("/duty/insertDuty.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("dutyDe", "20310601")
						.param("dutyUser", " "))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("당직자를 입력하세요."));

		MvcResult invalidMonth = mockMvc.perform(get("/duty/dutyList")
						.session(session)
						.param("yyyymm", "bad"))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(String.valueOf(invalidMonth.getModelAndView().getModel().get("yyyymm")))
				.isEqualTo(LocalDate.now().format(MONTH));
	}

	@Test
	@Rollback
	void excelTemplateAndUploadShouldHandleHeaderAndRowErrors() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String note = "ZZ업로드메모-" + System.nanoTime();

		MvcResult template = mockMvc.perform(get("/duty/downloadDutyTemplate.do")
						.session(session))
				.andExpect(status().isOk())
				.andReturn();
		Workbook templateWb = new HSSFWorkbook(
				new ByteArrayInputStream(template.getResponse().getContentAsByteArray()));
		Sheet templateSheet = templateWb.getSheetAt(0);
		assertThat(templateSheet.getRow(2).getCell(0).getStringCellValue())
				.isEqualTo("날짜(YYYYMMDD)");

		mockMvc.perform(multipart("/duty/uploadDutyExcel.json")
						.session(session))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("업로드할 엑셀 파일이 없습니다."));

		byte[] bytes = workbookBytes(
				new String[] { "20310601", "업로드당직", note },
				new String[] { "20310631", "오류당직", "오류메모" },
				new String[] { null, null, null });
		MockMultipartFile file = new MockMultipartFile("file", "duty.xls",
				"application/vnd.ms-excel", bytes);
		mockMvc.perform(multipart("/duty/uploadDutyExcel.json")
						.file(file)
						.session(session))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.okCnt").value(1))
				.andExpect(jsonPath("$.failCnt").value(1))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0]").value(containsString("존재하지 않는 날짜입니다: 20310631")));

		assertThat(requestList(session, "203106", "업로드당직")).hasSize(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT NOTE FROM DUTY WHERE NOTE = ?", String.class, note)).isEqualTo(note);

		Workbook tooMany = new HSSFWorkbook();
		Sheet sheet = tooMany.createSheet("당직");
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("날짜(YYYYMMDD)");
		for (int i = 1; i <= 1001; i++) {
			sheet.createRow(i);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tooMany.write(out);
		MockMultipartFile bigFile = new MockMultipartFile("file", "too-many.xls",
				"application/vnd.ms-excel", out.toByteArray());
		mockMvc.perform(multipart("/duty/uploadDutyExcel.json")
						.file(bigFile)
						.session(session))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("한 번에 1000행까지만 등록할 수 있습니다."));
	}
}
