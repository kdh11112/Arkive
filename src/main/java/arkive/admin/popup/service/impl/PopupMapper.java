package arkive.admin.popup.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 팝업창 MyBatis 매퍼. SQL은 Popup_SQL.xml에 있다.
 */
@EgovMapper("popupMapper")
public interface PopupMapper {

	List<EgovMap> selectPopupList(EgovMap egovMap) throws Exception;

	int selectPopupListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectPopupDetail(String popupId) throws Exception;

	List<EgovMap> selectActivePopupList() throws Exception;

	void insertPopup(EgovMap egovMap) throws Exception;

	void updatePopup(EgovMap egovMap) throws Exception;

	void deletePopup(String popupId) throws Exception;
}
