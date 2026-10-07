package arkive.admin.counsel.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.counsel.service.CounselService;
import jakarta.annotation.Resource;

/**
 * 상담 서비스 구현.
 */
@Service("counselService")
public class CounselServiceImpl extends EgovAbstractServiceImpl implements CounselService {

	@Resource(name = "counselMapper")
	private CounselMapper counselMapper;

	@Override
	public List<EgovMap> selectCounselList(EgovMap egovMap) throws Exception {
		return counselMapper.selectCounselList(egovMap);
	}

	@Override
	public int selectCounselListTotCnt(EgovMap egovMap) throws Exception {
		return counselMapper.selectCounselListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectCounselDetail(String counselId) throws Exception {
		EgovMap detail = counselMapper.selectCounselDetail(counselId);
		if (detail != null) {
			convertClobToString(detail, "content");
			convertClobToString(detail, "answer");
		}
		return detail;
	}

	@Override
	public boolean checkCounselPassword(EgovMap egovMap) throws Exception {
		return counselMapper.checkCounselPassword(egovMap) > 0;
	}

	@Override
	public void insertCounsel(EgovMap egovMap) throws Exception {
		counselMapper.insertCounsel(egovMap);
	}

	@Override
	public void updateCounsel(EgovMap egovMap) throws Exception {
		counselMapper.updateCounsel(egovMap);
	}

	@Override
	public void deleteCounsel(String counselId) throws Exception {
		counselMapper.deleteCounsel(counselId);
	}

	@Override
	public List<EgovMap> selectCounselAnswerList(EgovMap egovMap) throws Exception {
		return counselMapper.selectCounselAnswerList(egovMap);
	}

	@Override
	public int selectCounselAnswerListTotCnt(EgovMap egovMap) throws Exception {
		return counselMapper.selectCounselAnswerListTotCnt(egovMap);
	}

	@Override
	public void updateCounselAnswer(EgovMap egovMap) throws Exception {
		String status = String.valueOf(egovMap.get("status"));
		if (!"접수대기".equals(status) && !"접수".equals(status) && !"완료".equals(status)) {
			throw new IllegalArgumentException("진행상태는 접수대기·접수·완료 중 하나여야 합니다.");
		}
		counselMapper.updateCounselAnswer(egovMap);
	}

	private void convertClobToString(EgovMap map, String key) throws Exception {
		Object value = map.get(key);
		if (value instanceof Clob clob) {
			try (Reader reader = clob.getCharacterStream()) {
				StringBuilder sb = new StringBuilder();
				char[] buf = new char[8192];
				int len;
				while ((len = reader.read(buf)) != -1) {
					sb.append(buf, 0, len);
				}
				map.put(key, sb.toString());
			}
		}
	}
}
