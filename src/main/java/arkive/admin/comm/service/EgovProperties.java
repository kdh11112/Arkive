package arkive.admin.comm.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * application.properties를 단일 설정 소스로 사용하는 프로퍼티 유틸리티.
 * <p>
 * application.properties는 UTF-8, BOM 없음으로 저장한다.
 * Properties.load(InputStream)은 ISO-8859-1로 해석하므로 반드시 UTF-8 Reader를 사용한다.
 * egovProps/globals.properties 기반 조회는 더 이상 사용하지 않는다.
 * </p>
 */
public class EgovProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovProperties.class);

	//파일구분자
	static final char FILE_SEPARATOR = File.separatorChar;

	private static final String APPLICATION_PROPERTIES = "application.properties";

	private static final Properties PROPERTIES = loadApplicationProperties();

	private static Properties loadApplicationProperties() {
		Properties props = new Properties();
		try (InputStream input = EgovProperties.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)) {
			if (input == null) {
				LOGGER.warn("{} was not found on the classpath.", APPLICATION_PROPERTIES);
			} else {
				props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			}
		} catch (IOException ioe) {
			LOGGER.error("Failed to load {}.", APPLICATION_PROPERTIES, ioe);
		}
		return props;
	}

	/**
	 * 인자로 주어진 문자열을 Key값으로 하는 프로퍼티 값을 반환한다.
	 * @param keyName String
	 * @return String 프로퍼티가 없으면 null
	 */
	public static String getProperty(String keyName) {
		if (keyName == null) {
			return null;
		}
		String value = PROPERTIES.getProperty(keyName);
		return value != null ? value.trim() : null;
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

				Properties props = new Properties();
				fis  = new FileInputStream(src);
				props.load(new BufferedInputStream(fis));

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
		if (obj instanceof Exception) {
			LOGGER.debug("IGNORED: " + ((Exception)obj).getMessage());
		}
	}
}
