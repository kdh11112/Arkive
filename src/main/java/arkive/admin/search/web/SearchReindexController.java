package arkive.admin.search.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.web.LoginController;
import arkive.admin.search.service.SearchIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;

/**
 * Elasticsearch 색인 재구축 관리자 도구.
 *
 * <p>색인 기능이 꺼져 있으면 아무 것도 하지 않는다. 관리자 세션이 아니면 거부한다.
 * 대량 색인은 시간이 걸릴 수 있으므로 운영에서는 배치·CLI로 옮기는 것을 권장한다.</p>
 */
@Tag(name = "통합검색", description = "검색 색인 재구축")
@Controller
public class SearchReindexController {

	@Resource(name = "searchIndexService")
	private SearchIndexService searchIndexService;

	@Operation(summary = "검색 색인 전체 재구축 (관리자)")
	@RequestMapping(value = "/search/reindex.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> reindex(HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		if (session == null || session.getAttribute(LoginController.ADMIN_KEY) == null) {
			result.put("success", false);
			result.put("message", "관리자만 실행할 수 있습니다.");
			return result;
		}
		if (!searchIndexService.isEnabled()) {
			result.put("success", false);
			result.put("message", "Elasticsearch 검색 색인이 꺼져 있습니다.");
			return result;
		}
		try {
			int count = searchIndexService.reindexAll();
			result.put("success", true);
			result.put("count", count);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "색인 재구축에 실패했습니다.");
		}
		return result;
	}
}
