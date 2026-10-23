package arkive.admin.comm.web;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 로그인 사용자용 읽기 전용 사이트맵.
 *
 * <p>메뉴 목록과 권한 필터는 {@code MenuInterceptor}가 만든 {@code menuList}를
 * 그대로 사용한다. 이 Controller는 화면에 넘기기만 하므로 메뉴 구조·권한 규칙을
 * 중복 구현하지 않는다.</p>
 */
@Controller
public class SitemapController {

	@RequestMapping(value = "/sitemap", method = RequestMethod.GET)
	public String sitemap(HttpServletRequest request, ModelMap model) {
		Object menuList = request.getAttribute("menuList");
		if (menuList instanceof List<?> menus) {
			model.put("menuList", menus);
		} else {
			model.put("menuList", Collections.emptyList());
		}
		return "comm/sitemap";
	}
}
