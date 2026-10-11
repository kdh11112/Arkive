package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * HtmlSanitizer 단위 테스트. DB 없이 실행된다.
 * 검증 기준: script 제거, 이벤트 속성 제거, 비허용 iframe 제거,
 * 유튜브 iframe·표·style 유지, null·빈값 그대로.
 */
class HtmlSanitizerTest {

	private final HtmlSanitizer sanitizer = new HtmlSanitizer();

	@Test
	void scriptBlockShouldBeRemoved() {
		String out = sanitizer.sanitizeBoardContent("<p>hi</p><script>alert(1)</script>");
		assertThat(out).doesNotContain("<script>").doesNotContain("alert(1)");
		assertThat(out).contains("hi");
	}

	@Test
	void eventHandlerShouldBeRemovedButLinkKept() {
		String out = sanitizer.sanitizeBoardContent("<a href=\"http://x\" onclick=\"evil()\">l</a>");
		assertThat(out).doesNotContain("onclick");
		assertThat(out).contains("http://x");
	}

	@Test
	void youtubeIframeShouldBeKept() {
		String out = sanitizer.sanitizeBoardContent("<iframe src=\"https://www.youtube.com/embed/abc\"></iframe>");
		assertThat(out).contains("youtube.com/embed/abc");
	}

	@Test
	void nonVideoIframeShouldBeRemoved() {
		String out = sanitizer.sanitizeBoardContent("<iframe src=\"https://evil.com/x\"></iframe><p>ok</p>");
		assertThat(out).doesNotContain("evil.com");
		assertThat(out).contains("ok");
	}

	@Test
	void styleAndTableShouldBeKept() {
		String out = sanitizer.sanitizeBoardContent(
				"<p style=\"color:red;text-align:center\">K</p><table><tr><td>c</td></tr></table>");
		assertThat(out).contains("color:red");
		assertThat(out).contains("<table>");
	}

	@Test
	void nullAndEmptyShouldPassThrough() {
		assertThat(sanitizer.sanitizeBoardContent(null)).isNull();
		assertThat(sanitizer.sanitizeBoardContent("")).isEmpty();
	}
}
