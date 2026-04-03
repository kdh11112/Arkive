package arkive.admin.comm.web;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import arkive.admin.comm.service.FileDownloadVO;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FileDownloadUtil {
	
//	@Resource(name = "historyService")
//	private HistoryService historyService;
	
	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	public void downloadZip(HttpServletResponse response, FileDownloadVO fileDownloadVO) {
		
		//압축될 파일명이 없을 때
		if(fileDownloadVO.getZipFileName() == null || "".equals(fileDownloadVO.getZipFileName())) {
			try {
				throw new IllegalArgumentException("파일명이 존재하지 않습니다.");
			} catch(Error e) {
				logger.error("downloadZip :: IllegalArgumentException");
			}
		}
		
		//파일이 없을 때
		if(fileDownloadVO.getSourceFile() == null || fileDownloadVO.getSourceFile().length == 0) {
			try {
				throw new IllegalArgumentException("파일이 존재하지 않습니다.");
			} catch(Error e) {
				logger.error("downloadZip :: IllegalArgumentException");
			}
		} 

		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileDownloadVO.getZipFileName().getBytes(StandardCharsets.UTF_8)) + ".zip");
		response.setStatus(HttpServletResponse.SC_OK);
		
		try(ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())){
			
			Map<String, Integer> fileNameCnt = new HashMap<>();
			
			//List<String>에 저장된 파일명 검색
			for(String sourceFile : fileDownloadVO.getSourceFile()) {
				Path path = Paths.get(sourceFile);
				
				//파일명을 원본파일명으로 변환
				String fileName = path.getFileName().toString();
				int lastIdx = fileName.lastIndexOf(".");
				String realFileName = fileName.substring(0, lastIdx);
				String originalFileName = getOriginalFileName(realFileName);
				
				String uniqueFileName = originalFileName;
				
				if(fileNameCnt.containsKey(originalFileName)) {
					int cnt = fileNameCnt.get(originalFileName) + 1;
					
					int unqLastIdx = uniqueFileName.lastIndexOf(".");
					uniqueFileName = uniqueFileName.substring(0, unqLastIdx) + " (" + cnt + ")" + uniqueFileName.substring(unqLastIdx);
					
					fileNameCnt.put(originalFileName, 1);
				} else {
					fileNameCnt.put(originalFileName, 1);
				}
				

				try(FileInputStream fis = new FileInputStream(path.toFile())){
					//압축될 파일명을 ZipEntry에 저장
					ZipEntry zipEntry = new ZipEntry(uniqueFileName);
					//압축될 파일명을 ZipOutputStream에 저장
					zos.putNextEntry(zipEntry);
					
					byte[] buffer = new byte[1024];
					int length;
					while((length = fis.read(buffer)) >= 0) {
						zos.write(buffer, 0, length);
					}
			
				} catch(FileNotFoundException e) {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					throw new IllegalArgumentException(sourceFile + " 파일을 찾을 수 없습니다.");
					
				} catch(IOException e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					throw new IllegalArgumentException(sourceFile + " 파일을 다운로드 할 수 없습니다.");
					
				} finally {
					zos.flush();
					zos.closeEntry();
				}
			}
			
		} catch (IOException e) {
			logger.error("에러");
		} finally {
			try {
				response.flushBuffer();
			} catch (IOException e) {
				logger.error("에러");
			}
		} 	
	}
	
	private String getOriginalFileName(String fileId) {	
		String originalFileName = "";
		//originalFileName = historyService.getOriginalFileName(fileId);

	    return originalFileName;
	}
}
