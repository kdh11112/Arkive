package arkive.admin.qna.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import arkive.support.ArkiveIntegrationTest;

/**
 * Q&A 공개범위(ASK_TYPE) 필터와 상세 반환을 실제 SQL로 확인한다.
 */
@ArkiveIntegrationTest
@Transactional
class QnaMapperTest {

	@Autowired
	private QnaMapper qnaMapper;

	private void insert(String boardId, String title, String askType) throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", boardId);
		param.put("title", title);
		param.put("content", "본문");
		param.put("register", "작성자");
		param.put("askType", askType);
		qnaMapper.insertQna(param);
	}

	private EgovMap param(boolean publicOnly) {
		EgovMap param = new EgovMap();
		param.put("publicOnly", publicOnly);
		param.put("firstIndex", 0);
		param.put("recordCountPerPage", 20);
		return param;
	}

	@Test
	@Rollback
	void publicOnlyShouldHideInternalQuestions() throws Exception {
		insert("BDQNA00000000000001", "공개 질문", "PUBLIC");
		insert("BDQNA00000000000002", "내부 질문", "INTERNAL");

		List<EgovMap> publicList = qnaMapper.selectQnaList(param(true));
		List<EgovMap> allList = qnaMapper.selectQnaList(param(false));

		assertThat(publicList).extracting(row -> String.valueOf(row.get("boardId")))
				.contains("BDQNA00000000000001")
				.doesNotContain("BDQNA00000000000002");
		assertThat(allList).extracting(row -> String.valueOf(row.get("boardId")))
				.contains("BDQNA00000000000001", "BDQNA00000000000002");
		assertThat(qnaMapper.selectQnaListTotCnt(param(true)))
				.isLessThan(qnaMapper.selectQnaListTotCnt(param(false)));
	}

	@Test
	@Rollback
	void detailShouldReturnAskType() throws Exception {
		insert("BDQNA00000000000003", "내부 상세", "INTERNAL");

		EgovMap detail = qnaMapper.selectQnaDetail("BDQNA00000000000003");

		assertThat(String.valueOf(detail.get("askType"))).isEqualTo("INTERNAL");
	}

	@Test
	@Rollback
	void answerStatusShouldFollowAnswerAndCascadeDelete() throws Exception {
		String boardId = "BDQNA00000000000004";
		insert(boardId, "답변 상태 질문", "PUBLIC");
		assertThat(answerStatus(boardId)).isEqualTo("미답변");

		EgovMap answer = new EgovMap();
		answer.put("boardId", boardId);
		answer.put("answerContent", "답변 내용");
		answer.put("answerer", "admin");
		qnaMapper.saveQnaAnswer(answer);

		assertThat(qnaMapper.selectQnaAnswer(boardId)).isNotNull();
		assertThat(answerStatus(boardId)).isEqualTo("답변완료");
		assertThat(String.valueOf(qnaMapper.selectQnaAnswer(boardId).get("answerer"))).isEqualTo("admin");

		// 답변은 UPSERT로 갱신되고, 질문 삭제 시 FK ON DELETE CASCADE로 함께 지워진다.
		answer.put("answerContent", "수정 답변");
		qnaMapper.saveQnaAnswer(answer);
		qnaMapper.deleteQna(boardId);
		assertThat(qnaMapper.selectQnaAnswer(boardId)).isNull();
	}

	private String answerStatus(String boardId) throws Exception {
		return qnaMapper.selectQnaList(param(false)).stream()
				.filter(row -> boardId.equals(String.valueOf(row.get("boardId"))))
				.map(row -> String.valueOf(row.get("answerStatus")).trim())
				.findFirst()
				.orElseThrow();
	}
}
