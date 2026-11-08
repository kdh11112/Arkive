# DB 스키마 (Arkive 기록)

> **Arkive 기록 · 대상에 복사 금지.** 대상 지식은 [빈 양식](../portable/project-template-pack.md)을 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 DB 제품·버전, 운영 스키마와 컬럼 의미, 외래키/인덱스/제약조건의 전체 현황, 환경별 차이와 데이터 보존 기준.
필요한 근거/확인자: 운영 DDL/DB 카탈로그, 승인된 데이터 사전, DBA 및 데이터 소유자 확인.
완성 방법: 초기화용 개발 스크립트와 실제 환경의 스키마를 분리해 기록하고, 업무 의미는 데이터 사전/담당자 근거를 연결한다.
완료 기준: 대상 환경과 기준 버전이 명시되고, 운영 대상 객체와 제약조건이 확인된 출처에 연결되어 있다.
-->

> 이 문서는 저장소에 있는 초기화 SQL을 기준으로 한 개발 스키마 현황이며 운영 스키마 문서가 아니다. 실제 대상 DB와 버전은 환경 설정으로 별도 확인한다.

찾기: [연결·초기화](#확인된-연결초기화-설정) · [주요 테이블](#주요-테이블-최신-정의) · [추가 기능 객체](#추가-기능별-객체-최신-정의) · [변경 기록](#스키마-변경-기록).

## 확인된 연결·초기화 설정

- `application.properties`는 `Globals.DbType=hsql_server`와 `Globals.hsql_server.Port=9001`을 설정한다. `EgovConfigDatasource.java`는 이 값에서 `./data/ArkiveDB` 파일 HSQLDB 서버를 시작한다. 테스트는 `application-test.properties`에서 경로 `target/test-db`, 포트 9002로 분리한다.
- 2026-11-08부터 `src/main/resources/db/schema.sql`에 54개 기능 테이블과 최종 컬럼·PK/FK·Unique·인덱스를, `db/seed.sql`에 필수 초기값을 둔다. `spring.sql.init.mode=never`를 유지하고 `HsqlDemoDatabaseInitializer`가 빈 로컬 DB에만 `schema` → `seed`를 한 번 실행한다. 근거: 두 SQL, `EgovConfigDatasource.java`, `HsqlDemoDatabaseInitializer.java`.
- 초기화기의 `ARKIVE_DB_INIT`은 기능 스키마 밖의 데모 상태표다. 완료 상태·초기화 SQL 체크섬·객체 메타데이터 서명을 확인하고 일반 재시작은 DDL/시드를 실행하지 않는다. 파일 변경·불완전 초기화·구조 불일치는 자동 삭제나 갱신 없이 시작을 중단한다. 구 체인 전체 적용 이력과 구조가 일치하는 DB는 전환 때 한 번 인수하며 기존 데이터는 유지한다. 자세한 조건과 새 DB 경로 사용은 [마이그레이션 기록](migration.md)을 따른다.
- `seed.sql`은 메뉴 63행, 개발용 admin 1행, 역할 2행, 사용자-역할 1행, 역할-메뉴 권한 92행, `IDS.SAMPLE=1`, 약관 자리표시자 3행을 준비한다. 샘플 CRUD 레코드·회의실·자동 연차 부여·업무 로그·토큰·사용자 동의는 넣지 않는다. 구 DB를 인수할 때 이 시드를 재실행하지 않는다.
- 전환 전 `db/versions/`의 V1~V18·V30~V35(24개, V19~V29 없음)은 최신 정의로 통합해 제거했다. 덤프 표시용 주석 `db/data.sql`도 제거했으며 `DBJob`은 이 리소스를 읽지 않는다. 구 SQL·실행 결과는 아래 변경 기록과 Git 이력에 남긴다.
- `db/baseline_schema.sql`(구 복사용 밑그림, V10 시절에서 멈춤)과 `db/sampledb.sql`(레거시, `DROP TABLE` 포함)은 2026-10-11에 삭제했다. `db/system.sql`은 저장소에 이미 없고 문서에만 남아 있던 언급이라 함께 정리했다.
- HSQLDB 런타임 파일은 프로젝트 `data/` 아래에 만들어지며 `.lck`는 실행 중 잠금 파일이다. 현재 `.gitignore`에서 `data/`를 제외해 DB 파일은 Git에 올리지 않는다.
- 운영 DB 제품/버전과 운영 스키마: 미확인.

## 주요 테이블 (최신 정의)

현행 객체의 원본은 `db/schema.sql`, 필수 초기값의 원본은 `db/seed.sql`이다. 같은 객체에 대한 변경 결과를 최종 CREATE에 반영하며 기존 DB용 ALTER/백필과 구분한다. [SQL 배치 기준](../common/sql-standard.md#db-변경을-어느-파일에-넣을지)을 따른다.

| 테이블 | 확인된 용도/구조 | 주의 |
| --- | --- | --- |
| `MENU` | 메뉴 ID·이름·경로·상위·순서·사용 여부. 필수 메뉴 63행 | 기존 ID·상위 관계·경로·순서 유지. `ME02010300`은 Swagger, `ME02010400`은 사용자관리 |
| `ATCH_FILE` | 파일 ID·그룹 ID·원본/물리 파일명·경로·크기·확장자·등록 정보 (`USE_YN` 없음) | hard delete. 기존 `OTHER_` 그룹은 `DROPZONE_`으로 이전된 상태 |
| `SAMPLE` | eGovFrame CRUD 대상. 빈 DB에 샘플 레코드는 넣지 않음 | 매퍼가 살아 있어 테이블 유지 |
| `IDS` | eGovFrame 샘플 ID 생성기. `SAMPLE` 다음 ID 1로 초기화 | 샘플 레코드가 없으므로 1부터 등록 가능 |
| `BOARD` | 게시판. `BOARD_TYPE`: CK·TOAST·FAQ·QNA. `CONTENT CLOB`, 첨부 `BOARD_` 그룹, 공지·중요글·Q&A 공개 구분·초성·메일/SMS 발송 상태를 최종 정의에 포함 | 공지·중요글·발송 선점은 기본 'N', `ASK_TYPE` 기본 'PUBLIC'. TOAST 커서 목록은 공지 고정 정렬을 쓰지 않음 |
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
| `APP_USER` | 사용자. bcrypt 비밀번호, 관리자·잠금·사용·메일/SMS 동의 상태, 이메일과 `PHONE VARCHAR(500)`. 개발용 admin 1건 포함 | 시드에 이메일·전화·수신/약관 동의 없음. `PHONE` AES-GCM 저장에는 별도 키 필요. 개발용 계정을 실제 서비스에 복사하지 않음 |
| `APP_ROLE` | 역할 마스터. `ROLE_ID` PK, 역할명, 설명, 사용 여부 | `ROLE_ADMIN`·`ROLE_USER` 초기값. 역할 ID는 애플리케이션이 참조 |
| `APP_USER_ROLE` | 사용자-역할 매핑. `USER_ID`+`ROLE_ID` PK | 여러 역할 가능. 사용자 삭제 시 애플리케이션이 매핑 정리 |
| `APP_ROLE_MENU` | 역할-메뉴 권한. `ROLE_ID`+`MENU_ID` PK, `READ_YN`, `WRITE_YN` | `WRITE_YN='Y'`는 조회 포함으로 저장. 여러 역할은 메뉴별 OR 집계 |
| `OAUTH_LINK` | 소셜 로그인 연계 (`PROVIDER`, `PROVIDER_ID` PK) |  |

제외한 테이블: 구 V2의 `menu_author`·`user`는 매퍼·자바 참조 0건이라 baseline에서 뺐다. 로그인은 `APP_USER`를 쓴다.

## 추가 기능별 객체 (최신 정의)

아래 객체도 모두 `db/schema.sql`에 최종 정의가 있다. 메뉴·역할·권한 초기값은 `seed.sql`에서 필요한 부분만 가져간다. 과거 버전 경로와 실행 증거는 [변경 기록](#스키마-변경-기록)에 보존하고 현재 실행 검증과 구분한다.

| 객체/영역 | 현재 SQL | 확인된 용도/구조 | 주의 (DB종속·보존·백업) |
| --- | --- | --- | --- |
| `BOARD_COMMENT` | `schema.sql` | 게시글 1단 댓글. `COMMENT_ID` PK, `BOARD_ID` FK, `CONTENT VARCHAR(1000)`, 작성자 ID·표시명, 등록일 | `BOARD` 삭제 시 cascade. 삭제 권한은 애플리케이션 판정 |
| `MESSAGE` | `schema.sql` + 메뉴/권한 `seed.sql` | 발신자·수신자 ID/명, 제목·내용, 읽음 일시, 함별 삭제 플래그 | `CONTENT VARCHAR(4000)`과 `LIMIT/OFFSET`은 대상 DB 문자 길이·문법 확인. 양쪽 삭제 후에도 행은 남음, 보존 정책 미확인 |
| `BOARD.NOTICE_YN` | `schema.sql` | 공지 고정 여부(`Y`/`N`, 기본 `N`). CK 목록은 `NOTICE_YN DESC, REGIST_DT DESC, BOARD_ID DESC` | 기존 DB에 새 컬럼을 적용할 때는 별도 갱신 SQL 필요 |
| `APP_ROLE`, `APP_USER_ROLE`, `APP_ROLE_MENU` | `schema.sql` + `seed.sql` | 역할·사용자 매핑·메뉴 READ/WRITE, 권한관리 메뉴 | 기본 역할 권한은 기존 최종 목록 유지. 새 메뉴에 일반 역할 권한을 자동 부여하지 않음 |
| 메일·재설정 (`APP_USER`, `BOARD`, `PASSWORD_RESET_TOKEN`) | `schema.sql` | 메일 동의·중요글·게시글당 1회 발송 선점, 토큰 해시·만료·사용 상태 | 동의·중요글·선점 기본 `N`. 토큰 원문/초기 토큰 없음, 사용자 FK 없음. 유효기간/재발급 정책은 애플리케이션 책임 |
| `BOARD.ASK_TYPE`, `AUDIT_LOG` | `schema.sql` + 메뉴/권한 `seed.sql` | Q&A 공개/내부 구분, 감사 로그, 로그관리·통계 메뉴 | 감사 로그에 비밀번호·민감 원문을 저장하지 않음. 운영 보존 정책 미확인 |
| `LEAVE_BALANCE`, `LEAVE_REQUEST` | `schema.sql` + 메뉴/권한 `seed.sql` | 연도별 총 연차·사용일수, 휴가 신청·승인·반려 상태 | 새 DB에 연차를 임의 부여하지 않음. 연차관리 화면에서 등록. 신청중/승인 차감·취소 환원·주말/공휴일 제외는 애플리케이션 책임 |
| `APP_USER_TOTP`, `APP_USER_TOTP_RECOVERY` | `schema.sql` | TOTP 비밀키 암호문과 복구코드 해시·사용 상태 | 평문 비밀키·복구코드를 저장하지 않음. 사용자/TOTP 삭제 시 cascade. 초기 비밀키 없음 |
| `TERMS`, `USER_TERMS_CONSENT` | `schema.sql` + 약관 `seed.sql` | 버전별 약관과 사용자 동의 이력 | 시드는 약관 자리표시자 3행만. 기존 동의 이력 보존, 실제 문구·새 VERSION은 담당자 검토 |
| `BOARD.SEARCH_CHOSUNG` | `schema.sql` | 초성 검색용 색인 컬럼 | 빈 DB에는 색인할 게시물이 없음. 애플리케이션의 저장/갱신 경로와 선택 ES 연계 확인 |
| `APP_USER.PHONE`, `APP_USER.SMS_YN` | `schema.sql` | 휴대전화 `VARCHAR(500)`과 SMS 수신 동의(기본 `N`) | `AuthServiceImpl` 저장/조회, `SmsServiceImpl` 발송 대상 복호화. EMAIL은 조회 조건이라 평문. 기존 평문 암호화 이관·키 관리는 별도 |
| `MEETING_ROOM`, `MEETING_RESERVATION` | `schema.sql` + 메뉴/권한 `seed.sql` | 회의실 이름·위치·인원·사용 여부, 사용자·날짜·시간 예약 | 샘플 회의실 시드 없음. 먼저 화면에서 회의실 등록. 겹침 검사는 애플리케이션 책임, 시간은 `VARCHAR(5)` |
| `APPROVAL_DOC`, `APPROVAL_HISTORY` | `schema.sql` + 메뉴/권한 `seed.sql` | 기안자·결재자·문서 상태와 변경 이력 | `DRAFT`→`SUBMITTED`→`APPROVED`/`REJECTED`/`CANCELLED`. DOC_TYPE은 샘플 상수, 결재자 1명 |
| 주소록·명함 연락처/이메일 | `schema.sql` | `ADDRESS_BOOK`·`BUSINESS_CARD` 연락처 `VARCHAR(500)` | AES-GCM 암호문 길이 수용. 키 관리·기존 평문 이관 별도 |
| `SURVEY_TEMPLATE`, `SURVEY`, `SURVEY_QUESTION`, `SURVEY_ITEM`, `SURVEY_PARTICIPATION`, `SURVEY_ANSWER` | `schema.sql` + 메뉴/권한 `seed.sql` | 설문 템플릿·설문·질문·보기·참여·응답 | 삭제 cascade, `(SURVEY_ID, USER_ID)` UNIQUE. SINGLE/MULTI/TEXT는 샘플 유형 |
| `BOARD.SMS_SENT_YN`, `BOARD.SMS_SENT_DT` | `schema.sql` | SMS 1회 발송 선점 | 기본 `N`. `arkive.sms.*` 기본 비활성, 실제 제공자 계약 미확인 |
| `HR_ANNIVERSARY`, `HR_EVENT`, `HR_REWARD` | `schema.sql` + 메뉴/권한 `seed.sql` | 기념일·경조사·포상. 개인연차 화면은 휴가·당직 객체 조합 | 사용자 FK 없음, 유형 샘플 상수. 사용자·날짜 인덱스 6개 유지 |
| `LECTURE`, `LECTURE_PROGRESS` | `schema.sql` + 메뉴/권한 `seed.sql` | 동영상 경로·길이·설문·진도 기준과 사용자 수료 상태 | 기본 90%는 샘플 정책. 강의 삭제 시 진도 cascade, 서버 파일의 보존/복구 별도 |
| `MAP_GUIDE` | `schema.sql` + 메뉴/권한 `seed.sql` | 주소·위도·경도·설명 | 좌표 `DECIMAL(10,7)`. 지도 키가 비면 SDK 없이 표시 |
| `CHAT_MESSAGE` | `schema.sql` + 메뉴/권한 `seed.sql` | 사용자·표시명·메시지·등록일 | 단일 JVM 메모리 브로드캐스트, 다중 인스턴스·메시지 보존 미확인 |
| `STAT_DAILY` | `schema.sql` + 메뉴/권한 `seed.sql` | 날짜·유형·키별 일일 집계 값 | `ResourcelessJobRepository`, `BATCH_*` 테이블 없음. 같은 날짜 재집계는 삭제 후 재삽입, 실행 이력/스케줄러 없음 |
| `BOARD_VIEW_LOG` | `schema.sql` + 메뉴/권한 `seed.sql` | 조회 로그와 조회수 통계 메뉴. 날짜/게시글 인덱스 2개 | 일별 원본과 `BOARD.VIEW_CNT` 누적 순위 구분. 도입 전 누적값은 일별 그래프에 없음, 로그 보존 정책 미확인 |

### 애플리케이션 처리·시드·검증 범위

다음 V번호 설명은 **전환 전의 적용·검증 이력**이다. 새 DB의 시드 범위는 위 연결·초기화 절을 따르며, 아래의 샘플 회의실·자동 연차 부여·기존 사용자 자동 매핑을 다시 실행하지 않는다.

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

2026-11-08 최신 schema/seed 전환: 구 24개 SQL의 최종 메타데이터와 새 정의가 일치하고 메뉴·역할·매핑·권한의 차이가 없음을 격리 HSQLDB에서 대조했다. 기존 업무 데이터는 새 시드에서만 제외하며 구 DB에서 지우지 않는다. 실행 조건·검증 제약은 [전환 검증 기록](current-state-analysis.md#2026-11-08-최신-schemaseed-초기화-전환)에 둔다. 이후 신규 DB 정의는 `schema.sql`·`seed.sql`에서 관리하고 기존 DB 갱신과 구분한다.

컬럼 의미, 제약조건 및 실제 운영 데이터는 SQL 파일만으로 추정하지 않는다. 변경은 [sql-standard.md](../common/sql-standard.md) 및 [migration.md](migration.md)를 따른다.
