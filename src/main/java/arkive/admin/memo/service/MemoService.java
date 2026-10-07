package arkive.admin.memo.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 메모보고 서비스. 상태 전이(확인/의견) 실패 시 IllegalStateException을 던진다.
 */
public interface MemoService {

	List<EgovMap> selectMemoList(EgovMap egovMap) throws Exception;

	int selectMemoListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMemoDetail(String reportId) throws Exception;

	void insertMemo(EgovMap egovMap) throws Exception;

	void updateMemo(EgovMap egovMap) throws Exception;

	void deleteMemo(String reportId) throws Exception;

	void confirmMemo(String reportId) throws Exception;

	void opinionMemo(EgovMap egovMap) throws Exception;
}
