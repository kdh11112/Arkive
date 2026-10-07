package arkive.admin.duty.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 당직 MyBatis 매퍼. SQL은 Duty_SQL.xml에 있다.
 */
@EgovMapper("dutyMapper")
public interface DutyMapper {

	List<EgovMap> selectDutyList(EgovMap egovMap) throws Exception;

	EgovMap selectDutyDetail(String dutyId) throws Exception;

	void insertDuty(EgovMap egovMap) throws Exception;

	void updateDuty(EgovMap egovMap) throws Exception;

	void deleteDuty(String dutyId) throws Exception;
}
