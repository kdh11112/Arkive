package arkive.admin.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

/**
 * JWT 발급·검증 단위 테스트. DB나 Spring 컨텍스트 없이 돈다.
 * 시크릿이 바뀌면 토큰이 무효가 되는지, 만료·형식 오류가 false로 처리되는지 확인한다.
 */
class JwtUtilTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef";
	private static final String OTHER_SECRET = "abcdef0123456789abcdef0123456789";

	@Test
	void createAndParseShouldCarrySubjectAndName() {
		JwtUtil jwtUtil = new JwtUtil(SECRET, 60);

		String token = jwtUtil.createToken("user1", "사용자1");
		Jws<Claims> parsed = jwtUtil.parseToken(token);

		assertThat(parsed.getPayload().getSubject()).isEqualTo("user1");
		assertThat(parsed.getPayload().get("userNm")).isEqualTo("사용자1");
		assertThat(jwtUtil.isValid(token)).isTrue();
	}

	@Test
	void tokenFromAnotherSecretShouldBeInvalid() {
		JwtUtil issuer = new JwtUtil(SECRET, 60);
		JwtUtil other = new JwtUtil(OTHER_SECRET, 60);

		String token = issuer.createToken("user1", "사용자1");

		assertThat(other.isValid(token)).isFalse();
		assertThatThrownBy(() -> other.parseToken(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void expiredOrMalformedTokenShouldBeInvalid() {
		JwtUtil expired = new JwtUtil(SECRET, -1);
		String expiredToken = expired.createToken("user1", "사용자1");

		assertThat(expired.isValid(expiredToken)).isFalse();
		assertThat(expired.isValid("not-a-jwt")).isFalse();
		assertThat(expired.isValid(null)).isFalse();
	}

	@Test
	void blankSecretShouldGenerateUsableRandomKey() {
		JwtUtil first = new JwtUtil("", 60);
		String token = first.createToken("user1", "사용자1");

		assertThat(first.isValid(token)).isTrue();
		// 다른 인스턴스는 다른 임의 키를 쓰므로 재기동(새 인스턴스) 후 토큰은 무효다.
		assertThat(new JwtUtil("", 60).isValid(token)).isFalse();
	}
}
