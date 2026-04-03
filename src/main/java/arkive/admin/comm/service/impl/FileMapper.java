package arkive.admin.comm.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

//@Mapper("fileMapper")
public interface FileMapper {
	
	public List<EgovMap> getFileList(EgovMap egovMap);
	
	/**
	 * 파일 상세 정보 조회
	 * @param pEgovMap 
	 * @return 	: 파일 상세 정보
	 * @throws
	*/
	public EgovMap selectFileInfo(EgovMap param);
	
	public EgovMap selectFileGroupInfo(EgovMap param);

	public List<EgovMap> selectFileZipInfo(EgovMap egovMap);

	public void insertAtchFile(EgovMap egovMap);

	public EgovMap selectVdoFileInfo(EgovMap egovMap);

	/**
	 * 파일을 ZIP폴더에 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @
	 */
	public EgovMap selectActPlnFileInfo(EgovMap egovMap);
	
	public void deleteAtchFile(EgovMap egovMap);

	public int getDeleteFileId(String fileId);
	
	public int getDeleteFileGroupId(String fileId);
	
	public String getOriginalFileName(String fileId);
	
	public int setUpdateOthbcAt(EgovMap param);
	
}
