package arkive.user.faq.web;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 외부 FAQ 목록. 내부 등록분이 보인다. 아코디언 읽기 전용이다.
 */
@Tag(name = "사용자 FAQ", description = "FAQ 목록 (읽기 전용)")
@Controller
@RequestMapping("/user/faq")
public class UserFaqController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "faqService")
	private FaqService faqService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "외부 FAQ 목록")
	@RequestMapping(value = "/faqList", method = { RequestMethod.GET, RequestMethod.POST })
	public String faqList(HttpServletRequest request, ModelMap model) throws Exception {
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

		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", faqService.selectFaqList(param));
		paginationInfo.setTotalRecordCount(faqService.selectFaqListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "user/faq/faqList";
	}
}
