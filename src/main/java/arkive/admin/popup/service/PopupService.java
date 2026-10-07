package arkive.admin.popup.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 팝업창 서비스.
 */
public interface PopupService {

	List<EgovMap> selectPopupList(EgovMap egovMap) throws Exception;

	int selectPopupListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectPopupDetail(String popupId) throws Exception;

	List<EgovMap> selectActivePopupList() throws Exception;

	void insertPopup(EgovMap egovMap) throws Exception;

	void updatePopup(EgovMap egovMap) throws Exception;

	void deletePopup(String popupId) throws Exception;
}
