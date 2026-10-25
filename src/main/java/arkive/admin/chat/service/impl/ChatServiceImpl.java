package arkive.admin.chat.service.impl;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.chat.service.ChatService;
import arkive.admin.comm.web.CommUtil;
import jakarta.annotation.Resource;

/**
 * 채팅 서비스 구현. 최근 메시지를 시간순으로 돌려주고, 전송된 메시지를 저장한다.
 */
@Service("chatService")
public class ChatServiceImpl extends EgovAbstractServiceImpl implements ChatService {

	private static final int MAX_MESSAGE = 1000;
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Resource(name = "chatMapper")
	private ChatMapper chatMapper;

	@Override
	public List<EgovMap> selectRecentMessages(int limit) throws Exception {
		EgovMap param = new EgovMap();
		param.put("limit", Math.max(1, Math.min(limit, 200)));
		List<EgovMap> rows = chatMapper.selectRecentMessages(param);
		Collections.reverse(rows);
		List<EgovMap> messages = new ArrayList<>();
		for (EgovMap row : rows) {
			EgovMap message = new EgovMap();
			message.put("messageId", row.get("messageId"));
			message.put("userId", row.get("userId"));
			message.put("userNm", row.get("userNm"));
			message.put("message", row.get("message"));
			message.put("sentAt", format(row.get("registDt")));
			messages.add(message);
		}
		return messages;
	}

	@Override
	@Transactional
	public String insertMessage(String userId, String userNm, String message) throws Exception {
		String senderId = trim(userId);
		String senderNm = trim(userNm);
		String text = trim(message).replace('\r', ' ').replace('\n', ' ');
		if (senderId.isEmpty() || senderNm.isEmpty()) {
			throw new IllegalArgumentException("로그인 사용자만 메시지를 보낼 수 있습니다.");
		}
		if (text.isEmpty() || text.length() > MAX_MESSAGE) {
			throw new IllegalArgumentException("메시지는 1~1000자로 입력하세요.");
		}
		EgovMap param = new EgovMap();
		param.put("messageId", "CM" + CommUtil.getFileId());
		param.put("userId", senderId);
		param.put("userNm", senderNm);
		param.put("message", text);
		chatMapper.insertMessage(param);
		return String.valueOf(param.get("messageId"));
	}

	private String format(Object value) {
		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime().format(TIME_FORMAT);
		}
		return value == null ? "" : String.valueOf(value);
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
