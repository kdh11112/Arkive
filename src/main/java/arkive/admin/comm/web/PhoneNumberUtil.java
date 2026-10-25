package arkive.admin.comm.web;

import java.util.regex.Pattern;

/**
 * 휴대전화 번호 정규화·검증.
 *
 * <p>DB와 SMS 제공자마다 하이픈·국가번호 표기가 달라 저장 전에 숫자만 남긴다.
 * 한국 휴대전화 형식(010/011/016/017/018/019)만 허용하고, 그 외 번호는 발송 대상에서 제외한다.
 * 대상 프로젝트의 국가·번호 정책이 다르면 이 클래스만 교체한다.</p>
 */
public final class PhoneNumberUtil {

	private static final Pattern MOBILE = Pattern.compile("^01[016789][0-9]{7,8}$");

	private PhoneNumberUtil() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String digits = value.replaceAll("[^0-9]", "");
		if (digits.startsWith("82")) {
			digits = "0" + digits.substring(2);
		}
		return digits.isEmpty() ? null : digits;
	}

	public static boolean isValidMobile(String value) {
		String normalized = normalize(value);
		return normalized != null && MOBILE.matcher(normalized).matches();
	}

	public static String normalizeMobile(String value) {
		return isValidMobile(value) ? normalize(value) : null;
	}
}
