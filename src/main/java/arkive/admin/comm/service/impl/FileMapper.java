package arkive.admin.comm.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

@EgovMapper("fileMapper")
public interface FileMapper {

	/**
	 * 첨부파일 등록 (Insert)
	 */
	public void setInsertAtchFile(EgovMap param);

	/**
	 * 단일 파일 정보 조회 (Select)
	 */
	public EgovMap getFileInfo(String fileId);

	/**
	 * 파일 그룹 목록 조회 (Select)
	 */
	public List<EgovMap> getFileInfoList(String atchFileGrpid);

	/**
	 * 파일 삭제 (Update / Soft Delete)
	 */
	public void setDeleteAtchFile(String fileId);

}
