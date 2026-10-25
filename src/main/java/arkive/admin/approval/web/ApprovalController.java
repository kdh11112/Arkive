package arkive.admin.approval.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.approval.service.ApprovalService;
import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 약식결재. 기안·상신·승인·반려·취소와 이력을 한 화면 세트로 제공한다.
 * 조회 범위는 내 기안/결재할 문서이며, 슈퍼관리자는 전체를 볼 수 있다.
 */
@Tag(name = "약식결재", description = "기안 작성·상신·승인·반려·이력")
@Controller
@RequestMapping("/approval")
public class ApprovalController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "approvalService")
	private ApprovalService approvalService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "결재 문서 목록 (내 기안·결재할 문서·전체)")
	@RequestMapping(value = "/approvalList", method = { RequestMethod.GET, RequestMethod.POST })
	public String approvalList(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String userId = String.valueOf(loginUser.get("userId"));
		boolean admin = isAdmin(request);
		String scope = cmmUtil.convertHtml(request, "scope");
		if (!"todo".equals(scope) && !"all".equals(scope)) {
			scope = "my";
		}
		if ("all".equals(scope) && !admin) {
			scope = "my";
		}
		String status = cmmUtil.convertHtml(request, "status");
		if (status == null || !status.matches("^(DRAFT|SUBMITTED|APPROVED|REJECTED|CANCELLED)$")) {
			status = "";
		}
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("userId", userId);
		param.put("drafterId", "my".equals(scope) ? userId : "");
		param.put("approverId", "todo".equals(scope) ? userId : "");
		param.put("status", status);
		param.put("searchKeyword", searchKeyword);
		model.put("scope", scope);
		model.put("status", status);
		model.put("searchKeyword", searchKeyword);
		model.put("admin", admin);
		model.put("resultList", approvalService.selectDocList(param));
		return "approval/approvalList";
	}

	@Operation(summary = "기안 작성·수정 화면")
	@RequestMapping(value = "/approvalWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String approvalWrite(HttpServletRequest request, HttpServletResponse response, ModelMap model)
			throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String userId = String.valueOf(loginUser.get("userId"));
		String docId = cmmUtil.convertHtml(request, "docId");
		if (docId != null && !docId.trim().isEmpty()) {
			EgovMap detail = approvalService.selectDocDetail(docId.trim());
			if (detail == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return null;
			}
			if (!userId.equals(String.valueOf(detail.get("drafterId"))) || !"DRAFT".equals(detail.get("status"))) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}
			model.put("detail", detail);
		}
		model.put("userList", approvalService.selectActiveUserList());
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "APPROVAL"));
		return "approval/approvalWrite";
	}

	@Operation(summary = "기안 상세·이력")
	@RequestMapping(value = "/approvalView", method = { RequestMethod.GET, RequestMethod.POST })
	public String approvalView(HttpServletRequest request, HttpServletResponse response, ModelMap model)
			throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
		String userId = String.valueOf(loginUser.get("userId"));
		String docId = cmmUtil.convertHtml(request, "docId");
		EgovMap detail = approvalService.selectDocDetail(docId);
		if (detail == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		boolean admin = isAdmin(request);
		boolean drafter = userId.equals(String.valueOf(detail.get("drafterId")));
		boolean approver = userId.equals(String.valueOf(detail.get("approverId")));
		if (!admin && !drafter && !approver) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		model.put("detail", detail);
		model.put("historyList", approvalService.selectHistory(docId.trim()));
		model.put("isDrafter", drafter);
		model.put("isApprover", approver);
		model.put("admin", admin);
		return "approval/approvalView";
	}

	@Operation(summary = "기안 임시저장")
	@RequestMapping(value = "/insertDraft.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertDraft(HttpServletRequest request) {
		return execute(request, () -> {
			if (!consumeToken("APPROVAL")) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 확인하세요.");
			}
			EgovMap param = draftParam(request);
			approvalService.insertDraft(param);
			return param.get("docId");
		});
	}

	@Operation(summary = "기안 수정")
	@RequestMapping(value = "/updateDraft.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateDraft(HttpServletRequest request) {
		return execute(request, () -> {
			EgovMap param = draftParam(request);
			param.put("docId", cmmUtil.convertHtml(request, "docId"));
			approvalService.updateDraft(param);
			return param.get("docId");
		});
	}

	@Operation(summary = "기안 삭제 (임시저장)")
	@RequestMapping(value = "/deleteDraft.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteDraft(HttpServletRequest request) {
		return execute(request, () -> {
			approvalService.deleteDraft(cmmUtil.convertHtml(request, "docId"), currentUserId(request));
			return null;
		});
	}

	@Operation(summary = "결재 상신")
	@RequestMapping(value = "/submitApproval.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> submitApproval(HttpServletRequest request) {
		return execute(request, () -> {
			approvalService.submit(cmmUtil.convertHtml(request, "docId"), currentUserId(request));
			return null;
		});
	}

	@Operation(summary = "결재 승인")
	@RequestMapping(value = "/approveApproval.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> approveApproval(HttpServletRequest request) {
		return execute(request, () -> {
			approvalService.approve(cmmUtil.convertHtml(request, "docId"), currentUserId(request));
			return null;
		});
	}

	@Operation(summary = "결재 반려")
	@RequestMapping(value = "/rejectApproval.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> rejectApproval(HttpServletRequest request) {
		return execute(request, () -> {
			approvalService.reject(cmmUtil.convertHtml(request, "docId"), currentUserId(request),
					cmmUtil.convertHtml(request, "rejectReason"));
			return null;
		});
	}

	@Operation(summary = "상신 취소 (기안자)")
	@RequestMapping(value = "/cancelApproval.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelApproval(HttpServletRequest request) {
		return execute(request, () -> {
			approvalService.cancel(cmmUtil.convertHtml(request, "docId"), currentUserId(request));
			return null;
		});
	}

	private EgovMap draftParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("docType", cmmUtil.convertHtml(request, "docType"));
		param.put("title", cmmUtil.convertHtml(request, "title"));
		param.put("content", cmmUtil.convertHtml(request, "content"));
		param.put("drafterId", currentUserId(request));
		param.put("approverId", cmmUtil.convertHtml(request, "approverId"));
		return param;
	}

	private Map<String, Object> execute(HttpServletRequest request, Action action) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (getLoginUser(request) == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			Object data = action.run();
			result.put("success", true);
			if (data != null) {
				result.put("docId", data);
			}
		} catch (Exception e) {
			logger.error("approval action failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private String currentUserId(HttpServletRequest request) {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		return String.valueOf(loginUser.get("userId"));
	}

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private boolean isAdmin(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute(LoginController.ADMIN_KEY) != null;
	}

	private boolean consumeToken(String key) {
		try {
			return EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			return false;
		}
	}

	@FunctionalInterface
	private interface Action {
		Object run() throws Exception;
	}
}
