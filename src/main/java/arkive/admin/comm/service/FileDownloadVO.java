package arkive.admin.comm.service;

public class FileDownloadVO {
	private String zipFileName;
	private String[] sourceFile;
	//private List<String> sourceFile;
	
	public String getZipFileName() {
		return zipFileName;
	}
	
	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}
	
	public String[] getSourceFile() {
		String[] ret = null;
		if(this.sourceFile != null) {
			ret = new String[sourceFile.length];
			
			for(int i = 0; i < sourceFile.length; i++) {
				ret[i] = this.sourceFile[i];
			}
		}
		
		return ret;
	}
	
	public void setSourceFile(String[] sourceFile) {
		this.sourceFile = new String[sourceFile.length];
		
		for(int i = 0; i < sourceFile.length; i++) {
			this.sourceFile[i] = sourceFile[i];
		}
	}
	
	/* public List<String> getSourceFile() {
		return sourceFile;
	}
	
	public void setSourceFile(List<String> sourceFile) {
		this.sourceFile = sourceFile;
	} */
	
}
