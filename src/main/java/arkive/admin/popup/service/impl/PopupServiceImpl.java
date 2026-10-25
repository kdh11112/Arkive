package arkive.admin.popup.service.impl;

import java.io.Reader;
import java.sql.Clob;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.popup.service.PopupService;
import jakarta.annotation.Resource;

/**
 * 팝업창 서비스 구현.
 */
@Service("popupService")
public class PopupServiceImpl extends EgovAbstractServiceImpl implements PopupService {

	@Resource(name = "popupMapper")
	private PopupMapper popupMapper;

	@Override
	public List<EgovMap> selectPopupList(EgovMap egovMap) throws Exception {
		return popupMapper.selectPopupList(egovMap);
	}

	@Override
	public int selectPopupListTotCnt(EgovMap egovMap) throws Exception {
		return popupMapper.selectPopupListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectPopupDetail(String popupId) throws Exception {
		EgovMap detail = popupMapper.selectPopupDetail(popupId);
		convertClobToString(detail, "content");
		return detail;
	}

	@Override
	public List<EgovMap> selectActivePopupList() throws Exception {
		List<EgovMap> rows = popupMapper.selectActivePopupList();
		// JSON 응답에 JDBC CLOB 객체가 그대로 나가면 DB 세션까지 직렬화된다.
		for (EgovMap row : rows) {
			convertClobToString(row, "content");
		}
		return rows;
	}

	private void convertClobToString(EgovMap map, String key) throws Exception {
		if (map == null) {
			return;
		}
		if (map.get(key) instanceof Clob clob) {
			try (Reader reader = clob.getCharacterStream()) {
				StringBuilder sb = new StringBuilder();
				char[] buf = new char[8192];
				int len;
				while ((len = reader.read(buf)) != -1) {
					sb.append(buf, 0, len);
				}
				map.put(key, sb.toString());
			}
		}
	}

	@Override
	public void insertPopup(EgovMap egovMap) throws Exception {
		popupMapper.insertPopup(egovMap);
	}

	@Override
	public void updatePopup(EgovMap egovMap) throws Exception {
		popupMapper.updatePopup(egovMap);
	}

	@Override
	public void deletePopup(String popupId) throws Exception {
		popupMapper.deletePopup(popupId);
	}
}
