package arkive.admin.hr.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * HR 세트 서비스. 기념일·경조사·포상 CRUD와 개인연차 화면의 당직 조회를 담당한다.
 * 개인연차의 연차 현황·신청 내역은 기존 LeaveService를 그대로 사용한다.
 */
public interface HrService {

	List<EgovMap> selectAnniversaryList(EgovMap egovMap) throws Exception;

	int selectAnniversaryListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectAnniversaryDetail(String annivId) throws Exception;

	void insertAnniversary(EgovMap egovMap) throws Exception;

	void updateAnniversary(EgovMap egovMap) throws Exception;

	void deleteAnniversary(String annivId) throws Exception;

	List<EgovMap> selectEventList(EgovMap egovMap) throws Exception;

	int selectEventListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectEventDetail(String eventId) throws Exception;

	void insertEvent(EgovMap egovMap) throws Exception;

	void updateEvent(EgovMap egovMap) throws Exception;

	void deleteEvent(String eventId) throws Exception;

	List<EgovMap> selectRewardList(EgovMap egovMap) throws Exception;

	int selectRewardListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectRewardDetail(String rewardId) throws Exception;

	void insertReward(EgovMap egovMap) throws Exception;

	void updateReward(EgovMap egovMap) throws Exception;

	void deleteReward(String rewardId) throws Exception;

	List<EgovMap> selectActiveUserList() throws Exception;

	List<EgovMap> selectDutyListByUserName(EgovMap egovMap) throws Exception;

	EgovMap selectUser(String userId) throws Exception;
}
