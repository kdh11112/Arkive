package arkive.admin.poll.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 온라인POLL. 투표 기간·사용여부 검사 + 세션 중복투표 차단을 한다.
 * 참고: GTEMS qestnrList/qestnrReg/qestnrStatsList + SystemSQL.xml#572~786.
 */
@Tag(name = "온라인POLL", description = "POLL 목록·등록·수정·삭제, 투표·결과")
@Controller
@RequestMapping("/poll")
public class PollController {

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

	@Operation(summary = "POLL 목록 (검색 + 사용여부 필터 + 10건 페이징)")
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
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("useYn", param.get("useYn"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", pollService.selectPollList(param));
		paginationInfo.setTotalRecordCount(pollService.selectPollListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "poll/pollList";
	}

	@Operation(summary = "POLL 쓰기 화면 (pollId 있으면 수정, 보기는 줄바꿈 구분)")
	@RequestMapping(value = "/pollWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String pollWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String pollId = cmmUtil.convertHtml(request, "pollId");
		if (pollId != null && !pollId.isEmpty()) {
			model.put("detail", pollService.selectPollDetail(pollId));
			List<EgovMap> items = pollService.selectPollItemList(pollId);
			model.put("itemList", items);
			StringBuilder sb = new StringBuilder();
			for (EgovMap item : items) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(String.valueOf(item.get("itemNm")));
			}
			model.put("itemsText", sb.toString());
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_POLL"));
		return "poll/pollWrite";
	}

	@Operation(summary = "POLL 상세 (투표 + 결과)")
	@RequestMapping(value = "/pollDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String pollDetail(HttpServletRequest request, ModelMap model) throws Exception {
		String pollId = cmmUtil.convertHtml(request, "pollId");
		model.put("detail", pollService.selectPollDetail(pollId));
		model.put("itemList", pollService.selectPollItemList(pollId));
		model.put("voted", getVotedSet(request).contains(pollId));
		return "poll/pollDetail";
	}

	private void checkYmd(String ymd, String label) {
		if (ymd == null || !ymd.matches("^[12][0-9]{3}[01][0-9][0-3][0-9]$")) {
			throw new IllegalArgumentException(label + "을 YYYYMMDD 형식으로 입력하세요.");
		}
	}

	private String[] parseItems(HttpServletRequest request) {
		String items = request.getParameter("items") == null ? "" : request.getParameter("items");
		String[] lines = items.split("\\r?\\n");
		List<String> result = new ArrayList<>();
		for (String line : lines) {
			String name = line.trim();
			if (!name.isEmpty()) {
				result.add(name);
			}
		}
		if (result.size() < 2) {
			throw new IllegalArgumentException("보기를 2개 이상 입력하세요.");
		}
		if (result.size() > 10) {
			throw new IllegalArgumentException("보기는 최대 10개까지 입력하세요.");
		}
		return result.toArray(new String[0]);
	}

	private void saveItems(String pollId, String[] items) throws Exception {
		pollService.deletePollItemAll(pollId);
		for (int i = 0; i < items.length; i++) {
			EgovMap item = new EgovMap();
			item.put("itemId", "PI" + CommUtil.getFileId() + i);
			item.put("pollId", pollId);
			item.put("itemNm", items[i]);
			item.put("ordr", i + 1);
			pollService.insertPollItem(item);
		}
	}

	@Operation(summary = "POLL 등록")
	@RequestMapping(value = "/insertPoll.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertPoll(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_POLL");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			String title = cmmUtil.convertHtml(request, "title");
			String startYmd = cmmUtil.convertHtml(request, "startYmd");
			String endYmd = cmmUtil.convertHtml(request, "endYmd");
			if (startYmd != null) {
				startYmd = startYmd.replace("-", "");
			}
			if (endYmd != null) {
				endYmd = endYmd.replace("-", "");
			}
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			checkYmd(startYmd, "투표시작일");
			checkYmd(endYmd, "투표종료일");
			if (startYmd.compareTo(endYmd) > 0) {
				throw new IllegalArgumentException("투표시작일이 종료일보다 늦을 수 없습니다.");
			}
			String[] items = parseItems(request);
			EgovMap param = new EgovMap();
			param.put("pollId", "PL" + CommUtil.getFileId());
			param.put("title", title.trim());
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			param.put("register", "SYSTEM");
			pollService.insertPoll(param);
			saveItems(String.valueOf(param.get("pollId")), items);
			result.put("success", true);
			result.put("pollId", param.get("pollId"));
		} catch (Exception e) {
			logger.error("insertPoll failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "POLL 수정 (보기 재등록, 기존 득표 초기화됨)")
	@RequestMapping(value = "/updatePoll.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updatePoll(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String pollId = cmmUtil.convertHtml(request, "pollId");
			String title = cmmUtil.convertHtml(request, "title");
			String startYmd = cmmUtil.convertHtml(request, "startYmd");
			String endYmd = cmmUtil.convertHtml(request, "endYmd");
			if (startYmd != null) {
				startYmd = startYmd.replace("-", "");
			}
			if (endYmd != null) {
				endYmd = endYmd.replace("-", "");
			}
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			checkYmd(startYmd, "투표시작일");
			checkYmd(endYmd, "투표종료일");
			if (startYmd.compareTo(endYmd) > 0) {
				throw new IllegalArgumentException("투표시작일이 종료일보다 늦을 수 없습니다.");
			}
			String[] items = parseItems(request);
			EgovMap param = new EgovMap();
			param.put("pollId", pollId);
			param.put("title", title.trim());
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			param.put("updusr", "SYSTEM");
			pollService.updatePoll(param);
			saveItems(pollId, items);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updatePoll failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "POLL 삭제 (보기 CASCADE)")
	@RequestMapping(value = "/deletePoll.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deletePoll(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			pollService.deletePoll(cmmUtil.convertHtml(request, "pollId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deletePoll failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "POLL 투표 (기간·사용여부·세션 중복 검사)")
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
