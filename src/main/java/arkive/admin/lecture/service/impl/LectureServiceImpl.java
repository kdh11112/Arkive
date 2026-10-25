package arkive.admin.lecture.service.impl;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.lecture.service.LectureService;
import arkive.admin.lecture.service.VideoEncodingService;
import arkive.admin.lecture.service.VideoEncodingService.EncodedVideo;
import arkive.admin.survey.service.SurveyService;
import jakarta.annotation.Resource;

/**
 * 강의형 콘텐츠 서비스 구현.
 *
 * <p>진도는 사용자별 최대 재생 위치로 저장한다. 수료 기준 비율(기본 90%)에 도달하고
 * 연결된 설문이 있으면 참여까지 마친 경우에만 수료로 계산한다. 강의는 작성중(DRAFT)
 * 상태에서만 수정·삭제할 수 있고, 공개하려면 동영상과 재생 길이가 있어야 한다.</p>
 */
@Service("lectureService")
public class LectureServiceImpl extends EgovAbstractServiceImpl implements LectureService {

	private static final String DRAFT = "DRAFT";
	private static final String OPEN = "OPEN";
	private static final int DEFAULT_COMPLETE_RATE = 90;
	private static final int MAX_TITLE = 200;
	private static final int MAX_DESCRIPTION = 2000;

	@Resource(name = "lectureMapper")
	private LectureMapper lectureMapper;

	@Resource(name = "surveyService")
	private SurveyService surveyService;

	@Resource(name = "videoEncodingService")
	private VideoEncodingService videoEncodingService;

	@Override
	public List<EgovMap> selectLectureList(EgovMap egovMap) throws Exception {
		return lectureMapper.selectLectureList(egovMap);
	}

	@Override
	public int selectLectureListTotCnt(EgovMap egovMap) throws Exception {
		return lectureMapper.selectLectureListTotCnt(egovMap);
	}

	@Override
	public List<EgovMap> selectAvailableLectureList(String userId) throws Exception {
		EgovMap param = new EgovMap();
		param.put("userId", userId);
		List<EgovMap> lectures = lectureMapper.selectAvailableLectureList(param);
		for (EgovMap lecture : lectures) {
			enrich(lecture);
		}
		return lectures;
	}

	@Override
	public EgovMap selectLectureDetail(String lectureId) throws Exception {
		return isBlank(lectureId) ? null : lectureMapper.selectLectureDetail(lectureId.trim());
	}

	@Override
	public EgovMap selectLectureForView(String lectureId, String userId) throws Exception {
		if (isBlank(lectureId)) {
			throw new IllegalArgumentException("강의가 없습니다.");
		}
		EgovMap param = new EgovMap();
		param.put("lectureId", lectureId.trim());
		param.put("userId", userId);
		EgovMap lecture = lectureMapper.selectLectureForView(param);
		if (lecture == null) {
			throw new IllegalStateException("수강할 수 있는 강의가 없습니다. 공개 상태와 사용여부를 확인하세요.");
		}
		enrich(lecture);
		return lecture;
	}

	@Override
	@Transactional
	public void insertLecture(EgovMap egovMap, MultipartFile video) throws Exception {
		validateLecture(egovMap);
		String lectureId = "LC" + CommUtil.getFileId();
		egovMap.put("lectureId", lectureId);
		egovMap.put("status", DRAFT);

		String storedPath = null;
		if (video != null && !video.isEmpty()) {
			EncodedVideo encoded = videoEncodingService.store(video, lectureId, toInt(egovMap.get("durationSec")));
			storedPath = encoded.relativePath();
			egovMap.put("videoPath", encoded.relativePath());
			egovMap.put("videoOriginalNm", encoded.originalName());
			egovMap.put("durationSec", encoded.durationSec());
		} else {
			egovMap.put("videoPath", null);
			egovMap.put("videoOriginalNm", null);
			egovMap.put("durationSec", Math.max(0, toInt(egovMap.get("durationSec"))));
		}
		try {
			lectureMapper.insertLecture(egovMap);
		} catch (Exception e) {
			videoEncodingService.delete(storedPath);
			throw e;
		}
	}

	@Override
	@Transactional
	public void updateLecture(EgovMap egovMap, MultipartFile video) throws Exception {
		String lectureId = trim(egovMap.get("lectureId"));
		EgovMap existing = requireDraft(lectureId, "수정");
		validateLecture(egovMap);
		egovMap.put("lectureId", lectureId);

		String videoPath = emptyToNull(trim(existing.get("videoPath")));
		String videoOriginalNm = emptyToNull(trim(existing.get("videoOriginalNm")));
		int durationSec = toInt(existing.get("durationSec"));
		int requestedDuration = toInt(egovMap.get("durationSec"));
		if (requestedDuration > 0) {
			durationSec = requestedDuration;
		}

		EncodedVideo replacement = null;
		if (video != null && !video.isEmpty()) {
			replacement = videoEncodingService.store(video, lectureId, requestedDuration);
			videoPath = replacement.relativePath();
			videoOriginalNm = replacement.originalName();
			durationSec = replacement.durationSec();
		}
		egovMap.put("videoPath", videoPath);
		egovMap.put("videoOriginalNm", videoOriginalNm);
		egovMap.put("durationSec", durationSec);

		try {
			if (lectureMapper.updateLecture(egovMap) == 0) {
				throw new IllegalStateException("작성중인 강의만 수정할 수 있습니다. 새로고침 후 다시 시도하세요.");
			}
		} catch (Exception e) {
			if (replacement != null && !replacement.relativePath().equals(trim(existing.get("videoPath")))) {
				videoEncodingService.delete(replacement.relativePath());
			}
			throw e;
		}
		if (replacement != null && !replacement.relativePath().equals(trim(existing.get("videoPath")))) {
			videoEncodingService.delete(trim(existing.get("videoPath")));
		}
	}

	@Override
	@Transactional
	public void deleteLecture(String lectureId) throws Exception {
		EgovMap existing = requireDraft(lectureId, "삭제");
		if (lectureMapper.deleteLecture(lectureId.trim()) == 0) {
			throw new IllegalStateException("작성중인 강의만 삭제할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
		videoEncodingService.delete(trim(existing.get("videoPath")));
	}

	@Override
	@Transactional
	public void openLecture(String lectureId) throws Exception {
		EgovMap lecture = selectLectureDetail(lectureId);
		if (lecture == null) {
			throw new IllegalArgumentException("강의가 없습니다.");
		}
		if (!DRAFT.equals(trim(lecture.get("status")))) {
			throw new IllegalStateException("작성중인 강의만 공개할 수 있습니다.");
		}
		if (isBlank(trim(lecture.get("videoPath"))) || toInt(lecture.get("durationSec")) <= 0) {
			throw new IllegalStateException("동영상과 재생 길이를 등록한 뒤 공개하세요.");
		}
		String surveyId = trim(lecture.get("surveyId"));
		if (!surveyId.isEmpty() && surveyService.selectSurveyDetail(surveyId) == null) {
			throw new IllegalStateException("연결된 설문이 없습니다. 설문을 다시 선택하세요.");
		}
		if (lectureMapper.openLecture(lectureId.trim()) == 0) {
			throw new IllegalStateException("작성중인 강의만 공개할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void closeLecture(String lectureId) throws Exception {
		EgovMap lecture = selectLectureDetail(lectureId);
		if (lecture == null) {
			throw new IllegalArgumentException("강의가 없습니다.");
		}
		if (!OPEN.equals(trim(lecture.get("status")))) {
			throw new IllegalStateException("공개중인 강의만 마감할 수 있습니다.");
		}
		if (lectureMapper.closeLecture(lectureId.trim()) == 0) {
			throw new IllegalStateException("공개중인 강의만 마감할 수 있습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public Map<String, Object> saveProgress(String lectureId, String userId, int watchedSec) throws Exception {
		EgovMap lecture = selectLectureForView(lectureId, userId);
		int durationSec = toInt(lecture.get("durationSec"));
		if (durationSec <= 0) {
			throw new IllegalStateException("강의 재생 길이가 설정되지 않았습니다.");
		}
		String trimmedLectureId = trim(lecture.get("lectureId"));
		int rate = normalizeRate(lecture.get("completeRate"));

		EgovMap key = new EgovMap();
		key.put("lectureId", trimmedLectureId);
		key.put("userId", userId);
		EgovMap existing = lectureMapper.selectProgress(key);
		int previousWatched = existing == null ? 0 : toInt(existing.get("watchedSec"));
		int maxWatched = Math.max(previousWatched, Math.max(0, Math.min(watchedSec, durationSec)));

		int threshold = (int) Math.ceil(durationSec * (rate / 100.0));
		boolean videoCompleted = maxWatched >= threshold;
		String surveyId = trim(lecture.get("linkedSurveyId"));
		boolean surveyRequired = !surveyId.isEmpty();
		boolean surveyCompleted = surveyRequired && surveyService.hasParticipated(surveyId, userId);
		boolean completed = videoCompleted && (!surveyRequired || surveyCompleted);

		EgovMap progress = new EgovMap();
		progress.put("lectureId", trimmedLectureId);
		progress.put("userId", userId);
		progress.put("watchedSec", maxWatched);
		progress.put("videoCompletedYn", videoCompleted ? "Y" : "N");
		progress.put("surveyCompletedYn", surveyCompleted ? "Y" : "N");
		progress.put("completedYn", completed ? "Y" : "N");
		if (existing == null) {
			progress.put("completedDt", completed ? new Timestamp(System.currentTimeMillis()) : null);
			lectureMapper.insertProgress(progress);
		} else if (completed) {
			lectureMapper.updateProgressCompleted(progress);
		} else {
			lectureMapper.updateProgress(progress);
		}

		Map<String, Object> status = new LinkedHashMap<>();
		status.put("watchedSec", maxWatched);
		status.put("durationSec", durationSec);
		status.put("percent", percent(maxWatched, durationSec));
		status.put("videoCompletedYn", videoCompleted ? "Y" : "N");
		status.put("surveyRequiredYn", surveyRequired ? "Y" : "N");
		status.put("surveyCompletedYn", surveyCompleted ? "Y" : "N");
		status.put("completedYn", completed ? "Y" : "N");
		return status;
	}

	@Override
	public List<EgovMap> selectCompletionList(EgovMap egovMap) throws Exception {
		List<EgovMap> rows = lectureMapper.selectCompletionList(egovMap);
		for (EgovMap row : rows) {
			row.put("percent", percent(toInt(row.get("watchedSec")), toInt(row.get("durationSec"))));
		}
		return rows;
	}

	@Override
	public int selectCompletionListTotCnt(EgovMap egovMap) throws Exception {
		return lectureMapper.selectCompletionListTotCnt(egovMap);
	}

	private void enrich(EgovMap lecture) {
		int durationSec = toInt(lecture.get("durationSec"));
		int watchedSec = toInt(lecture.get("watchedSec"));
		int rate = normalizeRate(lecture.get("completeRate"));
		boolean videoCompleted = durationSec > 0 && (long) watchedSec * 100 >= (long) durationSec * rate;
		boolean surveyRequired = !isBlank(trim(lecture.get("linkedSurveyId")));
		boolean surveyCompleted = surveyRequired && "Y".equals(trim(lecture.get("surveyParticipatedYn")));
		boolean completed = videoCompleted && (!surveyRequired || surveyCompleted);
		lecture.put("percent", percent(watchedSec, durationSec));
		lecture.put("surveyRequiredYn", surveyRequired ? "Y" : "N");
		lecture.put("surveyCompletedYn", surveyCompleted ? "Y" : "N");
		lecture.put("completedYn", completed ? "Y" : "N");
	}

	private double percent(int watchedSec, int durationSec) {
		if (durationSec <= 0) {
			return 0;
		}
		double value = Math.round(Math.min(watchedSec, durationSec) * 1000.0 / durationSec) / 10.0;
		return Math.min(value, 100.0);
	}

	private EgovMap requireDraft(String lectureId, String action) throws Exception {
		EgovMap lecture = selectLectureDetail(lectureId);
		if (lecture == null) {
			throw new IllegalArgumentException("강의가 없습니다.");
		}
		if (!DRAFT.equals(trim(lecture.get("status")))) {
			throw new IllegalStateException("작성중인 강의만 " + action + "할 수 있습니다.");
		}
		return lecture;
	}

	private void validateLecture(EgovMap egovMap) throws Exception {
		String title = trim(egovMap.get("title"));
		String description = trim(egovMap.get("description"));
		String surveyId = trim(egovMap.get("surveyId"));
		String useYn = trim(egovMap.get("useYn"));
		int durationSec = toInt(egovMap.get("durationSec"));
		int completeRate = egovMap.get("completeRate") == null ? DEFAULT_COMPLETE_RATE : toInt(egovMap.get("completeRate"));

		if (title.isEmpty() || title.length() > MAX_TITLE) {
			throw new IllegalArgumentException("강의 제목은 1~200자로 입력하세요.");
		}
		if (description.length() > MAX_DESCRIPTION) {
			throw new IllegalArgumentException("설명은 2000자 이내로 입력하세요.");
		}
		if (durationSec < 0) {
			throw new IllegalArgumentException("재생 길이는 0초 이상으로 입력하세요.");
		}
		if (completeRate < 1 || completeRate > 100) {
			throw new IllegalArgumentException("수료 기준은 1~100% 사이로 입력하세요.");
		}
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			throw new IllegalArgumentException("사용여부가 올바르지 않습니다.");
		}
		if (!surveyId.isEmpty() && surveyService.selectSurveyDetail(surveyId) == null) {
			throw new IllegalArgumentException("연결할 설문이 없습니다.");
		}
		egovMap.put("title", title);
		egovMap.put("description", description);
		egovMap.put("surveyId", surveyId);
		egovMap.put("useYn", useYn);
		egovMap.put("durationSec", durationSec);
		egovMap.put("completeRate", completeRate);
	}

	private int normalizeRate(Object value) {
		int rate = toInt(value);
		return rate >= 1 && rate <= 100 ? rate : DEFAULT_COMPLETE_RATE;
	}

	private int toInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return value == null ? 0 : Integer.parseInt(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
