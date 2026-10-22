package arkive.admin.message.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.message.service.MessageService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 쪽지관리. 받은쪽지·보낸쪽지를 로그인 사용자 기준으로 분리하고, 삭제는 보낸/받은 함별 소프트 삭제로 처리한다.
 * 근거: eGov v5.0 uss 쪽지(발송·수신함 분리) 패턴. 권한은 발신자/수신자 본인으로 제한한다.
 */
@Tag(name = "쪽지관리", description = "받은쪽지·보낸쪽지 목록, 쪽지쓰기, 상세·읽음 처리, 함별 삭제")
@Controller
@RequestMapping("/message")
public class MessageController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "messageService")
	private MessageService messageService;

	private CommUtil cmmUtil = new CommUtil();

	/**
	 * 세션 로그인 사용자. AuthInterceptor가 먼저 막지만, JSON 처리에서 한 번 더 확인한다.
	 */
	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private String list(String folder, HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		if (loginUser == null) {
			return "redirect:/login/login";
		}
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
		param.put("folder", folder);
		param.put("userId", String.valueOf(loginUser.get("userId")));
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		param.put("firstIndex", paginationInfo.getFirstRecordIndex());
		param.put("recordCountPerPage", paginationInfo.getRecordCountPerPage());
		model.put("folder", folder);
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("pageIndex", pageIndex);
		model.put("resultList", messageService.selectMessageList(param));
		paginationInfo.setTotalRecordCount(messageService.selectMessageListTotCnt(param));
		model.put("paginationInfo", paginationInfo);
		return "message/messageList";
	}

	@Operation(summary = "받은쪽지 목록 (로그인 사용자 수신함)")
	@RequestMapping(value = "/inbox", method = { RequestMethod.GET, RequestMethod.POST })
	public String inbox(HttpServletRequest request, ModelMap model) throws Exception {
		return list("inbox", request, model);
	}

	@Operation(summary = "보낸쪽지 목록 (로그인 사용자 발신함)")
	@RequestMapping(value = "/sent", method = { RequestMethod.GET, RequestMethod.POST })
	public String sent(HttpServletRequest request, ModelMap model) throws Exception {
		return list("sent", request, model);
	}

	@Operation(summary = "쪽지쓰기 화면 (receiverId 있으면 답장 프리필)")
	@RequestMapping(value = "/write", method = { RequestMethod.GET, RequestMethod.POST })
	public String write(HttpServletRequest request, ModelMap model) {
		model.put("receiverId", cmmUtil.convertHtml(request, "receiverId"));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "MESSAGE"));
		return "message/messageWrite";
	}

	@Operation(summary = "쪽지 상세 (수신자가 열면 읽음 처리)")
	@RequestMapping(value = "/detail", method = { RequestMethod.GET, RequestMethod.POST })
	public String detail(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap loginUser = getLoginUser(request);
		String msgId = cmmUtil.convertHtml(request, "msgId");
		EgovMap detail = msgId == null || msgId.isEmpty() ? null : messageService.selectMessageDetail(msgId);
		if (detail == null || loginUser == null) {
			model.put("accessDenied", true);
			return "message/messageDetail";
		}
		String userId = String.valueOf(loginUser.get("userId"));
		String senderId = String.valueOf(detail.get("senderId"));
		String receiverId = String.valueOf(detail.get("receiverId"));
		if (!userId.equals(senderId) && !userId.equals(receiverId)) {
			model.put("accessDenied", true);
			return "message/messageDetail";
		}
		String folder = userId.equals(senderId) ? "sent" : "inbox";
		// 함에서 삭제한 쪽지는 직접 URL로도 열람할 수 없다.
		boolean deleted = ("inbox".equals(folder) && "Y".equals(String.valueOf(detail.get("receiverDelYn"))))
				|| ("sent".equals(folder) && "Y".equals(String.valueOf(detail.get("senderDelYn"))));
		if (deleted) {
			model.put("accessDenied", true);
			return "message/messageDetail";
		}
		if ("inbox".equals(folder) && "N".equals(String.valueOf(detail.get("readYn")))) {
			messageService.updateMessageRead(msgId);
			detail.put("readYn", "Y");
			detail.put("readDt", new Date());
		}
		model.put("detail", detail);
		model.put("folder", folder);
		return "message/messageDetail";
	}

	@Operation(summary = "쪽지 발송 (받는 사람 존재·사용여부 확인)")
	@RequestMapping(value = "/insertMessage.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertMessage(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String receiverId = cmmUtil.convertHtml(request, "receiverId");
			String title = cmmUtil.convertHtml(request, "title");
			String content = cmmUtil.convertHtml(request, "content");
			if (receiverId == null || receiverId.trim().isEmpty()) {
				throw new IllegalArgumentException("받는 사람 아이디를 입력하세요.");
			}
			if (title == null || title.trim().isEmpty()) {
				throw new IllegalArgumentException("제목을 입력하세요.");
			}
			if (content == null || content.trim().isEmpty()) {
				throw new IllegalArgumentException("내용을 입력하세요.");
			}
			receiverId = receiverId.trim();
			title = title.trim();
			if (title.length() > 200) {
				throw new IllegalArgumentException("제목은 200자 이내로 입력하세요.");
			}
			if (content.length() > 4000) {
				throw new IllegalArgumentException("내용은 4000자 이내로 입력하세요.");
			}
			String userId = String.valueOf(loginUser.get("userId"));
			if (userId.equals(receiverId)) {
				throw new IllegalArgumentException("자신에게는 쪽지를 보낼 수 없습니다.");
			}
			EgovMap receiver = messageService.selectUser(receiverId);
			if (receiver == null || !"Y".equals(String.valueOf(receiver.get("useYn")))) {
				throw new IllegalArgumentException("받는 사람을 찾을 수 없습니다.");
			}
			// 입력 검증이 끝난 뒤 토큰을 소모한다. 검증 실패 시 같은 화면에서 다시 보낼 수 있게 한다.
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("MESSAGE");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 보낸쪽지함에서 발송 여부를 확인하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("msgId", "MS" + CommUtil.getFileId());
			param.put("senderId", userId);
			param.put("senderNm", loginUser.get("userNm") == null ? userId : String.valueOf(loginUser.get("userNm")));
			param.put("receiverId", receiverId);
			param.put("receiverNm", receiver.get("userNm") == null ? receiverId : String.valueOf(receiver.get("userNm")));
			param.put("title", title);
			param.put("content", content);
			messageService.insertMessage(param);
			result.put("success", true);
			result.put("msgId", param.get("msgId"));
		} catch (Exception e) {
			logger.error("insertMessage failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "쪽지 삭제 (받은함/보낸함 소프트 삭제)")
	@RequestMapping(value = "/deleteMessage.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteMessage(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String msgId = cmmUtil.convertHtml(request, "msgId");
			String folder = cmmUtil.convertHtml(request, "folder");
			if (msgId == null || msgId.isEmpty()) {
				throw new IllegalArgumentException("쪽지 ID가 없습니다.");
			}
			if (!"inbox".equals(folder) && !"sent".equals(folder)) {
				throw new IllegalArgumentException("삭제 위치가 올바르지 않습니다.");
			}
			EgovMap detail = messageService.selectMessageDetail(msgId);
			if (detail == null) {
				throw new IllegalArgumentException("쪽지가 없습니다.");
			}
			String userId = String.valueOf(loginUser.get("userId"));
			EgovMap param = new EgovMap();
			param.put("msgId", msgId);
			param.put("userId", userId);
			int updated;
			if ("inbox".equals(folder)) {
				if (!userId.equals(String.valueOf(detail.get("receiverId")))) {
					throw new IllegalStateException("삭제 권한이 없습니다.");
				}
				updated = messageService.deleteReceiverMessage(param);
			} else {
				if (!userId.equals(String.valueOf(detail.get("senderId")))) {
					throw new IllegalStateException("삭제 권한이 없습니다.");
				}
				updated = messageService.deleteSenderMessage(param);
			}
			if (updated == 0) {
				throw new IllegalStateException("이미 삭제된 쪽지입니다.");
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteMessage failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
