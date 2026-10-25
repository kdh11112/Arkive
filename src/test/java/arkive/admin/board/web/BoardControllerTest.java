package arkive.admin.board.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.board.BoardFixtures;
import arkive.admin.board.service.BoardService;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.admin.comm.service.FileService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;

/**
 * 게시판 화면·저장 API를 실제 요청으로 확인한다.
 * 본문 정제, 이중등록, 조회수 1회 집계, 삭제 시 에디터 이미지·첨부 동반 삭제,
 * 에디터 이미지 업로드 후 인라인 다운로드까지 다룬다.
 */
@ArkiveIntegrationTest
@Transactional
class BoardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BoardService boardService;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private FileService fileService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<Path> filesToDelete = new ArrayList<>();

	@AfterEach
	void cleanUp() throws Exception {
		for (Path path : filesToDelete) {
			Files.deleteIfExists(path);
		}
		filesToDelete.clear();
	}

	private String insertBoard(MockHttpSession session, String token, String title, String content) throws Exception {
		MvcResult result = mockMvc.perform(post("/board/insertBoard.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("boardType", "CK")
						.param("title", title)
						.param("content", content)
						.param("importantYn", "N"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.boardId");
	}

	@Test
	@Rollback
	void insertBoardShouldSanitizeContentAndRejectDuplicateToken() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");
		String token = EgovDoubleSubmitHelper.setToken(session, "BOARD_CK");
		String content = "<p>본문</p><script>alert(1)</script>"
				+ "<img src=\"/x.png\" onerror=\"alert(2)\">"
				+ "<iframe src=\"https://www.youtube.com/embed/abc\"></iframe>"
				+ "<iframe src=\"https://evil.example/x\"></iframe>";

		String boardId = insertBoard(session, token, "게시판 정제 테스트", content);

		EgovMap detail = boardService.selectBoardDetail(boardId);
		assertThat(String.valueOf(detail.get("boardType"))).isEqualTo("CK");
		assertThat(String.valueOf(detail.get("title"))).isEqualTo("게시판 정제 테스트");
		String stored = String.valueOf(detail.get("content"));
		assertThat(stored).contains("<p>본문</p>", "youtube.com/embed/abc");
		assertThat(stored).doesNotContain("<script>", "onerror", "evil.example");

		mockMvc.perform(post("/board/insertBoard.json")
						.session(session)
						.param(EgovDoubleSubmitHelper.PARAMETER_NAME, token)
						.param("boardType", "CK")
						.param("title", "중복 등록")
						.param("content", "<p>dup</p>"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message", containsString("중복")));
	}

	@Test
	@Rollback
	void ckeditorWriteScreenTokenShouldBeAcceptedByInsert() throws Exception {
		MockHttpSession session = TestSessions.admin("admin");

		MvcResult screen = mockMvc.perform(get("/board/ckeditorWrite").session(session))
				.andExpect(status().isOk())
				.andReturn();
		Object token = screen.getModelAndView().getModel().get("doubleSubmitToken");
		assertThat(token).isNotNull();
		assertThat(screen.getModelAndView().getModel().get("isAdmin")).isEqualTo(true);

		String boardId = insertBoard(session, String.valueOf(token), "화면 토큰 테스트", "<p>ok</p>");
		assertThat(String.valueOf(boardService.selectBoardDetail(boardId).get("title")))
				.isEqualTo("화면 토큰 테스트");
	}

	@Test
	@Rollback
	void detailShouldIncreaseViewCountOnlyOncePerSession() throws Exception {
		boardMapper.insertBoard(BoardFixtures.board(
				"BDCTRLDETAIL000001", "CK", "조회수 테스트", "<p>body</p>"));

		MockHttpSession first = TestSessions.admin("viewer");
		mockMvc.perform(get("/board/detail").param("boardId", "BDCTRLDETAIL000001").session(first))
				.andExpect(status().isOk());
		mockMvc.perform(get("/board/detail").param("boardId", "BDCTRLDETAIL000001").session(first))
				.andExpect(status().isOk());
		assertThat(viewCount("BDCTRLDETAIL000001")).isEqualTo(1);

		mockMvc.perform(get("/board/detail").param("boardId", "BDCTRLDETAIL000001")
						.session(TestSessions.admin("viewer2")))
				.andExpect(status().isOk());
		assertThat(viewCount("BDCTRLDETAIL000001")).isEqualTo(2);
	}

	@Test
	@Rollback
	void deleteShouldRemoveBoardEditorImageAndAttachmentFiles() throws Exception {
		String boardId = "BDCTRLDELETE000001";
		String editorId = "98111111111111111111";
		String attachId = "98111111111111111112";
		Path realRoot = Path.of(fileService.getWritableRealPath());
		Path editorFile = realRoot.resolve("ctrl_editor_" + UUID.randomUUID() + ".png");
		Path attachFile = realRoot.resolve("ctrl_attach_" + UUID.randomUUID() + ".txt");
		Files.write(editorFile, new byte[] { 1, 2, 3 });
		Files.write(attachFile, "attach".getBytes(StandardCharsets.UTF_8));
		filesToDelete.add(editorFile);
		filesToDelete.add(attachFile);

		insertAtchFile(editorId, "BOARD_CTRLDEL", "editor.png", "png", editorFile);
		insertAtchFile(attachId, "BOARD_CTRLDEL", "attach.txt", "txt", attachFile);
		boardMapper.insertBoard(BoardFixtures.board(boardId, "CK", "삭제 테스트",
				"<p><img src=\"/file/downloadFile.do?downloadFileId=" + editorId + "\"></p>"));
		jdbcTemplate.update("UPDATE BOARD SET ATCH_FILE_GRPID = ? WHERE BOARD_ID = ?", "BOARD_CTRLDEL", boardId);

		mockMvc.perform(post("/board/deleteBoard.json")
						.session(TestSessions.admin("admin"))
						.param("boardId", boardId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(boardService.selectBoardDetail(boardId)).isNull();
		assertThat(fileService.getFileInfo(editorId)).isNull();
		assertThat(fileService.getFileInfo(attachId)).isNull();
		assertThat(Files.exists(editorFile)).isFalse();
		assertThat(Files.exists(attachFile)).isFalse();
	}

	@Test
	@Rollback
	void editorImageUploadShouldStoreImageAndServeInline() throws Exception {
		byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 9, 8, 7 };

		MvcResult uploaded = mockMvc.perform(multipart("/board/editorImageUpload.json")
						.file(new MockMultipartFile("upload", "editor.png", MediaType.IMAGE_PNG_VALUE, png))
						.session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploaded").value(true))
				.andReturn();
		String fileId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.fileId");
		String url = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.url");
		assertThat(fileId).matches("\\d{20}");
		assertThat(url).isEqualTo("/file/downloadFile.do?downloadFileId=" + fileId);

		EgovMap fileInfo = fileService.getFileInfo(fileId);
		assertThat(fileInfo).isNotNull();
		filesToDelete.add(Path.of(String.valueOf(fileInfo.get("filePath"))));
		assertThat(String.valueOf(fileInfo.get("atchFileGrpid"))).startsWith("BOARD_EDITOR_");

		mockMvc.perform(get("/file/downloadFile.do").param("downloadFileId", fileId))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
				.andExpect(content().bytes(png));

		fileService.setDeleteFile(fileId);
	}

	private void insertAtchFile(String fileId, String groupId, String name, String ext, Path path) {
		jdbcTemplate.update("INSERT INTO ATCH_FILE (FILE_ID, ATCH_FILE_GRPID, ORGNL_FILE_NM, PHYS_FILE_NM,"
						+ " FILE_PATH, FILE_SZ, FILE_EXT, REG_DT, RGTR_ID)"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
				fileId, groupId, name, path.getFileName().toString(), path.toString(),
				path.toFile().length(), ext, "tester");
	}

	private int viewCount(String boardId) {
		return jdbcTemplate.queryForObject("SELECT VIEW_CNT FROM BOARD WHERE BOARD_ID = ?", Integer.class, boardId);
	}
}
