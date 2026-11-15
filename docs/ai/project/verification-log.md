# 검증 및 변경 로그 (Arkive 기록)

> **Arkive 기록 · 대상에 복사 금지.** 대상 지식은 [빈 양식](../portable/project-template-pack.md)을 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 배포 검증, 외부 연동(SMTP/SMS/OAuth/SSO/NICE) 실기기 E2E 검증, 13.3인치 실기기 화면 검증.
필요한 근거/확인자: ego-browser 실행 증거, 운영 파이프라인/배포 확인자, 연계 시스템 담당자.
완성 방법: 날짜별 자동 검증 및 GUI 검증 증거를 지속적으로 누적하고, DB 변경 시 호환성 검증 내역을 기록한다.
완료 기준: 검증 일시·환경·결과·잔여 위험이 명확히 기록되고, 미수행 항목의 사유가 기술되어 있다.
-->

기존 `current-state-analysis.md`, `migration.md`, `change-impact.md`(작업별 기록)의 내용을 통합한 검증 및 변경 이력 로그다.
현재 구현 및 검증 분류의 1차 기준은 [README.md](../../../README.md), 최신 자동 테스트 통계는 [src/test/README.md](../../../src/test/README.md)를 따른다.

---

## 1. 확인된 기능 흐름

저장소 코드에서 확인한 핵심 기능 흐름과 호출 출처다.

| 영역 | 코드에서 확인한 흐름 | 확인된 출처 | 추가 확인 필요 |
| --- | --- | --- | --- |
| 로그인·세션 | `/login/login.json` 성공 시 새 세션 발급, `LOGIN_USER`·`ADMIN_USER` 저장 후 `LoginSessionRegistry` 등록. 동일 사용자 재로그인 시 이전 세션 만료. OAuth 콜백·계정연계도 동일 레지스트리 공유 | `LoginController`, `OauthController`, `LoginSessionRegistry`, `EgovConfigWeb` | 다중 인스턴스 공유 세션 저장소, 세션 타임아웃 실제 동작 |
| 메뉴 관리 | `/system/menuList` 화면에서 Controller → Service → Mapper/XML로 조회·등록·수정·삭제 처리 | `SystemController`, `SystemServiceImpl`, `SystemMapper`, `System_SQL.xml` | 화면 호출 상세, 메뉴 정책, 오류 계약 |
| 파일 처리 | `FileController`가 일반 멀티파트, TUS, 드롭존 업로드 및 다운로드·삭제 제공. temp 업로드 후 본문 저장 시 확정(`MP_`) | `FileController`, `FileServiceImpl`, `File_SQL.xml`, `application.properties` | 파일 접근 권한, 보존·삭제 정책, 대용량 스토리지 연동 |
| DB 초기화/재사용 | 빈 로컬 DB에 `schema.sql` → `seed.sql` 1회 실행. 정상 재시작은 DDL/시드 없이 체크섬·메타데이터 읽기 검사만 수행. 구조 불일치는 자동 삭제 없이 기동 중단 | `application.properties`, `EgovConfigDatasource`, `HsqlDemoDatabaseInitializer`, `db/schema.sql`·`seed.sql` | 기존 데이터 갱신 절차, 운영 DB 마이그레이션 도구 연동 |
| 게시글 댓글 | 게시글 상세에서 댓글 목록/건수 조회. 로그인 사용자는 `/board/insertComment.json` 등록, 작성자/관리자만 `/board/deleteComment.json` 삭제. 1단 평문 저장, 게시글 삭제 시 cascade | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `db/schema.sql` (`BOARD_COMMENT`), `boardDetail.html` | 댓글 수정·신고 정책, 알림 연계 |
| 쪽지 관리 | `/message/inbox`·`/sent` 목록·검색·페이징, `/message/write` 발송, `/message/detail` 읽음 처리, `/message/deleteMessage.json` 함별 소프트 삭제 | `MessageController`, `MessageServiceImpl`, `MessageMapper`, `Message_SQL.xml`, `db/schema.sql` (`MESSAGE`) | 쪽지 보존·파기 정책, 알림 연계 |
| 공지 고정 | CK 목록 `NOTICE_YN DESC, REGIST_DT DESC` 정렬. `/board/updateNotice.json`으로 관리자 세션에서만 고정/해제. 목록·상세에 '공지' 배지 노출 | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `db/schema.sql` (`BOARD`) | 고정 개수 제한, TOAST 확장 여부 |
| 메뉴 권한 | `PermissionServiceImpl`이 `APP_USER_ROLE`→`APP_ROLE_MENU`를 사용자별로 조회해 메뉴별 OR 집계. `MenuPermissionInterceptor`가 READ/WRITE 검사(`ADMIN_YN` 우회). `MenuInterceptor`는 사이드바 필터링 | `PermissionServiceImpl`, `Permission_SQL.xml`, `MenuPermissionInterceptor`, `EgovConfigWeb`, `db/schema.sql`·`seed.sql` | 새 Controller 메뉴 매핑 누락 방지 |
| 사이트맵 | `/sitemap` 로그인 사용자 대상 읽기 전용 메뉴 트리. `MenuInterceptor`가 만든 권한 필터 메뉴(`menuList`) 재사용 | `SitemapController`, `comm/sitemap.html`, `layout.html` | 메뉴명·순서 변경은 메뉴관리 화면에서 처리 |
| 통합검색 | `/search` 로그인 사용자 대상. BOARD(CK·TOAST·FAQ·QNA) 제목 LIKE + `SEARCH_CHOSUNG` 초성 검색. 본문은 태그 제거 요약 표시. ES 활성화 시 색인 조회로 전환 | `SearchController`, `SearchServiceImpl`, `SearchMapper`, `KoreanSearchUtil`, `ElasticsearchSearchIndexService` | 본문 검색 HTML 오탐 보완, ES 동기화 정책 |
| 감사 로그 | `AuditInterceptor`가 로그인·로그아웃·메뉴접근·PII조회/다운로드·API접근 판정 후 `AuditEvent` 발행 → `AuditLogEventListener`가 `AUDIT_LOG`에 비동기 적재. `/system/auditLogList` 조회·엑셀 | `AuditInterceptor`, `AuditEvent`, `AuditLogEventListener`, `AuditLogServiceImpl`, `AuditLogController` | 보관주기 삭제 정책, 다중 인스턴스 중복 적재 방지 |
| 개인정보 보호 | `MaskingInterceptor`가 화면 모델 `List<EgovMap>` 마스킹. 주소록·명함·사용자 연락처는 `PiiFieldService`가 AES-GCM(`enc:v1:`) 암복호화. `APP_USER.EMAIL`은 평문 유지 | `MaskingInterceptor`, `MaskingUtil`, `PiiFieldService`, `FieldCryptoService`, `AddressServiceImpl`, `CardServiceImpl` | 상세·수동 화면 마스킹 누락 점검, 키 관리·회전 |
| 2차인증 (TOTP) | 로그인 성공 후 TOTP 대기 세션 → `/login/totpVerify.json` 검증(5회 실패 시 세션 폐기). 비밀키 AES-GCM 암호화, 복구코드 SHA-256 해시 저장 | `LoginController`, `TotpServiceImpl`, `TotpMapper`, `UserManageController` | 복구코드 분실 시 본인확인 절차 |
| 약관 동의 | `/terms` 공개, 로그인 후 `TermsInterceptor`가 미동의 사용자를 `/login/termsAgree`로 유도. `USER_TERMS_CONSENT`에 버전별 동의 이력 보관 | `TermsController`, `TermsServiceImpl`, `TermsMapper`, `TermsInterceptor` | 실제 약관 문구 법무 검토, 선택 동의 철회 |
| SMS·알림 채널 | `BoardNotificationEvent` 발행 시 SSE·메일·SMS 채널 분리. SMS는 메뉴 권한·수신동의(`SMS_YN='Y'`) 대상자에게 `BOARD.SMS_SENT_YN` 선점 후 발송 | `BoardNotificationEvent`, `BoardNotificationEventListener`, `SmsServiceImpl`, `AligoSmsSender` | 실제 SMS 제공자 계약, 발신 실패 재시도 |
| 통계·웹분석 | `/stats/statsList`에서 기간별 집계 및 엑셀 다운로드. `AnalyticsModelAdvice`가 화면 모델에 동의 기반 GA4/Clarity 설정 주입 | `StatsController`, `StatsServiceImpl`, `AnalyticsModelAdvice`, `arkive-analytics.js` | 실제 측정 ID 및 도메인 검증 |
| SSE 역할 필터 | `/board/subscribe` 30분 `SseEmitter` 등록. 글 등록 시 메뉴 조회 권한 역할을 가진 구독자에게만 브로드캐스트 | `BoardController`, `BoardNotificationEventListener`, `BoardSseService` | 단일 서버 메모리 기반, 다중 인스턴스 브로커 필요 |
| 메일 발송 | 환영·재설정·공지 알림 메일 발송. `@Async("mailTaskExecutor")`, `BOARD.MAIL_SENT_YN` 선점으로 1회 발송 보장. 수신 거부 사용자 제외 | `MailAsyncService`, `MailServiceImpl`, `EgovConfigAsync` | 실제 SMTP 제공자, 대량 발송 정책 |
| 비밀번호 재설정 | 1회용 링크 발급, SHA-256 해시 저장(`PASSWORD_RESET_TOKEN`), 30분 유효, 60초 쿨다운, 변경 성공 시 기존 세션 만료 | `LoginController`, `AuthServiceImpl`, `PASSWORD_RESET_TOKEN` | IP별 요청 제한, 메일 발송 실패 시 안내 |

---

## 2. 날짜별 검증 이력 (GUI 및 시스템 전환)

### 2.1. 2026-11-08: 최신 schema/seed 초기화 전환 검증

- **목적:** 다른 PC의 Git checkout에서도 별도 DB 설치 없이 HSQLDB 자동 시작으로 데모가 구동되도록 하되, 빈 DB만 최신 스키마·필수값으로 초기화하고 일반 재시작은 기존 데이터를 재사용하도록 전환.
- **기준 버전:** `1008db2901315a96f512b07d5df7655ad8f59e38` + 최신 변경.
- **환경:** Windows LOCAL, Java 17, Maven 3.9.16, HSQLDB 2.7.4, JaCoCo 0.8.12, ego-browser 0.6.2.7.
- **실행 조건:** 웹 포트 18080, DB 포트 19001, 격리 경로에서 실행.

| 검증 항목 | 조건 및 실제 결과 | 판정 |
| --- | --- | --- |
| 최종 구조·필수값 대조 | 구 SQL 24개 적용 DB와 새 `schema.sql`·`seed.sql` 적용 DB의 메타데이터 완전 일치. 메뉴 63행, 역할 2행, 권한 92행 일치 | 통과 |
| 빈 DB 최초 실행 | 기능 테이블 54개 + `ARKIVE_DB_INIT` 1개, 개발 admin 1명, 약관 3행 생성, 상태 `READY` 전환 확인 | 통과 |
| 구 DB 인수·재사용 | 완료된 구 체인 DB를 재시드 없이 정상 인수 확인. 기존 데이터 보존 | 통과 |
| 파일 DB 재기동 | 회의실 등록 및 메뉴 권한 변경 후 앱 재기동 시 데이터 보존 및 읽기 전용 기동 확인 (초기화 로그 390ms) | 통과 |
| 로그인 및 기본 흐름 | admin 로그인 → 약관 동의 → 사용자메인/대시보드 이동, 회의실 등록 성공 확인 | 통과 |
| 회원가입 및 권한 | 일반 회원 가입 후 로그인, `ROLE_USER` 권한 및 사이트맵 필터링 확인. 관리자 화면 직접 접근 시 로그인 화면으로 리다이렉트 확인 | 통과 |
| 불일치 DB 차단 | SQL 체크섬 불일치, 컬럼 길이 변경, 인덱스 삭제 시 앱 시작 거부 및 프로세스 종료(exit 1) 확인 | 통과 |
| 기존 자동 검증 | `verify.cmd` 최종 실행: 테스트 338개 통과, 실패 0, JaCoCo 기준 충족 (`BUILD SUCCESS`, 3분 02초) | 통과 |

---

### 2.2. 2026-11-08: 페이징과 이전글/다음글 GUI 확인

- **대상:** 공지사항 오프셋 페이징, 자료실 커서+더보기 페이징, 상세 화면 이전글/다음글.
- **도구 및 환경:** ego-browser 0.6.2.7, `127.0.0.1:8080` 관리자 세션.
- **결과:**
  - 공지사항 오프셋: 임시 글 11건 생성 후 `?pageIndex=1`에서 10행, 블록 `이전 1 2 다음`. 2페이지 클릭 시 1행 노출 및 검색 조건 유지 확인.
  - 자료실 커서: 첫 화면 10행 노출 후 `더보기` 클릭 시 11행으로 확장 및 `마지막 글입니다.` 표시 확인.
  - 이전글/다음글: 11번 글에서 다음글 없음, 이전글 10번 링크. 10번 글 이동 시 다음글 11번, 이전글 9번 정상 연동 확인.
  - 사용자 화면(`/user/board/*`)에서도 동일 동작 확인 후 생성 데이터 완전 삭제(`deleteBoard.json`)로 원상복구 완료.

---

### 2.3. 2026-11-08: 조회수 통계 화면 폭 반응형 대응

- **대상:** `/board/viewStats` 화면.
- **수정:** 누적 순위 표 등록일 셀 `white-space: nowrap` 적용, 640px 이하 모바일 폭 대응(차트 날짜 라벨 격일 표시, 오늘 라벨 항상 표시, 일별 표 최소 폭 해제).
- **GUI 확인:** 폭 1920, 1280, 768, 640, 480, 375, 320px 전 구간에서 페이지 가로 넘침 0건, 차트 넘침 0px 확인. 누적 순위 표는 5열로 640px 이하에서 내부 가로 스크롤 정상 동작 확인.

---

### 2.4. 2026-11-07: 게시판 화면 및 조회수 통계 검증

- **게시판 기본 GUI:** 관리자 CK 목록·등록·상세·댓글·삭제 및 HWP 표 붙여넣기, TOAST 에디터 등록·상세·삭제, 이미지 업로드(10×10 PNG 본문 삽입 및 서빙) 정상 확인.
- **조회수 통계 GUI:** `/board/viewStats` 14일 막대 차트 렌더링 확인. 동일 브라우저 재조회 시 24시간 중복 집계 차단 확인.

---

### 2.5. 2026-11-13: GUI 추가 확인

- **도구:** ego-browser.
- **결과:** 로그인·약관동의·통합검색·공지/게시판관리·FAQ·Q&A·공통코드·Swagger·멀티파트/TUS 업로드·사이트맵·TOTP 화면 렌더링 정상. README의 "구현 및 검증완료" 이동 항목과 동일 범위다.

---

## 3. 작업별 변경 영향 및 검증 기록

| 작업 일시 및 기능 | 직접 호출자 | 공통 사용처 | 정책 및 권한 | DB 및 외부 영향 | 검증 근거 |
| --- | --- | --- | --- | --- | --- |
| **2026-11-08 최신 schema/seed 초기화** | DataSource 빈 생성 | 전체 DB 소비 기능, 로그인, 메뉴, eGov ID 생성기 | 기존 최종 메뉴·역할 유지, 개발 admin | 빈 DB만 생성, 기존 DB 자동 삭제 없음, 체크섬 불일치 차단 | `verify.cmd` 338/338 통과, JAR 격리 기동 검증 통과 |
| **게시글 댓글** | `boardDetail.html` AJAX | 게시판 상세, 댓글 API | 로그인 사용자 작성, 작성자/관리자 삭제 | `BOARD_COMMENT` 신규, `BOARD` 삭제 시 cascade | `BoardMapperTest`, `BoardCommentControllerTest`, `verify.cmd` 통과 |
| **쪽지 관리** | `thymeleaf/message/*` | `/message/**`, 레이아웃 메뉴 | 로그인 사용자, 발신/수신 본인만 열람·삭제 | `MESSAGE` 신규, 쪽지 메뉴 4행 | `MessageMapperTest`, `MessageControllerTest`, `verify.cmd` 통과 |
| **공지 고정** | `boardList.html` 토글, `noticeList.html` 배지 | CK 공지 목록 대시보드·사용자 메인 | 관리자 전용, CK 게시판 한정 | `BOARD.NOTICE_YN`, `IDX_BOARD_NOTICE` | `BoardNoticeMapperTest`, `BoardNoticeControllerTest`, `verify.cmd` 통과 |
| **중복 로그인 방지** | 로그인 성공 경로 (폼·OAuth) | 세션 로그인 화면 전체, `AuthInterceptor` | 새 로그인 우선(이전 세션 만료) | 메모리 레지스트리 (단일 인스턴스) | `LoginSessionRegistryTest`, `LoginSessionControllerTest`, `verify.cmd` 통과 |
| **권한 관리** | `roleList.html`, `roleWrite.html`, 사이드바 | 모든 관리자 Controller, SSE, 사이트맵 | 역할별 READ/WRITE OR 집계, `ADMIN_YN` 우회 | `APP_ROLE`, `APP_USER_ROLE`, `APP_ROLE_MENU` | `PermissionMapperTest`, `RoleManageControllerTest`, `MenuPermissionControllerTest`, `verify.cmd` 통과 |
| **SSE 역할 필터** | `layout.html` SSE 구독 | 새 글 등록 알림, 게시판 목록 | 메뉴 조회 권한 역할에만 전송 | DB 변경 없음 (역할 조회만) | `BoardNotificationEventListenerTest`, `BoardNotificationControllerTest`, `verify.cmd` 통과 |
| **메일 발송** | 회원가입, OAuth, 비번찾기, 중요글 저장 | 로그인, 게시판, 비밀번호 찾기 | 수신 동의 사용자 한정, 1회 선점, 기본 비활성 | `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`·`MAIL_SENT_YN` | `MailServiceImplTest`, `PasswordResetControllerTest`, `verify.cmd` 통과 |
| **비밀번호 재설정** | `findPw.html`, `resetPw.html` | 로그인, 계정 찾기, 메일 링크 | 30분 유효, 1회용, 60초 쿨다운, 세션 무효화 | `PASSWORD_RESET_TOKEN` 신규 | `PasswordResetControllerTest` 통과 |
| **통합검색 1단계** | GNB 통합검색 링크, `/search` | 게시판, FAQ, Q&A 콘텐츠 | 로그인 사용자 전용, 제목 LIKE+초성 | DB 변경 없음, HTML 태그 제거 요약 노출 | `SearchMapperTest`, `SearchServiceImplTest`, `SearchControllerTest`, `verify.cmd` 통과 |
| **휴가 관리** | `myLeave.html`, `leaveList.html`, `balanceList.html` | 사이드바 인사관리 메뉴, `APP_USER` | 본인 휴가 신청/취소, 관리자 승인/반려, 잔여 초과 금지 | `LEAVE_BALANCE`, `LEAVE_REQUEST` 신규, 메뉴 4행 | `LeaveDaysCalculatorTest`, `LeaveControllerTest`, `verify.cmd` 통과 |

---

## 4. DB 마이그레이션 현황 및 개발자 사용법

### 4.1. 현재 확인된 상태
- 기본 DB는 `./data/ArkiveDB` 파일 HSQLDB를 사용하며 서버 재시작 후에도 스키마와 데이터를 보존한다.
- 빈 DB 기동 시 `src/main/resources/db/schema.sql`과 `src/main/resources/db/seed.sql`을 1회 실행하여 최신 객체와 필수 초기 데이터를 적재한다.
- `ARKIVE_DB_INIT` 테이블에서 초기화 파일의 SHA-256 체크섬 및 객체 메타데이터 서명을 검증하며, 정상 기동 시에는 DDL/시드를 재실행하지 않는다.
- 스키마 정의나 시드 SQL이 변경된 경우 기존 DB와의 불일치를 감지하여 **앱 기동을 안전하게 중단**한다.

### 4.2. 개발자 사용법
1. **로컬 기동:** Java 17·Maven 환경에서 `mvn spring-boot:run`을 실행하면 HSQLDB가 자동 구동된다.
2. **코드 수정 후 재시작:** Java, JavaScript, HTML만 수정한 경우 재시작 시 초기화 SQL이 실행되지 않고 기존 데이터가 그대로 유지된다.
3. **스키마 변경 시:**
   - `schema.sql` 및 `seed.sql`을 수정한 경우 기존 DB는 체크섬 불일치로 시작이 중단된다.
   - 기존 데이터를 보존할 필요가 없다면 새 DB 경로를 지정하여 기동한다:
     ```cmd
     mvn spring-boot:run "-Dspring-boot.run.arguments=--Globals.hsql_server.DatabasePath=./data/ArkiveDB-new"
     ```
   - 기존 데이터를 보존해야 하는 경우 수동 ALTER 및 마이그레이션 계획을 수립해야 한다.

### 4.3. 대상 프로젝트 이식 주의점
- 대상 프로젝트에는 HSQLDB 자동 시작 클래스(`HsqlDemoDatabaseInitializer`) 및 `ARKIVE_DB_INIT`을 복사하지 않는다.
- 대상 프로젝트의 DataSource 설정 및 마이그레이션 도구(Flyway/Liquibase 등)에 맞춰 `schema.sql`과 `seed.sql`의 필요한 DDL/DML만 발췌해 적용한다.

### 4.4. 보존·복구 주의
- `DBJob`은 10분마다 HSQLDB `SCRIPT` 덤프를 생성하지만, 교체 실패 시 기존 덤프 유실 위험 및 CLOB 단독 SQL 복구의 한계가 있으므로 운영 백업으로 간주하지 않는다.
- 운영 환경에서는 인프라 수준의 일관된 백업 및 복구 절차를 수립해야 한다.

---

## 5. 이전 버전 실행기 및 스키마 변경 이력 (V1~V35)

과거 버전별 마이그레이션 변경 이력을 보존한다 (현재는 최신 `schema.sql`·`seed.sql`로 통합됨).

- **2026-10-11:** 레거시 SQL 스쿼시(구 체인 통합).
- **2026-10-22:** `V2__board_comment.sql` (`BOARD_COMMENT`), `V3__message.sql` (`MESSAGE`, 메뉴 4행).
- **2026-10-23:** `V4__board_notice.sql` (`BOARD.NOTICE_YN`), `V5__permission.sql` (역할·권한 메뉴), `V6__mail_notification.sql` (메일 동의, 중요글, 비밀번호 토큰).
- **2026-10-24:** `V7`~`V12` (Q&A 공개 구분, 감사 로그, TOTP, 약관, 초성 검색, 연락처 암호화).
- **2026-10-24:** 3단계 마이그레이션 `V13`~`V18`, `V30`~`V33` (회의실, 약식결재, 필드암호화, 설문, SMS, HR, 강의, 약도, 채팅, 배치).
- **2026-10-24:** `V34__user_phone_crypto.sql` (`APP_USER.PHONE` 500바이트 확장).
- **2026-11-07:** `V35__board_view_stats.sql` (`BOARD_VIEW_LOG` 및 조회수 통계 메뉴).
- **2026-11-08:** 최신 `schema.sql` 및 `seed.sql`로 통합 전환 완료.

---

## 6. 주요 동작 변경 이력

- **2026-11-07 조회수 집계 방식 개선:** 세션 무기한 1회에서 **게시글당 브라우저 24시간 1회**로 전환. 세션 조회 시각과 `boardView_<boardId>` 쿠키(`Max-Age=86400`, HttpOnly)를 복합 검사하여 24시간 내 중복 조회를 차단하고 일별 통계의 정확성을 확보.
- **2026-11-03 게시판 검색 범위 분리:** 제목/내용/제목+내용 분리 검색 적용.
