# 기존 코드 현행 분석 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 메뉴·파일·예약 작업의 실제 화면 호출과 업무 흐름, 역할별 처리, 예외/오류 응답 및 데이터 상태 전이. 현재 기록은 저장소에서 찾은 일부 코드 흐름이다.
필요한 근거/확인자: 현행 화면·API 실행 결과, 운영 로그(민감정보 마스킹), 업무 담당자 확인, 기존 시스템 문서.
완성 방법: 기능별로 보이는 입력부터 DB/외부 연계까지 추적하고, 코드 관찰과 담당자가 확인한 실제 현행 동작을 구분해 표에 기록한다.
완료 기준: 분석 범위의 각 기능에 호출자·사전조건·처리·결과·오류·근거가 작성되고 미확인 동작이 별도 표시되어 있다.
-->

> 저장소의 코드 관찰이며 승인된 업무 정책이나 운영 검증이 아니다. 분석 이력은 2026-09-25, 10-22~24다(당시 `verify.cmd -o` 193개 테스트·JaCoCo 게이트 통과). 10-24의 Chrome GUI·세션 쿠키 HTTP 기록은 카테고리·공휴일·당직·파일·게시판 이전글/다음글·SSE 일부에 한정되며, 현재 ego-browser 검증완료 판정으로 승격하지 않는다. 현재 분류는 [README.md](../../../README.md), 최신 자동 검증은 [테스트 자산](../../../src/test/README.md)을 따른다. 메일·SMS·OAuth·SSO·NICE 실제 외부 연동은 검증 전이다.

## 확인된 기능 흐름

| 영역 | 코드에서 확인한 흐름 | 확인된 출처 | 추가 확인 필요 |
| --- | --- | --- | --- |
| 로그인·세션 | `/login/login.json` 성공 시 세션 ID를 새로 발급하고 `LOGIN_USER`·`ADMIN_USER`를 저장한 뒤 `LoginSessionRegistry`에 등록한다. 같은 사용자로 다시 로그인하면 이전 세션이 만료된다. OAuth 콜백·계정연계도 같은 레지스트리를 사용한다. | `LoginController`, `OauthController`, `LoginSessionRegistry`, `EgovConfigWeb` | 기존 로그인 거부 방식, 다중 인스턴스 공유 세션, 세션 타임아웃 실제 동작 |
| 메뉴 관리 | `/system/menuList` 화면에서 Controller가 Service를 호출하고 Mapper/XML로 조회·등록·수정·삭제를 연결한다. | `SystemController`, `SystemServiceImpl`, `SystemMapper`, `System_SQL.xml` | 화면 호출 상세, 인증/권한, 메뉴 정책, 오류 계약 |
| 파일 처리 | 공통 FileController가 업로드·목록·다운로드·삭제 및 TUS 경로를 제공하고 파일 Service/Mapper 및 파일 저장 경로 설정을 사용한다. | `FileController`, `FileServiceImpl`, `File_SQL.xml`, `application.properties` | 화면별 호출 관계, 파일 접근 권한, 보존·삭제 정책, 외부 연계 |
| DB 초기화/마이그레이션 | Spring SQL 초기화는 꺼져 있다. 애플리케이션 시작 시 `SCHEMA_MIGRATION`에 없는 `db/versions/V*__*.sql`을 숫자 순으로 실행한다. 현재 V1~V18·V30~V35이며 번호 공백은 건너뛴다. 2026-10-11 구 체인 스쿼시 이후 변경은 증분 버전으로 추가한다. | `application.properties`, `EgovConfigDatasource`, `src/main/resources/db/versions/*.sql` (2026-11-08 재대조) | 운영 DB 제품·버전과 백업/복구 절차 |
| 게시글 댓글 | 게시글 상세에서 댓글 목록·건수를 조회한다. 로그인 사용자는 `/board/insertComment.json`으로 등록하고, 작성자 또는 관리자만 `/board/deleteComment.json`으로 삭제한다. 댓글은 1단 평문 저장 후 `th:text`로 출력하며 게시글 삭제 시 cascade된다. | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `V2__board_comment.sql`, `board/boardDetail.html` | 댓글 수정·신고·차단 정책, 관리자 판정 범위, 알림 연계 |
| 쪽지관리 | `/message/inbox`·`/sent`에서 로그인 사용자 기준 목록·검색·페이징, `/message/write`에서 수신자 존재·사용여부 확인 후 발송, `/message/detail`에서 발신자/수신자 본인 확인 후 수신자 읽음 처리, `/message/deleteMessage.json`에서 함별 소프트 삭제. | `MessageController`, `MessageServiceImpl`, `MessageMapper`, `Message_SQL.xml`, `V3__message.sql`, `templates/thymeleaf/message/*` | 쪽지 보존·파기 정책, 수신자 삭제 후 발신자 표시 정책, 메일·알림 연계 |
| 공지 고정 | CK 목록은 `NOTICE_YN DESC, REGIST_DT DESC, BOARD_ID DESC`로 정렬한다. 관리자 화면에서 `/board/updateNotice.json`으로 고정/해제하며, 서버가 관리자 세션과 CK 여부를 다시 확인한다. 관리·사용자 목록과 상세는 `NOTICE_YN='Y'`일 때 '공지' 배지를 보여준다. | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `V4__board_notice.sql`, `board/boardList.html`, `user/board/noticeList.html` | 고정 개수 제한·우선순위 정책, TOAST 확장 여부, 수동 화면 확인 |
| 메뉴 권한 | `PermissionServiceImpl.selectUserMenuPermissions`가 `APP_USER_ROLE`→`APP_ROLE_MENU`를 사용자별로 조회해 여러 역할을 메뉴별 OR로 합친다. `MenuPermissionInterceptor`는 컨트롤러→메뉴 매핑으로 GET/조회성 POST는 READ, 쓰기성 POST는 READ+WRITE를 검사하고, `ADMIN_YN` 세션은 통과시킨다. 거부 시 `.json`은 403 JSON, 화면은 403 오류다. `MenuInterceptor`는 같은 READ 권한으로 사이드바를 필터링한다. | `PermissionServiceImpl`, `Permission_SQL.xml`, `MenuPermissionInterceptor`, `EgovConfigWeb`, `V5__permission.sql` | `/system`·`/auth` 관리자 전용 정책과 역할 부여의 관계, 메뉴 매핑 누락, 역할 변경 즉시 반영 여부(캐시 없음), 수동 화면 확인 |
| 사이트맵 | `/sitemap`은 로그인 사용자에게 읽기 전용 메뉴 트리를 보여준다. `SitemapController`가 `MenuInterceptor`의 `menuList`를 모델에 넘기므로 사이드바와 같은 권한 필터가 적용되고, 메뉴가 없으면 안내 문구를 표시한다. 메뉴 저장·순서 변경 기능은 없다. | `SitemapController`, `comm/sitemap.html`, `layout/layout.html`, `EgovConfigWeb.MenuInterceptor`, `SitemapControllerTest` | 메뉴명·순서 변경은 메뉴관리 화면 담당, 외부 사용자 전용 사이트맵 분리 여부, 수동 화면 확인 |
| 통합검색 | `/search`는 로그인 사용자만 접근한다. 검색어가 있으면 BOARD의 CK·TOAST·FAQ·QNA 제목을 LIKE로 조회하고, `SEARCH_CHOSUNG`으로 초성 검색을 보조하며 최신순으로 페이징한다. 결과는 유형 라벨·제목·작성자·등록일과 HTML 태그를 제거한 본문 요약을 보여주고, 상세는 `/user/board/*`, `/user/faq/faqList`, `/user/qna/qnaDetail`로 연결한다. ES가 켜져 있으면 `SearchIndexService`가 색인 조회·재색인을 담당하고, 꺼져 있으면 DB LIKE를 쓴다. | `SearchController`, `SearchReindexController`, `SearchServiceImpl`, `SearchMapper`, `Search_SQL.xml`, `ElasticsearchSearchIndexService`, `search/searchList.html`, `SearchMapperTest`, `SearchServiceImplTest`, `SearchControllerTest` | 본문 검색은 HTML 오탐 때문에 미적용, ES 운영·동기화 정책, Q&A 내·외부 공개 범위, 검색 성능·입력 길이 제한, FAQ 결과의 정확한 위치 이동 |
| 감사 로그 | `AuditInterceptor`가 로그인·로그아웃·메뉴·개인정보 조회/다운로드·API 접근을 판정해 `AuditEvent`를 발행하고, `AuditLogEventListener`가 `AuditLogService`로 `AUDIT_LOG`에 적재한다. `/system/auditLogList`는 기간·이벤트·키워드로 조회하고 엑셀로 내려받는다. `AuditRetentionJob`은 `arkive.audit.retention-days`가 0이면 삭제하지 않는다. | `AuditInterceptor`, `AuditEvent`, `AuditLogEventListener`, `AuditLogServiceImpl`, `AuditLogController`, `V7__stage2_qna_audit.sql`, `AuditLogServiceTest`, `AuditLogControllerTest` | 인터셉터 대상 경로 전수, 비동기 유실, 보관주기 정책, 다중 인스턴스 중복 기록 |
| 개인정보 마스킹·암호화 | `MaskingInterceptor`가 화면 모델의 `List<EgovMap>`를 표시 직전에 마스킹하고, 감사 인터셉터가 개인정보 조회·다운로드를 기록한다. 주소록·명함은 `PiiFieldService`가 `telNo`·`email`을, `AuthServiceImpl`이 사용자 `phone`을 AES-GCM(`enc:v1:`)으로 저장·복호화한다. `APP_USER.EMAIL`은 조회 조건이라 평문이다. | 기존 코드 출처: `MaskingInterceptor`, `MaskingUtil`, `PiiFieldService`, `FieldCryptoService`, `AddressServiceImpl`, `CardServiceImpl`, `AuthServiceImpl`, `SmsServiceImpl`, `V15__field_crypto.sql`, `V34__user_phone_crypto.sql`. 자동 검증: `FieldCryptoServiceTest`, `PiiFieldServiceTest`, `MaskingUtilTest` (2026-11-08 Surefire 보고서 재대조) | 상세·엑셀 마스킹 누락, 운영 키 관리·회전, 기존 평문 데이터 이관 |
| 2차인증 (TOTP) | 비밀번호 로그인 성공 후 TOTP가 켜져 있으면 세션에 대기 정보를 두고 `/login/totpVerify.json`에서 코드 또는 복구코드를 검증한다. 5회 실패 시 대기를 폐기하고, 성공 시 `LoginSessionService.establishSession`으로 세션을 완성한다. 설정·해제·복구코드 발급은 `/login/totpSetup*`, 관리자 초기화는 `/auth/resetTotp.json`이다. | `LoginController`, `TotpServiceImpl`, `TotpMapper`, `UserManageController`, `V9__totp.sql`, `TotpServiceTest` | 로그인 E2E, 복구코드 분실·본인확인 정책, 다중 인스턴스 세션 |
| 약관동의 | `/terms`는 현재 약관을 공개하고, 로그인 후 `TermsInterceptor`가 필수 약관 미동의 사용자를 `/login/termsAgree`로 보낸다. 동의 저장은 `/login/termsAgree.json`이 `USER_TERMS_CONSENT`에 이력을 남기고 세션의 `PENDING_TERMS`를 해제한다. 회원가입·OAuth 가입도 필수 약관을 검사한다. | `TermsController`, `TermsServiceImpl`, `TermsMapper`, `TermsInterceptor`, `LoginController`, `OauthController`, `V10__terms.sql`, `TermsServiceTest`, `TermsInterceptorTest` | 실제 약관 문구(자리표시자), 선택 동의 철회, 버전 추가·재동의 정책 |
| SMS·알림 채널 | 게시글 저장 시 `BoardNotificationEvent`가 발행되면 `BoardNotificationEventListener`가 SSE·메일·SMS를 각각 처리한다. SMS는 `SmsServiceImpl`이 메뉴 조회 권한·`SMS_YN='Y'`·복호화한 휴대전화를 확인하고 `BOARD.SMS_SENT_YN`을 선점한 뒤 `AligoSmsSender`로 보낸다. 기본 비활성이다. | `BoardNotificationEvent`, `BoardNotificationEventListener`, `SmsServiceImpl`, `SmsAsyncService`, `AligoSmsSender`, `V17__sms.sql`, `Permission_SQL.xml`. 자동 검증: `AligoSmsSenderTest`, `SmsServiceImplTest`, `BoardNotificationEventListenerTest` (2026-11-08 Surefire 보고서 재대조) | 실제 알리고 발송·계약·발신번호·요금, 실패 재시도 |
| 통계·웹분석·내부 API | `/stats/statsList`가 `AUDIT_LOG`·게시판 데이터를 기간별로 집계해 화면과 엑셀로 제공한다. `AnalyticsModelAdvice`가 분석 설정을 전달하고 `arkive-analytics.js`가 동의 후 GA4·Clarity 스크립트를 로드한다. ID가 없으면 비활성이다. `/internal/v1/*`은 `InternalApiAuthFilter`가 `X-Internal-Token`을 상수시간 비교해 보호한다. | `StatsController`, `StatsServiceImpl`, `Stats_SQL.xml`, `AnalyticsModelAdvice`, `arkive-analytics.js`, `InternalApiController`, `InternalApiAuthFilter`. 자동 검증: `StatsControllerTest`, `AnalyticsModelAdviceTest`, `InternalApiControllerTest` (2026-11-08 Surefire 보고서 재대조) | 지표 정의, 실제 측정 ID·도메인·스크립트 동작, 운영 토큰 발급·회전 |
| SSE 역할 필터 | `/board/subscribe`가 로그인 세션의 역할을 저장하고, 새 글 등록 시 `BoardController`가 `BoardNotificationEvent`를 발행하면 `BoardNotificationEventListener`가 `selectRoleIdsWithMenuRead`로 해당 게시판 메뉴의 조회 역할을 구해 `broadcast(..., targetRoles)`로 겹치는 통로에만 보낸다. 역할이 없거나 대상 역할이 비면 발송하지 않는다. | `BoardController`, `BoardNotificationEvent`, `BoardNotificationEventListener`, `BoardSseService`, `Permission_SQL.xml`, `layout.html` | SSE 기본 비활성(`ARKIVE_SSE_ENABLED=false`), 다중 인스턴스 브로커, 화면 수동 확인 |
| 메일 발송 | 회원가입·사용자 등록·OAuth 가입 시 환영 메일, 비밀번호 찾기 시 재설정 링크 메일, CK 공지/TOAST 중요글 저장·전환 시 `BoardNotificationEvent`를 발행하고 `BoardNotificationEventListener`가 `MailAsyncService`로 비동기 요청한다. `MailServiceImpl`이 `arkive.mail.enabled`, 발신자, 수신 동의(`APP_USER.MAIL_YN`)를 다시 검사하고, 공지/중요글은 `BOARD.MAIL_SENT_YN`을 원자적으로 선점해 1회만 발송한다. 비밀번호 재설정은 거래성 메일로 수신 동의와 무관하게 이메일이 있으면 발송한다. SMTP 실패는 로그만 남기고 요청을 실패시키지 않는다. | `MailAsyncService`, `MailServiceImpl`, `EgovConfigAsync`, `LoginController`, `OauthController`, `UserManageController`, `BoardController`, `BoardNotificationEvent`, `BoardNotificationEventListener`, `Board_SQL.xml`, `application.properties` | 실제 SMTP 제공자·발신 도메인·SPF/DKIM, 대량 발송·수신거부 정책, 실패 재시도, 메일 본문/템플릿 소유자 |
| 비밀번호 재설정 | `/login/findPw.json`은 아이디·이름·이메일이 모두 일치할 때만 1회용 토큰을 발급하고, 화면에는 계정 존재 여부를 구분하지 않는 동일 성공 메시지를 반환한다. 토큰은 원문 대신 SHA-256 해시로 `PASSWORD_RESET_TOKEN`에 저장하고 30분 유효·1회 사용·60초 재발급 제한을 적용한다. `/login/resetPw`에서 새 비밀번호를 설정하면 토큰을 사용 처리하고 잠금·실패 횟수를 초기화하며 기존 로그인 세션을 만료한다. | `LoginController`, `AuthServiceImpl`, `AuthMapper`, `Auth_SQL.xml`, `V6__mail_notification.sql`, `login/findPw.html`, `login/resetPw.html` | IP별 요청 제한·감사 로그, 메일 전달 실패 시 사용자 안내, 운영 메일 계정, 링크 만료 안내 문구 |
| 레거시 초기화 SQL | `sampledb.sql`, `baseline_schema.sql`은 2026-10-11에 삭제했다. `db/data.sql`은 DBJob 덤프 대상 표시용 주석 파일로 남았다. | `application.properties`, `src/main/resources/db/data.sql` | 실제 서버 파일 권한·출력 위치, 백업 보관/복구 정책 |
| 예약 DB 작업 | `DBJob`이 10분마다 `Globals.dumpPath`(기본 `./backup/ArkiveDB.sql`)에 HSQLDB `SCRIPT` 덤프를 쓴다. 폴더 생성 후 기존 덤프 파일을 지우고 새로 기록한다. `src/main/resources/db/data.sql` 주석 파일과 실제 덤프 위치는 다르다. | `arkive/com/config/QuartzConfig.java`, `arkive/com/config/DBJob.java`, `application.properties` (2026-11-08 재대조) | 쓰기 권한, 이전 덤프 삭제 후 실패 시 복구, 보관·복구 정책 |
| eGovFrame 샘플 | Controller, Service, Mapper/XML, 샘플 CRUD 및 페이지 처리 코드가 있다. V1 baseline이 `SAMPLE`, `IDS`와 기본 레코드를 준비한다. | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml`, `db/versions/V1__baseline.sql` | 실제 업무 사용 여부 |

코드에서 확인되지 않는 정책·역할·데이터 의미는 담당자나 승인된 문서로 확인한다. 변경 전·후 차이는 해당 작업의 [Acceptance Criteria](../portable/acceptance-template.md), Baseline 및 [Evidence](../portable/evidence-report-template.md)와 연결해 기록한다.

## 기존 검증 이력

기존 `README.md`와 기능 상세에서 옮긴 실행 기록이다. 이번 문서 정리에서 GUI를 다시 확인한 것은 아니다. 현재 기능 분류는 [README.md](../../../README.md), 최신 자동 테스트 수치는 [테스트 자산](../../../src/test/README.md)에서 관리한다.

### 자동 검증

- 자동 검증 이력: 2026-11-07 `verify.cmd` 테스트 338개 통과, 실패·오류 0, JaCoCo 기준 충족(instruction 52.0% / branch 43.9% / line 52.7%). 이전 기록: 2026-10-25 테스트 314개 통과.
- 2026-11-08 조회수 통계 화면 수정 후 `verify.cmd` 재실행: 테스트 338개 통과, 실패·오류·skip 0, JaCoCo 기준 충족, `BUILD SUCCESS`. 테스트를 새로 추가하지 않아 개수는 11-07과 같다.

### 2026-11-07 조회수 통계

- GUI 기록(2026-11-07, 게시판 조회수 통계, ego-browser 0.6.2.7, `127.0.0.1:8080` 관리자 세션): `/board/viewStats`가 14일 막대 14칸·일별 표·합계·`게시글별 누적 조회수` 표를 렌더했고, 조회 로그가 없는 상태에서는 모든 막대가 `0%`·합계 0이었다. 게시글 상세를 1회 열자 오늘(11-07) 막대가 `1`·`100%`, 합계 1로 바뀌고 순위 표의 해당 글 조회수가 3→4로 증가했다. 같은 브라우저에서 상세를 다시 열어도 1로 유지돼 24시간 중복 집계가 화면에서도 확인됐다. 사이드바 '조회수 통계' 메뉴(V35 시드)도 노출된다. 스크린샷은 에고 창 최소화로 미확보(화면 확인은 DOM 스냅샷 기준).
- 당시 자동 검증: `BoardViewStatsControllerTest`의 14칸·오늘 값·막대 100%·합계, 24시간 내 재조회 시 로그 1건, 로그가 없을 때 `height: 0%` 렌더. 같은 날 `verify.cmd` 전체 338개 통과 기록과 함께 기능 상세에서 옮겼다.

### 2026-11-08 페이징과 이전글·다음글 GUI 확인

- 대상: ① 공지사항 오프셋 페이징(전자정부 `paginationInfo` + KRDS 블록), ② 자료실 커서+더보기(`/board/toastList.json`의 `cursorId`/`hasNext`/`nextCursorId`), ③ 상세 화면의 이전글·다음글.
- 준비: GUI로 다중 페이지를 확인하려면 데이터가 필요해 관리자 세션에서 임시 글 11건(CK)과 11건(TOAST)을 생성했다. 이중등록 방지 토큰은 글쓰기 화면(GET `/board/ckeditorWrite`·`/board/toastWrite`)의 `doubleSubmitToken`을 읽어 `egovframework.double.submit.preventer.parameter.name`으로 전송해야 통과한다(토큰 없이 호출하면 `중복 제출이 감지되었습니다`로 거부된다 — 이번에 확인).
- 결과 ① 공지 오프셋: `?searchKeyword=페이징GUI&pageIndex=1`에서 10행, 번호 11~2, 페이지 블록 `이전 1 2 다음`. `2` 클릭 후 `pageIndex=2`로 1행(번호 1)이고 활성 페이지가 `2`. 검색 조건이 페이지 이동 후에도 유지된다.
- 결과 ② 자료실 커서: 첫 화면 10행·번호 1~10·`더보기` 표시. `더보기` 클릭 후 11행으로 늘고 번호 1~11이 이어지며 `더보기`가 숨고 `마지막 글입니다.`가 표시된다. 전체 제목 11건이 모두 유일해 중복·누락이 없다. 상세 링크는 `/board/detail?boardId=...` 형태로 삽입된다.
- 결과 ③ 이전글/다음글: `페이징GUI CK 11` 상세에서 `다음글`은 "다음글이 없습니다."(최신글), `이전글`은 `페이징GUI CK 10` 링크. 그 링크를 클릭하자 CK 10 상세로 이동해 `다음글` CK 11·`이전글` CK 09로 바뀌었다(양방향 일치).
- 원상복구: 생성한 22건을 `deleteBoard.json`으로 모두 삭제했고(`ok 22`, 실패 0), 삭제 후 공지 목록은 원래 1건(`동작 확인 테스트 글`)으로 돌아왔다. 확인 후 로컬 앱은 종료했다.
- 기록 시점에서 화면 증적은 DOM 상태 기준이다(사용자 요청으로 스크린샷은 생략). 스크린샷을 찍으려면 ego 창이 최소화되지 않은 상태여야 한다.
- 당시 자동 검증: `verify.cmd` 테스트 338개 통과·실패 0·JaCoCo 기준 충족(`BUILD SUCCESS`). 페이징 자체는 `BoardPagingControllerTest`가 12건 fixture로 10/2 분할·비중복·`hasNext`·`nextCursorId`를 검증한다.
- 사용자 화면 확인(같은 날, 임시 글 11건씩 재생성 후 삭제): `/user/board/noticeList`는 1페이지 10행(번호 11~2)·`이전 1 2 다음` 블록이고 `2` 클릭 시 1행(번호 1)·활성 페이지 `2`. `/user/board/archiveList`는 첫 10행·`더보기` 표시 → 클릭 후 11행·제목 11건 모두 유일·`더보기` 숨김·`마지막 글입니다.` 표시. `/user/board/noticeDetail`은 CK 11에서 다음글 없음·이전글 CK 10 링크이고 클릭하면 CK 10으로 이동해 다음글 CK 11·이전글 CK 09로 바뀐다. `/user/board/archiveDetail`도 TOAST 11에서 다음글 없음·이전글 TOAST 10으로 같은 규칙이다.
- 사용자 화면 확인 후 원상복구: 생성한 22건 삭제(`22/22`), `/user/board/noticeList`는 다시 1건(`동작 확인 테스트 글`, 조회수 5), `/user/board/archiveList`는 `게시글이 없습니다.`로 복귀.

### 2026-11-08 조회수 통계 화면 폭 대응

- 수정 내용: `/board/viewStats`의 누적 순위 표 `등록일` 셀에 `white-space: nowrap`을 넣어 `2026-10-23`이 두 줄로 갈라지던 표시를 고쳤다. 이어서 차트·헤더의 인라인 스타일을 페이지 전용 클래스로 옮기고 `<style>`을 추가해 폭 대응을 넣었다(640px 이하에서 날짜 라벨 격일 표시, 마지막 칸인 오늘은 항상 표시, 차트 간격 4px, 일별 표의 최소 폭 720px 해제, 설명·버튼 `flex-wrap`). 표의 가로 스크롤은 기존 `.krds-table-wrap { overflow-x: auto }`를 그대로 쓴다.
- GUI 기록(2026-11-08, ego-browser 0.6.2.7, `http://localhost:8080` 관리자 `admin` 세션, `mvn spring-boot:run` 로컬 기동): 수정 전 등록일 셀은 텍스트 rect 2개(두 줄)였고 수정 후 1개(한 줄)로 렌더됐다. 폭 1920/1440/1280/1024/768/480/375에서 페이지 가로 넘침 0건, 표는 래퍼 안에서만 가로 스크롤됐다. 480px 이하에서 날짜 라벨이 `10-` / `26`으로 두 줄로 갈라지고 375에서 차트가 21px 넘쳤으며, 375에서 일별 표의 `조회수` 헤더가 잘려 보였다.
- 수정 후 재측정(폭 1920/1280/768/640/480/375/320): 페이지 가로 넘침 0건, 차트 넘침 0px, 라벨은 768 이상에서 14개·640 이하에서 7개이며 모두 1줄·겹침 0건, 오늘(11-08) 라벨 항상 표시, 일별 표 내부 스크롤 0px. 누적 순위 표는 5열이라 640 이하에서 내부 스크롤이 남는다(640 124px, 480 284px, 375 389px, 320 444px). 이 항목은 좁은 화면에서 조회수·등록일 열을 보려면 스크롤이 필요하다는 뜻이며 미해결로 둔다.
- 스크린샷: `C:/fb/gui-viewstats.png`, `C:/fb/gui-viewstats-full.png`, `C:/fb/gui-viewstats-datefix.png`, `C:/fb/resp-1920.png`, `C:/fb/resp2-1280.png`, `C:/fb/resp2-480.png`, `C:/fb/resp2-375.png`(에고 창이 최소화돼 있으면 캡처가 실패하므로 창을 복원한 뒤 찍었다).
- DB 부작용: 확인 중 게시글 상세를 1회 열어 `BOARD_VIEW_LOG`에 2026-11-08 로그 1건과 `BOARD.VIEW_CNT` +1이 남았다. 되돌리지 않았으므로 로컬 `data/ArkiveDB`에는 그 값이 남아 있다. 확인용으로 띄운 앱은 모두 종료했다.

### 2026-11-07 게시판 화면

- GUI 기록(2026-11-07, ego-browser 0.6.2.7, `127.0.0.1:8080` 로그인 세션): 관리자 CK 목록·등록·상세·댓글 등록/삭제·게시글 삭제·수정 폼 프리필·HWP식 표 붙여넣기 정상, TOAST 목록·글쓰기·저장·상세·삭제 정상, CK 에디터 이미지 업로드(10×10 PNG → `downloadFile.do` 200·image/png) 정상, 사용자 공지 목록·상세와 자료실 목록·상세(임시 등록 후 확인·삭제) 정상, 수정 저장(상세 반영 후 원문 복구) 정상, 콘솔 예외 0건. 검증용 게시글·댓글·업로드 이미지는 화면과 `setFileDelete.json`으로 삭제해 원상복구했다(원본 글 조회수만 2→3 증가, 등록일·댓글 유지).
- 기능 상세에 있던 확인 조건: CK 제목·본문 저장 후 목록/상세 반영·중복 제출 차단, 댓글·게시글 삭제 confirm 후 목록 복귀, 수정 폼 제목·본문·boardId 프리필. CK 툴바 '컴퓨터에서 이미지 업로드'의 PNG가 `editorImageUpload.json`의 `{uploaded:true,url}` 응답 후 본문 `<img>`로 삽입되고 재조회 200·image/png였다. TOAST는 목록 1행·상세 본문·이전/다음글·댓글 0건 확인 후 삭제했다. 자료실 임시 글은 `/user/board/archiveDetail`의 제목·등록일·조회수·본문·목록 링크와 `/user/board/archiveList` 1행을 확인한 뒤 삭제해 0건으로 복구했다. 수정 저장은 본문 표식 반영 후 원문을 되돌렸고 등록일·댓글을 유지했다.

### 2026-11-07 검색 범위

- 기능 상세에서 옮긴 당시 자동 검증: `BoardSearchScopeControllerTest`로 CK·TOAST·사용자 공지/자료실의 제목/내용 분리와 미지원 값의 title 폴백을 확인, `verify.cmd` 전체 321개 통과. 321개는 이 실행 시점의 이력이며 최신 개수가 아니다.
- 당시 ego-browser GUI 확인: CK·TOAST·자료실에서 제목/내용 분리 검색, 셀렉트 유지, 콘솔 오류 없음. 화면 스크린샷은 에고 창 최소화로 미확보. 이번 정리에서 GUI를 재실행하지 않았다.

### 동작 변경 이력

- 2026-11-07 조회수 집계를 세션 무기한 1회에서 **게시글당 브라우저(세션·쿠키) 24시간 1회**로 바꿨다. 세션에 본 시각을 두고 `boardView_<boardId>` 쿠키를 `Max-Age=86400`·HttpOnly로 함께 내려 세션이 끊겨도 하루 한 번만 세고, 24시간이 지나면 다시 센다. 자동 테스트(`BoardViewCountDailyWindowTest` 6건)와 GUI 확인(같은 브라우저 재조회 시 증가 없음)을 마쳤다.
- 2026-11-03 게시판 검색 범위(제목/내용/제목+내용)를 추가했다. 본문 검색은 CONTENT 원문 LIKE라 `table`·`style` 같은 영문 태그 문자열에 오탐이 날 수 있다(해결: 평문 컬럼 또는 DB별 `REGEXP_REPLACE`).
