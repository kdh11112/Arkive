package arkive.admin.meeting.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.meeting.service.MeetingService;
import jakarta.annotation.Resource;

/**
 * 회의실 서비스 구현.
 */
@Service("meetingService")
public class MeetingServiceImpl extends EgovAbstractServiceImpl implements MeetingService {

	private static final Pattern DATE = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
	private static final Pattern TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	@Resource(name = "meetingMapper")
	private MeetingMapper meetingMapper;

	@Override
	public List<EgovMap> selectRoomList(EgovMap egovMap) throws Exception {
		return meetingMapper.selectRoomList(egovMap);
	}

	@Override
	public EgovMap selectRoomDetail(String roomId) throws Exception {
		return isBlank(roomId) ? null : meetingMapper.selectRoomDetail(roomId.trim());
	}

	@Override
	public List<EgovMap> selectActiveRoomList() throws Exception {
		return meetingMapper.selectActiveRoomList();
	}

	@Override
	@Transactional
	public void insertRoom(EgovMap egovMap) throws Exception {
		validateRoom(egovMap);
		egovMap.put("roomId", "MR" + CommUtil.getFileId());
		meetingMapper.insertRoom(egovMap);
	}

	@Override
	@Transactional
	public void updateRoom(EgovMap egovMap) throws Exception {
		validateRoom(egovMap);
		if (meetingMapper.updateRoom(egovMap) == 0) {
			throw new IllegalArgumentException("회의실이 없습니다.");
		}
	}

	@Override
	@Transactional
	public void deleteRoom(String roomId) throws Exception {
		if (isBlank(roomId)) {
			throw new IllegalArgumentException("회의실 ID가 없습니다.");
		}
		if (meetingMapper.countReservationsByRoom(roomId.trim()) > 0) {
			throw new IllegalStateException("예약이 있는 회의실은 삭제할 수 없습니다. 사용중지로 변경하세요.");
		}
		if (meetingMapper.deleteRoom(roomId.trim()) == 0) {
			throw new IllegalArgumentException("회의실이 없습니다.");
		}
	}

	@Override
	public List<EgovMap> selectReservationList(EgovMap egovMap) throws Exception {
		return meetingMapper.selectReservationList(egovMap);
	}

	@Override
	@Transactional
	public void insertReservation(EgovMap egovMap) throws Exception {
		String roomId = trim(egovMap.get("roomId"));
		String reserveDe = trim(egovMap.get("reserveDe"));
		String startTime = trim(egovMap.get("startTime"));
		String endTime = trim(egovMap.get("endTime"));
		String title = trim(egovMap.get("title"));
		String purpose = trim(egovMap.get("purpose"));
		String userId = trim(egovMap.get("userId"));

		if (userId.isEmpty()) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		EgovMap room = selectRoomDetail(roomId);
		if (room == null || !"Y".equals(trim(room.get("useYn")))) {
			throw new IllegalArgumentException("사용중인 회의실을 선택하세요.");
		}
		if (!DATE.matcher(reserveDe).matches()) {
			throw new IllegalArgumentException("예약 날짜를 YYYY-MM-DD 형식으로 입력하세요.");
		}
		if (!isValidDate(reserveDe)) {
			throw new IllegalArgumentException("존재하지 않는 날짜입니다.");
		}
		if (!TIME.matcher(startTime).matches() || !TIME.matcher(endTime).matches()) {
			throw new IllegalArgumentException("시간을 HH:mm 형식으로 입력하세요.");
		}
		LocalTime start = LocalTime.parse(startTime, TIME_FORMAT);
		LocalTime end = LocalTime.parse(endTime, TIME_FORMAT);
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
		}
		if (title.isEmpty()) {
			throw new IllegalArgumentException("예약 제목을 입력하세요.");
		}
		if (title.length() > 200) {
			throw new IllegalArgumentException("예약 제목은 200자 이내로 입력하세요.");
		}
		if (purpose.length() > 500) {
			throw new IllegalArgumentException("사용 목적은 500자 이내로 입력하세요.");
		}

		EgovMap overlapParam = new EgovMap();
		overlapParam.put("roomId", roomId);
		overlapParam.put("reserveDe", reserveDe);
		overlapParam.put("startTime", startTime);
		overlapParam.put("endTime", endTime);
		if (meetingMapper.countOverlappingReservation(overlapParam) > 0) {
			throw new IllegalStateException("같은 시간에 이미 예약이 있습니다.");
		}

		egovMap.put("reserveId", "RV" + CommUtil.getFileId());
		egovMap.put("title", title);
		egovMap.put("purpose", purpose);
		egovMap.put("startTime", startTime);
		egovMap.put("endTime", endTime);
		meetingMapper.insertReservation(egovMap);
	}

	@Override
	@Transactional
	public void cancelReservation(String reserveId, String userId, boolean admin) throws Exception {
		if (isBlank(reserveId)) {
			throw new IllegalArgumentException("예약 ID가 없습니다.");
		}
		EgovMap detail = meetingMapper.selectReservationDetail(reserveId.trim());
		if (detail == null) {
			throw new IllegalArgumentException("예약이 없습니다.");
		}
		if (!admin && !userId.equals(trim(detail.get("userId")))) {
			throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
		}
		EgovMap param = new EgovMap();
		param.put("reserveId", reserveId.trim());
		param.put("userId", userId);
		param.put("admin", admin);
		if (meetingMapper.deleteReservation(param) == 0) {
			throw new IllegalStateException("이미 취소된 예약입니다.");
		}
	}

	private void validateRoom(EgovMap egovMap) {
		String roomNm = trim(egovMap.get("roomNm"));
		String location = trim(egovMap.get("location"));
		String useYn = trim(egovMap.get("useYn"));
		String remark = trim(egovMap.get("remark"));
		if (roomNm.isEmpty()) {
			throw new IllegalArgumentException("회의실명을 입력하세요.");
		}
		if (roomNm.length() > 100) {
			throw new IllegalArgumentException("회의실명은 100자 이내로 입력하세요.");
		}
		if (location.length() > 200) {
			throw new IllegalArgumentException("위치는 200자 이내로 입력하세요.");
		}
		if (remark.length() > 500) {
			throw new IllegalArgumentException("비고는 500자 이내로 입력하세요.");
		}
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			throw new IllegalArgumentException("사용 여부가 올바르지 않습니다.");
		}
		int capacity;
		try {
			capacity = Integer.parseInt(trim(egovMap.get("capacity")));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("수용 인원을 숫자로 입력하세요.");
		}
		if (capacity < 0 || capacity > 100000) {
			throw new IllegalArgumentException("수용 인원은 0 이상 100000 이하로 입력하세요.");
		}
		egovMap.put("roomNm", roomNm);
		egovMap.put("location", location);
		egovMap.put("useYn", useYn);
		egovMap.put("remark", remark);
		egovMap.put("capacity", capacity);
	}

	private boolean isValidDate(String value) {
		try {
			LocalDate.parse(value);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
