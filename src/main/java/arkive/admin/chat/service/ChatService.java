package arkive.admin.chat.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 채팅 서비스. 최근 메시지 조회와 저장을 담당한다.
 * 브로드캐스트는 WebSocket 핸들러가 맡고, 서비스는 DB 기록만 책임진다.
 */
public interface ChatService {

	List<EgovMap> selectRecentMessages(int limit) throws Exception;

	String insertMessage(String userId, String userNm, String message) throws Exception;
}
