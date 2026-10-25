package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 초성 변환과 한글 포함 판별을 확인한다. 초성 검색의 기준값이 되는 유틸이다.
 */
class KoreanSearchUtilTest {

	@Test
	void shouldConvertHangulSyllablesToChosung() {
		assertThat(KoreanSearchUtil.toChosung("공지사항")).isEqualTo("ㄱㅈㅅㅎ");
		assertThat(KoreanSearchUtil.toChosung("초성검색")).isEqualTo("ㅊㅅㄱㅅ");
	}

	@Test
	void shouldKeepNonHangulCharactersAndHandleNull() {
		assertThat(KoreanSearchUtil.toChosung("공지 ABC 123")).isEqualTo("ㄱㅈ ABC 123");
		assertThat(KoreanSearchUtil.toChosung(null)).isEmpty();
		assertThat(KoreanSearchUtil.toChosung("")).isEmpty();
	}

	@Test
	void containsHangulShouldDetectSyllablesAndJamo() {
		assertThat(KoreanSearchUtil.containsHangul("공지")).isTrue();
		assertThat(KoreanSearchUtil.containsHangul("ㅊㅅ")).isTrue();
		assertThat(KoreanSearchUtil.containsHangul("ABC123")).isFalse();
		assertThat(KoreanSearchUtil.containsHangul(null)).isFalse();
	}
}
