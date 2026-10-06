package arkive.admin.system.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.system.service.CommCodeService;
import jakarta.annotation.Resource;

/**
 * 공통코드 서비스 구현. 마스터 삭제는 상세까지 함께 지운다(Mapper SQL 기준).
 */
@Service("commCodeService")
public class CommCodeServiceImpl extends EgovAbstractServiceImpl implements CommCodeService {

	@Resource(name = "commCodeMapper")
	private CommCodeMapper commCodeMapper;

	@Override
	public List<EgovMap> selectMasterList(EgovMap egovMap) throws Exception {
		return commCodeMapper.selectMasterList(egovMap);
	}

	@Override
	public List<EgovMap> selectDetailList(EgovMap egovMap) throws Exception {
		return commCodeMapper.selectDetailList(egovMap);
	}

	@Override
	public List<EgovMap> selectCmmnCodeList(EgovMap egovMap) throws Exception {
		return commCodeMapper.selectCmmnCodeList(egovMap);
	}

	@Override
	public EgovMap selectCodeInfo(EgovMap egovMap) throws Exception {
		return commCodeMapper.selectCodeInfo(egovMap);
	}

	@Override
	public int getChkCode(EgovMap egovMap) throws Exception {
		return commCodeMapper.getChkCode(egovMap);
	}

	@Override
	public void insertCode(EgovMap egovMap) throws Exception {
		commCodeMapper.insertCode(egovMap);
	}

	@Override
	public void updateCode(EgovMap egovMap) throws Exception {
		commCodeMapper.updateCode(egovMap);
	}

	@Override
	public void deleteCode(EgovMap egovMap) throws Exception {
		commCodeMapper.deleteCode(egovMap);
	}
}
