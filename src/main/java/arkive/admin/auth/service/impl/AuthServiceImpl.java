package arkive.admin.auth.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.auth.service.AuthService;
import jakarta.annotation.Resource;

/**
 * 로그인·사용자 서비스 구현.
 */
@Service("authService")
public class AuthServiceImpl extends EgovAbstractServiceImpl implements AuthService {

	@Resource(name = "authMapper")
	private AuthMapper authMapper;

	@Override
	public EgovMap selectLoginUser(String userId) throws Exception {
		return authMapper.selectLoginUser(userId);
	}

	@Override
	public List<EgovMap> selectUserList(EgovMap egovMap) throws Exception {
		return authMapper.selectUserList(egovMap);
	}

	@Override
	public int selectUserListTotCnt(EgovMap egovMap) throws Exception {
		return authMapper.selectUserListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectUserDetail(String userId) throws Exception {
		return authMapper.selectUserDetail(userId);
	}

	@Override
	public void insertUser(EgovMap egovMap) throws Exception {
		authMapper.insertUser(egovMap);
	}

	@Override
	public void updateUser(EgovMap egovMap) throws Exception {
		authMapper.updateUser(egovMap);
	}

	@Override
	public void deleteUser(String userId) throws Exception {
		authMapper.deleteUser(userId);
	}

	@Override
	public void addFailCount(String userId) throws Exception {
		authMapper.addFailCount(userId);
	}

	@Override
	public void lockUser(String userId) throws Exception {
		authMapper.lockUser(userId);
	}

	@Override
	public void resetFailCount(String userId) throws Exception {
		authMapper.resetFailCount(userId);
	}

	@Override
	public void updatePassword(EgovMap egovMap) throws Exception {
		authMapper.updatePassword(egovMap);
	}

	@Override
	public EgovMap selectOauthLink(EgovMap egovMap) throws Exception {
		return authMapper.selectOauthLink(egovMap);
	}

	@Override
	public void insertOauthLink(EgovMap egovMap) throws Exception {
		authMapper.insertOauthLink(egovMap);
	}
}
