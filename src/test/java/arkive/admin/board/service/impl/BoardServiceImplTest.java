package arkive.admin.board.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.sql.rowset.serial.SerialClob;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import arkive.admin.search.event.SearchIndexEvent;

/**
 * 게시판 서비스의 CLOB 변환·초성 색인·검색 색인 이벤트 분기를 확인한다.
 * 실제 SQL은 BoardMapperTest가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

	@Mock
	private BoardMapper boardMapper;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private BoardServiceImpl boardService;

	@Test
	void selectBoardDetailShouldReturnNullWhenMissing() throws Exception {
		when(boardMapper.selectBoardDetail("NONE")).thenReturn(null);

		assertThat(boardService.selectBoardDetail("NONE")).isNull();
	}

	@Test
	void selectBoardDetailShouldConvertClobAndKeepPlainText() throws Exception {
		EgovMap clobRow = new EgovMap();
		clobRow.put("content", new SerialClob("<p>CLOB 본문</p>".toCharArray()));
		when(boardMapper.selectBoardDetail("B1")).thenReturn(clobRow);

		assertThat(boardService.selectBoardDetail("B1").get("content")).isEqualTo("<p>CLOB 본문</p>");

		EgovMap textRow = new EgovMap();
		textRow.put("content", "<p>문자열 본문</p>");
		when(boardMapper.selectBoardDetail("B2")).thenReturn(textRow);

		assertThat(boardService.selectBoardDetail("B2").get("content")).isEqualTo("<p>문자열 본문</p>");
	}

	@Test
	void insertBoardShouldFillChosungAndPublishIndex() throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", "BD1");
		param.put("title", "공지사항");

		boardService.insertBoard(param);

		verify(boardMapper).insertBoard(param);
		assertThat(param.get("searchChosung")).isEqualTo("ㄱㅈㅅㅎ");

		ArgumentCaptor<SearchIndexEvent> captor = ArgumentCaptor.forClass(SearchIndexEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().boardId()).isEqualTo("BD1");
		assertThat(captor.getValue().delete()).isFalse();
	}

	@Test
	void insertBoardShouldHandleNullTitleAndSkipIndexForMissingBoardId() throws Exception {
		EgovMap param = new EgovMap();

		boardService.insertBoard(param);

		assertThat(param.get("searchChosung")).isEqualTo("");
		verify(eventPublisher, never()).publishEvent(any(SearchIndexEvent.class));
	}

	@Test
	void insertBoardShouldSkipIndexForLiteralNullBoardId() throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", "null");
		param.put("title", "제목");

		boardService.insertBoard(param);

		verify(eventPublisher, never()).publishEvent(any(SearchIndexEvent.class));
	}

	@Test
	void updateBoardShouldPublishIndex() throws Exception {
		EgovMap param = new EgovMap();
		param.put("boardId", "BD2");
		param.put("title", "수정 제목");

		boardService.updateBoard(param);

		verify(boardMapper).updateBoard(param);
		ArgumentCaptor<SearchIndexEvent> captor = ArgumentCaptor.forClass(SearchIndexEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().boardId()).isEqualTo("BD2");
		assertThat(captor.getValue().delete()).isFalse();
	}

	@Test
	void deleteBoardShouldPublishDeleteIndex() throws Exception {
		boardService.deleteBoard("BD3");

		verify(boardMapper).deleteBoard("BD3");
		ArgumentCaptor<SearchIndexEvent> captor = ArgumentCaptor.forClass(SearchIndexEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().boardId()).isEqualTo("BD3");
		assertThat(captor.getValue().delete()).isTrue();
	}

	@Test
	void deleteBoardShouldSkipDeleteIndexForMissingOrLiteralNullBoardId() throws Exception {
		boardService.deleteBoard(null);
		boardService.deleteBoard("null");

		verify(boardMapper).deleteBoard(null);
		verify(boardMapper).deleteBoard("null");
		verify(eventPublisher, never()).publishEvent(any(SearchIndexEvent.class));
	}

	@Test
	void listAndCountQueriesShouldForwardToMapper() throws Exception {
		EgovMap param = new EgovMap();
		List<EgovMap> rows = List.of(new EgovMap());
		when(boardMapper.selectBoardList(param)).thenReturn(rows);
		when(boardMapper.selectBoardListTotCnt(param)).thenReturn(3);
		when(boardMapper.selectBoardListByCursor(param)).thenReturn(rows);

		assertThat(boardService.selectBoardList(param)).isSameAs(rows);
		assertThat(boardService.selectBoardListTotCnt(param)).isEqualTo(3);
		assertThat(boardService.selectBoardListByCursor(param)).isSameAs(rows);
	}

	@Test
	void updateAndNeighborQueriesShouldForwardToMapper() throws Exception {
		EgovMap param = new EgovMap();
		EgovMap prev = new EgovMap();
		EgovMap next = new EgovMap();
		when(boardMapper.updateNoticeYn(param)).thenReturn(1);
		when(boardMapper.claimBoardMail("B1")).thenReturn(1);
		when(boardMapper.claimBoardSms("B1")).thenReturn(1);
		when(boardMapper.selectPrevOne(param)).thenReturn(prev);
		when(boardMapper.selectNextOne(param)).thenReturn(next);

		boardService.updateViewCnt("B1");
		assertThat(boardService.updateNoticeYn(param)).isEqualTo(1);
		assertThat(boardService.claimBoardMail("B1")).isEqualTo(1);
		assertThat(boardService.claimBoardSms("B1")).isEqualTo(1);
		assertThat(boardService.selectPrevOne(param)).isSameAs(prev);
		assertThat(boardService.selectNextOne(param)).isSameAs(next);

		verify(boardMapper).updateViewCnt("B1");
	}

	@Test
	void commentQueriesShouldForwardToMapper() throws Exception {
		EgovMap comment = new EgovMap();
		List<EgovMap> comments = List.of(comment);
		EgovMap param = new EgovMap();
		when(boardMapper.selectCommentList("B1")).thenReturn(comments);
		when(boardMapper.selectCommentCount("B1")).thenReturn(1);
		when(boardMapper.selectCommentDetail("C1")).thenReturn(comment);

		assertThat(boardService.selectCommentList("B1")).isSameAs(comments);
		assertThat(boardService.selectCommentCount("B1")).isEqualTo(1);
		assertThat(boardService.selectCommentDetail("C1")).isSameAs(comment);

		boardService.insertComment(param);
		boardService.deleteComment("C1");

		verify(boardMapper).insertComment(param);
		verify(boardMapper).deleteComment("C1");
	}
}
