package arkive.admin.schedule.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 일정·공휴일 서비스.
 */
public interface ScheduleService {

	List<EgovMap> selectScheduleList(EgovMap egovMap) throws Exception;

	EgovMap selectScheduleDetail(String scheduleId) throws Exception;

	void insertSchedule(EgovMap egovMap) throws Exception;

	void updateSchedule(EgovMap egovMap) throws Exception;

	void deleteSchedule(String scheduleId) throws Exception;

	List<EgovMap> selectHolidayList(String yyyymm) throws Exception;

	void insertHoliday(EgovMap egovMap) throws Exception;

	void deleteHoliday(String holidayDe) throws Exception;
}
