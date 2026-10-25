package arkive.admin.lecture.service;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

/**
 * 강의 동영상 저장·인코딩.
 *
 * <p>FFmpeg 인코딩이 꺼져 있으면 브라우저 재생 가능한 mp4/webm만 저장하고
 * 재생 길이는 관리자가 입력한 값을 쓴다. 인코딩이 켜져 있으면 mp4(H.264/AAC)로 변환한다.</p>
 */
public interface VideoEncodingService {

	boolean isEncodingEnabled();

	/**
	 * 업로드 파일을 VOD 경로에 저장한다. 반환 경로는 VOD 루트 기준 상대 경로다.
	 * {@code fallbackDurationSec}는 인코딩이 꺼져 있거나 길이를 읽지 못했을 때 사용한다.
	 */
	EncodedVideo store(MultipartFile video, String lectureId, int fallbackDurationSec);

	/** VOD 루트 밖으로 나가는 경로를 차단하며 실제 파일 경로를 찾는다. */
	Path resolve(String relativePath);

	void delete(String relativePath);

	record EncodedVideo(String relativePath, String originalName, int durationSec, boolean encoded) {
	}
}
