package arkive.admin.card.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 명함 MyBatis 매퍼. SQL은 Card_SQL.xml에 있다.
 */
@EgovMapper("cardMapper")
public interface CardMapper {

	List<EgovMap> selectPublicCardList(EgovMap egovMap) throws Exception;

	int selectPublicCardListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectMyCardList(EgovMap egovMap) throws Exception;

	int selectMyCardListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectCardDetail(String cardId) throws Exception;

	void insertCard(EgovMap egovMap) throws Exception;

	int updateCard(EgovMap egovMap) throws Exception;

	int deleteCard(EgovMap egovMap) throws Exception;

	void useCard(EgovMap egovMap) throws Exception;

	void cancelUseCard(EgovMap egovMap) throws Exception;
}
