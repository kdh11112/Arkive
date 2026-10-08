package arkive.user.poll.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.poll.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 외부 온라인POLL. 목록·투표만 제공한다. 등록·수정은 내부(관리)에서 한다.
 */
@Tag(name = "사용자 POLL", description = "POLL 목록·투표")
@Controller
@RequestMapping("/user/poll")
public class UserPollController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "pollService")
	private PollService pollService;

	private CommUtil cmmUtil = new CommUtil();

	@SuppressWarnings("unchecked")
	private Set<String> getVotedSet(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Object attr = session.getAttribute("votedPolls");
		if (attr instanceof Set) {
			return (Set<String>) attr;
		}
		Set<String> voted = new HashSet<>();
		session.setAttribute("votedPolls", voted);
		return voted;
	}

	@Operation(summary = "외부 POLL 목록")
	@RequestMapping(value = "/pollList", method = { RequestMethod.GET, RequestMethod.POST })
	public String pollList(HttpServletRequest request, ModelMap model) throws Exception {
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
		param.put("useYn", "Y");
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", pollService.selectPollList(param));
		paginationInfo.setTotalRecordCount(pollService.selectPollListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "user/poll/pollList";
	}

	@Operation(summary = "외부 POLL 상세 (투표 + 결과)")
	@RequestMapping(value = "/pollDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String pollDetail(HttpServletRequest request, ModelMap model) throws Exception {
		String pollId = cmmUtil.convertHtml(request, "pollId");
		model.put("detail", pollService.selectPollDetail(pollId));
		model.put("itemList", pollService.selectPollItemList(pollId));
		model.put("voted", getVotedSet(request).contains(pollId));
		return "user/poll/pollDetail";
	}

	@Operation(summary = "외부 POLL 투표 (기간·사용여부·세션 중복 검사)")
	@RequestMapping(value = "/votePoll.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> votePoll(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String pollId = cmmUtil.convertHtml(request, "pollId");
			String itemId = cmmUtil.convertHtml(request, "itemId");
			if (pollId == null || pollId.isEmpty() || itemId == null || itemId.isEmpty()) {
				throw new IllegalArgumentException("투표 정보가 올바르지 않습니다.");
			}
			EgovMap detail = pollService.selectPollDetail(pollId);
			if (detail == null || !"Y".equals(String.valueOf(detail.get("useYn")))) {
				throw new IllegalStateException("진행 중인 투표가 아닙니다.");
			}
			String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			if (today.compareTo(String.valueOf(detail.get("startYmd"))) < 0
					|| today.compareTo(String.valueOf(detail.get("endYmd"))) > 0) {
				throw new IllegalStateException("투표 기간이 아닙니다.");
			}
			Set<String> voted = getVotedSet(request);
			if (voted.contains(pollId)) {
				throw new IllegalStateException("이미 투표했습니다.");
			}
			pollService.votePollItem(itemId);
			voted.add(pollId);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("votePoll failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
