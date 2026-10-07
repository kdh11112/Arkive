package arkive.admin.counsel.web;

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
import arkive.admin.comm.web.HtmlSanitizer;
import arkive.admin.counsel.service.CounselService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 상담관리. 일반 사용자 상담 등록·수정(비밀번호 확인)과 관리자 답변 목록·답변 저장을 분리한다.
 * 근거: eGov v5.0 uss 상담관리(COMTNCNSLTLIST, COM028 접수대기/접수/완료, 첨부 최대 3개).
 */
@Tag(name = "상담", description = "상담 목록·등록·수정·삭제, 답변 목록·답변 저장")
@Controller
@RequestMapping("/counsel")
public class CounselController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "counselService")
	private CounselService counselService;

	@Resource(name = "fileService")
	private FileService fileService;

	@Resource
	private HtmlSanitizer htmlSanitizer;

	private CommUtil cmmUtil = new CommUtil();

	/** eGov 상담 첨부 최대 3개 규칙을 그대로 적용한다. */
	private static final int MAX_ATCH = 3;

	private int parsePage(HttpServletRequest request) {
		try {
			int pageIndex = Integer.parseInt(cmmUtil.convertHtml(request, "pageIndex"));
			return pageIndex < 1 ? 1 : pageIndex;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private PaginationInfo buildPage(int pageIndex, int total) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		paginationInfo.setTotalRecordCount(total);
		return paginationInfo;
	}

	@Operation(summary = "상담 목록 (작성자·제목 검색 + 10건 페이징)")
	@RequestMapping(value = "/counselList", method = { RequestMethod.GET, RequestMethod.POST })
	public String counselList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = parsePage(request);
		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", (pageIndex - 1) * 10);
		param.put("recordCountPerPage", 10);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", counselService.selectCounselList(param));
		model.put("paginationInfo", buildPage(pageIndex, counselService.selectCounselListTotCnt(param)));
		return "counsel/counselList";
	}

	@Operation(summary = "상담 쓰기 화면 (counselId 있으면 수정, 비밀번호 필요)")
	@RequestMapping(value = "/counselWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String counselWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String counselId = cmmUtil.convertHtml(request, "counselId");
		if (counselId != null && !counselId.isEmpty()) {
			EgovMap detail = counselService.selectCounselDetail(counselId);
			model.put("detail", detail);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
			}
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_COUNSEL"));
		return "counsel/counselWrite";
	}

	@Operation(summary = "상담 상세")
	@RequestMapping(value = "/counselDetail", method = { RequestMethod.GET, RequestMethod.POST })
	public String counselDetail(HttpServletRequest request, ModelMap model) throws Exception {
		String counselId = cmmUtil.convertHtml(request, "counselId");
		EgovMap detail = counselService.selectCounselDetail(counselId);
		model.put("detail", detail);
		if (detail != null && detail.get("atchFileGrpid") != null) {
			model.put("fileList", fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid"))));
		}
		return "counsel/counselDetail";
	}

	@Operation(summary = "관리자 답변 목록 (작성자·진행상태 검색)")
	@RequestMapping(value = "/answerList", method = { RequestMethod.GET, RequestMethod.POST })
	public String answerList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = parsePage(request);
		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("status", cmmUtil.convertHtml(request, "status"));
		param.put("firstIndex", (pageIndex - 1) * 10);
		param.put("recordCountPerPage", 10);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("status", param.get("status"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", counselService.selectCounselAnswerList(param));
		model.put("paginationInfo", buildPage(pageIndex, counselService.selectCounselAnswerListTotCnt(param)));
		return "counsel/counselAnswerList";
	}

	private String sha(String plain) throws Exception {
		return CommUtil.encryptSHA(plain);
	}

	@Operation(summary = "상담 비밀번호 확인 (수정·삭제 전)")
	@RequestMapping(value = "/checkPassword.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> checkPassword(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("counselId", cmmUtil.convertHtml(request, "counselId"));
			param.put("password", sha(cmmUtil.convertHtml(request, "password")));
			boolean ok = counselService.checkCounselPassword(param);
			result.put("success", ok);
			if (!ok) {
				result.put("message", "비밀번호가 틀립니다.");
			}
		} catch (Exception e) {
			logger.error("checkPassword failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
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

	@Operation(summary = "상담 등록")
	@RequestMapping(value = "/insertCounsel.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertCounsel(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_COUNSEL");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
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
			param.put("password", sha(password));
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

	private void assertPassword(String counselId, String password) throws Exception {
		EgovMap param = new EgovMap();
		param.put("counselId", counselId);
		param.put("password", sha(password == null ? "" : password));
		if (!counselService.checkCounselPassword(param)) {
			throw new IllegalStateException("비밀번호가 틀립니다.");
		}
	}

	@Operation(summary = "상담 수정 (비밀번호 확인)")
	@RequestMapping(value = "/updateCounsel.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateCounsel(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String counselId = cmmUtil.convertHtml(request, "counselId");
			assertPassword(counselId, cmmUtil.convertHtml(request, "password"));
			String title = cmmUtil.convertHtml(request, "title");
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("counselId", counselId);
			param.put("title", title.trim());
			String content = request.getParameter("content") == null ? "" : request.getParameter("content");
			param.put("content", htmlSanitizer.sanitizeBoardContent(content));
			String grpid = cmmUtil.convertHtml(request, "atchFileGrpid");
			if (grpid == null || grpid.isEmpty()) {
				EgovMap detail = counselService.selectCounselDetail(counselId);
				grpid = detail == null || detail.get("atchFileGrpid") == null ? null
						: String.valueOf(detail.get("atchFileGrpid"));
			}
			param.put("atchFileGrpid", grpid);
			counselService.updateCounsel(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateCounsel failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "상담 삭제 (비밀번호 확인, 첨부 함께 삭제)")
	@RequestMapping(value = "/deleteCounsel.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteCounsel(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String counselId = cmmUtil.convertHtml(request, "counselId");
			assertPassword(counselId, cmmUtil.convertHtml(request, "password"));
			EgovMap detail = counselService.selectCounselDetail(counselId);
			counselService.deleteCounsel(counselId);
			if (detail != null && detail.get("atchFileGrpid") != null) {
				for (EgovMap f : fileService.getFileInfoList(String.valueOf(detail.get("atchFileGrpid")))) {
					fileService.setDeleteFile(String.valueOf(f.get("fileId")));
				}
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteCounsel failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "상담 답변 저장 (답변·진행상태만 갱신)")
	@RequestMapping(value = "/updateCounselAnswer.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateCounselAnswer(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("counselId", cmmUtil.convertHtml(request, "counselId"));
			String answer = request.getParameter("answer") == null ? "" : request.getParameter("answer");
			if (answer.trim().isEmpty()) {
				throw new IllegalArgumentException("답변을 입력하세요.");
			}
			param.put("answer", htmlSanitizer.sanitizeBoardContent(answer));
			String status = cmmUtil.convertHtml(request, "status");
			if (status == null || status.isEmpty()) {
				status = "완료";
			}
			param.put("status", status);
			counselService.updateCounselAnswer(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateCounselAnswer failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
