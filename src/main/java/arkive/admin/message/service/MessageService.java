package arkive.admin.message.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 쪽지 서비스. 받은쪽지(inbox)와 보낸쪽지(sent)를 사용자 기준으로 분리 조회한다.
 */
public interface MessageService {

	List<EgovMap> selectMessageList(EgovMap egovMap) throws Exception;

	int selectMessageListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMessageDetail(String msgId) throws Exception;

	EgovMap selectUser(String userId) throws Exception;

	void insertMessage(EgovMap egovMap) throws Exception;

	void updateMessageRead(String msgId) throws Exception;

	int deleteReceiverMessage(EgovMap egovMap) throws Exception;

	int deleteSenderMessage(EgovMap egovMap) throws Exception;
}
