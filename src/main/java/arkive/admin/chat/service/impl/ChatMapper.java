package arkive.admin.chat.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 채팅 MyBatis 매퍼. SQL은 Chat_SQL.xml에 있다.
 */
@EgovMapper("chatMapper")
public interface ChatMapper {

	List<EgovMap> selectRecentMessages(EgovMap egovMap) throws Exception;

	void insertMessage(EgovMap egovMap) throws Exception;
}
