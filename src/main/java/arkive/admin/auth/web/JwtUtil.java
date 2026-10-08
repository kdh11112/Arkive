package arkive.admin.auth.web;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 발급·검증. 화면·관리는 세션, 외부 API·모바일은 JWT로 병행한다.
 * 시크릿·만료는 Globals에서 읽는다. 로그아웃 블랙리스트는 두지 않고 만료를 짧게 둔다.
 *
 * 열쇠 역할:
 * - createToken: 아이디+이름+발급시각+만료(1시간)를 묶어 서명한다. 결과가 xxxxx.yyyyy.zzzzz 3마디 토큰이다.
 * - parseToken: 서명을 검증한다. 틀리면·만료면 예외를 던진다.
 */
@Component
public class JwtUtil {

	private final SecretKey key;
	private final long expireSeconds;

	public JwtUtil(@Value("${Globals.jwt.secret}") String secret,
			@Value("${Globals.jwt.expireSeconds:3600}") long expireSeconds) {
		// 시크릿 문자열을 서명용 키로 바꾼다. 짧으면 Keys가 예외를 던진다(256비트 이상 필요).
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		// 토큰 유효시간(초). 없으면 3600(1시간)이 기본값이다.
		this.expireSeconds = expireSeconds;
	}

	/** 토큰 발급. subject=USER_ID, claim=userNm. */
	public String createToken(String userId, String userNm) {
		// 지금 시각(ms). 발급·만료 계산 기준이다.
		long now = System.currentTimeMillis();
		return Jwts.builder()
				// 토큰 주인. 검증 뒤 getSubject()로 꺼낸다.
				.subject(userId)
				// 화면 표시용 이름. 민감값은 넣지 않는다.
				.claim("userNm", userNm)
				// 발급 시각.
				.issuedAt(new Date(now))
				// 만료 시각. 지나면 parseToken이 예외를 던진다.
				.expiration(new Date(now + expireSeconds * 1000L))
				// 위 내용을 key로 서명한다. 서명이 맞아야 위조가 아니다.
				.signWith(key)
				// xxxxx.yyyyy.zzzzz 문자열로 만든다.
				.compact();
	}

	/** 검증 후 Claims. 실패하면 JwtException. */
	public Jws<Claims> parseToken(String token) {
		return Jwts.parser()
				// 이 키로 서명한 것만 받는다. 다른 키·무서명은 여기서 걸러진다.
				.verifyWith(key)
				.build()
				// 서명+만료 검사 후 내용(Claims)을 돌려준다.
				.parseSignedClaims(token);
	}

	public boolean isValid(String token) {
		try {
			parseToken(token);
			// 예외 없으면 살아있는 토큰이다.
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			// 서명 불일치·만료·형식 오류·빈 값은 전부 false다.
			return false;
		}
	}
}
