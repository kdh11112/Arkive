package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 파일 실형식 검증. 확장자가 아니라 매직바이트로 판별하는지 확인한다.
 * DB 없이 실행되는 단위 테스트다.
 */
class FileTypeUtilTest {

	private static final byte[] PNG = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };
	private static final byte[] JPG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0 };
	private static final byte[] GIF = { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0, 0, 0 };
	private static final byte[] PDF = { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37 };
	private static final byte[] BMP = { 0x42, 0x4D, 0, 0, 0, 0, 0, 0 };

	@Test
	void detectShouldUseMagicBytes() {
		assertThat(FileTypeUtil.detect(PNG)).isEqualTo("PNG");
		assertThat(FileTypeUtil.detect(JPG)).isEqualTo("JPG");
		assertThat(FileTypeUtil.detect(GIF)).isEqualTo("GIF");
		assertThat(FileTypeUtil.detect(PDF)).isEqualTo("PDF");
		assertThat(FileTypeUtil.detect(BMP)).isEqualTo("BMP");
		assertThat(FileTypeUtil.detect("plain text".getBytes(StandardCharsets.UTF_8))).isEqualTo("UNKNOWN");
	}

	@Test
	void shortOrNullHeadShouldBeUnknown() {
		assertThat(FileTypeUtil.detect(null)).isEqualTo("UNKNOWN");
		assertThat(FileTypeUtil.detect(new byte[0])).isEqualTo("UNKNOWN");
		assertThat(FileTypeUtil.detect(new byte[] { 0x01, 0x02 })).isEqualTo("UNKNOWN");
	}

	@Test
	void spoofedExtensionShouldBeRejected() throws Exception {
		MockMultipartFile fakePng = new MockMultipartFile(
				"file", "fake.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));
		assertThat(FileTypeUtil.isAllowedImage(fakePng)).isFalse();
	}

	@Test
	void realImageWithMatchingExtensionShouldPass() throws Exception {
		MockMultipartFile png = new MockMultipartFile("file", "real.png", "image/png", PNG);
		assertThat(FileTypeUtil.isAllowedImage(png)).isTrue();
		assertThat(FileTypeUtil.isAllowedImage(png, "PNG")).isTrue();
		assertThat(FileTypeUtil.isAllowedImage(png, "JPG")).isFalse();
	}

	@Test
	void emptyNullAndUnknownExtensionShouldBeRejected() throws Exception {
		assertThat(FileTypeUtil.isAllowedImage(null)).isFalse();
		assertThat(FileTypeUtil.isAllowedImage(
				new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))).isFalse();
		assertThat(FileTypeUtil.isAllowedImage(
				new MockMultipartFile("file", "note.txt", "text/plain", PNG))).isFalse();
		assertThat(FileTypeUtil.isAllowedImage(
				new MockMultipartFile("file", "no-extension", "application/octet-stream", PNG))).isFalse();
	}
}
