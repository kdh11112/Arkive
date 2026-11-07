package arkive.admin.comm.web;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 게시글 조회수 중복 집계를 막는다. 게시글 1건은 브라우저(세션)당 24시간에 한 번만 센다.
 * <p>
 * 세션에 본 시각을 남기고, 24시간 만료 쿠키도 함께 내려 브라우저를 닫거나 세션이 끊겨도
 * 같은 기기에서는 하루 한 번만 집계되게 한다. 24시간이 지나면 둘 다 만료돼 다시 센다.
 */
public final class BoardViewTracker {

	/** 같은 게시글을 다시 집계하지 않는 기간. 24시간. */
	public static final long WINDOW_MS = 24L * 60L * 60L * 1000L;

	/** 세션에 본 시각(epoch ms)을 담는 키. */
	public static final String SESSION_KEY = "boardViewTimes";

	/** 게시글별 쿠키 이름 접두어. 게시글 ID를 붙여 boardView_&lt;boardId&gt; 형태로 쓴다. */
	public static final String COOKIE_PREFIX = "boardView_";

	private BoardViewTracker() {
	}

	/**
	 * 24시간 안에 본 적이 없으면 집계 대상으로 보고, 본 시각을 세션·쿠키에 기록한 뒤 true를 돌려준다.
	 * 이미 본 글이면 아무것도 하지 않고 false.
	 */
	public static boolean markIfDue(HttpServletRequest request, HttpServletResponse response, String boardId) {
		long now = System.currentTimeMillis();
		if (isRecentlyViewedInSession(request, boardId, now) || isRecentlyViewedByCookie(request, boardId, now)) {
			return false;
		}
		rememberInSession(request, boardId, now);
		writeCookie(response, request, boardId, now);
		return true;
	}

	private static boolean isRecentlyViewedInSession(HttpServletRequest request, String boardId, long now) {
		Long last = viewTimes(request.getSession()).get(boardId);
		return last != null && now - last < WINDOW_MS;
	}

	private static boolean isRecentlyViewedByCookie(HttpServletRequest request, String boardId, long now) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return false;
		}
		String name = COOKIE_PREFIX + boardId;
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				try {
					// 쿠키 값이 깨졌으면 만료된 것으로 보고 다시 센다.
					return now - Long.parseLong(cookie.getValue()) < WINDOW_MS;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		}
		return false;
	}

	private static void rememberInSession(HttpServletRequest request, String boardId, long now) {
		viewTimes(request.getSession()).put(boardId, now);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Long> viewTimes(HttpSession session) {
		Object attr = session.getAttribute(SESSION_KEY);
		if (attr instanceof Map) {
			return (Map<String, Long>) attr;
		}
		Map<String, Long> times = new HashMap<>();
		session.setAttribute(SESSION_KEY, times);
		return times;
	}

	private static void writeCookie(HttpServletResponse response, HttpServletRequest request, String boardId, long now) {
		Cookie cookie = new Cookie(COOKIE_PREFIX + boardId, String.valueOf(now));
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure());
		// 브라우저가 24시간 뒤 자동으로 버린다. 값도 함께 확인하므로 시계 오차는 문제되지 않는다.
		cookie.setMaxAge((int) (WINDOW_MS / 1000L));
		response.addCookie(cookie);
	}
}
