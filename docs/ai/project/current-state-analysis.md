# 기존 코드 현행 분석 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. 기능별로 입력→처리→결과·오류를 추적해 1행씩 적는다. 코드 관찰과 담당자 확인을 구분한다.
2. 근거(파일·화면·로그)와 미확인 사항을 반드시 함께 적는다.

<!--
문서 상태: 부분 미완성
미완성 항목: 메뉴·파일·예약 작업의 실제 화면 호출과 업무 흐름, 역할별 처리, 예외/오류 응답 및 데이터 상태 전이. 현재 기록은 저장소에서 찾은 일부 코드 흐름이다.
필요한 근거/확인자: 현행 화면·API 실행 결과, 운영 로그(민감정보 마스킹), 업무 담당자 확인, 기존 시스템 문서.
완성 방법: 기능별로 보이는 입력부터 DB/외부 연계까지 추적하고, 코드 관찰과 담당자가 확인한 실제 현행 동작을 구분해 표에 기록한다.
완료 기준: 분석 범위의 각 기능에 호출자·사전조건·처리·결과·오류·근거가 작성되고 미확인 동작이 별도 표시되어 있다.
-->

> 이 문서는 현재 저장소 구현에서 관찰한 동작 흐름을 기록한다. 과거 운영 시스템의 동작, 업무 의도 또는 승인된 정책을 뜻하지 않는다. 저장소 분석 시점: 2026-09-25, 2026-10-22, 2026-10-23 보강(메일·비밀번호 재설정 포함).

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 기능 흐름 (Arkive 예시)

| 영역 | 코드에서 확인한 흐름 | 확인된 출처 | 추가 확인 필요 |
| --- | --- | --- | --- |
| 로그인·세션 | `/login/login.json` 성공 시 세션 ID를 새로 발급하고 `LOGIN_USER`·`ADMIN_USER`를 저장한 뒤 `LoginSessionRegistry`에 등록한다. 같은 사용자로 다시 로그인하면 이전 세션이 만료된다. OAuth 콜백·계정연계도 같은 레지스트리를 사용한다. | `LoginController`, `OauthController`, `LoginSessionRegistry`, `EgovConfigWeb` | 기존 로그인 거부 방식, 다중 인스턴스 공유 세션, 세션 타임아웃 실제 동작 |
| 메뉴 관리 | `/system/menuList` 화면에서 Controller가 Service를 호출하고 Mapper/XML로 조회·등록·수정·삭제를 연결한다. | `SystemController`, `SystemServiceImpl`, `SystemMapper`, `System_SQL.xml` | 화면 호출 상세, 인증/권한, 메뉴 정책, 오류 계약 |
| 파일 처리 | 공통 FileController가 업로드·목록·다운로드·삭제 및 TUS 경로를 제공하고 파일 Service/Mapper 및 파일 저장 경로 설정을 사용한다. | `FileController`, `FileServiceImpl`, `File_SQL.xml`, `application.properties` | 화면별 호출 관계, 파일 접근 권한, 보존·삭제 정책, 외부 연계 |
| DB 초기화/마이그레이션 | Spring SQL 초기화는 꺼져 있다. 애플리케이션 시작 시 `SCHEMA_MIGRATION`에 없는 `db/versions/V1__baseline.sql`부터 `V6__mail_notification.sql`까지 숫자 순으로 실행한다. 2026-10-11에 구 체인을 단일 V1으로 합쳤고, 2026-10-22에 V2·V3, 2026-10-23에 V4·V5·V6을 추가했다. | `application.properties`, `EgovConfigDatasource`, `src/main/resources/db/versions/*.sql` | 운영 DB 제품·버전과 백업/복구 절차 |
| 게시글 댓글 | 게시글 상세에서 댓글 목록·건수를 조회한다. 로그인 사용자는 `/board/insertComment.json`으로 등록하고, 작성자 또는 관리자만 `/board/deleteComment.json`으로 삭제한다. 댓글은 1단 평문 저장 후 `th:text`로 출력하며 게시글 삭제 시 cascade된다. | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `V2__board_comment.sql`, `board/boardDetail.html` | 댓글 수정·신고·차단 정책, 관리자 판정 범위, 알림 연계 |
| 쪽지관리 | `/message/inbox`·`/sent`에서 로그인 사용자 기준 목록·검색·페이징, `/message/write`에서 수신자 존재·사용여부 확인 후 발송, `/message/detail`에서 발신자/수신자 본인 확인 후 수신자 읽음 처리, `/message/deleteMessage.json`에서 함별 소프트 삭제. | `MessageController`, `MessageServiceImpl`, `MessageMapper`, `Message_SQL.xml`, `V3__message.sql`, `templates/thymeleaf/message/*` | 쪽지 보존·파기 정책, 수신자 삭제 후 발신자 표시 정책, 메일·알림 연계 |
| 공지 고정 | CK 목록은 `NOTICE_YN DESC, REGIST_DT DESC, BOARD_ID DESC`로 정렬한다. 관리자 화면에서 `/board/updateNotice.json`으로 고정/해제하며, 서버가 관리자 세션과 CK 여부를 다시 확인한다. 관리·사용자 목록과 상세는 `NOTICE_YN='Y'`일 때 '공지' 배지를 보여준다. | `BoardController`, `BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `V4__board_notice.sql`, `board/boardList.html`, `user/board/noticeList.html` | 고정 개수 제한·우선순위 정책, TOAST 확장 여부, 수동 화면 확인 |
| 메뉴 권한 | `PermissionServiceImpl.selectUserMenuPermissions`가 `APP_USER_ROLE`→`APP_ROLE_MENU`를 사용자별로 조회해 여러 역할을 메뉴별 OR로 합친다. `MenuPermissionInterceptor`는 컨트롤러→메뉴 매핑으로 GET/조회성 POST는 READ, 쓰기성 POST는 READ+WRITE를 검사하고, `ADMIN_YN` 세션은 통과시킨다. 거부 시 `.json`은 403 JSON, 화면은 403 오류다. `MenuInterceptor`는 같은 READ 권한으로 사이드바를 필터링한다. | `PermissionServiceImpl`, `Permission_SQL.xml`, `MenuPermissionInterceptor`, `EgovConfigWeb`, `V5__permission.sql` | `/system`·`/auth` 관리자 전용 정책과 역할 부여의 관계, 메뉴 매핑 누락, 역할 변경 즉시 반영 여부(캐시 없음), 수동 화면 확인 |
| 사이트맵 | `/sitemap`은 로그인 사용자에게 읽기 전용 메뉴 트리를 보여준다. `SitemapController`가 `MenuInterceptor`의 `menuList`를 모델에 넘기므로 사이드바와 같은 권한 필터가 적용되고, 메뉴가 없으면 안내 문구를 표시한다. 메뉴 저장·순서 변경 기능은 없다. | `SitemapController`, `comm/sitemap.html`, `layout/layout.html`, `EgovConfigWeb.MenuInterceptor`, `SitemapControllerTest` | 메뉴명·순서 변경은 메뉴관리 화면 담당, 외부 사용자 전용 사이트맵 분리 여부, 수동 화면 확인 |
| 통합검색 (1단계) | `/search`는 로그인 사용자만 접근한다. 검색어가 있으면 BOARD의 CK·TOAST·FAQ·QNA 제목을 LIKE로 조회하고 최신순으로 페이징한다. 결과는 유형 라벨·제목·작성자·등록일과 HTML 태그를 제거한 본문 요약을 보여주며, 상세는 `/user/board/*`, `/user/faq/faqList`, `/user/qna/qnaDetail`로 연결한다. 빈 검색어와 무결과는 각각 안내 문구를 표시한다. | `SearchController`, `SearchServiceImpl`, `SearchMapper`, `Search_SQL.xml`, `search/searchList.html`, `SearchMapperTest`, `SearchServiceImplTest`, `SearchControllerTest` | 본문 검색은 HTML 오탐 때문에 미적용, 초성·ES 미적용, Q&A 내·외부 공개 범위 정책, 검색 성능·입력 길이 제한, FAQ 결과의 정확한 위치 이동 |
| SSE 역할 필터 | `/board/subscribe`가 로그인 세션의 역할을 저장하고, 새 글 등록 시 `BoardController`가 `BoardNotificationEvent`를 발행하면 `BoardNotificationEventListener`가 `selectRoleIdsWithMenuRead`로 해당 게시판 메뉴의 조회 역할을 구해 `broadcast(..., targetRoles)`로 겹치는 통로에만 보낸다. 역할이 없거나 대상 역할이 비면 발송하지 않는다. | `BoardController`, `BoardNotificationEvent`, `BoardNotificationEventListener`, `BoardSseService`, `Permission_SQL.xml`, `layout.html` | SSE 기본 비활성(`ARKIVE_SSE_ENABLED=false`), 다중 인스턴스 브로커, 화면 수동 확인 |
| 메일 발송 | 회원가입·사용자 등록·OAuth 가입 시 환영 메일, 비밀번호 찾기 시 재설정 링크 메일, CK 공지/TOAST 중요글 저장·전환 시 `BoardNotificationEvent`를 발행하고 `BoardNotificationEventListener`가 `MailAsyncService`로 비동기 요청한다. `MailServiceImpl`이 `arkive.mail.enabled`, 발신자, 수신 동의(`APP_USER.MAIL_YN`)를 다시 검사하고, 공지/중요글은 `BOARD.MAIL_SENT_YN`을 원자적으로 선점해 1회만 발송한다. 비밀번호 재설정은 거래성 메일로 수신 동의와 무관하게 이메일이 있으면 발송한다. SMTP 실패는 로그만 남기고 요청을 실패시키지 않는다. | `MailAsyncService`, `MailServiceImpl`, `EgovConfigAsync`, `LoginController`, `OauthController`, `UserManageController`, `BoardController`, `BoardNotificationEvent`, `BoardNotificationEventListener`, `Board_SQL.xml`, `application.properties` | 실제 SMTP 제공자·발신 도메인·SPF/DKIM, 대량 발송·수신거부 정책, 실패 재시도, 메일 본문/템플릿 소유자 |
| 비밀번호 재설정 | `/login/findPw.json`은 아이디·이름·이메일이 모두 일치할 때만 1회용 토큰을 발급하고, 화면에는 계정 존재 여부를 구분하지 않는 동일 성공 메시지를 반환한다. 토큰은 원문 대신 SHA-256 해시로 `PASSWORD_RESET_TOKEN`에 저장하고 30분 유효·1회 사용·60초 재발급 제한을 적용한다. `/login/resetPw`에서 새 비밀번호를 설정하면 토큰을 사용 처리하고 잠금·실패 횟수를 초기화하며 기존 로그인 세션을 만료한다. | `LoginController`, `AuthServiceImpl`, `AuthMapper`, `Auth_SQL.xml`, `V6__mail_notification.sql`, `login/findPw.html`, `login/resetPw.html` | IP별 요청 제한·감사 로그, 메일 전달 실패 시 사용자 안내, 운영 메일 계정, 링크 만료 안내 문구 |
| 레거시 초기화 SQL | `sampledb.sql`, `baseline_schema.sql`은 2026-10-11에 삭제했다. `db/data.sql`은 DBJob 덤프 대상 표시용 주석 파일로 남았다. | `application.properties`, `src/main/resources/db/data.sql` | 실제 서버 파일 권한·출력 위치, 백업 보관/복구 정책 |
| 예약 DB 작업 | Quartz 설정이 `DBJob`을 10분마다 실행하도록 Trigger를 선언하고, Job은 HSQLDB `SCRIPT` 명령에 `/db/data.sql` 경로를 전달한다. 이 절대 경로가 저장소의 `src/main/resources/db/data.sql`을 뜻한다고 보장되지 않는다. | `QuartzConfig`, `DBJob`, `src/main/resources/db/data.sql` | 실제 서버 파일 권한·출력 위치, 백업 보관/복구 정책 |
| eGovFrame 샘플 | Controller, Service, Mapper/XML, 샘플 CRUD 및 페이지 처리 코드가 있다. V1 baseline이 `SAMPLE`, `IDS`와 기본 레코드를 준비한다. | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml`, `db/versions/V1__baseline.sql` | 실제 업무 사용 여부 |

## 현행 분석 보완 양식

| 기능 | 사용자/호출자 | 사전 조건 | 현재 입력/처리/상태 전이 | 현재 결과/오류 | 근거(경로·화면·로그) | 미확인 사항 |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

코드에서 확인되지 않는 정책·역할·데이터 의미는 담당자나 승인된 문서로 확인한다. 변경 전·후 차이는 해당 작업의 [Acceptance Criteria](../portable/acceptance-template.md), Baseline 및 [Evidence](../portable/evidence-report-template.md)와 연결해 기록한다.
