package arkive.admin.comm.service.impl;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.session.SqlSessionException;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.FileController;
import arkive.admin.comm.web.FormBasedFileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service("fileService")
public class FileServiceImpl extends EgovAbstractServiceImpl implements FileService {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	protected final int BUFFER_SIZE = 8192;
	
	protected  String FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
	protected  String FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
	protected final String SEPERATOR 	= File.separator;
	private CommUtil cmmUtil = new CommUtil();

	@Resource(name = "fileMapper")
	private FileMapper fileMapper;

	/**
	 * 파일리스트 조회
	 */
	@Override
	public List<EgovMap> getFileList(EgovMap egovMap) {
		return fileMapper.getFileList(egovMap);
	}
	
	/**
	 * 파일을 real 폴더에 복사 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SqlSessionException
	 */
	public void uploadTempFiles(EgovMap pFileMap) throws IOException {
		
		String jobId = String.valueOf(pFileMap.get("jobId"));
		
		
		String advertNo = String.valueOf(pFileMap.get("advertNo"));
		String odr = String.valueOf(pFileMap.get("odr"));
		String pSubPath= "";
		
		if("MD".equals(jobId) || "ID".equals(jobId)) {
			// realPath/업무코드/관리번호/차수
			pSubPath = jobId+"/"+advertNo+"/"+odr;
		}
		String lsFileId = String.valueOf(pFileMap.get("fileId"));
		String NlsFileId = String.valueOf(pFileMap.get("nfileid"));
		//시큐어코딩을 위하여 파일명 검증
		FileInputStream fis 	= null;
		try {
			fis 	= new FileInputStream(CommUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR + pSubPath+  SEPERATOR + lsFileId));
			saveFile(fis, new File(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR + NlsFileId)));
		} finally {
			if(fis != null){
				fis.close();
			}

			//close(fis);
		}
	}
	
	/**
	 * 파일을 real 폴더에 복사 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SftpException 
	 * @throws JSchException 
	 * @throws Exception
	 */
	public String uploadRealCopyFiles(String fileData, String filePath) throws FileNotFoundException, IOException {
		String fileGroupId = "";
		
		List<Map<String, Object>> fileList = (ArrayList<Map<String, Object>>)cmmUtil.fromJsonStr(fileData);
		
		for(int i=0; i<fileList.size(); i++) {
			Map<String,Object> fileMap = (Map)fileList.get(i);
			
			fileGroupId = String.valueOf(fileMap.get("fileGroupId"));
			String lsFileId = String.valueOf(fileMap.get("fileId"));
			
			//시큐어코딩을 위하여 파일명 검증
			FileInputStream fis 	= null;		
			try {
				fis 	= new FileInputStream(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR + lsFileId));
				saveFile(fis, new File(CommUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR), FilenameUtils.getName(lsFileId) + "." + String.valueOf(fileMap.get("fileType"))));
				
			} catch(FileNotFoundException e){
				throw e;
			} catch(IOException e){
				throw e;
			} finally {
				if(fis != null)
					fis.close();
			}
		
			File loDelFile = new File(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR), FilenameUtils.getName(lsFileId) + "." + String.valueOf(fileMap.get("fileType")));
			
			if(loDelFile.exists()) {
				 loDelFile.delete();
			}
			
			EgovMap paramMap = new EgovMap();
			
			paramMap.put("fileGroupId", String.valueOf(fileMap.get("fileGroupId")));
			paramMap.put("fileId", String.valueOf(fileMap.get("fileId")));
			paramMap.put("fileNm", String.valueOf(fileMap.get("fileNm")));
			paramMap.put("fileKnd", String.valueOf(fileMap.get("fileType")));
			paramMap.put("fileMg", String.valueOf(fileMap.get("fileSize")));
			paramMap.put("fileOrdr", String.valueOf(fileMap.get("ordr")));
			paramMap.put("userId", String.valueOf(fileMap.get("userId")));
			paramMap.put("fileCours", FILE_REAL_PATH);
				
			if( "Y".equals(fileMap.get("othbcAt"))) {
				paramMap.put("othbcAt", String.valueOf(fileMap.get("othbcAt")));
			}
			
			fileMapper.insertAtchFile(paramMap);
		}
		
		return fileGroupId;
	}
	
	/**
	 * 파일을 real 폴더에 복사 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SftpException 
	 * @throws JSchException 
	 * @throws Exception
	 */
	public String uploadRealCopyFiles2(String fileData, String orgGroupId) throws FileNotFoundException, IOException {
		String fileGroupId = "";
		
		List<Map<String, Object>> fileList = (ArrayList<Map<String, Object>>)cmmUtil.fromJsonStr(fileData);
		
		for(int i=0; i<fileList.size(); i++) {
			Map<String,Object> fileMap = (Map)fileList.get(i);
			
			if(orgGroupId != null && !orgGroupId.equals("")) {
				fileGroupId = orgGroupId;
			}else {
				fileGroupId = String.valueOf(fileMap.get("fileGroupId"));
			}
			String lsFileId = String.valueOf(fileMap.get("fileId"));
			
			//시큐어코딩을 위하여 파일명 검증
			FileInputStream fis 	= null;		
			try {
				fis 	= new FileInputStream(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR + lsFileId));
				saveFile(fis, new File(CommUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR), FilenameUtils.getName(lsFileId) + "." + String.valueOf(fileMap.get("fileType"))));
				
			} catch(FileNotFoundException e){
				throw e;
			} catch(IOException e){
				throw e;
			} finally {
				if(fis != null)
					fis.close();
			}
		
			File loDelFile = new File(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR), FilenameUtils.getName(lsFileId) + "." + String.valueOf(fileMap.get("fileType")));
			
			if(loDelFile.exists()) {
				 loDelFile.delete();
			}
			
			EgovMap paramMap = new EgovMap();
			
			paramMap.put("fileGroupId", fileGroupId);
			paramMap.put("fileId", String.valueOf(fileMap.get("fileId")));
			paramMap.put("fileNm", String.valueOf(fileMap.get("fileNm")));
			paramMap.put("fileKnd", String.valueOf(fileMap.get("fileType")));
			paramMap.put("fileSize", String.valueOf(fileMap.get("fileSize")));
			paramMap.put("ordr", String.valueOf(fileMap.get("ordr")));
			paramMap.put("userId", String.valueOf(fileMap.get("userId")));
			paramMap.put("fileCours", FILE_REAL_PATH);
			
			fileMapper.insertAtchFile(paramMap);
		}
		
		return fileGroupId;
	}
	
	/**
	 * Stream으로부터 파일을 저장함.
	 * @param is InputStream
	 * @param file File
	 * @throws IOException
	 */
	public long saveFile(InputStream is, File file) throws IOException {
		//KISA 보안약점 조치 (2018-10-29, 윤창원)
		if (file.getParentFile() == null) {
			logger.debug("file.getParentFile() is null");
			throw new RuntimeException("file.getParentFile() is null");
		}
		
		// 디렉토리 생성
		if (!file.getParentFile().exists()) {
			if(file.getParentFile().mkdirs()){
				logger.debug("[file.mkdirs] file : Directory Creation Success");
			}else{				
				logger.error("[file.mkdirs] file : Directory Creation Fail");
			}
		}

		OutputStream os = null;
		long size = 0L;

		try {
			os = new FileOutputStream(file);

			int bytesRead = 0;
			byte[] buffer = new byte[BUFFER_SIZE];

			while ((bytesRead = is.read(buffer, 0, BUFFER_SIZE)) != -1) {
				size += bytesRead;
				os.write(buffer, 0, bytesRead);
			}
		} finally {
			FormBasedFileUtil.close(os);
		}

		return size;
	}
	
	public static void close(Closeable  ... resources) {
		for (Closeable resource : resources) {
			if (resource != null) {
				try {
					resource.close();
				} catch (IOException ignore) {//KISA 보안약점 조치 (2018-10-29, 윤창원)

				} catch (Exception ignore) {

				}
			}
		}
	}

	public EgovMap selectFileInfo(EgovMap param) {
		return fileMapper.selectFileInfo(param);
	}

	public EgovMap selectFileGroupInfo(EgovMap param) {
		return fileMapper.selectFileGroupInfo(param);
	}
	
	
	public EgovMap uploadFileInsert( MultipartFile file, String userId) throws DataAccessException, RuntimeException, Exception{
		String ACCEPTED_FILE_TYPE = ".jpeg,.jpg,.png,.gif,.JPEG,.JPG,.PNG,.GIF,.doc,.xls,.ppt,.docx,.xlsx,.pptx,.pdf,.mp4,.zip,.avi,.mkv,.wmv,.hwp,.txt";
		
		EgovMap loFileMap = new EgovMap();
				
		/** << 2020.10.06 웹취약점점검 조치관련 추가  **/
		String lsFileName = file.getOriginalFilename();
		if(lsFileName != null && !"".equals(lsFileName)) {
			String lsFileExtension = "." + FileController.getFileExtension(lsFileName);
			
			if(ACCEPTED_FILE_TYPE.indexOf(lsFileExtension) < 0) {
				throw new IOException("허용하지 않는 파일 확장자 예외발생");
			}
		}
		/** 웹취약점점검 조치관련 추가 >>  **/
		
		String lsFileId = cmmUtil.getFileId();
					
		String tmp = file.getOriginalFilename();
		String filePath = cmmUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR + lsFileId + "." + FileController.getFileExtension(lsFileName));		//GlobalsPath로 수정해야함.
		InputStream is = null;
		
		try {
			is = file.getInputStream();

			long size = saveFile(is, new File(filePath));
			if(size!=0) {
				loFileMap.put("groupId", lsFileId);
				loFileMap.put("fileId", lsFileId);
				loFileMap.put("fileNm", tmp);
				loFileMap.put("fileCours", FILE_REAL_PATH);
				loFileMap.put("fileType", FileController.getFileExtension(tmp));
				loFileMap.put("fileSize", file.getSize());
				loFileMap.put("ordr", 1);
				loFileMap.put("userId", userId);
				int result = 0; 
						//systemManageDAO.insertPbancFileInfo(loFileMap);
			}			
			if (is != null) {
				is.close();
			}
			
		}catch(IOException e) {
			
			throw e;
		}
		return loFileMap;
	}

	/**
	 * 파일을 ZIP폴더에 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @
	 */
	public List<EgovMap>selectFileZipInfo(EgovMap egovMap) {
		List<EgovMap> result = fileMapper.selectFileZipInfo(egovMap);
		
		return result;
	}

	/**
	 * 저장된 파일을 삭제 처리한다.
	 * @param EgovMap
	 * @return
	 * @throws Exception
	 */
	public void deleteFile(EgovMap pFileMap, String pSubPath) throws DataAccessException, IOException {
		String lsFileId = (String)pFileMap.get("atchFileId");
		//시큐어코딩을 위하여 파일명 검증
		String path = FILE_REAL_PATH + SEPERATOR + pSubPath + SEPERATOR;
		File loDelFile = new File(CommUtil.filePathBlackList(path), FilenameUtils.getName(lsFileId));
		
		if(loDelFile.exists()) {
			//loDelFile.delete();
			this.fileDelete(loDelFile);
		}
		
		fileMapper.deleteAtchFile(pFileMap);
	}
	
	/**
	 * 저장된 파일을 삭제 처리한다.
	 * @param EgovMap
	 * @return
	 * @throws Exception
	 */
	public int getDeleteFileId(String fileId) throws DataAccessException {
		//시큐어코딩을 위하여 파일명 검증
		String path = FILE_REAL_PATH + SEPERATOR;
		File loDelFile = new File(CommUtil.filePathBlackList(path), fileId);
		
		//추후 수정
		if(loDelFile.exists()) {
			this.fileDelete(loDelFile);
		}
		
		int result = fileMapper.getDeleteFileId(fileId);
		return result;
	}
	
	public int getDeleteFileGroupId(String fileId) throws DataAccessException {
		//시큐어코딩을 위하여 파일명 검증
		String path = FILE_REAL_PATH + SEPERATOR;
		File loDelFile = new File(CommUtil.filePathBlackList(path), fileId);
		
		//추후 수정
		if(loDelFile.exists()) {
			this.fileDelete(loDelFile);
		}
		
		int result = fileMapper.getDeleteFileGroupId(fileId);
		return result;
	}

	/**
	 *파일 사이즈를 가졍온다
	 * @param EgovMap
	 * @return
	 * @throws Exception
	 */
	public EgovMap getFileWidthHeight(String fileId, String pSubPath) throws IOException {
		EgovMap rtnMap = new EgovMap();
		try {
			String path = FILE_REAL_PATH + SEPERATOR + pSubPath + SEPERATOR;
			File file = new File(CommUtil.filePathBlackList(path), FilenameUtils.getName(fileId));
		    BufferedImage bi = ImageIO.read( file );
		    
		    rtnMap.put("imgWidth", bi.getWidth());
		    rtnMap.put("imgHeight", bi.getHeight());
		    
		} catch(IOException e){
			logger.debug("getFileWidthHeight :: IOException");
		}
		return rtnMap;
	}
	
	/**
	 * java secure coding(TOCTOU Issue)
	 * @param File
	 * @return boolean
	 * @throws 
	 */
	private synchronized boolean fileDelete(File file) {
		return file.delete();
	}
	
	@Override
	public String getOriginalFileName(String fileId) throws DataAccessException {
		return fileMapper.getOriginalFileName(fileId);
	}

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

		String tempPath = FILE_TEMP_PATH;
		if (!StringUtils.hasText(tempPath) || "99".equals(tempPath.trim())) {
		        tempPath = System.getProperty("java.io.tmpdir") + File.separator + "arkive" + File.separator + "temp";
		}
		File saveDir = new File(tempPath);
		try {
		        if (!saveDir.exists()) {
		                saveDir.mkdirs();
		        }
		        if (!saveDir.isDirectory() || !saveDir.canWrite()) {
		                throw new IOException("Not writable");
		        }
		} catch (Exception e) {
		        tempPath = System.getProperty("java.io.tmpdir") + File.separator + "arkive" + File.separator + "temp";
		        saveDir = new File(tempPath);
		        saveDir.mkdirs();
		}

		try {
			for (MultipartFile file : files) {
				if (file == null || file.isEmpty()) {
					continue;
				}

				String orgnlFileNm = FilenameUtils.getName(file.getOriginalFilename());
				String ext = FilenameUtils.getExtension(orgnlFileNm);
				String fileId = CommUtil.getFileId();
				String physFileNm = fileId; // temp 폴더에 타임스탬프 형식 파일명으로 저장

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

				try {
					fileMapper.setInsertAtchFile(paramMap);
				} catch (RuntimeException e) {
					if (destFile.exists() && !destFile.delete()) {
						logger.warn("Could not remove file after metadata insert failed: {}", destFile.getAbsolutePath());
					}
					throw e;
				}
				uploadedList.add(paramMap);
			}
		} catch (IOException | RuntimeException e) {
			for (EgovMap uploaded : uploadedList) {
				File savedFile = new File(String.valueOf(uploaded.get("filePath")));
				if (savedFile.exists() && !savedFile.delete()) {
					logger.warn("Could not roll back uploaded file: {}", savedFile.getAbsolutePath());
				}
			}
			throw e;
		}

		return uploadedList;
	}

	@Override
	public List<EgovMap> selectAtchFileListByType(String type) {
	        return fileMapper.selectAtchFileListByType(type);
	}

	@Override
	public void saveTusFileInfo(EgovMap paramMap) {
	        fileMapper.setInsertAtchFile(paramMap);
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
