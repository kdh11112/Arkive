package arkive.admin.meeting.web;

import java.time.LocalDate;
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

import arkive.admin.auth.web.LoginController;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.meeting.service.MeetingService;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 회의실관리 + 회의실예약.
 *
 * <p>회의실 관리(등록·수정·삭제)는 관리자 메뉴 권한이 필요하고,
 * 예약 화면은 로그인 사용자 누구나 사용할 수 있다. 취소는 예약자 본인 또는 관리자만 가능하다.
 */
@Tag(name = "회의실", description = "회의실 관리와 시간대 예약·취소")
@Controller
@RequestMapping("/meeting")
public class MeetingController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "meetingService")
	private MeetingService meetingService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "회의실관리 화면")
	@RequestMapping(value = "/roomList", method = { RequestMethod.GET, RequestMethod.POST })
	public String roomList(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", cmmUtil.convertHtml(request, "searchKeyword"));
		model.put("searchKeyword", param.get("searchKeyword"));
		model.put("resultList", meetingService.selectRoomList(param));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "MEETING_ROOM"));
		return "meeting/roomList";
	}

	@Operation(summary = "회의실 등록·수정 화면")
	@RequestMapping(value = "/roomWrite", method = { RequestMethod.GET, RequestMethod.POST })
	public String roomWrite(HttpServletRequest request, ModelMap model) throws Exception {
		String roomId = cmmUtil.convertHtml(request, "roomId");
		if (roomId != null && !roomId.trim().isEmpty()) {
			model.put("detail", meetingService.selectRoomDetail(roomId.trim()));
		}
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "MEETING_ROOM"));
		return "meeting/roomWrite";
	}

	@Operation(summary = "회의실 등록")
	@RequestMapping(value = "/insertRoom.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertRoom(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!consumeToken("MEETING_ROOM")) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 목록에서 등록 여부를 확인하세요.");
			}
			EgovMap param = roomParam(request);
			meetingService.insertRoom(param);
			result.put("success", true);
			result.put("roomId", param.get("roomId"));
		} catch (Exception e) {
			logger.error("insertRoom failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "회의실 수정")
	@RequestMapping(value = "/updateRoom.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateRoom(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = roomParam(request);
			param.put("roomId", cmmUtil.convertHtml(request, "roomId"));
			meetingService.updateRoom(param);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("updateRoom failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "회의실 삭제")
	@RequestMapping(value = "/deleteRoom.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteRoom(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			meetingService.deleteRoom(cmmUtil.convertHtml(request, "roomId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteRoom failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "회의실예약 화면")
	@RequestMapping(value = "/reserveList", method = { RequestMethod.GET, RequestMethod.POST })
	public String reserveList(HttpServletRequest request, ModelMap model) throws Exception {
		String reserveDe = cmmUtil.convertHtml(request, "reserveDe");
		if (reserveDe == null || !reserveDe.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
			reserveDe = LocalDate.now().toString();
		}
		String roomId = cmmUtil.convertHtml(request, "roomId");
		String searchKeyword = cmmUtil.convertHtml(request, "searchKeyword");

		EgovMap param = new EgovMap();
		param.put("reserveDe", reserveDe);
		param.put("roomId", roomId);
		param.put("searchKeyword", searchKeyword);
		model.put("reserveDe", reserveDe);
		model.put("roomId", roomId);
		model.put("searchKeyword", searchKeyword);
		model.put("roomList", meetingService.selectActiveRoomList());
		model.put("resultList", meetingService.selectReservationList(param));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "MEETING_RESERVE"));
		return "meeting/reserveList";
	}

	@Operation(summary = "회의실 예약 등록")
	@RequestMapping(value = "/insertReservation.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertReservation(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			if (!consumeToken("MEETING_RESERVE")) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 예약 목록에서 확인하세요.");
			}
			EgovMap param = new EgovMap();
			param.put("userId", String.valueOf(loginUser.get("userId")));
			param.put("roomId", cmmUtil.convertHtml(request, "roomId"));
			param.put("reserveDe", cmmUtil.convertHtml(request, "reserveDe"));
			param.put("startTime", cmmUtil.convertHtml(request, "startTime"));
			param.put("endTime", cmmUtil.convertHtml(request, "endTime"));
			param.put("title", cmmUtil.convertHtml(request, "title"));
			param.put("purpose", cmmUtil.convertHtml(request, "purpose"));
			meetingService.insertReservation(param);
			result.put("success", true);
			result.put("reserveId", param.get("reserveId"));
		} catch (Exception e) {
			logger.error("insertReservation failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "회의실 예약 취소 (본인·관리자)")
	@RequestMapping(value = "/cancelReservation.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelReservation(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap loginUser = getLoginUser(request);
			if (loginUser == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			HttpSession session = request.getSession(false);
			boolean admin = session != null && session.getAttribute(LoginController.ADMIN_KEY) != null;
			meetingService.cancelReservation(cmmUtil.convertHtml(request, "reserveId"),
					String.valueOf(loginUser.get("userId")), admin);
			result.put("success", true);
		} catch (Exception e) {
			logger.error("cancelReservation failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private EgovMap roomParam(HttpServletRequest request) {
		EgovMap param = new EgovMap();
		param.put("roomNm", cmmUtil.convertHtml(request, "roomNm"));
		param.put("location", cmmUtil.convertHtml(request, "location"));
		param.put("capacity", cmmUtil.convertHtml(request, "capacity"));
		param.put("useYn", cmmUtil.convertHtml(request, "useYn"));
		param.put("remark", cmmUtil.convertHtml(request, "remark"));
		return param;
	}

	private EgovMap getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object user = session.getAttribute(LoginController.SESSION_KEY);
		return user instanceof EgovMap ? (EgovMap) user : null;
	}

	private boolean consumeToken(String key) {
		try {
			return EgovDoubleSubmitHelper.checkAndSaveToken(key);
		} catch (RuntimeException e) {
			return false;
		}
	}
}
