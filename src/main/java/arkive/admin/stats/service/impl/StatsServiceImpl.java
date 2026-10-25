package arkive.admin.stats.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.stats.service.StatsService;
import jakarta.annotation.Resource;

/**
 * 통계 서비스 구현.
 */
@Service("statsService")
public class StatsServiceImpl extends EgovAbstractServiceImpl implements StatsService {

	@Resource(name = "statsMapper")
	private StatsMapper statsMapper;

	@Override
	public List<EgovMap> selectBoardStats() throws Exception {
		return statsMapper.selectBoardStats();
	}

	@Override
	public List<EgovMap> selectEventStats(EgovMap egovMap) throws Exception {
		return statsMapper.selectEventStats(egovMap);
	}

	@Override
	public List<EgovMap> selectDailyLoginStats(EgovMap egovMap) throws Exception {
		return statsMapper.selectDailyLoginStats(egovMap);
	}

	@Override
	public List<EgovMap> selectDailyAccessStats(EgovMap egovMap) throws Exception {
		return statsMapper.selectDailyAccessStats(egovMap);
	}

	@Override
	public List<EgovMap> selectDownloadStats(EgovMap egovMap) throws Exception {
		return statsMapper.selectDownloadStats(egovMap);
	}
}
