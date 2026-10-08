package arkive.user.board.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.board.service.BoardService;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 외부 사용자 화면. 공지사항(CK)·자료실(TOAST) 목록·상세만 제공한다. 등록·수정·삭제는 내부(관리)에서 한다.
 * 조회·검색 로직은 내부 BoardController와 같고, 쓰기 경로는 두지 않는다.
 */
@Tag(name = "사용자 게시판", description = "공지사항·자료실 목록·상세 (읽기 전용)")
@Controller
@RequestMapping("/user/board")
public class UserBoardController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "fileService")
	private FileService fileService;

	private CommUtil cmmUtil = new CommUtil();

	@SuppressWarnings("unchecked")
	private Set<String> getViewedSet(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Object attr = session.getAttribute("viewedBoards");
		if (attr instanceof Set) {
			return (Set<String>) attr;
		}
		Set<String> viewed = new HashSet<>();
		session.setAttribute("viewedBoards", viewed);
		return viewed;
	}

	private void putNoticeList(int pageUnit, int pageSize, HttpServletRequest request, ModelMap model) throws Exception {
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
		paginationInfo.setRecordCountPerPage(pageUnit);
		paginationInfo.setPageSize(pageSize);

		EgovMap param = new EgovMap();
		param.put("boardType", "CK");
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", boardService.selectBoardList(param));
		paginationInfo.setTotalRecordCount(boardService.selectBoardListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
	}

	@Operation(summary = "외부 공지사항 목록 (내부 등록분이 보임)")
	@RequestMapping(value = "/noticeList", method = { RequestMethod.GET, RequestMethod.POST })
	public String noticeList(HttpServletRequest request, ModelMap model) throws Exception {
		putNoticeList(10, 10, request, model);
		return "user/board/noticeList";
	}

	@Operation(summary = "외부 공지사항 상세")
	@RequestMapping(value = "/noticeDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String noticeDetail(HttpServletRequest request, ModelMap model) throws Exception {
		return detail(request, model, "user/board/noticeDetail");
	}

	@Operation(summary = "외부 자료실 목록 껍데기 (목록은 /board/toastList.json 공용)")
	@RequestMapping(value = "/archiveList", method = { RequestMethod.GET, RequestMethod.POST })
	public String archiveList(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		return "user/board/archiveList";
	}

	@Operation(summary = "외부 자료실 상세")
	@RequestMapping(value = "/archiveDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String archiveDetail(HttpServletRequest request, ModelMap model) throws Exception {
		return detail(request, model, "user/board/archiveDetail");
	}

	private String detail(HttpServletRequest request, ModelMap model, String view) throws Exception {
		String boardId = cmmUtil.convertHtml(request, "boardId");
		Set<String> viewed = getViewedSet(request);
		if (boardId != null && !boardId.isEmpty() && !viewed.contains(boardId)) {
			boardService.updateViewCnt(boardId);
			viewed.add(boardId);
		}
		EgovMap detail = boardService.selectBoardDetail(boardId);
		model.put("detail", detail);
		if (detail != null) {
			EgovMap neighborParam = new EgovMap();
			neighborParam.put("boardType", detail.get("boardType"));
			neighborParam.put("boardId", boardId);
			neighborParam.put("registDt", detail.get("registDt"));
			model.put("prev", boardService.selectPrevOne(neighborParam));
			model.put("next", boardService.selectNextOne(neighborParam));
		}
		if (detail != null && detail.get("atchFileGrpid") != null) {
			List<EgovMap> files = fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid")));
			model.put("fileList", files);
		}
		return view;
	}
}
