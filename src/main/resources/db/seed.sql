-- Arkive 로컬 데모의 필수 초기값. schema.sql 실행 후 빈 DB에만 한 번 실행한다.
-- 기존 DB의 계정·비밀번호·메뉴·권한·동의 이력을 재시작 때 덮어쓰지 않는다.
-- 메뉴, 개발용 로그인, 역할·사용자 매핑·메뉴 권한, 샘플 ID 생성기와 약관 자리표시자만 포함한다.
-- 게시물·공통코드 업무값·회의실·연차·신청·로그·토큰·사용자 동의 이력은 업무 화면에서 만든다.
-- 다른 프로젝트에는 필요한 초기값만 가져가고 실제 계정·권한·약관은 담당자 승인값으로 교체한다.

-- 메뉴 ID·경로·상위 관계·순서는 기존 데모와 동일하다.
-- ME02010300은 Swagger, ME02010400은 사용자관리다.
INSERT INTO MENU (MENU_ID, MENU_NM, MENU_COURS, GRAD, UP_MENU_ID, ORDR, USE_YN, REGIST_DT, REGISTER, UPDT_DT, UPDUSR) VALUES
    ('ME00000000','아카이브 시스템',NULL,0,NULL,1,'Y',NULL,NULL,NULL,NULL),
    ('ME01000000','외부포털',NULL,1,'ME00000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02000000','내부포털',NULL,1,'ME00000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02010000','시스템관리','/system/menuList',2,'ME02000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02010100','메뉴관리','/system/menuList',3,'ME02010000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02010200','공통코드관리','/system/commCodeList',3,'ME02010000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02010300','API 문서(Swagger)','/swagger-ui.html',3,'ME02010000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02010400','사용자관리','/auth/userList',3,'ME02010000',4,'Y',NULL,NULL,NULL,NULL),
    ('ME02010500','권한관리','/auth/roleList',3,'ME02010000',5,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02010600','로그관리','/system/auditLogList',3,'ME02010000',6,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02010700','통계','/stats/statsList',3,'ME02010000',7,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02010800','배치관리','/batch/batchList',3,'ME02010000',8,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02020000','업로드','/system/multipartUpload',2,'ME02000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020100','멀티파트 업로드','/system/multipartUpload',3,'ME02020000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02020200','TUS 업로드','/system/tusUpload',3,'ME02020000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020300','드롭존 업로드','/system/dropzoneUpload',3,'ME02020000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02030000','게시판관리',NULL,2,'ME02000000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02030100','공지사항관리','/board/ckeditorList',3,'ME02030000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02030200','자료실관리','/board/toastList',3,'ME02030000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02030300','FAQ관리','/faq/faqList',3,'ME02030000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02030400','Q&A관리','/qna/qnaList',3,'ME02030000',4,'Y',NULL,NULL,NULL,NULL),
    ('ME02031200','온라인POLL','/poll/pollList',3,'ME02030000',5,'Y',NULL,NULL,NULL,NULL),
    ('ME02031500','상담관리','/counsel/counselList',3,'ME02030000',6,'Y',NULL,NULL,NULL,NULL),
    ('ME02031600','설문템플릿','/survey/templateList',3,'ME02030000',7,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02031700','설문관리','/survey/surveyList',3,'ME02030000',8,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02031800','설문참여','/survey/participateList',3,'ME02030000',9,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02031900','설문통계','/survey/stats',3,'ME02030000',10,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02032000','조회수 통계','/board/viewStats',3,'ME02030000',11,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02040000','화면관리',NULL,2,'ME02000000',4,'Y',NULL,NULL,NULL,NULL),
    ('ME02030600','팝업창관리','/popup/popupList',3,'ME02040000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02030700','배너관리','/banner/bannerList',3,'ME02040000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02040200','약도관리','/map/mapList',3,'ME02040000',3,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02050000','일정관리',NULL,2,'ME02000000',5,'Y',NULL,NULL,NULL,NULL),
    ('ME02030800','일정관리','/schedule/scheduleList',3,'ME02050000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02030900','공휴일관리','/schedule/holidayList',3,'ME02050000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02031300','당직관리','/duty/dutyList',3,'ME02050000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02050100','회의실관리','/meeting/roomList',3,'ME02050000',4,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02050200','회의실예약','/meeting/reserveList',3,'ME02050000',5,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02060000','주소·분류관리',NULL,2,'ME02000000',6,'Y',NULL,NULL,NULL,NULL),
    ('ME02031100','주소록관리','/address/addressList',3,'ME02060000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02031400','명함관리','/card/cardList',3,'ME02060000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02031000','카테고리관리','/category/tree',3,'ME02060000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02070000','보고관리',NULL,2,'ME02000000',7,'Y',NULL,NULL,NULL,NULL),
    ('ME02030500','메모보고','/memo/memoList',3,'ME02070000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02070100','약식결재','/approval/approvalList',3,'ME02070000',2,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02080000','쪽지관리',NULL,2,'ME02000000',8,'Y',NULL,NULL,NULL,NULL),
    ('ME02080100','받은쪽지','/message/inbox',3,'ME02080000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02080200','보낸쪽지','/message/sent',3,'ME02080000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02080300','쪽지쓰기','/message/write',3,'ME02080000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02090000','인사관리',NULL,2,'ME02000000',9,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090100','내 휴가','/leave/myLeave',3,'ME02090000',1,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090200','휴가관리','/leave/leaveList',3,'ME02090000',2,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090300','연차관리','/leave/balanceList',3,'ME02090000',3,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090400','기념일관리','/hr/anniversaryList',3,'ME02090000',4,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090500','경조사관리','/hr/eventList',3,'ME02090000',5,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090600','포상관리','/hr/rewardList',3,'ME02090000',6,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME02090700','개인연차','/hr/personalLeave',3,'ME02090000',7,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020A0000','강의관리',NULL,2,'ME02000000',10,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020A0100','강의관리','/lecture/lectureList',3,'ME020A0000',1,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020A0200','내 강의','/lecture/myLecture',3,'ME020A0000',2,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020A0300','수료현황','/lecture/completionList',3,'ME020A0000',3,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020B0000','커뮤니케이션',NULL,2,'ME02000000',11,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL),
    ('ME020B0100','채팅','/chat/chat',3,'ME020B0000',1,'Y',CURRENT_TIMESTAMP,NULL,NULL,NULL);

-- 로컬 개발용 admin / Admin1234! (bcrypt). 실제 서비스에는 이 계정을 복사하지 않는다.
-- 이메일·휴대전화·수신 동의는 비워 두며, 약관 동의는 로그인 후 사용자에게 받는다.
INSERT INTO APP_USER (USER_ID, PASSWORD, USER_NM, ADMIN_YN)
VALUES ('admin', '$2a$10$MvioV9e1Afy22kJ3sQ8rSuRDMxvOz1IVHOxmasa43HhbeZWjGl3Pq', '관리자', 'Y');

-- 시스템 역할 ID는 애플리케이션에서 참조하므로 유지한다.
INSERT INTO APP_ROLE (ROLE_ID, ROLE_NM, DESCRIPTION, USE_YN) VALUES
    ('ROLE_ADMIN','관리자','전체 메뉴 조회·쓰기','Y'),
    ('ROLE_USER','일반사용자','시스템관리 제외 콘텐츠 메뉴 조회·쓰기','Y');

INSERT INTO APP_USER_ROLE (USER_ID, ROLE_ID) VALUES ('admin', 'ROLE_ADMIN');

-- 관리자 역할은 전체 메뉴를 조회한다. 조회수 통계는 기존과 같이 읽기 전용이다.
INSERT INTO APP_ROLE_MENU (ROLE_ID, MENU_ID, READ_YN, WRITE_YN)
SELECT 'ROLE_ADMIN', MENU_ID, 'Y', CASE WHEN MENU_ID = 'ME02032000' THEN 'N' ELSE 'Y' END
FROM MENU WHERE USE_YN = 'Y' AND GRAD >= 2;

-- 일반 역할은 기존 허용 목록만 유지한다. 새 메뉴를 추가했다고 자동으로 권한을 주지 않는다.
-- 시스템관리·업로드·관리자 전용 HR/설문/강의/약도/통계 화면은 제외한다.
-- 강의 상위 메뉴는 조회만, 실제 수강 메뉴는 조회·쓰기를 허용한다.
INSERT INTO APP_ROLE_MENU (ROLE_ID, MENU_ID, READ_YN, WRITE_YN)
SELECT 'ROLE_USER', MENU_ID, 'Y', CASE WHEN MENU_ID = 'ME020A0000' THEN 'N' ELSE 'Y' END
FROM MENU
WHERE MENU_ID IN (
    'ME02030000', 'ME02030100', 'ME02030200', 'ME02030300', 'ME02030400', 'ME02031200', 'ME02031500',
    'ME02040000', 'ME02030600', 'ME02030700',
    'ME02050000', 'ME02030800', 'ME02030900', 'ME02031300',
    'ME02060000', 'ME02031100', 'ME02031400', 'ME02031000',
    'ME02070000', 'ME02030500',
    'ME02080000', 'ME02080100', 'ME02080200', 'ME02080300',
    'ME02090100', 'ME02050200', 'ME02070100', 'ME02031800',
    'ME020A0000', 'ME020A0200', 'ME020B0000', 'ME020B0100'
);

-- 샘플 레코드는 넣지 않지만 ID 생성기가 바로 등록할 수 있도록 시작값은 준비한다.
INSERT INTO IDS (TABLE_NAME, NEXT_ID) VALUES ('SAMPLE', 1);

-- 약관 기능·가입/로그인 흐름 확인용 기본 문구. 실제 서비스에 이식할 때는
-- 법무·개인정보 담당자가 검토해 새 VERSION을 등록한다.
-- 기존 사용자 동의 이력을 시드로 생성하거나 삭제하지 않는다.
INSERT INTO TERMS (TERMS_ID, TERMS_TYPE, VERSION, TITLE, CONTENT, REQUIRED_YN, EFFECTIVE_DT, USE_YN) VALUES
    ('T_SERVICE_1', 'SERVICE', '1.0', '서비스 이용약관',
     '제1조 (목적)' || CHAR(10) ||
     '이 약관은 Arkive 서비스의 이용과 관련하여 서비스 제공자와 이용자 간의 권리, 의무 및 책임사항을 규정합니다.' || CHAR(10) || CHAR(10) ||
     '제2조 (서비스의 제공)' || CHAR(10) ||
     '서비스는 업무 지원을 위한 게시판, 일정, 전자결재, 파일 관리, 통합검색 등의 기능을 제공합니다. 시스템 점검이나 장애로 일시 중단될 수 있습니다.' || CHAR(10) || CHAR(10) ||
     '제3조 (이용자의 의무)' || CHAR(10) ||
     '이용자는 관계 법령, 본 약관 및 서비스 안내를 준수해야 하며, 타인의 정보를 도용하거나 허위 정보를 등록해서는 안 됩니다.' || CHAR(10) || CHAR(10) ||
     '제4조 (책임의 제한)' || CHAR(10) ||
     '서비스는 천재지변, 시스템 장애 등 불가항력으로 인한 서비스 중단에 대해 책임을 지지 않습니다.' || CHAR(10) || CHAR(10) ||
     '제5조 (준거법)' || CHAR(10) ||
     '본 약관과 서비스 이용에 관한 분쟁은 대한민국 법령을 준거법으로 합니다.',
     'Y', CURRENT_DATE, 'Y'),
    ('T_PRIVACY_1', 'PRIVACY', '1.0', '개인정보 수집·이용 동의',
     '1. 수집하는 개인정보 항목' || CHAR(10) ||
     '- 필수: 아이디, 비밀번호, 이름, 이메일, 휴대전화번호, 부서/직위' || CHAR(10) ||
     '- 선택: 생년월일, 주소, 프로필 사진' || CHAR(10) ||
     '- 자동 수집: 접속 IP, 쿠키, 서비스 이용 기록, 접속 로그' || CHAR(10) || CHAR(10) ||
     '2. 수집 및 이용 목적' || CHAR(10) ||
     '회원 가입 및 본인 확인, 서비스 제공 및 계정 관리, 업무 연락 및 알림 발송, 서비스 이용 통계 및 품질 개선' || CHAR(10) || CHAR(10) ||
     '3. 보유 및 이용 기간' || CHAR(10) ||
     '회원 탈퇴 시까지 보유하며 탈퇴 후 즉시 파기합니다. 다만 관계 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관합니다.' || CHAR(10) || CHAR(10) ||
     '4. 동의 거부 권리' || CHAR(10) ||
     '이용자는 개인정보 수집·이용에 대한 동의를 거부할 수 있으나, 필수 항목에 대한 동의를 거부할 경우 서비스 이용이 제한될 수 있습니다.',
     'Y', CURRENT_DATE, 'Y'),
    ('T_MARKETING_1', 'MARKETING', '1.0', '마케팅 정보 수신 동의',
     '1. 수신 채널: 이메일, SMS, 쪽지, 푸시 알림' || CHAR(10) ||
     '2. 수신 내용: 신규 기능 안내, 업데이트 공지, 이벤트 및 프로모션, 설문 참여 요청' || CHAR(10) ||
     '3. 수신 주기: 월 1~4회 (긴급 공지는 별도 발송)' || CHAR(10) ||
     '4. 수신 거부: 마이페이지 또는 수신 메일 하단의 수신 거부 링크를 통해 언제든 철회할 수 있습니다.' || CHAR(10) ||
     '5. 본 동의는 선택 사항이며, 거부하더라도 서비스 이용에는 제한이 없습니다.',
     'N', CURRENT_DATE, 'Y');
