package arkive.admin.comm.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import jakarta.annotation.Resource;

@Service("fileService")
public class FileServiceImpl extends EgovAbstractServiceImpl implements FileService {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "fileMapper")
	private FileMapper fileMapper;

	// 프로퍼티 화이트리스트를 파싱한 업로드 허용 확장자 집합. 형식: ".gif.jpg.png" (점으로 이어붙임)
	private static final Set<String> UPLOAD_EXTENSIONS =
			parseExtensionSet(EgovProperties.getProperty("Globals.fileUpload.Extensions"));

	// 허용 확장자 집합을 만든다. 소문자로 통일하고 빈 토큰은 버린다.
	private static Set<String> parseExtensionSet(String value) {
		Set<String> extensions = new HashSet<>();
		if (value != null) {
			for (String ext : value.toLowerCase(Locale.ROOT).split("\\.")) {
				if (!ext.trim().isEmpty()) {
					extensions.add(ext.trim());
				}
			}
		}
		return extensions;
	}

	// 업로드 허용 확장자인지 확인한다. 집합이 비었으면 경고만 남기고 전체 허용한다.
	private boolean isAllowedUploadExtension(String fileName) {
		if (UPLOAD_EXTENSIONS.isEmpty()) {
			logger.warn("Upload extension whitelist is empty. Allowing all extensions.");
			return true;
		}
		return UPLOAD_EXTENSIONS.contains(FilenameUtils.getExtension(
				fileName != null ? fileName : "").toLowerCase(Locale.ROOT));
	}

	// 업로드 경로 프로퍼티를 읽고 디렉터리를 생성한 뒤 절대 경로를 반환한다.
	// 값이 없거나 "99"이면 java.io.tmpdir 하위로 대체한다.
	private static String resolveWritablePath(String key, String subDir) {
		String configured = EgovProperties.getProperty(key);
		if (configured == null || configured.trim().isEmpty() || "99".equals(configured.trim())) {
			configured = System.getProperty("java.io.tmpdir") + File.separator + "arkive"
					+ ((subDir != null && !subDir.trim().isEmpty()) ? File.separator + subDir : "");
		}

		File dir = new File(configured);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IllegalStateException("업로드 경로를 생성할 수 없습니다: " + dir.getAbsolutePath());
		}
		return dir.getAbsolutePath();
	}

	// 파일 임시 저장 경로(Globals.FILE_TEMP_PATH)
	@Override
	public String getWritableTempPath() {
		return resolveWritablePath("Globals.FILE_TEMP_PATH", "temp");
	}

	// 최종 저장 경로(Globals.FILE_REAL_PATH)
	@Override
	public String getWritableRealPath() {
		return resolveWritablePath("Globals.FILE_REAL_PATH", "");
	}

	@Override
	public List<EgovMap> setUploadFiles(List<MultipartFile> files, String atchFileGrpid, String userId) throws IOException {	
		List<EgovMap> uploadedList = new ArrayList<>();

		if (files == null || files.isEmpty()) {
			return uploadedList;
		}

		String groupNo = atchFileGrpid;
		if (!StringUtils.hasText(groupNo) || "undefined".equalsIgnoreCase(groupNo) || "null".equalsIgnoreCase(groupNo)) {	
			groupNo = UUID.randomUUID().toString();
		}

		String tempPath = getWritableTempPath();
		File saveDir = new File(tempPath);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}
		if (!saveDir.isDirectory() || !saveDir.canWrite()) {
			throw new IOException("Not writable temp path: " + tempPath);
		}

		try {
			for (MultipartFile file : files) {
				if (file == null || file.isEmpty()) {
					continue;
				}

				String orgnlFileNm = FilenameUtils.getName(file.getOriginalFilename());
				String ext = FilenameUtils.getExtension(orgnlFileNm);
				// 화이트리스트에 없는 확장자는 temp에도 올리지 않고 전체를 실패시킨다
				if (!isAllowedUploadExtension(orgnlFileNm)) {
					throw new IOException("허용하지 않는 파일 확장자입니다: " + orgnlFileNm);
				}
				String fileId = CommUtil.getFileId();

				// temp 폴더에만 저장한다. 최종 경로 이동과 DB 등록은 saveTempFiles에서 수행한다.
				File tempFile = new File(saveDir, fileId);
				file.transferTo(tempFile);

				EgovMap paramMap = new EgovMap();
				paramMap.put("fileId", fileId);
				paramMap.put("atchFileGrpid", groupNo);
				paramMap.put("orgnlFileNm", orgnlFileNm);
				paramMap.put("fileSz", tempFile.length());
				paramMap.put("fileExt", ext);
				paramMap.put("userId", StringUtils.hasText(userId) ? userId : "SYSTEM");

				uploadedList.add(paramMap);
			}
		} catch (IOException | RuntimeException e) {
			for (EgovMap uploaded : uploadedList) {
				File stagedFile = new File(saveDir, String.valueOf(uploaded.get("fileId")));
				if (stagedFile.exists() && !stagedFile.delete()) {
					logger.warn("Could not roll back staged file: {}", stagedFile.getAbsolutePath());
				}
			}
			throw e;
		}

		return uploadedList;
	}

	/**
	 * temp 폴더에 저장된 파일을 Arkive 최종 경로로 이동하고 DB에 등록한다.
	 * 멀티파트, TUS, 드롭존 업로드가 공통으로 사용한다.
	 */
	@Override
	public List<EgovMap> saveTempFiles(List<Map<String, String>> stagedFiles, String atchFileGrpid, String userId) throws IOException {
		List<EgovMap> savedList = new ArrayList<>();

		if (stagedFiles == null || stagedFiles.isEmpty()) {
			return savedList;
		}

		String tempPath = getWritableTempPath();
		String realPath = getWritableRealPath();

		String groupNo = atchFileGrpid;
		if (!StringUtils.hasText(groupNo) || "undefined".equalsIgnoreCase(groupNo) || "null".equalsIgnoreCase(groupNo)) {
			groupNo = UUID.randomUUID().toString();
		}

		String regUserId = StringUtils.hasText(userId) ? userId : "SYSTEM";
		List<String> movedFiles = new ArrayList<>();

		try {
			for (Map<String, String> staged : stagedFiles) {
				if (staged == null) {
					continue;
				}

				String fileId = staged.get("fileId");
				String fileName = staged.get("fileName");

				if (!StringUtils.hasText(fileId)) {
					continue;
				}

				File tempFile = new File(tempPath, fileId);
				if (!tempFile.exists()) {
					logger.warn("saveTempFiles skipped, temp file not found. fileId={}", fileId);
					continue;
				}

				String ext = FilenameUtils.getExtension(fileName != null ? fileName : "");
				// 화이트리스트에 없는 확장자는 확정하지 않고 건너뛴다
				if (!isAllowedUploadExtension(fileName)) {
					logger.warn("saveTempFiles skipped, extension not allowed. fileId={}", fileId);
					continue;
				}
				String physFileNm = StringUtils.hasText(ext) ? fileId + "." + ext : fileId;
				File destFile = new File(realPath, physFileNm);

				// temp -> arkive 이동 (같은 볼륨이므로 rename)
				Files.move(tempFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				movedFiles.add(destFile.getAbsolutePath());

				EgovMap paramMap = new EgovMap();
				paramMap.put("fileId", fileId);
				paramMap.put("atchFileGrpid", groupNo);
				paramMap.put("orgnlFileNm", StringUtils.hasText(fileName) ? fileName : fileId);
				paramMap.put("physFileNm", physFileNm);
				paramMap.put("filePath", destFile.getAbsolutePath());
				paramMap.put("fileSz", destFile.length());
				paramMap.put("fileExt", ext);
				paramMap.put("userId", regUserId);

				if (fileMapper.getFileInfo(fileId) == null) {
					fileMapper.setInsertAtchFile(paramMap);
				}
				savedList.add(paramMap);
			}
		} catch (IOException | RuntimeException e) {
			// DB 등록 실패 시 이미 이동한 파일을 temp로 되돌린다
			for (String moved : movedFiles) {
				File movedFile = new File(moved);
				if (movedFile.exists()) {
					File restoreFile = new File(tempPath, FilenameUtils.getBaseName(movedFile.getName()));
					Files.move(movedFile.toPath(), restoreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			throw e;
		}

		return savedList;
	}

	@Override
	public List<EgovMap> selectAtchFileListByType(String type) {
	        return fileMapper.selectAtchFileListByType(type);
	}

	@Override
	public EgovMap getFileInfo(String fileId) {
		if (!StringUtils.hasText(fileId)) {
			return null;
		}
		return fileMapper.getFileInfo(fileId);
	}

	@Override
	public List<EgovMap> getFileInfoList(String atchFileGrpid) {
		if (!StringUtils.hasText(atchFileGrpid)) {
			return new ArrayList<>();
		}
		return fileMapper.getFileInfoList(atchFileGrpid);
	}

	/**
	 * 최종 저장 파일을 삭제한다.
	 */
	@Override
	public boolean setDeleteFile(String fileId) {
		// fileId가 비었으면 삭제할 것이 없으므로 false
		if (!StringUtils.hasText(fileId)) {
			return false;
		}

		// DB에 행이 없으면 이미 없거나 잘못된 ID이므로 false
		EgovMap fileInfo = fileMapper.getFileInfo(fileId);
		if (fileInfo == null) {
			return false;
		}

		String filePathStr = (String) fileInfo.get("filePath");
		if (StringUtils.hasText(filePathStr)) {
			try {
				// 경로 조작 방어를 위해 정규 경로로 변환한다
				File physicalFile = new File(filePathStr).getCanonicalFile();
				File uploadRoot = new File(getWritableRealPath()).getCanonicalFile();
				// 최종 경로 안에 있을 때만 실제 파일을 지운다
				if (physicalFile.toPath().startsWith(uploadRoot.toPath()) && physicalFile.isFile()
						&& !physicalFile.delete()) {
					logger.warn("Could not remove saved file: {}", physicalFile.getAbsolutePath());
				}
			} catch (IOException e) {
				// 경로를 확정할 수 없으면 파일을 건드리지 않고 false
				logger.warn("Could not resolve saved file path: {}", filePathStr, e);
				return false;
			}
		}

		// DB 행을 DELETE한다
		fileMapper.setDeleteAtchFile(fileId);
		return true;
	}

	/**
	 * 파일 확장자를 추출한다. (FileController에 있던 공용 로직을 서비스로 이동)
	 *
	 * @param fileNamePath
	 * @return
	 */
	private static String getFileExtension(String fileNamePath) {
		String ext = fileNamePath.substring(fileNamePath.lastIndexOf(".") + 1, fileNamePath.length());
		return (ext == null) ? "" : ext;
	}

}
