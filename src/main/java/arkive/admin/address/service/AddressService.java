package arkive.admin.address.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 주소록 서비스.
 */
public interface AddressService {

	List<EgovMap> selectAddressList(EgovMap egovMap) throws Exception;

	int selectAddressListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectAddressDetail(String addrId) throws Exception;

	void insertAddress(EgovMap egovMap) throws Exception;

	void updateAddress(EgovMap egovMap) throws Exception;

	void deleteAddress(String addrId) throws Exception;
}
