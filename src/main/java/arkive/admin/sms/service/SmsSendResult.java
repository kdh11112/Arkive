package arkive.admin.sms.service;

/**
 * SMS 발송 결과. 제공자 응답 원문은 로그·화면에 노출하지 않고 코드와 성공 여부만 남긴다.
 */
public record SmsSendResult(boolean success, String provider, String resultCode, String message) {

	public static SmsSendResult disabled(String provider) {
		return new SmsSendResult(false, provider, "DISABLED", "SMS 발송이 설정되지 않았습니다.");
	}

	public static SmsSendResult invalid(String provider, String message) {
		return new SmsSendResult(false, provider, "INVALID", message);
	}

	public static SmsSendResult success(String provider, String resultCode) {
		return new SmsSendResult(true, provider, resultCode, "OK");
	}

	public static SmsSendResult fail(String provider, String resultCode, String message) {
		return new SmsSendResult(false, provider, resultCode, message);
	}
}
