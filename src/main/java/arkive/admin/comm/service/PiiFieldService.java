package arkive.admin.comm.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 개인정보 필드 암호화.
 *
 * <p>주소록·명함은 연락처(TEL_NO)와 이메일(EMAIL)을, 사용자는 휴대전화(PHONE)를 암호화한다.
 * 이름·부서·회사명은 목록 검색에 필요해 평문으로 두고, 사용자 EMAIL은 아이디 찾기·비밀번호
 * 재설정의 조회 조건이라 평문을 유지한다. 대상 프로젝트에서 개인정보 분류가 달라지면
 * {@link #ENCRYPTED_FIELDS}와 사용자 전용 메서드를 조정한다.</p>
 *
 * <p>키가 없으면 새 저장을 거부하고, 기존 평문은 그대로 읽는다. 이미 암호화된 값은
 * 다시 암호화하지 않는다.</p>
 */
@Service("piiFieldService")
public class PiiFieldService {

	private static final String PREFIX = "enc:v1:";
	private static final String USER_PHONE_FIELD = "phone";
	private static final List<String> ENCRYPTED_FIELDS = List.of("telNo", "email");

	@Resource(name = "fieldCryptoService")
	private FieldCryptoService fieldCryptoService;

	public void encrypt(EgovMap row) {
		if (row == null) {
			return;
		}
		for (String field : ENCRYPTED_FIELDS) {
			encryptField(row, field);
		}
	}

	/** 사용자 휴대전화만 암호화한다. EMAIL은 조회 조건이라 건드리지 않는다. */
	public void encryptUserPhone(EgovMap row) {
		encryptField(row, USER_PHONE_FIELD);
	}

	/** 사용자 휴대전화만 복호화한다. 평문(접두사 없음)은 그대로 둔다. */
	public void decryptUserPhone(EgovMap row) {
		if (row == null) {
			return;
		}
		Object value = row.get(USER_PHONE_FIELD);
		if (value == null) {
			return;
		}
		String text = String.valueOf(value);
		if (text.startsWith(PREFIX)) {
			row.put(USER_PHONE_FIELD, fieldCryptoService.decrypt(text));
		}
	}

	private void encryptField(EgovMap row, String field) {
		if (row == null) {
			return;
		}
		Object value = row.get(field);
		if (value == null) {
			return;
		}
		String text = String.valueOf(value);
		if (text.isEmpty() || text.startsWith(PREFIX)) {
			return;
		}
		row.put(field, fieldCryptoService.encrypt(text));
	}

	public void decrypt(EgovMap row) {
		if (row == null) {
			return;
		}
		for (String field : ENCRYPTED_FIELDS) {
			Object value = row.get(field);
			if (value == null) {
				continue;
			}
			String text = String.valueOf(value);
			if (text.startsWith(PREFIX)) {
				row.put(field, fieldCryptoService.decrypt(text));
			}
		}
	}

	public void decrypt(List<EgovMap> rows) {
		if (rows == null) {
			return;
		}
		for (EgovMap row : rows) {
			decrypt(row);
		}
	}
}
