package arkive.admin.address.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.address.service.AddressService;
import arkive.admin.comm.service.PiiFieldService;
import jakarta.annotation.Resource;

/**
 * 주소록 서비스 구현.
 */
@Service("addressService")
public class AddressServiceImpl extends EgovAbstractServiceImpl implements AddressService {

	@Resource(name = "addressMapper")
	private AddressMapper addressMapper;

	@Resource(name = "piiFieldService")
	private PiiFieldService piiFieldService;

	@Override
	public List<EgovMap> selectAddressList(EgovMap egovMap) throws Exception {
		List<EgovMap> rows = addressMapper.selectAddressList(egovMap);
		piiFieldService.decrypt(rows);
		return rows;
	}

	@Override
	public int selectAddressListTotCnt(EgovMap egovMap) throws Exception {
		return addressMapper.selectAddressListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectAddressDetail(String addrId) throws Exception {
		EgovMap row = addressMapper.selectAddressDetail(addrId);
		piiFieldService.decrypt(row);
		return row;
	}

	@Override
	public void insertAddress(EgovMap egovMap) throws Exception {
		piiFieldService.encrypt(egovMap);
		addressMapper.insertAddress(egovMap);
	}

	@Override
	public void updateAddress(EgovMap egovMap) throws Exception {
		piiFieldService.encrypt(egovMap);
		addressMapper.updateAddress(egovMap);
	}

	@Override
	public void deleteAddress(String addrId) throws Exception {
		addressMapper.deleteAddress(addrId);
	}
}
