package arkive.admin.preview.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 파일 미리보기(뷰어). 원본 다운로드 경로를 노출하지 않고 fileId만으로 표시한다.
 * 실제 바이너리는 기존 downloadFile.do(inline 지원 확장자)가 내려준다.
 */
@Tag(name = "미리보기", description = "파일 미리보기 뷰어")
@Controller
@RequestMapping("/preview")
public class PreviewController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "fileService")
	private FileService fileService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "파일 미리보기 (이미지·PDF, 그 외는 다운로드 안내)")
	@RequestMapping(value = "/fileView", method = RequestMethod.GET)
	public String fileView(HttpServletRequest request, ModelMap model) {
		String fileId = cmmUtil.convertHtml(request, "fileId");
		String viewKind = "other";
		String fileName = "";
		try {
			EgovMap info = fileService.getFileInfo(fileId);
			if (info != null) {
				fileName = String.valueOf(info.get("orgnlFileNm"));
				String ext = String.valueOf(info.get("fileExt")).toLowerCase();
				if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "bmp".equals(ext)) {
					viewKind = "image";
				} else if ("pdf".equals(ext)) {
					viewKind = "pdf";
				}
				model.put("fileId", fileId);
				model.put("fileName", fileName);
			}
		} catch (Exception e) {
			logger.error("fileView failed", e);
		}
		model.put("viewKind", viewKind);
		if (fileName.isEmpty()) {
			model.put("fileName", fileId);
		}
		return "preview/fileView";
	}

	/** 미리보기 가능 여부만 돌려준다(목록에서 아이콘 표시용). */
	@RequestMapping(value = "/canPreview.json", method = { RequestMethod.GET, RequestMethod.POST })
	@org.springframework.web.bind.annotation.ResponseBody
	public Map<String, Object> canPreview(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap info = fileService.getFileInfo(cmmUtil.convertHtml(request, "fileId"));
			boolean can = false;
			if (info != null) {
				String ext = String.valueOf(info.get("fileExt")).toLowerCase();
				can = "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)
						|| "bmp".equals(ext) || "pdf".equals(ext);
			}
			result.put("success", true);
			result.put("canPreview", can);
		} catch (Exception e) {
			logger.error("canPreview failed", e);
			result.put("success", false);
		}
		return result;
	}
}
