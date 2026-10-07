package arkive.admin.faq.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.faq.service.FaqService;
import jakarta.annotation.Resource;

/**
 * FAQ 서비스 구현. 상세 조회 시 HSQLDB CLOB을 문자열로 변환한다(Board 패턴).
 */
@Service("faqService")
public class FaqServiceImpl extends EgovAbstractServiceImpl implements FaqService {

	@Resource(name = "faqMapper")
	private FaqMapper faqMapper;

	@Override
	public List<EgovMap> selectFaqList(EgovMap egovMap) throws Exception {
		List<EgovMap> rows = faqMapper.selectFaqList(egovMap);
		// 목록 아코디언에 답변을 바로 보여주므로 목록에서도 CLOB을 문자열로 변환한다.
		for (EgovMap row : rows) {
			convertClobToString(row, "content");
		}
		return rows;
	}

	@Override
	public int selectFaqListTotCnt(EgovMap egovMap) throws Exception {
		return faqMapper.selectFaqListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectFaqDetail(String boardId) throws Exception {
		EgovMap detail = faqMapper.selectFaqDetail(boardId);
		convertClobToString(detail, "content");
		return detail;
	}

	private void convertClobToString(EgovMap map, String key) throws Exception {
		if (map == null) {
			return;
		}
		Object content = map.get(key);
		if (content instanceof Clob clob) {
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

	@Override
	public void insertFaq(EgovMap egovMap) throws Exception {
		faqMapper.insertFaq(egovMap);
	}

	@Override
	public void updateFaq(EgovMap egovMap) throws Exception {
		faqMapper.updateFaq(egovMap);
	}

	@Override
	public void deleteFaq(String boardId) throws Exception {
		faqMapper.deleteFaq(boardId);
	}
}
