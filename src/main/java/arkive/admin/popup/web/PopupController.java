package arkive.admin.popup.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.FileTypeUtil;
import arkive.admin.comm.web.HtmlSanitizer;
import arkive.admin.popup.service.PopupService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 팝업창관리. 게시기간·사용여부 조건 + 현재 게시중 조회(대시보드 연계용)를 제공한다.
 * 참고: BLMS SystemController popupList/popupReg/updatePopup/deletePopup + System_SQL.xml#810~918.
 */
@Tag(name = "팝업창", description = "팝업 목록·등록·수정·삭제, 이미지 업로드, 게시중 조회")
@Controller
@RequestMapping("/popup")
public class PopupController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "popupService")
	private PopupService popupService;

	@Resource(name = "fileService")
	private FileService fileService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "팝업 목록 (검색 + 사용여부 필터 + 10건 페이징)")
	@RequestMapping(value = "/popupList", method = { RequestMethod.GET, RequestMethod.POST })
	public String popupList(HttpServletRequest request, ModelMap model) throws Exception {
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
		model.put("resultList", popupService.selectPopupList(param));
		paginationInfo.setTotalRecordCount(popupService.selectPopupListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "popup/popupList";
	}

	@Operation(summary = "팝업 쓰기 화면 (popupId 있으면 수정)")
	@RequestMapping(value = "/popupWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String popupWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String popupId = cmmUtil.convertHtml(request, "popupId");
		if (popupId != null && !popupId.isEmpty()) {
			EgovMap detail = popupService.selectPopupDetail(popupId);
			model.put("detail", detail);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
			}
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_POPUP"));
		return "popup/popupWrite";
	}

	@Operation(summary = "현재 게시중 팝업 (대시보드 진입 시 조회 연계용)")
	@RequestMapping(value = "/activePopup.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> activePopup() {
		Map<String, Object> result = new HashMap<>();
		try {
			result.put("success", true);
			result.put("list", popupService.selectActivePopupList());
		} catch (Exception e) {
			logger.error("activePopup failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "팝업 이미지 업로드 (단일 이미지, 매직바이트 검증 후 확정)")
	@RequestMapping(value = "/popupImageUpload.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> popupImageUpload(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			List<MultipartFile> files = new ArrayList<>();
			if (request instanceof MultipartHttpServletRequest multipart) {
				List<MultipartFile> image = multipart.getFiles("image");
				if (image != null && !image.isEmpty()) {
					files.addAll(image);
				}
			}
			if (files.isEmpty() || files.get(0).isEmpty()) {
				throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
			}
			MultipartFile file = files.get(0);
			// 실형식 검증. 확장자 속임(text→.png)을 매직바이트로 차단한다.
			if (!FileTypeUtil.isAllowedImage(file, "PNG", "JPG", "GIF", "BMP")) {
				throw new IllegalArgumentException("이미지 파일(PNG/JPG/GIF/BMP)만 업로드할 수 있습니다.");
			}
			String groupId = "POPUP_" + UUID.randomUUID().toString();
			List<?> staged = fileService.setUploadFiles(List.of(file), groupId, "SYSTEM");
			List<Map<String, String>> stagedJson = new ArrayList<>();
			for (Object o : staged) {
				EgovMap m = (EgovMap) o;
				Map<String, String> item = new HashMap<>();
				item.put("fileId", String.valueOf(m.get("fileId")));
				item.put("fileName", String.valueOf(m.get("orgnlFileNm")));
				stagedJson.add(item);
			}
			List<EgovMap> saved = fileService.saveTempFiles(stagedJson, groupId, "SYSTEM");
			if (saved.isEmpty()) {
				throw new IllegalStateException("이미지 저장에 실패했습니다.");
			}
			String fileId = String.valueOf(saved.get(0).get("fileId"));
			result.put("success", true);
			result.put("grpid", groupId);
			result.put("fileId", fileId);
			result.put("url", "/file/downloadFile.do?downloadFileId=" + fileId);
		} catch (Exception e) {
			logger.error("popupImageUpload failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private void checkYmd(String ymd, String label) {
		if (ymd == null || !ymd.matches("^[12][0-9]{3}[01][0-9][0-3][0-9]$")) {
			throw new IllegalArgumentException(label + "을 YYYYMMDD 형식으로 입력하세요.");
		}
	}

	@Operation(summary = "팝업 등록")
	@RequestMapping(value = "/insertPopup.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertPopup(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_POPUP");
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
			checkYmd(startYmd, "게시시작일");
			checkYmd(endYmd, "게시종료일");
			if (startYmd.compareTo(endYmd) > 0) {
				throw new IllegalArgumentException("게시시작일이 종료일보다 늦을 수 없습니다.");
			}
			EgovMap param = new EgovMap();
			param.put("popupId", "PP" + CommUtil.getFileId());
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			param.put("linkUrl", cmmUtil.convertHtml(request, "linkUrl"));
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty() || "undefined".equalsIgnoreCase(grpid) || "null".equalsIgnoreCase(grpid)) {
				grpid = null;
			}
			param.put("atchFileGrpid", grpid);
			param.put("register", "SYSTEM");
			popupService.insertPopup(param);
			result.put("success", true);
			result.put("popupId", param.get("popupId"));
		} catch (Exception e) {
			logger.error("insertPopup failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "팝업 수정")
	@RequestMapping(value = "/updatePopup.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updatePopup(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("popupId", cmmUtil.convertHtml(request, "popupId"));
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
			checkYmd(startYmd, "게시시작일");
			checkYmd(endYmd, "게시종료일");
			if (startYmd.compareTo(endYmd) > 0) {
				throw new IllegalArgumentException("게시시작일이 종료일보다 늦을 수 없습니다.");
			}
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			param.put("linkUrl", cmmUtil.convertHtml(request, "linkUrl"));
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				EgovMap detail = popupService.selectPopupDetail(String.valueOf(param.get("popupId")));
				grpid = detail == null || detail.get("atchFileGrpid") == null ? null
						: String.valueOf(detail.get("atchFileGrpid"));
			}
			param.put("atchFileGrpid", grpid);
			param.put("updusr", "SYSTEM");
			popupService.updatePopup(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updatePopup failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "팝업 삭제 (이미지도 함께 삭제)")
	@RequestMapping(value = "/deletePopup.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deletePopup(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String popupId = cmmUtil.convertHtml(request, "popupId");
			EgovMap detail = popupService.selectPopupDetail(popupId);
			popupService.deletePopup(popupId);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				for (EgovMap f : fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid")))) {
					fileService.setDeleteFile(String.valueOf(f.get("fileId")));
				}
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deletePopup failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
