package arkive.admin.memo.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 메모보고 MyBatis 매퍼. SQL은 Memo_SQL.xml에 있다.
 * 상태 전이 SQL(update/confirm/opinion)은 적용 건수를 반환한다. 0건이면 상태 불일치다.
 */
@EgovMapper("memoMapper")
public interface MemoMapper {

	List<EgovMap> selectMemoList(EgovMap egovMap) throws Exception;

	int selectMemoListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMemoDetail(String reportId) throws Exception;

	void insertMemo(EgovMap egovMap) throws Exception;

	int updateMemo(EgovMap egovMap) throws Exception;

	void deleteMemo(String reportId) throws Exception;

	int confirmMemo(String reportId) throws Exception;

	int opinionMemo(EgovMap egovMap) throws Exception;
}
