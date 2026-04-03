/************************************
 * @class    : FileController.java
 * @Description	: 파일 Controller
 * @Author      : 문준구
 * @LastUpdate  : 2020.02.12
*/

package arkive.admin.comm.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.session.SqlSessionException;
import org.apache.log4j.Logger;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import arkive.admin.comm.service.EgovProperties;
import arkive.admin.comm.service.FileService;
import arkive.admin.comm.service.FileUploadUtil;
import arkive.admin.comm.service.FileUploadVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class FileController extends FormBasedFileUtil{

	protected final Logger logger = Logger.getLogger(getClass());		//log4j 사용 정의
	
	protected final int BUFFER_SIZE = 8192;

	protected final String SEPERATOR = File.separator;
	
	public static String FILE_TEMP_PATH 	= "";
	public static String FILE_REAL_PATH 	= "";
	
	@Resource(name = "fileService")
	private FileService fileService;
	
	private CommUtil cmmUtil = new CommUtil();
	
	/**
	 * 파일업로드 페이지로 이동
	 * @param
	 * @param
	 * @return : 파일업로드 페이지
	 * @throws
	*/
	@RequestMapping(value = "fileUpload/fileUpload.do")
	public ModelAndView goUser(HttpServletRequest request, ModelMap model)  {
		ModelAndView  mView = new ModelAndView();
		
		try {
			String lsMaxFilesNum 	= cmmUtil.convertHtml(request, "maxFilesNum");
			String lsJobId 			= cmmUtil.convertHtml(request, "jobId");
			String lsType 			= cmmUtil.convertHtml(request, "type");
			String lsExistFileId	= cmmUtil.convertHtml(request, "existFileId");
			
			mView.setViewName("common/fileUpload");
			
			model.put("maxFilesNum", lsMaxFilesNum);
			model.put("existFileId", lsExistFileId);
			model.put("jobId", lsJobId);
			model.put("type", lsType);		//type = 확장자 제한 있을 경우 fileUpload에서 구분하기 위해 추가
		
		} catch(SqlSessionException e) {
			logger.debug("goUser :: SqlSessionException");
		}
		
		return mView;
	}
	
	/**
	 * temp 폴더에 파일 업로드
	 * @param	MultipartHttpServletRequest
	 * @return : 
	 * @throws
	*/
	@RequestMapping(value = "file/tempFileUpload.json")
	public String setTempFileUpload(MultipartHttpServletRequest request, ModelMap model)  {
		try {
			HttpSession session = request.getSession();
			
			List<MultipartFile> fileList 	= request.getFiles("files");
			String lsExistFileId 			= cmmUtil.convertHtml(request, "existFileId");
			List<?> loTempFileList 			= uploadTempFiles(fileList, lsExistFileId, "");
			model.put("tempFileList", loTempFileList);
			
		} catch(SqlSessionException | IOException e) {
			logger.debug("setTempFileUpload :: SqlSessionException | IOException");
		}
		
		return "jsonView";
	}
	
	/**
	 * temp 폴더에 파일 업로드
	 * @param	MultipartHttpServletRequest
	 * @return : 
	 * @throws
	*/
	@RequestMapping(value = "file/reportImgFileUpload.json")
	public String setReportImgFileUpload(MultipartHttpServletRequest request, ModelMap model)  {
		try {
			HttpSession session = request.getSession();
			//LoginVO loginVo = (LoginVO)session.getAttribute("USER");
			
			List<MultipartFile> fileList 	= request.getFiles("files");

			List<?> loTempFileList 			= uploadRealFiles(fileList,"" /*loginVo.getUserId()*/);
			
			model.put("tempFileList", loTempFileList);
			
		} catch(SqlSessionException | IOException e) {
			logger.debug("setReportImgFileUpload :: SqlSessionException | IOException");
		}
		
		return "jsonView";
	}

	/**
	 * 파일을 temp폴더에 Upload 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @
	 */
	public List<?> uploadTempFiles(List<MultipartFile> fileList, String pExistFileId, String pUserId) throws IOException {
		List<EgovMap> loTempFileList = new ArrayList();
		int i = 1;
		String lsFileGroupId = null;
		if(EgovProperties.getProperty("Globals.FILE_TEMP_PATH") != null) {
			FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
		}
		for (MultipartFile mf : fileList) {
			String lsFileId = CommUtil.getFileId();
			
			String tmp = mf.getOriginalFilename();
			InputStream is = null;
			
			try {
				is = mf.getInputStream();
				fileService.saveFile(is, new File(CommUtil.filePathBlackList(FILE_TEMP_PATH + SEPERATOR + lsFileId)));
				EgovMap loFileMap	= new EgovMap();
				if(i == 1) {
					if(pExistFileId != null && !pExistFileId.equals("") && !pExistFileId.equals("undefined")) {
						lsFileGroupId = pExistFileId;
					}else {
						lsFileGroupId = lsFileId;
					}
				}
				loFileMap.put("fileGroupId", lsFileGroupId);
				loFileMap.put("fileId", lsFileId);
				
				loFileMap.put("fileNm", tmp);
				loFileMap.put("fileType", getFileExtension(tmp));
				loFileMap.put("fileSize", mf.getSize());
				loFileMap.put("ordr", i);
				loFileMap.put("userId", pUserId);
				i++;
				loTempFileList.add(loFileMap);
			}catch (IOException e){
				logger.debug("IOexception");
			}
			finally {
				if (is != null) {
					is.close();
				}
			}
		}
		return loTempFileList;
	}
	
	/**
	 * 파일을 temp폴더에 Upload 처리한다.
	 *
	 * @param request
	 * @return 파일리스트
	 * @
	 */
	public List<?> uploadRealFiles(List<MultipartFile> fileList, String pUserId) throws IOException {
		
		List<EgovMap> loTempFileList = new ArrayList();
		int i = 1;
		String lsFileGroupId = null;
		if(EgovProperties.getProperty("Globals.FILE_REAL_PATH") != null) {
			FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
		}
		for (MultipartFile mf : fileList) { 			
			String lsFileId = CommUtil.getFileId();
						
			String tmp = mf.getOriginalFilename();
			InputStream is = null;
			
			try {
				is = mf.getInputStream();
				fileService.saveFile(is, new File(CommUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR + lsFileId)));
				EgovMap loFileMap	= new EgovMap();
				
				if(i == 1) {
					lsFileGroupId = lsFileId;
				}
				loFileMap.put("fileGroupId", lsFileGroupId);
				loFileMap.put("fileId", lsFileId);
				loFileMap.put("fileNm", tmp);
				loFileMap.put("fileType", getFileExtension(tmp));
				loFileMap.put("fileSize", mf.getSize());
				loFileMap.put("ordr", i);
				loFileMap.put("userId", pUserId);
				i++;
				loTempFileList.add(loFileMap);
			}catch (IOException e){
				logger.debug("IOexception");
			}
			finally {
				if (is != null) {
					is.close();
				}
			}
		}
		return loTempFileList;
	}

	/**
	 * 파일 확장자를 추출한다.
	 *
	 * @param fileNamePath
	 * @return
	 */
	public static String getFileExtension(String fileNamePath) {	
		String ext = fileNamePath.substring(fileNamePath.lastIndexOf(".") + 1,fileNamePath.length());
		return (ext == null) ? "" : ext;
	}
	
	@RequestMapping(value="file/downloadFile2.do")
	public void downloadFile2(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		
		try {
			boolean isFileExist = false;
			EgovMap loParamMap = new EgovMap();
			
			String fileId =  cmmUtil.convertHtml(request, "downloadFileId");
			loParamMap.put("fileId", fileId);
			
			EgovMap fileInfo = fileService.selectFileGroupInfo(loParamMap);
			
			filDown(request, response, (String)fileInfo.get("fileCours"), (String)fileInfo.get("fileId"), (String)fileInfo.get("fileNm"));		//파일다운로드
			
		} catch (IOException e) {
			logger.error("downloadFile :: IOException");
		}
	}
	
	/**
	 * 파일 다운로드
	 * @param map 
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value="file/downloadFile.do")
	public void downloadFile(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		
		try {
			boolean isFileExist = false;
			EgovMap param = new EgovMap();
			
			String fileId =  cmmUtil.convertHtml(request, "downloadFileId");

			EgovMap fileInfo = null;
			if(EgovProperties.getProperty("Globals.FILE_REAL_PATH") != null) {
				FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
			}
			
			if (fileId.contains("groupId_")) {
				param.put("fileId", fileId.split("_")[1]);
				fileInfo = fileService.selectFileGroupInfo(param);
			} else {
				param.put("fileId", fileId);
				fileInfo = fileService.selectFileInfo(param);
			}
			
			if(fileInfo.get("fileKnd").equals("pdf") || fileInfo.get("fileKnd").equals("jpg") ||
				fileInfo.get("fileKnd").equals("jpeg") || fileInfo.get("fileKnd").equals("png") || fileInfo.get("fileKnd").equals("gif")) {
				String fileKnd = (String)fileInfo.get("fileKnd");
				String realPath = cmmUtil.filePathBlackList(FILE_REAL_PATH + File.separator + fileId + "." + fileKnd);
				File tempFile = new File(realPath);
				byte[] pdfBytes;
				
				try {
					if (!tempFile.exists() || !tempFile.isFile()) {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.getWriter().write("파일이 없습니다.");
						return;
					}
					pdfBytes = org.apache.commons.io.FileUtils.readFileToByteArray(tempFile);
				} catch (IOException e){
					logger.error("IOException 에러");
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().write("파일을 읽지 못하였습니다..");
					return;
				}
				if(fileInfo.get("fileKnd").equals("pdf")) {
                	response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                }else if(fileInfo.get("fileKnd").equals("jpg") || fileInfo.get("fileKnd").equals("jpeg")) {
                	response.setContentType(MediaType.IMAGE_JPEG_VALUE);
				}else if(fileInfo.get("fileKnd").equals("png")) {
					response.setContentType(MediaType.IMAGE_PNG_VALUE);
				}else if(fileInfo.get("fileKnd").equals("gif")) {
					response.setContentType(MediaType.IMAGE_GIF_VALUE);
				}
				String downloadFileName = fileId + "." + fileKnd;
				
				response.setHeader("Content-Disposition", "inline; filename=\"" + downloadFileName + "\"");
				response.setContentLength(pdfBytes.length);
				if(pdfBytes != null && pdfBytes.length > 0) {
					response.getOutputStream().write(pdfBytes);
				}
				response.getOutputStream().flush();
				
			}else {
				filDown(request, response, (String)fileInfo.get("fileCours"), (String)fileInfo.get("fileId"), (String)fileInfo.get("fileNm"));		//파일다운로드				
			}
			
		} catch (IOException e) {
			logger.error("downloadFile :: IOException");
		}
	}
	
	/**
	 * 파일 존재 유무 체크
	 * @param map e
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value = "file/checkFileExistance.json")
	public String checkFileExistance(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Throwable {

		boolean isFileExist = false;
		String fileId =  cmmUtil.convertHtml(request, "fileId");
		
		EgovMap loParamMap = new EgovMap();		
		loParamMap.put("fileId", fileId);
		
		EgovMap fileInfo = fileService.selectFileInfo(loParamMap);
		
		if(fileInfo != null) {
			String fileCours = (String)fileInfo.get("fileCours");
			String strgFileId = (String)fileInfo.get("fileId");
			
			if(StringUtils.hasText(fileCours) && StringUtils.hasText(strgFileId)) {
				File file = new File(CommUtil.filePathBlackList(fileCours), FilenameUtils.getName(strgFileId));
				if(file.exists()) {
					isFileExist = true;
				}
			}
		}
		
		if(isFileExist) {
			model.addAttribute("result", "true");
			
		} else {
			model.addAttribute("result", "false");
		}
		
		return "jsonView";
	}
	
	/**
	 * 파일그룹 체크
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 * @throws Throwable
	 */
	@RequestMapping(value = "file/checkFileGroupExistance.json")
	public String checkFileGroupExistance(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Throwable {
		String groupId =  cmmUtil.convertHtml(request, "groupId");
		
		EgovMap loParamMap = new EgovMap();		
		loParamMap.put("groupId", groupId);
		
		List<EgovMap> fileList = fileService.getFileList(loParamMap);
		
		model.addAttribute("result", fileList);
		
		return "jsonView";
	}
	
	@RequestMapping(name = "다운로드(zip)", value = "file/fileZipDownload.do", method = RequestMethod.POST)
	public void fileZipDownload(HttpServletRequest req, HttpServletResponse res) throws DataAccessException, IOException {

		EgovMap param = cmmUtil.makeRequestEgovMap(req);
		log.info("param----------------------------- type=[{}]", new Object[]{param});
		
		List<EgovMap> fileList = fileService.getFileList(param);
		List<File> files = new ArrayList<>();
		
		for (EgovMap map : fileList) {
			String fileCours = (String) map.get("fileCours");
			String fileId = (String) map.get("fileId");
			String fileExt = (String) map.get("fileKnd"); // 확장자
			String streFileNm = fileId + "." + fileExt;
		
			if (fileCours != null && streFileNm != null) {
				File file = new File(fileCours, streFileNm);
				if (file.exists()) {
					files.add(file);
				}
			}
		}

		String zipFileNm = param.get("title") + ".zip";
		
		String encodedZipNm = "";
		
		try {
			encodedZipNm = URLEncoder.encode(zipFileNm, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e1) {
			logger.debug("UnsupportedEncodingException 에러");
		}
		
		res.setContentType("application/zip");
		res.setHeader("Content-Disposition", "attachment; filename=\"" + encodedZipNm + "\"");
		res.setHeader("Content-Transfer-Encoding", "binary");
		
		byte[] buffer = new byte[4096];

		try (ZipOutputStream zos = new ZipOutputStream(res.getOutputStream())) {
			for (EgovMap map : fileList) {
				String fileCours = (String) map.get("fileCours");
				String fileId = (String) map.get("fileId");
				String fileExt = (String) map.get("fileKnd");
				String fileNm = (String) map.get("fileNm");
				String streFileNm = fileId + "." + fileExt;

				File file = new File(fileCours, streFileNm);

				if (file.exists()) {
					try (FileInputStream fis = new FileInputStream(file)) {
						String zipEntryName = (fileNm != null) ? fileNm : file.getName();
						zos.putNextEntry(new ZipEntry(zipEntryName));
						
						int len;
						while ((len = fis.read(buffer)) > 0) {
							zos.write(buffer, 0, len);
						}
						zos.closeEntry();
					}
				}
			}
			zos.finish();
	    } catch (IOException e) {
			log.error("ZIP 다운로드 에러");
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "압축 파일 전송 실패");
		}
	}

	
	/**
	 * 파일 다운로드
	 * @param map 
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value="file/pmisDownloadFile.do")
	public void pmisDownloadFile(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		URL url = null;
        InputStream in = null;
        OutputStream out = null;
        
		try {
			
			String fileNm 	= cmmUtil.convertHtml(request, "fileNm");
			String filePath	= cmmUtil.convertHtml(request, "filePath");
			filePath = filePath.replace("https", "http");
			
			String header = request.getHeader("User-Agent");
			
			if(header.contains("MSIE") || header.contains("Trident")) {
				fileNm = URLEncoder.encode(fileNm, "UTF-8").replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename="+ fileNm +";");
            } else {
            	fileNm = URLEncoder.encode(fileNm, "UTF-8").replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename=\""+ fileNm +"\"");
            }
			
			response.setHeader("Pragma", "no-cache;");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Transfer-Encoding", "binary");
            
            out = response.getOutputStream();
            
            char[] filePathChar = filePath.toCharArray();
            for (int j = 0; j < filePathChar.length; j++) {
                if ((filePathChar[j] >= '\uAC00' && filePathChar[j] <= '\uD7A3') || filePathChar[j] == '\u0020') {
                    String targetText = String.valueOf(filePathChar[j]);
                    try {
                    	filePath = filePath.replace(targetText, URLEncoder.encode(targetText, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                    	logger.debug("pmisDownloadFile :: UnsupportedEncodingException");
                    }
                } 
            }
            filePath = filePath.replaceAll("\\+", "%20");
            
            url = new URL(filePath);
            
            in = url.openStream();
            
            while(true){
                //파일을 읽어온다.
                int data = in.read();
                if(data == -1){
                    break;
                }
                //파일을 쓴다.
                out.write(data);
            }
 
            in.close();
            out.close();
			
		} catch (IOException e) {
			logger.error("파일다운로드 오류 ");
		} finally {
            if(in != null) in.close();
            if(out != null) out.close();
        }
	}

	
	public void filDown(HttpServletRequest request,
			HttpServletResponse response, String filePath, String realFilNm,
			String viewFileNm) throws IOException {
		File file = new File(CommUtil.filePathBlackList(filePath), FilenameUtils.getName(realFilNm));
		if (file.exists() && file.isFile()) {
			response.setContentType("application/octet-stream; charset=utf-8");
			response.setContentLength((int) file.length());
			String browser = getBrowser(request);
			String disposition = getDisposition(viewFileNm, browser);
			response.setHeader("Content-Disposition", disposition);
			response.setHeader("Content-Transfer-Encoding", "binary");
			OutputStream out = response.getOutputStream();
			FileInputStream fis = null;
			fis = new FileInputStream(file);

			if (fis != null) {
				try {
					FileCopyUtils.copy(fis, out);
				} catch (IOException e) {
					logger.debug("file copy fail");
				} finally {
					fis.close();
				}
			}
			out.flush();
			out.close();
		}else {
			logger.error("1+  "+file.exists()+"__"+file.isFile());
			logger.error("2+  "+filePath+"__"+realFilNm);
			logger.error("3+  "+CommUtil.filePathBlackList(filePath)+"__"+FilenameUtils.getName(realFilNm));
		}
	}
	
	/**
	 * 이미지 파일 띄우기
	 * @param
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value = "/document/showImageFile.do")
	public  void showImageFile(
			HttpSession session
			, HttpServletRequest request
			, HttpServletResponse response
			, ModelAndView mav) throws Throwable {
		try {
			String lsFileCours	= cmmUtil.convertHtml(request, "fileCours");
			String lsFileId		= cmmUtil.convertHtml(request, "fileId");
			String lsFileNm 	= cmmUtil.convertHtml(request, "fileNm");
			
			if(EgovProperties.getProperty("Globals.FILE_REAL_PATH") != null) {
				FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
			}
			
			File file = new File(CommUtil.filePathBlackList(FILE_REAL_PATH + SEPERATOR + lsFileCours), FilenameUtils.getName(lsFileId));
			FileInputStream fis = new FileInputStream(file);
			OutputStream out = response.getOutputStream();
			if (fis != null) {
				try {
					FileCopyUtils.copy(fis, out);
				} catch (IOException e) {
					logger.debug("fis close fail");
				} finally {
					fis.close();
				}
			}
			out.flush();
			out.close();
		} catch (IOException e) {

			logger.error("이미지 보여주기 에러");
		}
	}
	
	private String getBrowser(HttpServletRequest request) {
		String header = request.getHeader("User-Agent");
		if (header.indexOf("MSIE") > -1 || header.indexOf("Trident") > -1)
			return "MSIE";
		else if (header.indexOf("Chrome") > -1)
			return "Chrome";
		else if (header.indexOf("Opera") > -1)
			return "Opera";
		return "Firefox";
	}

	private String getDisposition(String filename, String browser)
			throws UnsupportedEncodingException {
		String dispositionPrefix = "attachment;filename=\"";
		String encodedFilename = null;
		if (browser.equals("MSIE")) {
			encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll(
					"\\+", "%20");
		} else if (browser.equals("Firefox")) {
			encodedFilename = "\""
					+ new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
		} else if (browser.equals("Opera")) {
			encodedFilename = "\""
					+ new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
		} else if (browser.equals("Chrome")) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < filename.length(); i++) {
				char c = filename.charAt(i);
				if (c > '~') {
					sb.append(URLEncoder.encode("" + c, "UTF-8"));
				} else {
					sb.append(c);
				}
			}
			encodedFilename = sb.toString();
		}
		return dispositionPrefix + encodedFilename +"\";";
	}
	
	@PostMapping("/file/upload.do")
	public void postImage(MultipartHttpServletRequest mptRequest, HttpServletRequest request, HttpServletResponse response) {
        PrintWriter printWriter = null;

        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        if(EgovProperties.getProperty("Globals.FILE_TEMP_PATH") != null) {
			FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
		}
        String imgUploadPath = FILE_TEMP_PATH;
        
        try {
        	List<FileUploadVO> fileList = FileUploadUtil.extendUploadFiles(mptRequest, imgUploadPath);
        	
				
			String saveFileName = fileList.get(0).getSaveFileName();
			String serverSubPath = fileList.get(0).getServerSubPath();
			
			String callback = request.getParameter("CKEditorFuncNum");
			callback = callback.replace("<", "");
			
			String realImagePath = "/tempShowImage" + File.separator + saveFileName;
			realImagePath = realImagePath.replace("\\", "/");
			
			printWriter = response.getWriter();
			
			/* printWriter.println("<script type='text/javascript'>"
                    + "window.parent.CKEDITOR.tools.callFunction("
                    + callback + ",'" + realImagePath + "','이미지를 업로드하였습니다.')"
                    +"</script>"); */
			
			printWriter.println(" <html   xml:lang=\"ko\" lang=\"ko\" xmlns=\"http://www.w3.org/1999/xhtml\">\n");
			printWriter.println(" 	<script language=\"javascript\">");
			printWriter.println("		window.parent.CKEDITOR.tools.callFunction(");
			printWriter.println(callback + ",'" + realImagePath + "','이미지를 업로드하였습니다.')");
			printWriter.println(" 		</script>");
			printWriter.println(" </html>");

            printWriter.flush();
			
		} catch (IOException e) {
			logger.error("IOException :: /file/upload.do");
		} catch (InterruptedException e) {
			logger.error("InterruptedException :: /file/upload.do");
		}
	}
	
	@RequestMapping(value = "/file/ImgUpload.do")
	public ModelAndView ImgUpload(MultipartHttpServletRequest mptRequest, HttpServletRequest request, HttpServletResponse response) {
		//드래그 앤 드랍을 사용할시에 데이터를 받아올땐 mav로 리턴시엔 jsonView로
		ModelAndView  mView = new ModelAndView("jsonView");
		if(EgovProperties.getProperty("Globals.FILE_TEMP_PATH") != null) {
			FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
		}
		String imgUploadPath = FILE_TEMP_PATH;
		try {
			List<FileUploadVO> fileList = FileUploadUtil.extendUploadFiles(mptRequest, imgUploadPath);
			
			String saveFileName = fileList.get(0).getSaveFileName();
			
			String realImagePath = "/tempShowImage" + File.separator + saveFileName;
			realImagePath = realImagePath.replace("\\", "/");
			mView.addObject("uploaded", true);
			mView.addObject("url", realImagePath);
			
		} catch (IOException e) {
			logger.error("IOException :: /file/ImgUpload.do");
		} catch (InterruptedException e) {
			logger.error("InterruptedException :: /file/ImgUpload.do");
		}
		
		return mView;
	}
	
	@RequestMapping(value = "/file/setFileDelete.json")
	public String setFileDelete(HttpServletRequest request, ModelMap model) throws DataAccessException, FileNotFoundException, IOException {
		
		String fileId = cmmUtil.convertHtml(request, "fileId");
		
		int result = fileService.getDeleteFileId(fileId);
		
		model.put("result", result);
		
		return "jsonView";
	}
	
	@RequestMapping(name = "파일목록 조회", value = "/file/getFileList.json")
		public String getFileList(@RequestParam Map<String, Object> map, ModelMap model) throws DataAccessException {
		EgovMap param = new EgovMap();
		String fileId = (String) map.get("fileId");
		param.put("groupId", fileId);
		
		log.info("--------------------------> /getFileList.json");
		log.info("getFileList ----------------------------- type=[{}]", new Object[] { param });
		
		List<EgovMap> result = fileService.getFileList(param);
		
		model.put("result", result);
		
		return "jsonView";
	}
	

	@RequestMapping(name = "PDF 조회", value = "/file/pdfView.json")
    public ResponseEntity<byte[]> pdfView(HttpServletRequest request) throws DataAccessException, UnsupportedEncodingException{
    	request.setCharacterEncoding("utf-8");
        String fileId =  cmmUtil.convertHtml(request, "fileId");
        String fileKnd = cmmUtil.convertHtml(request, "fileKnd");
        if(EgovProperties.getProperty("Globals.FILE_REAL_PATH") != null) {
			FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
		}
        String realPath = cmmUtil.filePathBlackList(FILE_REAL_PATH + File.separator + fileId+"."+fileKnd);
        File tempFile = new File(realPath);
        byte[] pdfBytes;
        
        try {
        	pdfBytes = org.apache.commons.io.FileUtils.readFileToByteArray(tempFile);
        } catch (IOException e){
      	  logger.error("IOException 에러");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.".getBytes(StandardCharsets.UTF_8));
        }
            
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF); // PDF 타입 지정
        String downloadFileName = fileId + "." + fileKnd;
        //파일이 다운로드 되지 않고 브라우저 내에 보이도록 함
        headers.set("Content-Disposition", "inline; filename=\"" + downloadFileName + "\"");
        headers.setContentLength(pdfBytes.length); // 파일 길이 설정
        
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
