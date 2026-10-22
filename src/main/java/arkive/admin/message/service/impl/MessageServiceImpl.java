package arkive.admin.message.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.message.service.MessageService;
import jakarta.annotation.Resource;

/**
 * 쪽지 서비스 구현.
 */
@Service("messageService")
public class MessageServiceImpl extends EgovAbstractServiceImpl implements MessageService {

	@Resource(name = "messageMapper")
	private MessageMapper messageMapper;

	@Override
	public List<EgovMap> selectMessageList(EgovMap egovMap) throws Exception {
		return messageMapper.selectMessageList(egovMap);
	}

	@Override
	public int selectMessageListTotCnt(EgovMap egovMap) throws Exception {
		return messageMapper.selectMessageListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectMessageDetail(String msgId) throws Exception {
		return messageMapper.selectMessageDetail(msgId);
	}

	@Override
	public EgovMap selectUser(String userId) throws Exception {
		return messageMapper.selectUser(userId);
	}

	@Override
	public void insertMessage(EgovMap egovMap) throws Exception {
		messageMapper.insertMessage(egovMap);
	}

	@Override
	public void updateMessageRead(String msgId) throws Exception {
		messageMapper.updateMessageRead(msgId);
	}

	@Override
	public int deleteReceiverMessage(EgovMap egovMap) throws Exception {
		return messageMapper.deleteReceiverMessage(egovMap);
	}

	@Override
	public int deleteSenderMessage(EgovMap egovMap) throws Exception {
		return messageMapper.deleteSenderMessage(egovMap);
	}
}
