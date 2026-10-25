package arkive.admin.meeting.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 회의실·예약 서비스.
 *
 * <p>예약은 회의실 마스터가 있고 사용중인 경우에만 등록한다. 같은 회의실·날짜에
 * 시간대가 겹치는 예약은 거부하며, 취소는 예약자 본인 또는 관리자만 할 수 있다.
 */
public interface MeetingService {

	List<EgovMap> selectRoomList(EgovMap egovMap) throws Exception;

	EgovMap selectRoomDetail(String roomId) throws Exception;

	List<EgovMap> selectActiveRoomList() throws Exception;

	void insertRoom(EgovMap egovMap) throws Exception;

	void updateRoom(EgovMap egovMap) throws Exception;

	void deleteRoom(String roomId) throws Exception;

	List<EgovMap> selectReservationList(EgovMap egovMap) throws Exception;

	void insertReservation(EgovMap egovMap) throws Exception;

	void cancelReservation(String reserveId, String userId, boolean admin) throws Exception;
}
