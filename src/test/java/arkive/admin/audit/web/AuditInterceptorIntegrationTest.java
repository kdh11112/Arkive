package arkive.admin.audit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.audit.service.AuditLogService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 요청 인터셉터가 발행한 감사 이벤트가 리스너를 거쳐 DB에 저장되는지 확인한다.
 * 화면 접근(MENU_ACCESS), 개인정보 조회(PII_VIEW), 다운로드(PII_DOWNLOAD),
 * 비로그인 다운로드 대상 식별자 기록까지 실제 요청으로 검증한다.
 */
@ArkiveIntegrationTest
@Transactional
class AuditInterceptorIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuditLogService auditLogService;

	private List<EgovMap> find(String keyword, String eventType) throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", keyword);
		param.put("eventType", eventType);
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 50);
		return auditLogService.selectAuditLogList(param);
	}

	@Test
	@Rollback
	void menuAccessShouldBeRecordedWithMenuIdAndUri() throws Exception {
		mockMvc.perform(get("/stats/statsList").session(TestSessions.admin("auditmenu")))
				.andExpect(status().isOk());

		List<EgovMap> rows = find("auditmenu", "MENU_ACCESS");
		assertThat(rows).isNotEmpty();
		EgovMap row = rows.get(0);
		assertThat(String.valueOf(row.get("menuId"))).isEqualTo("ME02010700");
		assertThat(String.valueOf(row.get("requestUri"))).isEqualTo("/stats/statsList");
		assertThat(String.valueOf(row.get("httpMethod"))).isEqualTo("GET");
		assertThat(String.valueOf(row.get("userId"))).isEqualTo("auditmenu");
	}

	@Test
	@Rollback
	void piiViewShouldRecordTargetType() throws Exception {
		mockMvc.perform(get("/address/addressList").session(TestSessions.admin("piiUser")))
				.andExpect(status().isOk());

		List<EgovMap> rows = find("piiUser", "PII_VIEW");
		assertThat(rows).isNotEmpty();
		assertThat(String.valueOf(rows.get(0).get("targetType"))).isEqualTo("ADDRESS_BOOK");
		assertThat(String.valueOf(rows.get(0).get("menuId"))).isEqualTo("ME02031100");
	}

	@Test
	@Rollback
	void downloadShouldBeRecordedAsPiiDownload() throws Exception {
		mockMvc.perform(get("/stats/downloadStatsExcel.do").session(TestSessions.admin("dlUser")))
				.andExpect(status().isOk());

		List<EgovMap> rows = find("dlUser", "PII_DOWNLOAD");
		assertThat(rows).isNotEmpty();
		assertThat(String.valueOf(rows.get(0).get("requestUri")))
				.isEqualTo("/stats/downloadStatsExcel.do");
	}

	@Test
	@Rollback
	void anonymousDownloadShouldRecordTargetIdWithoutUser() throws Exception {
		mockMvc.perform(get("/file/downloadFile.do").param("downloadFileId", "00000000000000000000"))
				.andExpect(status().isNotFound());

		List<EgovMap> rows = find("file/downloadFile.do", "PII_DOWNLOAD");
		assertThat(rows).isNotEmpty();
		EgovMap row = rows.get(0);
		assertThat(String.valueOf(row.get("targetId"))).isEqualTo("00000000000000000000");
		assertThat(String.valueOf(row.get("targetType"))).isEqualTo("FILE");
		assertThat(row.get("userId")).isNull();
	}
}
