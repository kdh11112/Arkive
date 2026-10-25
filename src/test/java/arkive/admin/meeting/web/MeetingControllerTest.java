package arkive.admin.meeting.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.meeting.service.impl.MeetingMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 회의실 관리·예약의 권한, 중복 시간대 거부, 본인 취소 제한을 확인한다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class MeetingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeetingMapper meetingMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Test
	void adminShouldRenderScreensAndUserShouldOnlyReserve() throws Exception {
		mockMvc.perform(get("/meeting/roomList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("회의실관리")));
		mockMvc.perform(get("/meeting/reserveList").session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("회의실예약")));

		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", "meeting-user");
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		MockHttpSession userSession = TestSessions.user("meeting-user", "회의실사용자");

		mockMvc.perform(get("/meeting/reserveList").session(userSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/meeting/roomList").session(userSession))
				.andExpect(status().isForbidden());
	}

	@Test
	void roomShouldBeCreatedUpdatedAndDeleted() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String roomId = insertRoom(session, "테스트실", "3층", "8");

		EgovMap detail = meetingMapper.selectRoomDetail(roomId);
		assertThat(detail.get("roomNm")).isEqualTo("테스트실");

		mockMvc.perform(post("/meeting/updateRoom.json").session(session)
				.param("roomId", roomId)
				.param("roomNm", "테스트실2")
				.param("location", "4층")
				.param("capacity", "10")
				.param("useYn", "N")
				.param("remark", "수정"))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(meetingMapper.selectRoomDetail(roomId).get("useYn")).isEqualTo("N");

		mockMvc.perform(post("/meeting/deleteRoom.json").session(session).param("roomId", roomId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(meetingMapper.selectRoomDetail(roomId)).isNull();
	}

	@Test
	void roomWithReservationShouldNotBeDeleted() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String roomId = insertRoom(session, "예약실", "2층", "4");
		insertReservation(session, roomId, "2030-05-07", "10:00", "11:00", "회의");

		mockMvc.perform(post("/meeting/deleteRoom.json").session(session).param("roomId", roomId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("예약이 있는")));
	}

	@Test
	void overlappingAndInvalidTimesShouldBeRejected() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String roomId = insertRoom(session, "중복실", "1층", "6");
		insertReservation(session, roomId, "2030-05-08", "09:00", "10:00", "첫 회의");

		mockMvc.perform(post("/meeting/insertReservation.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MEETING_RESERVE"))
				.param("roomId", roomId)
				.param("reserveDe", "2030-05-08")
				.param("startTime", "09:30")
				.param("endTime", "10:30")
				.param("title", "겹치는 회의"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("같은 시간")));

		mockMvc.perform(post("/meeting/insertReservation.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MEETING_RESERVE"))
				.param("roomId", roomId)
				.param("reserveDe", "2030-05-09")
				.param("startTime", "11:00")
				.param("endTime", "10:00")
				.param("title", "잘못된 시간"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("종료 시간")));
	}

	@Test
	void onlyOwnerOrAdminShouldCancelReservation() throws Exception {
		MockHttpSession adminSession = TestSessions.admin("admin");
		String roomId = insertRoom(adminSession, "취소실", "1층", "4");
		insertReservation(adminSession, roomId, "2030-05-10", "13:00", "14:00", "취소 테스트");

		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", "other-user");
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		MockHttpSession otherSession = TestSessions.user("other-user", "다른사용자");

		mockMvc.perform(post("/meeting/cancelReservation.json").session(otherSession)
				.param("reserveId", firstReservationId(roomId)))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(containsString("본인의 예약")));
	}

	private String insertRoom(MockHttpSession session, String roomNm, String location, String capacity)
			throws Exception {
		mockMvc.perform(post("/meeting/insertRoom.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MEETING_ROOM"))
				.param("roomNm", roomNm)
				.param("location", location)
				.param("capacity", capacity)
				.param("useYn", "Y")
				.param("remark", ""))
				.andExpect(jsonPath("$.success").value(true));
		EgovMap param = new EgovMap();
		param.put("searchKeyword", roomNm);
		return String.valueOf(meetingMapper.selectRoomList(param).get(0).get("roomId"));
	}

	private String insertReservation(MockHttpSession session, String roomId, String reserveDe, String startTime,
			String endTime, String title) throws Exception {
		mockMvc.perform(post("/meeting/insertReservation.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "MEETING_RESERVE"))
				.param("roomId", roomId)
				.param("reserveDe", reserveDe)
				.param("startTime", startTime)
				.param("endTime", endTime)
				.param("title", title)
				.param("purpose", "테스트"))
				.andExpect(jsonPath("$.success").value(true));
		return firstReservationId(roomId);
	}

	private String firstReservationId(String roomId) throws Exception {
		EgovMap param = new EgovMap();
		param.put("roomId", roomId);
		return String.valueOf(meetingMapper.selectReservationList(param).get(0).get("reserveId"));
	}
}
