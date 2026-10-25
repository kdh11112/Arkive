package arkive.admin.search.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 선택적 검색 색인(Elasticsearch) 연동.
 *
 * <p>DB LIKE 검색이 기본이고, {@code arkive.search.elasticsearch.enabled=true}이며 URL이
 * 설정된 경우에만 사용한다. 색인이 비었거나 외부 장애가 나면 {@code null}을 돌려주어
 * 호출자가 DB 검색으로 폴백하게 한다.</p>
 */
public interface SearchIndexService {

	boolean isEnabled();

	SearchResult search(EgovMap param) throws Exception;

	void indexBoard(String boardId);

	void deleteBoard(String boardId);

	int reindexAll() throws Exception;

	record SearchResult(List<EgovMap> list, int total) {
	}
}
