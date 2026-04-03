/**
 *
 */
package arkive.admin.comm.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import arkive.admin.comm.web.DateUtil;

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
public class FileUploadUtil extends EgovFormBasedFileUtil{

	private static Logger logger = LoggerFactory.getLogger(FileUploadUtil.class);
	
	private final static long MAX_FILE_SIZE = Long.parseLong(String.valueOf(EgovProperties.getProperty("MaxFileSize")));

	/**
	 * 파일을 Upload 처리한다.
	 * @param request
	 * @param path
	 * @return
	 * @throws IOException 
	 * @throws Exception
	 */
	public static List<FileUploadVO> extendUploadFiles(MultipartHttpServletRequest request, String path) throws IOException, InterruptedException {
		List<FileUploadVO> rtnFileList = new ArrayList<FileUploadVO>();
		if(request != null && path != null) {
			rtnFileList = extendUploadFiles(request, path, MAX_FILE_SIZE);
		}
		return rtnFileList;
	}
	
	/**
	 * 파일을 Upload 처리한다.
	 * @param request
	 * @param path
	 * @param maxFileSize
	 * @return
	 * @throws InterruptedException 
	 * @throws Exception
	 */
	public static List<FileUploadVO> extendUploadFiles(MultipartHttpServletRequest mptRequest, String path, long maxFileSize) throws IOException, InterruptedException {

		List<FileUploadVO> list = new ArrayList<FileUploadVO>();

		Iterator<?> fileIter = mptRequest.getFileNames();
		
		while (fileIter.hasNext()) {
			MultipartFile mFile = mptRequest.getFile((String) fileIter.next());

			if(mFile.getSize() > maxFileSize) {
				throw new IOException("업로드 최대 파일 용량(" + getAddShortUnitFileSize(maxFileSize) + ")을 초과하였습니다.\\n업로드 용량(" + getAddShortUnitFileSize(mFile.getSize()) + ")");
			}
			
			FileUploadVO vo = new FileUploadVO();

			String tmp = mFile.getOriginalFilename();
			
			if (tmp.lastIndexOf("/") >= 0) {
				tmp = tmp.substring(tmp.lastIndexOf("/") + 1);
			}
			
			// 2020-12 소스취약점 START //
			path = path.replaceAll("\\.{2,}[/\\\\]", "");
			// 2020-12 소스취약점 END //

			vo.setFileName(tmp); 
			vo.setContentType(mFile.getContentType());
			vo.setServerSubPath(DateUtil.getCurrentDate());
			vo.setSize(mFile.getSize());
			vo.setExtension(tmp.substring(tmp.lastIndexOf(".")+1).toLowerCase());
			vo.setPhysicalName(getOnlyFileName(path, vo.getServerSubPath(), vo.getExtension()));
			vo.setSaveFileName(vo.getPhysicalName() + "." + vo.getExtension());

			if (tmp.lastIndexOf(".") >= 0) {
				vo.setPhysicalName(vo.getPhysicalName()); // 2012.11 KISA 보안조치
			}

			if (mFile.getSize() > 0) {
				InputStream is = null;

				try {
					is = mFile.getInputStream();
					saveFile(is, new File(EgovWebUtil.filePathBlackList(path + SEPERATOR + vo.getSaveFileName())));
					
				} finally {
					if (is != null) {
						is.close();
					}
				}
				
				list.add(vo);
			}
		}
		
		return list;
	}
	
	/**
	 * 해당 저장 디렉토리에 유일한 파일명을 생성한다.<br />
	 *
	 * @param path 업로드 파일의 저장경로
	 * @param subPath 년월일 디렉토리
	 * @param extension 업로드 파일의 확장자
	 * @return 해당 저장 디렉토리에 유일한 파일명을 반환한다.
	 */
	public static String getOnlyFileName(String path, String subPath, String extension) {

		String returnValue = "";

		try {

			returnValue = getPhysicalFileName();
			// 2020-12 소스취약점 START //
			String filePath = path + SEPERATOR + subPath;
			filePath = filePath.replaceAll("\\.{2,}[/\\\\]", "");
			File existsfile = new File(filePath, returnValue + "." + extension);

			//중복파일 존재시 이름 재생성
			while (existsfile.exists()) {
				returnValue = getOnlyFileName(path, subPath, extension);
			}
			
		} catch(NullPointerException e) {
			//logger.error(e.getMessage(),e);
			logger.error("getOnlyFileName NullPointerException", e);
		}// 2020-12 소스취약점 END //

		return returnValue;

	}

	/**
	 * 축약된 단위가 추가된 파일 크기 문자열을 생성한다.<br />
	 * unitTextArrray 배열의 개별 문자열중 하나가 단위로 사용되며, 해당 배열의 인덱스는 1,024 단위로 나눌수 있는 최대 횟수이다.<br />
	 * 파일 크기 단위는 Byte, KB, MB, GB, TB, PB, EB, ZB, YB 이다.<br />
	 * KB 이하의 파일 크기일 경우 1KB 로 표현된다.<br />
	 *
	 * @param uploadFileSize 업로드 파일 크기
	 * @param unitTextArrray 축약된 파일 크기 단위 문자열 배열
	 * @return 단위가 추가된 파일 크기 문자열을 반환한다.
	 */
	public static String getAddShortUnitFileSize(long uploadFileSize) {

		String[] unitTextArrray = new String[] {
					"Byte",
					"KB",
					"MB",
					"GB",
					"TB",
					"PB",
					"EB",
					"ZB",
					"YB",
				};

		return getAddUnitFileSize(uploadFileSize, unitTextArrray, true);

	}

	/**
	 * 축약된 단위가 추가된 파일 크기 문자열을 생성한다.<br />
	 * unitTextArrray 배열의 개별 문자열중 하나가 단위로 사용되며, 해당 배열의 인덱스는 1,024 단위로 나눌수 있는 최대 횟수이다.<br />
	 * 파일 크기 단위는 Byte, KB, MB, GB, TB, PB, EB, ZB, YB 이다.<br />
	 *
	 * @param uploadFileSize 업로드 파일 크기
	 * @param unitTextArrray 축약된 파일 크기 단위 문자열 배열
	 * @param isDefaultUnitKiloByte Kilo Byte 를 기본 파일 크기 단위로 사용할지 여부(true 일 경우 기본단위가 Kilo Byte, 기본단위 문자열이 unitTextArrray 배열의 1번 인덱스 값이다.)
	 * @return 단위가 추가된 파일 크기 문자열을 반환한다.
	 */
	public static String getAddShortUnitFileSize(long uploadFileSize, boolean isDefaultUnitKiloByte) {

		String[] unitTextArrray = new String[] {
					"Byte",
					"KB",
					"MB",
					"GB",
					"TB",
					"PB",
					"EB",
					"ZB",
					"YB",
				};

		return getAddUnitFileSize(uploadFileSize, unitTextArrray, isDefaultUnitKiloByte);

	}

	/**
	 * 단위가 추가된 파일 크기 문자열을 생성한다.<br />
	 * unitTextArrray 배열의 개별 문자열중 하나가 단위로 사용되며, 해당 배열의 인덱스는 1,024 단위로 나눌수 있는 최대 횟수이다.<br />
	 * 파일 크기 단위는 KiloByte, MegaByte, GigaByte, TeraByte, PetaByte, ExaByte, ZettaByte, YottaByte 이다.<br />
	 * KiloByte 이하의 파일 크기일 경우 1KiloByte 로 표현된다.<br />
	 *
	 * @param uploadFileSize 업로드 파일 크기
	 * @param unitTextArrray 파일 크기 단위 문자열 배열
	 * @return 단위가 추가된 파일 크기 문자열을 반환한다.
	 */
	public static String getAddUnitFileSize(long uploadFileSize) {

		String[] unitTextArrray = new String[] {
					"Byte",
					"KiloByte",
					"MegaByte",
					"GigaByte",
					"TeraByte",
					"PetaByte",
					"ExaByte",
					"ZettaByte",
					"YottaByte",
				};

		return getAddUnitFileSize(uploadFileSize, unitTextArrray, true);

	}

	/**
	 * 단위가 추가된 파일 크기 문자열을 생성한다.<br />
	 * unitTextArrray 배열의 개별 문자열중 하나가 단위로 사용되며, 해당 배열의 인덱스는 1,024 단위로 나눌수 있는 최대 횟수이다.<br />
	 * 파일 크기 단위는 Byte, Kilo Byte, Mega Byte, Giga Byte, Tera Byte, Peta Byte, Exa Byte, Zetta Byte, Yotta Byte 이다.<br />
	 *
	 * @param uploadFileSize 업로드 파일 크기
	 * @param unitTextArrray 파일 크기 단위 문자열 배열
	 * @param isDefaultUnitKiloByte Kilo Byte 를 기본 파일 크기 단위로 사용할지 여부(true 일 경우 기본단위가 Kilo Byte, 기본단위 문자열이 unitTextArrray 배열의 1번 인덱스 값이다.)
	 * @return 단위가 추가된 파일 크기 문자열을 반환한다.
	 */
	public static String getAddUnitFileSize(long uploadFileSize, boolean isDefaultUnitKiloByte) {

		String[] unitTextArrray = new String[] {
					"Byte",
					"KiloByte",
					"MegaByte",
					"GigaByte",
					"TeraByte",
					"PetaByte",
					"ExaByte",
					"ZettaByte",
					"YottaByte",
				};

		return getAddUnitFileSize(uploadFileSize, unitTextArrray, isDefaultUnitKiloByte);

	}

	/**
	 * 단위가 추가된 파일 크기 문자열을 생성한다.<br />
	 * unitTextArrray 배열의 개별 문자열중 하나가 단위로 사용되며, 해당 배열의 인덱스는 1,024 단위로 나눌수 있는 최대 횟수이다.<br />
	 *
	 * @param uploadFileSize 업로드 파일 크기
	 * @param unitTextArrray 파일 크기 단위 문자열 배열
	 * @param isDefaultUnitKiloByte Kilo Byte 를 기본 파일 크기 단위로 사용할지 여부(true 일 경우 기본단위가 Kilo Byte, 기본단위 문자열이 unitTextArrray 배열의 1번 인덱스 값이다.)
	 * @return 단위가 추가된 파일 크기 문자열을 반환한다.
	 */
	public static String getAddUnitFileSize(long uploadFileSize, String[] unitTextArrray, boolean isDefaultUnitKiloByte) {

		String returnValue = "";

		try {
			long divideUploadFileSize = uploadFileSize;

			int unitArrayIndex = 0;

			while (divideUploadFileSize > 1024) {
				divideUploadFileSize = divideUploadFileSize / 1024;

				unitArrayIndex++;
			}

			if ((unitTextArrray != null) && (unitTextArrray.length > unitArrayIndex)) {
				if ((isDefaultUnitKiloByte) && (unitArrayIndex == 0)) {
					if (unitTextArrray.length >= unitArrayIndex + 1) {
						returnValue = "1" + unitTextArrray[unitArrayIndex + 1];
					}
				} else {
					returnValue = EgovStringUtil.getThousandCommaSeperator(divideUploadFileSize) + unitTextArrray[unitArrayIndex];
				}
			} else {
				returnValue = EgovStringUtil.getThousandCommaSeperator(divideUploadFileSize);
			}
			// 2020-12 소스취약점 START //
		} catch (ArrayIndexOutOfBoundsException e) {
			//logger.error(e.getMessage(),e);
			logger.error("getAddUnitFileSize ArrayIndexOutOfBoundsException", e);
		} catch (NullPointerException e) {
			//logger.error(e.getMessage(),e);
			logger.error("getAddUnitFileSize ArrayIndexOutOfBoundsException", e);
		}// 2020-12 소스취약점 END //

		return returnValue;

	}
	
	/**
	 * 업로드한 파일의 속성권한을 644로 설정한다.
	 *
	 * @param filePath 
	 * @param fileNam
	 * @return 파일 속성권한변경 여부를 반환한다.
	 */
	public static boolean setFileAuth644(String filePath, String fileName) {
		boolean authChange = false;
		
		logger.info(">>>>setFileAuth644() >> filePath ::" + filePath);
		logger.info(">>>>setFileAuth644() >> fileName ::" + fileName);
		
		File authfile = new File(filePath, fileName);
		
		if(authfile != null && authfile.exists()) {
			if(!authfile.getParentFile().canRead()) {
				authfile.getParentFile().setReadable(true, false);
			}else {
				logger.info(">>>>setFileAuth644() getParentFile.canRead() :" + filePath);
			}
			if(!authfile.getParentFile().canExecute()) {
				authfile.getParentFile().setExecutable(true, false);
			}else {
				logger.info(">>>>setFileAuth644() getParentFile.canExecute() :" + filePath);
			}
			
			authfile.setReadable(true, false);
			authfile.setWritable(true, true);
			
			authChange = true;
		}else {
			logger.info(">>>> FILE NOT EXSTS ::" + filePath + "/" + fileName);
		}
		
		return authChange;
	}
	
	/**
	 * 업로드한 파일의 속성권한을 755로 설정한다.
	 *
	 * @param filePath 
	 * @param fileNam
	 * @return 파일 속성권한변경 여부를 반환한다.
	 */
	public static boolean setFileAuth755(String filePath, String fileName) throws IOException, InterruptedException{
		boolean authChange = false;
		
		File authfile = new File(filePath, fileName);
		
		if(authfile != null && authfile.exists()) {
			//root에서 실행되지 않도록 /를 제외한 뒤 글자길이 판단
			String checkFilePath = filePath.replaceAll("/", "");
			if(checkFilePath.length() > 0) {
				String cmd = "";  
				
				logger.error("::: setFileAuth755() : " + filePath + fileName);
				filePath = filePath.trim();
				if(filePath.endsWith(File.separator)){
					cmd = "chmod 755 " + filePath + fileName;  
				}else {
					cmd = "chmod 755 " + filePath + File.separator + fileName;  
				}
				
				logger.error("::::::: setFileAuth755() cmd 	:: " + cmd);
				
				Runtime rt = Runtime.getRuntime();
				
				Process prc = rt.exec(cmd);
				
				authChange = true;
				
				prc.waitFor();
			}else {
				logger.info(">>>> filePath error :: " + filePath);
			}
		}else {
			logger.info(">>>> FILE NOT EXSTS ::" + filePath + "/" + fileName);
		}
		
		return authChange;
	}
	
	/**
	 * 업로드한 폴더의 속성권한을 755로 설정한다.
	 *
	 * @param filePath 
	 * @return 폴더 속성권한변경 여부를 반환한다.
	 */
	public static boolean setFileAuth755(String filePath) throws IOException, InterruptedException{
		boolean authChange = false;
		
		File authfile = new File(filePath);
		
		if(authfile != null && authfile.exists()) {
			//root에서 실행되지 않도록 /를 제외한 뒤 글자길이 판단
			String checkFilePath = filePath.replaceAll("/", "");
			if(checkFilePath.length() > 0) {
				if(filePath != null) {
					String cmd = "chmod 755 " + filePath;  
					
					Runtime rt = Runtime.getRuntime();
					
					if(cmd != null && rt != null) {
						Process prc = rt.exec(cmd);
						
						authChange = true;
						
						prc.waitFor();
					}
				}
			}else {
				logger.info(">>>> filePath error :: " + filePath);
			}
		}else {
			logger.info(">>>> FILE NOT EXSTS ::" + filePath);
		}
		
		return authChange;
	}
}
