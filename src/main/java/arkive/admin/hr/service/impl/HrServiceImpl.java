package arkive.admin.hr.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.hr.service.HrService;
import jakarta.annotation.Resource;

/**
 * HR 세트 서비스 구현. 유형·날짜·금액은 샘플 정책으로 검증하며,
 * 대상 프로젝트의 공통코드·정산 규칙에 맞게 교체한다.
 */
@Service("hrService")
public class HrServiceImpl extends EgovAbstractServiceImpl implements HrService {

	private static final Set<String> ANNIV_TYPES = Set.of("BIRTH", "WEDDING", "HIRE", "ETC");
	private static final Set<String> EVENT_TYPES = Set.of("WEDDING", "FUNERAL", "BIRTH", "ETC");
	private static final Set<String> REWARD_TYPES = Set.of("EXCELLENT", "TEAMWORK", "ETC");
	private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

	@Resource(name = "hrMapper")
	private HrMapper hrMapper;

	@Override
	public List<EgovMap> selectAnniversaryList(EgovMap egovMap) throws Exception {
		return hrMapper.selectAnniversaryList(egovMap);
	}

	@Override
	public int selectAnniversaryListTotCnt(EgovMap egovMap) throws Exception {
		return hrMapper.selectAnniversaryListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectAnniversaryDetail(String annivId) throws Exception {
		return isBlank(annivId) ? null : hrMapper.selectAnniversaryDetail(annivId.trim());
	}

	@Override
	@Transactional
	public void insertAnniversary(EgovMap egovMap) throws Exception {
		validateAnniversary(egovMap);
		egovMap.put("annivId", "AN" + CommUtil.getFileId());
		hrMapper.insertAnniversary(egovMap);
	}

	@Override
	@Transactional
	public void updateAnniversary(EgovMap egovMap) throws Exception {
		validateAnniversary(egovMap);
		String annivId = trim(egovMap.get("annivId"));
		requireExisting(selectAnniversaryDetail(annivId), "기념일");
		egovMap.put("annivId", annivId);
		if (hrMapper.updateAnniversary(egovMap) == 0) {
			throw new IllegalStateException("기념일이 없습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteAnniversary(String annivId) throws Exception {
		requireExisting(selectAnniversaryDetail(annivId), "기념일");
		hrMapper.deleteAnniversary(annivId.trim());
	}

	@Override
	public List<EgovMap> selectEventList(EgovMap egovMap) throws Exception {
		return hrMapper.selectEventList(egovMap);
	}

	@Override
	public int selectEventListTotCnt(EgovMap egovMap) throws Exception {
		return hrMapper.selectEventListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectEventDetail(String eventId) throws Exception {
		return isBlank(eventId) ? null : hrMapper.selectEventDetail(eventId.trim());
	}

	@Override
	@Transactional
	public void insertEvent(EgovMap egovMap) throws Exception {
		validateEvent(egovMap);
		egovMap.put("eventId", "EV" + CommUtil.getFileId());
		hrMapper.insertEvent(egovMap);
	}

	@Override
	@Transactional
	public void updateEvent(EgovMap egovMap) throws Exception {
		validateEvent(egovMap);
		String eventId = trim(egovMap.get("eventId"));
		requireExisting(selectEventDetail(eventId), "경조사");
		egovMap.put("eventId", eventId);
		if (hrMapper.updateEvent(egovMap) == 0) {
			throw new IllegalStateException("경조사가 없습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteEvent(String eventId) throws Exception {
		requireExisting(selectEventDetail(eventId), "경조사");
		hrMapper.deleteEvent(eventId.trim());
	}

	@Override
	public List<EgovMap> selectRewardList(EgovMap egovMap) throws Exception {
		return hrMapper.selectRewardList(egovMap);
	}

	@Override
	public int selectRewardListTotCnt(EgovMap egovMap) throws Exception {
		return hrMapper.selectRewardListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectRewardDetail(String rewardId) throws Exception {
		return isBlank(rewardId) ? null : hrMapper.selectRewardDetail(rewardId.trim());
	}

	@Override
	@Transactional
	public void insertReward(EgovMap egovMap) throws Exception {
		validateReward(egovMap);
		egovMap.put("rewardId", "RW" + CommUtil.getFileId());
		hrMapper.insertReward(egovMap);
	}

	@Override
	@Transactional
	public void updateReward(EgovMap egovMap) throws Exception {
		validateReward(egovMap);
		String rewardId = trim(egovMap.get("rewardId"));
		requireExisting(selectRewardDetail(rewardId), "포상");
		egovMap.put("rewardId", rewardId);
		if (hrMapper.updateReward(egovMap) == 0) {
			throw new IllegalStateException("포상이 없습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteReward(String rewardId) throws Exception {
		requireExisting(selectRewardDetail(rewardId), "포상");
		hrMapper.deleteReward(rewardId.trim());
	}

	@Override
	public List<EgovMap> selectActiveUserList() throws Exception {
		return hrMapper.selectActiveUserList();
	}

	@Override
	public List<EgovMap> selectDutyListByUserName(EgovMap egovMap) throws Exception {
		return hrMapper.selectDutyListByUserName(egovMap);
	}

	@Override
	public EgovMap selectUser(String userId) throws Exception {
		return isBlank(userId) ? null : hrMapper.selectUser(userId.trim());
	}

	private void validateAnniversary(EgovMap egovMap) throws Exception {
		String userId = requireActiveUser(egovMap.get("userId"));
		String annivType = trim(egovMap.get("annivType"));
		String annivDe = trim(egovMap.get("annivDe"));
		String title = trim(egovMap.get("title"));
		String remark = trim(egovMap.get("remark"));
		String useYn = trim(egovMap.get("useYn"));

		if (!ANNIV_TYPES.contains(annivType)) {
			throw new IllegalArgumentException("기념일 구분이 올바르지 않습니다.");
		}
		if (!isMonthDay(annivDe)) {
			throw new IllegalArgumentException("기념일은 MM-DD 형식으로 입력하세요.");
		}
		if (title.isEmpty() || title.length() > 200) {
			throw new IllegalArgumentException("기념일명은 1~200자로 입력하세요.");
		}
		if (remark.length() > 500) {
			throw new IllegalArgumentException("비고는 500자 이내로 입력하세요.");
		}
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			throw new IllegalArgumentException("사용여부가 올바르지 않습니다.");
		}
		egovMap.put("userId", userId);
		egovMap.put("annivType", annivType);
		egovMap.put("annivDe", annivDe);
		egovMap.put("title", title);
		egovMap.put("remark", remark);
		egovMap.put("useYn", useYn);
	}

	private void validateEvent(EgovMap egovMap) throws Exception {
		String userId = requireActiveUser(egovMap.get("userId"));
		String eventType = trim(egovMap.get("eventType"));
		String eventDe = trim(egovMap.get("eventDe"));
		String title = trim(egovMap.get("title"));
		BigDecimal amount = parseAmount(egovMap.get("amount"));
		String remark = trim(egovMap.get("remark"));

		if (!EVENT_TYPES.contains(eventType)) {
			throw new IllegalArgumentException("경조사 구분이 올바르지 않습니다.");
		}
		if (!isDate(eventDe)) {
			throw new IllegalArgumentException("경조사일은 YYYY-MM-DD 형식으로 입력하세요.");
		}
		if (title.isEmpty() || title.length() > 200) {
			throw new IllegalArgumentException("경조사명은 1~200자로 입력하세요.");
		}
		if (remark.length() > 500) {
			throw new IllegalArgumentException("비고는 500자 이내로 입력하세요.");
		}
		egovMap.put("userId", userId);
		egovMap.put("eventType", eventType);
		egovMap.put("eventDe", eventDe);
		egovMap.put("title", title);
		egovMap.put("amount", amount);
		egovMap.put("remark", remark);
	}

	private void validateReward(EgovMap egovMap) throws Exception {
		String userId = requireActiveUser(egovMap.get("userId"));
		String rewardType = trim(egovMap.get("rewardType"));
		String rewardDe = trim(egovMap.get("rewardDe"));
		String title = trim(egovMap.get("title"));
		BigDecimal amount = parseAmount(egovMap.get("amount"));
		String rewarder = trim(egovMap.get("rewarder"));
		String remark = trim(egovMap.get("remark"));

		if (!REWARD_TYPES.contains(rewardType)) {
			throw new IllegalArgumentException("포상 구분이 올바르지 않습니다.");
		}
		if (!isDate(rewardDe)) {
			throw new IllegalArgumentException("포상일은 YYYY-MM-DD 형식으로 입력하세요.");
		}
		if (title.isEmpty() || title.length() > 200) {
			throw new IllegalArgumentException("포상명은 1~200자로 입력하세요.");
		}
		if (rewarder.length() > 100) {
			throw new IllegalArgumentException("포상자는 100자 이내로 입력하세요.");
		}
		if (remark.length() > 500) {
			throw new IllegalArgumentException("비고는 500자 이내로 입력하세요.");
		}
		egovMap.put("userId", userId);
		egovMap.put("rewardType", rewardType);
		egovMap.put("rewardDe", rewardDe);
		egovMap.put("title", title);
		egovMap.put("amount", amount);
		egovMap.put("rewarder", rewarder);
		egovMap.put("remark", remark);
	}

	private String requireActiveUser(Object value) throws Exception {
		String userId = trim(value);
		if (userId.isEmpty()) {
			throw new IllegalArgumentException("대상 사용자를 선택하세요.");
		}
		if (hrMapper.selectUser(userId) == null) {
			throw new IllegalArgumentException("사용중인 사용자를 선택하세요.");
		}
		return userId;
	}

	private BigDecimal parseAmount(Object value) {
		String text = trim(value);
		if (text.isEmpty()) {
			return BigDecimal.ZERO;
		}
		BigDecimal amount;
		try {
			amount = new BigDecimal(text);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("금액을 숫자로 입력하세요.");
		}
		if (amount.signum() < 0 || amount.compareTo(new BigDecimal("999999999999")) > 0) {
			throw new IllegalArgumentException("금액은 0 이상 999,999,999,999 이하로 입력하세요.");
		}
		if (amount.stripTrailingZeros().scale() > 0) {
			throw new IllegalArgumentException("금액은 원 단위 정수로 입력하세요.");
		}
		return amount;
	}

	private boolean isMonthDay(String value) {
		if (value == null || !value.matches("^[0-9]{2}-[0-9]{2}$")) {
			return false;
		}
		try {
			MonthDay.parse(value, MONTH_DAY);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private boolean isDate(String value) {
		if (value == null || !value.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
			return false;
		}
		try {
			LocalDate.parse(value);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private void requireExisting(EgovMap detail, String label) {
		if (detail == null) {
			throw new IllegalArgumentException(label + "이(가) 없습니다.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
