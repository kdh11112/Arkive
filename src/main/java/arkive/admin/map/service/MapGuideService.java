package arkive.admin.map.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 약도관리 서비스. 위치 정보를 등록·조회하고 사용여부로 노출을 제어한다.
 */
public interface MapGuideService {

	List<EgovMap> selectMapGuideList(EgovMap egovMap) throws Exception;

	int selectMapGuideListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMapGuideDetail(String mapId) throws Exception;

	void insertMapGuide(EgovMap egovMap) throws Exception;

	void updateMapGuide(EgovMap egovMap) throws Exception;

	void deleteMapGuide(String mapId) throws Exception;
}
