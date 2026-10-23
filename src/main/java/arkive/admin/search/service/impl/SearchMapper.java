package arkive.admin.search.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 통합검색 MyBatis 매퍼. SQL은 Search_SQL.xml에 있다.
 */
@EgovMapper("searchMapper")
public interface SearchMapper {

	List<EgovMap> selectSearchList(EgovMap egovMap) throws Exception;

	int selectSearchListTotCnt(EgovMap egovMap) throws Exception;
}
