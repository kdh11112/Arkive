package arkive.user.counsel.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
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
import arkive.admin.comm.web.HtmlSanitizer;
import arkive.admin.counsel.service.CounselService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 외부 상담 신청. 쓰기만 제공한다. 목록·답변은 내부(관리)에서 한다.
 * 첨부는 eGov 규칙대로 최대 3개다.
 */
@Tag(name = "사용자 상담", description = "상담 신청")
@Controller
@RequestMapping("/user/counsel")
public class UserCounselController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "counselService")
	private CounselService counselService;

	@Resource(name = "fileService")
	private FileService fileService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	private static final int MAX_ATCH = 3;

	@Operation(summary = "외부 상담 신청 화면")
	@RequestMapping(value = "/counselWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String counselWrite(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "USER_COUNSEL"));
		return "user/counsel/counselWrite";
	}

	@Operation(summary = "상담 첨부 업로드 (최대 3개, 기존 포함)")
	@RequestMapping(value = "/counselFileUpload.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> counselFileUpload(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			List<MultipartFile> files = new ArrayList<>();
			if (request instanceof MultipartHttpServletRequest multipart) {
				List<MultipartFile> upload = multipart.getFiles("file");
				if (upload != null) {
					for (MultipartFile f : upload) {
						if (f != null && !f.isEmpty()) {
							files.add(f);
						}
					}
				}
			}
			if (files.isEmpty()) {
				throw new IllegalArgumentException("업로드할 파일이 없습니다.");
			}
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				grpid = "COUNSEL_" + UUID.randomUUID().toString();
			}
			int existing = fileService.getFileInfoList(grpid).size();
			if (existing + files.size() > MAX_ATCH) {
				throw new IllegalArgumentException("첨부파일은 최대 " + MAX_ATCH + "개까지 등록할 수 있습니다.");
			}
			List<?> staged = fileService.setUploadFiles(files, grpid, "SYSTEM");
			List<Map<String, String>> stagedJson = new ArrayList<>();
			for (Object o : staged) {
				EgovMap m = (EgovMap) o;
				Map<String, String> item = new HashMap<>();
				item.put("fileId", String.valueOf(m.get("fileId")));
				item.put("fileName", String.valueOf(m.get("orgnlFileNm")));
				stagedJson.add(item);
			}
			List<EgovMap> saved = fileService.saveTempFiles(stagedJson, grpid, "SYSTEM");
			List<Map<String, String>> done = new ArrayList<>();
			for (EgovMap m : saved) {
				Map<String, String> item = new HashMap<>();
				item.put("fileId", String.valueOf(m.get("fileId")));
				item.put("fileName", String.valueOf(m.get("orgnlFileNm")));
				done.add(item);
			}
			result.put("success", true);
			result.put("grpid", grpid);
			result.put("files", done);
		} catch (Exception e) {
			logger.error("counselFileUpload failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "외부 상담 신청 등록")
	@RequestMapping(value = "/insertCounsel.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertCounsel(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("USER_COUNSEL");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 잠시 후 다시 시도하세요.");
			}
			String title = cmmUtil.convertHtml(request, "title");
			String writer = cmmUtil.convertHtml(request, "writer");
			String password = cmmUtil.convertHtml(request, "password");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			if (writer == null || writer.trim().isEmpty()) {
				throw new IllegalArgumentException("작성자를 입력하세요.");
			}
			if (password == null || password.isEmpty()) {
				throw new IllegalArgumentException("비밀번호를 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("counselId", "CS" + CommUtil.getFileId());
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			param.put("writer", writer.trim());
			param.put("password", CommUtil.encryptSHA(password));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				grpid = null;
			}
			param.put("atchFileGrpid", grpid);
			counselService.insertCounsel(param);
			result.put("success", true);
			result.put("counselId", param.get("counselId"));
		} catch (Exception e) {
			logger.error("insertCounsel failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
