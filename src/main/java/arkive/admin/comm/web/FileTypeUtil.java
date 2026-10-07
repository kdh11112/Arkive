package arkive.admin.comm.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 실형식 검증. 확장자가 아닌 매직바이트로 판별한다.
 * TIKA 풀파서 대신 경량 검사만 둔다(무거움·신규 의존성 회피).
 * 허용 MIME은 호출자가 화이트리스트로 정한다.
 */
public class FileTypeUtil {

	private static final Map<String, byte[]> MAGIC = new HashMap<>();

	static {
		MAGIC.put("PNG", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
		MAGIC.put("JPG", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
		MAGIC.put("GIF87a", new byte[] { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 });
		MAGIC.put("GIF89a", new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 });
		MAGIC.put("PDF", new byte[] { 0x25, 0x50, 0x44, 0x46 });
		MAGIC.put("BMP", new byte[] { 0x42, 0x4D });
	}

	/**
	 * 파일 앞 바이트로 실형식을 판별한다. text를 .png로 바꾼 경우 PNG가 아니다.
	 *
	 * @return PNG/JPG/GIF/PDF/BMP, 판별 불가 시 UNKNOWN
	 */
	public static String detect(byte[] head) {
		if (head == null || head.length < 3) {
			return "UNKNOWN";
		}
		for (Map.Entry<String, byte[]> e : MAGIC.entrySet()) {
			byte[] magic = e.getValue();
			if (head.length < magic.length) {
				continue;
			}
			boolean match = true;
			for (int i = 0; i < magic.length; i++) {
				if (head[i] != magic[i]) {
					match = false;
					break;
				}
			}
			if (match) {
				String key = e.getKey();
				return key.startsWith("GIF") ? "GIF" : key;
			}
		}
		return "UNKNOWN";
	}

	/**
	 * 이미지 업로드 허용 검사. 확장자 기대 MIME과 매직바이트 실형식이 일치해야 한다.
	 */
	public static boolean isAllowedImage(MultipartFile file, String... allowedTypes) throws IOException {
		if (file == null || file.isEmpty()) {
			return false;
		}
		String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toUpperCase() : "";
		String expected;
		if ("PNG".equals(ext)) {
			expected = "PNG";
		} else if ("JPG".equals(ext) || "JPEG".equals(ext)) {
			expected = "JPG";
		} else if ("GIF".equals(ext)) {
			expected = "GIF";
		} else if ("BMP".equals(ext)) {
			expected = "BMP";
		} else {
			return false;
		}
		if (allowedTypes != null && allowedTypes.length > 0
				&& !Arrays.asList(allowedTypes).contains(expected)) {
			return false;
		}
		byte[] head = new byte[8];
		try (InputStream in = file.getInputStream()) {
			int read = in.read(head);
			if (read < 3) {
				return false;
			}
		}
		return expected.equals(detect(head));
	}
}
