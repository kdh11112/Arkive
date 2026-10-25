package arkive.admin.comm.web;

import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 목록 화면의 개인정보를 표시 직전에 마스킹한다.
 *
 * <p>모델의 List 안에 있는 EgovMap만 대상으로 한다. 수정 화면이 쓰는 detail/answer 같은
 * 단건 모델은 건드리지 않아 원본 편집이 가능하다. 엑셀은 각 다운로드 코드에서 별도로 마스킹한다.</p>
 */
public class MaskingInterceptor implements HandlerInterceptor {

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		if (modelAndView == null || modelAndView.getViewName() == null
				|| modelAndView.getViewName().startsWith("redirect:")) {
			return;
		}
		for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
			if (!(entry.getValue() instanceof List<?> list)) {
				continue;
			}
			for (Object item : list) {
				if (item instanceof EgovMap map) {
					maskMap(map);
				}
			}
		}
	}

	private void maskMap(EgovMap map) {
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof String text) {
				map.put(key, MaskingUtil.mask(String.valueOf(key), text));
			}
		}
	}
}
