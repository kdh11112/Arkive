package arkive.admin.schedule.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 일정·공휴일 MyBatis 매퍼. SQL은 Schedule_SQL.xml에 있다.
 */
@EgovMapper("scheduleMapper")
public interface ScheduleMapper {

	List<EgovMap> selectScheduleList(EgovMap egovMap) throws Exception;

	EgovMap selectScheduleDetail(String scheduleId) throws Exception;

	void insertSchedule(EgovMap egovMap) throws Exception;

	void updateSchedule(EgovMap egovMap) throws Exception;

	void deleteSchedule(String scheduleId) throws Exception;

	List<EgovMap> selectHolidayList(String yyyymm) throws Exception;

	void insertHoliday(EgovMap egovMap) throws Exception;

	void deleteHoliday(String holidayDe) throws Exception;
}
