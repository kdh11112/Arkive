package arkive.admin.address.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.address.service.AddressService;
import jakarta.annotation.Resource;

/**
 * 주소록 서비스 구현.
 */
@Service("addressService")
public class AddressServiceImpl extends EgovAbstractServiceImpl implements AddressService {

	@Resource(name = "addressMapper")
	private AddressMapper addressMapper;

	@Override
	public List<EgovMap> selectAddressList(EgovMap egovMap) throws Exception {
		return addressMapper.selectAddressList(egovMap);
	}

	@Override
	public int selectAddressListTotCnt(EgovMap egovMap) throws Exception {
		return addressMapper.selectAddressListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectAddressDetail(String addrId) throws Exception {
		return addressMapper.selectAddressDetail(addrId);
	}

	@Override
	public void insertAddress(EgovMap egovMap) throws Exception {
		addressMapper.insertAddress(egovMap);
	}

	@Override
	public void updateAddress(EgovMap egovMap) throws Exception {
		addressMapper.updateAddress(egovMap);
	}

	@Override
	public void deleteAddress(String addrId) throws Exception {
		addressMapper.deleteAddress(addrId);
	}
}
