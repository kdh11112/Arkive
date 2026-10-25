package arkive.admin.comm.web;

/**
 * 화면 표시용 개인정보 마스킹.
 *
 * <p>저장 원본은 바꾸지 않는다. 목록·엑셀처럼 여러 사람이 보는 출력에서만 사용하고,
 * 수정 화면의 원본 값은 그대로 둔다. 실제 마스킹 대상·범위는 개인정보 처리방침과
 * 담당자 확인 후 조정한다.</p>
 */
public final class MaskingUtil {

	private MaskingUtil() {
	}

	public static String mask(String key, String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		String normalized = key == null ? "" : key.toLowerCase();
		if (normalized.equals("usernm") || normalized.equals("registernm") || normalized.equals("register")
				|| normalized.equals("answerer") || normalized.equals("sendernm")
				|| normalized.equals("receivernm") || normalized.equals("counselnm")
				|| normalized.equals("dutyuser") || normalized.equals("writernm")) {
			return maskName(value);
		}
		if (normalized.equals("email")) {
			return maskEmail(value);
		}
		if (normalized.equals("telno") || normalized.equals("phone") || normalized.equals("hpno")
				|| normalized.equals("mobileno") || normalized.equals("mbtlnum")
				|| normalized.equals("phonenum")) {
			return maskPhone(value);
		}
		if (normalized.equals("cardno") || normalized.equals("accountno")) {
			return maskTail(value, 4);
		}
		if (normalized.equals("birthdt") || normalized.equals("birthday") || normalized.equals("birth")) {
			return maskBirth(value);
		}
		if (normalized.equals("addr") || normalized.equals("address") || normalized.equals("addrdetail")) {
			return maskAddress(value);
		}
		return value;
	}

	public static String maskName(String value) {
		if (value == null || value.isEmpty() || "익명".equals(value) || "SYSTEM".equalsIgnoreCase(value)) {
			return value;
		}
		if (value.length() == 1) {
			return value;
		}
		return value.charAt(0) + "*".repeat(Math.min(value.length() - 1, 4));
	}

	public static String maskEmail(String value) {
		int at = value.indexOf('@');
		if (at <= 0) {
			return maskTail(value, 2);
		}
		String local = value.substring(0, at);
		String domain = value.substring(at);
		if (local.length() <= 2) {
			return local.charAt(0) + "***" + domain;
		}
		return local.substring(0, 2) + "***" + domain;
	}

	public static String maskPhone(String value) {
		String digits = value.replaceAll("[^0-9]", "");
		if (digits.length() < 7) {
			return maskTail(value, 2);
		}
		String tail = digits.substring(digits.length() - 4);
		return digits.substring(0, Math.min(3, digits.length())) + "-****-" + tail;
	}

	public static String maskBirth(String value) {
		String digits = value.replaceAll("[^0-9]", "");
		if (digits.length() < 4) {
			return "****";
		}
		return digits.substring(0, 4) + "-**-**";
	}

	public static String maskAddress(String value) {
		if (value.length() <= 10) {
			return value.charAt(0) + "***";
		}
		return value.substring(0, 10) + "***";
	}

	private static String maskTail(String value, int visible) {
		if (value.length() <= visible) {
			return "*".repeat(value.length());
		}
		return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
	}
}
