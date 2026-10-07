package arkive.admin.banner.web;

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

import arkive.admin.banner.service.BannerService;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.FileTypeUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 배너관리. 일반 배너(POSITION=BANNER)와 메인이미지(POSITION=MAIN)를 위치로 구분한다.
 * 메인이미지 별도 테이블 없이 통합한 형태다.
 * 참고: BLMS SystemController bannerList/bannerInfo/updateBanner/deleteBanner + System_SQL.xml#1046~1215.
 */
@Tag(name = "배너", description = "배너·메인이미지 목록·등록·수정·삭제, 이미지 업로드")
@Controller
@RequestMapping("/banner")
public class BannerController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "bannerService")
	private BannerService bannerService;

	@Resource(name = "fileService")
	private FileService fileService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "배너 목록 (위치·사용여부 필터 + 10건 페이징)")
	@RequestMapping(value = "/bannerList", method = { RequestMethod.GET, RequestMethod.POST })
	public String bannerList(HttpServletRequest request, ModelMap model) throws Exception {
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
		param.put("position", cmmUtil.convertHtml(request, "position"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("position", param.get("position"));
		model.put("useYn", param.get("useYn"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", bannerService.selectBannerList(param));
		paginationInfo.setTotalRecordCount(bannerService.selectBannerListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "banner/bannerList";
	}

	@Operation(summary = "배너 쓰기 화면 (bannerId 있으면 수정)")
	@RequestMapping(value = "/bannerWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String bannerWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String bannerId = cmmUtil.convertHtml(request, "bannerId");
		if (bannerId != null && !bannerId.isEmpty()) {
			EgovMap detail = bannerService.selectBannerDetail(bannerId);
			model.put("detail", detail);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
			}
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_BANNER"));
		return "banner/bannerWrite";
	}

	@Operation(summary = "위치별 현재 게시중 배너 (노출 영역 연계용)")
	@RequestMapping(value = "/activeBanner.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> activeBanner(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String position = cmmUtil.convertHtml(request, "position");
			if (position == null || position.isEmpty()) {
				position = "BANNER";
			}
			result.put("success", true);
			result.put("list", bannerService.selectActiveBannerList(position));
		} catch (Exception e) {
			logger.error("activeBanner failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "배너 이미지 업로드 (단일 이미지, 매직바이트 검증 후 확정)")
	@RequestMapping(value = "/bannerImageUpload.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> bannerImageUpload(HttpServletRequest request) {
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
			if (!FileTypeUtil.isAllowedImage(file, "PNG", "JPG", "GIF", "BMP")) {
				throw new IllegalArgumentException("이미지 파일(PNG/JPG/GIF/BMP)만 업로드할 수 있습니다.");
			}
			String groupId = "BANNER_" + UUID.randomUUID().toString();
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
			logger.error("bannerImageUpload failed", e);
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

	private int parseOrdr(String ordr) {
		try {
			return Integer.parseInt(ordr);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Operation(summary = "배너 등록")
	@RequestMapping(value = "/insertBanner.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertBanner(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_BANNER");
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
			String position = cmmUtil.convertHtml(request, "position");
			if (!"MAIN".equals(position)) {
				position = "BANNER";
			}
			EgovMap param = new EgovMap();
			param.put("bannerId", "BN" + CommUtil.getFileId());
			param.put("title", title.trim());
			param.put("linkUrl", cmmUtil.convertHtml(request, "linkUrl"));
			param.put("position", position);
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			param.put("ordr", parseOrdr(cmmUtil.convertHtml(request, "ordr")));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty() || "undefined".equalsIgnoreCase(grpid) || "null".equalsIgnoreCase(grpid)) {
				grpid = null;
			}
			param.put("atchFileGrpid", grpid);
			param.put("register", "SYSTEM");
			bannerService.insertBanner(param);
			result.put("success", true);
			result.put("bannerId", param.get("bannerId"));
		} catch (Exception e) {
			logger.error("insertBanner failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "배너 수정")
	@RequestMapping(value = "/updateBanner.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateBanner(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("bannerId", cmmUtil.convertHtml(request, "bannerId"));
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
			String position = cmmUtil.convertHtml(request, "position");
			if (!"MAIN".equals(position)) {
				position = "BANNER";
			}
			param.put("title", title.trim());
			param.put("linkUrl", cmmUtil.convertHtml(request, "linkUrl"));
			param.put("position", position);
			param.put("startYmd", startYmd);
			param.put("endYmd", endYmd);
			String useYn = cmmUtil.convertHtml(request, "useYn");
			param.put("useYn", "N".equals(useYn) ? "N" : "Y");
			param.put("ordr", parseOrdr(cmmUtil.convertHtml(request, "ordr")));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				EgovMap detail = bannerService.selectBannerDetail(String.valueOf(param.get("bannerId")));
				grpid = detail == null || detail.get("atchFileGrpid") == null ? null
						: String.valueOf(detail.get("atchFileGrpid"));
			}
			param.put("atchFileGrpid", grpid);
			param.put("updusr", "SYSTEM");
			bannerService.updateBanner(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateBanner failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "배너 삭제 (이미지도 함께 삭제)")
	@RequestMapping(value = "/deleteBanner.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteBanner(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String bannerId = cmmUtil.convertHtml(request, "bannerId");
			EgovMap detail = bannerService.selectBannerDetail(bannerId);
			bannerService.deleteBanner(bannerId);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				for (EgovMap f : fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid")))) {
					fileService.setDeleteFile(String.valueOf(f.get("fileId")));
				}
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteBanner failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
