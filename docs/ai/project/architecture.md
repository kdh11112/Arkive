# 아키텍처 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. 진입 클래스(BootApplication), 패키지 경계(업무·공통·설정)를 적는다.
2. 대표 1개 기능의 `Controller → Service → Mapper → XML` 흐름을 추적해 적는다.
3. 화면 경로(Thymeleaf/JSP), Mapper XML 경로, 설정 클래스 위치를 적는다.
4. 예약작업·외부연계가 있으면 주기·경로·소유자와 함께 적는다. 없으면 미확인으로 둔다.

## 양식 (Arkive 기록 참고)

| 항목 | 내용 | 근거 (파일·확인일) |
| --- | --- | --- |
| 진입 클래스 | 예: `com.example.BootApplication` | 소스 경로 |
| 패키지 경계 | 예: 업무 `biz.*`, 공통 `com.*` | 소스 경로 |
| 대표 호출 흐름 | 예: `XController → XService → XMapper → X_SQL.xml` | 각 파일 경로 |
| 화면·Mapper 위치 | 예: `templates/...`, `sqlmap/.../mappers/` | 경로 |
| 예약·외부연계 | 예: 10분 Trigger, 외부 API명 | 설정·명세 |

<!--
문서 상태: 부분 미완성
미완성 항목: 인증·인가 적용 범위/역할 매핑, 외부 연계 호출 흐름과 소유자, 프로파일별 구성, 운영 배포 토폴로지, 전체 모듈 경계.
필요한 근거/확인자: Security 설정과 필터·인터셉터, API 명세/호출부, 환경별 설정, 운영 아키텍처 문서 및 담당자.
완성 방법: 코드·설정에서 확인한 흐름을 먼저 보강하고, 운영 구성과 책임은 담당자 확인 자료를 근거로 추가한다.
완료 기준: 업무·공통 모듈 경계와 실제 인증·외부 연계·배포 구성이 출처와 함께 기록되거나, 미지원/미확인이 담당자 확인으로 종결되어 있다.
-->

> 저장소 코드에서 확인한 구현 구조다. 목표 아키텍처나 전체 운영 구성을 의미하지 않는다.

## 작성 예시 (Arkive) — 아래는 참고용

## 애플리케이션 구성 (Arkive 예시)

- 진입 클래스: `egovframework.example.EgovBootApplication`
- Spring 기반 MVC 및 Thymeleaf 템플릿 렌더링을 사용한다.
- 업무 코드에는 `arkive.*`, eGovFrame 샘플·설정에는 `egovframework.example.*`, 공통 eGovFrame 코드에는 `egovframework.com.*` 패키지가 함께 있다.
- Arkive 시스템 기능 예: `arkive.admin.system.web.SystemController` → `SystemService` / `SystemServiceImpl` → `SystemMapper` → `System_SQL.xml`.
- Mapper XML은 `src/main/resources/egovframework/sqlmap/example/mappers/`에 있다. 인터페이스, XML namespace, SQL ID, 파라미터와 반환 타입을 함께 확인한다.
- Thymeleaf 화면은 `src/main/resources/templates/thymeleaf/` 아래에 있으며 레이아웃·공통 조각은 같은 디렉터리의 `layout/` 등에서 관리한다.
- 템플릿에서 `th:replace` 조각 사용과 화면 JavaScript의 jQuery `$.ajax` 호출이 확인된다. 요청·오류 표시의 세부 동작은 대상 화면과 호출 API에서 확인한다.
- 공통 파일 기능은 `arkive.admin.comm` 아래 Controller, Service, Mapper, 업로드/다운로드 유틸리티로 구성되어 있다.
- 게시글 댓글: `BoardController`의 상세 조회가 `BoardService.selectCommentList/selectCommentCount`를 호출하고, `/board/insertComment.json`·`/board/deleteComment.json`이 `BoardService` → `BoardMapper` → `Board_SQL.xml`로 이어진다. 화면은 `templates/thymeleaf/board/boardDetail.html`이다.
- 쪽지: `MessageController`(`/message`) → `MessageService`/`MessageServiceImpl` → `MessageMapper` → `Message_SQL.xml`. 화면은 `templates/thymeleaf/message/`의 목록·작성·상세이며, 수신자가 상세를 열 때 읽음 처리한다.
- 로그인 세션: `LoginSessionRegistry`가 사용자별 최신 세션 1개를 유지한다. `LoginController`·`OauthController`가 로그인 성공 시 등록하고, 같은 사용자의 새 로그인은 이전 세션을 만료시킨다. `EgovConfigWeb`이 `HttpSessionListener`로 등록해 세션 만료 시 매핑을 지운다. 단일 인스턴스 메모리 기준이다.
- 권한: `APP_USER_ROLE`로 사용자에 역할을 배정하고 `APP_ROLE_MENU`가 역할별 메뉴 조회·쓰기를 정의한다. `PermissionServiceImpl`이 여러 역할을 메뉴별 OR로 합치고, `MenuPermissionInterceptor`가 컨트롤러→메뉴 매핑으로 READ/WRITE를 서버에서 강제한다. `MenuInterceptor`는 같은 조회 권한으로 사이드바를 필터링한다. `ADMIN_YN='Y'`는 슈퍼관리자로 통과하며 `/system`·`/auth`는 기존 `AuthInterceptor` 정책상 관리자만 접근한다.
- 사이트맵: `SitemapController`(`/sitemap`)가 `MenuInterceptor`의 `menuList`를 화면에 넘겨 읽기 전용 메뉴 트리를 렌더한다. 별도 권한 필터를 다시 구현하지 않으므로 사이드바와 같은 메뉴만 보인다. 메뉴가 없는 컨트롤러라 `MenuPermissionInterceptor` 매핑에는 넣지 않으며, 로그인 검사만 `AuthInterceptor`가 담당한다.
- 메일: `LoginController`·`OauthController`(환영), `UserManageController`(관리자 등록), `LoginController`(비밀번호 재설정)가 `MailAsyncService`를 호출한다. 게시판 공지/중요글은 `BoardController`가 `BoardNotificationEvent`를 발행하고 `BoardNotificationEventListener`가 `MailAsyncService`로 넘긴다. `EgovConfigAsync`의 `mailTaskExecutor`가 요청 스레드에서 분리하고, `MailServiceImpl`이 `arkive.mail.enabled`·발신자·수신 동의를 재검사한 뒤 `JavaMailSender`로 보낸다. 공지/중요글은 `BOARD.MAIL_SENT_YN` 선점으로 1회만 발송한다.
- 비밀번호 재설정: `AuthServiceImpl.issuePasswordResetToken`이 32바이트 난수를 URL-safe Base64로 만들고 SHA-256 해시만 `PASSWORD_RESET_TOKEN`에 저장한다. `LoginController`의 `/login/resetPw`가 토큰 유효성(30분·1회)을 확인하고 `AuthServiceImpl.resetPasswordByToken`이 비밀번호 변경·잠금 해제·세션 만료를 처리한다.
- DB, Mapper, 트랜잭션 및 웹 설정은 `egovframework.example.config`에 Java Config로 존재한다.
- Controller에 `@Valid`와 `BindingResult`를 사용하는 입력 검증 예제가 있다. 실제 변경은 해당 기능의 서버 검증 패턴을 확인한다.
- 로깅은 Lombok `@Slf4j`와 SLF4J `LoggerFactory` 직접 사용이 함께 확인된다. 수정 시 대상 모듈의 기존 방식을 따른다.

## 비동기 및 예약 작업

- `arkive.com.config.QuartzConfig`에서 `DBJob`을 10분 간격으로 실행하는 Trigger를 구성한다.
- `DBJob`은 DB 덤프를 파일로 기록하는 코드를 포함한다. 스케줄 활성화, 경로, 보존 및 복구 정책은 [project-overview.md](project-overview.md)와 운영 담당자 확인을 참조한다.
- `EgovConfigAsync`가 `mailTaskExecutor`를 등록하고 `@Async("mailTaskExecutor")` 메일 호출을 처리한다. 기본 `arkive.mail.enabled=false`이면 실제 SMTP 연결 없이 즉시 반환한다.

## 새 글 알림(SSE)

- `BoardController`의 `GET /board/subscribe`가 로그인 세션의 역할을 조회해 `BoardSseService.subscribe(boardType, roles)`를 호출하고, `SseEmitter` 연결을 30분간 유지한다.
- 새 글 등록 성공 후 `BoardController`가 `BoardNotificationEvent(notifySse=true)`를 발행하고, `BoardNotificationEventListener`가 해당 메뉴의 조회 권한 역할(`selectRoleIdsWithMenuRead`)을 조회해 `BoardSseService.broadcast(..., targetRoles)`로 해당 게시판과 `ALL` 통로에 `{boardId,title,boardType}`을 보낸다. 역할이 겹치는 구독자에게만 보내며, 대상 역할이 비면 발송하지 않는다. 통로는 메모리에만 있으므로 단일 서버 기준이며, 서버 재시작 시 브라우저가 재구독한다.
- `layout.html`은 전역 배너와 SSE 구독 스크립트를 포함하지만, 현재 `ARKIVE_SSE_ENABLED=false`라 기본 화면에서는 구독하지 않는다. 활성화하려면 `true`로 바꾸고, 페이지별 `window.ARKIVE_SSE` 선언이 layout 스크립트보다 먼저 실행되는지 확인한다.
- 화면별 설정: `window.ARKIVE_SSE = { banner: false };`는 해당 화면에서 구독·배너를 끄고, `window.ARKIVE_SSE = { hideAfterMs: 30000 };`은 30초 뒤 배너를 자동으로 숨긴다. 기본값은 계속 표시다.
- 역할 필터는 활성화되어 있다. 댓글·쪽지·다운로드·SSE 구독은 메뉴 CRUD 권한과 분리해 로그인 사용자 공통 기능으로 둔다.

## 메일·비밀번호 재설정

- 메일 설정 키는 `application.properties`의 `arkive.mail.enabled`, `arkive.mail.from`, `arkive.mail.base-url`과 Spring `spring.mail.*`이다. 실제 값은 환경변수로 주입하며 저장소에 자격증명을 두지 않는다.
- `MailServiceImpl.sendWelcome`·`sendBoardNotification`은 `APP_USER.MAIL_YN='Y'`와 유효한 이메일을 가진 사용자만 대상으로 한다. `sendPasswordReset`은 거래성 메일이므로 `MAIL_YN`과 무관하게 이메일이 있으면 보낸다.
- `sendBoardNotification`은 `BOARD.MAIL_SENT_YN='N'`인 글만 `Y`로 선점한 뒤 수신자에게 보낸다. 같은 글을 다시 저장하거나 공지 고정을 반복해도 두 번 보내지 않는다. 선점 후 SMTP가 실패해도 선점은 유지되므로 자동 재시도는 없다(정책 미확인).
- 비밀번호 재설정 토큰은 원문을 저장하지 않는다. `PASSWORD_RESET_TOKEN.TOKEN_HASH`에 SHA-256 해시를 저장하고, `EXPIRES_DT`(30분)·`USED_YN`·`REGIST_DT`로 유효성을 판정한다. 같은 사용자의 재발급은 60초 쿨다운을 두며, 새 토큰 발급 시 이전 미사용 토큰을 폐기한다.
- 메일 본문은 `MailServiceImpl`에 하드코딩된 단순 HTML이다. 운영 템플릿·다국어·수신거부 링크·발송 이력은 미구현이며, 대상 프로젝트에서 요구사항을 확인해야 한다.

## 참조와 개선 결정 (Arkive 예시)

기능을 만들 때 아래를 참고하고, 더 나은 방법이 있으면 그 방법을 사용한다. 똑같이 만드는 게 목표가 아니다.

- eGovFrame 공통컴포넌트 v5.0 가이드: https://www.egovframe.go.kr/wiki/doku.php?id=egovframework:com:v5.0:init_guide
  - 명함관리(cop)·상담관리(uss) 등 업무 규칙·테이블·상태코드의 근거로 사용했다.
- 저장소 내 실측 프로젝트: ADTM, BLMS, gcsms, GIES, GTEMS (pony-main, GCG-master, HTA-main 포함 survey)
  - BLMS 팝업·배너·카테고리·FAQ/Q&A, pony 공지·Q&A, gcsms 당직·엑셀·민원, GTEMS 설문(qestnr)을 CRUD·스키마 패턴 근거로 사용했다.
  - ADTM(지역 JOIN)·HTA(raw JDBC)는 이식 부적합이라 제외했다.

대표 개선 예시:

| 기능 | 참조 | 더 나은 방식 |
| --- | --- | --- |
| FAQ·Q&A 질문 | BLMS/gcsms 단일테이블+타입코드 | 신규 테이블 없이 `BOARD`+`BOARD_TYPE` 재사용 |
| Q&A·메모 상태 | pony `answerStatus` 컬럼 | Q&A는 상태 컬럼 제거(JOIN 판정), 메모는 상태 전이를 SQL 조건으로 강제 (동기화 불일치 제거) |
| 카테고리 트리 | BLMS jstree 라이브러리 | 네이티브 `<details>` + jstree 규격 `tree.json` 병행 (의존성 제거) |
| 메인이미지 | eGov 별도 컴포넌트 | `BANNER`+`POSITION` 통합 |
| 파일 실형식 검증 | 로드맵 Tika案 | `FileTypeUtil` 매직바이트 검사 (의존성 추가 없음) |
| 온라인POLL 중복투표 | 로그인 전제 방식 | 세션 집합 차단 |
| 게시글 댓글 | eGov cop/cmt 세트 | 기존 `BOARD`에 종속된 1단 댓글로 단순화(별도 게시판 마스터 미도입), 작성자/관리자 삭제 |
| 쪽지 | eGov uss 쪽지 | `MESSAGE` 단일 테이블 + 발신/수신 삭제 플래그로 함별 소프트 삭제 |

## 확인이 필요한 점

- 인증·인가 필터/인터셉터 적용 범위는 코드에서 확인했으나, 운영 역할·메뉴 매핑과 `/system`·`/auth` 관리자 전용 정책의 최종 승인은 미확인
- 외부 시스템 연계별 호출 흐름, 소유자, 타임아웃 및 오류 처리 계약
- SMTP 제공자·발신 도메인·SPF/DKIM·반송/수신거부·대량 발송 정책과 메일 발송 이력·재시도 설계
- 프로파일별 구성과 운영 배포 토폴로지
- 전체 모듈 경계 및 기능 소유 조직

## 타 프로젝트로 이식할 때 복사 범위

- 기능 단위 복사: 화면(Thymeleaf+JS) → Controller → Service/Impl → Mapper 인터페이스 → Mapper XML → `db/versions` SQL → `application.properties`·Globals 키 → `pom.xml` 의존성 순서로 가져간다.
- 메일·비밀번호 재설정을 복사할 때는 `spring-boot-starter-mail`, `EgovConfigAsync`, `MailService`/`MailAsyncService`/`MailServiceImpl`, `AuthServiceImpl`의 토큰 메서드, `LoginController`의 화면·API, `V6__mail_notification.sql`, `spring.mail.*`·`arkive.mail.*` 설정을 함께 가져간다. 게시판 알림은 `BoardNotificationEvent`·`BoardNotificationEventListener`와 해당 테스트도 함께 복사한다. 실제 SMTP 값은 대상 환경변수로 주입한다.
- 사이트맵을 복사할 때는 `SitemapController`, `templates/thymeleaf/comm/sitemap.html`, `layout/layout.html`의 링크, `MenuInterceptor`의 `menuList` 모델, `SitemapControllerTest`를 함께 가져간다. 메뉴 테이블과 권한 필터가 없는 프로젝트에서는 먼저 메뉴·권한 구조를 준비한다.
- 프로젝트명·패키지(`arkive.*`)·테이블명·파일 경로·DB 문법은 대상 프로젝트에 맞게 최소한만 바꾼다.

수정 전 실제 관련 호출 흐름과 설정을 검색한다. 이 요약만으로 보안 적용이나 기능 소유권을 단정하지 않는다.
