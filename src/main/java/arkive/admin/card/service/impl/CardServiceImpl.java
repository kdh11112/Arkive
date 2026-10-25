package arkive.admin.card.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.card.service.CardService;
import arkive.admin.comm.service.PiiFieldService;
import jakarta.annotation.Resource;

/**
 * 명함 서비스 구현. 수정·삭제는 본인 등록분만 허용한다.
 */
@Service("cardService")
public class CardServiceImpl extends EgovAbstractServiceImpl implements CardService {

	@Resource(name = "cardMapper")
	private CardMapper cardMapper;

	@Resource(name = "piiFieldService")
	private PiiFieldService piiFieldService;

	@Override
	public List<EgovMap> selectPublicCardList(EgovMap egovMap) throws Exception {
		List<EgovMap> rows = cardMapper.selectPublicCardList(egovMap);
		piiFieldService.decrypt(rows);
		return rows;
	}

	@Override
	public int selectPublicCardListTotCnt(EgovMap egovMap) throws Exception {
		return cardMapper.selectPublicCardListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectMyCardList(EgovMap egovMap) throws Exception {
		List<EgovMap> rows = cardMapper.selectMyCardList(egovMap);
		piiFieldService.decrypt(rows);
		return rows;
	}

	@Override
	public int selectMyCardListTotCnt(EgovMap egovMap) throws Exception {
		return cardMapper.selectMyCardListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectCardDetail(String cardId) throws Exception {
		EgovMap row = cardMapper.selectCardDetail(cardId);
		piiFieldService.decrypt(row);
		return row;
	}

	@Override
	public void insertCard(EgovMap egovMap) throws Exception {
		piiFieldService.encrypt(egovMap);
		cardMapper.insertCard(egovMap);
	}

	@Override
	public void updateCard(EgovMap egovMap) throws Exception {
		piiFieldService.encrypt(egovMap);
		if (cardMapper.updateCard(egovMap) == 0) {
			throw new IllegalStateException("본인이 등록한 명함만 수정할 수 있습니다.");
		}
	}

	@Override
	public void deleteCard(EgovMap egovMap) throws Exception {
		if (cardMapper.deleteCard(egovMap) == 0) {
			throw new IllegalStateException("본인이 등록한 명함만 삭제할 수 있습니다.");
		}
	}

	@Override
	public void useCard(EgovMap egovMap) throws Exception {
		cardMapper.useCard(egovMap);
	}

	@Override
	public void cancelUseCard(EgovMap egovMap) throws Exception {
		cardMapper.cancelUseCard(egovMap);
	}
}
