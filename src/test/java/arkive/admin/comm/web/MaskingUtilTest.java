package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 표시용 개인정보 마스킹 규칙을 확인한다.
 */
class MaskingUtilTest {

	@Test
	void nameShouldKeepFirstCharacter() {
		assertThat(MaskingUtil.maskName("홍길동")).isEqualTo("홍**");
		assertThat(MaskingUtil.maskName("익명")).isEqualTo("익명");
	}

	@Test
	void emailShouldHideLocalPart() {
		assertThat(MaskingUtil.maskEmail("tester@example.com")).isEqualTo("te***@example.com");
		assertThat(MaskingUtil.maskEmail("ab@example.com")).isEqualTo("a***@example.com");
	}

	@Test
	void phoneShouldKeepLastFourDigits() {
		assertThat(MaskingUtil.maskPhone("010-1234-5678")).isEqualTo("010-****-5678");
	}

	@Test
	void unknownKeyShouldNotChangeValue() {
		assertThat(MaskingUtil.mask("title", "제목")).isEqualTo("제목");
	}
}
