package arkive.admin.comm.web;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FormBasedFileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormBasedFileUtil.class);

	/**
	 * Resource close 처리.
	 * @param resources
	 */
	public static void close(Closeable  ... resources) {
		for (Closeable resource : resources) {
			if (resource != null) {
				try {
					resource.close();
				} catch (IOException ignore) {//KISA 보안약점 조치 (2018-10-29, 윤창원)
					LOGGER.debug("Close Error");
				} catch (Exception ignore) {
					LOGGER.debug("Close Error");
				}
			}
		}
	}
}
