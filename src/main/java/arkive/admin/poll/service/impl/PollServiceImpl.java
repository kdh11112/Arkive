package arkive.admin.poll.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.poll.service.PollService;
import jakarta.annotation.Resource;

/**
 * 온라인POLL 서비스 구현.
 */
@Service("pollService")
public class PollServiceImpl extends EgovAbstractServiceImpl implements PollService {

	@Resource(name = "pollMapper")
	private PollMapper pollMapper;

	@Override
	public List<EgovMap> selectPollList(EgovMap egovMap) throws Exception {
		return pollMapper.selectPollList(egovMap);
	}

	@Override
	public int selectPollListTotCnt(EgovMap egovMap) throws Exception {
		return pollMapper.selectPollListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectPollDetail(String pollId) throws Exception {
		return pollMapper.selectPollDetail(pollId);
	}

	@Override
	public List<EgovMap> selectPollItemList(String pollId) throws Exception {
		return pollMapper.selectPollItemList(pollId);
	}

	@Override
	public void insertPoll(EgovMap egovMap) throws Exception {
		pollMapper.insertPoll(egovMap);
	}

	@Override
	public void updatePoll(EgovMap egovMap) throws Exception {
		pollMapper.updatePoll(egovMap);
	}

	@Override
	public void deletePoll(String pollId) throws Exception {
		pollMapper.deletePoll(pollId);
	}

	@Override
	public void insertPollItem(EgovMap egovMap) throws Exception {
		pollMapper.insertPollItem(egovMap);
	}

	@Override
	public void deletePollItemAll(String pollId) throws Exception {
		pollMapper.deletePollItemAll(pollId);
	}

	@Override
	public void votePollItem(String itemId) throws Exception {
		pollMapper.votePollItem(itemId);
	}
}
