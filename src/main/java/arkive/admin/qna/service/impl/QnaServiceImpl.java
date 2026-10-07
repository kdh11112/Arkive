package arkive.admin.qna.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.qna.service.QnaService;
import jakarta.annotation.Resource;

/**
 * Q&A 서비스 구현. CLOB 문자열 변환은 Board/Faq 패턴과 같다.
 */
@Service("qnaService")
public class QnaServiceImpl extends EgovAbstractServiceImpl implements QnaService {

	@Resource(name = "qnaMapper")
	private QnaMapper qnaMapper;

	@Override
	public List<EgovMap> selectQnaList(EgovMap egovMap) throws Exception {
		return qnaMapper.selectQnaList(egovMap);
	}

	@Override
	public int selectQnaListTotCnt(EgovMap egovMap) throws Exception {
		return qnaMapper.selectQnaListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectQnaDetail(String boardId) throws Exception {
		EgovMap detail = qnaMapper.selectQnaDetail(boardId);
		convertClobToString(detail, "content");
		return detail;
	}

	@Override
	public EgovMap selectQnaAnswer(String boardId) throws Exception {
		EgovMap answer = qnaMapper.selectQnaAnswer(boardId);
		convertClobToString(answer, "answerContent");
		return answer;
	}

	@Override
	public void insertQna(EgovMap egovMap) throws Exception {
		qnaMapper.insertQna(egovMap);
	}

	@Override
	public void updateQna(EgovMap egovMap) throws Exception {
		qnaMapper.updateQna(egovMap);
	}

	@Override
	public void deleteQna(String boardId) throws Exception {
		qnaMapper.deleteQna(boardId);
	}

	@Override
	public void saveQnaAnswer(EgovMap egovMap) throws Exception {
		qnaMapper.saveQnaAnswer(egovMap);
	}

	private void convertClobToString(EgovMap map, String key) throws Exception {
		if (map == null) {
			return;
		}
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
