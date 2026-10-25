package arkive.admin.search.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import arkive.admin.search.service.SearchIndexService;
import jakarta.annotation.Resource;

/**
 * 검색 색인 갱신 리스너.
 *
 * <p>게시글 저장 트랜잭션이 끝난 뒤 비동기로 색인한다. Elasticsearch가 꺼져 있거나
 * 장애가 나도 원본 저장에는 영향을 주지 않는다.</p>
 */
@Component
public class SearchIndexEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SearchIndexEventListener.class);

	@Resource(name = "searchIndexService")
	private SearchIndexService searchIndexService;

	@Async("mailTaskExecutor")
	@TransactionalEventListener(fallbackExecution = true)
	public void handle(SearchIndexEvent event) {
		if (!searchIndexService.isEnabled()) {
			return;
		}
		try {
			if (event.delete()) {
				searchIndexService.deleteBoard(event.boardId());
			} else {
				searchIndexService.indexBoard(event.boardId());
			}
		} catch (Exception e) {
			logger.warn("search index event failed. boardId={}, reason={}", event.boardId(),
					e.getClass().getSimpleName());
		}
	}
}
