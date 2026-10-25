package arkive.admin.chat.web;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import arkive.admin.auth.web.LoginController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 채팅 화면. 실제 메시지 교환은 /ws/chat WebSocket이 담당한다.
 */
@Tag(name = "채팅", description = "WebSocket 채팅")
@Controller
@RequestMapping("/chat")
public class ChatController {

	@Operation(summary = "채팅 화면")
	@RequestMapping(value = "/chat", method = { RequestMethod.GET, RequestMethod.POST })
	public String chat(HttpServletRequest request, ModelMap model) {
		HttpSession session = request.getSession(false);
		Object rawUser = session == null ? null : session.getAttribute(LoginController.SESSION_KEY);
		if (!(rawUser instanceof EgovMap loginUser)) {
			return "redirect:/login/login";
		}
		model.put("loginUserId", String.valueOf(loginUser.get("userId")));
		model.put("loginUserNm", String.valueOf(loginUser.get("userNm")));
		return "chat/chat";
	}
}
