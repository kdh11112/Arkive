package arkive.admin.search.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 통합검색 1단계(DB LIKE) 서비스.
 * BOARD 테이블의 CK/TOAST/FAQ/QNA 글을 제목으로 검색하고, 본문은 결과 요약에만 사용한다.
 */
public interface SearchService {

	List<EgovMap> selectSearchList(EgovMap egovMap) throws Exception;

	int selectSearchListTotCnt(EgovMap egovMap) throws Exception;
}
