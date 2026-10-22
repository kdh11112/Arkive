package arkive.admin.message.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 쪽지 MyBatis 매퍼. SQL은 Message_SQL.xml에 있다.
 */
@EgovMapper("messageMapper")
public interface MessageMapper {

	List<EgovMap> selectMessageList(EgovMap egovMap) throws Exception;

	int selectMessageListTotCnt(EgovMap egovMap) throws Exception;

	EgovMap selectMessageDetail(String msgId) throws Exception;

	EgovMap selectUser(String userId) throws Exception;

	void insertMessage(EgovMap egovMap) throws Exception;

	void updateMessageRead(String msgId) throws Exception;

	int deleteReceiverMessage(EgovMap egovMap) throws Exception;

	int deleteSenderMessage(EgovMap egovMap) throws Exception;
}
