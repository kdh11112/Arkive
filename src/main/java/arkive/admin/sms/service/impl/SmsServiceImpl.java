package arkive.admin.sms.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import arkive.admin.auth.service.impl.PermissionMapper;
import arkive.admin.board.service.BoardService;
import arkive.admin.comm.service.PiiFieldService;
import arkive.admin.comm.web.PhoneNumberUtil;
import arkive.admin.sms.service.SmsSendResult;
import arkive.admin.sms.service.SmsSender;
import arkive.admin.sms.service.SmsService;
import jakarta.annotation.Resource;

/**
 * 게시판 SMS 알림 구현.
 *
 * <p>게시글 단위로 발송을 한 번만 선점하고(BOARD.SMS_SENT_YN), 해당 메뉴 조회 권한이 있으면서
 * SMS 수신에 동의한 사용자에게만 보낸다. 제목·본문에 개인정보가 들어갈 수 있으므로 SMS에는
 * 제목만 짧게 보내고 상세는 사이트에서 확인하게 한다.</p>
 */
@Service("smsService")
public class SmsServiceImpl implements SmsService {

	private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

	@Resource(name = "smsSender")
	private SmsSender smsSender;

	@Resource(name = "boardService")
	private BoardService boardService;

	@Resource(name = "permissionMapper")
	private PermissionMapper permissionMapper;

	@Resource(name = "piiFieldService")
	private PiiFieldService piiFieldService;

	@Value("${arkive.sms.title-prefix:[Arkive] }")
	private String titlePrefix;

	@Override
	public boolean isReady() {
		return smsSender.isReady();
	}

	@Override
	public int sendBoardNotification(String boardId) throws Exception {
		if (!smsSender.isReady()) {
			return 0;
		}
		EgovMap detail = boardService.selectBoardDetail(boardId);
		if (detail == null) {
			return 0;
		}
		String boardType = String.valueOf(detail.get("boardType"));
		String menuId = menuId(boardType);
		if (menuId == null) {
			return 0;
		}
		// 같은 글을 여러 요청·재시작에서 중복 발송하지 않도록 먼저 선점한다.
		if (boardService.claimBoardSms(boardId) == 0) {
			return 0;
		}
		String title = String.valueOf(detail.get("title"));
		String message = (titlePrefix == null ? "" : titlePrefix) + title;
		List<EgovMap> recipients = permissionMapper.selectBoardSmsRecipients(menuId);
		int sent = 0;
		int failed = 0;
		for (EgovMap recipient : recipients) {
			piiFieldService.decryptUserPhone(recipient);
			String phone = PhoneNumberUtil.normalizeMobile(String.valueOf(recipient.get("phone")));
			if (phone == null) {
				failed++;
				continue;
			}
			SmsSendResult result = smsSender.send(phone, title, message);
			if (result.success()) {
				sent++;
			} else {
				failed++;
			}
		}
		if (failed > 0) {
			logger.warn("board SMS partially failed. boardId={}, sent={}, failed={}", boardId, sent, failed);
		}
		return sent;
	}

	private String menuId(String boardType) {
		if ("CK".equals(boardType)) {
			return "ME02030100";
		}
		if ("TOAST".equals(boardType)) {
			return "ME02030200";
		}
		return null;
	}
}
