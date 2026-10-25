package arkive.admin.comm.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import arkive.admin.comm.service.FileService;
import arkive.support.ArkiveIntegrationTest;
import arkive.support.TestSessions;

/**
 * 공통 파일의 temp 업로드 → 확정 → 다운로드 → 삭제, TUS 이어받기, ZIP 묶음 다운로드,
 * 경로 조작 방어를 실제 컨트롤러·DB·파일시스템으로 확인한다.
 * 물리 파일은 테스트가 만든 것만 정리한다. DB 행은 트랜잭션 롤백으로 되돌린다.
 */
@ArkiveIntegrationTest
@Transactional
class FileControllerTest {

	@Autowired
	private MockMvc mockMvc;

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

	private Path tempPath(String fileId) {
		return Path.of(fileService.getWritableTempPath(), fileId);
	}

	private Path realPath(String filePath) {
		return Path.of(filePath);
	}

	private String uploadTemp(String fileName, String contentType, byte[] bytes) throws Exception {
		MvcResult result = mockMvc.perform(multipart("/system/file/tempFileUpload.json")
						.file(new MockMultipartFile("files", fileName, contentType, bytes))
						.session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		String fileId = JsonPath.read(result.getResponse().getContentAsString(), "$.tempFileList[0].fileId");
		filesToDelete.add(tempPath(fileId));
		return fileId;
	}

	private String saveTemp(String filesJson, String fileType) throws Exception {
		MvcResult result = mockMvc.perform(post("/system/file/saveTempFiles.json")
						.session(TestSessions.admin("admin"))
						.param("files", filesJson)
						.param("fileType", fileType))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.atchFileGrpid");
	}

	private String filesJson(String fileId, String fileName) {
		return "[{\"fileId\":\"" + fileId + "\",\"fileName\":\"" + fileName + "\"}]";
	}

	private void rememberRealFile(EgovMap fileInfo) {
		if (fileInfo != null) {
			filesToDelete.add(realPath(String.valueOf(fileInfo.get("filePath"))));
		}
	}

	@Test
	@Rollback
	void tempUploadShouldMoveOnSaveAndBeDownloadableAndDeletable() throws Exception {
		String fileId = uploadTemp("sample.txt", MediaType.TEXT_PLAIN_VALUE,
				"hello".getBytes(StandardCharsets.UTF_8));
		assertThat(Files.exists(tempPath(fileId))).isTrue();

		String groupId = saveTemp(filesJson(fileId, "sample.txt"), "MULTIPART");

		assertThat(groupId).startsWith("MP_");
		assertThat(Files.exists(tempPath(fileId))).isFalse();
		EgovMap fileInfo = fileService.getFileInfo(fileId);
		assertThat(fileInfo).isNotNull();
		rememberRealFile(fileInfo);
		assertThat(String.valueOf(fileInfo.get("atchFileGrpid"))).isEqualTo(groupId);
		assertThat(String.valueOf(fileInfo.get("fileExt"))).isEqualTo("txt");
		assertThat(String.valueOf(fileInfo.get("fileSz"))).isEqualTo("5");
		assertThat(Files.readString(realPath(String.valueOf(fileInfo.get("filePath"))),
				StandardCharsets.UTF_8)).isEqualTo("hello");

		mockMvc.perform(get("/file/downloadFile.do").param("downloadFileId", fileId))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));

		mockMvc.perform(post("/file/setFileDelete.json")
						.session(TestSessions.admin("admin"))
						.param("fileId", fileId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result").value(1));

		assertThat(fileService.getFileInfo(fileId)).isNull();
		assertThat(Files.exists(realPath(String.valueOf(fileInfo.get("filePath"))))).isFalse();

		mockMvc.perform(post("/file/setFileDelete.json")
						.session(TestSessions.admin("admin"))
						.param("fileId", fileId))
				.andExpect(jsonPath("$.result").value(0));
	}

	@Test
	@Rollback
	void imageShouldBeServedInlineWithItsContentType() throws Exception {
		byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3 };
		String fileId = uploadTemp("pic.png", MediaType.IMAGE_PNG_VALUE, png);

		String groupId = saveTemp(filesJson(fileId, "pic.png"), "BOARD");
		assertThat(groupId).startsWith("BOARD_");
		EgovMap fileInfo = fileService.getFileInfo(fileId);
		rememberRealFile(fileInfo);

		mockMvc.perform(get("/file/downloadFile.do").param("downloadFileId", fileId))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
				.andExpect(header().string("Content-Disposition", containsString("inline")))
				.andExpect(content().bytes(png));

		fileService.setDeleteFile(fileId);
	}

	@Test
	@Rollback
	void uploadShouldRejectDisallowedExtensionAndEmptyRequests() throws Exception {
		mockMvc.perform(multipart("/system/file/tempFileUpload.json")
						.file(new MockMultipartFile("files", "evil.exe", MediaType.APPLICATION_OCTET_STREAM_VALUE,
								"x".getBytes(StandardCharsets.UTF_8)))
						.session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message", containsString("허용하지 않는 파일 확장자")));

		mockMvc.perform(multipart("/system/file/tempFileUpload.json")
						.session(TestSessions.admin("admin")))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message", containsString("업로드할 파일이 없습니다")));

		mockMvc.perform(post("/system/file/saveTempFiles.json")
						.session(TestSessions.admin("admin"))
						.param("fileType", "MULTIPART"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message", containsString("저장할 파일이 없습니다")));
	}

	@Test
	@Rollback
	void tusChunksShouldResumeAndRejectWrongOffset() throws Exception {
		MvcResult created = mockMvc.perform(post("/system/tus")
						.header("Upload-Length", "5")
						.session(TestSessions.admin("admin")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Tus-Resumable", "1.0.0"))
				.andExpect(header().string("Upload-Offset", "0"))
				.andReturn();
		String location = created.getResponse().getHeader("Location");
		assertThat(location).matches("/system/tus/\\d{20}");
		String fileId = location.substring(location.lastIndexOf('/') + 1);
		filesToDelete.add(tempPath(fileId));
		filesToDelete.add(tempPath(fileId + ".len"));

		mockMvc.perform(head("/system/tus/" + fileId).session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(header().string("Upload-Offset", "0"))
				.andExpect(header().string("Upload-Length", "5"));

		mockMvc.perform(patch("/system/tus/" + fileId)
						.header("Upload-Offset", "2")
						.content("abc".getBytes(StandardCharsets.UTF_8))
						.session(TestSessions.admin("admin")))
				.andExpect(status().isConflict())
				.andExpect(header().string("Upload-Offset", "0"));

		mockMvc.perform(patch("/system/tus/" + fileId)
						.header("Upload-Offset", "0")
						.content("hello".getBytes(StandardCharsets.UTF_8))
						.session(TestSessions.admin("admin")))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Upload-Offset", "5"));

		assertThat(Files.readString(tempPath(fileId), StandardCharsets.UTF_8)).isEqualTo("hello");
		assertThat(Files.exists(tempPath(fileId + ".len"))).isFalse();

		mockMvc.perform(head("/system/tus/" + fileId).session(TestSessions.admin("admin")))
				.andExpect(status().isOk())
				.andExpect(header().string("Upload-Offset", "5"))
				.andExpect(header().string("Upload-Length", "5"));

		mockMvc.perform(options("/system/tus"))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Tus-Resumable", "1.0.0"))
				.andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));

		mockMvc.perform(head("/system/tus/not-a-valid-id").session(TestSessions.admin("admin")))
				.andExpect(status().isNotFound());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/system/tus/" + fileId)
						.session(TestSessions.admin("admin")))
				.andExpect(status().isNoContent());
		assertThat(Files.exists(tempPath(fileId))).isFalse();

		mockMvc.perform(head("/system/tus/" + fileId).session(TestSessions.admin("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	@Rollback
	void zipDownloadShouldIncludeGroupFilesAndDeduplicateNames() throws Exception {
		MvcResult uploaded = mockMvc.perform(multipart("/system/file/tempFileUpload.json")
						.file(new MockMultipartFile("files", "dup.txt", MediaType.TEXT_PLAIN_VALUE,
								"first".getBytes(StandardCharsets.UTF_8)))
						.file(new MockMultipartFile("files", "dup.txt", MediaType.TEXT_PLAIN_VALUE,
								"second".getBytes(StandardCharsets.UTF_8)))
						.session(TestSessions.admin("admin")))
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		List<String> fileIds = JsonPath.read(uploaded.getResponse().getContentAsString(),
				"$.tempFileList[*].fileId");
		assertThat(fileIds).hasSize(2);
		fileIds.forEach(fileId -> filesToDelete.add(tempPath(fileId)));

		String filesJson = "[{\"fileId\":\"" + fileIds.get(0) + "\",\"fileName\":\"dup.txt\"},"
				+ "{\"fileId\":\"" + fileIds.get(1) + "\",\"fileName\":\"dup.txt\"}]";
		String groupId = saveTemp(filesJson, "MULTIPART");
		assertThat(fileService.getFileInfoList(groupId)).hasSize(2);
		fileIds.forEach(fileId -> rememberRealFile(fileService.getFileInfo(fileId)));

		MvcResult zipped = mockMvc.perform(get("/file/downloadFile2.do").param("downloadFileId", groupId))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/zip"))
				.andReturn();

		Map<String, String> entries = unzip(zipped.getResponse().getContentAsByteArray());
		assertThat(entries).containsOnlyKeys("dup.txt", "dup (2).txt");
		assertThat(entries.values()).containsExactlyInAnyOrder("first", "second");

		mockMvc.perform(get("/file/downloadFile2.do").param("downloadFileId", "NO_SUCH_GROUP"))
				.andExpect(status().isNotFound());
	}

	@Test
	@Rollback
	void filesOutsideUploadRootShouldNotBeServedOrDeleted() throws Exception {
		String fileId = "99111111111111111111";
		File pom = new File("pom.xml").getCanonicalFile();
		assertThat(pom.isFile()).isTrue();
		jdbcTemplate.update("INSERT INTO ATCH_FILE (FILE_ID, ATCH_FILE_GRPID, ORGNL_FILE_NM, PHYS_FILE_NM,"
						+ " FILE_PATH, FILE_SZ, FILE_EXT, REG_DT, RGTR_ID)"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
				fileId, "BOARD_TRAVERSAL", "pom.xml", "pom.xml", pom.getAbsolutePath(),
				pom.length(), "xml", "tester");

		mockMvc.perform(get("/file/downloadFile.do").param("downloadFileId", fileId))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/file/setFileDelete.json")
						.session(TestSessions.admin("admin"))
						.param("fileId", fileId))
				.andExpect(jsonPath("$.result").value(1));

		assertThat(fileService.getFileInfo(fileId)).isNull();
		assertThat(pom.isFile()).isTrue();
	}

	@Test
	void tempUploadShouldRequireLogin() throws Exception {
		mockMvc.perform(multipart("/system/file/tempFileUpload.json")
						.file(new MockMultipartFile("files", "sample.txt", MediaType.TEXT_PLAIN_VALUE,
								"hello".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	private Map<String, String> unzip(byte[] bytes) throws Exception {
		Map<String, String> entries = new LinkedHashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return entries;
	}
}
