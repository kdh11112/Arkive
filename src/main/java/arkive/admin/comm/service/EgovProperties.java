package arkive.admin.comm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

public class EgovProperties{
	
	//프로퍼티값 로드시 에러발생하면 반환되는 에러문자열 
	public static final String ERR_CODE =" EXCEPTION OCCURRED";
	public static final String ERR_CODE_FNFE =" EXCEPTION(FNFE) OCCURRED";
	public static final String ERR_CODE_IOE =" EXCEPTION(IOE) OCCURRED";
	
	//파일구분자
    static final char FILE_SEPARATOR     = File.separatorChar;

	//프로퍼티 파일의 물리적 위치
    /*public static final String GLOBALS_PROPERTIES_FILE 
    = System.getProperty("user.home") + System.getProperty("file.separator") + "egovProps"
    + System.getProperty("file.separator") + "globals.properties";*/
    
    //public static final String RELATIVE_PATH_PREFIX = EgovProperties.class.getResource("").getPath()
    /***********************************************************
     * 	<-- 2020.10.07 취약점 점검관련 수정
    
    public static final String RELATIVE_PATH_PREFIX = EgovProperties.class.getResource("").getPath().replaceAll("%20", " ")
    + System.getProperty("file.separator") + ".." + System.getProperty("file.separator")
    + ".." + System.getProperty("file.separator") + ".." + System.getProperty("file.separator")
    + ".." + System.getProperty("file.separator");
    
    public static final String GLOBALS_PROPERTIES_FILE 
    = RELATIVE_PATH_PREFIX + "egovProps" + System.getProperty("file.separator") + "globals.properties";
    
     ***********************************************************/
    public static String RELATIVE_PATH_PREFIX = "";
    public static String GLOBALS_PROPERTIES_FILE = "";
    
    public static String GET_PATH = EgovProperties.class.getResource("").getPath();
     
	/**
	 * 인자로 주어진 문자열을 Key값으로 하는 프로퍼티 값을 반환한다(Globals.java 전용)
	 * @param keyName String
	 * @return String
	 */
	public static String getProperty(String keyName){
		/***********************************************************
	     * 	2020.10.07 취약점 점검관련 수정 -->
	     ***********************************************************/
		RELATIVE_PATH_PREFIX = "";
		if(GET_PATH != null) {
			RELATIVE_PATH_PREFIX = GET_PATH.replaceAll("%20", " ");
		}
		RELATIVE_PATH_PREFIX =  RELATIVE_PATH_PREFIX
		+ System.getProperty("file.separator") + ".." + System.getProperty("file.separator")
	    + ".." + System.getProperty("file.separator") + ".." + System.getProperty("file.separator")
	    + ".." + System.getProperty("file.separator");
		
		if(RELATIVE_PATH_PREFIX !=  null && !RELATIVE_PATH_PREFIX.equals("")) {
			GLOBALS_PROPERTIES_FILE =  RELATIVE_PATH_PREFIX + "egovProps" + System.getProperty("file.separator") + "globals.properties";
		}
		String value = ERR_CODE;
		value="99";
		debug(GLOBALS_PROPERTIES_FILE + " : " + keyName);
		FileInputStream fis = null;
		try{
			Properties props = new Properties();
			if(GLOBALS_PROPERTIES_FILE != null && !GLOBALS_PROPERTIES_FILE.equals("")) {
				fis  = new FileInputStream(GLOBALS_PROPERTIES_FILE.replace("/", File.separator));
			}
			
			if(fis.read() > 0 && props!= null) {
				props.load(new java.io.BufferedInputStream(fis));
			}
			value = props.getProperty(keyName);
			if(value != null) {
				value = value.trim();
			}
		}catch(FileNotFoundException fne){
			debug("FileNotFoundException 에러");
		}catch(IOException ioe){
			debug("IOException 에러");
		}finally{
			try {
				if (fis != null) fis.close();
			} catch (IOException ex) {
				debug("IOException 에러");
			}
			
		}
		return value;
	}
	
	
	/**
	 * 주어진 프로파일의 내용을 파싱하여 (key-value) 형태의 구조체 배열을 반환한다.
	 * @param property String
	 * @return ArrayList
	 */
	public static ArrayList loadPropertyFile(String property){

		// key - value 형태로 된 배열 결과
		ArrayList keyList = new ArrayList();
		
		String src = property.replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
		FileInputStream fis = null;
		try
		{   
			
			File srcFile = new File(src);
			if (srcFile.exists()) {
				
				java.util.Properties props = new java.util.Properties();
				fis  = new FileInputStream(src);
				props.load(new java.io.BufferedInputStream(fis));
				
				int i = 0;
				Enumeration plist = props.propertyNames();
				if (plist != null) {
					while (plist.hasMoreElements()) {
						Map map = new HashMap();
						String key = (String)plist.nextElement();
						map.put(key, props.getProperty(key));
						keyList.add(map);
					}
				}
			}
		} catch (IOException ex){
			debug("IOException 에러");
		} finally {
			try {
				if (fis != null) fis.close();
			} catch (IOException ex) {
				debug("IOException 에러");
			}
		}
		
		return keyList;
	}

	/**
	 * 시스템 로그를 출력한다.
	 * @param obj Object
	 */
	private static void debug(Object obj) {
		if (obj instanceof java.lang.Exception) {
			//((Exception)obj).printStackTrace();
			Logger.getLogger(EgovProperties.class).debug("IGNORED: " + ((Exception)obj).getMessage());
		}
	}
}