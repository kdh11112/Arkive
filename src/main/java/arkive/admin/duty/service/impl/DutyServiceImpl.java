package arkive.admin.duty.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.duty.service.DutyService;
import jakarta.annotation.Resource;

/**
 * 당직 서비스 구현.
 */
@Service("dutyService")
public class DutyServiceImpl extends EgovAbstractServiceImpl implements DutyService {

	@Resource(name = "dutyMapper")
	private DutyMapper dutyMapper;

	@Override
	public List<EgovMap> selectDutyList(EgovMap egovMap) throws Exception {
		return dutyMapper.selectDutyList(egovMap);
	}

	@Override
	public EgovMap selectDutyDetail(String dutyId) throws Exception {
		return dutyMapper.selectDutyDetail(dutyId);
	}

	@Override
	public void insertDuty(EgovMap egovMap) throws Exception {
		dutyMapper.insertDuty(egovMap);
	}

	@Override
	public void updateDuty(EgovMap egovMap) throws Exception {
		dutyMapper.updateDuty(egovMap);
	}

	@Override
	public void deleteDuty(String dutyId) throws Exception {
		dutyMapper.deleteDuty(dutyId);
	}
}
