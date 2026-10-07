package arkive.admin.poll.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 온라인POLL MyBatis 매퍼. SQL은 Poll_SQL.xml에 있다.
 */
@EgovMapper("pollMapper")
public interface PollMapper {

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
