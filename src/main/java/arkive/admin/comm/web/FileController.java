package arkive.admin.comm.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * 공통 파일 업로드·다운로드.
 * temp 선저장 후 확정(saveTempFiles)하는 2단계가 원칙이다.
 * 유형 접두사: MP_(멀티파트) TUS_ DROPZONE_ BOARD_(게시판).
 */
@Tag(name = "공통 파일", description = "temp 업로드·확정·다운로드·삭제, TUS 청크 업로드")
@Controller
public class FileController extends FormBasedFileUtil{

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static String FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
	public static String FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
	public static String MAX_FILES_NUM 	= EgovProperties.getProperty("Globals.MaxFilesNum");
	public static String MAX_FILE_SIZE_MB = EgovProperties.getProperty("Globals.fileUpload.maxFileSize.mb");
	public static String TUS_MAX_FILE_SIZE_MB = EgovProperties.getProperty("Globals.tus.maxFileSize.mb");
	public static String UPLOAD_EXTENSIONS 	= EgovProperties.getProperty("Globals.fileUpload.Extensions");
	public static String DOWNLOAD_EXTENSIONS 	= EgovProperties.getProperty("Globals.fileDownload.Extensions");
	
	private static final String TUS_LENGTH_FILE_SUFFIX = ".len"; //업로드 총 길이를 저장할 TUS 길이 파일 접미사
	
	@Resource(name = "fileService")
	private FileService fileService;

	private CommUtil cmmUtil = new CommUtil();

	/**
	 * 용량 프로퍼티(MB)를 읽는다. 없거나 숫자가 아니면 기본값을 쓴다.
	 */
	private static long parseMb(String value, long defaultMb) {
		try {
			return Long.parseLong(StringUtils.trimWhitespace(value));
		} catch (Exception e) {
			return defaultMb;
		}
	}

	/**
	 * TUS fileId 검증용 패턴. CommUtil.getFileId()는 yyyyMMddHHmmss + 숫자 6자리(20자리)를 반환한다.
	 * 경로 조작(../ 등)으로 임의 파일에 접근하지 못하도록 제한한다.
	 * @param fileId
	 * @return : 유효하면 true
	 */
	private static boolean isValidTusFileId(String fileId) {
		return fileId != null && fileId.matches("\\d{20}");
	}

	/**
	 * TUS 길이 파일에서 업로드 총 길이를 읽는다. 없으면 기본값을 반환한다.
	 * @param tempPath
	 * @param fileId
	 * @param defaultValue
	 * @return : 업로드 총 길이
	 */
	private long readTusUploadLength(String tempPath, String fileId, long defaultValue) {
		File lengthFile = new File(tempPath, fileId + TUS_LENGTH_FILE_SUFFIX);
		if (!lengthFile.isFile()) {
			return defaultValue;
		}
		try {
			String value = new String(Files.readAllBytes(lengthFile.toPath()), StandardCharsets.UTF_8).trim();
			if (!value.isEmpty()) {
				return Long.parseLong(value);
			}
		} catch (Exception e) {
			logger.warn("Failed to read TUS upload length. fileId={}", fileId, e);
		}
		return defaultValue;
	}

	/**
	 * TUS 길이 파일을 삭제한다.
	 * @param tempPath
	 * @param fileId
	 */
	private void deleteTusUploadLength(String tempPath, String fileId) {
		File lengthFile = new File(tempPath, fileId + TUS_LENGTH_FILE_SUFFIX);
		if (lengthFile.exists() && !lengthFile.delete()) {
			logger.warn("Could not remove TUS length file: {}", lengthFile.getAbsolutePath());
		}
	}
	
	/**
	 * temp 폴더에 파일 업로드 (staging 전용. DB 등록·최종 이동 없음).
	 * 멀티파트 메인(선택 즉시)·드롭존 팝업(확인 버튼)이 사용한다.
	 * 확정은 /system/file/saveTempFiles.json이 담당한다.
	 * @param	HttpServletRequest
	 * @param	ModelMap
	 * @return : jsonView
	*/
	@Operation(summary = "temp 선저장 (DB 등록·확정 없음, saveTempFiles에서 확정)")
	@RequestMapping(value = {"/system/file/tempFileUpload.json", "/file/tempFileUpload.json", "file/tempFileUpload.json"}, method = RequestMethod.POST)
	public String setTempFileUpload(HttpServletRequest request, ModelMap model)  {
	        try {
	                // "files" 파라미터 우선, 없으면 전송된 전체 멀티파트 파일을 모은다.
	                List<MultipartFile> fileList = new ArrayList<>();
	                if (request instanceof MultipartHttpServletRequest) {
	                        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
	                        List<MultipartFile> files = multipartRequest.getFiles("files");
	                        if (files != null && !files.isEmpty()) {
	                                fileList.addAll(files);
	                        } else if (multipartRequest.getMultiFileMap() != null) {
	                                for (List<MultipartFile> mfList : multipartRequest.getMultiFileMap().values()) {
	                                        if (mfList != null) {
	                                                fileList.addAll(mfList);
	                                        }
	                                }
	                        }
	                }
	                // 받을 파일이 없으면 실패 응답으로 끝낸다.
	                if (fileList.isEmpty()) {
	                        model.put("tempFileList", Collections.emptyList());
	                        model.put("success", false);
	                        model.put("message", "업로드할 파일이 없습니다.");
	                        model.put("root", fileService.getWritableRealPath());
	                return "jsonView";
	                }
	                // 그룹 ID 정규화. 목록 조회(SQL)가 MP_ 접두사로 멀티파트를 구분한다.
	                String groupId = request.getParameter("atchFileGrpid");
	                if (!StringUtils.hasText(groupId) || "undefined".equalsIgnoreCase(groupId) || "null".equalsIgnoreCase(groupId)) {
	                        groupId = "MP_" + UUID.randomUUID().toString();
	                } else if (!groupId.startsWith("MP_")) {
	                        groupId = "MP_" + groupId;
	                }
	                // temp 폴더에만 저장한다. DB 등록·최종 이동은 저장 API에서 한다.
	                List<?> uploadedFiles = fileService.setUploadFiles(fileList, groupId, "SYSTEM");
	                model.put("tempFileList", uploadedFiles);
	                model.put("success", true);
	        } catch(Exception e) {
	                // 실패하면 빈 목록 + 에러 메시지로 응답한다.
	                logger.error("setTempFileUpload failed", e);
	                model.put("tempFileList", Collections.emptyList());
	                model.put("success", false);
	                model.put("message", "파일 저장에 실패했습니다: " + e.getMessage());
	        }
	        model.put("root", fileService.getWritableRealPath());

	        return "jsonView";
	}
	
	/**
	 * 그룹 파일 zip 다운로드
	 * downloadFileId를 그룹 ID로 보고 해당 그룹 전체를 zip으로 묶어 첨부 다운로드한다.
	 */
	@Operation(summary = "그룹 ZIP 다운로드 (downloadFileId=그룹ID)")
	@RequestMapping(value="file/downloadFile2.do", method = RequestMethod.GET)
	public void downloadFile2(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		
		try {
			// downloadFileId를 그룹 ID로 본다. 비었으면 404
			String groupId = cmmUtil.convertHtml(request, "downloadFileId");
			if (!StringUtils.hasText(groupId)) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("파일이 없습니다.");
				return;
			}

			// 그룹에 속한 저장 파일 목록 조회. 비었으면 404
			List<EgovMap> fileList = fileService.getFileInfoList(groupId);
			if (fileList == null || fileList.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("파일이 없습니다.");
				return;
			}

			File uploadRoot = new File(fileService.getWritableRealPath()).getCanonicalFile();
			// zip 파일명은 title 파라미터가 있으면 쓰고, 없으면 그룹 ID를 쓴다
			String zipTitle = cmmUtil.convertHtml(request, "title");
			if (!StringUtils.hasText(zipTitle)) {
				zipTitle = groupId;
			}
			// 헤더 인젝션 방어로 개행 제거
			String zipName = URLEncoder.encode(zipTitle.replaceAll("[\\r\\n]", ""), "UTF-8").replaceAll("\\+", "%20") + ".zip";
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");

			Set<String> usedNames = new HashSet<>();
			// 한 파일씩 zip에 담아 바로 전송한다. 빠진 파일은 건너뛴다.
			try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
				for (EgovMap fileInfo : fileList) {
			// DB 경로가 최종 경로 안에 있고 실제 파일일 때만 통과. 아니면 404
			File savedFile = new File(String.valueOf(fileInfo.get("filePath"))).getCanonicalFile();
					if (!savedFile.toPath().startsWith(uploadRoot.toPath()) || !savedFile.isFile()) {
						logger.warn("downloadFile2 skipped, file not found. fileId={}", fileInfo.get("fileId"));
						continue;
					}
					// zip 안에서 겹치지 않는 파일명을 만든다. 같은 이름이 있으면 뒤에 (2), (3)을 붙인다.
					String entryName = String.valueOf(fileInfo.get("orgnlFileNm"));
					if (!StringUtils.hasText(entryName)) {
						entryName = "file";
					}
					String baseName = entryName;
					int dup = 1;
					while (!usedNames.add(entryName)) {
						dup++;
						int dot = baseName.lastIndexOf('.');
						if (dot > 0) {
							entryName = baseName.substring(0, dot) + " (" + dup + ")" + baseName.substring(dot);
						} else {
							entryName = baseName + " (" + dup + ")";
						}
					}
					zos.putNextEntry(new ZipEntry(entryName));
					try (InputStream fis = new FileInputStream(savedFile)) {
						FileCopyUtils.copy(fis, zos);
					}
					zos.closeEntry();
				}
				zos.finish();
			}
		} catch (IOException e) {
			logger.error("downloadFile2 :: IOException");
		}
	}
	
	/**
	 * 파일 다운로드
	 * downloadFileId 그대로 단건 조회한다. "groupId_" 접두사가 있으면 걷어내고 조회한다.
	 * pdf·jpg·jpeg·png·gif는 브라우저에 인라인 표시하고, 그 외는 첨부 다운로드한다.
	 * 미리보기 없이 그룹 단위 다운로드만 필요하면 downloadFile2를 사용한다.
	 * @param map 
	 * @param
	 * @throws IOException
	 */
	@Operation(summary = "단건 다운로드 (이미지·PDF는 인라인 표시)")
	@RequestMapping(value="file/downloadFile.do", method = RequestMethod.GET)
	public void downloadFile(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		
		try {
			// "groupId_" 접두사가 있으면 걷어내고 파일 ID만 쓴다
			String fileId =  cmmUtil.convertHtml(request, "downloadFileId");
			if (fileId.startsWith("groupId_")) {
				String[] parts = fileId.split("_");
				if (parts.length < 2 || !StringUtils.hasText(parts[1])) {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					response.getWriter().write("파일이 없습니다.");
					return;
				}
				fileId = parts[1];
			}

			// DB에 행이 없으면 404
			EgovMap fileInfo = fileService.getFileInfo(fileId);
			if (fileInfo == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("파일이 없습니다.");
				return;
			}

			File savedFile = new File(String.valueOf(fileInfo.get("filePath"))).getCanonicalFile();
			File uploadRoot = new File(fileService.getWritableRealPath()).getCanonicalFile();
			if (!savedFile.toPath().startsWith(uploadRoot.toPath()) || !savedFile.isFile()) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("파일이 없습니다.");
				return;
			}

			String fileExt = String.valueOf(fileInfo.get("fileExt"));
			String physFileNm = String.valueOf(fileInfo.get("physFileNm"));
			// 미리보기 대상(Globals.fileDownload.Extensions)인지 여기서 직접 판단한다
			boolean inlineView = false;
			if (StringUtils.hasText(DOWNLOAD_EXTENSIONS) && StringUtils.hasText(fileExt)) {
				for (String allowed : DOWNLOAD_EXTENSIONS.toLowerCase(java.util.Locale.ROOT).split("\\.")) {
					if (!allowed.trim().isEmpty() && fileExt.equalsIgnoreCase(allowed.trim())) {
						inlineView = true;
						break;
					}
				}
			}
			if (inlineView) {
				// 확장자별 브라우저 표시 타입 지정
				if (fileExt.equals("pdf")) {
					response.setContentType(MediaType.APPLICATION_PDF_VALUE);
				} else if (fileExt.equals("jpg") || fileExt.equals("jpeg")) {
					response.setContentType(MediaType.IMAGE_JPEG_VALUE);
				} else if (fileExt.equals("png")) {
					response.setContentType(MediaType.IMAGE_PNG_VALUE);
				} else if (fileExt.equals("gif")) {
					response.setContentType(MediaType.IMAGE_GIF_VALUE);
				}
				// 첨부가 아니라 브라우저 표시(inline)로 응답한다 미리보기형태
				response.setHeader("Content-Disposition", "inline; filename=\"" + physFileNm + "\"");
				response.setContentLengthLong(savedFile.length());
				// 파일 내용을 스트리밍한다. 메모리에 안 올린다.
				try (InputStream fis = new FileInputStream(savedFile)) {
					FileCopyUtils.copy(fis, response.getOutputStream());
				}
				response.getOutputStream().flush();
			} else {
				// 첨부 다운로드. 호출자가 여기 하나뿐이라 인라인한다.
				response.setContentType("application/octet-stream; charset=utf-8");
				response.setContentLengthLong(savedFile.length());
				// 파일명은 URL 인코딩한다. 한글·공백 깨짐 방지.
				String downloadName = URLEncoder.encode(
						String.valueOf(fileInfo.get("orgnlFileNm")), "UTF-8").replaceAll("\\+", "%20");
				response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
				response.setHeader("Content-Transfer-Encoding", "binary");
				try (InputStream fis = new FileInputStream(savedFile)) {
					FileCopyUtils.copy(fis, response.getOutputStream());
				} catch (IOException e) {
					logger.debug("file copy fail");
				}
				response.getOutputStream().flush();
			}
		} catch (IOException e) {
			logger.error("downloadFile :: IOException");
		}
	}
	
	/**
	 * 최종 저장 파일 삭제. result 1이면 삭제됨, 0이면 대상 없음·ID 오류다.
	 * @return result
	 */
	@Operation(summary = "확정 파일 삭제 (실물+DB행)")
	@RequestMapping(value = "/file/setFileDelete.json", method = RequestMethod.POST)
	public String setFileDelete(HttpServletRequest request, ModelMap model) throws DataAccessException, FileNotFoundException, IOException {
		
		String fileId = cmmUtil.convertHtml(request, "fileId");
		
		// ID 형식이 맞고 실제 삭제까지 되면 1이다
		int result = 0;
		if (isValidTusFileId(fileId) && fileService.setDeleteFile(fileId)) {
			result = 1;
		}
		
		model.put("result", result);
		
		return "jsonView";
	}

	
	@Operation(summary = "멀티파트 업로드 화면")
	@RequestMapping(name = "멀파트 업로드", value = "/system/multipartUpload", method = RequestMethod.GET)
	public String multipartUpload(HttpServletRequest request, ModelMap model) throws Exception {
	        List<EgovMap> fileList = fileService.selectAtchFileListByType("MULTIPART");
	        model.put("fileList", fileList);
	        model.put("maxFileSizeMb", parseMb(MAX_FILE_SIZE_MB, 50L));
	        return "comm/multipartUpload";
	}

	
	@Operation(summary = "TUS 업로드 화면")
	@RequestMapping(name = "TUS 파일업로드", value = "/system/tusUpload", method = RequestMethod.GET)
	public String tusUpload(HttpServletRequest request, ModelMap model) throws Exception {
	        List<EgovMap> fileList = fileService.selectAtchFileListByType("TUS");
	        model.put("fileList", fileList);
	        return "comm/tusUpload";
	}

	@Operation(summary = "TUS 팝업 (업로더 UI)")
	@RequestMapping(name = "TUS 팝업", value = "/system/tusPopup", method = RequestMethod.GET)      
	public String tusPopup(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("maxFileSizeMb", parseMb(TUS_MAX_FILE_SIZE_MB, 50L));
	        return "comm/tusPopup";
	}


	
	/**
	 * TUS CORS 프리플라이트 응답. 실제 업로드는 하지 않는다.
	 * 다른 출처에서 TUS 업로드를 시도할 때 브라우저가 본 요청 전에 보내므로 유지한다.
	 * 같은 출처(우리 팝업)는 이 메서드를 타지 않는다.
	 *
	 * [프리플라이트란]
	 * 브라우저는 남의 출처(프로토콜·도메인·포트 중 하나라도 다르면 다른 출처)에
	 * PATCH 같은 메서드나 Upload-Length 같은 비표준 헤더로 요청할 때,
	 * 본 요청을 보내기 전에 OPTIONS로 먼저 허락을 받는다. 이 허락 요청이 프리플라이트다.
	 * 예: 포털(http://localhost:8081)에서 우리 TUS(http://localhost:8080)로 올리면
	 *  1) 브라우저 → 서버 : OPTIONS /system/tus ("POST 써도 돼? Upload-Length 붙여도 돼?")
	 *  2) 서버 → 브라우저 : 204 + 아래 허용 목록 (이 메서드의 응답)
	 *  3) 브라우저 → 서버 : POST /system/tus (실제 생성 요청)
	 * 2)가 없으면 3)은 절대 안 나가고 브라우저 콘솔에 CORS 에러만 뜬다.
	 * 서버 로그에는 아무것도 안 찍히므로(요청 자체가 안 옴) 알아두어야 한다.
	 */
	@Operation(summary = "TUS CORS 프리플라이트 (OPTIONS)")
	@RequestMapping(value = "/system/tus/**", method = RequestMethod.OPTIONS)
	public ResponseEntity<?> tusOptions(HttpServletRequest request, HttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		// TUS 프로토콜 버전과 지원 확장(생성·삭제)을 알린다
		headers.add("Tus-Resumable", "1.0.0");
		headers.add("Tus-Version", "1.0.0");
		headers.add("Tus-Extension", "creation,termination");
		// 1회 업로드 최대 용량은 프로퍼티(Globals.tus.maxFileSize.mb) 기준이다
		headers.add("Tus-Max-Size", String.valueOf(parseMb(TUS_MAX_FILE_SIZE_MB, 50L) * 1024L * 1024L));
		// 크로스 도메인 허용 메서드·헤더. *는 모든 출처 허용이라 외부 공개 시 도메인으로 좁혀야 한다
		headers.add("Access-Control-Allow-Origin", "*");
		headers.add("Access-Control-Allow-Methods", "POST, HEAD, PATCH, DELETE, OPTIONS");
		headers.add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Upload-Length, Upload-Offset, Tus-Resumable, Upload-Metadata");
		return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
	}

	/**
	 * TUS 업로드 생성. 파일을 받는 게 아니라 접수증을 끊어준다.
	 * 20자리 접수번호 발급 + 빈 temp 파일 생성 + TUS 길이 파일에 총 길이 기록 후,
	 * 201과 함께 이후 청크 전송 주소(Location)를 돌려준다.
	 */
	@Operation(summary = "TUS 업로드 생성 (접수번호 발급, Location 반환)")
	@RequestMapping(value = "/system/tus", method = RequestMethod.POST)
	public ResponseEntity<?> tusPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			// 접수번호 발급 + 클라이언트가 알린 총 길이 읽기
			String fileId = CommUtil.getFileId();
			String uploadLengthStr = request.getHeader("Upload-Length");
			long uploadLength = uploadLengthStr != null ? Long.parseLong(uploadLengthStr) : 0;

			String tempPath = fileService.getWritableTempPath();
			File dir = new File(tempPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File file = new File(dir, fileId);
			if (!file.exists()) {
				file.createNewFile();
			}

			Files.write(new File(tempPath, fileId + TUS_LENGTH_FILE_SUFFIX).toPath(),
					String.valueOf(uploadLength).getBytes(StandardCharsets.UTF_8));

			// 업로드 URL과 시작 위치(0)를 알린다
			HttpHeaders headers = new HttpHeaders();
			headers.add("Tus-Resumable", "1.0.0");
			headers.add("Location", "/system/tus/" + fileId);
			headers.add("Upload-Offset", "0");
			headers.add("Access-Control-Allow-Origin", "*");
			return new ResponseEntity<>(headers, HttpStatus.CREATED);
		} catch (Exception e) {
			logger.error("tusPost error", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * TUS 이어받기 상태 조회. "얼마나 받았어?"에 답한다.
	 * 받은 바이트(Upload-Offset)와 총 길이(Upload-Length)를 돌려주면
	 * 클라이언트는 그 위치부터 이어서 보낸다. 모르는 URL은 404.
	 */
	@Operation(summary = "TUS 이어받기 상태 조회 (Upload-Offset 반환)")
	@RequestMapping(value = "/system/tus/{fileId}", method = RequestMethod.HEAD)
	public ResponseEntity<?> tusHead(@PathVariable("fileId") String fileId, HttpServletRequest request) {
		// 접수번호 형식이 아니면 404
		if (!isValidTusFileId(fileId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		try {
			String tempPath = fileService.getWritableTempPath();
			File file = new File(tempPath, fileId);
			File lengthFile = new File(tempPath, fileId + TUS_LENGTH_FILE_SUFFIX);
			if (!file.exists() && !lengthFile.exists()) {
				// 모르는 업로드 URL은 404로 돌려준다. 200 + 0/0으로 응답하면
				// 클라이언트가 없는 업로드를 이어받으려다 실제 전송 없이 성공으로 볼 수 있다.
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			// 받은 바이트는 temp 파일 크기, 총 길이는 TUS 길이 파일에서 구한다
			long offset = file.exists() ? file.length() : 0;
			long length = readTusUploadLength(tempPath, fileId, offset);

			// 받은 데까지와 총 길이를 알린다
			HttpHeaders headers = new HttpHeaders();
			headers.add("Tus-Resumable", "1.0.0");
			headers.add("Upload-Offset", String.valueOf(offset));
			headers.add("Upload-Length", String.valueOf(length));
			headers.add("Access-Control-Allow-Origin", "*");
			return new ResponseEntity<>(headers, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("tusHead error", e);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * TUS 청크 수신. 받은 본문을 temp 파일 끝에 이어붙인다.
	 * 위치가 어긋나면 409로 서버 위치를 알리고, 다 받으면 TUS 길이 파일을 지운다.
	 */
	@Operation(summary = "TUS 청크 수신 (이어붙이기, 409=위치 어긋남)")
	@RequestMapping(value = "/system/tus/{fileId}", method = RequestMethod.PATCH)
	public ResponseEntity<?> tusPatch(@PathVariable("fileId") String fileId, HttpServletRequest request) {
		// 접수번호 형식이 아니면 404
		if (!isValidTusFileId(fileId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		try {
			String uploadOffsetStr = request.getHeader("Upload-Offset");
			long requestOffset = uploadOffsetStr != null ? Long.parseLong(uploadOffsetStr) : 0;

			String tempPath = fileService.getWritableTempPath();
			File file = new File(tempPath, fileId);
			long currentOffset = file.exists() ? file.length() : 0;

			// 클라이언트가 말한 위치와 서버 실제 크기가 다르면 409로 서버 위치를 알린다
			if (requestOffset != currentOffset) {
				HttpHeaders headers = new HttpHeaders();
				headers.add("Tus-Resumable", "1.0.0");
				headers.add("Upload-Offset", String.valueOf(currentOffset));
				return new ResponseEntity<>(headers, HttpStatus.CONFLICT);
			}

			// 현재 끝 위치부터 요청 본문을 이어붙인다
			try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
				 InputStream in = request.getInputStream()) {
				raf.seek(currentOffset);
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					raf.write(buffer, 0, bytesRead);
				}
			}

			long newOffset = file.length();
			long expectedLength = readTusUploadLength(tempPath, fileId, newOffset);
			// 다 받았으면 TUS 길이 파일은 볼일 끝났으므로 지운다
			if (newOffset >= expectedLength) {
				deleteTusUploadLength(tempPath, fileId);
			}

			// 새로 받은 데까지를 알린다
			HttpHeaders headers = new HttpHeaders();
			headers.add("Tus-Resumable", "1.0.0");
			headers.add("Upload-Offset", String.valueOf(newOffset));
			headers.add("Access-Control-Allow-Origin", "*");
			return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
		} catch (Exception e) {
			logger.error("tusPatch error", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * TUS temp 삭제. 본 파일과 TUS 길이 파일을 함께 지운다.
	 * temp 공용 삭제구라 멀티파트·드롭존 취소도 이걸 쓴다.
	 */
	@Operation(summary = "TUS temp 삭제 (멀티파트·드롭존 취소도 공용)")
	@RequestMapping(value = "/system/tus/{fileId}", method = RequestMethod.DELETE)
	public ResponseEntity<?> tusDelete(@PathVariable("fileId") String fileId, HttpServletRequest request) {
		// 접수번호 형식이 아니면 404
		if (!isValidTusFileId(fileId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		try {
			String tempPath = fileService.getWritableTempPath();
			// temp 본 파일을 지운다. 실패해도 로그만 남긴다
			File file = new File(tempPath, fileId);
			if (file.exists() && !file.delete()) {
				logger.warn("Could not remove TUS temp file: {}", file.getAbsolutePath());
			}
			// TUS 길이 파일도 함께 지운다
			deleteTusUploadLength(tempPath, fileId);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Tus-Resumable", "1.0.0");
			headers.add("Access-Control-Allow-Origin", "*");
			return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
		} catch (Exception e) {
 			logger.error("tusDelete error", e);
 			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
 		}
 	}

	/**
	 * temp 폴더에 저장된 파일을 Arkive 최종 경로로 이동하고 DB에 등록한다.
	 * 멀티파트, TUS, 드롭존 업로드가 공통으로 사용하는 저장 API.
	 * @param map files(JSON), atchFileGrpid, fileType(MULTIPART | TUS | DROPZONE)
	 */
	@Operation(summary = "temp 확정 저장 (최종 이동+DB 등록, fileType으로 그룹 접두사 결정)")
	@RequestMapping(value = {"/system/file/saveTempFiles.json"}, method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveTempFilesApi(@RequestParam Map<String, Object> map) {
		Object fileType = map.get("fileType");
		return saveStagedFiles(map, fileType != null ? String.valueOf(fileType) : null);
	}

	/**
	 * 업로드 유형별 ATCH_FILE_GRPID 접두사를 반환한다.
	 * 파일 목록 조회(SQL)가 접두사로 유형을 구분하므로 반드시 유지해야 한다.
	 */
	private String resolveFileGroupPrefix(String fileType) {
		if (!StringUtils.hasText(fileType)) {
			return "MP_";
		}
		if ("TUS".equalsIgnoreCase(fileType)) {
			return "TUS_";
		}
		if ("DROPZONE".equalsIgnoreCase(fileType)) {
			return "DROPZONE_";
		}
		if ("BOARD".equalsIgnoreCase(fileType) || "BOARD_EDITOR".equalsIgnoreCase(fileType)) {
			return "BOARD_";
		}
		return "MP_";
	}

	/**
	 * 공용 확정 저장 본체. temp 파일을 최종 경로로 옮기고 DB에 등록한다.
	 * 멀티파트·TUS·드롭존 저장이 마지막에 다 여기로 온다.
	 */
	private Map<String, Object> saveStagedFiles(Map<String, Object> map, String fileType) {
		Map<String, Object> result = new HashMap<>();
		try {
			String filesJson = (String) map.get("files");
			String atchFileGrpid = (String) map.get("atchFileGrpid");

			// 저장할 목록이 없으면 실패로 끝낸다
			if (!StringUtils.hasText(filesJson)) {
				result.put("success", false);
				result.put("message", "저장할 파일이 없습니다.");
				return result;
			}

			// JSON 문자열을 파일 목록으로 바꾼다
			ObjectMapper objectMapper = new ObjectMapper();
			List<Map<String, String>> files = objectMapper.readValue(filesJson,
					new TypeReference<List<Map<String, String>>>() {
					});

			// 그룹 ID 확정. 없으면 유형 접두사+UUID를 만든다
			String prefix = resolveFileGroupPrefix(fileType);
			if (!StringUtils.hasText(atchFileGrpid)
					|| "undefined".equalsIgnoreCase(atchFileGrpid)
					|| "null".equalsIgnoreCase(atchFileGrpid)) {
				atchFileGrpid = prefix + UUID.randomUUID().toString();
			} else if (!atchFileGrpid.startsWith(prefix)) {
				atchFileGrpid = prefix + atchFileGrpid;
			}

			// temp→최종 이동 + DB 등록은 서비스가 한다
			List<EgovMap> savedFiles = fileService.saveTempFiles(files, atchFileGrpid, "SYSTEM");

			// 저장이 끝났으므로 TUS 길이 파일 정리
			String tempPath = fileService.getWritableTempPath();
			for (Map<String, String> file : files) {
				String fileId = file.get("fileId");
				if (StringUtils.hasText(fileId) && isValidTusFileId(fileId)) {
					deleteTusUploadLength(tempPath, fileId);
				}
			}

			// 저장 개수·목록·그룹 ID를 돌려준다
			result.put("success", true);
			result.put("message", "저장되었습니다.");
			result.put("count", savedFiles.size());
			result.put("files", savedFiles);
			result.put("atchFileGrpid", atchFileGrpid);
		} catch (Exception e) {
			// 실패하면 메시지만 담아 돌려준다
			logger.error("saveStagedFiles failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
	
	@Operation(summary = "드롭존 업로드 화면")
	@RequestMapping(name = "드롭존 업로드", value = "/system/dropzoneUpload", method = RequestMethod.GET)
	public String dropzoneUpload(HttpServletRequest request, ModelMap model) throws Exception {
	        List<EgovMap> fileList = fileService.selectAtchFileListByType("DROPZONE");
	        model.put("fileList", fileList);
	        model.put("maxFilesNum", MAX_FILES_NUM);
	        return "comm/dropzoneUpload";
	}

	@Operation(summary = "드롭존 팝업 (업로더 UI)")
	@RequestMapping(name = "드롭존 팝업", value = "/system/dropzonePopup", method = RequestMethod.GET)
	public String dropzonePopup(HttpServletRequest request, ModelMap model) throws Exception {
	        String maxFilesNum = cmmUtil.convertHtml(request, "maxFilesNum");
	        if (!StringUtils.hasText(maxFilesNum)) {
	                maxFilesNum = MAX_FILES_NUM;
	        }
	        model.put("maxFilesNum", maxFilesNum);
	        // 프로퍼티 화이트리스트를 Dropzone acceptedFiles 형식(.gif,.jpg)으로 바꾼다
	        StringBuilder acceptedFiles = new StringBuilder();
	        if (StringUtils.hasText(UPLOAD_EXTENSIONS)) {
	                for (String ext : UPLOAD_EXTENSIONS.split("\\.")) {
	                        if (StringUtils.hasText(ext)) {
	                                if (acceptedFiles.length() > 0) {
	                                        acceptedFiles.append(',');
	                                }
	                                acceptedFiles.append('.').append(ext.trim().toLowerCase(java.util.Locale.ROOT));
	                        }
	                }
	        }
        model.put("uploadExtensions", acceptedFiles.toString());
        String popupType = cmmUtil.convertHtml(request, "type");
        // 용량 상한은 프로퍼티(Globals.fileUpload.maxFileSize.mb) 기준이다
        long maxFileSizeMb = parseMb(MAX_FILE_SIZE_MB, 50L);
        model.put("maxFileSizeMb", maxFileSizeMb);
        model.put("type", popupType);
	        model.put("existFileId", cmmUtil.convertHtml(request, "existFileId"));
	        return "comm/dropzonePopup";
	}
	
}
