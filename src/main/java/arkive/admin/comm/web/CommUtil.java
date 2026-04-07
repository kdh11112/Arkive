/************************************
 * @class    : CommUtil.java
 * @Description	: JAVA에서 사용하는 공통 유틸리티
 * @Author      : 문준구
 * @LastUpdate  : 2020.02.10
*/

package arkive.admin.comm.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.EncoderException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import arkive.admin.comm.service.EgovProperties;
import egovframework.com.com.service.impl.EgovComAbstractDAO;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import twitter4j.JSONArray;
import twitter4j.JSONObject;
import ws.schild.jave.InputFormatException;

@Component
public class CommUtil {

	protected final static   org.slf4j.Logger logger = LoggerFactory.getLogger(CommUtil.class);
	
	public static String PUBLIC_KEY 	= EgovProperties.getProperty("Globals.PUBLIC_KEY");		//공개키
	public static String RSA_WEB_KEY 	= EgovProperties.getProperty("Globals.RSA_WEB_KEY");	// 개인키 sssion key
    public static String RSA_INSTANCE 	= EgovProperties.getProperty("Globals.RSA_INSTANCE");	// rsa transformation
    
    protected  final String FILE_TEMP_PATH 	= EgovProperties.getProperty("Globals.FILE_TEMP_PATH");
	protected  final String FILE_REAL_PATH 	= EgovProperties.getProperty("Globals.FILE_REAL_PATH");
	protected  final String FFMPEG_PATH 	= EgovProperties.getProperty("Globals.FFMPEG_PATH");
	
	private static Key keySpec;
	public static int KEYSIZE = 2048;
	public static String AR_FORMAT = "#,###.##"; 		//천단위, 소수점
	public static String AMOUNT_FORMAT = "#,###.###"; 	//천단위, 소수점 3자리
	
	public static String alg = "AES/CBC/PKCS5Padding";
	private static final String key = EgovProperties.getProperty("private.key");
    //private static final String iv = key.substring(0, 16); 	//16byte
    private static final String iv = ""; 	//16byte
    
    
	/**
     * rsa 개인키 생성
     * 
     * @param request
     */
    public void initRsa(HttpServletRequest request, ModelAndView mView) {
        HttpSession session = request.getSession();
        
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance(RSA_INSTANCE);
            generator.initialize(KEYSIZE);
 
            KeyPair keyPair 		= generator.genKeyPair();
            KeyFactory keyFactory 	= KeyFactory.getInstance(RSA_INSTANCE);
            PublicKey publicKey 	= keyPair.getPublic();
            PrivateKey privateKey 	= keyPair.getPrivate();
            //session.setMaxInactiveInterval(1800);
            session.setAttribute(RSA_WEB_KEY, privateKey); // session에 RSA 개인키를 세션에 저장
            
            
            RSAPublicKeySpec publicSpec = (RSAPublicKeySpec) keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
            String publicKeyModulus 	= publicSpec.getModulus().toString(16);
            String publicKeyExponent 	= publicSpec.getPublicExponent().toString(16);
 
            mView.addObject("RSAModulus", publicKeyModulus);
            mView.addObject("RSAExponent", publicKeyExponent);
        } //catch (SqlSessionException e) {
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO Auto-generated catch block
        	logger.error("에러가 발생했습니다.");
        }
    }
    
    /**
     * 로그인 복호화
     * 
     * @param privateKey
     * @param securedValue
     * @return
     * @
     */
    public String decryptRsa(PrivateKey privateKey, String securedValue) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
    	String decryptedValue = "";
    	try {
	    	Cipher cipher = Cipher.getInstance(RSA_INSTANCE);
	        byte[] encryptedBytes = hexToByteArray(securedValue);
	        cipher.init(Cipher.DECRYPT_MODE, privateKey);
	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        decryptedValue = new String(decryptedBytes, "utf-8"); // 문자 인코딩 주의.
    	}
    	//catch(SqlSessionException e) {
    	catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
    		logger.error("에러가 발생했습니다.");
    	}
        return decryptedValue;
    }
    
    /**
     * AES 암호화
     * 
     * @param privateKey
     * @param securedValue
     * @return
     * @
     */
    public static String getEncryptString(String encryptData) throws NoSuchAlgorithmException, GeneralSecurityException, UnsupportedEncodingException {
    	Cipher cipher = Cipher.getInstance(alg);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);

        byte[] encrypted = cipher.doFinal(encryptData.getBytes("UTF-8"));
        
        return Base64.getEncoder().encodeToString(encrypted);
	}
    
    /**
     * AES 복호화
     * 
     * @param privateKey
     * @param securedValue
     * @return
     * @
     */
    public static String getDecyptString(String encryptedData) throws NoSuchAlgorithmException, GeneralSecurityException, UnsupportedEncodingException { 
        Cipher cipher = Cipher.getInstance(alg);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = cipher.doFinal(decodedBytes);

        return new String(decrypted, "UTF-8");
	}
    
    /**
     * SHA256 암호화
     * 
     * @param planText
     * @return 
     * @
     */
    public static String encryptSHA(String planText) { 
    	try{ 
    		MessageDigest md = MessageDigest.getInstance("SHA-256"); 
    		md.update(planText.getBytes()); 
    		byte byteData[] = md.digest(); 
    		StringBuffer sb = new StringBuffer(); 
    		for (int i = 0; i < byteData.length; i++) { 
    			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1)); 
    		} 
    		StringBuffer hexString = new StringBuffer(); 
    		for (int i=0;i<byteData.length;i++) { 
    			String hex=Integer.toHexString(0xff & byteData[i]); 
    			if(hex.length()==1){ 
    				hexString.append('0'); 
    			} hexString.append(hex); 
    		} 
    		return hexString.toString(); 
    	}
    	//catch(SqlSessionException e){
    	catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException(); 
    	} 
    }

    
    
    /**
	 * 페이징 값 세팅
	 * @param egovMap
	 * @return
	 * @throws Exception
	 */
    public PaginationInfo setPagingValue(EgovMap egovMap, int pageUnit, int pageSize) {
    	PaginationInfo pg = new PaginationInfo();
    	try {
    		
    		// 현재 조회 페이지
			String pageIndexStr = String.valueOf(egovMap.get("pageIndex"));
			if (pageIndexStr.equals("")||pageIndexStr==null) {
				pageIndexStr = "1";
			}//페이지에 처음 들어올 시 페이지인덱스 1로 세팅
			
			int pageIndex = Integer.parseInt(pageIndexStr);
			
			pg.setCurrentPageNo(pageIndex);
			pg.setRecordCountPerPage(pageUnit);
			pg.setPageSize(pageSize);
		
			
		} catch (RuntimeException e) {
			logger.error("setPagingValue Exception");
		}  catch (Exception e) {
			logger.error("setPagingValue Exception");
		}
		return pg;
	}
    
    
    public  EgovMap makeRequestEgovMap(HttpServletRequest request)throws RuntimeException{
    	EgovMap requestMap = new EgovMap();
    	try{
    		Enumeration<String> paramNames = request.getParameterNames();
			while(paramNames.hasMoreElements()){
				String paramName = paramNames.nextElement();
				String[] paramValues = request.getParameterValues(paramName);
				if (paramValues.length == 1){
					String paramValue = paramValues[0];
					if(paramValue.length() == 0 || "".equals(paramValue)){
						continue;
					}else{
						requestMap.put(paramName, paramValue);
					}
				}else{
					JSONObject outerObject = new JSONObject();
		    		JSONArray outerArray = new JSONArray();
					for(int i=0; i<paramValues.length; i++){
						JSONObject innerObject = new JSONObject();
						String paramValue = paramValues[i];
						if(paramValue.length() == 0 || "".equals(paramValue)){
							continue;
						}else{
							innerObject.put(paramName, paramValue);
							outerArray.put(innerObject);
						}
	                }
					outerObject.put("list", outerArray);
					requestMap.put(paramName, outerObject.toString());
				}
			}
    	}catch(RuntimeException e){
    		logger.error("makeRequestEgovMap Exception"); 
    	}
    	return requestMap;
    }
    
 
    /**
     * 16진 문자열을 byte 배열로 변환한다.
     * 
     * @param hex
     * @return
     */
    public static byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() % 2 != 0) { return new byte[] {}; }
 
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            byte value = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            bytes[(int) Math.floor(i / 2)] = value;
        }
        return bytes;
    }
    
    public static String byteArrayToHex(byte[] ba) {
		if (ba == null || ba.length == 0) {
			return null;
		}
		StringBuffer sb = new StringBuffer(ba.length * 2);

		for (int x = 0; x < ba.length; x++) {
			String hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
			sb.append(hexNumber.substring(hexNumber.length() - 2));
		}
		return sb.toString();
	}
    
    public static String stringNullCheck(String input) {
    	if(input == null || "".equals(input.trim())) {
    		input = "";
    	}
    	
    	return input;
    }
    
    /**
     * null Check 및 html convert
     * 
     * @param planText
     * @return 
     * @
     */
    public String convertHtml(HttpServletRequest request, String pkey) {
    	String rtnValue = "";
    	rtnValue 		= ServletRequestUtils.getStringParameter(request, pkey, "");
    	rtnValue 		= rtnValue.replace("&quot;","\'");
    	
    	return rtnValue;
    }
    
    /**
     * 배열로 넘어오는 파라미터  null Check 및 html convert
     * 
     * @param planText
     * @return 
     * @
     */
    public String[] convertHtmlArray(HttpServletRequest request, String pkey) {
    	String rtnValues[] = ServletRequestUtils.getStringParameters(request, pkey);

    	if(rtnValues == null || rtnValues.length == 0) {
    		rtnValues = null;
    	}else {
    		for(int i=0; i<rtnValues.length; i++) {
	    		rtnValues[i] = rtnValues[i].replace("&quot;","\'");
	    	}
    	}
    	
    	return rtnValues;
    }

	/**
	 * JsonArray 형태의 String을 파라미터를 Egovmap List 형태로 변경
	 * @param request
	 * @param pkey
	 * @return
	 * @
	 */
	public List<EgovMap> convertHtmlList(HttpServletRequest request, String pkey){
    	List<EgovMap> rtnList = new ArrayList<>();
    	String jsString = convertHtml(request, pkey);
    	JSONArray jsonArray = new JSONArray(jsString);

    	for(int i=0; i<jsonArray.length(); i++){
    		JSONObject jsonObject = jsonArray.getJSONObject(i);
		    Iterator<String> iterator = jsonObject.keys();
		    EgovMap egovMap = new EgovMap();

		    while(iterator.hasNext()){
		    	String jsonKey = iterator.next();
		    	String jsonVal = (String)jsonObject.get(jsonKey);
		    	egovMap.put(jsonKey, jsonVal);
		    }

		    rtnList.add(egovMap);
	    }

    	return rtnList;
    }
    
    public static String clearXSSMinimum(String value) {
		if (value == null || value.trim().equals("")) {
			return "";
		}

		String returnValue = value;

		returnValue = returnValue.replaceAll("&", "&amp;");
		returnValue = returnValue.replaceAll("<", "&lt;");
		returnValue = returnValue.replaceAll(">", "&gt;");
		returnValue = returnValue.replaceAll("\"", "&#34;");
		returnValue = returnValue.replaceAll("\'", "&#39;");
		returnValue = returnValue.replaceAll("\\.", "&#46;");
		returnValue = returnValue.replaceAll("%2E", "&#46;");
		returnValue = returnValue.replaceAll("%2F", "&#47;");
		return returnValue;
	}

	public static String clearXSSMaximum(String value) {
		String returnValue = value;
		returnValue = clearXSSMinimum(returnValue);

		returnValue = returnValue.replaceAll("%00", null);

		returnValue = returnValue.replaceAll("%", "&#37;");

		// \\. => .

		returnValue = returnValue.replaceAll("\\.\\./", ""); // ../
		returnValue = returnValue.replaceAll("\\.\\.\\\\", ""); // ..\
		returnValue = returnValue.replaceAll("\\./", ""); // ./
		returnValue = returnValue.replaceAll("%2F", "");

		return returnValue;
	}

	public static String filePathBlackList(String value) {
		String returnValue = value;
		if (returnValue == null || returnValue.trim().equals("")) {
			return "";
		}

		returnValue = returnValue.replaceAll("\\.\\.", "");

		return returnValue;
	}

	/**
	 * 행안부 보안취약점 점검 조치 방안.
	 *
	 * @param value
	 * @return
	 */
	public static String filePathReplaceAll(String value) {
		String returnValue = value;
		if (returnValue == null || returnValue.trim().equals("")) {
			return "";
		}

		returnValue = returnValue.replaceAll("/", "");
//		returnValue = returnValue.replaceAll("\\", "");
		returnValue = returnValue.replaceAll("\\.\\.", ""); // ..
		returnValue = returnValue.replaceAll("&", "");

		return returnValue;
	}
	
	public static String fileInjectPathReplaceAll(String value) {
		String returnValue = value;
		if (returnValue == null || returnValue.trim().equals("")) {
			return "";
		}

		
		returnValue = returnValue.replaceAll("/", "");
		returnValue = returnValue.replaceAll("\\..", ""); // ..
		returnValue = returnValue.replaceAll("\\\\", "");// \
		returnValue = returnValue.replaceAll("&", "");

		return returnValue;
	}

	public static String filePathWhiteList(String value) {
		return value;
	}
	
	/**
	 * 오늘 날짜 문자열 취득.
	 * ex) 20090101
	 * @return
	 */
	public static String getFileId() {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
		
		String lsDate = format.format(new Date());
		
		StringBuffer sb = new StringBuffer();
		sb.append(lsDate);
		
		//Random r = new Random();
		SecureRandom r = new SecureRandom();
		
		for (int i = 1; i <= 6; i++) {
			String ch = "" + ((int) (Math.random() * 9));
			r.setSeed(System.currentTimeMillis());
			//String ch = "" + (r.nextInt(10));
			sb.append(ch);
		}
		
		return sb.toString();
	}

	public static String getCrtfcNumber(){
		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		String crtfcNumber = String.format("%06d", (r.nextInt(1000000)));

		return crtfcNumber;
	}
	
	/**
	 * 천단위 , 문자열으로 변환
	 * ex) 20090101
	 * @return
	 */
	public static String getNumFormat(String numStr) {
		long lNumber = 0;
		
		if(!"".equals(numStr)) {
			lNumber = new Long(numStr).longValue();
		}
		
		DecimalFormat df = new DecimalFormat("#,###");
		return df.format(lNumber);
	}
	
	/**
	 * 개인정보접속 로그 쿼리 취득
	 * @return
	 */
	public static String getSQL(EgovComAbstractDAO dao, String sqlId, EgovMap pEgovMap) {
		String sql = dao.getSqlSession().getConfiguration().getMappedStatement(sqlId).getBoundSql(pEgovMap).getSql();
		List<ParameterMapping> parameterMappings = dao.getSqlSession().getConfiguration().getMappedStatement(sqlId).getBoundSql(pEgovMap).getParameterMappings(); 
		for (ParameterMapping parameterMapping : parameterMappings) { 
			String param = String.valueOf(pEgovMap.get(parameterMapping.getProperty()));
			sql = sql.replaceFirst("\\?", "'" + param + "'");
		}
		return sql;
	}
	
	/**
	 * 개인정보 접속 로그 쿼리 취득 ( foreach 사용 쿼리는 null 값이 나와서 새로 만듬)
	 * @return
	 */
	public static String getForeachSQL(EgovComAbstractDAO dao, String sqlId ,EgovMap pEgovMap) {
		BoundSql boundSql =dao.getSqlSession().getConfiguration().getMappedStatement("Ntcnq.selectNtcnqReqstManageList").getBoundSql(pEgovMap);
		String sql = boundSql.getSql();
		
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		
		for (ParameterMapping parameterMapping : parameterMappings) {
			 String propKey = parameterMapping.getProperty(); // <foreach>인 경우 propKey가 "__frch_%아이템명%_반복횟수"
		     String value =  String.valueOf(pEgovMap.get(propKey));
		       
		       if(boundSql.hasAdditionalParameter(propKey)) {
		    	   value = String.valueOf(boundSql.getAdditionalParameter(propKey));
		       }
		       
		       sql = sql.replaceFirst("\\?", "'" + value + "'");
		}
		
		return sql;
	}
	
	/**
	 * 세션 풀린 경우 안내 페이지
	 * @return
	 */	
	public String checkSession(HttpSession session){
//		if((LoginVO)session.getAttribute("USER") == null) {
//			return "/common/nologin";
//		}
		return null;
	}

	
	/**
	 * client IP 조회
	 */
	public static String getClientIP(HttpServletRequest request) {
	    String ip = request.getHeader("X-Forwarded-For");

	    if (ip == null) {
	        ip = request.getHeader("Proxy-Client-IP");
	    }
	    
	    if (ip == null) {
	        ip = request.getHeader("WL-Proxy-Client-IP");
	    }
	    
	    if (ip == null) {
	        ip = request.getHeader("HTTP_CLIENT_IP");
	    }
	    
	    if (ip == null) {
	        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
	    }
	    
	    if (ip == null) {
	        ip = request.getRemoteAddr();
	    }

	    return ip;
	}
	
	public static boolean isEmpty(Object obj) {
        if(obj == null) { return true; }
        if((obj instanceof String) && (((String)obj).trim().length() == 0)) { return true; } 
        if(obj instanceof Map) { return ((Map<?, ?>)obj).isEmpty(); }
        if(obj instanceof List) { return ((List<?>)obj).isEmpty(); }
        if(obj instanceof Object[]) { return (((Object[])obj).length == 0); } 

        return false;
    }
	
	/**
	 * 시스템 로그를 출력한다.
	 * @param obj Object
	 */
	public static void debug(Class<?> clazz, Object obj) {
		if(obj instanceof java.sql.SQLException){
			((java.sql.SQLException) obj).printStackTrace();
			logger.error("SQLException: " + ((SQLException)obj).getMessage());
			((SQLException)obj).printStackTrace();
		}else if(obj instanceof java.io.FileNotFoundException){
			((java.io.FileNotFoundException) obj).printStackTrace();
			logger.error("FileNotFoundException: " + ((FileNotFoundException)obj).getMessage());
			((FileNotFoundException)obj).printStackTrace();
		}else if(obj instanceof java.io.IOException){
			((java.io.IOException) obj).printStackTrace();
			logger.error("IOException: " + ((IOException)obj).getMessage());
			((IOException)obj).printStackTrace();
		}else if(obj instanceof java.lang.NumberFormatException){
			((java.lang.NumberFormatException) obj).printStackTrace();
			logger.error("NumberFormatException: " + ((NumberFormatException)obj).getMessage());
			((NumberFormatException)obj).printStackTrace();
		}
		// eGov 관련 
		 else if(obj instanceof org.springframework.web.bind.ServletRequestBindingException){
			 ((org.springframework.web.bind.ServletRequestBindingException) obj).printStackTrace();
			logger.error("ServletRequestBindingException: " + ((ServletRequestBindingException)obj).getMessage());
			((ServletRequestBindingException)obj).printStackTrace();
		}else if(obj instanceof org.springframework.dao.DataAccessException){
			((org.springframework.dao.DataAccessException) obj).printStackTrace();
			logger.error("DataAccessException: " + ((DataAccessException)obj).getMessage());
			((DataAccessException)obj).printStackTrace();
		}else if(obj instanceof org.springframework.web.bind.ServletRequestBindingException){
			((org.springframework.web.bind.ServletRequestBindingException) obj).printStackTrace();
			logger.error("ServletRequestBindingException: " + ((ServletRequestBindingException)obj).getMessage());
			((ServletRequestBindingException)obj).printStackTrace();
		}else if(obj instanceof org.egovframe.rte.fdl.cmmn.exception.FdlException){
			((org.egovframe.rte.fdl.cmmn.exception.FdlException) obj).printStackTrace();
			logger.error("FdlException: " + ((FdlException)obj).getMessage());
			((FdlException)obj).printStackTrace();
		// RSA 암호화 관련
		}else if(obj instanceof java.io.UnsupportedEncodingException){
			((java.io.UnsupportedEncodingException) obj).printStackTrace();
			logger.error("UnsupportedEncodingException: " + ((UnsupportedEncodingException)obj).getMessage());
			((UnsupportedEncodingException)obj).printStackTrace();
		}else if(obj instanceof java.security.NoSuchAlgorithmException){
			((java.security.NoSuchAlgorithmException) obj).printStackTrace();
			logger.error("NoSuchAlgorithmException: " + ((NoSuchAlgorithmException)obj).getMessage());
			((NoSuchAlgorithmException)obj).printStackTrace();
		}else if(obj instanceof java.security.spec.InvalidKeySpecException){
			((java.security.spec.InvalidKeySpecException) obj).printStackTrace();
			logger.error("InvalidKeySpecException: " + ((InvalidKeySpecException)obj).getMessage());
			((InvalidKeySpecException)obj).printStackTrace();
		}else if(obj instanceof javax.crypto.IllegalBlockSizeException){
			((javax.crypto.IllegalBlockSizeException) obj).printStackTrace();
			logger.error("IllegalBlockSizeException: " + ((IllegalBlockSizeException)obj).getMessage());
			((IllegalBlockSizeException)obj).printStackTrace();
		// SFTP
		/*}else if(obj instanceof com.jcraft.jsch.JSchException){
			((com.jcraft.jsch.JSchException) obj).printStackTrace();
			logger.error("JSchException: " + ((JSchException)obj).getMessage());
			((JSchException)obj).printStackTrace();
		}else if(obj instanceof com.jcraft.jsch.SftpException){
			((com.jcraft.jsch.SftpException) obj).printStackTrace();
			logger.error("SftpException: " + ((SftpException)obj).getMessage());
			((SftpException)obj).printStackTrace();20210901*/
		// 기타 나머지
		}else if(obj instanceof java.text.ParseException){
			((java.text.ParseException) obj).printStackTrace();
			logger.error("ParseException: " + ((ParseException)obj).getMessage());
			((ParseException)obj).printStackTrace();
		}else if(obj instanceof java.lang.IllegalArgumentException){
			((java.lang.IllegalArgumentException) obj).printStackTrace();
			logger.error("IllegalArgumentException: " + ((IllegalArgumentException)obj).getMessage());
			((IllegalArgumentException)obj).printStackTrace();
		}else if(obj instanceof java.net.MalformedURLException){
			((java.net.MalformedURLException) obj).printStackTrace();
			logger.error("MalformedURLException: " + ((MalformedURLException)obj).getMessage());
			((MalformedURLException)obj).printStackTrace();
		}else if(obj instanceof java.io.UnsupportedEncodingException){
			((java.io.UnsupportedEncodingException) obj).printStackTrace();
			logger.error("UnsupportedEncodingException: " + ((UnsupportedEncodingException)obj).getMessage());
			((UnsupportedEncodingException)obj).printStackTrace();
		}else if(obj instanceof java.lang.RuntimeException){
			((java.lang.RuntimeException) obj).printStackTrace();
			logger.error("RuntimeException: " + ((RuntimeException)obj).getMessage());
			((RuntimeException)obj).printStackTrace();
		}else if(obj instanceof java.lang.Throwable){
			((java.lang.Throwable) obj).printStackTrace();
			logger.error("Throwable: " + ((Throwable)obj).getMessage());
			((Throwable)obj).printStackTrace();
		}else if (obj instanceof java.lang.Exception) {
			((java.lang.Exception) obj).printStackTrace();
			logger.error("Exception: " + ((Exception)obj).getMessage());
			((Exception)obj).printStackTrace();
		}
	}
	
	/**
	 * 개인정보 접속 로그 정보 담기(IP, 페이지주소)
	 * @return
	 */	
	public void putIndvdlinfo(HttpSession session, EgovMap pEgovMap) {
		if(session != null) {
			pEgovMap.put("userIp", 		(String)session.getAttribute("userIp"));
			pEgovMap.put("progrmCours", (String)session.getAttribute("progrmCours"));
			pEgovMap.put("loginDt", 	(String)session.getAttribute("loginDt"));
		}
	}
    
    public static String getIpAddr(HttpServletRequest request){	
		String ipAddr = request.getHeader("X-Forwarded-For");	
		
		if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) { 
			ipAddr = request.getHeader("Proxy-Client-IP");
		}
		
		if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) { 
			ipAddr = request.getHeader("WL-Proxy-Client-IP");
		}
		
		if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) { 
			ipAddr = request.getHeader("HTTP_CLIENT_IP");
		}
		
		if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) { 
			ipAddr = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		
		if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
			ipAddr = request.getRemoteAddr();
		}
		
		return ipAddr;
	}
    
    public String convertToWebMP4TodayFolder(String inputPath, String originalFileName, int quality) {
        String video_quality 	= "";
        String mkDir			= ""; //변환 파일 저장 소

        // 1 ~ 4 범위, 1 = 저화질, 2 = 일반화질, 3 = 고화질
        if(quality == 1) {
            video_quality = "30";
            mkDir = "LOW";
        } else if(quality == 2) {
            video_quality = "24";
            mkDir = "GEN";
        } else if(quality == 3) {
            video_quality = "21";
            mkDir = "HIGH";
        } else {
            video_quality = "24";
            mkDir = "GEN";
        }

        // 인코딩 폴더 경로
        String targetDirPath = inputPath + "/" + mkDir + "/";

        // 디렉토리 없으면 생성
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }       

        // 확장자 제거하고 .mp4 붙이기
        String outputFileName = originalFileName + ".mp4";

        // 최종 출력 경로
        Path outputPath = Paths.get(targetDirPath + outputFileName);

        // FFmpeg 명령어
        String[] command = {
        	FFMPEG_PATH + "/ffmpeg",
            "-i", inputPath + "/" + originalFileName,
            "-c:v", "libx264",
            "-preset", "fast",
            "-crf", video_quality,
            "-c:a", "aac",
            "-b:a", "128k",
            "-y",
            outputPath.toString()
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 변환 로그 출력
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            int exitCode = process.waitFor();
            if (exitCode == 0) {
  
            } else {
                throw new RuntimeException("변환 실패! 종료 코드: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("변환 중 오류 발생");
        }

        // 경로 반환 (백슬래시 → 슬래시 변환)
        return outputPath.toString().replace("\\", "/");
    }
    
    /**
     * Convert Json String into Object
     * 
     * @param obj
     * @return
     */
    public static Object fromJsonStr(String jsonStr) {
        Object natural = null;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());
        Gson gson = gsonBuilder.create();
        natural = gson.fromJson(jsonStr, Object.class);

        return natural;
    }
    
    public String changeFfmpeg(String inputPath, String originalFileName) throws IllegalArgumentException, InputFormatException, EncoderException, IOException {
    	FFmpeg ffmpeg = new FFmpeg(FFMPEG_PATH + "/ffmpeg");  // ffmpeg 리눅스 경로
		FFprobe ffprobe = new FFprobe(FFMPEG_PATH + "/ffprobe");  // ffprobe 리눅스 경로
		String returnData = "0";
		// 디렉토리 없으면 생성
		File targetDir = new File(FILE_REAL_PATH+"/VOD");
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		FFmpegBuilder builder = new FFmpegBuilder().setInput(inputPath+"/"+originalFileName) // 파일경로
				.overrideOutputFiles(true) // 오버라이드
				.addOutput(FILE_REAL_PATH+"/VOD/" +originalFileName) // 저장 경로 ( mov to mp4 )
				.setFormat("mp4") // 포맷 ( 확장자 )
				.setVideoCodec("libx264") // 비디오 코덱
				.disableSubtitle() // 서브타이틀 제거
				.setAudioChannels(2) // 오디오 채널 ( 1 : 모노 , 2 : 스테레오 )
				.setVideoResolution(1280, 720) // 동영상 해상도
				.setVideoBitRate(1464800) // 비디오 비트레이트
				.setVideoFrameRate(30) //프레임
				.setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // ffmpeg 빌더 실행 허용
				.done();
		
		FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
		executor.createJob(builder).run();
		FFmpegProbeResult probeResult = ffprobe.probe(FILE_REAL_PATH+"/VOD/" +originalFileName); // 동영상 경로
		FFmpegFormat format = probeResult.getFormat();			
		double second = format.duration; // 초단위 			returnData = second + "";
		returnData = second + "";
		createThumbnail(inputPath, originalFileName);
		return returnData;
	}

	public void createThumbnail(String inputPath, String originalFileName) throws IOException{
		FFmpeg ffmpeg = new FFmpeg(FFMPEG_PATH + "/ffmpeg");  // ffmpeg 리눅스 경로
		FFprobe ffprobe = new FFprobe(FFMPEG_PATH + "/ffprobe");  // ffprobe 리눅스 경로
		
		// 디렉토리 없으면 생성
		File targetDir = new File(FILE_REAL_PATH+"/"+ "THUM");
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		FFmpegBuilder builder = new FFmpegBuilder()
				.overrideOutputFiles(true) // 오버라이드 여부
				.setInput(FILE_REAL_PATH +"/VOD/"+originalFileName) // 썸네일 생성대상 파일
				.addExtraArgs("-ss", "00:00:10") // 썸네일 추출 시작점
				.addExtraArgs("-vsync", "vfr")
				.addOutput(FILE_REAL_PATH +"/"+ "THUM" + "/" +originalFileName + ".png") // 썸네일 파일의 Path
				.setFrames(1) // 프레임 수
				.done();
		FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
		executor.createJob(builder).run();
	}

	// XSS 필터
	public static String xssDecoding(String str) {
		String result = "";
		
		result = str.replace("&amp;quot;", "\"");
		
		return result;
	}
	
	/**
     * 자릿수(digit) 만큼 랜덤한 숫자를 반환 받습니다.
     *
     * @param length 자릿수
     * @return 랜덤한 숫자
     */
    public static int generateRandomNum(int length) {
        SecureRandom secureRandom = new SecureRandom();
        int upperLimit = (int) Math.pow(10, length);
        return secureRandom.nextInt(upperLimit);
    }
    
	// VO -> Map
    public static Map<String, Object> convertToMap(Object obj) throws IllegalArgumentException, IllegalAccessException {
    	if (Objects.isNull(obj)) {
        	return Collections.emptyMap();
    	}
        Map<String, Object> convertMap = new HashMap<>();
 
        Field[] fields = obj.getClass().getDeclaredFields();
 
        for (Field field : fields) {
        	field.setAccessible(true);
            convertMap.put(field.getName(), field.get(obj));
        }
        return convertMap;
    }
    
    /**
	 * 인증번호용 난수 생성
	 * @return 난수값
	 */
	public String makeRandom() {
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		sb.append(random.nextInt(10));
		sb.append(random.nextInt(10));
		sb.append(random.nextInt(10));
		sb.append(random.nextInt(10));
		sb.append(random.nextInt(10));
		sb.append(random.nextInt(10));
		return sb.toString();
	}
    

}
