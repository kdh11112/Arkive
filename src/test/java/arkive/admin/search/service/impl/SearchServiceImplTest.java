package arkive.admin.search.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.sql.rowset.serial.SerialClob;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import arkive.admin.search.service.SearchIndexService;

/**
 * 통합검색 결과의 유형 라벨과 본문 요약 변환, 색인 사용·실패 폴백, 초성 준비를
 * Mapper 목으로 검증한다. 실제 SQL 검색은 {@link SearchMapperTest}가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

	@Mock
	private SearchMapper searchMapper;

	@Mock
	private SearchIndexService searchIndexService;

	@InjectMocks
	private SearchServiceImpl searchService;

	private EgovMap row(String boardType, Object content) {
		EgovMap row = new EgovMap();
		row.put("boardType", boardType);
		row.put("content", content);
		return row;
	}

	@Test
	void shouldMapLabelsAndEmptySnippetWhenContentIsMissing() throws Exception {
		List<EgovMap> rows = new ArrayList<>();
		rows.add(row("CK", null));
		rows.add(row("TOAST", ""));
		rows.add(row("FAQ", null));
		rows.add(row("QNA", null));
		rows.add(row("ETC", null));
		when(searchMapper.selectSearchList(any(EgovMap.class))).thenReturn(rows);

		List<EgovMap> result = searchService.selectSearchList(new EgovMap());

		assertThat(result).extracting(item -> item.get("sourceLabel"))
				.containsExactly("공지사항", "자료실", "FAQ", "Q&A", "ETC");
		assertThat(result).allMatch(item -> "".equals(item.get("snippet")));
	}

	@Test
	void shouldStripHtmlAndTruncateLongSnippet() throws Exception {
		String longHtml = "<p>" + "가".repeat(200) + "</p>";
		when(searchMapper.selectSearchList(any(EgovMap.class)))
				.thenReturn(List.of(row("CK", longHtml)));

		List<EgovMap> result = searchService.selectSearchList(new EgovMap());

		String snippet = String.valueOf(result.get(0).get("snippet"));
		assertThat(snippet).startsWith("가").doesNotContain("<p>").hasSize(161).endsWith("…");
	}

	@Test
	void shouldReadClobContent() throws Exception {
		SerialClob clob = new SerialClob("<p>CLOB 본문</p>".toCharArray());
		when(searchMapper.selectSearchList(any(EgovMap.class)))
				.thenReturn(List.of(row("TOAST", clob)));

		List<EgovMap> result = searchService.selectSearchList(new EgovMap());

		assertThat(result.get(0).get("snippet")).isEqualTo("CLOB 본문");
	}

	@Test
	void shouldUseIndexedListAndExposeTotal() throws Exception {
		EgovMap param = new EgovMap();
		List<EgovMap> indexedRows = List.of(row("CK", "<p>indexed</p>"));
		when(searchIndexService.isEnabled()).thenReturn(true);
		when(searchIndexService.search(param))
				.thenReturn(new SearchIndexService.SearchResult(indexedRows, 7));

		List<EgovMap> result = searchService.selectSearchList(param);

		assertThat(result).isSameAs(indexedRows);
		assertThat(param.get("esTotal")).isEqualTo(7);
		assertThat(searchService.selectSearchListTotCnt(param)).isEqualTo(7);
		verifyNoInteractions(searchMapper);
	}

	@Test
	void shouldUseIndexedTotalWhenEsTotalIsMissing() throws Exception {
		EgovMap param = new EgovMap();
		when(searchIndexService.isEnabled()).thenReturn(true);
		when(searchIndexService.search(param))
				.thenReturn(new SearchIndexService.SearchResult(List.of(), 3));

		assertThat(searchService.selectSearchListTotCnt(param)).isEqualTo(3);
		verifyNoInteractions(searchMapper);
	}

	@Test
	void shouldFallBackToDatabaseWhenIndexSearchFails() throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", "공지");
		when(searchIndexService.isEnabled()).thenReturn(true);
		when(searchIndexService.search(param)).thenThrow(new RuntimeException("es down"));
		when(searchMapper.selectBoardsMissingChosung(200)).thenReturn(List.of());
		when(searchMapper.selectSearchList(param)).thenReturn(List.of(row("CK", "<p>fallback</p>")));
		when(searchMapper.selectSearchListTotCnt(param)).thenReturn(1);

		assertThat(searchService.selectSearchList(param)).hasSize(1);
		assertThat(searchService.selectSearchListTotCnt(param)).isEqualTo(1);
	}

	@Test
	void shouldFillMissingChosungAndPrepareChosungKeyword() throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", "ㅊㅅ");
		EgovMap missing = new EgovMap();
		missing.put("boardId", "BD1");
		missing.put("title", "초성검색");
		when(searchMapper.selectBoardsMissingChosung(200))
				.thenReturn(List.of(missing), List.of());
		when(searchMapper.selectSearchList(param)).thenReturn(List.of());

		searchService.selectSearchList(param);

		ArgumentCaptor<EgovMap> captor = ArgumentCaptor.forClass(EgovMap.class);
		verify(searchMapper).updateSearchChosung(captor.capture());
		assertThat(captor.getValue().get("boardId")).isEqualTo("BD1");
		assertThat(captor.getValue().get("searchChosung")).isEqualTo("ㅊㅅㄱㅅ");
		assertThat(param.get("searchChosung")).isEqualTo("ㅊㅅ");
	}

	@Test
	void asciiKeywordShouldClearChosung() throws Exception {
		EgovMap param = new EgovMap();
		param.put("searchKeyword", "ABC");
		when(searchMapper.selectBoardsMissingChosung(200)).thenReturn(List.of());
		when(searchMapper.selectSearchList(param)).thenReturn(List.of());

		searchService.selectSearchList(param);

		assertThat(param.get("searchChosung")).isNull();
	}

	@Test
	void chosungIndexFailureShouldNotBlockSearch() throws Exception {
		EgovMap param = new EgovMap();
		when(searchMapper.selectBoardsMissingChosung(200))
				.thenThrow(new RuntimeException("db down"));
		when(searchMapper.selectSearchList(param)).thenReturn(List.of());

		assertThat(searchService.selectSearchList(param)).isEmpty();
	}
}
