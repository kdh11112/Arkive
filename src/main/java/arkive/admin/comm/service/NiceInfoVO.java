package arkive.admin.comm.service;

import lombok.Data;

@Data
public class NiceInfoVO {
	private String niceSuccUrl;
	private String niceFailUrl;
	private String niceMessage;
	private String niceNm;
	private String mblDn;
	private String phone;
	private String gender;
	private String birthdate;
	private String nationalinfo;
	private String fcltUsrTyGb;
	private String searchUserId
	;
	//2022.04.29 개인식별정보가 없을 시 예외처리
	private String searchUserNm;	//사용자명
	private String searchMbtlnum;	//휴대전화번호
	//개인식별정보가 없을 시 예외처리
	
	private String goUrl;			//성공시 이동하는 url
	private String sSiteCode;		//nice 코드
	private String sSitePassword;	//nice 패스워드
}
