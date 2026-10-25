package arkive.admin.hr.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * HR 세트 MyBatis 매퍼. SQL은 Hr_SQL.xml에 있다.
 */
@EgovMapper("hrMapper")
public interface HrMapper {

	List<EgovMap> selectAnniversaryList(EgovMap egovMap) throws Exception;

	int selectAnniversaryListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectAnniversaryDetail(String annivId) throws Exception;

	void insertAnniversary(EgovMap egovMap) throws Exception;

	int updateAnniversary(EgovMap egovMap) throws Exception;

	int deleteAnniversary(String annivId) throws Exception;

	List<EgovMap> selectEventList(EgovMap egovMap) throws Exception;

	int selectEventListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectEventDetail(String eventId) throws Exception;

	void insertEvent(EgovMap egovMap) throws Exception;

	int updateEvent(EgovMap egovMap) throws Exception;

	int deleteEvent(String eventId) throws Exception;

	List<EgovMap> selectRewardList(EgovMap egovMap) throws Exception;

	int selectRewardListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectRewardDetail(String rewardId) throws Exception;

	void insertReward(EgovMap egovMap) throws Exception;

	int updateReward(EgovMap egovMap) throws Exception;

	int deleteReward(String rewardId) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	List<EgovMap> selectDutyListByUserName(EgovMap egovMap) throws Exception;

	EgovMap selectUser(String userId) throws Exception;
}
