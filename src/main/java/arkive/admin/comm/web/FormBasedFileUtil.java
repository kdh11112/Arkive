package arkive.admin.comm.web;

import java.io.Closeable;
import java.io.IOException;

import org.apache.log4j.Logger;


public class FormBasedFileUtil {

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
					Logger.getLogger("FormBasedFileUtil").debug("Close Error");
				} catch (Exception ignore) {
					Logger.getLogger("FormBasedFileUtil").debug("Close Error");
				}
			}
		}
	}
}
