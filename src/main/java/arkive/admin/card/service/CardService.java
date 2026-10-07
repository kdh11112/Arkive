package arkive.admin.card.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 명함 서비스.
 */
public interface CardService {

	List<EgovMap> selectPublicCardList(EgovMap egovMap) throws Exception;

	int selectPublicCardListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectMyCardList(EgovMap egovMap) throws Exception;

	int selectMyCardListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectCardDetail(String cardId) throws Exception;

	void insertCard(EgovMap egovMap) throws Exception;

	void updateCard(EgovMap egovMap) throws Exception;

	void deleteCard(EgovMap egovMap) throws Exception;

	void useCard(EgovMap egovMap) throws Exception;

	void cancelUseCard(EgovMap egovMap) throws Exception;
}
