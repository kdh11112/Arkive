package arkive.admin.schedule.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.schedule.service.ScheduleService;
import jakarta.annotation.Resource;

/**
 * 일정·공휴일 서비스 구현.
 */
@Service("scheduleService")
public class ScheduleServiceImpl extends EgovAbstractServiceImpl implements ScheduleService {

	@Resource(name = "scheduleMapper")
	private ScheduleMapper scheduleMapper;

	@Override
	public List<EgovMap> selectScheduleList(EgovMap egovMap) throws Exception {
		return scheduleMapper.selectScheduleList(egovMap);
	}

	@Override
	public EgovMap selectScheduleDetail(String scheduleId) throws Exception {
		EgovMap detail = scheduleMapper.selectScheduleDetail(scheduleId);
		if (detail != null && detail.get("content") instanceof Clob clob) {
			try (Reader reader = clob.getCharacterStream()) {
				StringBuilder sb = new StringBuilder();
				char[] buf = new char[8192];
				int len;
				while ((len = reader.read(buf)) != -1) {
					sb.append(buf, 0, len);
				}
				detail.put("content", sb.toString());
			}
		}
		return detail;
	}

	@Override
	public void insertSchedule(EgovMap egovMap) throws Exception {
		scheduleMapper.insertSchedule(egovMap);
	}

	@Override
	public void updateSchedule(EgovMap egovMap) throws Exception {
		scheduleMapper.updateSchedule(egovMap);
	}

	@Override
	public void deleteSchedule(String scheduleId) throws Exception {
		scheduleMapper.deleteSchedule(scheduleId);
	}

	@Override
	public List<EgovMap> selectHolidayList(String yyyymm) throws Exception {
		return scheduleMapper.selectHolidayList(yyyymm);
	}

	@Override
	public void insertHoliday(EgovMap egovMap) throws Exception {
		scheduleMapper.insertHoliday(egovMap);
	}

	@Override
	public void deleteHoliday(String holidayDe) throws Exception {
		scheduleMapper.deleteHoliday(holidayDe);
	}
}
