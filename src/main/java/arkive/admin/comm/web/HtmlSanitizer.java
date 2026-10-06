package arkive.admin.comm.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nhncorp.lucy.security.xss.XssFilter;

/**
 * 게시판 본문 HTML 정제기.
 *
 * 왜 필요한가:
 * 에디터(CK/토스트) 저장 HTML을 그대로 DB에 넣으면 &lt;script&gt;도 함께 저장돼
 * 상세 화면에서 남의 브라우저에 실행된다(XSS). 그래서 저장 직전에 여기서 걸러낸다.
 *
 * 처리 순서 (3단):
 * 1) script 블록 통째 제거. Lucy 기본값이 script 태그를 통과시켜서(실측 확인)
 *    자바 정규식으로 먼저 지운다. 닫힘 태그 없는 변형까지 처리한다.
 * 2) iframe 선별. 유튜브·비메오 embed만 살리고 나머지는 통째로 지운다.
 *    Lucy는 src 값을 검사하지 않아서(실측 확인) 여기서 먼저 골라낸다.
 *    (CK 미디어 삽입이 저장하는 유튜브 iframe이 여기에 해당한다)
 * 3) Lucy(XssFilter 기본 설정)에 통과. 남은 onclick 같은 이벤트 속성,
 *    javascript: 링크, 허용 외 태그를 주석 처리하며 지운다.
 *    표·이미지·style(색·정렬, HWP 복붙용)은 Lucy 기본값이 살린다(실측 확인).
 *
 * 서블릿 필터(XssEscapeServletFilter)는 쓰지 않는다.
 * 그 필터는 javax.servlet용이라 Boot 3(jakarta)에서 동작하지 않는다.
 */
@Component
public class HtmlSanitizer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/** script 블록. 대소문자 무시, 줄바꿈 포함. */
	private static final Pattern SCRIPT_BLOCK =
			Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/** 닫힘 없는 script 변형. */
	private static final Pattern SCRIPT_SINGLE =
			Pattern.compile("<script\\b[^>]*/>", Pattern.CASE_INSENSITIVE);

	/** iframe 한 개 (쌍 태그 또는 단독 태그). */
	private static final Pattern IFRAME_TAG =
			Pattern.compile("<iframe\\b[^>]*>.*?</iframe>|<iframe\\b[^>]*/>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/** iframe 안의 src 값. */
	private static final Pattern IFRAME_SRC =
			Pattern.compile("src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

	/** 살리는 동영상 주소. 유튜브·비메오 embed만 허용한다. */
	private static final Pattern VIDEO_SRC = Pattern.compile(
			"^https?://(www\\.youtube\\.com/embed/|www\\.youtube-nocookie\\.com/embed/|youtu\\.be/|player\\.vimeo\\.com/video/)",
			Pattern.CASE_INSENSITIVE);

	/** Lucy 필터는 무겁지 않지만 매번 만들지 않고 하나를 공유한다. 스레드 안전하다. */
	private final XssFilter xssFilter;

	public HtmlSanitizer() {
		XssFilter filter;
		try {
			// 기본 설정(lucy-xss-default.xml)을 쓴다. 별도 설정 파일이 필요 없다.
			filter = XssFilter.getInstance();
		} catch (Exception e) {
			logger.error("XssFilter init failed. sanitize가 동작하지 않는다", e);
			filter = null;
		}
		this.xssFilter = filter;
	}

	/**
	 * 게시판 본문을 정제해서 돌려준다.
	 * null·빈값은 그대로 돌려준다.
	 */
	public String sanitizeBoardContent(String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		String cleaned = stripScripts(html);
		cleaned = filterIframes(cleaned);
		if (xssFilter != null) {
			cleaned = xssFilter.doFilter(cleaned);
		}
		return cleaned;
	}

	/** script 블록을 통째로 지운다. */
	private String stripScripts(String html) {
		String cleaned = SCRIPT_BLOCK.matcher(html).replaceAll("");
		return SCRIPT_SINGLE.matcher(cleaned).replaceAll("");
	}

	/**
	 * 동영상 iframe만 남긴다. src가 유튜브·비메오 embed가 아니면 태그째 지운다.
	 * src가 없거나 형식이 이상해도 지운다.
	 */
	private String filterIframes(String html) {
		Matcher m = IFRAME_TAG.matcher(html);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String tag = m.group();
			Matcher src = IFRAME_SRC.matcher(tag);
			String url = src.find() ? src.group(1) : "";
			if (VIDEO_SRC.matcher(url).find()) {
				m.appendReplacement(sb, Matcher.quoteReplacement(tag));
			} else {
				m.appendReplacement(sb, "");
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
