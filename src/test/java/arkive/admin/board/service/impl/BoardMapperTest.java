package arkive.admin.board.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.EgovBootApplication;

/**
 * BoardMapper 통합 테스트. 실매퍼+실SQL+격리DB로 검증한다.
 * @Transactional + @Rollback이라 테스트 데이터가 남지 않는다.
 * (test-standard.md Mapper/SQL 항목, porting-guide 통합 테스트 항목 기준)
 */
@SpringBootTest(classes = EgovBootApplication.class)
@TestPropertySource("classpath:/application-test.properties")
@Transactional
class BoardMapperTest {

	@Autowired
	private BoardMapper boardMapper;

	@Test
	@Rollback
	void insertSelectDeleteShouldRoundTrip() throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", "BDTEST00000000000001");
		param.put("boardType", "CK");
		param.put("title", "mapper-test");
		param.put("content", "<p>x</p>");
		param.put("atchFileGrpid", null);
		param.put("register", "TEST");

		boardMapper.insertBoard(param);

		EgovMap param2 = new EgovMap();
		param2.put("boardType", "CK");
		param2.put("searchKeyword", "mapper-test");
		param2.put("firstIndex", 0);
		param2.put("recordCountPerPage", 10);
		List<EgovMap> list = boardMapper.selectBoardList(param2);
		assertThat(list).anyMatch(m -> "BDTEST00000000000001".equals(String.valueOf(m.get("boardId"))));

		boardMapper.deleteBoard("BDTEST00000000000001");
		assertThat(boardMapper.selectBoardDetail("BDTEST00000000000001")).isNull();
	}

	@Test
	@Rollback
	void nullContentShouldBeAllowed() throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", "BDTEST00000000000002");
		param.put("boardType", "TOAST");
		param.put("title", "null-content");
		param.put("content", null);
		param.put("atchFileGrpid", null);
		param.put("register", "TEST");

		boardMapper.insertBoard(param);

		EgovMap detail = boardMapper.selectBoardDetail("BDTEST00000000000002");
		assertThat(detail).isNotNull();

		boardMapper.deleteBoard("BDTEST00000000000002");
	}
}
