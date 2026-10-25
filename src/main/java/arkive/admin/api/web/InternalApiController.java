package arkive.admin.api.web;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 내부 연동 API v1.
 *
 * <p>세션·JWT가 아니라 {@code X-Internal-Token} 서비스 토큰으로 인증한다. 토큰이 없으면
 * InternalApiAuthFilter가 요청을 막는다. 개인정보를 노출하지 않는 상태·공지 조회만 제공한다.</p>
 */
@Tag(name = "내부 API", description = "서비스 토큰 기반 내부 연동 API")
@Controller
@RequestMapping("/internal/v1")
public class InternalApiController {

	private static final Logger logger = LoggerFactory.getLogger(InternalApiController.class);

	@Resource(name = "boardService")
	private BoardService boardService;

	@Operation(summary = "내부 API 상태 확인")
	@RequestMapping(value = "/health", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> health() {
		Map<String, Object> result = baseResult();
		result.put("status", "UP");
		result.put("timestamp", OffsetDateTime.now().toString());
		return result;
	}

	@Operation(summary = "내부 API 공지사항 최신 10건")
	@RequestMapping(value = "/notices", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> notices() {
		Map<String, Object> result = baseResult();
		try {
			EgovMap param = new EgovMap();
			param.put("boardType", "CK");
			param.put("searchKeyword", "");
			param.put("firstIndex", 0);
			param.put("recordCountPerPage", 10);
			List<EgovMap> rows = boardService.selectBoardList(param);
			result.put("success", true);
			result.put("list", rows);
			result.put("total", boardService.selectBoardListTotCnt(param));
		} catch (Exception e) {
			logger.error("internal notices failed", e);
			result.put("success", false);
			result.put("message", "공지사항을 조회하지 못했습니다.");
		}
		return result;
	}

	private Map<String, Object> baseResult() {
		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("apiVersion", "v1");
		return result;
	}
}
