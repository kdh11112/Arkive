package arkive.admin.comm.service;

import java.io.IOException;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
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

}
