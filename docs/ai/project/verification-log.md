# 검증 및 변경 로그 (Arkive 기록)

> **Arkive 기록 · 대상에 복사 금지.** 대상 지식은 [빈 양식](../portable/project-template-pack.md)을 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 배포 검증, 외부 연동(SMTP/SMS/OAuth/SSO/NICE) 실기기 E2E 검증, 13.3인치 실기기 화면 검증.
필요한 근거/확인자: ego-browser 실행 증거, 운영 파이프라인/배포 확인자, 연계 시스템 담당자.
완성 방법: 날짜별 자동 검증 및 GUI 검증 증거를 지속적으로 누적하고, DB 변경 시 호환성 검증 내역을 기록한다.
완료 기준: 검증 일시·환경·결과·잔여 위험이 명확히 기록되고, 미수행 항목의 사유가 기술되어 있다.
-->

확인된 기능 흐름, 날짜별 검증 이력, 작업별 변경 영향, DB 마이그레이션 현황을 기록하는 검증 및 변경 이력 로그다.
현재 구현 및 검증 분류의 1차 기준은 [README.md](../../../README.md), 최신 자동 테스트 통계는 [src/test/README.md](../../../src/test/README.md)를 따른다.
2026-11-22의 현재 DB 구성은 [2.21절](#221-2026-11-22-postgresql-단일-dbboot-자동-구성-정리)과 4절을 따른다. 이전 날짜의 HSQLDB·Globals 설정·검증 결과는 당시 이력이며 현재 실행 안내가 아니다.

---

## 1. 확인된 기능 흐름

저장소 코드에서 확인한 핵심 기능 흐름과 호출 출처다.

| 영역 | 코드에서 확인한 흐름 | 확인된 출처 | 추가 확인 필요 |
| --- | --- | --- | --- |
| 로그인·세션 | `/login/login.json` 성공 시 새 세션 발급, `LOGIN_USER`·`ADMIN_USER` 저장 후 `LoginSessionRegistry` 등록. 동일 사용자 재로그인 시 이전 세션 만료. OAuth 콜백·계정연계도 동일 레지스트리 공유 | `LoginController`, `OauthController`, `LoginSessionRegistry`, `EgovConfigWeb` | 다중 인스턴스 공유 세션 저장소, 세션 타임아웃 실제 동작 |
| 메뉴 관리 | `/system/menuList` 화면에서 Controller → Service → Mapper/XML로 조회·등록·수정·삭제 처리 | `SystemController`, `SystemServiceImpl`, `SystemMapper`, `System_SQL.xml` | 화면 호출 상세, 메뉴 정책, 오류 계약 |
| 파일 처리 | `FileController`가 일반 멀티파트, TUS, 드롭존 업로드 및 다운로드·삭제 제공. temp 업로드 후 본문 저장 시 확정(`MP_`) | `FileController`, `FileServiceImpl`, `File_SQL.xml`, `application.properties` | 파일 접근 권한, 보존·삭제 정책, 대용량 스토리지 연동 |
| DB 접속/초기화 | Boot JDBC·HikariCP 자동 구성. 앱 자동 DDL·시드 없음. 새 대상 DB에 수동 적용, 테스트만 격리 PG 초기화기가 schema→seed 적용 | `application.properties`, `pom.xml`, `db/postgres/schema.sql`·`seed.sql`, `PostgresTestDatabaseInitializer` | 기존 데이터 갱신 절차, 운영 DB 마이그레이션 도구 연동 |
| 게시글 댓글 | 게시글 상세에서 댓글 목록/건수 조회. 로그인 사용자는 `/board/insertComment.json` 등록, 작성자/관리자만 `/board/deleteComment.json` 삭제. 1단 평문 저장, 게시글 삭제 시 cascade | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `db/postgres/schema.sql` (`BOARD_COMMENT`), `boardDetail.html` | 댓글 수정·신고 정책, 알림 연계 |
| 쪽지 관리 | `/message/inbox`·`/sent` 목록·검색·페이징, `/message/write` 발송, `/message/detail` 읽음 처리, `/message/deleteMessage.json` 함별 소프트 삭제 | `MessageController`, `MessageServiceImpl`, `MessageMapper`, `Message_SQL.xml`, `db/postgres/schema.sql` (`MESSAGE`) | 쪽지 보존·파기 정책, 알림 연계 |
| 공지 고정 | CK 목록 `NOTICE_YN DESC, REGIST_DT DESC` 정렬. `/board/updateNotice.json`으로 관리자 세션에서만 고정/해제. 목록·상세에 '공지' 배지 노출 | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `db/postgres/schema.sql` (`BOARD`) | 고정 개수 제한, TOAST 확장 여부 |
| 메뉴 권한 | `PermissionServiceImpl`이 `APP_USER_ROLE`→`APP_ROLE_MENU`를 사용자별로 조회해 메뉴별 OR 집계. `MenuPermissionInterceptor`가 READ/WRITE 검사(`ADMIN_YN` 우회). `MenuInterceptor`는 사이드바 필터링 | `PermissionServiceImpl`, `Permission_SQL.xml`, `MenuPermissionInterceptor`, `EgovConfigWeb`, `db/postgres/schema.sql`·`seed.sql` | 새 Controller 메뉴 매핑 누락 방지 |
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

### 2.1. 2026-11-07: 게시판 화면 및 조회수 통계 검증

- **게시판 기본 GUI:** 관리자 CK 목록·등록·상세·댓글·삭제 및 HWP 표 붙여넣기, TOAST 에디터 등록·상세·삭제, 이미지 업로드(10×10 PNG 본문 삽입 및 서빙) 정상 확인.
- **조회수 통계 GUI:** `/board/viewStats` 14일 막대 차트 렌더링 확인. 동일 브라우저 재조회 시 24시간 중복 집계 차단 확인.

---

### 2.2. 2026-11-08: 최신 schema/seed 초기화 전환 검증

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

### 2.3. 2026-11-08: 페이징과 이전글/다음글 GUI 확인

- **대상:** 공지사항 오프셋 페이징, 자료실 커서+더보기 페이징, 상세 화면 이전글/다음글.
- **도구 및 환경:** ego-browser 0.6.2.7, `127.0.0.1:8080` 관리자 세션.
- **결과:**
  - 공지사항 오프셋: 임시 글 11건 생성 후 `?pageIndex=1`에서 10행, 블록 `이전 1 2 다음`. 2페이지 클릭 시 1행 노출 및 검색 조건 유지 확인.
  - 자료실 커서: 첫 화면 10행 노출 후 `더보기` 클릭 시 11행으로 확장 및 `마지막 글입니다.` 표시 확인.
  - 이전글/다음글: 11번 글에서 다음글 없음, 이전글 10번 링크. 10번 글 이동 시 다음글 11번, 이전글 9번 정상 연동 확인.
  - 사용자 화면(`/user/board/*`)에서도 동일 동작 확인 후 생성 데이터 완전 삭제(`deleteBoard.json`)로 원상복구 완료.

---

### 2.4. 2026-11-08: 조회수 통계 화면 폭 반응형 대응

- **대상:** `/board/viewStats` 화면.
- **수정:** 누적 순위 표 등록일 셀 `white-space: nowrap` 적용, 640px 이하 모바일 폭 대응(차트 날짜 라벨 격일 표시, 오늘 라벨 항상 표시, 일별 표 최소 폭 해제).
- **GUI 확인:** 폭 1920, 1280, 768, 640, 480, 375, 320px 전 구간에서 페이지 가로 넘침 0건, 차트 넘침 0px 확인. 누적 순위 표는 5열로 640px 이하에서 내부 가로 스크롤 정상 동작 확인.

---

### 2.5. 2026-11-13: GUI 추가 확인

- **도구:** ego-browser.
- **결과:** 로그인·약관동의·통합검색·공지/게시판관리·FAQ·Q&A·공통코드·Swagger·멀티파트/TUS 업로드·사이트맵·TOTP 화면 렌더링 정상. README의 "구현 및 검증완료" 이동 항목과 동일 범위다.
- **2026-11-18 재분류:** 이 기록은 화면 렌더링만 확인했고 기능별 시나리오·결과가 없다. 여기에 근거한 항목은 README 「구현 (재검증 대기)」로 옮겼다.

---

### 2.6. 2026-11-15: 사이트맵 UI 카드형 전환

- **대상:** `/sitemap` 화면(`comm/sitemap.html`)과 `css/krds-local.css`의 `.sitemap-*` 규격.
- **변경:** 대메뉴/하위메뉴 2열 표를 대메뉴별 카드 그리드로 교체. 하위 메뉴는 알약형 링크(`.sitemap-link`)로 표시하고 카드 hover·focus 시 테두리 강조. 575px 이하 1열 전환.
- **검증:** `verify.cmd` 338/338 통과, JaCoCo 게이트 통과(`BUILD SUCCESS`, 2026-11-15 21:34). ego-browser 0.6.3.14로 admin 로그인 후 확인.
- **측정값(대체 viewport):** 기본 창 innerWidth 1422px에서 4열(카드 폭 260px)·카드 11개·링크 49개·가로 넘침 0px. 533px 폭에서 1열 전환·가로 넘침 0px. 링크 최소 높이 32px. 테스트 3건(`SitemapControllerTest`)이 요구하는 "사이트맵"·"공지사항관리"·"표시할 메뉴가 없습니다." 문구 유지 확인.
- **미수행/제약:** 13.3인치 실기기 확인은 미수행(대체 viewport만 측정). 화면 폭이 1023px 이하일 때 측면 내비가 전폭으로 펼쳐져 사이트맵이 아래로 밀리는 동작은 기존 레이아웃 규칙이며 이번 변경 범위가 아니다.

---

### 2.7. 2026-11-15: 전역 헤더의 약관·2단계 인증 링크 정리

- **배경:** 2단계 인증은 로그인할 때 확인하는 관문인데, 설정 화면(`/login/totpSetup`) 링크가 전역 헤더에 있어 로그인 후 화면에서도 로그인 절차처럼 보였다. 약관도 마찬가지로 헤더에 고정 노출되고 있었다.
- **변경:** `layout.html` 헤더에서 `약관`·`2단계 인증` 링크 제거(이름·사이트맵·로그아웃은 유지). `footer.html`에 `이용약관·개인정보 처리방침`(`/terms`) 링크 추가. `main/dashboard.html`에 `계정 보안` 표를 추가해 2단계 인증 설정(`/login/totpSetup`)·약관 보기(`/terms`)를 로그인 후 화면에서 제공.
- **DB·권한 영향:** 없음. 링크 위치만 바꿨고 두 기능의 URL·인터셉터·세션 규칙은 그대로다.
- **검증(ego-browser):** admin 로그인 후 `/dashboard` 확인. 헤더 링크는 `관리자`·`사이트맵`·`로그아웃` 3개, 푸터에 `/terms` 링크 1개, 본문 `계정 보안` 표에 `설정`·`보기` 버튼 노출.
- **자동 검증:** `verify.cmd` → 테스트 338개 통과, 실패 0, JaCoCo 기준 충족, `BUILD SUCCESS`(3분 14초).

---

### 2.8. 2026-11-15: 통계 화면 카드형 그래프 전환

- **참고:** ADTM `illegalAdvertiseStat.jsp`(전자정부 클래식 JSP)의 `cont_half` + `graph_area` + 숫자 표 구성을 기준으로 삼았다. 원본은 toastui-chart로 그리지만, Arkive는 차트 라이브러리를 새로 넣지 않고 CSS 막대(`.stat-bar`)로 같은 정보를 보여준다.
- **변경:** `stats/statsList.html`을 카드 그리드(`.stat-grid`·`.stat-card`)로 재작성. 카드마다 제목·단위·막대 그래프·숫자 표를 함께 두고, 막대 높이는 페이지 스크립트가 카드 안 최대값을 100%로 계산한다. `survey/stats.html`의 질문 블록을 같은 카드로 감싸고 비율 막대를 `.stat-track`·`.stat-bar`로 통일. 공통 규격은 `krds-local.css`에 추가.
- **막대 색:** 항목이 모두 같은 색이라 구분이 안 되던 문제를 고쳐, 8색 팔레트를 열·행 순서대로 돌려 쓴다(toastui-chart 기본 팔레트 계열). 막대형은 `.stat-bar-col:nth-child`, 설문 비율형은 표 행 순서 기준이다.
- **DB·권한 영향:** 없음. Controller·Mapper·모델 속성은 그대로이고 화면 마크업만 바꿨다.
- **검증(ego-browser, 캐시 비활성):** `/stats/statsList` 카드 5개, 2열 그리드(534px×2), 그래프 높이 220px, 막대 최대 폭 160px(항목이 적을 때 막대가 카드 전폭으로 늘어나지 않게 함), 가로 넘침 0px. 막대 높이는 카드별 최대값 기준(게시판 1건 100%, 이벤트 4종 100/21.9/17.1/12.2%, 일별 로그인·접속 각 2일, 다운로드 카드는 '데이터 없음'). `/survey/stats`는 등록 설문이 없어 선택 안내만 표시(카드 0개).
- **자동 검증:** `verify.cmd` → 테스트 338개 통과, 실패 0, JaCoCo 기준 충족, `BUILD SUCCESS`(3분 27초).
- **미수행:** 설문 응답이 있는 설문으로 `/survey/stats?surveyId=...` 카드 렌더 확인, 13.3인치 실기기 확인.

---

### 2.9. 2026-11-15: 통계 막대 색상 구분과 날짜 입력 폭 보정

- **변경:** ① 막대가 모두 같은 파랑이라 항목 구분이 안 되던 문제를 8색 팔레트 순환으로 고쳤다(ADTM 원본의 toastui-chart 팔레트 계열, 막대형은 `.stat-bar-col:nth-child`, 설문 비율형은 표 행 순서 기준). ② 날짜 입력이 `width:160px`에서 `2026-11-0`처럼 잘려 보이던 문제를 `input[type="date"].krds-input { min-width: 190px }` 한 규칙으로 보정했다(통계·감사로그처럼 인라인 폭을 준 화면 공통).
- **검증(ego-browser, 캐시 비활성):** `/stats/statsList`의 날짜 입력 2개 모두 190px, 값(`2026-11-09`·`2026-11-15`)이 잘리지 않음(scrollWidth 189 = clientWidth 189). 막대 색이 항목별로 다르게 계산됨(파랑·하늘·청록·주황·빨강). `/system/auditLogList`도 190px, 가로 넘침 0px.
- **자동 검증:** `verify.cmd` → 테스트 338개 통과, 실패 0, JaCoCo 기준 충족, `BUILD SUCCESS`(3분 08초).
- **미수행:** 13.3인치 실기기 확인.
- **참고:** 날짜 표시는 공통코드가 아니라 화면별 코드로 관리된다. 각 화면이 `<input type="date">`와 `#dates.format(..., 'yyyy-MM-dd')`를 직접 쓰고 있어 형식 규칙이 화면마다 흩어져 있다.

---

### 2.10. 2026-11-15: 메뉴 관리 정렬 보정과 메뉴 ID 규칙 통일

- **배경:** 하위메뉴 목록에서 메뉴ID(`ME0203110` + `0`)와 경로가 두 줄로 갈라지고 관리 버튼이 세로로 쌓였다. 메뉴 ID도 부모별 규칙 없이 흩어져 있었다(`ME02060000` 아래 `ME02031100`·`ME02031400`·`ME02031000`).
- **화면:** `system/menuList.html`에 colgroup(150/150/자동/70/90/150)을 두고 생성 셀에 `menu-id`·`menu-path`·`menu-ordr`·`menu-use`·`menu-actions` 클래스를 붙였다. 경로는 한 줄 말줄임 + `title` 툴팁, 트리는 `#jstree` 규격으로 행 높이(38px)·선택·호버를 정리했다(jsTree 기본 CSS가 페이지 안에서 나중에 로드되므로 `#jstree`로 우선순위를 올림).
- **ID 규칙:** 2단계 부모 `ME020X0000`의 자식을 순서대로 `ME020X0100`·`0200`… 으로 재번호했다. 부모가 바뀌는 5개 그룹(게시판·화면·일정·주소분류·보고) 20건이 대상이며, `seed.sql`·`MenuPermissionInterceptor`·`AuditInterceptor`·`MailServiceImpl`·`SmsServiceImpl`·`ChatHandshakeInterceptor`·`BoardNotificationEventListener`와 테스트 상수를 같은 매핑으로 치환했다(예: `ME02031100`→`ME02060100`).
- **DB:** `seed.sql`의 체크섬이 바뀌면 기존 DB는 기동을 거부하므로, 기존 `data/ArkiveDB` 파일을 `data/ArkiveDB-backup-20261115/`로 옮기고 새 DB로 초기화했다(기존 데모 데이터는 백업에만 있다).
- **검증(2026-11-15):** `verify.cmd` → 테스트 338개 통과, 실패 0, JaCoCo 기준 충족, `BUILD SUCCESS`(3분 17초). 기동 로그 `Initialized empty HSQLDB with schema.sql and seed.sql`. ego-browser로 `/system/menuList` 확인: 주소·분류관리 하위 ID가 `ME02060100`·`ME02060200`·`ME02060300`으로 순서대로 나오고, ID·경로가 한 줄로 표시되며 관리 버튼이 가로로 나열됨. 가로 넘침 0px.
- **미수행:** 13.3인치 실기기 확인(대체 viewport만 측정).

---

### 2.11. 2026-11-15: 메뉴 관리 화면 정리와 2단계 메뉴 경로 제거

- **배경:** 화면이 없는 묶음 메뉴(`시스템관리`·`업로드`)에 화면 경로가 남아 있었고, 메뉴 관리 화면이 투박했다.
- **변경:** `seed.sql`에서 `ME02010000`(시스템관리)·`ME02020000`(업로드)의 `MENU_COURS`를 NULL로 바꿔, 2단계(묶음) 메뉴에는 경로를 두지 않도록 통일했다. `menuList.html`은 두 패널에 제목·설명을 넣고, 사용여부를 Y/N 칩으로 보여주며, 빈 목록 안내를 별도 스타일로 처리한다. 공통 규격은 `krds-local.css`에 추가.
- **가로 스크롤 제거(3차):** 표의 기본 `min-width:720px`과 고정 px 열 폭 때문에 세로 스크롤바 폭만큼 가로 스크롤이 생겼다. `.menu-table table.tbl.col.data`를 `width:100%`·`min-width:0`으로 두고, colgroup을 비율(18/16/auto/8/11/18%)로 바꿔 패널 폭에 맞춰 줄어들게 했다. 측정값: 넓은 화면·533px 모두 표·트리 박스의 `scrollWidth - clientWidth = 0`, 페이지 가로 넘침 0.
- **도움말(4차):** 제목 아래 상시 노출되던 설명을 제목 옆 `?` 아이콘(`.menu-help`)으로 옮기고, hover/focus일 때만 말풍선(`::after` + `attr(data-tip)`)이 보이게 했다. ego-browser 검증: 기본 `opacity:0/visibility:hidden` → hover `1/visible` → 마우스 이탈 `0/hidden`.
- **레이아웃(2차):** 두 패널 높이를 맞추고(648/686px), 왼쪽 트리는 300px 고정·460px 영역, 오른쪽 표는 520px까지만 보이고 나머지는 내부 스크롤(헤더 sticky). 행 높이를 68→61px로 줄이고, 경로가 빈 메뉴는 `-` 자리표시자로 보여준다. 트리 기본 테두리를 없애고 연한 배경으로 바꾼 뒤 `width:100%`로 패널 폭을 채웠다.
- **DB:** 체크섬 때문에 기존 DB 파일을 `data/ArkiveDB-backup-20261115-b/`로 옮기고 새 DB로 초기화했다(개발용 admin·약관 시드만 있는 상태).
- **검증:** `verify.cmd` → 테스트 338개 통과, 실패 0, JaCoCo 기준 충족, `BUILD SUCCESS`(3분 22초). 기동 로그 `Initialized empty HSQLDB with schema.sql and seed.sql`. ego-browser로 `/system/menuList`를 열어 패널 제목·설명, Y 칩(`menu-chip on`), 2단계 메뉴 11행(`외부포털`·`내부포털`·`시스템관리`·`업로드` 포함)의 경로가 모두 공백인 것을 확인. 가로 넘침 0px.
- **미수행:** 13.3인치 실기기 확인.

---

### 2.12. 2026-11-15: 전 화면 도움말 ? 아이콘 전환

- **목적:** 메뉴 관리에만 있던 `?` 도움말을 화면 공통 규격으로 정리하고, 페이지 상단의 설명 문단을 제목 옆 `?` 아이콘 호버 방식으로 통일.
- **변경 파일:** `krds-local.css`(`.menu-help` → `.help-tip`), `system/menuList.html`, `system/auditLogList.html`, `stats/statsList.html`, `comm/sitemap.html`, `batch/batchList.html`, `board/boardViewStats.html`, `terms/termsList.html`, `login/termsAgree.html`, `login/totpSetup.html`, `login/apiDemo.html`, `survey/participateList.html`, `lecture/lectureList.html`, `lecture/myLecture.html`, `lecture/completionList.html`, `map/mapList.html`, `chat/chat.html`, `user/qna/qnaWrite.html`.
- **변경 내용:** 페이지 레벨 설명 문단 17개 화면(도움말 18개)을 `<span class="help-tip" data-tip="...">?</span>`로 제목(`h2`) 옆으로 옮겼다. 폼 필드 단위 12px 안내문과 조건부 운영 경고(`FFmpeg 인코딩 꺼짐`, `카카오 지도 키 미설정`)는 화면에 그대로 남긴다. 말풍선은 `::after` + `content: attr(data-tip)` CSS 전용이라 JS와 요청이 늘지 않는다.
- **GUI 검증 (ego-browser 0.6.3.14, 1280×720, 캐시 비활성):** 15개 URL에서 `::after`가 `opacity 0 / visibility hidden` → 호버 시 `1 / visible` → 마우스 이탈 시 `0 / hidden`. 말풍선 260px 폭이 제목 오른쪽에 표시되고 화면 밖으로 나가지 않으며, 각 화면 가로 넘침 0. 확인 URL: `/system/menuList`(도움말 2개), `/system/auditLogList`, `/stats/statsList`, `/batch/batchList`, `/sitemap`, `/lecture/lectureList`, `/map/mapList`, `/board/viewStats`, `/survey/participateList`, `/chat/chat`, `/terms`, `/user/qna/qnaWrite`, `/login/totpSetup`, `/login/apiDemo`.
- **증거:** `ego-browser-shot-545921.png`(배치관리, 호버 상태).
- **미확인/대상 아님:** `/login/termsAgree`는 이미 동의한 admin 계정에서 `/user/userMain`으로 리다이렉트되어 화면 미노출(미동의 계정 필요). `/qna/qnaWrite`(관리자 Q&A 등록)는 원래 설명 문구가 없어 `?` 대상이 아니다.
- **13.3인치 실기기 확인:** 미수행(실기기 미보유, 이번 변경은 제목 옆 아이콘 위치 이동뿐).
- **자동 검증:** `verify.cmd` → Tests run 338, Failures 0, Errors 0, JaCoCo 충족, BUILD SUCCESS(02:35 min).

---

### 2.13. 2026-11-16: 미확인 GUI 항목 추가 검증 (프로필 5)

- **도구:** ego-browser 0.6.3.14, 프로필 "프로필 5" (Profile 7), `http://127.0.0.1:8080`, admin 세션.
- **검증 결과:**
  - 실시간 알림 SSE: `/board/subscribe?boardType=ALL` 스트림 개방(장시간 연결, fetch 타임아웃으로 확인), `ARKIVE_SSE_ENABLED=false` 기본 킬스위치 확인.
  - 외부 웹분석: `window.ARKIVE_ANALYTICS` 주입 확인(enabled=false, ga4Id/clarityId 빈값, requireConsent=true). 기본 비활성·동의 전 외부 요청 없음.
  - ApplicationEvent 기반 로그: `/system/auditLogList` 렌더링 및 메뉴 접근(MENU_ACCESS) 기록 확인.
  - 내부 API: 미설정 시 `/internal/v1/health` → 503, 잘못된 토큰 → 503 동작 확인.
  - 동영상 인코딩: `/lecture/lectureList` 렌더링 및 "FFmpeg 인코딩 꺼짐" 상태 표시 확인.
  - SSO: `/sso/login` 기본 비활성 404 확인.
  - OAuth 2.0: 네이버 로그인 버튼 클릭 시 "준비중" 알림 확인(실제 연결 미구현).
  - DB 암호화: `ARKIVE_FIELD_KEY` 주입(32바이트 AES 키) 후 주소록 등록 → TEL_NO/EMAIL이 `enc:v1:` 접두사로 DB 저장 확인. UI 목록에서 `010-****-5678`·`te***@example.com` 마스킹 확인. FieldCryptoServiceTest(12) 통과.
- **미수행/제약:** 중복로그인방지·OAuth 2.0·SMS·Mail·중요글 알림 채널 분리·본인인증(NICE) 6개는 외부 서비스 계약·자격증명·독립 브라우저 세션이 없어 GUI/E2E를 수행하지 못했다. 항목별 GUI 확인 조건과 관련 테스트는 [README의 GUI 확인 대기 목록](../../../README.md#구현-자동-테스트-통과--gui-확인-대기)에서 관리한다.

---

### 2.14. 2026-11-18: 휴가관리 GUI 재검증 (Worker 수행 + 총괄 대조)

- **배경:** 휴가관리는 커밋 `6a7ad0d`에서 항목별 기록 없이 「구현 및 검증완료」로 옮겨졌다. [GUI 검증 분담](../common/ai-orchestration.md#61-gui-검증-분담-ego-browser) 첫 적용으로 다시 확인했다.
- **도구·환경:** ego-browser 0.6.3.14, `http://127.0.0.1:8080`(로컬 HSQLDB), admin 세션, 기준 SHA `c4614c7`. 시나리오 수행은 Freebuff Worker(DeepSeek V4.1 Flash, Dispatch `ctx_721fbb5df7ed`), 대조·판정은 총괄.
- **결과:**

| 단계 | URL | 입력 | 기대 | 실제 | 판정 |
| --- | --- | --- | --- | --- | --- |
| 1 | `/leave/balanceList?leaveYear=2026` | 조회, 행 없음 → admin/2026/15일 부여 | admin 연차 확인·없으면 부여 | "조회된 연차가 없습니다." → 15.0 / 사용 0.0 / 잔여 15.0 | 일치 |
| 2 | `/leave/myLeave` | 연차, 2026-12-02 하루, 사유 `GUI검증-휴가-시험2306` | 신청 성공, 대기 | 대기 행 추가, 카드 총 15.0 / 사용 0.0 / 잔여 14.0 | 일치 |
| 3 | `/leave/myLeave` | 2026-12-03, 사유 빈 값 | 오류, 신청 미생성 | alert "휴가 사유를 입력하세요.", 목록 1건 유지 | 일치 |
| 4 | `/leave/leaveList` | `LV20261118230638111148` 승인(확인창 수락) | 승인 | "승인 / 승인: 관리자", 승인·반려 버튼 사라짐 | 일치 |
| 5 | `/leave/balanceList`, `/leave/myLeave` | 조회 | 사용 +1 | 사용 0.0 → 1.0, 잔여 14.0 | 일치 |
| 6 | `/leave/myLeave` | 본인 취소(확인창 수락) | 취소, 사용 원복 | "취소", 사용 0.0 / 잔여 15.0 | 일치 |

- **총괄 대조:** Worker 스크린샷 01~12 중 01·06·07·09를 직접 열어 보고 수치와 일치함을 확인했다. 정리 후 상태를 총괄이 ego-browser로 다시 조회했다: `leaveList`·`myLeave` 해당 건 "취소", `balanceList` admin 2026 "0.0일 / 0.0일 / 0.0일". Worker는 png 외 파일을 수정하지 않았다(`git status`).
- **증거:** Worker 스크린샷 12장과 총괄 재조회 스크린샷 1장(`target/gui-evidence/leave/`)으로 대조했고, 판정 후 삭제했다. 판정 근거는 이 절의 표와 대조 결과로 남긴다.
- **관찰:** 잔여 연차는 대기 건을 미리 빼서 표시한다(신청 직후 사용 0.0인데 잔여 14.0). `LeaveServiceImpl`의 사용 가능 연차 계산(총 − 사용 − 대기 예약)과 같은 기준이며 결함으로 보지 않는다. 빈 사유 오류는 브라우저 기본 alert라 스크린샷에 보이지 않아, 문구는 dialog 이벤트로 기록했다.
- **남은 테스트 데이터:** 취소 상태 휴가 1건, admin 2026 연차 행(0.0일). 화면에 삭제 기능이 없어 남겼다(로컬 개발 DB).
- **미수행:** 반려 흐름, 잔여 연차 부족 시 신청·승인 차단, 일반 사용자 권한 화면, 13.3인치 실기기 확인.

---

### 2.15. 2026-11-18: A1 계정 묶음 GUI 재검증 (Worker 수행 + 총괄 대조)

- **대상:** 회원가입+아이디·비밀번호 찾기, 아이디 저장, 관리자 단일 로그인, 사용자 관리, 약관동의.
- **도구·환경:** ego-browser 0.6.3.14, `http://127.0.0.1:8080`, 기준 SHA `c4614c7`. 수행 Freebuff Worker(DeepSeek V4.1 Flash, Dispatch `ctx_defe583019a5`), 대조·판정 총괄. 테스트 계정 `guiuser1`(회원가입, 유지)·`guiuser2`(관리자 등록, 삭제).
- **결과:**

| 기능 | 단계와 실제 결과 | 판정 |
| --- | --- | --- |
| 회원가입 | 필수 약관 미동의 → "필수 약관에 모두 동의해야 가입할 수 있습니다." / 동의 후 → "가입되었습니다. 로그인하세요." / 같은 아이디 재가입 → "이미 등록된 아이디입니다." | 일치 |
| 아이디·비밀번호 찾기 | 이름+이메일 일치 → `gui****` / 불일치 → "일치하는 회원 정보가 없습니다." / 비밀번호 찾기는 일치·불일치 모두 같은 안내 문구(계정 존재 여부 노출 없음). 메일 수신은 메일 비활성이라 미확인 | 일치 |
| 아이디 저장 | 체크 후 로그인 → `localStorage.arkiveSavedId=guiuser1`, 재진입 시 아이디 채워짐 / 해제 후 → 값 삭제, 칸 비어 있음 | 일치 |
| 관리자 단일 로그인 | 같은 `/login/login`에서 guiuser1 → `/user/userMain`, admin → `/dashboard`. guiuser1로 `/auth/userList` 직접 접근 → `/login/login`으로 리다이렉트(차단 안내 화면은 없음) | 일치 |
| 사용자 관리 | 등록 → 목록 표시 / 이름 수정 → 상세에서 반영 확인(목록은 이름 마스킹 `G****`라 목록으로는 구분 불가) / 비밀번호 초기화 → 확인창 후 임시 비밀번호 alert / 삭제 → 목록에서 사라짐 | 일치 |
| 약관동의 | 관리자 등록 계정 첫 로그인 → `/login/termsAgree`(필수 2건 v1.0) / 미동의 제출 → "필수 약관에 모두 동의해야 합니다." / 동의 → `/user/userMain` / 재로그인 시 약관 화면 없음 | 일치 |

- **총괄 대조:** Worker 스크린샷 27장 중 05(아이디 찾기)·23(약관 화면)·27(최종 사용자 목록)을 직접 열어 보고 수치와 일치함을 확인했다. ego-browser로 직접 다시 확인: guiuser1 로그인 → `/user/userMain`, guiuser1의 `/auth/userList` 접근 → `/login/login`, admin 로그인 → `/dashboard`, 사용자 목록 `guiuser1`·`admin` 2건(guiuser2 없음).
- **관찰:** 다른 Worker가 같은 admin 계정으로 로그인하자 기존 admin 세션이 로그인 화면으로 튕겼다(2026-11-18 23:3x, H 묶음 Worker와 동시 실행 중). 중복로그인방지가 동작한 정황이지만, 시나리오로 재현·기록한 것은 아니어서 해당 항목의 GUI 확인으로 쓰지 않는다.
- **남은 테스트 데이터:** `guiuser1`(일반 사용자, 이후 묶음에서 사용). 증거 스크린샷은 대조·판정 후 삭제했다.
- **미수행:** 비밀번호 재설정 메일 수신, 13.3인치 실기기 확인.

---

### 2.16. 2026-11-18: 공통코드·카테고리 GUI 재검증 (Worker 수행 + 총괄 재현)

- **대상:** 공통코드 관리, 카테고리 설정(jstree형), 카테고리 설정(컬럼 드릴다운형).
- **도구·환경:** ego-browser 0.6.3.14, Worker는 `http://localhost:8080`(Dispatch `ctx_ef8279d920c7`), 총괄 재현은 `http://127.0.0.1:8080`. admin 계정.
- **공통코드 관리 (Worker 수행, 일치):** 마스터 `GUIH_TEST` 등록 → "저장되었습니다." / 상세 `01` 등록 → 상세 목록 표시 / 같은 마스터 재등록 → "이미 등록된 코드입니다." / 상세 이름 수정 → 반영 / 마스터 삭제 → "마스터와 상세 전체를 삭제하시겠습니까?" 확인 후 "등록된 마스터가 없습니다.". 총괄이 스크린샷(01 상세 저장·수정)을 확인하고, 정리 후 목록에 `GUIH_TEST`가 없음을 직접 조회했다.
- **카테고리 (Worker 보고는 불일치, 총괄 재현 결과 정상):**
  - Worker는 "하위추가 후 트리에 하위가 안 보임", "드릴다운 하위 건수 0"을 불일치로 보고했다.
  - 총괄 재현: 트리는 서버가 `<details>`로 렌더링하고 기본으로 접혀 있다(▶). 하위 추가 후 DOM에 하위 노드가 있고, ▶를 펼치면 `총괄검증 하위`가 표시된다. 저장 성공 시 알림 없이 새로고침하는 것은 `categoryTree.html`의 정상 동작이다. 드릴다운의 `0`은 **순서** 열 값이며(열 제목: 순서·이름·하위·관리) 하위 건수가 아니다. Worker 쪽 하위 2건 중복은 Worker가 저장을 두 번 누른 결과다.
  - 총괄이 추가로 확인한 것: 트리 `수정`으로 이름 변경 → 반영, 드릴다운 `이름변경`(prompt) → 반영, 하위가 있는 상위 삭제 → "하위 카테고리가 있어 삭제할 수 없습니다. 하위부터 삭제하세요."(Worker), 하위 → 상위 순 삭제 후 "등록된 카테고리가 없습니다."
- **개선 여지(결함 아님):** 트리가 기본으로 접혀 있어 하위 추가 직후 결과가 바로 보이지 않는다. 추가한 노드의 상위를 펼친 상태로 다시 그리면 사용성이 좋아진다.
- **남은 테스트 데이터:** 없음. 증거 스크린샷은 대조·판정 후 삭제했다.
- **미수행:** 13.3인치 실기기 확인. 같은 묶음의 통계·엑셀·Spring Batch는 미수행(README 재검증 대기 유지).

---

### 2.17. 2026-11-22: Supabase(PostgreSQL) 접속·스키마 적용 검증

- **대상:** `application.properties`의 PostgreSQL 항목(`ARKIVE_DB_TYPE=postgresql` 전환)과 `db/postgres/schema.sql`·`seed.sql`. 실제 관리형 PostgreSQL(Supabase, PostgreSQL 17.12, ap-southeast-1)에 적용.
- **수행·결과:**
  - 접속: 직접 연결 호스트는 IPv6 전용(`2406:...`)이었고 이 PC는 IPv4만이라 JVM 이름 해석 단계에서 실패(`UnknownHostException`). Session pooler(`aws-1-ap-southeast-1.pooler.supabase.com:5432`, `postgres.<ref>`)로 연결 성공(약 2.1초).
  - 스키마: 105개 구문 성공, 오류 0. `public` 테이블 54개 확인. 변환은 `CLOB`→`TEXT`만.
  - 시드: 8개 INSERT 성공, 행 수 `MENU` 63 · `APP_USER` 1 · `APP_ROLE` 2 · `APP_USER_ROLE` 1 · `APP_ROLE_MENU` 92 · `IDS` 1 · `TERMS` 3. `CHAR(10)`→`chr(10)` 변환 확인.
  - 호환성 사전 확인: 컬럼명 `POSITION` 무인용 사용 가능, `numeric(5,1)`·`chr(10)` 동작, `MERGE INTO … USING (VALUES …) source(col,col)` 3개 매퍼 구문을 PG 17 임시 테이블로 재현해 멱등 동작 확인(1행 유지).
  - 앱: `ARKIVE_DB_TYPE=postgresql`로 기동(약 27초), `/`·`/login`·`/board/list` 요청 시 `PSQLException`·`relation … does not exist` 0건(스키마 적용 전에는 같은 요청에서 `relation "menu" does not exist`가 발생했다).
  - `verify.cmd`: 테스트 338개 통과(실패·오류 0), JaCoCo 기준 충족, BUILD SUCCESS.
- **환경 메모:** 이전 검증용 JVM이 8080과 `target/Arkive-1.0.0.jar`를 점유해 `mvn clean`이 실패했다. `jps -l`로 해당 PID를 찾아 종료한 뒤 재실행했다.
- **GUI 확인(ego-browser 0.6.3.17, 2026-11-22, postgresql 모드):** `admin`/`Admin1234!`로 세션 로그인 → 첫 로그인 약관 동의 2건 체크 후 `/user/userMain` 진입. 확인 화면: `/system/menuList`(2행), `/auth/roleList`(2행), `/auth/userList`(1행), `/board/ckeditorList`, `/qna/qnaList`, `/schedule/scheduleList`(6행), `/message/inbox`, `/banner/bannerList`, `/popup/popupList`, `/stats/statsList`(6행), `/leave/leaveList`, `/system/auditLogList`(11행). 모두 오류 페이지 없이 렌더되고 한글이 정상 표시됐다.
- **쓰기 경로 확인:** `/system/auditLogList`에 화면 접근마다 `AUDIT_LOG` INSERT가 `MENU_ACCESS`·`PII_VIEW`·`SUCCESS`로 기록되어 PostgreSQL INSERT가 동작함을 확인했다.
- **수정: 로그 시각 9시간 차이.** PG 세션 `TimeZone`이 UTC라 `CURRENT_TIMESTAMP`가 UTC로 저장됐다. 접속 URL의 `TimeZone`·`options=-c timezone=...` 파라미터는 드라이버·pooler 단계에서 모두 무시됐고(3가지 방식 모두 `show timezone`이 UTC), `SET TIME ZONE 'Asia/Seoul'`만 통했다. 그래서 `EgovConfigDatasource`가 postgresql일 때 DBCP2 `connectionInitSqls`로 `SET TIME ZONE`을 실행한다. 값은 `Globals.postgresql.TimeZone`(`ARKIVE_DB_TIMEZONE`, 기본 `Asia/Seoul`)이다.
- **수정 후 확인:** GUI로 admin 로그인 → `/system/auditLogList`에서 새 로그 일시가 `2026-11-22 16:25:37`(KST 16:25)으로 기록됨. 테스트 338개 통과·BUILD SUCCESS. 수정 전에 저장된 로그 3행(07:1x·07:20)은 UTC 값 그대로 남아 있다.
- **오탐 정정:** 처음 시도한 `/system/userList`·`/board/list`는 존재하지 않는 경로라 오류 페이지었고, 실제 경로는 `/auth/userList`·`/board/ckeditorList`다.
- **미수행:** 등록·수정 같은 업무 쓰기 시나리오, 13.3인치 화면 확인.
- **2026-11-22 후속 정리:** 별도 프로파일 파일(`application-postgres.properties`)을 없애고 `application.properties`에 합쳤다. 전환은 `Globals.DbType=${ARKIVE_DB_TYPE:hsql_server}`로 하고 값은 `ARKIVE_DB_*` 환경변수로 받는다. 같은 파일의 한글 주석 38줄이 `\uXXXX` 이스케이프로 저장돼 읽기 어려웠으므로 주석만 UTF-8 한글로 복원했다(값 줄은 무변경, `git diff`로 확인). 인코딩은 `server.servlet.encoding.*`(Boot 3 키) + `spring.messages.encoding=UTF-8`을 추가했는데, 처음 넣은 `force-response`(=`force=true`)가 테스트 `LectureControllerTest`의 `Content-Type: video/mp4`에 `;charset=UTF-8`을 불여 실패했으므로 `force-request=true`만 남겼다. 재검증: 테스트 338개 통과·BUILD SUCCESS, `ARKIVE_DB_TYPE=postgresql`과 기본 HSQL 두 모드 모두 기동 후 `/` 응답 `text/html;charset=UTF-8`과 한글 표시 확인, SQL 오류 0건.
- **남은 제약:** 접속 비밀번호는 환경변수로만 주입한다. Free 플랜은 7일 저활동 시 일시정지되고 자동 백업이 없으며 `DBJob` SCRIPT 덤프가 동작하지 않으므로 백업은 `pg_dump` 등으로 따로 수행해야 한다.

---

### 2.18. 2026-11-22: HikariCP전환PostgreSQL기본값검증

<!--
문서 상태: 부분 미완성
미완성 항목: 백업 복원 검증, PostgreSQL 전체 업무 시나리오·변이 테스트. HikariCP 변경 후 실제 Supabase 접속·Q&A 답변·공휴일 쓰기는 후속 2.19절에서 확인했다.
필요한 근거/확인자: 총괄의 verify.cmd 결과·ego-browser 증거, 접속 환경변수와 데이터 보존/이관 범위에 대한 사용자 확인.
완성 방법: 테스트를 HSQLDB로 격리해 전체 검증하고 기동·GUI를 확인한다. 실제 Supabase 쓰기·복원 검증은 대상과 복구 방법을 확인한 뒤 별도로 수행한다.
완료 기준: 실행 결과·근거·잔여 위험이 기록되고 실제 PostgreSQL 검증과 HSQLDB 회귀 검증이 구분되어 있다.
-->

- **범위·승인:** 사용자 요청으로 PostgreSQL을 기본 DB, HikariCP를 공통 연결 풀로 변경한다. HSQLDB는 수동 예비 실행·자동 테스트용으로 유지한다. Data API/SDK 추가, Supabase DDL·데이터 이관·권한 변경, 자동 폴백·자동 PostgreSQL 백업 구현은 이번 범위가 아니다.
- **기준 버전:** HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 미커밋 Supabase 설정 변경. 기존 사용자 변경·DB 파일은 보존했으며 커밋·푸시는 하지 않았다.
- **작업 배정:** Orca Run `run_919fd83e10be`, Freebuff Dispatch `ctx_aeb32e52431c`. Worker는 허용된 소스 5개만 변경하고 정적 점검을 보고했다. 총괄은 별도 사용자 승인을 받아 테스트 준비로 `application-test.properties`에 `Globals.DbType=hsql_server` 한 줄만 추가했으며 테스트 코드·기대값은 변경하지 않았다.
- **모델 예외:** 기본 정책은 Flash·high(2x), 새 세션당 일일 2시간 차감이다. 이번 Worker는 시작 직후 Flash Fast·high 표시를 확인했고 기존 2x 세션을 이어받지 못한 정황이 있었다. 사용자에게 알린 뒤 이미 시작된 이번 세션만 계속 활용하라는 승인을 받았다. 다음부터 Flash 세션을 먼저 준비·확인한다. 일일 실제 차감량의 전후 대조는 미확인이다.
- **핵심 변경:**
  - `pom.xml`: DBCP2 직접 의존성을 Boot parent 관리 HikariCP로 교체. `EgovConfigDatasource.java`: `HikariConfig`/`HikariDataSource`, bean alias 3개·close lifecycle·HSQL 최초 초기화·PostgreSQL `SET TIME ZONE` 유지. 유효성 검사는 JDBC4 `Connection.isValid()` 기본값을 사용한다.
  - `application.properties`: `Globals.DbType=${ARKIVE_DB_TYPE:postgresql}`, `Globals.Hikari.*` 풀 초기값 10/2/10000ms/3000ms/600000ms/1800000ms. 연결 누락·실패 시 HSQLDB 자동 전환 없음. 대상 DB/pooler 실제 연결 한도는 미확인이다.
  - `Qna_SQL.xml`·`Schedule_SQL.xml`: MERGE의 `UPDATE SET target.컬럼` 대상 수식어를 제거한다. `source.컬럼`·ON 조건·업무 의미는 유지한다. [PostgreSQL 17 MERGE](https://www.postgresql.org/docs/17/sql-merge.html)의 대상 컬럼 규칙에 맞춘 변경이다.
- **기존 기록과 구분:** 2.17절의 MERGE 임시 테이블 재현은 당시 현행 Mapper 전체의 PostgreSQL 호환성 증거가 아니다. 이번 수정본의 실제 PostgreSQL 업무 쓰기는 별도 확인이 필요하다.
- **전체 자동 검증: 통과.** 총괄이 `ARKIVE_DB_TYPE=postgresql`을 지정한 상태에서 `verify.cmd`를 독점 실행하여 exit 0, 테스트 338개·실패 0·오류 0·skip 0, JaCoCo 기준 충족, BUILD SUCCESS를 확인했다(2분 48초). 로그의 HSQLDB 9002 기동 및 `HikariPool-1 - Start completed`로 실제 테스트 DB 격리와 Hikari 풀 사용을 확인했다. 근거: 외부 임시 `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-hikari-verify.log`.
- **설정 해석·패키징: 통과.** Spring `ResourcePropertySource`로 기본 DB `postgresql`, `ARKIVE_DB_TYPE=postgresql`이 있을 때 테스트 DB `hsql_server`·9002·테스트 타입 고정 여부 true를 확인했다. 패키징된 JAR에 Spring Boot 3.5.6·HikariCP 6.3.3·PostgreSQL JDBC 42.7.3·HSQLDB 2.7.4 포함, DBCP2 JAR 미포함을 확인했다. 근거: 외부 임시 `arkive-hikari-property-review.jsh`, `target/Arkive-1.0.0.jar` ZIP 엔트리 확인. 테스트 파일은 승인된 DB 타입 한 줄 이외 변경 없음.
- **HSQLDB Runtime/GUI: 통과.** 위 검증으로 패키징한 JAR를 `Globals.DbType=hsql_server`, 웹 8080·DB 9003, 외부 임시 경로 `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-hikari-runtime-20261122/ArkiveDB`로 실행했다. Quartz 자동 시작을 꺼 기존 `data/`·`backup/`와 Supabase를 변경하지 않았다. HikariCP 기동·빈 DB 초기화·앱 기동(27.463초)을 확인했다. ego-browser의 기존 작업 공간에서 admin 세션으로 아래 흐름을 수행했다.

  | 단계 | URL·입력 | 기대·실제 결과 | 판정 |
  | --- | --- | --- | --- |
  | 로그인·약관 | `/login/login` → `/login/termsAgree`, 필수 2건 동의 | 로그인·동의 성공, `/user/userMain` 진입 | 통과 |
  | 질문 등록 | `/qna/qnaWrite`, `HIKARI_GUI_20261122 질문` | 목록에 질문 표시, `BOARD` INSERT 1행 | 통과 |
  | 답변 등록·수정 | `/qna/qnaDetail`, `HIKARI 답변 등록` → `HIKARI 답변 수정` | 저장 후 상세에 반영, 수정본 MERGE의 INSERT·UPDATE 각각 1행 | 통과 |
  | Q&A 정리 | 상세의 삭제 버튼 | 질문 삭제 1행·답변 cascade 삭제, 목록 0건 | 통과 |
  | 공휴일 등록·수정 | `/schedule/holidayList`, `2026-11-30`, `HIKARI 공휴일 등록` → `HIKARI 공휴일 수정` | 같은 날짜 1행 유지·이름 갱신, 수정본 MERGE 각각 1행 | 통과 |
  | 공휴일 정리 | `data-de=20261130` 삭제 버튼 | 삭제 1행, 목록 0건 | 통과 |

- **근거·재개 대조:** 이전 세션의 `arkive-hikari-gui-20261122/qna-answer-updated.png`·`holiday-updated.png`를 직접 열어 확인하고, Runtime `stdout.log`의 Mapper SQL·변경 행 수와 `ArkiveDB.log`의 INSERT·UPDATE·DELETE 기록을 대조했다(위 경로는 모두 `C:/Users/kdh/AppData/Local/Temp/opencode/` 아래). 재개 세션에서 ego-browser로 `/schedule/holidayList`의 "조회된 공휴일이 없습니다."와 `/qna/qnaList`의 "조회된 Q&A가 없습니다."를 직접 확인했다. 임시 질문·답변·공휴일은 정리됐고 약관 동의·감사 로그는 격리된 임시 DB에 남는다. 브라우저 작업 공간은 검증 후 종료했다.
- **화면 조건·관찰:** 대체 viewport 1080×563 CSS px, DPR 1.5. 화면/CSS 수정은 없으며 13.3인치 실기기 확인은 미수행이다. HSQLDB 드라이버의 `setNetworkTimeout` 미지원 예외가 log4jdbc에서 ERROR로 기록되지만 Hikari는 미지원으로 처리한 뒤 정상 기동했다. 이 기록을 근거로 전체 로그가 오류 0건이라고 표현하지 않는다.
- **재개 세션 검증:** `arkive-hikari-verify.log`의 테스트 338개·실패/오류/skip 0, JaCoCo 통과·BUILD SUCCESS(16:43:44 KST)를 직접 확인했다. 관련 소스·설정 수정 시각은 모두 이 실행 이전이며, 재개 세션에서는 Markdown 기록만 수정했으므로 `verify.cmd` 재실행은 문서 전용 예외로 생략했다. 변경 문서의 링크·앵커를 확인했다. 저장소 전체 `git diff --check`는 기존 `pom.xml` 변경 줄의 공백 경고로 exit 1이며, 이번 범위 밖 소스는 수정하지 않았다. 기존 소스·테스트·미커밋 변경은 그대로 보존했다.
- **당시 실제 PostgreSQL 검증: 미수행.** 이번 셸에는 `ARKIVE_DB_URL`·`ARKIVE_DB_USER`·`ARKIVE_DB_PASSWORD`가 없어 HikariCP 전환 후 실제 Supabase 재접속·업무 쓰기는 수행하지 않았다. 접속 정보는 비밀값을 출력하지 않고 존재 여부만 확인했다. 위 HSQLDB 결과와 2.17절의 이전 DBCP2 검증을 PostgreSQL 변경 후 쓰기 검증으로 간주하지 않는다. 이후 사용자가 저장한 비밀번호를 재사용하여 수행한 실제 PostgreSQL 검증은 2.19절에 기록한다.
- **남은 작업:** 보존할 기본 계정·추가 메뉴 범위를 확인한 뒤 기존 Supabase 시드와 대조하여 필요한 데이터만 이관한다. `pg_dump`·복원 검증, Data API 공개·RLS/권한 확인, PostgreSQL 전체 업무 시나리오·변이 테스트는 미수행이다. HSQLDB는 PostgreSQL 데이터 백업/복제본이 아니다.

---

### 2.19. 2026-11-22: Supabase HikariCP 실접속·업무 쓰기 검증

> 이 절은 당시 검증 이력이다. 현재 DB 주소·계정·비밀번호·드라이버·시간대는 아래 2.20절의 사용자 요청에 따라 `application.properties`의 직접 입력값을 사용한다. 이전 로컬 암호문/환경변수 주입은 현재 실행에 사용하지 않는다.

<!--
문서 상태: 부분 미완성
미완성 항목: PostgreSQL 전체 업무·권한 회귀, 변이 테스트, pg_dump 백업/복원, Data API 전체 노출·RLS 정책 검증, 13.3인치 실기기 확인.
필요한 근거/확인자: 각 추가 시나리오의 실행 증거, 사용자/운영 담당자의 데이터 보존·복원 대상 및 권한 정책 확인.
완성 방법: 아래 접속·쓰기 검증과 분리해 추가 범위를 승인받고 검증한다. 기존 데이터·권한·비밀번호는 임의로 변경하지 않는다.
완료 기준: 이번 범위의 실행 결과와 미수행 범위가 구분되어 있고 검증 레코드 정리가 확인되어 있다.
-->

- **범위·기준:** 사용자의 "다 해보고 필요한 것만 요청" 및 저장한 비밀번호 재확인 요청에 따라 기존 비밀번호로 접속 설정 보정·앱 기동·Q&A 답변과 공휴일 쓰기를 검증했다. HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 보존한 기존 미커밋 변경. 기존 Worker/원본 세션은 재개하지 않았고 이번 후속 작업은 단독 수행했다. 커밋·푸시 없음.
- **접속 정보 확인·보정:** 사용자가 `application.properties`에 넣은 DB 비밀번호는 실제 접속에 사용할 수 있었다. URL의 HTTPS API 주소와 UserName의 publishable API 키는 JDBC 접속 정보가 아니므로 세 항목을 `ARKIVE_DB_URL`·`ARKIVE_DB_USER`·`ARKIVE_DB_PASSWORD` 환경변수 참조로 정리했다. 검증한 Session pooler `aws-1-ap-southeast-1.pooler.supabase.com:5432` + `sslmode=require`, `postgres.<project-ref>` 계정을 재사용했다. 비밀번호 재설정·Supabase CLI login/init/link·DDL/시드 재적용은 하지 않았다.
- **로컬 비밀번호 관리:** `.settings/arkive-db-password.dpapi`에 현재 Windows 사용자 DPAPI 암호문을 저장하고 복호화 왕복 일치를 확인했다. `.settings/arkive-run.ps1`은 이를 프로세스 환경변수로 주입해 실행한다(기본 `mvn spring-boot:run`, `-Jar`는 패키징된 JAR, `-Verify`는 `verify.cmd`, `-LoadOnly`는 dot-source용). 두 파일은 기존 `.gitignore`의 `.settings/`로 Git 제외되며 다른 PC·사용자에게 복사하는 범용 비밀관리 방식이 아니다. 실제 비밀번호는 소스/패키징된 properties·검증 로그·Runtime 로그에 없음을 값 출력 없이 확인했다. 직접 Maven/IDE 실행에는 별도 환경변수 주입이 필요하다.
- **전체 자동 검증: 통과.** PostgreSQL 실행 환경변수를 주입한 상태에서 `verify.cmd` exit 0, 테스트 338개·실패 0·오류 0·skip 0, JaCoCo 기준 충족, BUILD SUCCESS(3분 6초, `2026-11-22T17:25:05+09:00`). 테스트 DB는 `application-test.properties`의 격리 HSQLDB이며 이 결과를 PostgreSQL 자동 통합 테스트로 표현하지 않는다. 테스트 파일은 이번 후속 작업에서 수정하지 않았다.
- **실제 앱 기동: 통과.** Java 17.0.20·Spring Boot 3.5.6·HikariCP 6.3.3·PostgreSQL JDBC 42.7.3. 패키징한 JAR SHA-256 `BED2A0321EB52888EBFB5BFF7392BB18AAF421C48667A247A7C3C6034B04D0E6`. 웹 8080, `ARKIVE_DB_TYPE=postgresql`, JDBC 직접 드라이버 사용. Runtime 로그에서 `HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection`·`Start completed`와 앱 기동 25.527초를 확인했다. Quartz 자동 기동·메일·SMS·ES는 검증 중 비활성화해 외부 발송·예약 작업을 피했다. 기존 HSQLDB `data/`를 사용하지 않았다.
- **DB 읽기 확인:** 실제 Supabase PostgreSQL 17.12, database `postgres`, schema `public`, 테이블 54개. 기존 admin 1행·메뉴 63행·역할 2행·역할-메뉴 92행을 확인했고 재적재·수정하지 않았다. JDBC 읽기 probe가 `SET TIME ZONE 'Asia/Seoul'`을 직접 수행한 결과와 앱의 Hikari 초기화 결과는 구분한다. 앱 답변 MERGE가 저장한 `ANSWER_DT=2026-11-22 17:28:23.525211`이 해당 Runtime 실행 시각 17:28 KST와 일치해 앱 쓰기의 시간대를 확인했다.
- **GUI/업무 쓰기: 통과.** ego-browser 0.6.3.17, 작업 공간 6, `http://localhost:8080`, 기존 admin 세션으로 아래 경로를 수행했다. 기존 약관 동의가 있어 재동의 없이 `/dashboard`로 로그인했다.

  | 단계 | URL·입력 | 기대·실제 결과 | 판정 |
  | --- | --- | --- | --- |
  | 질문 등록 | `/qna/qnaWrite`, `SUPABASE_HIKARI_GUI_20261122 질문`, `INTERNAL` | `BOARD` INSERT 1행, 목록에 내부·미답변 표시 | 통과 |
  | 답변 등록 | `/qna/qnaDetail`, `SUPABASE HIKARI 답변 등록 20261122` | 수정된 MERGE INSERT 1행, 상세에 저장값 반영 | 통과 |
  | 답변 수정 | 같은 상세, `SUPABASE HIKARI 답변 수정 20261122` | MERGE UPDATE 1행, API HTTP 200, DB 답변 1행·수정값 일치·한국 시각 확인 | 통과 |
  | 공휴일 등록·수정 | `/schedule/holidayList?yyyymm=209911`, `2099-11-22`, `SUPABASE HIKARI 공휴일 등록` → `SUPABASE HIKARI 공휴일 수정` | 각 MERGE 1행, UPDATE API HTTP 200, 같은 날짜 DB/화면 1행 유지·이름 일치 | 통과 |
  | 검증 데이터 정리 | 해당 날짜 삭제 버튼, 질문 상세 삭제 버튼 | 각 DELETE 1행, 질문 삭제의 FK cascade로 답변 제거, 두 목록 0건 | 통과 |

- **정리 결과:** 읽기 probe로 질문 `BD20261122172724552726`·답변·공휴일 `20991122` 모두 0행을 확인했다. BOARD 전체 0행·해당 공휴일 0행으로 사전 기준과 일치한다. admin·메뉴·역할·권한 행 수는 그대로다. 로그인/화면 접근에 따른 정상 감사 로그는 보존했고 기존 동의·로그·DB 데이터는 삭제하지 않았다. GUI 작업 공간은 종료했다.
- **증거:** `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-supabase-final-20261122/`의 `verify-final.log`, `runtime-stdout.log`·`runtime-stderr.log`, `round-03.mjs`~`round-16-finish.mjs`, `verification-records.json`, `Probe.ps1`·`DbProbe.java`·`db-final-check.log`, 직접 열어 확인한 `qna-answer-updated.png`·`holiday-updated.png`. 화면 입력 → Mapper MERGE·행 수 → 실제 DB 저장값 → GUI/DB 삭제 결과를 대조했다. Supabase Dashboard는 로그인 화면으로 이동했으나 기존 검증 호스트의 DB 인증 성공으로 작업을 계속했으며 대시보드 로그인 우회는 하지 않았다.
- **관찰·제약:** 공휴일 수정 라운드의 브라우저 예외/console error 0건, 해당 앱 Runtime 로그의 ERROR/Exception 0건·stderr 0바이트. 대체 viewport 1080×563 CSS px, DPR 1.5이며 화면/CSS 변경은 없다. 13.3인치 실기기 검증은 미수행이다. Q&A 전체페이지 스크린샷에는 고정 헤더가 중간에 잡힌 캡처 한계가 있어 반응형 품질 근거로 사용하지 않는다. 최초 로그인 대기식이 실제 `/dashboard`를 포함하지 않아 timeout, 삭제 후 대기식의 일시적 `document.body=null` 때문에 스크립트 오류가 발생했지만 현재 화면을 확인하고 대기만 보정했으며 로그인/DELETE를 중복 실행하지 않았다.
- **보안 읽기 점검·남은 범위:** `public` 일반 테이블 중 RLS가 없으면서 anon 역할에 SELECT 권한이 있는 테이블은 카탈로그 조회상 0개였다. 이것만으로 Data API 전체 노출·authenticated 역할·RLS 정책 검증 완료라고 판단하지 않는다. `pg_dump`/복원·전체 PostgreSQL 업무/권한 회귀·변이 테스트는 미수행이며 비밀번호·권한·기존 데이터 변경은 별도 승인 범위로 남긴다.
- **후속 변경 파일·최종 점검:** `src/main/resources/application.properties`, `README.md`, `docs/ai/project/database-schema.md`·`project-context.md`·`verification-log.md`, 로컬 Git 제외 `.settings/arkive-run.ps1`·`arkive-db-password.dpapi`. 후속 변경 범위의 `git diff --check` 통과, 새 검증 링크/제목 앵커 5건과 실행기 PowerShell 문법을 확인했다. 저장소 전체 `git diff --check`는 기존 `pom.xml` 공백 경고 때문에 exit 1이며 이번에 수정하지 않았다. `verify.cmd` 이후에는 프로젝트 Markdown만 추가 수정했으므로 전체 빌드는 재실행하지 않았다. 브라우저는 닫았고 검증용 앱 JVM PID 12592(웹 8080, Quartz/외부 연동 비활성)는 사용자 확인용으로 실행 상태를 유지했다.

---

### 2.20. 2026-11-22: DB 접속 설정의 properties 직접 편집 복원

<!--
문서 상태: 완료
확인 근거: 아래 verify.cmd 결과, 직접 JDBC 읽기 접속, 별도 자격증명 주입 없는 HikariCP 앱 기동 및 ego-browser 관리자 로그인·목록 조회 증거.
확인 범위: 사용자가 입력한 properties의 직접 사용과 실행기 자격증명 주입 제거. 2.19절의 전체 PostgreSQL 업무·권한 회귀, 백업/복원 등 미수행 항목을 완료한 것으로 보지 않는다.
완료 기준: 주소·계정·비밀번호·드라이버·시간대가 properties에서 직접 수정 가능하고 실행기가 해당 값을 덮어쓰지 않으며 빌드·접속 검증 결과와 비밀번호 공유 금지 주의가 기록되어 있다.
-->

- **사용자 요청·기준:** DB 정보를 다음에 직접 변경할 수 있도록 properties에 저장하라는 요청을 적용했다. 처음 주소·계정만 복원했으나 사용자가 비밀번호도 다시 직접 입력한 뒤 "그걸 써서 다시 확인, 굳이 어렵게 하지 말 것"을 요청하여 모든 로컬 접속 값을 그대로 사용한다. HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 미커밋 변경 보존. 입력한 비밀번호를 덮어쓰거나 재설정하지 않았고 DB 데이터·권한·업무 코드는 변경하지 않았다.
- **현재 수정 위치:** `src/main/resources/application.properties`의 `Globals.postgresql.DriverClassName`, `Url`, `UserName`, `Password`, `TimeZone`에 실제 설정값을 둔다. 주소는 검증된 Session pooler 5432 + `sslmode=require`, 계정은 해당 프로젝트의 `postgres.<ref>`, 드라이버는 `org.postgresql.Driver`, 시간대는 `Asia/Seoul`이다. 값을 직접 수정한 뒤 일반 `mvn spring-boot:run`으로 실행/재시작하면 적용된다. `ARKIVE_DB_URL/USER/PASSWORD/DRIVER/TIMEZONE`를 이 항목에서 참조하지 않으며 `ARKIVE_DB_TYPE`의 기본값은 postgresql을 유지한다.
- **비밀번호·안전 주의:** 사용자가 properties에 입력한 비밀번호를 그대로 보존했다. `.settings/arkive-run.ps1`의 암호문 읽기·비밀번호 주입도 제거하여 선택 실행기로만 남는다. 기존 `.settings/arkive-db-password.dpapi`는 현재 사용하지 않지만 임의 삭제하지 않았다. 실제 비밀번호가 들어 있는 로컬 properties는 공유·커밋하지 않으며 배포/공유 전 비밀번호를 제거해야 한다. 패키징된 JAR에도 이 properties가 포함되므로 현재 JAR 역시 공유하지 않는다. 원문 비밀번호는 도구 출력·문서·로그에 적지 않는다.
- **변경 파일:** 위 properties·로컬 실행기와 `docs/ai/project/project-context.md`·`database-schema.md`·`verification-log.md`. 테스트 파일·공통 Java·Mapper/XML·SQL·이전 사용자 변경은 수정하지 않았다.
- **초기 복원 단계 검증 이력:** 주소·계정을 먼저 복원한 단계에서 Spring 설정 해석·PowerShell 문법·변경 파일 공백 검사를 통과했다. 비밀번호 외 네 `ARKIVE_DB_*`에 다른 값을 넣어도 properties 값이 유지됨을 확인했다. 당시 `verify.cmd` exit 0, 테스트 338개·실패/오류/skip 0, JaCoCo·BUILD SUCCESS(2분 56초, `2026-11-22T17:43:20+09:00`). 비밀번호만 주입한 JAR PID 17416에서 Hikari의 PgConnection·Start completed·앱 기동 26.335초, ego-browser 작업 공간 7의 admin 로그인 → `/dashboard` → 빈 Q&A/공휴일 목록을 확인했다. 이 결과는 아래 직접 비밀번호 기준 재검증과 구분한다.
- **사용자 입력값 직접 접속: 통과.** Boot `PropertiesPropertySourceLoader`로 현재 properties의 직접 입력값을 읽고 모든 `ARKIVE_DB_*` 자격증명/라우팅 환경변수와 DPAPI 사용 없이 JDBC 읽기 접속에 성공했다. PostgreSQL 17.12·database `postgres`·schema `public`·테이블 54개·BOARD 0행·검증용 공휴일 0행, 설정된 Asia/Seoul을 확인했다. 비밀번호는 출력하지 않았다.
- **직접 실행 전체 자동 검증: 통과.** 사용자 입력값을 포함한 현재 구성으로 `verify.cmd` exit 0, 테스트 338개·실패 0·오류 0·skip 0, JaCoCo 기준 충족·BUILD SUCCESS(2분 9초, `2026-11-22T17:50:11+09:00`). 자동 테스트는 기존 격리 HSQLDB이며 PostgreSQL 전체 자동 통합 테스트로 표현하지 않는다. 테스트 파일은 변경하지 않았다. 이전에 직접 띄운 검증용 PID 12592/17416만 확인 후 종료하여 Windows의 JAR 점유를 해제했고 다른 Java 프로세스는 종료하지 않았다.
- **직접 실행 Runtime/GUI: 통과.** `ARKIVE_DB_PASSWORD/TYPE/URL/USER/DRIVER/TIMEZONE` 환경변수를 검증 프로세스에서 제거하고 로컬 실행기·DPAPI 없이 JAR를 실행했다. PID 18768·웹 8080에서 Hikari의 `Added connection org.postgresql.jdbc.PgConnection`·`Start completed`, 앱 기동 19.364초를 확인했다. ego-browser 작업 공간 7에서 관리자 로그인 → `/dashboard` → `/qna/qnaList`의 빈 목록 → `/schedule/holidayList?yyyymm=209911`의 빈 목록을 확인했고 저장한 스크린샷을 직접 열어 대조했다. 이 재확인은 접속·로그인·읽기 범위이며 2.19절의 등록/수정/삭제 시나리오를 반복 수행하지 않았다. 업무 데이터·권한은 변경하지 않았고 정상 로그인/조회 감사 로그는 보존했다.
- **패키지·누출 점검:** JAR SHA-256 `519998587E51DD5B55D31D9E064DD3948649EF1F408BBCB3F22371B66A840AC6`. 패키징된 비밀번호와 현재 properties 입력값이 일치하고 다섯 접속 항목이 환경변수를 참조하지 않음을 원문 출력 없이 확인했다. `verify-direct.log`·`direct-runtime-stdout.log`·`direct-runtime-stderr.log`에 해당 DB 비밀번호가 없음을 확인했다. Runtime ERROR/Exception 0건·stderr 0바이트. 선택 실행기의 자격증명 처리 제거·PowerShell 문법 검사 통과.
- **증거·최종 상태:** `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-db-properties-20261122/`의 `verify-direct.log`, `direct-db-check.log`, `direct-runtime-stdout.log`·`direct-runtime-stderr.log`, `ProbeDirect.ps1`·`DirectDbProbe.java`, `VerifyDirect.ps1`·`StartDirect.ps1`, `ScanDirectLogs.ps1`·`SecretScan.java`, `round-04-resume.mjs`·`round-05-direct-gui.mjs`·`round-06-finish.mjs`, `direct-properties-holiday.png`. 브라우저 작업 공간은 종료하고 검증용 JVM PID 18768은 사용자 확인용으로 실행 상태를 유지한다(Quartz·메일·SMS·ES 비활성, 일반 Maven 실행에는 이 검증 전용 옵션을 적용하지 않는다). 화면/CSS 변경은 없고 13.3인치 실기기 검증은 이번에도 미수행이다. 빌드 이후에는 Markdown만 추가 수정하여 전체 빌드를 다시 실행하지 않는다. 커밋·푸시 없음.

---

### 2.21. 2026-11-22: PostgreSQL 단일 DB·Boot 자동 구성 정리

<!--
문서 상태: 완료
확인 근거: Hy4 최종 보고·snapshot manifest·JAR SHA·ego-browser 증거, 2.22절 총괄 최종 verify, 2.23절 사용자 기기 확인 및 실제 표시 조건의 4개 화면 재조회.
확인 범위: PostgreSQL 단일 DB 구성 정리·격리 자동 테스트·핵심 업무 GUI와 13.3인치의 공지/Q&A/공휴일 목록·결재 상세 표시 확인. 전체 프로젝트의 모든 화면/모달·다중 폭 반응형·실제 Supabase 업무·백업/복원 검증을 완료했다는 뜻은 아니다.
완료 기준: 승인 범위의 코드·설정 보존과 전체 자동 검증·핵심 GUI 결과가 확인되고 잔여 제약이 기록되어 있다. 워커 메시지 저장만으로 실행 재개 또는 완료로 보지 않는다.
-->

- **기준·범위:** HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 사용자·워커 미커밋 변경. Orca Run `run_ef7d3d7a8457`. 기존 사용자 변경, `data/`·`backup/`, 접속 정보, 풀 값과 `Asia/Seoul`, 테스트 기대값을 보존했다. 이번 정리에서는 실제 Supabase 자동 테스트·업무 쓰기·DDL을 수행하지 않는다.
- **현재 구현:** `pom.xml`의 `spring-boot-starter-jdbc`와 `spring.datasource.*`·`spring.datasource.hikari.*`로 Boot 자동 구성. HSQLDB 의존성·`EgovConfigDatasource`·`HsqlDemoDatabaseInitializer`·`DBJob`·HSQL schema/seed는 제거했고 PostgreSQL SQL만 유지한다. `QuartzConfig`의 매일 03시 임시파일 정리와 ApplicationContext customizer는 유지했다. 자동 DB 백업·자동 스키마 초기화는 없다.
- **설정 보존:** 최대/최소 풀 10/2, 연결/유효성 대기 10000/3000ms, idle/maxLifetime 600000/1800000ms, `connection-init-sql=SET TIME ZONE 'Asia/Seoul'`, `spring.sql.init.mode=never`를 코드와 대조했다. 사용자가 입력한 비밀값 원문은 출력하지 않는다. 실제 비밀번호가 포함된 properties·JAR는 공유·커밋 금지다.
- **결재 SQL:** AtomicInteger 순번 ID의 충돌 위험을 교차 검토 후 기존 `"AH" + CommUtil.getFileId()`로 복원했다. `Approval_SQL.xml`의 `insertHistory.ACTION_DT`만 `statement_timestamp()`로 바꿨다. PostgreSQL의 트랜잭션 고정 `CURRENT_TIMESTAMP`와 이력 정렬 차이를 수정하며 다른 시각 컬럼·정렬·업무 규칙·테스트는 변경하지 않았다. 문장 시각 동률과 기존 난수 ID의 일반적 충돌 한계는 남는다.
- **기존 자동 검증:** 워커의 최종 재실행과 Hy4 읽기 전용 교차 검토는 통과했다. 총괄도 기존 로그·XML을 대조했다. 전체 수치·커버리지는 [테스트 실행 현황](../../../src/test/README.md#최신-실행-결과)을 따른다. 결재 테스트 3회 모두 5/0/0/0. 첫 `verify-final.log`는 JUnit discovery 실패, `verify-final-2.log`는 성공·JaCoCo 충족이다. 이번 문서 갱신 중 새 최종 `verify.cmd`를 실행하지 않았다.
- **패키징 교차 검토:** HikariCP 6.3.3·PostgreSQL JDBC 42.7.3은 포함, HSQLDB·DBCP2·embedded PostgreSQL과 제거 클래스는 배포 JAR에서 제외됐다. 당시 target/복사본 SHA-256은 `A87328861744756F1F410928FFCCB73C5B142DE4E148E850279C4D1A39FC3288`로 일치한다. 이 SHA를 미완료인 결재 템플릿 수정 후 JAR의 SHA로 보지 않는다.
- **빌드 경합:** 사용자 IDE가 같은 `target/classes`를 쓰는 동안 `clean verify`가 실행되어 19:06:29 `PermissionMapper` ClassNotFoundException이 발생했고 빌드 후 사용자 앱 재기동은 성공했다. 사용자 앱·IDE·8080은 건드리지 않으며 추가 원본 루트 clean은 조율 전 금지다.
- **결재 GUI 차단:** Freebuff가 18080·격리 PG50904에서 기안을 저장했으나 `approvalView.html` script의 `th:inline="javascript"` 누락으로 docId가 빈 값인 POST가 발생해 상신이 실패했다고 보고했다. 자동 테스트는 docId를 직접 보내므로 이 GUI 결함을 잡지 못한다. 총괄이 파일과 `target/gui-evidence/pg-hikari-final-20261122/16-approval-view-docid-rendered.png`를 대조하고 속성 하나의 최소 수정을 승인했다. 승인 지시는 전달 대기였으나 이후 워커가 최종 보고 없이 종료됐다. 실제 수정·재검증·최종 보고는 아직 확인되지 않았다. 약식결재를 검증완료로 올리지 않는다.
- **재배정 준비 확인(19:44 KST):** `worker-list`에서 기존 Dispatch `ctx_6720ef2e5a80`은 `failed`·`exited`·`released`, 같은 Task `task_e62321a296e8`은 `ready`다. 실행 중인 Freebuff 프로세스와 라이브 세션 파일은 없고, 설정 키만으로 실제 모델·강도·일일 잔여 시간을 확인할 수 없다. 사용자 재배정 요청은 받았지만, 기본 Flash·high 확인 없이 자동 첫 제출하거나 새 세션 차감 조건을 임의로 바꾸지 않는다. Freebuff 세션 준비 확인 전 재배정 실행·최종 빌드·GUI 확인은 미수행이며, 다른 Worker를 중복 생성하지 않았다.
- **재배정 후 정책 차단:** 사용자가 연 Freebuff의 실제 화면에서 Flash·high와 3/15시간 잔여를 확인한 뒤 같은 Task·기존 터미널에 Dispatch `ctx_e95ddfeb1176`으로 배정했다. 이후 보고 없는 턴 종료 알림을 받아 `worker-read`로 확인한 실제 오류는 `Driven by another agent` 사용 정책 차단이다. Freebuff는 타 에이전트가 맡긴 작업이라 요청을 모델에 보내지 않았고 당일 strike 1을 기록했다고 표시했다. 화면 잔여량은 1/15시간·세션 1h left로 바뀌었다. 템플릿 수정·최종 빌드·GUI는 시작되지 않았다. 문구 변경/사용자 요청 위장/재전송으로 우회하지 않는다. 사용자 소유 터미널·세션은 유지하며 일반 단독 처리 또는 다른 승인된 실행 수단으로의 전환이 필요하다. Task의 blocked 변경은 active Dispatch 때문에 Orca가 거절해 Dispatch는 아직 열려 있다. 근거는 외부 임시 `arkive-freebuff-policy-block-20261122.md`다.
- **GUI·최종 검증 잔여:** Q&A/공휴일 워커 최종 보고와 결재 상신→승인→이력 대조, 최신 snapshot/JAR 동일성, 총괄 verify 및 ego-browser 결과 재조회가 남았다. 사용자 앱에서 업무 쓰기를 하거나 이전 Supabase 검증을 새 격리 GUI 결과로 대체하지 않는다. 실제 13.3인치 확인은 미수행이다.
- **Supabase A안 확인:** 사용자는 필요할 때 수동 Resume를 선택했다. Hy4는 사용자 로그인 후 `https://supabase.com/dashboard/project/xzjcvayeseiiohoebyvr`에서 프로젝트 `Arkive`, STATUS `Healthy`, Resume 버튼 없음으로 확인했다. 휴면 상태의 실제 버튼 위치는 미확인이다. Resume/Pause·설정·SQL·데이터 변경은 하지 않았다. README에 바로가기와 디자인 샘플 선택기 설명을 유지했다. B cron은 미선택·미구현이다.
- **Hy4 후속 최종 보고:** Dispatch `ctx_8b9479765408`의 완료 메시지 `msg_738cc3cdc4fd`와 `arkive-hy4-final-fix-gui-report.md`를 받았다. 위 차단·재배정 경과는 당시 이력이며 현재 결재 인라인 속성 한 줄은 반영돼 있다. Hy4는 원본을 추가 수정하지 않고 `hy4-snapshot-20261122-200626`의 818개 파일 해시 일치, `verify.cmd` exit 0·338개/실패 0/오류 0/skip 0·JaCoCo 충족·BUILD SUCCESS를 보고했고 총괄이 로그·결재 화면 증거와 대조했다. JAR SHA-256 `1F86D22F8F62EF52A96234995E2B74372081250C94382E4460F6B916D6118B78`. 이는 이후 HSQL 주석 정리 이전 snapshot 기준이며 최신 빌드로 혼동하지 않는다.
- **GUI 보고·총괄 재조회:** Hy4가 로컬 앱 `127.0.0.1:18090`·격리 PostgreSQL 17.12(`56657`)에서 Q&A 등록→답변 저장/수정→삭제, 2099-11 공휴일 등록→같은 날짜 MERGE 수정→삭제, 승인된 일반 기안자 `hy4drafter` 생성→결재 기안→상신→관리자 승인을 수행했다. 결재 문서 `AP20261122203744862506`의 docId 렌더링과 AJAX 전달, 이력 [작성 20:37, 상신 20:38, 승인 20:42]을 보고했다. `hy4-09/10/11` PNG를 직접 대조했으며 Q&A·공휴일 초기 단계 PNG 부재는 보고의 제약으로 유지한다. 총괄은 ego-browser의 같은 작업 공간 12에서 Q&A 0건·2099-11 공휴일 0건·결재 승인과 세 이력 순서를 **읽기 재조회**하고 각각 DOM·PNG를 저장해 직접 열어 확인했다. 재조회는 쓰기 단계 전체 반복이나 별도 네트워크 캡처를 뜻하지 않는다.
- **GUI 재조회 제약·환경 유지:** 첫 p1의 evaluate/이동은 renderer 무응답으로 실패했다. 취소 여부 미확인 상태에서 쓰기 작업을 재시도하지 않고 같은 공간의 새 p2에서 DOMContentLoaded 대기로 읽기만 재개해 확인했다. 결과 페이지는 `finish({keep:['p2']})`로 사용자 확인용으로 남겼고 앱·로컬 PG도 유지한다. Q&A·공휴일은 정리됐으나 기안자 계정·승인 문서/이력은 로컬 PG에 남아 있으며 승인 문서를 임의 삭제하지 않는다. 실제 Supabase·사용자 8080·기존 18080 환경은 변경하지 않았다. 실제 13.3인치 검증은 미수행이며 viewport 검사가 이를 대체하지 않는다.
- **총괄 최종 자동 검증(21:03 KST):** 후속 잔재·주석 정리까지 포함한 최신 818개 입력 파일의 동일성 snapshot에서 `verify.cmd` exit 0·338개/실패 0/오류 0/skip 0·JaCoCo 충족·BUILD SUCCESS를 확인했다. 검증 종료 후 원본 입력 파일 drift 0이며 원본 `target`은 건드리지 않았다. 상세 재시도 경과·패키징 근거와 최신 JAR SHA는 2.22절을 따른다. 이 결과는 13.3인치 실기기·실제 Supabase 전체 업무·백업/복원 검증의 대체가 아니다.
- **13.3인치 후속 확인:** 사용자가 현재 노트북이 13.3인치임을 확인했다. 총괄은 실제 디스플레이 해상도·OS 배율·브라우저 창과 표시 조건을 수집하고 같은 로컬 GUI에서 4개 화면을 다시 확인했다. 위의 실기기 미수행 문구는 **기기 크기를 확인하기 전의 당시 기록**으로 보존하며 현재 수행 범위는 [2.23절](#223-2026-11-22-133인치-표시-확인readme-db-소개-축약)을 따른다. 이전 워커 보고서를 소급해 수정하거나 모든 화면의 실기기 검증으로 확대하지 않는다.
- **근거:** 외부 임시 `C:/Users/kdh/AppData/Local/Temp/opencode/`의 `arkive-pg-autoconfig-implementation-report.md`(초기 순번 ID안은 후속 복원으로 대체), `arkive-hy4-postfix-final-review-report.md`, `arkive-hy4-startup-failure-report.md`, `arkive-worker1-gui-20261122/logs/verify-final*.log`, `arkive-supabase-resume-view-report.md`, 승인 spec `arkive-approval-inline-fix-20261122.md`, `arkive-hy4-final-fix-gui-report.md`. 기존 `arkive-worker1-final-fix-gui-report.md`도 현재 존재하지만 중단된 전체 검증을 Hy4의 성공 로그와 혼동하지 않는다. 총괄 재조회 증거는 `arkive-hsql-cleanup-20261122/coordinator-{qna,holiday,approval}-snapshot.txt`·대응 PNG다. 커밋·푸시 없음.

---

### 2.22. 2026-11-22: HSQLDB 로컬 잔재 정리

<!--
문서 상태: 완료
확인 근거: 삭제 목록·파일 참조 점검·818개 snapshot manifest·verify exit 0·338개 테스트 통과·JaCoCo 충족·배포 JAR 점검, 총괄.
확인 범위: 사용자 승인된 로컬 HSQL 잔재 제거와 관련 주석·문서 정리. 전체 프로젝트의 실기기·배포·백업/복원 미수행 항목은 이 완료 상태에 포함하지 않는다.
완료 기준: 승인된 HSQL 파일만 제거되고 PostgreSQL SQL·파일 저장소·기존 테스트가 보존되며 전체 자동 검증이 통과한다.
-->

- **승인·범위:** 사용자가 일반적인 PostgreSQL 구성으로 다시 확인하고 불필요한 HSQL 잔재를 삭제·정리하도록 승인했다. 기준 HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 사용자·작업 담당 변경. 2.21절 당시 보존했던 로컬 HSQL 데이터의 후속 정리이며 이전 검증 이력은 삭제하지 않는다.
- **삭제 결과(20:41 KST):** `data/ArkiveDB.lck`·`.lobs`·`.log`·`.properties`·`.script`, `data/ArkiveDB.tmp/`, `data/ArkiveDB-backup-20261115/`·`data/ArkiveDB-backup-20261115-b/` 안의 HSQL 파일·빈 임시 폴더, `backup/ArkiveDB.sql`을 제거했다. 파일명·실제 참조·배타적 파일 접근 가능 여부를 확인한 뒤 13개 파일(3,465,472 bytes)과 빈 폴더만 삭제했다. 정리 뒤 `data/`·`backup/`는 다른 내용 없이 비어 있어 제거됐으며 PostgreSQL SQL의 전후 SHA-256은 일치한다. 옛 HSQL 데이터는 이 정리로 삭제됐으므로 해당 파일로 복구할 수 없다.
- **유지 대상:** `src/main/resources/db/postgres/schema.sql`·`seed.sql`과 격리 PostgreSQL 테스트 자산. 앱은 Boot JDBC·HikariCP로 외부 PostgreSQL에 접속하며 자동 DDL·시드를 실행하지 않는다. 실제 Supabase·사용자 앱·오케 검증용 DB/앱은 변경하지 않는다.
- **파일 저장소 구분:** `data/vod/`는 현재 `arkive.vod.path`의 동영상 저장 기본 경로다(`application.properties`, `FfmpegVideoEncodingService.java:49,79-83`). 정리 시 로컬 `data/`에는 HSQL 파일만 있고 동영상은 없었다. 동영상 설정은 유지하며 향후 업로드가 필요한 디렉터리를 생성한다. `.gitignore`의 `/data/`·`/backup/` 제외는 동영상·수동 백업의 비공개 로컬 파일 보호용으로 유지하고, HSQL 서버·자동 덤프를 뜻하던 주석만 수정했다.
- **주석·문서:** Board·FAQ·검색 서비스의 JDBC Clob 변환은 특정 HSQL 기능이 아닌 이식 호환 처리이므로 제거하지 않고 설명을 수정했다. Q&A MERGE·FAQ 페이징·Quartz의 오래된 HSQL 주석도 현재 구성에 맞췄다. SQL·업무 로직·테스트 기대값은 변경하지 않았다. README·현재 DB/아키텍처 기록의 옛 파일 보존 안내는 이번 승인된 제거와 파일 저장소 구분으로 갱신했다.
- **검증 상태:** 승인된 HSQL 잔재 정리의 자동 검증은 아래 최종 실행으로 통과했다. 삭제·소스 참조·자동 검증 결과는 `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-hsql-cleanup-20261122/`에 기록한다. 전체 작업의 GUI 수행 범위와 13.3인치·백업/복원 미수행 사항은 별개이며 이 정리만으로 완료 처리하지 않는다. 커밋·푸시 없음.
- **최신 자동 검증 경과:** 첫 `snapshot-20261122-204151`의 818개 입력 파일을 해시 확인해 복사하고 `verify.cmd`를 실행했다. surefire 84 suites 합계 338개·실패/오류/skip 0, JAR 생성까지 확인했으나 600초 도구 제한(exit 143)으로 종료돼 전체 verify·JaCoCo 통과로 판정하지 않는다. 당시 여유 메모리 약 0.38GB/총 7.81GB를 확인했다(중단 원인 전체가 메모리라고 단정하지 않는다). 재실행 프로세스에만 `JAVA_TOOL_OPTIONS=-Xmx512m -XX:ActiveProcessorCount=2`, `MAVEN_OPTS=-Xmx256m -XX:ActiveProcessorCount=2`를 추가하며 다른 앱·코드·테스트·JaCoCo 임계값은 변경하지 않는다.
- **임시 실행기 오류·재시도:** `retry-01/`은 818개 복사 후 Java의 정상 stderr 안내 `Picked up JAVA_TOOL_OPTIONS`를 Windows PowerShell 5.1의 `ErrorActionPreference=Stop`이 `NativeCommandError`로 처리해 시작 단계에서 exit 1로 중단됐다. 이는 자동 테스트의 실패 결과가 아니며 Maven 전체 결과도 없다. 이 정리용 프로세스가 남아 있지 않음을 확인한 뒤 **저장소 밖 임시 실행기만** `Start-Process -Wait -PassThru`·프로세스 stdout/stderr 직접 리디렉션·실제 exit 코드 판정으로 수정했다. 앞 로그를 덮어쓰지 않고 `retry-02/`의 새 동일성 snapshot에서 1800초 제한으로 재실행했으며 실제 결과는 아래와 같다.
- **최종 verify: 통과.** `retry-02/snapshot-20261122-210056`에서 루트와 바이트 동일한 818개 입력 파일로 `verify.cmd` 실행, exit 0·84 suites·테스트 338개/실패 0/오류 0/skip 0·`All coverage checks have been met.`·`BUILD SUCCESS`(1분 51초, 종료 `2026-11-22T21:03:03+09:00`). 원본 입력 파일의 종료 후 drift 0, 원본 `target` 미접촉을 확인했다. 근거: `retry-02/snapshot-manifest.json`, `verify-result.json`, `verify.log`, `package-test-summary.json`, snapshot의 surefire XML·JaCoCo 결과. 검증 후 저장소 수정은 이 Markdown 기록뿐이다.
- **최종 패키징 점검:** JAR SHA-256 `0E2B7103574ABA26E9F38B6A3820183FFC77BEA3ACCCA57A6ED9FB89D3BD57C8`. HikariCP 6.3.3·PostgreSQL JDBC 42.7.3·PostgreSQL schema/seed 포함, HSQLDB·DBCP2·테스트용 embedded PostgreSQL·제거된 초기화기/수동 DataSource/DBJob 클래스 미포함을 확인했다. JAR에는 사용자 비밀값을 포함한 properties가 들어 있으므로 공유·커밋하지 않는다. 현재 앱·동영상 설정·테스트 파일·기존 사용자 변경은 보존했다.
- **GUI snapshot 차이 점검:** Hy4 기준과 현재 Mapper 34개는 XML 주석·빈 줄을 제외한 SQL/요소 원문이 모두 같다(`gui-snapshot-mapper-comparison.json`). Board의 최초 DOM 비교 불일치는 주석 사이 텍스트 노드의 공백 차이였으며 직접 diff로 주석만 바뀐 것을 확인했다. 결재 템플릿·업무 코드 변경은 이 정리에 포함하지 않는다. 화면 최종 상태 대조는 2.21절을 따른다.

---

### 2.23. 2026-11-22: 13.3인치 표시 확인·README DB 소개 축약

<!--
문서 상태: 완료
확인 근거: 사용자 기기 크기 확인, Win32_VideoController·WmiMonitorBasicDisplayParams·GetScaleFactorForMonitor, ego-browser DOM/창/viewport·4개 PNG 직접 대조, README diff·링크 점검.
확인 범위: README의 DB 소개 축약과 현재 실제 13.3인치 환경의 아래 4개 화면 표시 확인. 모든 등록/수정/모달·다중 폭 대응·새 업무 쓰기 테스트는 포함하지 않는다.
완료 기준: 현재 구성 안내·비밀값 주의·Supabase 바로가기와 기능 체크 목록을 유지하고 실제 표시 조건·관찰 결과·제약을 기록한다.
-->

- **기기·조건:** 사용자가 현재 노트북 크기를 **13.3인치**로 확인했다. 로컬 시스템에서 활성 디스플레이 1개, 해상도 **1920×1080 / 60Hz**, Windows 표시 배율 **150%**를 확인했다. WMI의 화면 크기 29×17cm는 보조 근거이며 인치에서 해상도를 추정하지 않았다. ego-browser는 해당 디스플레이의 최대화 창(1280×720 논리 화면, 창 상태 maximized)이며 실제 페이지 viewport는 **1409×658 CSS px**, DPR 약 **1.35**, 브라우저 확대율은 **90%**(DPR 1.35 ÷ OS 배율 1.5로 산출; 브라우저 설정 UI를 별도 조작/캡처한 값은 아님)다. 화면 크기 에뮬레이션·OS 배율·브라우저 확대율을 변경하지 않았다.
- **GUI 표시 확인: 통과(4개 화면 범위).** 같은 공간 12에서 관리자 로그인 상태의 격리 앱 `http://127.0.0.1:18090`을 읽기 재조회했다. `/board/ckeditorList`, `/qna/qnaList`, `/schedule/holidayList?yyyymm=209911`, `/approval/approvalView?docId=AP20261122203744862506`의 제목·검색/등록 조작·목록·사이드바·결재 정보/이력 표시를 확인했다. 각 DOM의 불필요한 가로 overflow 없음, 표시된 입력·버튼의 가로 잘림 0을 확인했고 PNG 4개를 직접 열어 겹침·잘림 여부를 대조했다. 세로 스크롤은 정상 페이지 탐색이며 가로 overflow와 구분한다. Q&A·공휴일 0건과 결재 승인/작성→상신→승인 이력은 유지됐다. 업무 쓰기 0건이며 실제 Supabase·사용자 앱·기존 검증 DB는 변경하지 않았다.
- **증거·버전:** `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-laptop-gui-20261122/`의 `display-info.json`, `browser-info.json`, `gui-results.json`, `{board-list,qna-list,holiday-list,approval-view}-snapshot.txt`·PNG. 기준 HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 미커밋 변경, 재조회 런타임은 2.21절의 Hy4 snapshot/JAR 기준이다. 새로운 컴파일·실제 Supabase 시나리오·모든 화면의 유동 폭 검증을 수행한 것으로 보지 않는다.
- **README 최소화:** 사용자 요청에 따라 `README.md`의 기본 DB 소개에서 전환 경과·요금/한도·플랜 제약·미선택 cron·중복 접속 상세 설명을 제거했다. 현재 Spring Boot JDBC·HikariCP → Supabase PostgreSQL 구성, properties 설정 위치, 수동 Resume 바로가기, 실제 비밀번호를 포함한 properties/JAR 공유 금지만 남겼다. 실행 절차·SQL 사용 안내·전체 기능 체크 목록·디자인 링크·상세 검증 이력은 유지했다.
- **문서 검증:** 이번 저장소 수정은 `README.md`·이 기록의 Markdown뿐이므로 `verify.cmd`를 재실행하지 않는다. 변경한 링크·제목·diff를 확인하며 이전 자동 검증은 2.22절의 실제 실행 결과로만 기록한다. 코드·설정·테스트 수정과 커밋·푸시 없음.

---

### 2.24. 2026-11-22: 기본 화면 6개 읽기 중심 GUI 점검

<!--
문서 상태: 부분 미완성
미완성 항목: 데이터가 있는 FAQ 아코디언·2페이지 이상 페이징, 로그인 화면 전환 후 총괄의 사용자 검색 재확인.
필요한 근거/확인자: 담당자가 승인한 격리 테스트 데이터와 로그인 세션, ego-browser 동작·화면 증거 / 총괄 및 대상 담당자.
완성 방법: 승인된 격리 환경에서 남은 동작을 확인한다. 기존 데이터나 실제 Supabase에 임의 등록하지 않는다.
완료 기준: 아래 기본 조회 범위와 남은 검증을 구분하고 미수행 항목의 실제 동작 결과·증거를 추가한다. 전체 기능 검증완료 표시는 하지 않는다.
-->

- **범위·버전:** 사용자 요청에 따라 기존 Worker가 ego-browser로 관리자 메뉴·목록·검색·초기화·등록 화면 열람만 확인했다. 기존 Hy4 snapshot `hy4-snapshot-20261122-200626`의 앱 `http://127.0.0.1:18090`·격리 PostgreSQL 17.12(56657)를 재사용했으며 최신 사용자 소스를 재빌드하지 않았다. 기준 HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 미커밋 변경. 실제 Supabase·사용자 8080 앱·기존 18080 검증 환경은 대상이 아니다.
- **표시 조건:** 사용자 확인 13.3인치 노트북, 1920×1080·Windows 150%. Worker와 총괄의 실제 viewport는 1409×658 CSS px, DPR 약 1.35이며 배율·확대율·에뮬레이션을 바꾸지 않았다. 브라우저 90%는 2.23절과 같이 DPR/OS 배율로 산출한 값이지 설정 UI 확인값은 아니다. 다른 폭·모든 모달/상세 화면까지 검증한 것으로 확대하지 않는다.

| 화면 | Worker가 직접 확인한 동작·결과 | 판정·제약 |
| --- | --- | --- |
| 대시보드 `/dashboard` | 메뉴 펼침/접힘·이동, 현황 0건·최근 글 빈 목록, Health UP(DB UP) 표시 | 기본 표시·메뉴 통과 |
| 사용자 `/auth/userList` | 초기 2명 → `admin` 검색 1명 → 없는 ID 검색 0명 → 초기화 2명, 등록 화면 열람 후 복귀 | 조회·검색·열람 통과, 관리 버튼의 실제 처리는 미수행 |
| 공지 `/board/ckeditorList` | 빈 목록·제목 검색·초기화, CK에디터 등록 화면 열람 후 복귀 | 기본 조회·열람 통과, 데이터가 있는 검색·저장 미수행 |
| FAQ `/faq/faqList` | 제목 검색·초기화, 정상 빈 결과 안내 | 기본 조회 통과, 항목 0건으로 아코디언 미수행 |
| Q&A `/qna/qnaList` | 외부 공개 필터·제목 검색·초기화, 등록 화면 열람 후 복귀 | 기본 조회·열람 통과, 데이터가 있는 필터 결과·저장 미수행 |
| 공휴일 `/schedule/holidayList` | 메뉴 진입 기본 2026-11 → 2099-11·2099-10 검색 → 2099-11 복귀, 빈 목록·입력/등록 버튼 표시 | 월 검색·표시 통과, 등록 미수행 |

- **Worker 근거:** `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-basic-gui-smoke-20261122/report.md`와 같은 폴더의 DOM·라운드 스크립트, [PNG 증거 폴더](../../../target/gui-evidence/basic-smoke-20261122/20261122-212730/)의 신규 17개 이미지. 수집한 라운드의 콘솔/예외/네트워크 오류는 0건으로 보고했다. 단 사용자 검색 하위단계의 이벤트 버퍼는 Worker 스크립트 중단으로 미수집이므로 그 구간까지 오류 0건이라고 단정하지 않는다. 진단 자체 테스트의 의도적 콘솔 오류·404는 앱 오류와 구분한다. 데이터가 없어 2페이지 이상 페이징도 미수행이다.
- **총괄 대조:** 기본 화면 6종, 사용자 `admin`/없는 ID 검색 결과, 사용자·공지·Q&A 등록 열람의 PNG를 직접 열어 제목·표·입력/버튼·사이드바의 겹침/가로 잘림이 보이지 않음을 확인했다. 공간 12의 실제 공휴일 2099-11 빈 목록과 대시보드 Health UP(DB UP)·현황 표도 재조회했다. 대시보드의 최초 검사 중단은 표 셀 사이 탭을 공백으로 정규화하지 않은 임시 스크립트 문자열 문제이며 실제 DOM은 정상이다. 이후 사용자 목록 재조회는 `/login/login`으로 전환돼 추가 검색을 중지했다. 로그인 화면 전환 원인은 미확인으로 두며 Worker 검색 결과의 총괄 재실행 성공으로 기록하지 않는다. 별도 자격증명 입력 없이 `handOff()`로 공간 제어를 사용자에게 넘겼다.
- **안전·남은 확인:** 코드·설정·테스트 수정, 업무 등록/수정/삭제·승인·외부 발송·SQL/DDL 실행 없이 기존 앱·PG를 유지했다. 저장·권한 차단·데이터 정합성·실제 Supabase·최신 소스 전체의 GUI 검증은 미수행이다. Worker의 6개 기본 화면 통과는 이 조회 범위의 결과이며 전체 기능 검증완료가 아니다.
- **이번 문서 검증:** 저장소 변경은 이 Markdown 기록뿐이다. 증거 경로·제목·diff를 확인하며 `verify.cmd`는 Markdown 예외로 재실행하지 않는다. 이전 자동 검증 338개 통과는 2.22절의 당시 snapshot 결과이고 이번 실행 결과가 아니다. 커밋·푸시 없음. `target`·외부 임시 폴더의 증거는 clean 또는 임시 파일 정리 시 없어질 수 있다.

---

### 2.25. 2026-11-22: 현재 DB 안내 정리·불필요한 앱 초기화 설정 제거

<!--
문서 상태: 완료
확인 근거: 현재 안내 13개 파일의 링크·앵커 검사, README 기능 체크 목록의 index 대조, application.properties의 변경 블록 대조, 격리 snapshot verify-result.json·verify.log·Surefire XML·패키징 JAR.
확인 범위: 현재 안내의 오래된 DB 실행·이식 설명 정리와 앱의 불필요한 SQL 초기화 설정 제거. 별도 배정한 16개 기능 GUI 검증과 실제 Supabase·운영 검증은 포함하지 않는다.
완료 기준: 요청한 설정·현재 설명만 제거되고 기존 사용자 설정·테스트 자산·기능 목록·과거 검증 이력이 유지되며 문서 검사와 전체 자동 검증이 통과한다.
-->

- **변경 파일:** `README.md`, `PORTING.md`, `docs/ai/common/test-standard.md`, `docs/ai/boot/boot-apply.md`·`test-stack-boot.md`, `docs/ai/classic/test-stack-classic.md`, `docs/ai/portable/feature-porting-sheet.md`·`test-porting-sheet.md`·`ai-porting-request.md`, `docs/ai/project/project-context.md`·`database-schema.md`·`feature-catalog.md`·이 기록, `src/main/resources/application.properties`.
- **문서 정리:** 현재 안내의 불필요한 삭제 경과·옛 자동 실행·고정 DB 포트·덤프 예제·삭제된 SQL 경로를 제거하거나 현행 PostgreSQL/대상 DB 기준으로 갱신했다. 날짜별 실제 검증 이력은 유지했다. README 기능 체크 목록은 73개이며 index의 각 항목과 내용·상태가 동일하다.
- **앱 설정:** 사용자 요청으로 `spring.sql.init.mode=never`와 바로 위 주석 2줄을 제거했다. 설정 파일의 index 기준 차이가 이 블록뿐임을 비밀값 출력 없이 대조했다. PostgreSQL 연결 정보·HikariCP·시간대·파일 저장소 설정은 변경하지 않았다. `src/test/**`의 초기화 설정·코드는 이번 작업에서 수정하지 않았다.
- **문서 검사: 통과.** 현재 안내 13개 파일의 로컬 링크·앵커 117개, 현행 안내의 옛 DB 참조, 인용한 PostgreSQL SQL·테스트 자산 경로를 확인했다. 근거: `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-md-current-cleanup-20261122/validate.mjs`. 변경 대상의 `git diff --check`는 통과했다. 전체 작업 폴더 검사에서는 별도 사용자 변경인 `EgovSample_Sample_SQL.xml` 주석 7줄의 공백 경고가 나왔으며 임의 수정하지 않았다.
- **전체 자동 검증: 통과.** 총괄이 원본 입력 818개의 SHA-256이 일치하는 `snapshot-20261122-220838`에서 `verify.cmd`를 직접 실행했다. exit 0, 84 suites·338 tests·실패 0·오류 0·skip 0, JaCoCo 기준 충족, `BUILD SUCCESS`(4분 38초, Maven 종료 `2026-11-22T22:13:44+09:00`). 종료 후 원본 소스 drift 0을 확인했다. 원본 `target`·기존 실행 앱·격리 환경·실제 Supabase는 변경하지 않았다.
- **패키징·버전:** JAR SHA-256 `6BEA22D5C8C6DD61B45FAE703ADA2C04928C324B9AC6EB0ACB3B18CFB54F4471`. JAR의 앱 properties에서 제거한 설정이 없으며 HikariCP 6.3.3·PostgreSQL JDBC 42.7.3 포함을 확인했다. 기준 HEAD `5f681a49d21584aab5c43c8a7b7ff01ab8ebc950` + 기존 미커밋 변경. 이 JAR는 이전 GUI 실행본과 다르며 비밀값을 포함하므로 공유·커밋하지 않는다.
- **증거·제약:** `C:/Users/kdh/AppData/Local/Temp/opencode/arkive-hsql-cleanup-20261122/sql-init-removal-20261122/`의 `snapshot-manifest.json`, `verify-result.json`, `verify.log`, snapshot의 Surefire·JaCoCo 결과. 실행 앱 재시작·새 JAR GUI·실제 Supabase 시나리오·변이 테스트는 이번 설정 제거 작업에서 미수행이다. 화면 코드는 변경하지 않았다. 16개 기능의 별도 Worker 배정은 이 자동 검증만으로 완료 처리하지 않는다. 커밋·푸시 없음.

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
- PostgreSQL 단일 구성이며 Boot JDBC가 HikariCP를 자동 구성한다. `data/vod/`는 동영상 저장 기본 경로다.
- 현재 SQL 원본은 `src/main/resources/db/postgres/schema.sql`·`seed.sql`이다. 테스트 초기화만 격리 PostgreSQL에 schema→seed를 적용한다.
- 기존 DB에는 시드를 재실행하지 않는다. 스키마 파일 변경만으로 앱이 DB를 자동 갱신하거나 불일치를 차단한다고 보지 않는다.

### 4.2. 개발자 사용법
1. **실행:** Java 17·Maven을 준비하고 `application.properties`의 `spring.datasource.*`를 대상 PostgreSQL로 맞춘 뒤 `mvn spring-boot:run`을 실행한다. 비밀번호는 공유·로그·커밋하지 않는다.
2. **새 DB:** 담당자가 대상·데이터 보존·복구 방법을 확인한 뒤 `db/postgres/schema.sql` → `seed.sql`을 적용한다. 기존 DB·실제 Supabase에 테스트용 DDL/시드를 자동 적용하지 않는다.
3. **기존 DB 변경:** 필요한 ALTER·백필만 별도 마이그레이션으로 준비하고 백업·복원 검증 후 적용한다. 실행 중 DB를 지우거나 시드로 덮어쓰지 않는다.
4. **검증:** 테스트는 격리 embedded PostgreSQL을 사용한다. 원본 루트의 `verify.cmd`는 `clean`을 포함하므로 IDE·실행 JAR와 시점을 조율하거나 동일성 확인된 별도 빌드 복사본을 사용한다.

### 4.3. 대상 프로젝트 이식 주의점
- DB 접속 설정은 대상의 Boot 자동 구성 또는 기존 DataSource 패턴에 맞춘다.
- 대상 프로젝트의 DB 문법과 마이그레이션 도구(Flyway/Liquibase 등)에 맞춰 `db/postgres/schema.sql`과 `seed.sql`의 필요한 DDL/DML만 발췌해 적용한다. 테스트 자산 맞춤은 스펙 담당자의 별도 작업이다.

### 4.4. 보존·복구 주의
- 현재 PostgreSQL 자동 백업은 미구현이다.
- `pg_dump` 등 대상 환경의 백업과 실제 복원 검증을 별도로 준비한다. Supabase 휴면 Resume는 백업·복원 검증을 대신하지 않는다.
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

## 6. 주요 동작·문서 구조 변경 이력

- **2026-11-18 문서 중복 정리:** 기능 카탈로그의 구현·검증 상태 분류(체크박스·3단 분류)를 없애고 상태는 `README.md`에서만 관리하도록 일원화했다. 카탈로그 4절은 테스트 클래스 목록 대신 범위·남은 공백만 남기고, 이 로그 2절은 날짜순으로 재정렬했다. 일회성 계획서 `docs/ai/docs-optimization-plan.md`는 아래 이력만 남기고 삭제했다. 기능 단위 이식에는 쓰지 않는 준비 점검 스크립트 `doctor.cmd`도 삭제했다(점검 항목은 `PORTING.md` 2절에 그대로 있다).
- **2026-11-13 `docs/ai/project/` 통폐합 (10개 → 4개):** `project-overview`·`architecture`·`change-impact`(공유 지점)·`error-code` → `project-context.md`, `feature-status`·`function-inventory`·`api-contract` → `feature-catalog.md`, `current-state-analysis`·`migration`·`change-impact`(작업별 기록) → `verification-log.md`, `database-schema.md`는 유지. 같은 기능을 여러 파일이 관점별로 반복 기술하던 중복을 없애기 위한 것이다(검토안 A 가벼운 정리 / B 기능 중심 통합 / C README 통합 중 B 채택). `README.md` 기능 체크 목록, `common/`·`portable/`·트랙 문서, `AGENTS.md`·`PORTING.md`는 구조를 유지했다.
- **2026-11-07 조회수 집계 방식 개선:** 세션 무기한 1회에서 **게시글당 브라우저 24시간 1회**로 전환. 세션 조회 시각과 `boardView_<boardId>` 쿠키(`Max-Age=86400`, HttpOnly)를 복합 검사하여 24시간 내 중복 조회를 차단하고 일별 통계의 정확성을 확보.
- **2026-11-03 게시판 검색 범위 분리:** 제목/내용/제목+내용 분리 검색 적용.
