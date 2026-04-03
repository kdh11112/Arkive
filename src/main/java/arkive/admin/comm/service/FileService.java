/************************************
 * @class    : FileService.java
 * @Description	: 파일 관련 Service
 * @Author      : 문준구
 * @LastUpdate  : 2020.02.10
*/

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
