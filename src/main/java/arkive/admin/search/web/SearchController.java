package arkive.admin.search.web;

import java.util.Collections;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 통합검색 1단계. 로그인 사용자가 게시판·FAQ·Q&A를 한 번에 검색한다.
 *
 * <p>1단계는 제목 LIKE 검색이다. 본문은 에디터 HTML이라 LIKE 대상에서 제외하고 결과 요약에만 쓴다.
 * 검색 결과의 상세 링크는 외부 사용자 화면을 사용한다.
 * Q&A의 내·외부 공개 구분은 아직 정책이 확정되지 않아 현재 공개 범위 그대로 검색한다.</p>
 */
@Tag(name = "통합검색", description = "게시판·FAQ·Q&A 통합검색 (DB LIKE)")
@Controller
public class SearchController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "searchService")
	private SearchService searchService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "통합검색 (로그인 필요)")
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public String search(HttpServletRequest request, ModelMap model) throws Exception {
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword").trim();

		int pageIndex = 1;
		try {
			pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			if (pageIndex < 1) {
				pageIndex = 1;
			}
		} catch (NumberFormatException e) {
			pageIndex = 1;
		}

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);

		boolean searched = !searchKeyword.isEmpty();
		List<EgovMap> resultList = Collections.emptyList();
		if (searched) {
			EgovMap param = new EgovMap();
			param.put("searchKeyword", searchKeyword);
			param.put("firstIndex", paginationInfo.getFirstRecordIndex());
			param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
			resultList = searchService.selectSearchList(param);
			paginationInfo.setTotalRecordCount(searchService.selectSearchListTotCnt(param));
		}

		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("searched", searched);
		model.put("resultList", resultList);
		model.put("paginationInfo", paginationInfo);
		return "search/searchList";
	}
}
