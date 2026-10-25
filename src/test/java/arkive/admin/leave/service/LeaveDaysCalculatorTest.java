package arkive.admin.leave.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * 휴가 일수 계산 규칙을 고정한다.
 * 주말·공휴일 제외, 반차 0.5일, 같은 연도 제한이 대상이다.
 */
class LeaveDaysCalculatorTest {

	@Test
	void fullDayShouldCountWeekdaysOnly() {
		// 2026-10-02(금) ~ 2026-10-05(월): 주말을 제외하고 2일
		assertThat(LeaveDaysCalculator.calculate("FULL", "2026-10-02", "2026-10-05", Set.of())).isEqualTo(2.0);
	}

	@Test
	void fullDayShouldExcludeHolidays() {
		// 2026-10-05(월) ~ 2026-10-09(금), 10-06 공휴일: 4일
		assertThat(LeaveDaysCalculator.calculate("FULL", "2026-10-05", "2026-10-09", Set.of("20261006")))
				.isEqualTo(4.0);
	}

	@Test
	void halfDayShouldBeHalfOnlyForSingleDay() {
		assertThat(LeaveDaysCalculator.calculate("HALF_AM", "2026-10-06", "2026-10-06", Set.of())).isEqualTo(0.5);
		assertThat(LeaveDaysCalculator.calculate("HALF_PM", "2026-10-06", "2026-10-06", Set.of())).isEqualTo(0.5);
		assertThatThrownBy(() -> LeaveDaysCalculator.calculate("HALF_AM", "2026-10-06", "2026-10-07", Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("반차는 시작일과 종료일이 같아야 합니다");
	}

	@Test
	void invalidRangeShouldBeRejected() {
		assertThatThrownBy(() -> LeaveDaysCalculator.calculate("FULL", "2026-10-09", "2026-10-05", Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("종료일은 시작일보다 빠를 수 없습니다");
		assertThatThrownBy(() -> LeaveDaysCalculator.calculate("FULL", "2026-12-31", "2027-01-02", Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("같은 연도");
		assertThatThrownBy(() -> LeaveDaysCalculator.calculate("FULL", "2026-10-10", "2026-10-11", Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("평일이 없습니다");
	}

	@Test
	void unknownTypeShouldBeRejected() {
		assertThatThrownBy(() -> LeaveDaysCalculator.calculate("ETC", "2026-10-06", "2026-10-06", Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("휴가 유형");
	}
}
