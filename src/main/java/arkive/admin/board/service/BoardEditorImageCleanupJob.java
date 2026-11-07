package arkive.admin.board.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import arkive.admin.comm.service.FileService;
import jakarta.annotation.Resource;

/**
 * 에디터 본문 이미지 정리 실행기.
 *
 * <p>에디터 업로드({@code /board/editorImageUpload.json})는 글 저장 여부와 무관하게 파일을 확정한다.
 * 그래서 글을 쓰다가 이탈하면 {@code BOARD_EDITOR_} 그룹 이미지가 게시글 삭제 흐름에서 지워지지 않고 남는다.
 * 이 실행기는 보관 기간이 지난 후보 중 <b>어떤 게시글 본문도 참조하지 않는</b> 파일만 삭제한다.</p>
 *
 * <p>기본값은 0일(자동 삭제 안 함)이다. 실제 보관 기간은 운영 정책을 정한 뒤
 * {@code arkive.board.editor-image.retention-days}로 설정한다.</p>
 */
@Component
public class BoardEditorImageCleanupJob {

	private static final Logger logger = LoggerFactory.getLogger(BoardEditorImageCleanupJob.class);

	@Value("${arkive.board.editor-image.retention-days:0}")
	private int retentionDays;

	@Resource(name = "fileService")
	private FileService fileService;

	@Resource(name = "boardService")
	private BoardService boardService;

	@Scheduled(cron = "${arkive.board.editor-image.cleanup-cron:0 30 4 * * *}")
	public void purge() {
		if (retentionDays <= 0) {
			return;
		}
		try {
			Date before = Date.from(LocalDate.now().minusDays(retentionDays)
					.atStartOfDay(ZoneId.systemDefault()).toInstant());
			int deleted = purgeBefore(before);
			logger.info("board editor image purge completed. days={}, deleted={}", retentionDays, deleted);
		} catch (Exception e) {
			logger.warn("board editor image purge failed. days={}", retentionDays, e);
		}
	}

	/**
	 * 기준 시각 이전에 업로드된 에디터 이미지 중 게시글 본문에서 참조되지 않는 것만 삭제한다.
	 *
	 * @param before 이 시각 이전 등록분을 후보로 본다
	 * @return 실제로 삭제된 파일 수
	 */
	public int purgeBefore(Date before) throws Exception {
		if (before == null) {
			return 0;
		}
		List<EgovMap> candidates = fileService.getOldEditorFiles(before);
		int deleted = 0;
		for (EgovMap file : candidates) {
			String fileId = String.valueOf(file.get("fileId"));
			try {
				// 저장된 글의 본문이 참조하는 이미지는 지우지 않는다.
				if (boardService.countContentRef(fileId) > 0) {
					continue;
				}
				if (fileService.setDeleteFile(fileId)) {
					deleted++;
				}
			} catch (Exception e) {
				logger.warn("board editor image delete skipped. fileId={}", fileId, e);
			}
		}
		return deleted;
	}
}
