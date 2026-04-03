package arkive.admin.comm.web;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

import org.egovframe.rte.fdl.cmmn.exception.EgovBizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.FileCopyUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import arkive.admin.comm.service.EgovWebUtil;
import arkive.admin.comm.service.FileDownloadVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FileUtil {
	
	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	protected final String BASE_PATH = String.valueOf(ConfigUtil.getProperty("common.upload.basePath"));

	
	/**
	 *  이미지 포맷을 확인
	 * @param file	원본파일
	 * @return String
	 * @exception 
	 */
	public static String getImageFormat(File file) throws IOException {
        String formatName = "Unknown";
		
        try {
        	ImageInputStream iis = ImageIO.createImageInputStream(file); 
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                
                formatName = reader.getFormatName();
                if (formatName != null) {
                	formatName = formatName.toLowerCase();
                }
            }
            
            iis.close();
        }catch(IOException e) {
        	formatName = "Unknown";
        }catch(RuntimeException e) {
        	formatName = "Unknown";
        }
        
        return formatName;
    }
	
	/**
	 *  이미지를 회전
	 * @param originalImage	원본파일 저장경로
	 * @param angle 각도
	 * @return BufferedImage
	 * @exception 
	 */
    public static BufferedImage rotateImage(BufferedImage originalImage, double angle) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 이미지 회전 변환 설정
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), width / 2, height / 2);

        // 회전된 이미지의 크기를 결정하기 위해 새 이미지 크기 계산
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotatedImage = new BufferedImage(width, height, originalImage.getType());

        // 회전된 이미지를 새로운 BufferedImage에 그리기
        Graphics2D g2d = rotatedImage.createGraphics();
        g2d.drawImage(originalImage, op, 0, 0);
        g2d.dispose();

        return rotatedImage;
    }
	
	/**
	 *  썸네일 이미지 생성
	 * @param srcFilePath	원본파일 저장경로
	 * @param srcFileName	원본파일명
	 * @return boolean
	 * @exception 
	 */
	public static boolean createThumbnail(String srcFilePath, String srcFileName) {
		try {
			int maxSize = 800;
	        int thumbnail_width 	= maxSize;
	        int thumbnail_height 	= maxSize;
	        
	        File originFileName = new File(srcFilePath +"/"+ srcFileName);
	        
	        // EXIF 메타데이터 읽기
	        Metadata metadata = null;
	        try {
	            metadata = ImageMetadataReader.readMetadata(originFileName);
	        } catch (IOException e) {
	            logger.error("IOException 메타데이터를 읽을 수 없습니다: " + originFileName.getName());
	        }

	        int orientation = 1; // 기본적으로 정상 방향
	        if (metadata != null) {
	            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
	                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	            }
	        }
	        
	        logger.error(srcFileName + "\t :: orientation :: " + orientation);	
	        
	        String fileExt = getFileExt(srcFileName);
	        String thumbSavePathName = srcFilePath + "/thumb/";
	        String newFileName = thumbSavePathName + srcFileName;
	        
	        Path thumbPath = Paths.get(thumbSavePathName);
	        if(!Files.exists(thumbPath)) {
	        	Files.createDirectory(thumbPath);
	        	//FileUploadUtil.setFileAuth755(thumbSavePathName);
	        }
	        
	        // 원본 이미지를 읽어온다
            BufferedImage originalImage = ImageIO.read(originFileName);
            BufferedImage rotatedImage 	= null;
	            
            if(orientation == 6) {
            	 rotatedImage 	= rotateImage(originalImage, 90);
            }else if(orientation == 3) {
            	rotatedImage 	= rotateImage(originalImage, 180);
            }else if(orientation == 8) {
            	rotatedImage 	= rotateImage(originalImage, 270);
            }else {
            	rotatedImage = ImageIO.read(originFileName);
            }
            
	        double imgWidth 	= originalImage.getWidth();
            double imgHeight 	= originalImage.getHeight();
	        double rotatedImgWidth 	= rotatedImage.getHeight();
	        double rotatedImgHeight = rotatedImage.getWidth();
 	    
	        if(orientation == 6 || orientation == 8) {
	        	if(rotatedImgWidth < rotatedImgHeight) {
		            thumbnail_width = (int)((rotatedImgHeight / rotatedImgWidth) * maxSize);
		        } else {
		            thumbnail_height = (int)((rotatedImgWidth / rotatedImgHeight) * maxSize);
		        }
	        }else {
		        if(imgWidth < imgHeight) {
		            thumbnail_width = (int)((imgWidth / imgHeight) * maxSize);
		        } else {
		            thumbnail_height = (int)((imgWidth / imgHeight) * maxSize);
		        }
	        }
	        
	        // 원본 이미지 포맷을 확인한다.
	        String imageFormat = getImageFormat(originFileName);
            
	        // 이미지포맷을 확인할 수 없을 경우 파일 확장자로 대체한다.
	        if(imageFormat == null || "Unknown".equals(imageFormat)) {
	        	imageFormat = fileExt;
            }
	        
            // 리사이즈를 위한 새로운 BufferedImage 생성
            BufferedImage thumbnailBufferedImage = new BufferedImage(thumbnail_width, thumbnail_height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphic2D = thumbnailBufferedImage.createGraphics();

            // 리사이즈 및 품질 설정
            graphic2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if(orientation == 6 || orientation == 3 || orientation == 8) {
            	graphic2D.drawImage(rotatedImage, 0, 0, thumbnail_width, thumbnail_height, null);
            }else {
            	graphic2D.drawImage(originalImage, 0, 0, thumbnail_width, thumbnail_height, null);
            }
            
            graphic2D.dispose();
            
            // 썸네일 저장
            File thumbFileName = new File(newFileName);
            ImageIO.write(thumbnailBufferedImage, imageFormat, thumbFileName); 
            
            graphic2D.dispose();
            
            return true;
        
	    } catch (IOException e) {
        	//logger.error("error :: "+e.getMessage(), e);
	    	logger.error("error :: createThumbnail IOException", e);
	    	return false;
		} catch (RuntimeException e) {
        	//logger.error("error :: "+e.getMessage(), e);
			logger.error("error :: createThumbnail RuntimeException", e);
			return false;
		} catch (Exception e) {
			logger.error("error :: createThumbnail Exception", e);
	    	//logger.error(e.getStackTrace().toString());
	    	return false;
	    }
		
	}
	
	private static String getFileExt(String fileName) {
		int i = fileName.lastIndexOf('.');
	    int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
	    if (i > p) {
	        return fileName.substring(i+1);
	    }
	    
	    return null;
	}
	
	private static void writeJpeg(BufferedImage image, String destFile, float quality) throws IOException {
	    ImageWriter writer = null;
	    FileImageOutputStream output = null;
	          
	    try {
	        writer = ImageIO.getImageWritersByFormatName("jpeg").next();
	    
	        ImageWriteParam param = writer.getDefaultWriteParam();
	            
	        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        param.setCompressionQuality(quality);
	            
	        output = new FileImageOutputStream(new File(destFile));
	        writer.setOutput(output);
	            
	        IIOImage iioImage = new IIOImage(image, null, null);
	        writer.write(null, iioImage, param);
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (writer != null) {
	            writer.dispose();
	        }
	        
	        if (output != null) {
	            output.close();
	        }
	    }
	}
	
	public void filDown(HttpServletRequest request,
			HttpServletResponse response,String path, String filePath, String realFilNm,
			String viewFileNm) throws IOException {
		String fullPath = path+ filePath;//realFileNm에도 폴더경로 있어서 합침
		//File file = new File(CommUtil.filePathBlackList(filePath), FilenameUtils.getName(realFilNm));
		File file = new File(fullPath);
		if (file.exists() && file.isFile()) {
			
			String header = request.getHeader("User-Agent");

			response.setHeader("Pragma", "public");
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
			response.setHeader("Ajax", "true");
			//response.setHeader("Content-type", "application-download");
			response.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
			//response.setHeader("Content-Disposition", "attachment; filename="+ "\""  +URLEncoder.encode(viewFileNm, "UTF-8")+ "\"" );
			
			//response.setHeader("Content-Disposition", "attachment; filename="+new String(viewFileNm.getBytes("UTF-8"), "ISO-8859-1") );
			
	        if (header.contains("MSIE") || header.contains("Trident")) {
	        	viewFileNm = URLEncoder.encode(viewFileNm,"UTF-8").replaceAll("\\+", "%20");
	            response.setHeader("Content-Disposition", "attachment;filename=" + viewFileNm + ";");
	        } else {
	        	viewFileNm = new String(viewFileNm.getBytes("UTF-8"), "ISO-8859-1");
	           response.setHeader("Content-Disposition", "attachment; filename=\"" + viewFileNm + "\"");
	        }
			
 
			response.setHeader("Content-Transfer-Encoding", "binary");
			
			response.setContentType("application/octet-stream; charset=utf-8");
			response.setContentLength((int) file.length());
			String browser = getBrowser(request);
			String disposition = getDisposition(viewFileNm, browser);
			/*response.setHeader("Content-Disposition", disposition);
			response.setHeader("Content-Transfer-Encoding", "binary");*/
			OutputStream out = response.getOutputStream();
			FileInputStream fis = null;
			
			try {
				fis = new FileInputStream(file);
				FileCopyUtils.copy(fis, out);
			}catch(FileNotFoundException e) {
			
				logger.error("FileInputStream ERROR");
			}catch(IOException e) {
				logger.error("FileCopyUtils ERROR");
			}finally {
				if (fis != null)
					fis.close();
			}
			out.flush();
			out.close();
		}else {
			logger.error("FileCopyUtils ERROR 파일을 찾을 수 없습니다.");
			logger.error("FileCopyUtils :: "+fullPath);
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<script>alert('파일을 찾을 수 없습니다.'); window.history.back();</script>");
			out.flush();
		}
	}
	
	/**
	 * 파일 삭제
	 * @param lsFileDelData
	 * @param pSubPath
	 * @return
	 * @throws Exception
	 */
	public static void deleteRealFiles(String path, String fileId) throws DataAccessException, IOException {
		//시큐어코딩을 위하여 파일명 검증
		//File loDelFile = new File(CommUtil.filePathBlackList(path), FilenameUtils.getName(lsFileId));
		File loDelFile = new File(EgovWebUtil.filePathBlackList(path + fileId));
		
		if(loDelFile.exists()) {
			//loDelFile.delete();
			fileDelete(loDelFile);
		}
	}

	/**
	 * java secure coding(TOCTOU Issue)
	 * @param File
	 * @return boolean
	 * @throws 
	 */
	private synchronized static boolean fileDelete(File file) {
		return file.delete();
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
	
	public void downloadZip(HttpServletResponse response, FileDownloadVO fileDownloadVO, Map<String, String> originFileNameMap) throws EgovBizException { 
		
		//압축될 파일명이 없을 때
		if(fileDownloadVO.getZipFileName() == null || "".equals(fileDownloadVO.getZipFileName())) {
			throw new EgovBizException("파일명이 존재하지 않습니다.");
		}
		
		//파일이 없을 때
		if(fileDownloadVO.getSourceFile() == null || fileDownloadVO.getSourceFile().length == 0) {
			throw new EgovBizException("파일이 존재하지 않습니다.");
		}

		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileDownloadVO.getZipFileName().getBytes(StandardCharsets.UTF_8)) + ".zip");
		response.setStatus(HttpServletResponse.SC_OK);
		
		try(ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())){
			
			Map<String, Integer> fileNameCnt = new HashMap<>();
			
			//List<String>에 저장된 파일명 검색
			for(String sourceFile : fileDownloadVO.getSourceFile()) {
				Path path = Paths.get(sourceFile);		//파일경로
				String originFileName = (String)originFileNameMap.get(path.toString());		//key: 파일경로, value: 원본파일명으로 된 Map에서 원본파일명 선택
				
				try(FileInputStream fis = new FileInputStream(path.toFile())){
					//압축될 파일명을 ZipEntry에 저장
					ZipEntry zipEntry = new ZipEntry(originFileName);
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
			logger.error("파일 처리 중 오류 발생: {}", e);
		} finally {
			try {
				response.flushBuffer();
			} catch (IOException e) {
				logger.error("파일 처리 중 오류 발생: {}", e);
			}
		} 	
	}
}

