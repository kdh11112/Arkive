package arkive.admin.banner.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 배너 MyBatis 매퍼. SQL은 Banner_SQL.xml에 있다.
 */
@EgovMapper("bannerMapper")
public interface BannerMapper {

	List<EgovMap> selectBannerList(EgovMap egovMap) throws Exception;

	int selectBannerListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectBannerDetail(String bannerId) throws Exception;

	List<EgovMap> selectActiveBannerList(String position) throws Exception;

	void insertBanner(EgovMap egovMap) throws Exception;

	void updateBanner(EgovMap egovMap) throws Exception;

	void deleteBanner(String bannerId) throws Exception;
}
