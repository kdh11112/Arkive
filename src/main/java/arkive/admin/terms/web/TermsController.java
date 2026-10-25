package arkive.admin.terms.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.audit.service.AuditEvent;
import arkive.admin.auth.web.LoginController;
import arkive.admin.terms.service.TermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 공개 약관 열람과 로그인 후 필수 약관 동의.
 *
 * <p>약관 본문은 DB의 버전별 행을 그대로 보여준다. 실제 문구는 운영 담당자가 교체한다.</p>
 */
@Tag(name = "약관", description = "버전별 약관 열람·동의")
@Controller
public class TermsController {

	@Resource(name = "termsService")
	private TermsService termsService;

	@Resource
	private ApplicationEventPublisher auditEventPublisher;

	@Operation(summary = "현재 약관 목록 (공개)")
	@RequestMapping(value = "/terms", method = RequestMethod.GET)
	public String terms(ModelMap model) throws Exception {
		model.put("termsList", termsService.selectCurrentTerms());
		return "terms/termsList";
	}

	@Operation(summary = "필수 약관 동의 화면")
	@RequestMapping(value = "/login/termsAgree", method = RequestMethod.GET)
	public String termsAgree(HttpServletRequest request, ModelMap model) throws Exception {
		EgovMap user = loginUser(request);
		if (user == null) {
			return "redirect:/login/login";
		}
		List<EgovMap> pending = termsService.selectPendingRequiredTerms(String.valueOf(user.get("userId")));
		if (pending.isEmpty()) {
			return "redirect:/";
		}
		model.put("termsList", pending);
		model.put("pendingCount", pending.size());
		return "login/termsAgree";
	}

	@Operation(summary = "필수 약관 동의 저장")
	@RequestMapping(value = "/login/termsAgree.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> termsAgreeAction(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap user = loginUser(request);
			if (user == null) {
				throw new IllegalStateException("로그인이 필요합니다.");
			}
			String userId = String.valueOf(user.get("userId"));
			List<EgovMap> pending = termsService.selectPendingRequiredTerms(userId);
			List<String> selected = selectedIds(request);
			for (EgovMap term : pending) {
				String termsId = String.valueOf(term.get("termsId"));
				if (!selected.contains(termsId)) {
					throw new IllegalArgumentException("필수 약관에 모두 동의해야 합니다.");
				}
			}
			termsService.saveConsents(userId, selected);
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute(TermsInterceptor.PENDING_TERMS);
			}
			auditEventPublisher.publishEvent(AuditEvent.of("TERMS_CONSENT", userId,
					String.valueOf(user.get("userNm")), null, "TERMS", String.join(",", selected),
					"/login/termsAgree.json", "POST", request.getRemoteAddr(), "SUCCESS", null));
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private List<String> selectedIds(HttpServletRequest request) {
		String[] values = request.getParameterValues("termsIds");
		if (values == null) {
			return new ArrayList<>();
		}
		return Arrays.stream(values).filter(value -> value != null && !value.isBlank())
				.map(String::trim).distinct().toList();
	}

	private EgovMap loginUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object value = session.getAttribute(LoginController.SESSION_KEY);
		return value instanceof EgovMap map ? map : null;
	}
}
