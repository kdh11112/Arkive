package arkive.admin.comm.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.ibatis.session.SqlSessionException;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.dao.DataAccessException;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

	/**
	 * 멀티파트 파일 업로드 처리 및 DB 정보 저장 (Insert)
	 * @param files 업로드 파일 목록
	 * @param atchFileGrpid 파일 그룹 ID
	 * @param userId 사용자 ID
	 * @return 업로드된 파일 정보 목록
	 * @throws IOException
	 */
	public List<EgovMap> setUploadFiles(List<MultipartFile> files, String atchFileGrpid, String userId) throws IOException;

	/**
	 * 단일 파일 정보 조회 (Select)
	 * @param fileId 파일 고유 ID
	 * @return 파일 정보 Map
	 */
	public EgovMap getFileInfo(String fileId);

	/**
	 * 파일 그룹 목록 조회 (Select)
	 * @param atchFileGrpid 파일 그룹 ID
	 * @return 파일 정보 Map 목록
	 */
	public List<EgovMap> getFileInfoList(String atchFileGrpid);

	/**
	 * 파일 삭제 처리 (Delete)
	 * @param fileId 파일 고유 ID
	 * @return 삭제 성공 여부
	 */
	public boolean setDeleteFile(String fileId);
	
	public List<EgovMap> getFileList(EgovMap egovMap);
	
	/**
	 * 파일을 temp 폴더에 복사 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SqlSessionException
	 */
	public void uploadTempFiles(EgovMap pFileMap) throws IOException;
	
	/**
	 * Stream으로부터 파일을 저장함.
	 * @param is InputStream
	 * @param file File
	 * @throws IOException
	 */
	public long saveFile(InputStream is, File file) throws SqlSessionException, IOException;
	
	/**
	 * 파일 단건 조회
	 * @param param
	 * @return
	 */
	public EgovMap selectFileInfo(EgovMap param);

	/**
	 * 파일 단건 조회(게시판)
	 * @param param
	 * @return
	 */
	public EgovMap selectFileGroupInfo(EgovMap param);
	
	public EgovMap uploadFileInsert( MultipartFile file, String userId) throws DataAccessException, RuntimeException, Exception;
	
	/**
	 * 파일을 zip 폴더에 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SqlSessionException
	 */
	public List<EgovMap> selectFileZipInfo (EgovMap loParamMap);
	
	/**
	 * 파일을 real 폴더에 복사 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @throws SftpException 
	 * @throws JSchException 
	 * @throws Exception
	 */
	public String uploadRealCopyFiles(String fileData, String filePath) throws FileNotFoundException, IOException;
	public String uploadRealCopyFiles2(String fileData, String orgGroupId) throws FileNotFoundException, IOException;
	
	/**
	 * 저장된 파일을 삭제 처리한다.
	 * @param EgovMap
	 * @return
	 * @throws Exception
	 */
	public void deleteFile(EgovMap pFileMap, String pSubPath) throws DataAccessException, IOException;
	
	public int getDeleteFileId(String groupId);
	
	public int getDeleteFileGroupId(String groupId);
	
	/**
	 *파일 사이즈를 가졍온다
	 * @param EgovMap
	 * @return
	 * @throws Exception
	 */
	public EgovMap getFileWidthHeight(String fileId, String pSubPath) throws IOException;
	
	String getOriginalFileName(String fileName) throws DataAccessException;

}
