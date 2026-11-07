package arkive.admin.board.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.admin.comm.service.FileService;
import arkive.support.ArkiveIntegrationTest;

/**
 * 에디터 이미지 정리 배치를 실제 DB·파일로 확인한다.
 * 저장된 글 본문이 참조하는 이미지는 남기고, 글을 저장하지 않고 버려진 오래된 이미지만 지우는지 본다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardEditorImageCleanupJobTest {

	@Autowired
	private BoardEditorImageCleanupJob cleanupJob;

	@Autowired
	private FileService fileService;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private BoardService boardService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<Path> filesToDelete = new ArrayList<>();

	@AfterEach
	void deletePhysicalFiles() {
		for (Path path : filesToDelete) {
			try {
				Files.deleteIfExists(path);
			} catch (Exception e) {
				// 정리 실패는 테스트 결과에 영향을 주지 않는다
			}
		}
	}

	@Test
	@Rollback
	void purgeShouldDeleteOnlyUnreferencedOldEditorImages() throws Exception {
		String referencedId = "98222222222222222221";
		String orphanId = "98222222222222222222";
		String recentId = "98222222222222222223";

		Path referencedFile = newEditorFile("purge_ref_");
		Path orphanFile = newEditorFile("purge_orphan_");
		Path recentFile = newEditorFile("purge_recent_");

		Timestamp old = Timestamp.valueOf(LocalDateTime.now().minusDays(30));
		insertAtchFile(referencedId, old, referencedFile);
		insertAtchFile(orphanId, old, orphanFile);
		insertAtchFile(recentId, Timestamp.valueOf(LocalDateTime.now()), recentFile);

		// 저장된 글 본문이 참조하는 이미지
		boardMapper.insertBoard(BoardFixtures.board("BDPURGEREF01", "CK", "이미지 참조 글",
				"<p><img src=\"/file/downloadFile.do?downloadFileId=" + referencedId + "\"></p>"));

		Date before = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
		int deleted = cleanupJob.purgeBefore(before);

		assertThat(deleted).isEqualTo(1);
		// 글 본문이 참조하는 이미지는 남는다
		assertThat(fileService.getFileInfo(referencedId)).isNotNull();
		assertThat(Files.exists(referencedFile)).isTrue();
		// 버려진 오래된 이미지는 DB 행과 실물이 모두 지워진다
		assertThat(fileService.getFileInfo(orphanId)).isNull();
		assertThat(Files.exists(orphanFile)).isFalse();
		// 보관 기간이 지나지 않은 이미지는 남는다
		assertThat(fileService.getFileInfo(recentId)).isNotNull();
		assertThat(Files.exists(recentFile)).isTrue();
	}

	@Test
	@Rollback
	void purgeShouldDoNothingWhenRetentionDisabled() throws Exception {
		String orphanId = "98222222222222222224";
		Path orphanFile = newEditorFile("purge_disabled_");
		insertAtchFile(orphanId, Timestamp.valueOf(LocalDateTime.now().minusDays(30)), orphanFile);

		ReflectionTestUtils.setField(cleanupJob, "retentionDays", 0);
		cleanupJob.purge();

		assertThat(fileService.getFileInfo(orphanId)).isNotNull();
		assertThat(Files.exists(orphanFile)).isTrue();
	}

	@Test
	@Rollback
	void countContentRefShouldTreatBlankFileIdAsNoReference() throws Exception {
		assertThat(boardService.countContentRef(null)).isZero();
		assertThat(boardService.countContentRef("")).isZero();
	}

	private Path newEditorFile(String prefix) throws Exception {
		Path realRoot = Path.of(fileService.getWritableRealPath());
		Files.createDirectories(realRoot);
		Path file = realRoot.resolve(prefix + System.nanoTime() + ".png");
		Files.write(file, new byte[] { 1, 2, 3 });
		filesToDelete.add(file);
		return file;
	}

	private void insertAtchFile(String fileId, Timestamp regDt, Path path) {
		jdbcTemplate.update("INSERT INTO ATCH_FILE (FILE_ID, ATCH_FILE_GRPID, ORGNL_FILE_NM, PHYS_FILE_NM,"
						+ " FILE_PATH, FILE_SZ, FILE_EXT, REG_DT, RGTR_ID)"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				fileId, "BOARD_EDITOR_" + fileId, "editor.png", path.getFileName().toString(),
				path.toString(), path.toFile().length(), "png", regDt, "tester");
	}
}
