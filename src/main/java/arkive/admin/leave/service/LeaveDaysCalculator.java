package arkive.admin.leave.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * 휴가 일수 계산.
 *
 * <p>주말과 HOLIDAY(공휴일)를 제외한 평일을 센다. 반차(HALF_AM·HALF_PM)는
 * 시작일과 종료일이 같은 하루일 때만 0.5일로 계산한다.
 * 이 규칙은 Arkive 기준값이며 대상 프로젝트의 휴가 정책에 맞게 교체한다.
 */
public final class LeaveDaysCalculator {

	public static final String TYPE_FULL = "FULL";
	public static final String TYPE_HALF_AM = "HALF_AM";
	public static final String TYPE_HALF_PM = "HALF_PM";

	private LeaveDaysCalculator() {
	}

	public static boolean isValidType(String leaveType) {
		return TYPE_FULL.equals(leaveType) || TYPE_HALF_AM.equals(leaveType) || TYPE_HALF_PM.equals(leaveType);
	}

	public static boolean isHalfDay(String leaveType) {
		return TYPE_HALF_AM.equals(leaveType) || TYPE_HALF_PM.equals(leaveType);
	}

	public static LocalDate parseDate(String value, String label) {
		try {
			return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException | NullPointerException e) {
			throw new IllegalArgumentException(label + "은(는) YYYY-MM-DD 형식으로 입력하세요.");
		}
	}

	/**
	 * @param holidayDates HOLIDAY.HOLIDAY_DE 값(YYYYMMDD) 집합
	 * @return 연차는 평일 수, 반차는 0.5
	 */
	public static double calculate(String leaveType, String startDe, String endDe, Set<String> holidayDates) {
		if (!isValidType(leaveType)) {
			throw new IllegalArgumentException("휴가 유형이 올바르지 않습니다.");
		}
		LocalDate start = parseDate(startDe, "시작일");
		LocalDate end = parseDate(endDe, "종료일");
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
		}
		if (start.getYear() != end.getYear()) {
			throw new IllegalArgumentException("휴가 기간은 같은 연도 안에서만 신청할 수 있습니다.");
		}
		if (isHalfDay(leaveType) && !start.equals(end)) {
			throw new IllegalArgumentException("반차는 시작일과 종료일이 같아야 합니다.");
		}
		int businessDays = 0;
		for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
			if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				continue;
			}
			if (holidayDates != null && holidayDates.contains(date.format(DateTimeFormatter.BASIC_ISO_DATE))) {
				continue;
			}
			businessDays++;
		}
		if (businessDays == 0) {
			throw new IllegalArgumentException("휴가 기간에 평일이 없습니다.");
		}
		return isHalfDay(leaveType) ? 0.5 : businessDays;
	}
}
