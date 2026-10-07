package arkive.admin.faq.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * FAQ MyBatis 매퍼. SQL은 Faq_SQL.xml에 있다.
 * BOARD 테이블을 BOARD_TYPE='FAQ' 고정으로 재사용한다(신규 테이블 없음).
 */
@EgovMapper("faqMapper")
public interface FaqMapper {

	List<EgovMap> selectFaqList(EgovMap egovMap) throws Exception;

	int selectFaqListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectFaqDetail(String boardId) throws Exception;

	void insertFaq(EgovMap egovMap) throws Exception;

	void updateFaq(EgovMap egovMap) throws Exception;

	void deleteFaq(String boardId) throws Exception;
}
