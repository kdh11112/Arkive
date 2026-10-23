package arkive.admin.search.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.search.service.SearchService;
import jakarta.annotation.Resource;

/**
 * 통합검색 서비스 구현.
 *
 * <p>본문은 에디터 HTML이므로 화면에 그대로 넘기지 않고 태그를 제거한 요약문으로 바꾼다.
 * HSQLDB CLOB은 JDBC Clob으로 올 수 있어 문자열로 변환한다.</p>
 */
@Service("searchService")
public class SearchServiceImpl extends EgovAbstractServiceImpl implements SearchService {

	private static final Map<String, String> SOURCE_LABELS = Map.of(
			"CK", "공지사항",
			"TOAST", "자료실",
			"FAQ", "FAQ",
			"QNA", "Q&A");

	private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final int SNIPPET_LENGTH = 160;

	@Resource(name = "searchMapper")
	private SearchMapper searchMapper;

	@Override
	public List<EgovMap> selectSearchList(EgovMap egovMap) throws Exception {
		List<EgovMap> list = searchMapper.selectSearchList(egovMap);
		for (EgovMap row : list) {
			String boardType = row.get("boardType") == null ? "" : String.valueOf(row.get("boardType"));
			row.put("sourceLabel", SOURCE_LABELS.getOrDefault(boardType, boardType));
			row.put("snippet", buildSnippet(row.get("content")));
		}
		return list;
	}

	@Override
	public int selectSearchListTotCnt(EgovMap egovMap) throws Exception {
		return searchMapper.selectSearchListTotCnt(egovMap);
	}

	/**
	 * HTML 태그를 제거하고 공백을 정리한 뒤 최대 길이로 자른다.
	 * 원문 HTML은 화면에서 사용하지 않는다.
	 */
	private String buildSnippet(Object content) throws Exception {
		String text = readContent(content);
		if (text.isEmpty()) {
			return "";
		}
		text = HTML_TAG.matcher(text).replaceAll(" ");
		text = WHITESPACE.matcher(text).replaceAll(" ").trim();
		if (text.length() > SNIPPET_LENGTH) {
			return text.substring(0, SNIPPET_LENGTH) + "…";
		}
		return text;
	}

	private String readContent(Object content) throws Exception {
		if (content == null) {
			return "";
		}
		if (content instanceof Clob clob) {
			try (Reader reader = clob.getCharacterStream()) {
				StringBuilder sb = new StringBuilder();
				char[] buffer = new char[8192];
				int length;
				while ((length = reader.read(buffer)) != -1) {
					sb.append(buffer, 0, length);
				}
				return sb.toString();
			}
		}
		return String.valueOf(content);
	}
}
