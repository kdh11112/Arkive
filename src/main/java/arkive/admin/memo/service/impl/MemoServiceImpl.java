package arkive.admin.memo.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.memo.service.MemoService;
import jakarta.annotation.Resource;

/**
 * 메모보고 서비스 구현. 상태 전이 0건이면 중복·상태 오류로 판단한다.
 */
@Service("memoService")
public class MemoServiceImpl extends EgovAbstractServiceImpl implements MemoService {

	@Resource(name = "memoMapper")
	private MemoMapper memoMapper;

	@Override
	public List<EgovMap> selectMemoList(EgovMap egovMap) throws Exception {
		return memoMapper.selectMemoList(egovMap);
	}

	@Override
	public int selectMemoListTotCnt(EgovMap egovMap) throws Exception {
		return memoMapper.selectMemoListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectMemoDetail(String reportId) throws Exception {
		EgovMap detail = memoMapper.selectMemoDetail(reportId);
		if (detail != null) {
			convertClobToString(detail, "content");
			convertClobToString(detail, "opinion");
		}
		return detail;
	}

	@Override
	public void insertMemo(EgovMap egovMap) throws Exception {
		memoMapper.insertMemo(egovMap);
	}

	@Override
	public void updateMemo(EgovMap egovMap) throws Exception {
		if (memoMapper.updateMemo(egovMap) == 0) {
			throw new IllegalStateException("확인·의견 이후에는 수정할 수 없습니다.");
		}
	}

	@Override
	public void deleteMemo(String reportId) throws Exception {
		memoMapper.deleteMemo(reportId);
	}

	@Override
	public void confirmMemo(String reportId) throws Exception {
		if (memoMapper.confirmMemo(reportId) == 0) {
			throw new IllegalStateException("이미 처리되었거나 확인할 수 없는 상태입니다.");
		}
	}

	@Override
	public void opinionMemo(EgovMap egovMap) throws Exception {
		if (memoMapper.opinionMemo(egovMap) == 0) {
			throw new IllegalStateException("이미 처리되었거나 의견을 남길 수 없는 상태입니다.");
		}
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
