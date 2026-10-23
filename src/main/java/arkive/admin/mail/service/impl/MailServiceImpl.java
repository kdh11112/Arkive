package arkive.admin.mail.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import arkive.admin.auth.service.impl.AuthMapper;
import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.board.service.impl.BoardMapper;
import arkive.admin.mail.service.MailService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;

/**
 * SMTP 메일 발송 구현.
 *
 * <p>운영 안전장치:</p>
 * <ul>
 *   <li>arkive.mail.enabled=false(기본)이면 발송하지 않는다.</li>
 *   <li>회원가입 환영/게시글 알림은 APP_USER.MAIL_YN='Y'인 사용자에게만 보낸다.</li>
 *   <li>비밀번호 재설정은 보안 목적의 거래성 메일이므로 수신 동의와 무관하게 이메일이 있으면 보낸다.</li>
 *   <li>게시글 알림은 BOARD.MAIL_SENT_YN을 원자적으로 선점해 게시글당 1회만 발송한다.</li>
 * </ul>
 */
@Service("mailService")
public class MailServiceImpl implements MailService {

	private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	@Resource
	private JavaMailSender mailSender;

	@Resource(name = "authMapper")
	private AuthMapper authMapper;

	@Resource(name = "boardMapper")
	private BoardMapper boardMapper;

	@Resource(name = "permissionMapper")
	private PermissionMapper permissionMapper;

	@Value("${arkive.mail.enabled:false}")
	private boolean enabled;

	@Value("${arkive.mail.from:}")
	private String from;

	@Value("${arkive.mail.base-url:http://localhost:8080}")
	private String baseUrl;

	@Override
	public void sendWelcome(String userId) {
		if (!isReady()) {
			return;
		}
		try {
			EgovMap user = authMapper.selectUserDetail(userId);
			if (!canMail(user, true)) {
				return;
			}
			String email = String.valueOf(user.get("email")).trim();
			String userNm = String.valueOf(user.get("userNm"));
			String html = "<div style=\"font-family:Arial,'Malgun Gothic',sans-serif;line-height:1.6;\">"
					+ "<h2>Arkive 가입을 환영합니다</h2>"
					+ "<p>" + HtmlUtils.htmlEscape(userNm) + "님, 가입해 주셔서 감사합니다.</p>"
					+ "<p><a href=\"" + loginUrl() + "\">Arkive 로그인</a></p>"
					+ "<p style=\"color:#666;font-size:12px;\">이 메일은 회원가입 환영 안내입니다.</p>"
					+ "</div>";
			sendHtml(email, "[Arkive] 가입을 환영합니다", html);
		} catch (Exception e) {
			logger.error("welcome mail failed. userId={}", userId, e);
		}
	}

	@Override
	public void sendPasswordReset(String userId, String rawToken) {
		if (!isReady()) {
			return;
		}
		try {
			EgovMap user = authMapper.selectUserDetail(userId);
			// 비밀번호 재설정은 거래성 메일이므로 마케팅 수신 동의(MAIL_YN)를 요구하지 않는다.
			if (!canMail(user, false)) {
				return;
			}
			String email = String.valueOf(user.get("email")).trim();
			String userNm = String.valueOf(user.get("userNm"));
			String resetUrl = baseUrl() + "/login/resetPw?token="
					+ URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
			String html = "<div style=\"font-family:Arial,'Malgun Gothic',sans-serif;line-height:1.6;\">"
					+ "<h2>비밀번호 재설정 안내</h2>"
					+ "<p>" + HtmlUtils.htmlEscape(userNm) + "님, 아래 링크에서 새 비밀번호를 설정하세요.</p>"
					+ "<p><a href=\"" + resetUrl + "\">비밀번호 재설정</a></p>"
					+ "<p>링크는 30분 동안 한 번만 사용할 수 있습니다.</p>"
					+ "<p style=\"color:#666;font-size:12px;\">본인이 요청하지 않았다면 이 메일을 무시하세요. "
					+ "비밀번호는 변경되지 않습니다.</p>"
					+ "</div>";
			sendHtml(email, "[Arkive] 비밀번호 재설정 안내", html);
		} catch (Exception e) {
			logger.error("password reset mail failed. userId={}", userId, e);
		}
	}

	@Override
	public void sendBoardNotification(String boardId) {
		if (!isReady()) {
			return;
		}
		try {
			EgovMap board = boardMapper.selectBoardDetail(boardId);
			if (board == null) {
				return;
			}
			boolean notice = "Y".equals(String.valueOf(board.get("noticeYn")));
			boolean important = "Y".equals(String.valueOf(board.get("importantYn")));
			if (!notice && !important) {
				return;
			}
			// MAIL_SENT_YN='N'인 요청만 성공한다. 같은 게시글의 재수정·재고정에도 1회만 발송된다.
			if (boardMapper.claimBoardMail(boardId) == 0) {
				return;
			}
			boolean toast = "TOAST".equals(String.valueOf(board.get("boardType")));
			String menuId = toast ? "ME02030200" : "ME02030100";
			List<EgovMap> recipients = permissionMapper.selectBoardMailRecipients(menuId);
			if (recipients == null || recipients.isEmpty()) {
				logger.info("board mail skipped: no consented recipients. boardId={}", boardId);
				return;
			}
			String title = safeHeader(String.valueOf(board.get("title")));
			String label = notice ? "공지" : "중요글";
			String subject = "[Arkive " + label + "] " + title;
			String detailUrl = baseUrl() + (toast ? "/user/board/archiveDetail" : "/user/board/noticeDetail")
					+ "?boardId=" + URLEncoder.encode(boardId, StandardCharsets.UTF_8);
			String html = "<div style=\"font-family:Arial,'Malgun Gothic',sans-serif;line-height:1.6;\">"
					+ "<h2>새 " + label + "가 등록되었습니다</h2>"
					+ "<p><strong>" + HtmlUtils.htmlEscape(title) + "</strong></p>"
					+ "<p><a href=\"" + detailUrl + "\">게시글 확인</a></p>"
					+ "<p style=\"color:#666;font-size:12px;\">메일 수신에 동의한 사용자에게만 발송됩니다.</p>"
					+ "</div>";
			int sent = 0;
			for (EgovMap recipient : recipients) {
				String email = recipient.get("email") == null ? "" : String.valueOf(recipient.get("email")).trim();
				if (!isValidEmail(email)) {
					continue;
				}
				if (sendHtml(email, subject, html)) {
					sent++;
				}
			}
			logger.info("board mail finished. boardId={}, recipients={}, sent={}", boardId,
					recipients.size(), sent);
		} catch (Exception e) {
			logger.error("board mail failed. boardId={}", boardId, e);
		}
	}

	private boolean isReady() {
		if (!enabled) {
			logger.debug("mail disabled (arkive.mail.enabled=false)");
			return false;
		}
		if (from == null || from.trim().isEmpty()) {
			logger.error("mail enabled but arkive.mail.from is empty");
			return false;
		}
		if (mailSender == null) {
			logger.error("mail enabled but JavaMailSender is not configured");
			return false;
		}
		return true;
	}

	private boolean canMail(EgovMap user, boolean requireConsent) {
		if (user == null || !"Y".equals(String.valueOf(user.get("useYn")))) {
			return false;
		}
		if (requireConsent && !"Y".equals(String.valueOf(user.get("mailYn")))) {
			return false;
		}
		String email = user.get("email") == null ? "" : String.valueOf(user.get("email")).trim();
		return isValidEmail(email);
	}

	private boolean sendHtml(String to, String subject, String html) {
		if (!isValidEmail(to)) {
			return false;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(from.trim());
			helper.setTo(to);
			helper.setSubject(safeHeader(subject));
			helper.setText(html, true);
			mailSender.send(message);
			return true;
		} catch (Exception e) {
			// 수신자 전체 주소는 로그에 남기지 않는다.
			logger.error("mail send failed. to={}", maskEmail(to), e);
			return false;
		}
	}

	private String loginUrl() {
		return baseUrl() + "/login/login";
	}

	private String baseUrl() {
		String value = baseUrl == null ? "" : baseUrl.trim();
		while (value.endsWith("/")) {
			value = value.substring(0, value.length() - 1);
		}
		return value.isEmpty() ? "http://localhost:8080" : value;
	}

	private String safeHeader(String value) {
		String safe = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
		return safe.length() > 200 ? safe.substring(0, 200) : safe;
	}

	private boolean isValidEmail(String email) {
		return email != null && email.length() <= 100 && EMAIL_PATTERN.matcher(email).matches();
	}

	private String maskEmail(String email) {
		if (email == null || email.isEmpty()) {
			return "***";
		}
		int at = email.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		return email.substring(0, 2) + "***" + email.substring(at);
	}
}
