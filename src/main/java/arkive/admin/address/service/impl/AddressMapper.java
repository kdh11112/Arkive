package arkive.admin.address.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 주소록 MyBatis 매퍼. SQL은 Address_SQL.xml에 있다.
 */
@EgovMapper("addressMapper")
public interface AddressMapper {

	List<EgovMap> selectAddressList(EgovMap egovMap) throws Exception;

	int selectAddressListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectAddressDetail(String addrId) throws Exception;

	void insertAddress(EgovMap egovMap) throws Exception;

	void updateAddress(EgovMap egovMap) throws Exception;

	void deleteAddress(String addrId) throws Exception;
}
