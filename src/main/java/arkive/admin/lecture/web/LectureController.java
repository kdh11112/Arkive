package arkive.admin.lecture.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.lecture.service.LectureService;
import arkive.admin.lecture.service.VideoEncodingService;
import arkive.admin.survey.service.SurveyService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 강의형 콘텐츠. 관리자는 강의·동영상·수료현황을, 일반 사용자는 수강 화면을 사용한다.
 * 메뉴 권한은 MenuPermissionInterceptor가 서버에서 검사한다.
 */
@Tag(name = "강의관리", description = "강의형 콘텐츠·동영상 스트리밍·수료 판정")
@Controller
@RequestMapping("/lecture")
public class LectureController {

	private static final Set<String> LECTURE_STATUSES = Set.of("DRAFT", "OPEN", "CLOSED");

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "lectureService")
	private LectureService lectureService;

	@Resource(name = "surveyService")
	private SurveyService surveyService;

	@Resource(name = "videoEncodingService")
	private VideoEncodingService videoEncodingService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "강의 목록")
	@RequestMapping(value = "/lectureList", method = { RequestMethod.GET, RequestMethod.POST })
	public String lectureList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);
		String status = cmmUtil.convertHtml(request, "status");
		if (!LECTURE_STATUSES.contains(status)) {
			status = "";
		}
		String useYn = cmmUtil.convertHtml(request, "useYn");
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			useYn = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("status", status);
		param.put("useYn", useYn);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("status", status);
		model.put("useYn", useYn);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("encodingEnabled", videoEncodingService.isEncodingEnabled());
		model.put("resultList", lectureService.selectLectureList(param));
		paginationInfo.setTotalRecordCount(lectureService.selectLectureListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "lecture/lectureList";
	}

	@Operation(summary = "강의 등록·수정 화면")
	@RequestMapping(value = "/lectureWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String lectureWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String lectureId = cmmUtil.convertHtml(request, "lectureId");
		if (!lectureId.trim().isEmpty()) {
			EgovMap detail = lectureService.selectLectureDetail(lectureId);
			if (detail == null) {
				throw new IllegalArgumentException("강의가 없습니다.");
			}
			model.put("detail", detail);
		}
		EgovMap surveyParam = new EgovMap();
		surveyParam.put("status", "");
		surveyParam.put("searchKeyword", "");
		surveyParam.put("firstIndex", 0);
		surveyParam.put("recordCountPerPage", 500);
		model.put("surveyList", surveyService.selectSurveyList(surveyParam));
		model.put("encodingEnabled", videoEncodingService.isEncodingEnabled());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "LECTURE"));
		return "lecture/lectureWrite";
	}

	@Operation(summary = "강의 등록")
	@RequestMapping(value = "/insertLecture.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertLecture(HttpServletRequest request,
			@RequestParam(value = "video", required = false) MultipartFile video) {
		return execute(() -> {
			requireToken(request, "LECTURE");
			EgovMap param = lectureParam(request);
			lectureService.insertLecture(param, video);
			return param.get("lectureId");
		});
	}

	@Operation(summary = "강의 수정 (작성중)")
	@RequestMapping(value = "/updateLecture.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateLecture(HttpServletRequest request,
			@RequestParam(value = "video", required = false) MultipartFile video) {
		return execute(() -> {
			EgovMap param = lectureParam(request);
			param.put("lectureId", cmmUtil.convertHtml(request, "lectureId"));
			lectureService.updateLecture(param, video);
			return param.get("lectureId");
		});
	}

	@Operation(summary = "강의 삭제 (작성중)")
	@RequestMapping(value = "/deleteLecture.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteLecture(HttpServletRequest request) {
		return execute(() -> {
			lectureService.deleteLecture(cmmUtil.convertHtml(request, "lectureId"));
			return null;
		});
	}

	@Operation(summary = "강의 공개 (작성중 → 공개)")
	@RequestMapping(value = "/openLecture.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> openLecture(HttpServletRequest request) {
		return execute(() -> {
			lectureService.openLecture(cmmUtil.convertHtml(request, "lectureId"));
			return null;
		});
	}

	@Operation(summary = "강의 마감 (공개 → 마감)")
	@RequestMapping(value = "/closeLecture.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> closeLecture(HttpServletRequest request) {
		return execute(() -> {
			lectureService.closeLecture(cmmUtil.convertHtml(request, "lectureId"));
			return null;
		});
	}

	@Operation(summary = "내 강의 목록")
	@RequestMapping(value = "/myLecture", method = { RequestMethod.GET, RequestMethod.POST })
	public String myLecture(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		model.put("resultList", lectureService.selectAvailableLectureList(String.valueOf(loginUser.get("userId"))));
		return "lecture/myLecture";
	}

	@Operation(summary = "강의 수강 화면")
	@RequestMapping(value = "/view", method = { RequestMethod.GET, RequestMethod.POST })
	public String view(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String lectureId = cmmUtil.convertHtml(request, "lectureId");
		try {
			model.put("lecture", lectureService.selectLectureForView(lectureId,
					String.valueOf(loginUser.get("userId"))));
			model.put("lectureId", lectureId);
		} catch (Exception e) {
			model.put("errorMessage", e.getMessage());
			model.put("lectureId", lectureId);
		}
		return "lecture/view";
	}

	@Operation(summary = "강의 진도 저장")
	@RequestMapping(value = "/progress.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveProgress(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String lectureId = cmmUtil.convertHtml(request, "lectureId");
			int watchedSec = toInt(cmmUtil.convertHtml(request, "watchedSec"));
			Map<String, Object> status = lectureService.saveProgress(lectureId,
					String.valueOf(loginUser.get("userId")), watchedSec);
			result.put("success", true);
			result.putAll(status);
		} catch (Exception e) {
			logger.error("lecture progress failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "동영상 스트리밍 (Range 지원)")
	@RequestMapping(value = "/stream", method = { RequestMethod.GET, RequestMethod.HEAD })
	public void stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		boolean admin = session != null && session.getAttribute(LoginController.ADMIN_KEY) != null;
		boolean loggedIn = session != null && session.getAttribute(LoginController.SESSION_KEY) != null;
		String lectureId = cmmUtil.convertHtml(request, "lectureId");
		EgovMap lecture = lectureService.selectLectureDetail(lectureId);
		if (lecture == null || (!admin && !loggedIn)
				|| (!admin && (!"OPEN".equals(trim(lecture.get("status")))
						|| !"Y".equals(trim(lecture.get("useYn")))))) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String relativePath = trim(lecture.get("videoPath"));
		if (relativePath.isEmpty()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		Path path;
		try {
			path = videoEncodingService.resolve(relativePath);
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!Files.isRegularFile(path)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		writeVideo(request, response, path);
	}

	@Operation(summary = "수료 현황")
	@RequestMapping(value = "/completionList", method = { RequestMethod.GET, RequestMethod.POST })
	public String completionList(HttpServletRequest request, ModelMap model) throws Exception {
		int pageIndex = pageIndex(request);
		PaginationInfo paginationInfo = pagination(pageIndex);
		String lectureId = cmmUtil.convertHtml(request, "lectureId");
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("lectureId", lectureId);
		param.put("searchKeyword", searchKeyword);
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("lectureId", lectureId);
		model.put("searchKeyword", searchKeyword);
		model.put("pageIndex", pageIndex);
		model.put("resultList", lectureService.selectCompletionList(param));
		paginationInfo.setTotalRecordCount(lectureService.selectCompletionListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		model.put("lectureList", lectureOptions());
		return "lecture/completionList";
	}

	private java.util.List<EgovMap> lectureOptions() throws Exception {
		EgovMap param = new EgovMap();
		param.put("status", "");
		param.put("useYn", "");
		param.put("searchKeyword", "");
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 500);
		return lectureService.selectLectureList(param);
	}

	private EgovMap lectureParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("description", cmmUtil.convertHtml(request, "description"));
		param.put("surveyId", cmmUtil.convertHtml(request, "surveyId"));
		param.put("durationSec", cmmUtil.convertHtml(request, "durationSec"));
		param.put("completeRate", cmmUtil.convertHtml(request, "completeRate"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		return param;
	}

	private void writeVideo(HttpServletRequest request, HttpServletResponse response, Path path) throws IOException {
		long length = Files.size(path);
		long start = 0;
		long end = length - 1;
		boolean partial = false;

		String range = request.getHeader("Range");
		if (range != null && range.startsWith("bytes=")) {
			String spec = range.substring("bytes=".length()).split(",")[0].trim();
			int dash = spec.indexOf('-');
			if (dash >= 0) {
				try {
					String startText = spec.substring(0, dash).trim();
					String endText = spec.substring(dash + 1).trim();
					if (startText.isEmpty() && !endText.isEmpty()) {
						long suffix = Long.parseLong(endText);
						start = Math.max(0, length - suffix);
					} else if (!startText.isEmpty()) {
						start = Long.parseLong(startText);
						if (!endText.isEmpty()) {
							end = Long.parseLong(endText);
						}
					}
					partial = true;
				} catch (NumberFormatException e) {
					start = 0;
					end = length - 1;
					partial = false;
				}
			}
			if (partial && (start < 0 || start >= length || end < start)) {
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				response.setHeader("Content-Range", "bytes */" + length);
				return;
			}
			end = Math.min(end, length - 1);
		}

		response.setContentType(contentType(path));
		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("Cache-Control", "no-store");
		if (partial) {
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
		}
		response.setContentLengthLong(end - start + 1);
		if ("HEAD".equalsIgnoreCase(request.getMethod())) {
			return;
		}
		try (InputStream input = Files.newInputStream(path);
				OutputStream output = response.getOutputStream()) {
			input.skipNBytes(start);
			byte[] buffer = new byte[64 * 1024];
			long remaining = end - start + 1;
			while (remaining > 0) {
				int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
				if (read < 0) {
					break;
				}
				output.write(buffer, 0, read);
				remaining -= read;
			}
			output.flush();
		}
	}

	private String contentType(Path path) throws IOException {
		String name = path.getFileName().toString().toLowerCase();
		if (name.endsWith(".mp4")) {
			return "video/mp4";
		}
		if (name.endsWith(".webm")) {
			return "video/webm";
		}
		String probed = Files.probeContentType(path);
		return probed != null ? probed : "application/octet-stream";
	}

	private Map<String, Object> execute(Action action) {
		Map<String, Object> result = new HashMap<>();
		try {
			Object data = action.run();
			result.put("success", true);
			if (data != null) {
				result.put("id", data);
			}
		} catch (Exception e) {
			logger.error("lecture action failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private void requireToken(HttpServletRequest request, String key) {
		boolean valid;
		try {
			valid = EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			valid = false;
		}
		if (!valid) {
			throw new IllegalStateException("중복 제출이 감지되었습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
		}
	}

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private PaginationInfo pagination(int pageIndex) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(10);
		paginationInfo.setPageSize(10);
		return paginationInfo;
	}

	private int pageIndex(HttpServletRequest request) {
		return Math.max(1, toInt(cmmUtil.convertHtml(request, "pageIndex")));
	}

	private int toInt(String value) {
		try {
			return Integer.parseInt(value == null ? "" : value.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	@FunctionalInterface
	private interface Action {
		Object run() throws Exception;
	}
}
