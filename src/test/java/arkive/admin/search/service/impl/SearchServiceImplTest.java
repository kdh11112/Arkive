package arkive.admin.search.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.sql.rowset.serial.SerialClob;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 통합검색 결과의 유형 라벨과 본문 요약 변환을 Mapper 목으로 검증한다.
 * 실제 SQL 검색은 {@link SearchMapperTest}가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

	@Mock
	private SearchMapper searchMapper;

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
}
