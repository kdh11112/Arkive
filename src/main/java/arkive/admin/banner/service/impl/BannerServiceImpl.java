package arkive.admin.banner.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.banner.service.BannerService;
import jakarta.annotation.Resource;

/**
 * 배너 서비스 구현.
 */
@Service("bannerService")
public class BannerServiceImpl extends EgovAbstractServiceImpl implements BannerService {

	@Resource(name = "bannerMapper")
	private BannerMapper bannerMapper;

	@Override
	public List<EgovMap> selectBannerList(EgovMap egovMap) throws Exception {
		return bannerMapper.selectBannerList(egovMap);
	}

	@Override
	public int selectBannerListTotCnt(EgovMap egovMap) throws Exception {
		return bannerMapper.selectBannerListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectBannerDetail(String bannerId) throws Exception {
		return bannerMapper.selectBannerDetail(bannerId);
	}

	@Override
	public List<EgovMap> selectActiveBannerList(String position) throws Exception {
		return bannerMapper.selectActiveBannerList(position);
	}

	@Override
	public void insertBanner(EgovMap egovMap) throws Exception {
		bannerMapper.insertBanner(egovMap);
	}

	@Override
	public void updateBanner(EgovMap egovMap) throws Exception {
		bannerMapper.updateBanner(egovMap);
	}

	@Override
	public void deleteBanner(String bannerId) throws Exception {
		bannerMapper.deleteBanner(bannerId);
	}
}
