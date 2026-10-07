package arkive.admin.duty.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 당직 서비스.
 */
public interface DutyService {

	List<EgovMap> selectDutyList(EgovMap egovMap) throws Exception;

	EgovMap selectDutyDetail(String dutyId) throws Exception;

	void insertDuty(EgovMap egovMap) throws Exception;

	void updateDuty(EgovMap egovMap) throws Exception;

	void deleteDuty(String dutyId) throws Exception;
}
