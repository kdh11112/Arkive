package arkive.admin.map.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 약도관리 MyBatis 매퍼. SQL은 MapGuide_SQL.xml에 있다.
 */
@EgovMapper("mapGuideMapper")
public interface MapGuideMapper {

	List<EgovMap> selectMapGuideList(EgovMap egovMap) throws Exception;

	int selectMapGuideListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMapGuideDetail(String mapId) throws Exception;

	void insertMapGuide(EgovMap egovMap) throws Exception;

	int updateMapGuide(EgovMap egovMap) throws Exception;

	int deleteMapGuide(String mapId) throws Exception;
}
