package arkive.admin.lecture.service;

import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * 강의형 콘텐츠 서비스.
 *
 * <p>강의는 동영상 1개와 선택 설문 1세트로 구성한다. 수료는 최대 재생 위치가
 * 재생 길이의 기준 비율(기본 90%)에 도달하고, 연결된 설문이 있으면 참여까지
 * 마쳐야 인정한다. 이 기준은 샘플 정책이므로 대상 프로젝트에서 교체한다.</p>
 */
public interface LectureService {

	List<EgovMap> selectLectureList(EgovMap egovMap) throws Exception;

	int selectLectureListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectAvailableLectureList(String userId) throws Exception;

	EgovMap selectLectureDetail(String lectureId) throws Exception;

	EgovMap selectLectureForView(String lectureId, String userId) throws Exception;

	void insertLecture(EgovMap egovMap, MultipartFile video) throws Exception;

	void updateLecture(EgovMap egovMap, MultipartFile video) throws Exception;

	void deleteLecture(String lectureId) throws Exception;

	void openLecture(String lectureId) throws Exception;

	void closeLecture(String lectureId) throws Exception;

	Map<String, Object> saveProgress(String lectureId, String userId, int watchedSec) throws Exception;

	List<EgovMap> selectCompletionList(EgovMap egovMap) throws Exception;

	int selectCompletionListTotCnt(EgovMap egovMap) throws Exception;
}
