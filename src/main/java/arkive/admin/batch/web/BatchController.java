package arkive.admin.batch.web;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.batch.service.BatchService;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 일일 통계 집계 배치 화면. 메뉴 권한은 MenuPermissionInterceptor가 서버에서 검사한다.
 */
@Tag(name = "배치관리", description = "Spring Batch 일일 통계 집계")
@Controller
@RequestMapping("/batch")
public class BatchController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "batchService")
	private BatchService batchService;

	private final CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "배치 화면 (집계 결과 조회)")
	@RequestMapping(value = "/batchList", method = { RequestMethod.GET, RequestMethod.POST })
	public String batchList(HttpServletRequest request, ModelMap model) throws Exception {
		LocalDate statDate = parseDate(cmmUtil.convertHtml(request, "statDate"),
				LocalDate.now().minusDays(1));
		model.put("statDate", statDate.toString());
		model.put("resultList", batchService.selectDailyStats(statDate));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BATCH_STATS"));
		return "batch/batchList";
	}

	@Operation(summary = "일일 통계 집계 실행")
	@RequestMapping(value = "/runDailyStats.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> runDailyStats(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			requireToken("BATCH_STATS");
			LocalDate statDate = parseRequiredDate(cmmUtil.convertHtml(request, "statDate"));
			EgovMap execution = batchService.runDailyStats(statDate);
			boolean completed = "COMPLETED".equals(String.valueOf(execution.get("status")));
			result.put("success", completed);
			result.put("status", execution.get("status"));
			result.put("targetDate", execution.get("targetDate"));
			if (!completed) {
				result.put("message", execution.get("message"));
			}
		} catch (Exception e) {
			logger.error("daily stats batch failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private LocalDate parseDate(String value, LocalDate fallback) {
		try {
			return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private LocalDate parseRequiredDate(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("집계 날짜를 입력하세요.");
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (Exception e) {
			throw new IllegalArgumentException("집계 날짜 형식이 올바르지 않습니다.");
		}
	}

	private void requireToken(String key) {
		boolean valid;
		try {
			valid = EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			valid = false;
		}
		if (!valid) {
			throw new IllegalStateException("중복 제출이 감지되었습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
		}
	}
}
