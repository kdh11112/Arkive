package arkive.com.controller;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import arkive.admin.board.service.BoardService;
import arkive.admin.comm.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


@Controller
@Slf4j
public class ArkiveController {

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "fileService")
	private FileService fileService;
	
	@RequestMapping("/")
	public String root() {
		// 첫 페이지는 외부 화면이다. 관리는 /dashboard(2단계에서 로그인 관문)다.
		return "redirect:/user/userMain";
	}

	@RequestMapping("/dashboard")
	public String dashboard(ModelMap model) throws Exception {
		EgovMap ckParam = new EgovMap();
		ckParam.put("boardType", "CK");
		EgovMap toastParam = new EgovMap();
		toastParam.put("boardType", "TOAST");
		model.put("ckCount", boardService.selectBoardListTotCnt(ckParam));
		model.put("toastCount", boardService.selectBoardListTotCnt(toastParam));
		// MULTIPART 조회에 BOARD_ 그룹이 포함되므로 BOARD 분기는 더하지 않는다.
		model.put("fileCount", fileService.selectAtchFileListByType("MULTIPART").size()
				+ fileService.selectAtchFileListByType("TUS").size()
				+ fileService.selectAtchFileListByType("DROPZONE").size());
		// 최근 글 5건씩 (LIMIT/OFFSET 쿼리 재사용)
		EgovMap recentParam = new EgovMap();
		recentParam.put("searchKeyword", "");
		recentParam.put("firstIndex", 0);
		recentParam.put("recordCountPerPage", 5);
		recentParam.put("boardType", "CK");
		model.put("recentCk", boardService.selectBoardList(recentParam));
		recentParam.put("boardType", "TOAST");
		model.put("recentToast", boardService.selectBoardList(recentParam));
		return "main/dashboard";
	}
}
