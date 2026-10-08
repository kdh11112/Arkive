package arkive.admin.auth.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.auth.service.AuthService;
import arkive.admin.board.service.BoardService;
import arkive.admin.comm.web.CommUtil;
import arkive.admin.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 외부 API 제공. 화면(세션)과 분리하고 JWT로 인증한다.
 * 인증 없이 공개하지 않는다. 버전 변경 시 하위호환을 먼저 정한다.
 */
@Tag(name = "외부 API", description = "JWT 발급 + 읽기 API")
@Controller
@RequestMapping("/api")
public class ApiController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final int MAX_FAIL = 5;

	@Resource(name = "authService")
	private AuthService authService;

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "faqService")
	private FaqService faqService;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Resource
	private JwtUtil jwtUtil;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "API 로그인 (JWT 발급)")
	@RequestMapping(value = "/login.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> apiLogin(HttpServletRequest request) {
		// 발급 역할: id·pw 검사는 세션 로그인과 같다(잠금·실패 5회). 통과하면 createToken으로 토큰을 준다.
		Map<String, Object> result = new HashMap<>();
		try {
			// 입력값 꺼내기. convertHtml은 XSS 정제된 문자열을 돌려준다.
			String userId = cmmUtil.convertHtml(request, "userId");
			String password = cmmUtil.convertHtml(request, "password");
			// 빈값이면 DB까지 안 가고 끝낸다.
			if (userId == null || userId.trim().isEmpty() || password == null || password.isEmpty()) {
				throw new IllegalArgumentException("아이디와 비밀번호를 입력하세요.");
			}
			// 사용중 계정만 조회한다. 없거나 잠겼으면 아래에서 나눈다.
			EgovMap user = authService.selectLoginUser(userId.trim());
			if (user == null || !"N".equals(String.valueOf(user.get("lockYn")))) {
				// 잠긴 계정은 잠김 메시지를 준다. 없는 계정은 일반 메시지로 뭘 틀렸는지 숨긴다.
				if (user != null) {
					throw new IllegalStateException("잠긴 계정입니다. 관리자에게 문의하세요.");
				}
				throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
			}
			// bcrypt 비교. 평문을 DB값과 직접 비교하지 않는다.
			if (!passwordEncoder.matches(password, String.valueOf(user.get("password")))) {
				// 틀리면 실패 1 올리고 다시 읽는다.
				authService.addFailCount(userId.trim());
				EgovMap retry = authService.selectLoginUser(userId.trim());
				int failed = MAX_FAIL;
				try {
					failed = Integer.parseInt(String.valueOf(retry.get("failedCnt")));
				} catch (NumberFormatException e) {
					// 숫자 읽기 실패는 잠금 쪽으로 처리한다.
					failed = MAX_FAIL;
				}
				// 5회 찼으면 잠그고 잠김 메시지를 준다.
				if (failed >= MAX_FAIL) {
					authService.lockUser(userId.trim());
					throw new IllegalStateException("5회 실패로 잠겼습니다. 관리자에게 문의하세요.");
				}
				throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
			}
			// 성공하면 실패 기록을 지운다.
			authService.resetFailCount(userId.trim());
			String userNm = String.valueOf(user.get("userNm"));
			result.put("success", true);
			// 세션 대신 토큰을 준다. 이후 요청은 이 토큰으로 온다.
			result.put("token", jwtUtil.createToken(userId.trim(), userNm));
			result.put("userNm", userNm);
		} catch (Exception e) {
			// 실패는 로그만 남기고 메시지만 돌려준다. 스택은 안 준다.
			logger.error("apiLogin failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "API 공지사항 목록 (최신 10건)")
	@RequestMapping(value = "/noticeList.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> noticeList() {
		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("boardType", "CK");
			param.put("searchKeyword", "");
			param.put("firstIndex", 0);
			param.put("recordCountPerPage", 10);
			List<EgovMap> rows = boardService.selectBoardList(param);
			result.put("success", true);
			result.put("list", rows);
			result.put("total", boardService.selectBoardListTotCnt(param));
		} catch (Exception e) {
			logger.error("noticeList failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "API 공지사항 상세")
	@RequestMapping(value = "/noticeDetail.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> noticeDetail(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			String boardId = cmmUtil.convertHtml(request, "boardId");
			EgovMap detail = boardService.selectBoardDetail(boardId);
			if (detail == null) {
				throw new IllegalArgumentException("글이 없습니다.");
			}
			result.put("success", true);
			result.put("detail", detail);
		} catch (Exception e) {
			logger.error("noticeDetail failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "API FAQ 목록 (최신 10건)")
	@RequestMapping(value = "/faqList.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Map<String, Object> faqList() {		Map<String, Object> result = new HashMap<>();
		try {
			EgovMap param = new EgovMap();
			param.put("searchKeyword", "");
			param.put("firstIndex", 0);
			param.put("recordCountPerPage", 10);
			List<EgovMap> rows = faqService.selectFaqList(param);
			result.put("success", true);
			result.put("list", rows);
			result.put("total", faqService.selectFaqListTotCnt(param));
		} catch (Exception e) {
			logger.error("faqList failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}
}
