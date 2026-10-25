package arkive.admin.lecture.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.lecture.service.impl.LectureMapper;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 강의 진도·수료 판정, 공개 상태 전이, 동영상 스트리밍 접근 제어를 확인한다.
 * FFmpeg 인코딩은 테스트에서 꺼져 있으므로 mp4 업로드는 원본 그대로 저장된다.
 */
@ArkiveIntegrationTest
@org.springframework.transaction.annotation.Transactional
class LectureControllerTest {

	private static final String SURVEY_ID = "LC-SURVEY-1";
	private static final String OPEN_LECTURE_ID = "LC-OPEN-1";
	private static final String CLOSED_LECTURE_ID = "LC-CLOSED-1";
	private static final String UNUSED_LECTURE_ID = "LC-UNUSED-1";
	private static final byte[] VIDEO_BYTES = { 1, 2, 3, 4 };

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LectureMapper lectureMapper;

	@Autowired
	private AuthMapper authMapper;

	@Autowired
	private PermissionMapper permissionMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void progressShouldTrackMaxPositionAndRequireSurveyForCompletion() throws Exception {
		MockHttpSession user = userSession("lectureuser1");
		jdbcTemplate.update("INSERT INTO SURVEY (SURVEY_ID, TITLE, START_DE, END_DE, STATUS, ANONYMOUS_YN)"
				+ " VALUES (?, ?, ?, ?, ?, ?)",
				SURVEY_ID, "강의 연계 설문", "2020-01-01", "2099-12-31", "OPEN", "N");
		lectureMapper.insertLecture(lecture(OPEN_LECTURE_ID, "공개 강의", SURVEY_ID, "OPEN", "Y"));
		lectureMapper.insertLecture(lecture(CLOSED_LECTURE_ID, "마감 강의", null, "CLOSED", "Y"));
		lectureMapper.insertLecture(lecture(UNUSED_LECTURE_ID, "미사용 강의", null, "OPEN", "N"));

		MvcResult myList = mockMvc.perform(get("/lecture/myLecture").session(user))
				.andExpect(status().isOk())
				.andReturn();
		List<EgovMap> available = listModel(myList, "resultList");
		EgovMap availableRow = find(available, "lectureId", OPEN_LECTURE_ID);
		assertThat(availableRow).isNotNull();
		assertThat(number(availableRow.get("percent"))).isEqualTo(0.0);
		assertThat(availableRow.get("surveyRequiredYn")).isEqualTo("Y");
		assertThat(find(available, "lectureId", CLOSED_LECTURE_ID)).isNull();
		assertThat(find(available, "lectureId", UNUSED_LECTURE_ID)).isNull();

		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", OPEN_LECTURE_ID)
				.param("watchedSec", "50"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.watchedSec").value(50))
				.andExpect(jsonPath("$.durationSec").value(100))
				.andExpect(jsonPath("$.percent").value(50.0))
				.andExpect(jsonPath("$.videoCompletedYn").value("N"))
				.andExpect(jsonPath("$.surveyRequiredYn").value("Y"))
				.andExpect(jsonPath("$.surveyCompletedYn").value("N"))
				.andExpect(jsonPath("$.completedYn").value("N"));

		// 이전보다 작은 위치는 최대 시청 위치를 줄이지 않는다.
		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", OPEN_LECTURE_ID)
				.param("watchedSec", "30"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.watchedSec").value(50))
				.andExpect(jsonPath("$.percent").value(50.0));

		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", OPEN_LECTURE_ID)
				.param("watchedSec", "95"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.watchedSec").value(95))
				.andExpect(jsonPath("$.percent").value(95.0))
				.andExpect(jsonPath("$.videoCompletedYn").value("Y"))
				.andExpect(jsonPath("$.completedYn").value("N"));

		// 재생 길이를 넘겨도 저장 위치는 길이로 제한된다.
		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", OPEN_LECTURE_ID)
				.param("watchedSec", "500"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.watchedSec").value(100))
				.andExpect(jsonPath("$.percent").value(100.0))
				.andExpect(jsonPath("$.videoCompletedYn").value("Y"))
				.andExpect(jsonPath("$.completedYn").value("N"));

		jdbcTemplate.update("INSERT INTO SURVEY_PARTICIPATION (PARTICIPATION_ID, SURVEY_ID, USER_ID, SUBMIT_DT)"
				+ " VALUES (?, ?, ?, CURRENT_TIMESTAMP)", "PA-LC-1", SURVEY_ID, "lectureuser1");

		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", OPEN_LECTURE_ID)
				.param("watchedSec", "95"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.watchedSec").value(100))
				.andExpect(jsonPath("$.surveyCompletedYn").value("Y"))
				.andExpect(jsonPath("$.completedYn").value("Y"));

		Map<String, Object> progressRow = jdbcTemplate.queryForMap(
				"SELECT COMPLETED_YN, COMPLETED_DT FROM LECTURE_PROGRESS WHERE LECTURE_ID = ? AND USER_ID = ?",
				OPEN_LECTURE_ID, "lectureuser1");
		assertThat(progressRow.get("COMPLETED_YN")).isEqualTo("Y");
		assertThat(progressRow.get("COMPLETED_DT")).isNotNull();

		MvcResult view = mockMvc.perform(get("/lecture/view").session(user)
				.param("lectureId", OPEN_LECTURE_ID))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap lecture = (EgovMap) view.getModelAndView().getModel().get("lecture");
		assertThat(lecture.get("completedYn")).isEqualTo("Y");
		assertThat(number(lecture.get("percent"))).isEqualTo(100.0);

		MvcResult completion = mockMvc.perform(get("/lecture/completionList")
				.session(TestSessions.admin("admin"))
				.param("lectureId", OPEN_LECTURE_ID))
				.andExpect(status().isOk())
				.andReturn();
		EgovMap completionRow = find(listModel(completion, "resultList"), "userId", "lectureuser1");
		assertThat(completionRow).isNotNull();
		assertThat(completionRow.get("completedYn")).isEqualTo("Y");
		assertThat(number(completionRow.get("percent"))).isEqualTo(100.0);

		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", CLOSED_LECTURE_ID)
				.param("watchedSec", "10"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("수강할 수 있는 강의가 없습니다. 공개 상태와 사용여부를 확인하세요."));
		mockMvc.perform(post("/lecture/progress.json").session(user)
				.param("lectureId", "LC-NONE")
				.param("watchedSec", "10"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("수강할 수 있는 강의가 없습니다. 공개 상태와 사용여부를 확인하세요."));
	}

	@Test
	void insertAndStateMachineShouldValidateAndControlStreaming() throws Exception {
		MockHttpSession admin = TestSessions.admin("admin");
		MockHttpSession user = userSession("lectureuser2");

		MvcResult write = mockMvc.perform(get("/lecture/lectureWrite").session(admin))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("doubleSubmitToken", "surveyList", "encodingEnabled"))
				.andReturn();
		assertThat(write.getModelAndView().getModel().get("encodingEnabled")).isEqualTo(false);

		mockMvc.perform(post("/lecture/insertLecture.json").session(admin)
				.param("title", "강의")
				.param("durationSec", "60")
				.param("completeRate", "90")
				.param("useYn", "Y"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("중복 제출이 감지되었습니다. 화면을 새로고침한 뒤 다시 시도하세요."));

		expectInsertFailure(admin, "", "", "60", "90", "Y", "", "강의 제목은 1~200자로 입력하세요.");
		expectInsertFailure(admin, "a".repeat(201), "", "60", "90", "Y", "",
				"강의 제목은 1~200자로 입력하세요.");
		expectInsertFailure(admin, "강의", "a".repeat(2001), "60", "90", "Y", "",
				"설명은 2000자 이내로 입력하세요.");
		expectInsertFailure(admin, "강의", "", "-1", "90", "Y", "", "재생 길이는 0초 이상으로 입력하세요.");
		expectInsertFailure(admin, "강의", "", "60", "0", "Y", "", "수료 기준은 1~100% 사이로 입력하세요.");
		expectInsertFailure(admin, "강의", "", "60", "101", "Y", "", "수료 기준은 1~100% 사이로 입력하세요.");
		expectInsertFailure(admin, "강의", "", "60", "90", "X", "", "사용여부가 올바르지 않습니다.");
		expectInsertFailure(admin, "강의", "", "60", "90", "Y", "NO-SURVEY", "연결할 설문이 없습니다.");

		String lectureId = insert(admin, "테스트 강의", "", "120", "90", "Y", "");
		EgovMap detail = lectureMapper.selectLectureDetail(lectureId);
		assertThat(detail.get("status")).isEqualTo("DRAFT");
		assertThat(detail.get("videoPath")).isNull();

		mockMvc.perform(post("/lecture/openLecture.json").session(admin).param("lectureId", lectureId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("동영상과 재생 길이를 등록한 뒤 공개하세요."));

		mockMvc.perform(multipart("/lecture/updateLecture.json")
				.file(new MockMultipartFile("video", "sample.mp4", "video/mp4", VIDEO_BYTES))
				.session(admin)
				.param("lectureId", lectureId)
				.param("title", "테스트 강의")
				.param("description", "설명")
				.param("completeRate", "90")
				.param("useYn", "Y")
				.param("surveyId", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("인코딩이 꺼져 있으면 재생 길이(초)를 직접 입력하세요."));

		mockMvc.perform(multipart("/lecture/updateLecture.json")
				.file(new MockMultipartFile("video", "sample.mp4", "video/mp4", VIDEO_BYTES))
				.session(admin)
				.param("lectureId", lectureId)
				.param("title", "테스트 강의")
				.param("description", "설명")
				.param("durationSec", "120")
				.param("completeRate", "90")
				.param("useYn", "Y")
				.param("surveyId", ""))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.id").value(lectureId));

		detail = lectureMapper.selectLectureDetail(lectureId);
		assertThat(String.valueOf(detail.get("videoPath"))).endsWith("/" + lectureId + ".mp4");
		assertThat(detail.get("videoOriginalNm")).isEqualTo("sample.mp4");
		assertThat(number(detail.get("durationSec"))).isEqualTo(120.0);

		mockMvc.perform(post("/lecture/openLecture.json").session(admin).param("lectureId", lectureId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(lectureMapper.selectLectureDetail(lectureId).get("status")).isEqualTo("OPEN");

		mockMvc.perform(post("/lecture/updateLecture.json").session(admin)
				.param("lectureId", lectureId)
				.param("title", "공개 후 수정")
				.param("durationSec", "120")
				.param("completeRate", "90")
				.param("useYn", "Y")
				.param("surveyId", ""))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("작성중인 강의만 수정할 수 있습니다."));
		mockMvc.perform(post("/lecture/deleteLecture.json").session(admin).param("lectureId", lectureId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("작성중인 강의만 삭제할 수 있습니다."));

		mockMvc.perform(get("/lecture/stream").session(user).param("lectureId", lectureId))
				.andExpect(status().isOk())
				.andExpect(header().string("Accept-Ranges", "bytes"))
				.andExpect(header().string("Content-Type", "video/mp4"))
				.andExpect(content().bytes(VIDEO_BYTES));
		mockMvc.perform(get("/lecture/stream").session(user)
				.param("lectureId", lectureId)
				.header("Range", "bytes=1-2"))
				.andExpect(status().isPartialContent())
				.andExpect(header().string("Content-Range", "bytes 1-2/4"))
				.andExpect(content().bytes(new byte[] { 2, 3 }));
		mockMvc.perform(get("/lecture/stream").session(user)
				.param("lectureId", lectureId)
				.header("Range", "bytes=9-"))
				.andExpect(status().isRequestedRangeNotSatisfiable())
				.andExpect(header().string("Content-Range", "bytes */4"));

		mockMvc.perform(post("/lecture/closeLecture.json").session(admin).param("lectureId", lectureId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(lectureMapper.selectLectureDetail(lectureId).get("status")).isEqualTo("CLOSED");
		mockMvc.perform(post("/lecture/closeLecture.json").session(admin).param("lectureId", lectureId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("공개중인 강의만 마감할 수 있습니다."));

		mockMvc.perform(get("/lecture/stream").session(user).param("lectureId", lectureId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/lecture/stream").session(admin).param("lectureId", lectureId))
				.andExpect(status().isOk());

		String draftId = insert(admin, "삭제 강의", "", "30", "90", "Y", "");
		mockMvc.perform(post("/lecture/deleteLecture.json").session(admin).param("lectureId", draftId))
				.andExpect(jsonPath("$.success").value(true));
		assertThat(lectureMapper.selectLectureDetail(draftId)).isNull();
	}

	private void expectInsertFailure(MockHttpSession session, String title, String description,
			String durationSec, String completeRate, String useYn, String surveyId, String message)
			throws Exception {
		mockMvc.perform(post("/lecture/insertLecture.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "LECTURE"))
				.param("title", title)
				.param("description", description)
				.param("durationSec", durationSec)
				.param("completeRate", completeRate)
				.param("useYn", useYn)
				.param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(message));
	}

	private String insert(MockHttpSession session, String title, String description,
			String durationSec, String completeRate, String useYn, String surveyId) throws Exception {
		MvcResult result = mockMvc.perform(post("/lecture/insertLecture.json").session(session)
				.param(EgovDoubleSubmitHelper.PARAMETER_NAME,
						EgovDoubleSubmitHelper.setToken(session, "LECTURE"))
				.param("title", title)
				.param("description", description)
				.param("durationSec", durationSec)
				.param("completeRate", completeRate)
				.param("useYn", useYn)
				.param("surveyId", surveyId))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private EgovMap lecture(String lectureId, String title, String surveyId, String status, String useYn) {
		EgovMap lecture = new EgovMap();
		lecture.put("lectureId", lectureId);
		lecture.put("title", title);
		lecture.put("description", "");
		lecture.put("videoPath", null);
		lecture.put("videoOriginalNm", null);
		lecture.put("durationSec", 100);
		lecture.put("surveyId", surveyId);
		lecture.put("completeRate", 90);
		lecture.put("status", status);
		lecture.put("useYn", useYn);
		return lecture;
	}

	private MockHttpSession userSession(String userId) throws Exception {
		EgovMap user = new EgovMap();
		user.put("userId", userId);
		user.put("password", "encoded-test-password");
		user.put("userNm", userId + "-name");
		user.put("email", userId + "@example.com");
		user.put("mailYn", "N");
		authMapper.insertUser(user);

		EgovMap roleParam = new EgovMap();
		roleParam.put("userId", userId);
		roleParam.put("roleId", "ROLE_USER");
		permissionMapper.insertUserRole(roleParam);
		return TestSessions.user(userId, userId + "-name");
	}

	@SuppressWarnings("unchecked")
	private List<EgovMap> listModel(MvcResult result, String attribute) {
		return (List<EgovMap>) result.getModelAndView().getModel().get(attribute);
	}

	private EgovMap find(List<EgovMap> rows, String key, String value) {
		if (rows == null) {
			return null;
		}
		for (EgovMap row : rows) {
			if (value.equals(String.valueOf(row.get(key)))) {
				return row;
			}
		}
		return null;
	}

	private double number(Object value) {
		return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
	}
}
