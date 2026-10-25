package arkive.admin.lecture.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.lecture.service.VideoEncodingService;

/**
 * FFmpeg 기반 동영상 인코딩. 실행 파일 경로와 VOD 경로는 환경 설정으로 분리한다.
 *
 * <p>명령은 셸을 거치지 않고 인자 배열로 실행해 파일명을 통한 명령 주입을 막는다.
 * ffmpeg 출력은 임시 로그 파일로 받아 파이프 적체를 피하고, 실패 시 원문을 응답에 노출하지 않는다.</p>
 */
@Service("videoEncodingService")
public class FfmpegVideoEncodingService implements VideoEncodingService {

	private static final Logger logger = LoggerFactory.getLogger(FfmpegVideoEncodingService.class);

	private static final Set<String> BROWSER_EXTENSIONS = Set.of("mp4", "webm");
	private static final Set<String> UPLOAD_EXTENSIONS = Set.of("mp4", "webm", "mov", "avi", "mkv", "m4v");
	private static final Pattern DURATION_PATTERN =
			Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+)(?:\\.(\\d+))?");

	@Value("${arkive.ffmpeg.enabled:false}")
	private boolean encodingEnabled;

	@Value("${arkive.ffmpeg.path:/usr/bin}")
	private String ffmpegPath;

	@Value("${arkive.ffmpeg.timeout-seconds:600}")
	private long timeoutSeconds;

	@Value("${arkive.vod.path:./data/vod/}")
	private String vodPath;

	@Value("${arkive.vod.max-upload-mb:100}")
	private long maxUploadMb;

	@Override
	public boolean isEncodingEnabled() {
		return encodingEnabled;
	}

	@Override
	public EncodedVideo store(MultipartFile video, String lectureId, int fallbackDurationSec) {
		if (video == null || video.isEmpty()) {
			throw new IllegalArgumentException("동영상 파일을 선택하세요.");
		}
		if (video.getSize() > maxUploadMb * 1024 * 1024) {
			throw new IllegalArgumentException("동영상은 " + maxUploadMb + "MB 이하만 업로드할 수 있습니다.");
		}
		String extension = extension(video.getOriginalFilename());
		if (!UPLOAD_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("지원하지 않는 동영상 형식입니다. mp4, webm, mov, avi, mkv, m4v만 가능합니다.");
		}
		if (!encodingEnabled && !BROWSER_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("인코딩이 꺼져 있어 " + extension + " 파일은 저장할 수 없습니다. mp4 또는 webm을 올리세요.");
		}
		if (!encodingEnabled && fallbackDurationSec <= 0) {
			throw new IllegalArgumentException("인코딩이 꺼져 있으면 재생 길이(초)를 직접 입력하세요.");
		}

		String relativePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
				+ "/" + lectureId + (encodingEnabled ? ".mp4" : "." + extension);
		Path destination = resolve(relativePath);
		try {
			Files.createDirectories(destination.getParent());
			if (!encodingEnabled) {
				copy(video, destination);
				return new EncodedVideo(relativePath, originalName(video), fallbackDurationSec, false);
			}
			return encode(video, destination, relativePath, fallbackDurationSec);
		} catch (IOException e) {
			logger.error("video store failed. lectureId={}", lectureId, e);
			throw new IllegalStateException("동영상 저장에 실패했습니다. 잠시 뒤 다시 시도하세요.");
		}
	}

	@Override
	public Path resolve(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			throw new IllegalArgumentException("동영상 경로가 없습니다.");
		}
		Path root = Paths.get(vodPath).toAbsolutePath().normalize();
		Path resolved = root.resolve(relativePath).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("동영상 경로가 올바르지 않습니다.");
		}
		return resolved;
	}

	@Override
	public void delete(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(resolve(relativePath));
		} catch (Exception e) {
			logger.warn("video delete failed. path={}", relativePath);
		}
	}

	private EncodedVideo encode(MultipartFile video, Path destination, String relativePath, int fallbackDurationSec)
			throws IOException {
		Path tempInput = Files.createTempFile("arkive-vod-", "." + extension(video.getOriginalFilename()));
		Path logFile = Files.createTempFile("arkive-ffmpeg-", ".log");
		try {
			copy(video, tempInput);
			String executable = executable();
			ProcessBuilder builder = new ProcessBuilder(
					executable,
					"-hide_banner", "-y",
					"-i", tempInput.toAbsolutePath().toString(),
					"-map", "0:v:0", "-map", "0:a?",
					"-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
					"-c:a", "aac", "-b:a", "128k",
					"-movflags", "+faststart",
					destination.toAbsolutePath().toString());
			builder.redirectErrorStream(true);
			builder.redirectOutput(logFile.toFile());
			Process process = builder.start();
			boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new IllegalStateException("동영상 인코딩 시간이 초과되었습니다. 파일 크기와 FFmpeg 설정을 확인하세요.");
			}
			String output = Files.readString(logFile);
			if (process.exitValue() != 0 || !Files.isRegularFile(destination)) {
				logger.error("ffmpeg failed. exit={}, lectureOutput={}", process.exitValue(), safeLog(output));
				throw new IllegalStateException("동영상 인코딩에 실패했습니다. FFmpeg 경로와 파일 형식을 확인하세요.");
			}
			int duration = parseDuration(output);
			if (duration <= 0) {
				duration = fallbackDurationSec;
			}
			if (duration <= 0) {
				throw new IllegalStateException("동영상 재생 길이를 확인하지 못했습니다. 길이(초)를 직접 입력하세요.");
			}
			return new EncodedVideo(relativePath, originalName(video), duration, true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("동영상 인코딩이 중단되었습니다.");
		} finally {
			Files.deleteIfExists(tempInput);
			Files.deleteIfExists(logFile);
		}
	}

	private void copy(MultipartFile video, Path destination) throws IOException {
		try (var input = video.getInputStream()) {
			Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String executable() {
		Path configured = Paths.get(ffmpegPath == null ? "" : ffmpegPath.trim());
		if (Files.isRegularFile(configured)) {
			return configured.toAbsolutePath().toString();
		}
		String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
				? "ffmpeg.exe" : "ffmpeg";
		return configured.resolve(name).toAbsolutePath().toString();
	}

	private int parseDuration(String output) {
		Matcher matcher = DURATION_PATTERN.matcher(output == null ? "" : output);
		if (!matcher.find()) {
			return 0;
		}
		int hours = Integer.parseInt(matcher.group(1));
		int minutes = Integer.parseInt(matcher.group(2));
		int seconds = Integer.parseInt(matcher.group(3));
		String fraction = matcher.group(4);
		int total = hours * 3600 + minutes * 60 + seconds;
		if (fraction != null && !fraction.isEmpty()) {
			total += 1; // 재생 길이는 올림해 90% 계산에서 유리하게 하지 않는다.
		}
		return total;
	}

	private String safeLog(String output) {
		if (output == null) {
			return "";
		}
		String trimmed = output.length() > 400 ? output.substring(output.length() - 400) : output;
		return trimmed.replaceAll("[\\r\\n]+", " ");
	}

	private String extension(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	private String originalName(MultipartFile video) {
		String name = video.getOriginalFilename();
		if (name == null || name.isBlank()) {
			return "video";
		}
		String base = Paths.get(name).getFileName().toString();
		return base.length() > 255 ? base.substring(0, 255) : base;
	}
}
