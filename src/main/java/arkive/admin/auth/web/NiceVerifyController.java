package arkive.admin.auth.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import NiceID.Check.CPClient;
import arkive.admin.comm.service.NiceInfoVO;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.comm.web.NiceCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * NICE 본인인증(CheckPlus). 회원가입·비밀번호 찾기의 전제용 검증 모듈이다.
 * 사이트 코드·비밀번호 실계약 전에는 시작이 차단된다. 테스트키와 운영키를 분리한다.
 */
@Tag(name = "본인인증", description = "NICE CheckPlus 본인확인")
@Controller
@RequestMapping("/login/nice")
public class NiceVerifyController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String SESSION_VERIFIED = "NICE_VERIFIED";

	@Resource
	private NiceCheck niceCheck;

	@Resource
	private Environment env;

	private CommUtil cmmUtil = new CommUtil();

	private String prop(String key) {
		String value = env.getProperty(key);
		return value == null ? "" : value.trim();
	}

	@Operation(summary = "본인확인 시작 (NICE 요청 생성)")
	@RequestMapping(value = "/start", method = { RequestMethod.GET, RequestMethod.POST })
	public String start(HttpServletRequest request, ModelMap model) throws Exception {
		String siteCode = prop("Globals.nice.siteCode");
		String sitePassword = prop("Globals.nice.sitePassword");
		if (siteCode.isEmpty() || sitePassword.isEmpty()) {
			throw new IllegalStateException("NICE 미계약 상태입니다. 사이트 코드를 먼저 등록하세요.");
		}
		HttpSession session = request.getSession();
		String reqSeq = "REQ" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
				+ (int) (Math.random() * 9000 + 1000);
		session.setAttribute("REQ_SEQ", reqSeq);
		// CheckPlus v1 요청 평문 규격이다. 계약 버전에 맞는지 NICE 가이드로 확인한다.
		String returnUrl = prop("Globals.nice.returnUrl");
		String errorUrl = prop("Globals.nice.errorUrl");
		StringBuilder plain = new StringBuilder();
		plain.append("7:REQ_SEQ").append(reqSeq.getBytes("EUC-KR").length).append(":").append(reqSeq);
		plain.append("8:SITECODE").append(siteCode.getBytes("EUC-KR").length).append(":").append(siteCode);
		plain.append("9:AUTH_TYPE0:");
		plain.append("7:RTN_URL").append(returnUrl.getBytes("EUC-KR").length).append(":").append(returnUrl);
		plain.append("7:ERR_URL").append(errorUrl.getBytes("EUC-KR").length).append(":").append(errorUrl);
		CPClient client = new CPClient();
		if (client.fnEncode(siteCode, sitePassword, plain.toString()) != 0) {
			throw new IllegalStateException("NICE 요청 생성에 실패했습니다.");
		}
		model.put("sEncodeData", client.getCipherData());
		return "login/niceStart";
	}

	@Operation(summary = "본인확인 콜백 (복호화·세션 저장)")
	@RequestMapping(value = "/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(HttpServletRequest request, HttpSession session, ModelMap model) {
		try {
			NiceInfoVO niceInfo = niceCheck.niceCheck(request, session, new NiceInfoVO());
			if (niceInfo.getNiceMessage() != null && !niceInfo.getNiceMessage().isEmpty()) {
				model.put("message", niceInfo.getNiceMessage());
				return "login/niceResult";
			}
			EgovMap verified = new EgovMap();
			verified.put("name", niceInfo.getNiceNm());
			verified.put("birthdate", niceInfo.getBirthdate());
			verified.put("phone", niceInfo.getPhone());
			verified.put("di", niceInfo.getMblDn());
			verified.put("verifiedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			session.setAttribute(SESSION_VERIFIED, verified);
			model.put("verified", verified);
		} catch (Exception e) {
			logger.error("niceCallback failed", e);
			model.put("message", e.getMessage());
		}
		return "login/niceResult";
	}

	@Operation(summary = "본인확인 상태 조회")
	@RequestMapping(value = "/status.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> status(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		Object verified = request.getSession().getAttribute(SESSION_VERIFIED);
		result.put("success", true);
		result.put("verified", verified != null);
		if (verified instanceof EgovMap) {
			EgovMap map = (EgovMap) verified;
			result.put("name", map.get("name"));
			result.put("birthdate", map.get("birthdate"));
			result.put("phone", map.get("phone"));
		}
		return result;
	}

	@Operation(summary = "본인확인 초기화 (재인증용)")
	@RequestMapping(value = "/clear.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> clear(HttpServletRequest request) {
		request.getSession().removeAttribute(SESSION_VERIFIED);
		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		return result;
	}
}
