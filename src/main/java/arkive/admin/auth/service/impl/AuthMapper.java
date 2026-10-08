package arkive.admin.auth.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 로그인·사용자 MyBatis 매퍼. SQL은 Auth_SQL.xml에 있다.
 */
@EgovMapper("authMapper")
public interface AuthMapper {

	EgovMap selectLoginUser(String userId) throws Exception;

	List<EgovMap> selectUserList(EgovMap egovMap) throws Exception;

	int selectUserListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectUserDetail(String userId) throws Exception;

	void insertUser(EgovMap egovMap) throws Exception;

	void updateUser(EgovMap egovMap) throws Exception;

	void deleteUser(String userId) throws Exception;

	void addFailCount(String userId) throws Exception;

	void lockUser(String userId) throws Exception;

	void resetFailCount(String userId) throws Exception;

	void updatePassword(EgovMap egovMap) throws Exception;

	EgovMap selectOauthLink(EgovMap egovMap) throws Exception;

	void insertOauthLink(EgovMap egovMap) throws Exception;
}
