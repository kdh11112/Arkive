/**
 * 
 */
package arkive.admin.comm.service;


/**
 * 
 * @author 4DEPTH 모영석
 * @since 2016. 5. 3.
 * @version 1.0
 * @see 
 * <pre>
 *  == 개정이력(Modification Information) ==
 *   
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2016. 5. 3.  4DEPTH 모영석          최초 생성
 * 
 * </pre>
 */
public class FileUploadVO extends EgovFormBasedFileVo {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 파일 그룹 아이디
	 */
	private String fileGroupId = "";
	
	/**
	 * 파일 확장자
	 */
	private String extension = "";
	
	/**
	 * 원본 파일명
	 */
	private String fileName = "";
	
	/**
	 * 저장 파일명
	 */
	private String saveFileName = "";

	/**
	 * 하위 디렉토리명
	 */
	private String serverSubPath = "";
	
	/**
	 * 썸네일 파일명
	 */
	private String thumbnailFileName = "";

	/**
	 * 썸네일 저장경로명
	 */
	private String thumbnailSavePath = "";
	
	/**
	 * 파일 설명
	 */
	private String atchFileDc = "";
	
	/**
	 * 사용자 아이디
	 */
	private String regId = "";

	/**
	 * 사용자 아이디
	 */
	private String fileSn = "";

	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getServerSubPath() {
		return serverSubPath;
	}

	public void setServerSubPath(String serverSubPath) {
		this.serverSubPath = serverSubPath;
	}

	/**
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @param extension the extension to set
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * @return the saveFileName
	 */
	public String getSaveFileName() {
		return saveFileName;
	}

	/**
	 * @param saveFileName the saveFileName to set
	 */
	public void setSaveFileName(String saveFileName) {
		this.saveFileName = saveFileName;
	}

	public String getThumbnailFileName() {
		return thumbnailFileName;
	}

	public void setThumbnailFileName(String thumbnailFileName) {
		this.thumbnailFileName = thumbnailFileName;
	}

	public String getThumbnailSavePath() {
		return thumbnailSavePath;
	}

	public void setThumbnailSavePath(String thumbnailSavePath) {
		this.thumbnailSavePath = thumbnailSavePath;
	}

	public String getFileGroupId() {
		return fileGroupId;
	}

	public void setFileGroupId(String fileGroupId) {
		this.fileGroupId = fileGroupId;
	}

	public String getAtchFileDc() {
		return atchFileDc;
	}

	public void setAtchFileDc(String atchFileDc) {
		this.atchFileDc = atchFileDc;
	}

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getFileSn() {
		return fileSn;
	}

	public void setFileSn(String fileSn) {
		this.fileSn = fileSn;
	}

	@Override
	public String toString() {
		return "FileUploadVO [fileGroupId=" + fileGroupId + ", extension=" + extension + ", fileName=" + fileName
				+ ", saveFileName=" + saveFileName + ", serverSubPath=" + serverSubPath + ", thumbnailFileName="
				+ thumbnailFileName + ", thumbnailSavePath=" + thumbnailSavePath + ", atchFileDc=" + atchFileDc
				+ ", regId=" + regId + ", fileSn=" + fileSn + "]";
	}

}
