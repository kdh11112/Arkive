package arkive.admin.poll.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 온라인POLL 서비스.
 */
public interface PollService {

	List<EgovMap> selectPollList(EgovMap egovMap) throws Exception;

	int selectPollListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectPollDetail(String pollId) throws Exception;

	List<EgovMap> selectPollItemList(String pollId) throws Exception;

	void insertPoll(EgovMap egovMap) throws Exception;

	void updatePoll(EgovMap egovMap) throws Exception;

	void deletePoll(String pollId) throws Exception;

	void insertPollItem(EgovMap egovMap) throws Exception;

	void deletePollItemAll(String pollId) throws Exception;

	void votePollItem(String itemId) throws Exception;
}
