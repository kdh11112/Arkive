package arkive.user.main.web;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.board.service.BoardService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.faq.service.FaqService;
import arkive.admin.poll.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 외부 메인. 팝업·배너는 activePopup.json/activeBanner.json으로 그린다.
 * KRDS 메인 구성(히어로+최근 소식+서비스 카드+참여 배너)을 따른다.
 */
@Tag(name = "사용자 메인", description = "외부 메인 화면")
@Controller
@RequestMapping("/user")
public class UserMainController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "pollService")
	private PollService pollService;

	@Resource(name = "faqService")
	private FaqService faqService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "외부 메인")
	@RequestMapping(value = "/userMain", method = { RequestMethod.GET, RequestMethod.POST })
	public String userMain(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap recentParam = new EgovMap();
		recentParam.put("searchKeyword", "");
		recentParam.put("firstIndex", 0);
		recentParam.put("recordCountPerPage", 5);
		recentParam.put("boardType", "CK");
		model.put("recentNotice", boardService.selectBoardList(recentParam));

		EgovMap toastParam = new EgovMap();
		toastParam.put("searchKeyword", "");
		toastParam.put("firstIndex", 0);
		toastParam.put("recordCountPerPage", 5);
		toastParam.put("boardType", "TOAST");
		model.put("recentArchive", boardService.selectBoardList(toastParam));

		EgovMap faqParam = new EgovMap();
		faqParam.put("searchKeyword", "");
		faqParam.put("firstIndex", 0);
		faqParam.put("recordCountPerPage", 5);
		model.put("recentFaq", faqService.selectFaqList(faqParam));

		EgovMap pollParam = new EgovMap();
		pollParam.put("searchKeyword", "");
		pollParam.put("useYn", "Y");
		pollParam.put("firstIndex", 0);
		pollParam.put("recordCountPerPage", 3);
		model.put("activePolls", pollService.selectPollList(pollParam));
		return "user/userMain";
	}
}
