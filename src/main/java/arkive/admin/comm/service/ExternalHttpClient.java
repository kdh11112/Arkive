package arkive.admin.comm.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 HTTP 호출 공통 클라이언트.
 *
 * <p>연결·응답 시간 제한을 두고, 재시도해도 안전한 GET만 설정된 횟수만큼 재시도한다.
 * 토큰 교환·문자 발송처럼 부작용이 있는 POST는 중복 발송을 막기 위해 재시도하지 않는다.
 * 외부 API 키와 응답 본문은 로그에 남기지 않는다.</p>
 */
@Service("externalHttpClient")
public class ExternalHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(ExternalHttpClient.class);

	private final RestTemplate restTemplate;
	private final int getMaxAttempts;
	private final long retryBackoffMs;

	@Autowired
	public ExternalHttpClient(
			@Value("${arkive.http.connect-timeout-ms:3000}") int connectTimeoutMs,
			@Value("${arkive.http.read-timeout-ms:5000}") int readTimeoutMs,
			@Value("${arkive.http.get-max-attempts:3}") int getMaxAttempts,
			@Value("${arkive.http.retry-backoff-ms:200}") long retryBackoffMs) {
		this(buildRestTemplate(connectTimeoutMs, readTimeoutMs), getMaxAttempts, retryBackoffMs);
	}

	// 테스트에서 MockRestServiceServer를 붙일 수 있게 RestTemplate을 주입받는 생성자를 둔다.
	ExternalHttpClient(RestTemplate restTemplate, int getMaxAttempts, long retryBackoffMs) {
		this.restTemplate = restTemplate;
		this.getMaxAttempts = Math.max(1, getMaxAttempts);
		this.retryBackoffMs = Math.max(0, retryBackoffMs);
	}

	private static RestTemplate buildRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		RestTemplate template = new RestTemplate(factory);
		// 외부 API가 UTF-8 JSON을 반환할 때 한글이 깨지지 않게 한다.
		template.getMessageConverters().removeIf(converter -> converter instanceof StringHttpMessageConverter);
		template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		return template;
	}

	/**
	 * GET 요청. 네트워크 오류와 5xx만 재시도한다.
	 */
	public String get(String url, HttpHeaders headers) {
		RuntimeException last = null;
		for (int attempt = 1; attempt <= getMaxAttempts; attempt++) {
			try {
				ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
						new HttpEntity<>(headers == null ? new HttpHeaders() : headers), String.class);
				return response.getBody();
			} catch (ResourceAccessException | HttpServerErrorException e) {
				last = e;
				logger.warn("external GET failed. attempt={}/{}, url={}", attempt, getMaxAttempts, safeUrl(url));
				if (attempt < getMaxAttempts) {
					sleep();
				}
			}
		}
		throw last == null ? new IllegalStateException("외부 GET 요청에 실패했습니다.") : last;
	}

	/**
	 * application/x-www-form-urlencoded POST. 재시도하지 않는다.
	 */
	public String postForm(String url, MultiValueMap<String, String> form) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers),
				String.class);
		return response.getBody();
	}

	/**
	 * JSON POST. 색인 저장처럼 같은 본문을 다시 보내도 결과가 같은 요청에 쓴다. 재시도하지 않는다.
	 */
	public String postJson(String url, String body, HttpHeaders headers) {
		return exchangeJson(url, HttpMethod.POST, body, headers);
	}

	/**
	 * JSON PUT. 멱등 색인 저장에 쓴다. 재시도하지 않는다.
	 */
	public String putJson(String url, String body, HttpHeaders headers) {
		return exchangeJson(url, HttpMethod.PUT, body, headers);
	}

	/**
	 * DELETE. 재시도하지 않는다.
	 */
	public String delete(String url, HttpHeaders headers) {
		HttpHeaders requestHeaders = headers == null ? new HttpHeaders() : headers;
		requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE,
				new HttpEntity<>(requestHeaders), String.class);
		return response.getBody();
	}

	private String exchangeJson(String url, HttpMethod method, String body, HttpHeaders headers) {
		HttpHeaders requestHeaders = headers == null ? new HttpHeaders() : headers;
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = restTemplate.exchange(url, method,
				new HttpEntity<>(body, requestHeaders), String.class);
		return response.getBody();
	}

	private void sleep() {
		if (retryBackoffMs <= 0) {
			return;
		}
		try {
			Thread.sleep(retryBackoffMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String safeUrl(String url) {
		if (url == null) {
			return "";
		}
		int query = url.indexOf('?');
		return query < 0 ? url : url.substring(0, query);
	}
}
