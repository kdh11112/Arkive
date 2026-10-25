package arkive.admin.lecture.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import arkive.admin.lecture.service.VideoEncodingService.EncodedVideo;

/**
 * FFmpeg 비활성 상태의 업로드 저장·검증과 VOD 경로 이탈 차단을 확인한다.
 * 실제 인코딩은 FFmpeg 설치가 필요하므로 이 테스트에서는 다루지 않는다.
 */
class FfmpegVideoEncodingServiceTest {

	@TempDir
	private Path tempDir;

	private FfmpegVideoEncodingService service;

	@BeforeEach
	void setUp() {
		service = new FfmpegVideoEncodingService();
		ReflectionTestUtils.setField(service, "encodingEnabled", false);
		ReflectionTestUtils.setField(service, "vodPath", tempDir.toString());
		ReflectionTestUtils.setField(service, "ffmpegPath", tempDir.toString());
		ReflectionTestUtils.setField(service, "maxUploadMb", 1L);
	}

	@Test
	void disabledEncodingShouldCopyMp4AndKeepInputDuration() throws Exception {
		byte[] content = { 1, 2, 3, 4 };
		MockMultipartFile video = new MockMultipartFile("video", "sample.mp4", "video/mp4", content);

		EncodedVideo stored = service.store(video, "LEC-1", 120);

		assertThat(service.isEncodingEnabled()).isFalse();
		assertThat(stored.relativePath()).matches("\\d{6}/LEC-1\\.mp4");
		assertThat(stored.originalName()).isEqualTo("sample.mp4");
		assertThat(stored.durationSec()).isEqualTo(120);
		assertThat(stored.encoded()).isFalse();
		assertThat(Files.readAllBytes(service.resolve(stored.relativePath()))).isEqualTo(content);
	}

	@Test
	void disabledEncodingShouldRejectNonBrowserFormat() {
		MockMultipartFile video = new MockMultipartFile("video", "clip.avi", "video/avi", new byte[] { 1 });

		assertThatThrownBy(() -> service.store(video, "LEC-2", 120))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("인코딩이 꺼져");
	}

	@Test
	void disabledEncodingShouldRequireDuration() {
		MockMultipartFile video = new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[] { 1 });

		assertThatThrownBy(() -> service.store(video, "LEC-3", 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("재생 길이");
	}

	@Test
	void unsupportedExtensionAndEmptyFileShouldBeRejected() {
		MockMultipartFile text = new MockMultipartFile("video", "note.txt", "text/plain", new byte[] { 1 });
		MockMultipartFile empty = new MockMultipartFile("video", "empty.mp4", "video/mp4", new byte[0]);

		assertThatThrownBy(() -> service.store(text, "LEC-4", 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("지원하지 않는");
		assertThatThrownBy(() -> service.store(empty, "LEC-5", 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("동영상 파일");
	}

	@Test
	void oversizeFileShouldBeRejected() {
		byte[] content = new byte[1024 * 1024 + 1];
		MockMultipartFile video = new MockMultipartFile("video", "big.mp4", "video/mp4", content);

		assertThatThrownBy(() -> service.store(video, "LEC-6", 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("MB 이하");
	}

	@Test
	void resolveShouldBlockPathTraversal() {
		assertThatThrownBy(() -> service.resolve("../outside.mp4"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("경로");
	}

	@Test
	void deleteShouldRemoveStoredFileAndIgnoreMissingPath() throws Exception {
		MockMultipartFile video = new MockMultipartFile("video", "sample.mp4", "video/mp4", new byte[] { 1 });
		EncodedVideo stored = service.store(video, "LEC-7", 10);
		Path storedPath = service.resolve(stored.relativePath());

		service.delete(stored.relativePath());
		service.delete("not-found.mp4");

		assertThat(Files.exists(storedPath)).isFalse();
	}
}
