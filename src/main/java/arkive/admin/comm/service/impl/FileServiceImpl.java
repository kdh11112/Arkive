package arkive.admin.comm.service.impl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("fileService")
public class FileServiceImpl extends EgovAbstractServiceImpl implements FileService {

	@Resource(name = "fileMapper")
	private FileMapper fileMapper;

	private String getBasePath() {
		String basePath = EgovProperties.getProperty("Globals.FILE_REAL_PATH");
		if (!StringUtils.hasText(basePath)) {
			basePath = "C:/upload/arkive/";
		}
		if (!basePath.endsWith("/") && !basePath.endsWith("\\")) {
			basePath += "/";
		}
		return basePath;
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

		String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
		File saveDir = new File(getBasePath(), dateDir);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}

		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}

			String orgnlFileNm = file.getOriginalFilename();
			String ext = FilenameUtils.getExtension(orgnlFileNm);
			String fileId = UUID.randomUUID().toString();
			String physFileNm = fileId + (StringUtils.hasText(ext) ? "." + ext : "");

			File destFile = new File(saveDir, physFileNm);
			file.transferTo(destFile);

			EgovMap paramMap = new EgovMap();
			paramMap.put("fileId", fileId);
			paramMap.put("atchFileGrpid", groupNo);
			paramMap.put("orgnlFileNm", orgnlFileNm);
			paramMap.put("physFileNm", physFileNm);
			paramMap.put("filePath", destFile.getAbsolutePath());
			paramMap.put("fileSz", file.getSize());
			paramMap.put("fileExt", ext);
			paramMap.put("userId", StringUtils.hasText(userId) ? userId : "SYSTEM");

			fileMapper.setInsertAtchFile(paramMap);
			uploadedList.add(paramMap);
		}

		return uploadedList;
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

	@Override
	public boolean setDeleteFile(String fileId) {
		if (!StringUtils.hasText(fileId)) {
			return false;
		}

		EgovMap fileInfo = fileMapper.getFileInfo(fileId);
		if (fileInfo == null) {
			return false;
		}

		String filePathStr = (String) fileInfo.get("filePath");
		if (StringUtils.hasText(filePathStr)) {
			File physicalFile = new File(filePathStr);
			if (physicalFile.exists()) {
				physicalFile.delete();
			}
		}

		fileMapper.setDeleteAtchFile(fileId);
		return true;
	}

}
