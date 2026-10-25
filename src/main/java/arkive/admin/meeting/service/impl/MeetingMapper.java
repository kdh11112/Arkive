package arkive.admin.meeting.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 회의실 MyBatis 매퍼. SQL은 Meeting_SQL.xml에 있다.
 */
@EgovMapper("meetingMapper")
public interface MeetingMapper {

	List<EgovMap> selectRoomList(EgovMap egovMap) throws Exception;

	EgovMap selectRoomDetail(String roomId) throws Exception;

	List<EgovMap> selectActiveRoomList() throws Exception;

	void insertRoom(EgovMap egovMap) throws Exception;

	int updateRoom(EgovMap egovMap) throws Exception;

	int deleteRoom(String roomId) throws Exception;

	int countReservationsByRoom(String roomId) throws Exception;

	List<EgovMap> selectReservationList(EgovMap egovMap) throws Exception;

	EgovMap selectReservationDetail(String reserveId) throws Exception;

	int countOverlappingReservation(EgovMap egovMap) throws Exception;

	void insertReservation(EgovMap egovMap) throws Exception;

	int deleteReservation(EgovMap egovMap) throws Exception;
}
