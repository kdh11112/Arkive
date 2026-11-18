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

## 6. 주요 동작·문서 구조 변경 이력

- **2026-11-18 문서 중복 정리:** 기능 카탈로그의 구현·검증 상태 분류(체크박스·3단 분류)를 없애고 상태는 `README.md`에서만 관리하도록 일원화했다. 카탈로그 4절은 테스트 클래스 목록 대신 범위·남은 공백만 남기고, 이 로그 2절은 날짜순으로 재정렬했다. 일회성 계획서 `docs/ai/docs-optimization-plan.md`는 아래 이력만 남기고 삭제했다. 기능 단위 이식에는 쓰지 않는 준비 점검 스크립트 `doctor.cmd`도 삭제했다(점검 항목은 `PORTING.md` 2절에 그대로 있다).
- **2026-11-13 `docs/ai/project/` 통폐합 (10개 → 4개):** `project-overview`·`architecture`·`change-impact`(공유 지점)·`error-code` → `project-context.md`, `feature-status`·`function-inventory`·`api-contract` → `feature-catalog.md`, `current-state-analysis`·`migration`·`change-impact`(작업별 기록) → `verification-log.md`, `database-schema.md`는 유지. 같은 기능을 여러 파일이 관점별로 반복 기술하던 중복을 없애기 위한 것이다(검토안 A 가벼운 정리 / B 기능 중심 통합 / C README 통합 중 B 채택). `README.md` 기능 체크 목록, `common/`·`portable/`·트랙 문서, `AGENTS.md`·`PORTING.md`는 구조를 유지했다.
- **2026-11-07 조회수 집계 방식 개선:** 세션 무기한 1회에서 **게시글당 브라우저 24시간 1회**로 전환. 세션 조회 시각과 `boardView_<boardId>` 쿠키(`Max-Age=86400`, HttpOnly)를 복합 검사하여 24시간 내 중복 조회를 차단하고 일별 통계의 정확성을 확보.
- **2026-11-03 게시판 검색 범위 분리:** 제목/내용/제목+내용 분리 검색 적용.
