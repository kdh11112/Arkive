# DB 스키마 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. `application.properties`·Datasource 설정에서 DB 제품·버전·연결방식을 확인해 적는다. 운영DB는 추측 금지.
2. 버전 SQL(`db/V*`)별로 테이블·용도를 1행씩 적는다. 컬럼 의미는 데이터사전·담당자 근거 있을 때만 적는다.
3. Oracle/PG 등 대상 DB와 다른 문법(HSQL 전용 등)은 주의 칸에 표시한다.
4. 레거시 SQL(DROP 포함)은 자동실행 여부를 확인해 주의에 적는다.

## 양식 (Arkive 기록 참고)

| 테이블 | 스크립트 | 용도/구조 | 주의 (DB종속·보존·백업) |
| --- | --- | --- | --- |
| 예: `menu` | `db/V2__....sql` | 메뉴 ID·이름·경로 등 | 기존 ID 보존 |

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 DB 제품·버전, 운영 스키마와 컬럼 의미, 외래키/인덱스/제약조건의 전체 현황, 환경별 차이와 데이터 보존 기준.
필요한 근거/확인자: 운영 DDL/DB 카탈로그, 승인된 데이터 사전, DBA 및 데이터 소유자 확인.
완성 방법: 초기화용 개발 스크립트와 실제 환경의 스키마를 분리해 기록하고, 업무 의미는 데이터 사전/담당자 근거를 연결한다.
완료 기준: 대상 환경과 기준 버전이 명시되고, 운영 대상 객체와 제약조건이 확인된 출처에 연결되어 있다.
-->

> 이 문서는 저장소에 있는 초기화 SQL을 기준으로 한 개발 스키마 현황이며 운영 스키마 문서가 아니다. 실제 대상 DB와 버전은 환경 설정으로 별도 확인한다.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 연결·초기화 설정 (Arkive 예시)

- `application.properties`는 `Globals.DbType=hsql_server`와 `Globals.hsql_server.Port=9001`을 설정한다. `EgovConfigDatasource.java`는 이 값에서 `./data/ArkiveDB` 파일 HSQLDB 서버를 시작한다. 테스트는 `application-test.properties`에서 경로 `target/test-db`, 포트 9002로 분리한다.
- 기본 설정은 `spring.sql.init.mode=never`이며 스키마를 `db/versions/`의 `V*__*.sql`로 관리한다. 현재 파일은 V1(baseline), V2(댓글), V3(쪽지), V4(공지 고정), V5(권한), V6(메일·재설정), V7(2단계 Q&A 구분·감사 로그), V8(휴가), V9(TOTP), V10(약관), V11(초성 검색), V12(SMS 동의), V13(회의실), V14(약식결재), V15(연락처 컬럼 확장), V16(설문), V17(SMS 발송 상태), V18(HR), V30(강의), V31(약도), V32(채팅), V33(배치 집계), V34(사용자 휴대전화 암호화 컬럼 확장)이다. V19~V29는 없다. 빈 DB에서 시작기가 번호 순서로 적용하고 `SCHEMA_MIGRATION`에 버전별로 기록한다.
- 2026-10-11에 구 체인(V1,V2,V6~V29)을 최종 상태로 합쳐 단일 V1으로 교체했다. 구 버전 이력이 남은 DB에서는 새 V1이 스킵되므로 `data/`를 지우고 시작해야 한다. 2026-10-22에 V2(댓글)·V3(쪽지), 2026-10-23에 V4(공지 고정)·V5(권한)·V6(메일·재설정 토큰), 2026-10-24에 V8(휴가관리)을 추가했다. 현재 저장소에는 2단계 V7·V9~V12와 3단계 V13~V18·V30~V33이 더 있으며, 이후 변경은 V34부터 새 버전으로 추가한다 (적용된 SQL 수정 금지). 3단계 SQL은 파일만 확인했고 실제 적용·검증은 하지 않았다.
- `db/data.sql`은 시작 마이그레이션이 아니다. `DBJob` 덤프의 대상 파일명 표시용(143바이트 주석)이며 로더는 `db/versions/V*__*.sql`만 읽는다.
- `db/baseline_schema.sql`(구 복사용 밑그림, V10 시절에서 멈춤)과 `db/sampledb.sql`(레거시, `DROP TABLE` 포함)은 2026-10-11에 삭제했다. `db/system.sql`은 저장소에 이미 없고 문서에만 남아 있던 언급이라 함께 정리했다.
- HSQLDB 런타임 파일은 프로젝트 `data/` 아래에 만들어지며 `.lck`는 실행 중 잠금 파일이다. 현재 `.gitignore`에서 `data/`를 제외해 DB 파일은 Git에 올리지 않는다.
- 운영 DB 제품/버전과 운영 스키마: 미확인.

## 기준 스키마 테이블 (V1 baseline)

| 테이블 | 확인된 용도/구조 | 주의 |
| --- | --- | --- |
| `menu` | 메뉴 ID·이름·경로·상위·순서·사용 여부. V1 시드 32행 (사용자관리 `ME02010400` 포함)에 V3 쪽지 메뉴 4행이 추가된다 | 기존 메뉴 ID는 보존. `ME02010300`은 Swagger 유지. V3 메뉴 ID `ME02080000`~`ME02080300` |
| `ATCH_FILE` | 파일 ID·그룹 ID·원본/물리 파일명·경로·크기·확장자·등록 정보 (`USE_YN` 없음) | hard delete. 기존 `OTHER_` 그룹은 `DROPZONE_`으로 이전된 상태 |
| `SAMPLE` | eGovFrame CRUD 대상. 기본 레코드 6건 포함 | 샘플 매퍼가 살아 있어 유지 |
| `IDS` | eGovFrame 샘플 ID 생성기. `SAMPLE` 다음 ID 7로 초기화 |  |
| `BOARD` | 게시판. `BOARD_TYPE`: CK·TOAST·FAQ·QNA. 첨부는 `ATCH_FILE_GRPID`(`BOARD_` 접두사). `NOTICE_YN`은 V4, `IMPORTANT_YN`·`MAIL_SENT_YN`·`MAIL_SENT_DT`는 V6에서 추가(공지/중요글 메일) | `NOTICE_YN` 기본 'N'. `IMPORTANT_YN` 기본 'N'. `MAIL_SENT_YN`은 발송 전 원자적 선점에 사용하며 기본 'N'. TOAST 커서 목록은 고정 정렬을 쓰지 않음 |
| `COMM_CODE` | 공통코드. 마스터는 `MASTR_CODE = DETAIL_CODE`인 행 | 별도 마스터 테이블 없음 |
| `QNA_ANSWER` | Q&A 답변. `BOARD`와 1:1, 질문 삭제 시 cascade | 답변 존재 여부로 상태 판정 |
| `MEMO_REPORT` | 메모보고. 상태 보고→확인/의견 |  |
| `POPUP` | 팝업. 이미지는 `ATCH_FILE`(`POPUP_` 그룹) |  |
| `BANNER` | 배너. 메인이미지는 `POSITION='MAIN'`으로 통합 |  |
| `SCHEDULE` | 일정. 날짜는 문자열(`YYYY-MM-DD HH24:MI`) | `SCHEDULE_TYPE`으로 부서/개인 등 구분 |
| `HOLIDAY` | 공휴일. 달력 공용 |  |
| `CATEGORY` | 카테고리. `UP_CAT_ID` 자기참조 N단 |  |
| `ADDRESS_BOOK` | 주소록 |  |
| `POLL` / `POLL_ITEM` | 온라인POLL 질문·보기. 비로그인 중복투표 차단은 세션 |  |
| `DUTY` | 당직. 엑셀 일괄등록: 날짜(YYYYMMDD)\|당직자\|비고 |  |
| `BUSINESS_CARD` / `CARD_USER` | 명함·사용등록. 공개 명함만 목록 노출 |  |
| `COUNSEL` | 상담. 비밀번호 SHA 해시. 상태 접수대기/접수/완료 |  |
| `APP_USER` | 사용자. `ADMIN_YN`으로 관리자/일반 구분. `MAIL_YN`은 V6, `PHONE`·`SMS_YN`은 V12에서 추가된 수신 동의·연락처. 초기 admin 1건 포함 | 초기 비번은 개발용. 운영 전 변경. admin 시드의 이메일은 없으므로 메일 수신 대상이 아님. `PHONE`은 V34부터 AES-GCM 암호문 허용, 복호화 키 필요 |
| `APP_ROLE` | 역할 마스터. `ROLE_ID` PK, 역할명, 설명, 사용 여부 | V5에서 `ROLE_ADMIN`·`ROLE_USER` 시드. 역할 ID는 시스템 역할 고정 |
| `APP_USER_ROLE` | 사용자-역할 매핑. `USER_ID`+`ROLE_ID` PK | 여러 역할 가능. 사용자 삭제 시 애플리케이션이 매핑 정리 |
| `APP_ROLE_MENU` | 역할-메뉴 권한. `ROLE_ID`+`MENU_ID` PK, `READ_YN`, `WRITE_YN` | `WRITE_YN='Y'`는 조회 포함으로 저장. 여러 역할은 메뉴별 OR 집계 |
| `OAUTH_LINK` | 소셜 로그인 연계 (`PROVIDER`, `PROVIDER_ID` PK) |  |

제외한 테이블: 구 V2의 `menu_author`·`user`는 매퍼·자바 참조 0건이라 baseline에서 뺐다. 로그인은 `APP_USER`를 쓴다.

## 이후 마이그레이션 테이블 (V2~)

| 테이블 | 스크립트 | 확인된 용도/구조 | 주의 (DB종속·보존·백업) |
| --- | --- | --- | --- |
| `BOARD_COMMENT` | `db/versions/V2__board_comment.sql` | 게시글 1단 댓글. `COMMENT_ID` PK, `BOARD_ID` FK, `CONTENT VARCHAR(1000)`, 작성자 ID·표시명, 등록일 | `BOARD` 삭제 시 `ON DELETE CASCADE`. 대댓글 없음. 삭제 권한(작성자/관리자)은 애플리케이션에서 판정 |
| `MESSAGE` | `db/versions/V3__message.sql` | 받은/보낸 쪽지. 발신자·수신자 ID/명, 제목, 내용, 읽음 여부·일시, 발신/수신 삭제 플래그 | `CONTENT VARCHAR(4000)`은 DB 문자 길이 기준 확인 필요(Oracle bytes 주의). 목록 SQL은 `LIMIT ? OFFSET ?` 사용 → Oracle 이식 시 문법 조정. 양쪽 삭제 플래그가 Y여도 행은 남는다(보존·정리 정책 미확인) |
| `BOARD.NOTICE_YN` | `db/versions/V4__board_notice.sql` | 공지 고정 여부(`Y`/`N`, 기본 `N`). CK 목록은 `NOTICE_YN DESC, REGIST_DT DESC, BOARD_ID DESC`로 정렬 | `ALTER TABLE ... ADD COLUMN ... DEFAULT 'N' NOT NULL`은 HSQLDB 기준. Oracle/PG 등 대상 DB의 DDL 문법 확인 필요 |
| `APP_ROLE`, `APP_USER_ROLE`, `APP_ROLE_MENU` | `db/versions/V5__permission.sql` | 역할 마스터, 사용자-역할, 역할-메뉴 조회/쓰기 권한. `menu`에 `ME02010500`(권한관리) 추가 | 기존 사용자는 `ADMIN_YN`에 따라 `ROLE_ADMIN`/`ROLE_USER`로 매핑. `ROLE_USER`는 `ME0201*`·`ME0202*`(시스템·업로드)를 조회·쓰기 모두 제외하고 나머지 콘텐츠 메뉴에 쓰기까지 부여 |
| `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`, `BOARD.MAIL_SENT_YN`, `BOARD.MAIL_SENT_DT`, `PASSWORD_RESET_TOKEN` | `db/versions/V6__mail_notification.sql` | 메일 수신 동의, 중요글 표시, 게시글당 1회 메일 발송 선점, 1회용 비밀번호 재설정 토큰 | `MAIL_YN`·`IMPORTANT_YN`·`MAIL_SENT_YN` 기본 `N`. 토큰은 `TOKEN_HASH` PK에 SHA-256 해시만 저장하고 원문은 저장하지 않는다. `USED_YN`, `EXPIRES_DT`, `REGIST_DT`로 30분·1회를 판정. `APP_USER` FK는 선언하지 않음. `ALTER TABLE ... ADD COLUMN ... DEFAULT ... NOT NULL`·`CREATE INDEX IF NOT EXISTS`는 HSQLDB 기준이므로 대상 DB 문법 확인 필요 |
| `BOARD.ASK_TYPE`, `AUDIT_LOG`, `menu` 2행 | `db/versions/V7__stage2_qna_audit.sql` | Q&A 공개(`PUBLIC`)/내부(`INTERNAL`) 구분, 감사 로그, 로그관리·통계 메뉴(`ME02010600`·`ME02010700`) | 2단계 마이그레이션으로 이번 작업에서 새로 만든 것이 아니다. `DETAIL`에 비밀번호·주민번호·본문 원문을 저장하지 않는 전제. 적용·테스트 이력은 미확인 |
| `LEAVE_BALANCE`, `LEAVE_REQUEST`, `menu` 4행 | `db/versions/V8__leave.sql` | 사용자·연도별 총 연차·사용일수, 휴가 신청(유형·기간·일수·상태·승인자·반려사유). 메뉴 `ME02090000`(인사관리)과 `ME02090100`(내 휴가)·`ME02090200`(휴가관리)·`ME02090300`(연차관리) | 기존 `APP_USER`에게 현재 연도 15일을 시드한다(이미 있으면 건너뜀). 승인 시 차감·승인 취소 시 환원은 애플리케이션 트랜잭션 책임. 신청 기간은 같은 연도만 허용하고 일수는 주말·`HOLIDAY` 제외 평일, 반차 0.5일. `LIMIT/OFFSET`·`SUBSTRING`·`CREATE TABLE/INDEX IF NOT EXISTS`는 HSQLDB 기준이므로 대상 DB 문법 확인 필요 |
| `APP_USER_TOTP`, `APP_USER_TOTP_RECOVERY` | `db/versions/V9__totp.sql` | TOTP 비밀키(AES-GCM 암호문)와 복구코드 SHA-256 해시 | 2단계 마이그레이션. 평문 비밀키·복구코드는 저장하지 않는다. `APP_USER` 삭제 시 cascade. 적용·테스트 이력은 미확인 |
| `TERMS`, `USER_TERMS_CONSENT` | `db/versions/V10__terms.sql` | 버전별 약관(`SERVICE`·`PRIVACY`·`MARKETING`)과 사용자 동의 이력. 본문은 자리표시자 | 2단계 마이그레이션. 실제 문구는 법무·개인정보 담당자 검토 후 새 버전으로 추가하고 기존 동의 이력은 보존. 적용·테스트 이력은 미확인 |
| `BOARD.SEARCH_CHOSUNG` | `db/versions/V11__search_chosung.sql` | 초성 검색용 색인 컬럼 | 2단계 마이그레이션. 색인 값 채우기·갱신 주체는 애플리케이션/배치 확인 필요 |
| `APP_USER.PHONE`, `APP_USER.SMS_YN` | `db/versions/V12__user_contact.sql` | SMS 수신 동의와 연락처 | 2단계 마이그레이션. `SMS_YN` 기본 `N`. 발송은 별도 동의가 있을 때만. 값 저장은 V34에서 암호문 길이를 수용하도록 확장 |
| `APP_USER.PHONE` | `db/versions/V34__user_phone_crypto.sql` | 사용자 휴대전화 AES-GCM 암호문 저장을 위한 컬럼 길이 확장(`VARCHAR(500)`) | 2단계 보강. `AuthServiceImpl`이 저장 시 암호화·상세 조회 시 복호화하고, SMS 발송 대상은 `SmsServiceImpl`이 복호화한다. `APP_USER.EMAIL`은 아이디 찾기·비밀번호 재설정 조회 조건이라 평문 유지. 기존 평문 데이터의 암호화 이관은 미포함. 2026-10-24 테스트 DB에서 컬럼 확장 적용 확인, 휴대전화 암호화·복호화 경로는 자동 테스트 없음 `[일부검증]` |
| `MEETING_ROOM`, `MEETING_RESERVATION`, `menu` 2행 | `db/versions/V13__meeting.sql` | 회의실 마스터(이름·위치·수용인원·사용 여부)와 예약(회의실·사용자·날짜·시작/종료시각·제목·목적). 메뉴 `ME02050100`(회의실관리)·`ME02050200`(회의실예약) | 3단계. 같은 회의실·날짜의 시간대 겹침 거부는 애플리케이션 책임. 샘플 회의실 `MR001` 시드. 시간은 `VARCHAR(5)`(HH:mm). `[미검증]` |
| `APPROVAL_DOC`, `APPROVAL_HISTORY`, `menu` 1행 | `db/versions/V14__approval.sql` | 약식결재 문서(기안자·결재자·상태·상신/승인일·반려사유)와 상태 변경 이력. 메뉴 `ME02070100` | 3단계. 상태 `DRAFT`→`SUBMITTED`→`APPROVED`/`REJECTED`/`CANCELLED`. `DOC_TYPE`은 샘플 상수(GENERAL/LEAVE/EXPENSE/ETC). 결재자 1명 고정. `[미검증]` |
| `ADDRESS_BOOK.TEL_NO`, `ADDRESS_BOOK.EMAIL`, `BUSINESS_CARD.TEL_NO`, `BUSINESS_CARD.EMAIL` | `db/versions/V15__field_crypto.sql` | AES-GCM 암호문 저장을 위한 컬럼 길이 확장(`VARCHAR(500)`) | 3단계. 암호화 키 관리·기존 평문 데이터 마이그레이션은 별도 확인 필요. `[미검증]` |
| `SURVEY_TEMPLATE`, `SURVEY`, `SURVEY_QUESTION`, `SURVEY_ITEM`, `SURVEY_PARTICIPATION`, `SURVEY_ANSWER`, `menu` 4행 | `db/versions/V16__survey.sql` | 설문 템플릿·설문·질문·보기·참여·응답. 메뉴 `ME02031600`~`ME02031900`(템플릿·설문관리·참여·통계) | 3단계. 설문 삭제 시 질문·보기·참여·응답 cascade. 참여는 `(SURVEY_ID, USER_ID)` UNIQUE로 1회 제한. 질문 유형 SINGLE/MULTI/TEXT는 샘플 상수. `[미검증]` |
| `BOARD.SMS_SENT_YN`, `BOARD.SMS_SENT_DT` | `db/versions/V17__sms.sql` | 게시글 SMS 1회 발송 선점 상태 | 3단계. 기본 `N`. 발송 설정은 `arkive.sms.*`(기본 비활성). 외부 SMS 계약은 연계 명세 확인 필요. `[미검증]` |
| `HR_ANNIVERSARY`, `HR_EVENT`, `HR_REWARD`, `menu` 4행 | `db/versions/V18__hr.sql` | 기념일·경조사·포상 기록과 인사관리 메뉴 `ME02090400`~`ME02090700`. 개인연차 화면은 기존 `LEAVE_BALANCE`·`LEAVE_REQUEST`·`DUTY`를 조합 | 3단계. `APP_USER` FK는 선언하지 않음. 유형 코드는 샘플 상수. `[미검증]` |
| `LECTURE`, `LECTURE_PROGRESS`, `menu` 4행 | `db/versions/V30__lecture.sql` | 강의(동영상 경로·길이·선택 설문·수료 기준 진도율)와 사용자별 진도·수료 상태. 메뉴 `ME020A0000`(강의관리 상위)·`ME020A0100`~`ME020A0300` | 3단계. 진도율 기본 90%·설문 연계는 샘플 정책. `LECTURE_PROGRESS`는 강의 삭제 시 cascade. 파일 경로는 서버 저장소 기준. `[미검증]` |
| `MAP_GUIDE`, `menu` 1행 | `db/versions/V31__map_guide.sql` | 약도(주소·위도·경도·설명). 메뉴 `ME02040200` | 3단계. 카카오 지도 키는 화면 표시용이며 비어 있으면 지도 없이 표시. 좌표 정밀도 `DECIMAL(10,7)`. `[미검증]` |
| `CHAT_MESSAGE`, `menu` 2행 | `db/versions/V32__chat.sql` | 채팅 메시지(사용자·표시명·내용·등록일)와 커뮤니케이션 메뉴 `ME020B0000`·`ME020B0100` | 3단계. 브로드캐스트는 단일 JVM 메모리 기반이라 다중 인스턴스 공유 불가. 메시지 보존·정리 정책 미확인. `[미검증]` |
| `STAT_DAILY`, `menu` 1행 | `db/versions/V33__batch.sql` | 일일 집계 결과(날짜·유형·키·값). 메뉴 `ME02010800`(배치관리) | 3단계. `BATCH_*` 메타데이터 테이블은 만들지 않고 `ResourcelessJobRepository` 사용. 같은 날짜 재실행 시 삭제 후 재삽입. 스케줄러·실행 이력 없음. `[미검증]` |

- V3는 `MESSAGE`와 함께 `menu`에 `ME02080000`(쪽지관리), `ME02080100`(받은쪽지), `ME02080200`(보낸쪽지), `ME02080300`(쪽지쓰기)을 추가한다.
- V5는 `ME02010500`(권한관리) 메뉴를 추가하고 `ROLE_ADMIN` 전체 메뉴, `ROLE_USER` 콘텐츠 메뉴 권한을 시드한다. `/system`·`/auth`는 기존 `AuthInterceptor` 정책상 `ADMIN_YN` 관리자만 접근한다.
- V6은 메일 수신 동의(`APP_USER.MAIL_YN`)와 공지/중요글 알림(`BOARD.IMPORTANT_YN`, `MAIL_SENT_YN`, `MAIL_SENT_DT`), 비밀번호 재설정 토큰(`PASSWORD_RESET_TOKEN`)을 추가한다. 게시글 알림은 `MAIL_SENT_YN='N'`을 `Y`로 바꾸는 SQL이 1행을 갱신했을 때만 발송한다. 선점 후 SMTP 실패 시 자동 재시도는 없다.
- V8은 `LEAVE_BALANCE`·`LEAVE_REQUEST`, 인사관리 메뉴 4행, 역할-메뉴 5행(`ROLE_ADMIN` 4개·`ROLE_USER` 내 휴가 1개)을 만들고 기존 사용자에게 현재 연도 15일을 시드한다. 신청중 일수는 잔여 계산에서 차감하고, 승인 시 `USED_DAYS`를 조건부로 증가시켜 초과 승인을 막는다.
- V7·V9~V12는 2단계 기능(Q&A 공개 구분·감사 로그, TOTP, 약관, 초성 검색, SMS 동의)의 마이그레이션이다. 2026-10-24 테스트 DB 기동에서 전체 체인(V1→V34)과 함께 적용됐고 관련 테스트가 통과했다. 실제 운영 적용 이력은 미확인이다.
- V34는 `APP_USER.PHONE`을 `VARCHAR(500)`으로 늘려 사용자 휴대전화 AES-GCM 암호문을 저장할 수 있게 한다. 기존 평문 데이터의 암호화 이관은 포함하지 않으며, `ARKIVE_FIELD_KEY`가 없으면 새 휴대전화 저장은 거부된다. 애플리케이션 코드 변경과 함께 적용해야 한다.
- V13~V18·V30~V33은 2026-10-24 3단계 기능의 마이그레이션이다. 모두 `CREATE TABLE/INDEX IF NOT EXISTS`와 HSQLDB `ALTER TABLE ... ALTER COLUMN`·`ADD COLUMN` 문법을 쓰므로 Oracle/PostgreSQL 등 대상 DB에서는 문법과 DDL 트랜잭션 동작을 확인해야 한다.
- V13 회의실·V14 약식결재·V16 설문·V18 HR·V30 강의·V31 약도·V32 채팅·V33 배치는 각 기능 메뉴와 `APP_ROLE_MENU` 시드를 포함한다. 시드 권한은 샘플이며, 일반 역할은 회의실예약·약식결재·설문참여·내 강의·채팅만 받는다.
- V15는 주소록·명함 연락처 컬럼을 `VARCHAR(500)`으로 늘려 AES-GCM 암호문을 담게 한다. 기존 평문 데이터의 암호화 이관은 이 마이그레이션에 포함되지 않는다.
- V17은 게시글 SMS 1회 발송 선점 컬럼을 추가한다. SMS 발송은 기본 비활성이며 외부 제공자 연계는 별도 확인이 필요하다.
- V30은 동영상 파일 경로를 DB에 두고 서버 파일 시스템에서 스트리밍한다. 파일 저장 위치·백업·보존 정책은 운영 담당자 확인이 필요하다.
- V33은 집계 결과만 저장하고 Spring Batch 실행 이력(`BATCH_*`)은 남기지 않는다. 재실행은 같은 날짜 행을 지우고 다시 넣는 방식이라 과거 실행 감사가 필요하면 별도 이력 설계가 필요하다.
- 2단계·3단계 마이그레이션은 2026-10-24 `mvn -o test`의 테스트 DB(`target/test-db`) 기동에서 V1→V34 전체 체인 적용을 확인했다. 운영 DB 적용·데이터 이관은 미확인이다. `BOARD_COMMENT`·`MESSAGE`·메일·재설정 토큰·휴가의 애플리케이션 검증·권한 판정은 [current-state-analysis.md](current-state-analysis.md), API 경로는 [api-contract.md](api-contract.md)를 참조한다.

## 스키마 변경 기록

| 변경 ID/일자 | 객체 | 변경 이유 | DDL/마이그레이션 경로 | 영향 기능/Mapper | 검증·복구 근거 |
| --- | --- | --- | --- | --- | --- |
| 2026-10-11 | 스키마 전체 | 구 체인(V1,V2,V6~V29)을 최종 상태 단일 V1으로 합침. `menu_author`·`user` 제외, 사용자관리 `ME02010400` 신규, 메뉴 공백 정리 | `db/versions/V1__baseline.sql` | 전체 (경로·매퍼는 불변) | `verify.cmd` 성공(당시 Tests 9/9, JaCoCo는 HtmlSanitizer만 검사). 빈 테스트 DB에 V1 1회 적용 + `SCHEMA_MIGRATION` 기록 확인. 구 이력 DB는 `data/` 삭제 후 재생성 |
| 2026-10-11 | `baseline_schema.sql`, `sampledb.sql` | 밑그림이 V1에 흡수됨. 레거시는 `DROP` 위험으로 삭제 | 삭제 (Git 이력에 남음) | 없음 (로더 미참조) | 참조 grep 0건 확인 후 삭제 |
| 2026-10-22 | `BOARD_COMMENT` | 게시글 댓글(1단) 기능 추가 | `db/versions/V2__board_comment.sql` | `BoardController`, `BoardService`, `BoardMapper`, `Board_SQL.xml`, `board/boardDetail.html` | `verify.cmd` 성공(당시 61/61, JaCoCo 게이트 일부 클래스). 자동 테스트: `BoardMapperTest`(댓글·cascade), `BoardCommentControllerTest`(인증·검증·삭제 권한). 수동 E2E 기록은 별도 확인 필요 |
| 2026-10-22 | `MESSAGE`, `menu` 4행 | 받은/보낸 쪽지와 쪽지관리 메뉴 추가 | `db/versions/V3__message.sql` | `MessageController`, `MessageService`, `MessageMapper`, `Message_SQL.xml`, `message/*` | `verify.cmd` 성공(당시 61/61, JaCoCo 게이트 일부 클래스). 자동 테스트: `MessageMapperTest`(함 분리·읽음·소프트 삭제·검색), `MessageControllerTest`(인증·중복제출·권한). 수동 E2E 기록은 별도 확인 필요 |
| 2026-10-23 | `BOARD.NOTICE_YN`, `IDX_BOARD_NOTICE` | CK 공지 고정/해제와 목록 상단 정렬 | `db/versions/V4__board_notice.sql` | `BoardController`, `BoardService`, `BoardMapper`, `Board_SQL.xml`, `board/*`, `user/board/notice*` | `verify.cmd` 성공(당시 61/61, JaCoCo 게이트 일부 클래스). 자동 테스트: `BoardNoticeMapperTest`(정렬·해제·CK 외 거부), `BoardNoticeControllerTest`(401·관리자 권한·입력 검증·목록 렌더링). 수동 화면 확인은 별도 필요 |
| 2026-10-23 | `APP_ROLE`, `APP_USER_ROLE`, `APP_ROLE_MENU`, `menu` 1행 | 역할 기반 메뉴 권한과 SSE 대상 역할 | `db/versions/V5__permission.sql` | `PermissionService`/`PermissionMapper`/`Permission_SQL.xml`, `RoleManageController`, `MenuPermissionInterceptor`, `auth/role*` | `verify.cmd` 성공(당시 61/61, JaCoCo 게이트 일부 클래스). 자동 테스트: `PermissionMapperTest`(시드·기본 역할 제외·권한 집계·SSE 역할), `RoleManageControllerTest`(역할 CRUD·잘못된 사용자 거부·사용자 삭제 시 역할 정리), `MenuPermissionControllerTest`(READ/WRITE/거부/사이드바). 수동 화면 확인은 별도 필요 |
| 2026-10-23 | `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`·`MAIL_SENT_YN`·`MAIL_SENT_DT`, `PASSWORD_RESET_TOKEN` | 메일 수신 동의·공지/중요글 1회 알림·비밀번호 재설정 토큰 | `db/versions/V6__mail_notification.sql` | `MailServiceImpl`, `BoardController`/`Board_SQL.xml`, `BoardNotificationEvent`/`BoardNotificationEventListener`, `AuthServiceImpl`/`AuthMapper`/`Auth_SQL.xml`, `LoginController`, `UserManageController` | `verify.cmd` 성공(당시 105/105, MailServiceImpl·BoardNotificationEventListener JaCoCo 게이트 포함). 자동 테스트: `MailServiceImplTest`(비활성·동의·1회 발송·HTML escape·SMTP 실패), `BoardNotificationEventListenerTest`·`BoardNotificationControllerTest`(SSE·메일 분리, 중요글 전환, 실패 격리), `PasswordResetControllerTest`(일치 시 토큰 발급·1회 사용·만료·입력 검증·쿨다운). 실제 SMTP E2E는 미수행 |
| 2026-10-23 | `BOARD.SEARCH_CHOSUNG` 등 | 통합검색 1단계는 새 테이블·컬럼 없이 제목 LIKE, 이후 초성 검색용 `SEARCH_CHOSUNG` 추가 | `db/versions/V11__search_chosung.sql` | `SearchController`, `SearchService`/`SearchServiceImpl`, `SearchMapper`, `Search_SQL.xml`, `SearchIndexService`/`ElasticsearchSearchIndexService`, `SearchReindexController`, `search/searchList.html` | 제목 LIKE·초성 검색과 ES 선택 전환(`arkive.search.elasticsearch.enabled=false` 기본) 구현. 자동 테스트: `SearchMapperTest`, `SearchServiceImplTest`, `SearchControllerTest`, `KoreanSearchUtilTest` — 2026-10-24 `verify.cmd -o`에서 통과. ES 활성·재색인 경로는 미검증이고, `SearchServiceImpl` JaCoCo 게이트는 충족. 본문 검색은 미적용 |
| 2026-10-24 | `LEAVE_BALANCE`, `LEAVE_REQUEST`, `menu` 4행, `APP_ROLE_MENU` 5행 | 휴가 신청·승인/반려·취소와 연차 부여 | `db/versions/V8__leave.sql` | `LeaveController`, `LeaveService`/`LeaveServiceImpl`, `LeaveDaysCalculator`, `LeaveMapper`, `Leave_SQL.xml`, `leave/*`, `MenuPermissionInterceptor` | `mvn -o verify` 성공(134/134, JaCoCo 게이트 통과). 자동 테스트: `LeaveDaysCalculatorTest`(5), `LeaveControllerTest`(7) — 화면 3종·토큰·승인 전 미차감·승인 차감·취소 환원·반려·중복 기간·잔여 초과·연차 하향 거부. 실행 앱에서 화면 3종 HTTP 200과 연차 10일 부여→2일 신청→승인(사용 2.0일)→취소(사용 0.0일) 흐름 확인 후 검증 데이터 삭제 (세션 쿠키 기반 HTTP, 실제 브라우저 GUI 확인은 미완료) |
| 일자 미확인 (V34는 2026-10-24) | `BOARD.ASK_TYPE`, `AUDIT_LOG`, `APP_USER_TOTP*`, `TERMS`, `USER_TERMS_CONSENT`, `BOARD.SEARCH_CHOSUNG`, `APP_USER.PHONE`·`SMS_YN` | 2단계 기능(Q&A 구분·감사 로그·TOTP·약관·초성 검색·SMS 동의·휴대전화 암호화) | `db/versions/V7__stage2_qna_audit.sql`, `V9__totp.sql`, `V10__terms.sql`, `V11__search_chosung.sql`, `V12__user_contact.sql`, `V34__user_phone_crypto.sql` | 각 2단계 기능 | 2026-10-24 `mvn -o test`의 테스트 DB 기동에서 적용 확인, 관련 테스트 통과. 운영 DB 적용·기존 평문 이관은 미확인 |
| 2026-10-24 | `MEETING_ROOM`, `MEETING_RESERVATION`, `APPROVAL_DOC`, `APPROVAL_HISTORY`, 메뉴·권한 시드 | 회의실 예약과 약식결재 | `db/versions/V13__meeting.sql`, `V14__approval.sql` | `MeetingController`/`MeetingService`/`MeetingMapper`/`Meeting_SQL.xml`, `ApprovalController`/`ApprovalService`/`ApprovalMapper`/`Approval_SQL.xml`, `MenuPermissionInterceptor` | 2026-10-24 테스트 DB 적용·관련 테스트 통과. 실행 HTTP·GUI·업무 규칙은 미검증 |
| 2026-10-24 | `ADDRESS_BOOK`·`BUSINESS_CARD` 연락처 확장, `SURVEY_*` 6개, `BOARD.SMS_SENT_*`, `HR_*` 3개, 메뉴·권한 시드 | 연락처 암호화, 설문, SMS 발송 상태, HR 세트 | `db/versions/V15__field_crypto.sql`, `V16__survey.sql`, `V17__sms.sql`, `V18__hr.sql` | `SurveyController`/`SurveyService`/`SurveyMapper`/`Survey_SQL.xml`, `HrController`/`HrService`/`HrMapper`/`Hr_SQL.xml`, `FieldCryptoService`, SMS 발송부 | 2026-10-24 테스트 DB 적용·관련 테스트 통과. 외부 SMS·실행 HTTP·GUI 미검증 |
| 2026-10-24 | `LECTURE`, `LECTURE_PROGRESS`, `MAP_GUIDE`, 메뉴·권한 시드 | 강의형 콘텐츠·동영상 진도, 약도 | `db/versions/V30__lecture.sql`, `V31__map_guide.sql` | `LectureController`/`LectureService`/`LectureMapper`/`Lecture_SQL.xml`, `VideoEncodingService`, `MapController`/`MapGuideService`/`MapGuideMapper`/`MapGuide_SQL.xml` | 2026-10-24 테스트 DB 적용·관련 테스트 통과. FFmpeg·카카오 지도·실행 HTTP·GUI는 미검증(기본 비활성) |
| 2026-10-24 | `CHAT_MESSAGE`, `STAT_DAILY`, 메뉴·권한 시드 | WebSocket 채팅 이력, Spring Batch 일일 집계 결과 | `db/versions/V32__chat.sql`, `V33__batch.sql` | `ChatController`/`ChatWebSocketHandler`/`ChatService`/`ChatMapper`/`Chat_SQL.xml`, `BatchController`/`BatchService`/`BatchMapper`/`Batch_SQL.xml`, `BatchConfig`/`DailyStatsTasklet` | 2026-10-24 테스트 DB 적용·컨텍스트 기동 확인. WebSocket 실접속·배치 실행·GUI 미검증. 채팅 단일 인스턴스, 배치 스케줄러·실행 이력 없음 |

이후 신규 SQL은 `db/versions/` 바로 아래에 V35부터 추가한다.

컬럼 의미, 제약조건 및 실제 운영 데이터는 SQL 파일만으로 추정하지 않는다. 변경은 [sql-standard.md](../common/sql-standard.md) 및 [migration.md](migration.md)를 따른다.
