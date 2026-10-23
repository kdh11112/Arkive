# 변경 영향 분석 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 기능별 전체 호출·소비 관계와 API/DB/외부 연계 영향 지도. 아래 표는 검색에서 확인한 공유 지점 일부다.
필요한 근거/확인자: 화면/API 호출 검색, Mapper·테이블 참조 검색, 배치/예약 작업 및 외부 시스템 목록, 기능 담당자 확인.
완성 방법: 변경 작업마다 영향 기록 표를 채우고, 반복 변경되는 공통 의존성은 근거를 확인해 공유 지점 목록에 반영한다.
완료 기준: 변경 대상과 관련된 호출자·피호출자·공통 사용처·데이터/운영 영향이 근거와 함께 추적 가능하다. 전체 지도가 불가능하면 조사 범위와 제외 범위를 명시한다.
-->

## 공통 분석 방법

변경 전 다음 연결을 따라 사용처와 영향을 확인하고 근거를 남긴다.

```text
화면/DOM/JavaScript → Controller/API → Service → Mapper/SQL → DB 객체/데이터
                                      ↘ 외부 연계/배치/예약 작업
```

- 관련 호출자·피호출자, 공통 모듈, 다른 화면, 예약 작업, 외부 시스템을 검색한다.
- API Request/Response, 업무 정책·권한, 데이터 스키마, 배포 순서와 복구 영향을 구분한다.
- 영향 없음 판단에는 확인한 호출처, 검색 범위 또는 근거를 적는다.
- 변경 전 Baseline과 필요한 Snapshot은 [verification-standard.md](../common/verification-standard.md)에 따라 기록한다.

## 작성 예시 (Arkive) — 아래는 참고용

## Arkive에서 확인된 공유 지점 (예시)

| 공유 지점 | 확인된 소비/영향 경로 | 변경 시 확인할 내용 |
| --- | --- | --- |
| 시스템 메뉴 Mapper/SQL | `SystemController` → `SystemService` → `SystemMapper` → `System_SQL.xml` | 메뉴 화면·AJAX 호출, 메뉴 초기 데이터와 테이블 |
| 파일 공통 처리 | `FileController` → `FileService` / 파일 유틸 → `File_SQL.xml` 및 파일 시스템 | 업로드 유형, 다운로드·삭제 호출처, 저장 경로, DB 메타데이터, 파일 접근 보안 |
| 공통 화면 조각/정적 자원 | Thymeleaf `layout/`, `comm/`, `static/` | 이를 포함하거나 사용하는 각 화면 |
| DB 초기화/예약 작업 | `application.properties`, `db/*.sql`, `QuartzConfig`, `DBJob` | 시작 시 초기화 동작, DB 변경, 덤프 경로·스케줄 |
| 메뉴 시드·사이드바 | `db/versions/V3__message.sql`, `V5__permission.sql` → `menu`, `MenuInterceptor` | 신규 메뉴 ID·상위 메뉴·순서, 기존 메뉴 ID 충돌, 역할별 조회 권한 |
| 메뉴 권한 | `PermissionService`/`PermissionServiceImpl`/`PermissionMapper`/`Permission_SQL.xml`, `MenuPermissionInterceptor`, `MenuInterceptor`, `RoleManageController` | 컨트롤러→메뉴 매핑, READ/WRITE 판정, 관리자 우회, 사이드바 필터, SSE 대상 역할 |
| 로그인 세션·역할 | `LoginController`, `OauthController`, `LoginSessionRegistry`, `AuthServiceImpl`, `PermissionService` | 로그인·로그아웃·세션 만료, 중복 로그인, 사용자 삭제 시 역할 매핑 정리 |
| 메일 발송 | `MailAsyncService` → `MailServiceImpl` → `JavaMailSender`, `EgovConfigAsync` | 로그인·OAuth·사용자관리·게시판·비밀번호 찾기 호출처, SMTP 설정·수신 동의·발송 이력, 테스트 환경 비활성 |
| 비밀번호 재설정 토큰 | `LoginController`, `AuthServiceImpl`, `AuthMapper`/`Auth_SQL.xml`, `PASSWORD_RESET_TOKEN` | 로그인 화면, 메일 링크, 세션 무효화, 사용자 잠금 해제, 토큰 만료·1회 사용 |
| 공통 오류·헬스 | `EgovConfigWeb`의 `SimpleMappingExceptionResolver`, `application.properties`의 Actuator·메일 헬스 설정 | 모든 미매핑 URL·정적 리소스, 운영 health, 메일 발송 활성화 여부 |

위 표는 검색으로 확인된 일부 공유 지점이며 완전한 의존성 그래프가 아니다. 실제 변경별 호출처를 다시 검색한다.

## 작업별 기록

| 변경 파일/기능 | 직접 호출자 | 공통 사용처 | API/정책/권한 | DB/외부 영향 | 검증 근거 |
| --- | --- | --- | --- | --- | --- |
| 게시글 댓글 (`BoardController`/`BoardService`/`BoardMapper`/`Board_SQL.xml`, `V2__board_comment.sql`) | `board/boardDetail.html` 댓글 AJAX | 게시판 상세·댓글 API | 로그인 사용자 작성, 작성자/관리자 삭제 | `BOARD_COMMENT` 신규, `BOARD` 삭제 시 cascade | `BoardMapperTest`·`BoardCommentControllerTest`, `verify.cmd` 105/105 |
| 쪽지 (`MessageController`/`MessageService`/`MessageMapper`/`Message_SQL.xml`, `V3__message.sql`) | `templates/thymeleaf/message/*` | `/message/**`, 레이아웃 메뉴 | 로그인 사용자, 발신자/수신자 본인만 열람·삭제 | `MESSAGE` 신규, `menu` 4행 | `MessageMapperTest`·`MessageControllerTest`, `verify.cmd` 105/105 |
| 공지 고정 (`BoardController`/`BoardService`/`BoardMapper`/`Board_SQL.xml`, `V4__board_notice.sql`) | `board/boardList.html` 토글, `user/board/noticeList.html` 배지 | CK 목록 공통 쿼리(`selectBoardList`)를 쓰는 대시보드·사용자 메인·API 공지 목록 | 관리자만 변경, CK만 허용 | `BOARD.NOTICE_YN`·`IDX_BOARD_NOTICE` 추가, TOAST 커서 쿼리는 영향 없음 | `BoardNoticeMapperTest`·`BoardNoticeControllerTest`, `verify.cmd` 105/105 |
| 중복 로그인 방지 (`LoginController`/`OauthController`/`LoginSessionRegistry`, `EgovConfigWeb`) | 로그인 성공 경로(폼·OAuth 콜백·계정연계) | 세션 로그인 화면 전체, `AuthInterceptor` | 새 로그인 우선(이전 세션 만료), 관리자 포함 전체 사용자 | DB 변경 없음, 메모리 레지스트리(단일 인스턴스) | `LoginSessionRegistryTest`·`LoginSessionControllerTest`, `verify.cmd` 105/105 |
| 권한관리 (`PermissionService`/`PermissionMapper`/`Permission_SQL.xml`, `MenuPermissionInterceptor`, `MenuInterceptor`, `RoleManageController`, `SitemapController`, `V5__permission.sql`) | `auth/roleList.html`, `auth/roleWrite.html`, `comm/sitemap.html`, 모든 내부 화면의 사이드바 | 메뉴가 있는 모든 관리자 Controller, 로그인 세션 전체, SSE 구독, 사이트맵 | 역할별 READ/WRITE, 다중 역할 OR, `ADMIN_YN` 슈퍼관리자 우회, `/system`·`/auth`는 관리자 전용, 사이트맵은 `MenuInterceptor` 필터 재사용 | `APP_ROLE`·`APP_USER_ROLE`·`APP_ROLE_MENU`·`menu` 1행 신규, 사용자 삭제 시 역할 매핑 정리 | `PermissionMapperTest`·`RoleManageControllerTest`·`MenuPermissionControllerTest`·`SitemapControllerTest`, `verify.cmd` 105/105 |
| SSE 역할 필터 (`BoardController`, `BoardNotificationEvent`/`BoardNotificationEventListener`, `BoardSseService`, `PermissionService`) | `layout.html` SSE 구독 | 새 글 등록 알림, 게시판 목록 화면 | 대상 메뉴 조회 역할에만 발송, 대상 역할 없으면 미발송 | DB 변경 없음(역할 조회만) | `PermissionMapperTest`(역할 조회), `BoardNotificationEventListenerTest`(9), `BoardNotificationControllerTest`(4), 전체 `verify.cmd` 105/105 |
| 메일 발송 (`MailService`/`MailAsyncService`/`MailServiceImpl`, `EgovConfigAsync`, `LoginController`/`OauthController`/`UserManageController`/`BoardController`, `BoardNotificationEvent`/`BoardNotificationEventListener`, `V6__mail_notification.sql`) | 회원가입·OAuth 가입·관리자 사용자 등록, 비밀번호 찾기, CK 공지/TOAST 중요글 저장·전환 | 로그인·회원가입 화면, 게시판 저장, 비밀번호 찾기, 운영 SMTP | 수신 동의 사용자에게만 환영·게시글 알림, 재설정은 거래성, 게시글당 1회 선점, 기본 비활성 | `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`·`MAIL_SENT_YN`·`MAIL_SENT_DT` 추가, SMTP 외부 연계 | `MailServiceImplTest`(10), `PasswordResetControllerTest`(5), `BoardNotificationEventListenerTest`(9), `BoardNotificationControllerTest`(4), JaCoCo `MailServiceImpl`·`BoardNotificationEventListener` 게이트, `verify.cmd` 105/105. 실제 SMTP E2E는 미수행 |
| 비밀번호 재설정 (`LoginController`, `AuthServiceImpl`, `AuthMapper`/`Auth_SQL.xml`, `PASSWORD_RESET_TOKEN`) | `login/findPw.html`, `login/resetPw.html` | 로그인·계정 찾기, 메일 링크, 세션 레지스트리 | 아이디·이름·이메일 일치 시에만 발급, 계정 존재 여부 숨김, 30분·1회·60초 쿨다운, 성공 시 세션 만료 | `PASSWORD_RESET_TOKEN` 신규, `APP_USER` 비밀번호·잠금·실패 횟수 갱신 | `PasswordResetControllerTest`(발급·소비·만료·검증·쿨다운), `verify.cmd` 105/105. IP별 제한·감사 로그는 미구현 |
| 통합검색 1단계 (`SearchController`/`SearchService`/`SearchServiceImpl`/`SearchMapper`/`Search_SQL.xml`, `search/searchList.html`, `user/layout.html`) | 사용자 포털 GNB의 통합검색 링크, 직접 `/search` 요청 | BOARD를 조회하는 게시판·FAQ·Q&A 콘텐츠, 외부 사용자 상세 화면 | 로그인 사용자만 접근, 메뉴 CRUD 권한과 분리, 제목 LIKE 검색, Q&A는 현재 공개 범위 그대로 노출 | DB 변경 없음(BOARD 조회만), 본문 HTML은 요약 시 제거, 상세 URL은 `/user/**` 사용 | `SearchMapperTest`(3), `SearchServiceImplTest`(3), `SearchControllerTest`(4), JaCoCo `SearchServiceImpl` 게이트, `verify.cmd` 105/105. 본문 검색·초성·ES·Q&A 공개 범위는 미적용 |
| 공통 오류·헬스 (`EgovConfigWeb`, `application.properties`) | 모든 요청의 예외 해석, `/actuator/health` | 미매핑 URL·정적 리소스, 로그인 화면·운영 모니터링 | 미매핑 경로는 404, 그 외 `Throwable`은 500, 메일 비활성 시 메일 헬스 제외 | DB 변경 없음, `management.health.mail.enabled=${MAIL_HEALTH_ENABLED:false}` | `ErrorResponseTest`(3), `verify.cmd` 105/105, 실행 앱에서 health 200·미매핑 URL 404 확인 |
