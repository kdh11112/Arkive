package arkive.admin.comm.web;

/**
 * 한글 초성 검색 보조.
 *
 * <p>한글 음절(가~힣)을 초성 19자로 바꾸고, 영문·숫자·기호는 그대로 둔다.
 * 검색어가 "ㅎㄱ" 같은 초성이면 저장된 초성 색인과 비교하고, 일반 문자열이면
 * 기존 제목 LIKE와 함께 OR로 조회한다.</p>
 */
public final class KoreanSearchUtil {

	private static final char[] CHOSUNG = {
			'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
			'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	private KoreanSearchUtil() {
	}

	public static String toChosung(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		StringBuilder result = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch >= 0xAC00 && ch <= 0xD7A3) {
				int index = (ch - 0xAC00) / (21 * 28);
				result.append(CHOSUNG[index]);
			} else {
				result.append(ch);
			}
		}
		return result.toString();
	}

	public static boolean containsHangul(String text) {
		if (text == null) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if ((ch >= 0xAC00 && ch <= 0xD7A3) || (ch >= 0x3131 && ch <= 0x318E)) {
				return true;
			}
		}
		return false;
	}
}
