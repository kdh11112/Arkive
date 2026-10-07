package arkive.admin.faq.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * FAQ 서비스. BOARD_TYPE='FAQ' 행만 다룬다.
 */
public interface FaqService {

	List<EgovMap> selectFaqList(EgovMap egovMap) throws Exception;

	int selectFaqListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectFaqDetail(String boardId) throws Exception;

	void insertFaq(EgovMap egovMap) throws Exception;

	void updateFaq(EgovMap egovMap) throws Exception;

	void deleteFaq(String boardId) throws Exception;
}
