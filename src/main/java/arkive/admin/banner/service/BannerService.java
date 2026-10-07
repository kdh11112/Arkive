package arkive.admin.banner.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 배너 서비스. POSITION=BANNER(일반 배너)/MAIN(메인이미지)을 함께 다룬다.
 */
public interface BannerService {

	List<EgovMap> selectBannerList(EgovMap egovMap) throws Exception;

	int selectBannerListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectBannerDetail(String bannerId) throws Exception;

	List<EgovMap> selectActiveBannerList(String position) throws Exception;

	void insertBanner(EgovMap egovMap) throws Exception;

	void updateBanner(EgovMap egovMap) throws Exception;

	void deleteBanner(String bannerId) throws Exception;
}
