package arkive.admin.comm.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

	/**
	 * 업로드된 파일을 temp 폴더에만 저장한다. (최종 경로 이동 및 DB 등록 없음)
	 * @param files 업로드 파일 목록
	 * @param atchFileGrpid 파일 그룹 ID
	 * @param userId 사용자 ID
	 * @return temp 폴더에 저장된 파일 정보 목록
	 * @throws IOException
	 */
	public List<EgovMap> setUploadFiles(List<MultipartFile> files, String atchFileGrpid, String userId) throws IOException;

	/**
	 * temp 폴더에 저장된 파일을 Arkive 최종 경로로 이동하고 DB에 등록한다.
	 * 멀티파트, TUS, 드롭존 업로드가 공통으로 사용한다.
	 * @param stagedFiles fileId, fileName을 가진 Map 목록
	 * @param atchFileGrpid 파일 그룹 ID
	 * @param userId 사용자 ID
	 * @return 최종 저장된 파일 정보 목록
	 * @throws IOException
	 */
	public List<EgovMap> saveTempFiles(List<Map<String, String>> stagedFiles, String atchFileGrpid, String userId) throws IOException;

	/**
	 * 업로드 타입별 파일 목록 조회
	 * @param type 업로드 타입 (MULTIPART, TUS, DROPZONE)
	 * @return 파일 정보 Map 목록
	 */
	public List<EgovMap> selectAtchFileListByType(String type);

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

	/**
	 * 파일 임시 저장 경로(Globals.FILE_TEMP_PATH).
	 * @return 쓰기 가능한 임시 저장 절대 경로
	 */
	public String getWritableTempPath();

	/**
	 * 최종 저장 경로(Globals.FILE_REAL_PATH).
	 * @return 쓰기 가능한 최종 저장 절대 경로
	 */
	public String getWritableRealPath();

}
