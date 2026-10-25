package arkive.admin.search.service.impl;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.board.service.BoardService;
import arkive.admin.comm.service.ExternalHttpClient;
import arkive.admin.search.service.SearchIndexService;
import jakarta.annotation.Resource;

/**
 * Elasticsearch 검색 색인 어댑터.
 *
 * <p>Elasticsearch 클라이언트 의존성을 추가하지 않고 HTTP JSON만 사용한다. 인덱스 매핑은
 * 최초 사용 시 생성하며, 이미 있으면 그대로 쓴다. 검색 질의·색인 저장이 실패하면 예외를
 * 밖으로 던지지 않고 DB 검색 폴백을 유도한다.</p>
 */
@Service("searchIndexService")
public class ElasticsearchSearchIndexService implements SearchIndexService {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSearchIndexService.class);

	@Value("${arkive.search.elasticsearch.enabled:false}")
	private boolean enabled;

	@Value("${arkive.search.elasticsearch.url:http://localhost:9200}")
	private String baseUrl;

	@Value("${arkive.search.elasticsearch.index:arkive-board}")
	private String indexName;

	@Value("${arkive.search.elasticsearch.username:}")
	private String username;

	@Value("${arkive.search.elasticsearch.password:}")
	private String password;

	@Resource(name = "externalHttpClient")
	private ExternalHttpClient externalHttpClient;

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "searchMapper")
	private SearchMapper searchMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private volatile boolean indexReady;

	@Override
	public boolean isEnabled() {
		return enabled && baseUrl != null && !baseUrl.isBlank()
				&& indexName != null && indexName.matches("^[a-z0-9._-]{1,100}$");
	}

	@Override
	public SearchResult search(EgovMap param) {
		if (!isEnabled() || !ensureIndex()) {
			return null;
		}
		try {
			String keyword = String.valueOf(param.get("searchKeyword") == null ? "" : param.get("searchKeyword")).trim();
			if (keyword.isEmpty()) {
				return null;
			}
			int firstIndex = toInt(param.get("firstIndex"), 0);
			int size = toInt(param.get("recordCountPerPage"), 10);
			Map<String, Object> query = new LinkedHashMap<>();
			query.put("from", Math.max(0, firstIndex));
			query.put("size", Math.max(1, Math.min(size, 100)));
			query.put("track_total_hits", true);
			query.put("query", Map.of(
					"bool", Map.of(
							"filter", List.of(Map.of("terms", Map.of("boardType", List.of("CK", "TOAST", "FAQ", "QNA")))),
							"should", List.of(
									Map.of("match_phrase", Map.of("title", keyword)),
									Map.of("wildcard", Map.of("titleChosung", "*" + keyword + "*"))),
							"minimum_should_match", 1)));
			query.put("sort", List.of(
					Map.of("registDt", Map.of("order", "desc")),
					Map.of("boardId", Map.of("order", "desc"))));
			String response = externalHttpClient.postJson(indexUrl() + "/_search",
					objectMapper.writeValueAsString(query), headers());
			JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
			int total = root.path("hits").path("total").path("value").asInt(0);
			if (total == 0) {
				// 색인이 아직 채워지지 않았을 수 있으므로 DB 검색으로 폴백한다.
				return null;
			}
			List<EgovMap> list = new ArrayList<>();
			for (JsonNode hit : root.path("hits").path("hits")) {
				JsonNode source = hit.path("_source");
				EgovMap row = new EgovMap();
				row.put("boardId", source.path("boardId").asText());
				row.put("boardType", source.path("boardType").asText());
				row.put("title", source.path("title").asText());
				row.put("content", source.path("content").asText(""));
				row.put("register", source.path("register").asText(""));
				row.put("registDt", parseTimestamp(source.path("registDt").asText(null)));
				list.add(row);
			}
			return new SearchResult(list, total);
		} catch (Exception e) {
			logger.warn("elasticsearch search failed; fallback to DB. reason={}", e.getClass().getSimpleName());
			return null;
		}
	}

	@Override
	public void indexBoard(String boardId) {
		if (!isEnabled() || !ensureIndex()) {
			return;
		}
		try {
			EgovMap detail = boardService.selectBoardDetail(boardId);
			if (detail == null || !isIndexable(String.valueOf(detail.get("boardType")))) {
				return;
			}
			Map<String, Object> document = new LinkedHashMap<>();
			document.put("boardId", String.valueOf(detail.get("boardId")));
			document.put("boardType", String.valueOf(detail.get("boardType")));
			document.put("title", String.valueOf(detail.get("title")));
			document.put("titleChosung", arkive.admin.comm.web.KoreanSearchUtil.toChosung(
					String.valueOf(detail.get("title"))));
			document.put("content", String.valueOf(detail.get("content") == null ? "" : detail.get("content")));
			document.put("register", String.valueOf(detail.get("register") == null ? "" : detail.get("register")));
			document.put("registDt", toIso(detail.get("registDt")));
			externalHttpClient.putJson(indexUrl() + "/_doc/" + urlSegment(boardId),
					objectMapper.writeValueAsString(document), headers());
		} catch (Exception e) {
			logger.warn("elasticsearch index failed. boardId={}, reason={}", boardId, e.getClass().getSimpleName());
		}
	}

	@Override
	public void deleteBoard(String boardId) {
		if (!isEnabled() || !ensureIndex()) {
			return;
		}
		try {
			externalHttpClient.delete(indexUrl() + "/_doc/" + urlSegment(boardId), headers());
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
				logger.warn("elasticsearch delete failed. boardId={}, status={}", boardId, e.getStatusCode());
			}
		} catch (Exception e) {
			logger.warn("elasticsearch delete failed. boardId={}, reason={}", boardId, e.getClass().getSimpleName());
		}
	}

	@Override
	public int reindexAll() throws Exception {
		if (!isEnabled() || !ensureIndex()) {
			return 0;
		}
		List<String> ids = searchMapper.selectIndexableBoardIds();
		int count = 0;
		for (String id : ids) {
			indexBoard(id);
			count++;
		}
		return count;
	}

	private boolean ensureIndex() {
		if (indexReady) {
			return true;
		}
		try {
			Map<String, Object> mappings = Map.of("mappings", Map.of("properties", Map.of(
					"boardId", Map.of("type", "keyword"),
					"boardType", Map.of("type", "keyword"),
					"title", Map.of("type", "text"),
					"titleChosung", Map.of("type", "keyword"),
					"content", Map.of("type", "text", "index", false),
					"register", Map.of("type", "keyword"),
					"registDt", Map.of("type", "date"))));
			externalHttpClient.putJson(indexUrl(), objectMapper.writeValueAsString(mappings), headers());
			indexReady = true;
			return true;
		} catch (HttpClientErrorException e) {
			// 400 resource_already_exists_exception, 409도 이미 인덱스가 있다는 뜻이다.
			if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.CONFLICT) {
				indexReady = true;
				return true;
			}
			logger.warn("elasticsearch index check failed. status={}", e.getStatusCode());
			return false;
		} catch (ResourceAccessException | HttpServerErrorException e) {
			logger.warn("elasticsearch unavailable. reason={}", e.getClass().getSimpleName());
			return false;
		} catch (Exception e) {
			logger.warn("elasticsearch index check failed. reason={}", e.getClass().getSimpleName());
			return false;
		}
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		if (username != null && !username.isBlank()) {
			String credentials = username + ":" + (password == null ? "" : password);
			headers.set(HttpHeaders.AUTHORIZATION, "Basic "
					+ Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
		}
		return headers;
	}

	private String indexUrl() {
		String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return base + "/" + indexName;
	}

	private String urlSegment(String value) {
		return value.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private boolean isIndexable(String boardType) {
		return "CK".equals(boardType) || "TOAST".equals(boardType)
				|| "FAQ".equals(boardType) || "QNA".equals(boardType);
	}

	private int toInt(Object value, int fallback) {
		try {
			return value == null ? fallback : Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private String toIso(Object value) {
		if (value instanceof java.util.Date date) {
			return date.toInstant().toString();
		}
		return String.valueOf(value == null ? "" : value);
	}

	private Timestamp parseTimestamp(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Timestamp.from(Instant.parse(value));
		} catch (Exception e) {
			return null;
		}
	}
}
