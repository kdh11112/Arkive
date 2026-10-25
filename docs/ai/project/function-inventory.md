# 기능 목록 및 소유권 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. 기능 단위로 1행을 만든다. 화면·Controller·Service·Mapper·외부연계를 추적해 적는다.
2. 책임자/소유권은 담당자 확인 있을 때만 적고, 없으면 미확인으로 둔다.
3. 이식용으로 가져갈 기능은 복사 대상(이식 시 복사 범위) 칸에 함께 적는다.
4. DB 종속 문법(HSQL 전용 함수 등)은 비고에 표시한다.

## 양식 (Arkive 기록 참고)

| 기능/영역 | 구현 위치 (화면·Controller·Service·Mapper) | 범위 | 책임자/소유권 | 이식 시 복사 대상 |
| --- | --- | --- | --- | --- |
| 예: 메뉴 관리 | `biz.menu.*`, `Menu_SQL.xml` | 목록·등록·수정·삭제 | 미확인 | 화면+Controller+Service+Mapper+SQL+V2 |

<!--
문서 상태: 부분 미완성
미완성 항목: 전체 업무 기능 목록, 기능별 책임 팀/담당자, 기능별 지원 범위와 사용 여부.
필요한 근거/확인자: 업무 담당 조직, 운영 담당자, 사용자/화면 목록, 배포 대상 기능 목록.
완성 방법: 기능별 화면·API·Service·Mapper·외부 연계를 조사하고 담당 조직이 소유권을 확인한 뒤 각 행을 추가·검토한다.
완료 기준: 운영 대상 기능이 빠짐없이 분류되고 모든 기능 소유자가 확인되었거나, 소유권 미정 항목에 의사결정 담당과 기한이 기록되어 있다.
-->

## Arkive 작성 예시

> 2026-10-11, 2026-10-22, 2026-10-23(메일·비밀번호 재설정·사이트맵·통합검색 포함), 2026-10-24(공통 오류 응답·헬스 설정·휴가관리·2단계 보강) 저장소 검색으로 확인한 주요 코드 영역의 목록이다. 전체 기능 목록이나 조직별 소유권을 확정한 문서가 아니다. 소유자는 근거가 없어 미확인으로 둔다.

| 기능/영역 | 확인된 구현 위치 | 확인된 범위 | 책임자/소유권 |
| --- | --- | --- | --- |
| 시스템 메뉴·공통코드 관리 | `arkive.admin.system.*`, `System_SQL.xml`, `CommCode_SQL.xml`, `templates/thymeleaf/system/` | 메뉴·코드 목록·상세·등록·수정·삭제 (`/system` prefix) | 미확인 |
| 로그인·회원·계정 찾기 | `arkive.admin.auth.web.LoginController`, `Auth_SQL.xml`(APP_USER), `templates/thymeleaf/login/` | 로그인·회원가입·ID/PW찾기·로그아웃 (`/login`) | 미확인 |
| 메일 발송 | `arkive.admin.mail.*`, `MailServiceImpl`, `MailAsyncService`, `EgovConfigAsync`, `BoardNotificationEvent`/`BoardNotificationEventListener`, `V6__mail_notification.sql`, `application.properties`(`spring.mail.*`·`arkive.mail.*`) | 환영·비밀번호 재설정·공지/중요글 알림, 게시판 알림은 ApplicationEvent로 채널 분리, `@Async("mailTaskExecutor")`, 기본 비활성, 수신 동의 필터, 게시글당 1회 선점 | 미확인 (SMTP 제공자·발신 도메인·대량 발송 정책) |
| 비밀번호 재설정 | `LoginController`(`/login/findPw*`, `/login/resetPw*`), `AuthServiceImpl`, `AuthMapper`, `Auth_SQL.xml`, `PASSWORD_RESET_TOKEN`, `login/findPw.html`, `login/resetPw.html` | 아이디·이름·이메일 일치 시 1회용 링크 발급, 30분·1회·60초 쿨다운, SHA-256 해시 저장, 성공 시 세션 만료 | 미확인 (IP별 제한·감사 로그·메일 전달 정책) |
| 중복 로그인 방지 | `LoginController`, `OauthController`, `LoginSessionRegistry`, `EgovConfigWeb` | 폼·OAuth 로그인 성공 시 사용자별 최신 세션만 유지, 이전 세션 만료, 로그아웃·세션 만료 시 매핑 제거 | 미확인 |
| 사용자관리 | `arkive.admin.auth.web.UserManageController`, `templates/thymeleaf/auth/` | 목록·등록·수정·삭제·잠금해제·비번초기화 (`/auth`). 메뉴 `ME02010400` | 미확인 |
| 권한관리 | `arkive.admin.auth.web.RoleManageController`, `PermissionService`/`PermissionServiceImpl`, `PermissionMapper`, `Permission_SQL.xml`, `MenuPermissionInterceptor`, `auth/roleList.html`, `auth/roleWrite.html`, `V5__permission.sql` | 역할 목록·등록·수정·삭제, 메뉴별 조회/쓰기, 사용자 역할 배정 (`/auth/role*`). 서버 인터셉터로 메뉴 CRUD 강제, SSE 대상 역할 조회 | 미확인 |
| 사이트맵 | `arkive.admin.comm.web.SitemapController`, `templates/thymeleaf/comm/sitemap.html`, `layout/layout.html`, `EgovConfigWeb.MenuInterceptor` | `/sitemap` 읽기 전용 메뉴 트리, 로그인 사용자의 조회 권한 메뉴만 표시, 빈 메뉴 안내 | 미확인 (메뉴 순서·표시명 소유자) |
| 통합검색 | `arkive.admin.search.web.SearchController`·`SearchReindexController`, `SearchService`/`SearchServiceImpl`, `SearchMapper`, `Search_SQL.xml`, `SearchIndexService`/`ElasticsearchSearchIndexService`, `search/searchList.html`, `user/layout.html`, `V11__search_chosung.sql` | `/search` 로그인 사용자 대상 제목 LIKE + `SEARCH_CHOSUNG` 초성 검색. BOARD의 CK·TOAST·FAQ·QNA를 한 목록에 표시하고 본문은 태그 제거 요약으로 노출. Elasticsearch는 기본 비활성(`arkive.search.elasticsearch.enabled=false`)이며 켜면 색인 조회·재색인(`/search/reindex.json`) 사용 | 미확인 (본문 검색, ES 동기화·운영 정책, Q&A 공개 범위, 검색 성능 기준) |
| Q&A 내·외부 구분 | `arkive.admin.qna.*`, `arkive.user.qna.*`, `Qna_SQL.xml`, `V7__stage2_qna_audit.sql` | `BOARD.ASK_TYPE`(PUBLIC/INTERNAL). 공개 목록은 PUBLIC만, INTERNAL 상세는 비로그인 시 로그인 화면으로 유도, 외부 등록은 PUBLIC 강제, 답변은 관리자 세션만 (`/qna`, `/user/qna`) | 미확인 (내부글 사용자별 세부 권한, 역할 기반 답변) |
| 로그관리·감사 | `arkive.admin.audit.*`, `AuditInterceptor`, `AuditEvent`, `AuditLogEventListener`, `AuditLogController`, `V7__stage2_qna_audit.sql` | 로그인·로그아웃·메뉴·개인정보 조회/다운로드·API 접근을 `AUDIT_LOG`에 비동기 기록. `/system/auditLogList` 조회·엑셀, 보관주기 삭제 잡(기본 비활성) | 미확인 (이벤트·대상 경로 범위, 보관주기 정책, 다중 인스턴스 중복 기록) |
| 개인정보 보호 | `MaskingInterceptor`, `MaskingUtil`, `PiiFieldService`, `FieldCryptoService`, `AddressServiceImpl`/`CardServiceImpl`/`AuthServiceImpl`, `SmsServiceImpl`, `V15__field_crypto.sql`, `V34__user_phone_crypto.sql` | 목록 `EgovMap` 마스킹, PII 조회·다운로드 감사 연계, 주소록·명함 연락처/이메일과 사용자 휴대전화 AES-GCM 저장·복호화. `APP_USER.EMAIL`은 조회 조건이라 평문 | 미확인 (상세·엑셀 마스킹 누락, 키 관리·회전, 기존 평문 데이터 이관) |
| 2차인증 (TOTP) | `LoginController`의 `/login/totp*`, `TotpService`/`TotpServiceImpl`, `TotpMapper`, `V9__totp.sql`, `UserManageController.resetTotp` | 로그인 후 TOTP 코드·복구코드 검증, 설정·해제, 복구코드 1회 발급, 관리자 초기화. 비밀키 AES-GCM, 복구코드 SHA-256 | 미확인 (복구·본인확인 정책, 5회 실패 잠금 외 정책, 다중 인스턴스 키) |
| 약관동의 | `TermsController`, `TermsService`/`TermsServiceImpl`, `TermsMapper`, `TermsInterceptor`, `V10__terms.sql`, `login/termsAgree.html`, `terms/termsList.html` | 버전별 약관·동의 이력(`TERMS`, `USER_TERMS_CONSENT`), 공개 `/terms`, 로그인 후 미동의 필수 약관 인터셉트, 화면은 409 JSON·리다이렉트 | 미확인 (실제 약관 문구·법무 검토, 선택 동의 철회 정책) |
| SMS·알림 채널 | `arkive.admin.sms.*`, `AligoSmsSender`, `SmsServiceImpl`, `SmsAsyncService`, `BoardNotificationEvent`/`BoardNotificationEventListener`, `V17__sms.sql`, `Permission_SQL.xml`의 SMS 대상 쿼리 | 게시글 알림을 SSE·메일·SMS로 채널 분리. SMS는 `SMS_YN='Y'`·휴대전화·메뉴 조회 권한 대상, `BOARD.SMS_SENT_YN` 선점으로 1회 발송, 기본 비활성 | 미확인 (알리고 계약·발신번호·요금, 실패 재시도, 인증번호 발송) |
| 통계·외부 웹분석 | `StatsController`/`StatsService`/`StatsMapper`, `Stats_SQL.xml`, `AnalyticsModelAdvice`, `arkive-analytics.js`, `stats/statsList.html` | `/stats` 기간별 게시판·감사 이벤트·일별 로그인/접속·다운로드 집계와 엑셀. GA4·Clarity는 동의 후 로드, ID 미설정 시 비활성 | 미확인 (실측 ID·도메인, 지표 정의, 감사 로그 이전 구간) |
| 내부 API 제공 | `InternalApiController`, `InternalApiAuthFilter`, `InternalApiConfig`, `/internal/v1` | `X-Internal-Token` 상수시간 비교, 미설정 503·불일치 401. `/health`, `/notices`(CK 최신 10건, 개인정보 없음) | 미확인 (토큰 발급·회전, 소비자 계약, 버전·하위호환 정책) |
| 소셜 로그인 연계 | `arkive.admin.auth.web.OauthController`, `OAUTH_LINK` | 제공자 연결·콜백·연계 (`/login/oauth`) | 미확인 |
| NICE 본인 확인 | `NiceCheckController`(`/niceExt`), `NiceVerifyController`(`/login/nice`) | 연계·시작·콜백·상태 | 미확인 (외부 계약은 연계 명세 확인) |
| 공통 파일 처리 | `arkive.admin.comm.web.FileController`, `FileService*`, `File_SQL.xml` | 멀티파트·TUS·드롭존 업로드, 다운로드, 삭제 (`/system`, `/file`) | 미확인 |
| 게시판 (관리자) | `arkive.admin.board.*`, `Board_SQL.xml` | CK·토스트 목록·쓰기·상세·등록·수정·삭제·구독 (`/board`) | 미확인 |
| 팝업·배너·카테고리·일정·POLL·상담·명함·주소록·당직·메모·FAQ·Q&A (관리자) | `arkive.admin.popup/banner/category/schedule/poll/counsel/card/address/duty/memo/faq/qna.*` | 각 `/popup`·`/banner`·`/category`·`/schedule`·`/poll`·`/counsel`·`/card`·`/address`·`/duty`·`/faq`·`/memo`·`/qna` prefix | 미확인 |
| 외부포털 (사용자) | `arkive.user.board/faq/qna/poll/counsel/main.*` | 공지·자료·FAQ·Q&A·POLL·상담·메인 (`/user/**`) | 미확인 |
| 샘플 기능 | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml` | eGovFrame 샘플 CRUD 및 페이징 코드가 있음 | 샘플 코드, 업무 소유권 해당 없음 |
| 시스템 설정·예약 DB 작업 | `egovframework.example.config.*`, `arkive.com.config.*` | DB·Mapper 설정, 10분 Trigger와 덤프 작업 코드가 있음 | 운영 책임자 미확인 |
| 공통 오류·헬스 | `egovframework.example.config.EgovConfigWeb`, `application.properties` | 미매핑 URL·정적 리소스 404, `Throwable` 500, Actuator health, 메일 헬스 기본 비활성 | 미확인 |
| 게시글 댓글 | `arkive.admin.board.web.BoardController`(댓글 API), `BoardService`/`BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `templates/thymeleaf/board/boardDetail.html` | 로그인 사용자 1단 댓글 목록·등록·삭제, 작성자/관리자 삭제 (`/board/insertComment.json`, `/board/deleteComment.json`) | 미확인 |
| 공지 고정 | `arkive.admin.board.web.BoardController`, `BoardService`/`BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `V4__board_notice.sql`, `board/boardList.html`, `board/boardDetail.html`, `user/board/noticeList.html`, `user/board/noticeDetail.html` | CK 공지 목록 상단 정렬, 관리자 고정/해제 (`/board/updateNotice.json`), 관리·사용자 화면 '공지' 배지 | 미확인 |
| 쪽지관리 | `arkive.admin.message.*`, `Message_SQL.xml`, `templates/thymeleaf/message/` | 받은/보낸 목록·검색·페이징·발송·상세·읽음·함별 소프트 삭제 (`/message`) | 미확인 |
| 휴가관리 | `arkive.admin.leave.*`, `Leave_SQL.xml`, `templates/thymeleaf/leave/`, `V8__leave.sql` | 내 휴가(본인 연차·신청·취소), 휴가관리(전체 목록·승인/반려), 연차관리(사용자·연도별 부여). 주말·공휴일(HOLIDAY) 제외 평일 일수, 반차 0.5일, 승인 시 차감·취소 시 환원, 신청중 포함 잔여 초과 금지 (`/leave`) | 미확인 (연차 발생·이월 규칙, 결재선·대결자) |
| 회의실관리·예약 | `arkive.admin.meeting.*`, `Meeting_SQL.xml`, `templates/thymeleaf/meeting/`, `V13__meeting.sql` | 회의실 목록·등록·수정·삭제, 예약 목록·등록·취소. 같은 회의실·날짜 시간대 중복 차단, 예약 취소는 본인·관리자만 (`/meeting`) | 미확인 (자정 넘김 예약, 예약 변경 이력) |
| 약식결재 | `arkive.admin.approval.*`, `Approval_SQL.xml`, `templates/thymeleaf/approval/`, `V14__approval.sql` | 기안(DRAFT)→상신(SUBMITTED)→승인(APPROVED)/반려(REJECTED)/상신취소(CANCELLED), 지정 결재자 서버 검증, `APPROVAL_HISTORY` 이력 (`/approval`) | 미확인 (결재선·대결자, 대상 업무 연계, 공통코드 COM75 매핑) |
| HR 세트 | `arkive.admin.hr.*`, `Hr_SQL.xml`, `templates/thymeleaf/hr/`, `V18__hr.sql` | 기념일·경조사·포상 CRUD, 개인연차 화면에서 LEAVE_BALANCE·LEAVE_REQUEST·DUTY 통합 조회·연차 저장 (`/hr`) | 미확인 (유형 코드 샘플 상수, 휴가·당직·결재 연계) |
| 설문관리 세트 | `arkive.admin.survey.*`, `Survey_SQL.xml`, `templates/thymeleaf/survey/`, `V16__survey.sql` | 템플릿·설문·질문·항목·참여·통계 6종. 단일/복수/주관식, 익명 여부, 공개·마감, 문항별 통계 (`/survey`) | 미확인 (설문 정책·익명 통계 기준, 대량 응답 성능) |
| 강의형 콘텐츠·동영상 | `arkive.admin.lecture.*`, `Lecture_SQL.xml`, `VideoEncodingService`/`FfmpegVideoEncodingService`, `templates/thymeleaf/lecture/`, `V30__lecture.sql` | 강의 CRUD·공개/마감, 동영상 업로드(FFmpeg 선택), Range 스트리밍, 진도 저장·이어보기, 시청 기준 + 설문 참여 수료 판정, 수료현황 (`/lecture`) | 미확인 (FFmpeg 실제 인코딩, 출석·과제·시험·성적, 비로그인 수강자 식별) |
| 약도관리 | `arkive.admin.map.*`, `MapGuide_SQL.xml`, `templates/thymeleaf/map/`, `V31__map_guide.sql` | 약도 목록·등록·수정·삭제, 주소·위도·경도·설명, 카카오 지도 SDK는 키가 있을 때만 로드 (`/map`) | 미확인 (카카오 키·허용 도메인, 주소 지오코딩 계약) |
| SSO 연계 | `arkive.admin.sso.web.SsoController`, `OAUTH_LINK`, `templates/thymeleaf/sso/`, `application.properties`(`arkive.sso.*`) | OAuth2 authorization code, state 검증, 토큰 교환·사용자정보 조회, `OAUTH_LINK.PROVIDER='sso'` 사전 연계 계정 로그인, TOTP, 선택적 자동 가입 (`/sso`) | 미확인 (외부 SSO 서버 URL·필드·글로벌 로그아웃 계약, 자동 가입 정책) |
| WebSocket 채팅 | `arkive.admin.chat.*`, `Chat_SQL.xml`, `ChatWebSocketConfig`, `ChatHandshakeInterceptor`, `ChatWebSocketHandler`, `templates/thymeleaf/chat/chat.html`, `V32__chat.sql` | `/ws/chat` 세션 인증 + 채팅 메뉴 WRITE 검사, Origin 허용목록, DB 최근 100건 이력, 메시지 저장·브로드캐스트 (`/chat/chat`) | 미확인 (다중 인스턴스 브로커, 메시지 보존·신고·차단 정책, 대용량 동시 접속) |
| Spring Batch 일일 집계 | `arkive.admin.batch.*`, `Batch_SQL.xml`, `BatchConfig`, `DailyStatsTasklet`, `DailyStatsAggregator`, `templates/thymeleaf/batch/batchList.html`, `V33__batch.sql` | `dailyStatsJob`/`dailyStatsStep` 수동 실행, `STAT_DAILY`에 게시판 건수·로그인·접속·강의 수료·설문 참여 집계, 같은 날짜 재집계 멱등 (`/batch`) | 미확인 (자동 스케줄링, 실행 이력·재시작, 대량 발송·분산 배치) |

## 테스트 자산 현황 (2026-10-25)

전체 314개 테스트, instruction 51% / branch 43% / line 52% (2026-10-25 `verify.cmd`와 JaCoCo report 기준). 숫자만으로 품질을 판단하지 않고 아래처럼 기능별 검증 범위와 공백을 함께 본다. 2026-10-25 전체 테스트를 실행해 모두 통과했고 클래스별 JaCoCo 게이트도 충족했다. README에서 `[일부검증]`으로 남긴 항목은 외부 계약·실장비·브라우저 저장소에 의존해 자동 테스트로 닫지 못한 기능이다.

| 기능/영역 | 자동 테스트 | 검증 범위 | 남은 공백 |
| --- | --- | --- | --- |
| 컨텍스트·MyBatis | `ArkiveSmokeTest` 1 | 컨텍스트 로딩, SqlSessionFactory 배선 | - |
| 게시판 CRUD·화면 | `BoardMapperTest` 4, `BoardServiceImplTest` 11, `BoardControllerTest` 5, `BoardPagingControllerTest` 2 | insert/select/delete, CLOB 변환, 초성·색인 이벤트, sanitize·이중등록·조회수 1회·에디터 이미지·첨부 삭제, 오프셋/커서 페이징 | HWP 복붙 브라우저 동작, 검색 조건 세부, 실제 화면 확인 |
| 게시글 댓글 API | `BoardCommentControllerTest` 3 | 401, 게시글/내용 검증, 작성자·관리자 삭제 | 수정·신고, 관리자 판정 범위 |
| 공지 고정 | `BoardNoticeMapperTest` 2, `BoardNoticeControllerTest` 4 | 고정 우선 정렬·해제·CK 외 거부, 401·관리자 권한·입력 검증, 목록 템플릿 렌더링 | 페이지 경계, 다건 고정 정렬, 수동 화면 확인 |
| 게시판 알림 이벤트·SSE | `BoardNotificationEventListenerTest` 13, `BoardNotificationControllerTest` 4, `BoardSseServiceTest` 5 | 등록 시 SSE·메일 요청 분리, CK/TOAST 메뉴 역할, 중요글 전환 시에만 메일, 채널별 실패 격리, SSE 게시판 유형·ALL 채널 전달·역할 필터 | 실제 브라우저 EventSource 구독 E2E, 실제 SMTP·SMS E2E, 이벤트 비동기 전환 |
| 파일 업로드·TUS·다운로드 | `FileControllerTest` 7 | temp→확정(물리 이동·DB 기록), 다운로드·삭제, TUS 생성·HEAD·409 재개·PATCH·삭제, CORS 프리플라이트, ZIP 중복 이름, 경로 이탈 차단, 무세션 401 | 실제 브라우저 드롭존, 대용량·장시간 중단 재개, 운영 저장소 권한 |
| Swagger | `OpenApiDocsTest` 2 | `/v3/api-docs` 제목·버전·주요 경로 노출, `/swagger-ui/index.html` 응답 | 운영 공개 범위·내부망 제한 |
| 쪽지 | `MessageMapperTest` 2, `MessageControllerTest` 4 | 받은/보낸함 분리, 읽음, 소프트 삭제, 검색, 401, 중복제출, 함별 권한 | 보존·파기 정책, 수신자 삭제 후 발신자 표시, 메일·알림 연계 |
| 휴가관리 | `LeaveDaysCalculatorTest` 5, `LeaveControllerTest` 7 | 주말·공휴일 제외, 반차 0.5일, 기간·유형 검증, 화면 3종 렌더, 토큰 소모·승인 전 미차감, 승인 차감·취소 환원, 반려 사유, 중복 기간·잔여 초과·연차 하향 거부 | GUI 확인 미완료(실제 브라우저 JS), 연차 발생·이월 규칙, 결재선·대결자, 동시 승인 경합 |
| 회의실관리·예약 (2026-10-24 추가) | `MeetingControllerTest` 5 | 관리자 화면·일반 사용자 예약 전용, 회의실 CRUD, 예약 있는 회의실 삭제 거부, 시간대 중복·형식 오류 거부, 본인·관리자 취소 | **2026-10-24 실행 통과**. 자정 넘김, 예약 변경·이력, 동시 예약 경합 |
| 약식결재 (2026-10-24 추가) | `ApprovalControllerTest` 5 | 기안·상신·승인·반려·상신취소 권한과 상태 전이 | **2026-10-24 실행 통과**. 결재선·대결자, 동시 처리 경합, 이력 정렬 |
| HR 세트 (2026-10-24 추가) | `HrControllerTest` 4 | 기념일·경조사·포상 CRUD와 개인연차 화면 | **2026-10-24 실행 통과**. 유형 코드, 개인정보 노출 범위, 휴가·당직 연계 |
| 설문관리 세트 (2026-10-24 추가) | `SurveyControllerTest` 5 | 템플릿·설문·질문·참여·통계 화면/API 기본 흐름 | **2026-10-24 실행 통과**. 익명 통계, 복수선택·주관식 검증, 대량 응답 |
| 강의·동영상 | `LectureControllerTest` 2, `FfmpegVideoEncodingServiceTest` 7 | 최대 시청 위치·수료 판정, 상태 전환·스트리밍, 비활성 모드 저장·확장자/길이/크기/경로 이탈 거부·삭제 | FFmpeg 실제 인코딩(이 환경에 미설치), 브라우저 재생, Range 416·suffix 경계 |
| 약도관리 | `MapControllerTest` 2 | 약도 CRUD·사용여부 필터·제목/좌표 검증·이중등록 차단 | 카카오 SDK 로드·도메인 제한, 지오코딩 |
| SSO 연계 | `SsoControllerTest` 1 | 비활성 시 `/sso/login`·`/sso/callback` 404 | 외부 SSO 계약·state/토큰 오류·자동 가입 정책 |
| WebSocket 채팅 | `ChatWebSocketTest` 6 | 세션·채팅 WRITE 핸드셰이크, Origin 허용목록, 최근 이력, 세션 신원 저장·브로드캐스트, 공백/길이 차단, 저장 실패 응답 | 다중 인스턴스 브로커, 재연결, 실제 브라우저 E2E |
| Spring Batch 일일 집계 | `BatchControllerTest` 2 | 일일 집계, 같은 날짜 재집계 멱등, 날짜·토큰 검증 | 자동 스케줄링, 실행 이력·재시작, ResourcelessJobRepository 동시성 |
| HTML sanitize | `HtmlSanitizerTest` 6 | script·이벤트·iframe·표·style | 실제 에디터 출력 회귀 |
| 파일 실형식 | `FileTypeUtilTest` 5 | 매직바이트, 확장자 위조, 허용 목록 | 풀파서 미사용(경량 검사), 다운로드 권한은 `FileControllerTest`에서 확인 |
| JWT | `JwtUtilTest` 4, `ApiControllerTest` 2 | 발급·검증, 시크릿 불일치, 만료·형식 오류, 시크릿 미설정 임시 키, `/api/**` 토큰 검증 | 키 회전·로그아웃 블랙리스트 |
| 로그인·세션·중복로그인 | `AuthInterceptorTest` 5, `LoginSessionRegistryTest` 5, `LoginSessionControllerTest` 1 | 공개 경로, JSON 401, 화면 redirect, 관리자 경로, 사용자별 세션 교체·만료, 동일 세션 재등록, 실제 로그인 2회 | 다중 인스턴스·실제 컨테이너 세션 타임아웃, CSRF |
| 공통 오류·헬스 | `ErrorResponseTest` 3 | 미매핑 URL·정적 리소스 404, 메일 비활성 시 health UP | `.json` 미매핑 경로의 JSON 오류 규약, 실제 브라우저 오류 화면 |
| 회원가입·아이디/비밀번호 찾기 | `SignupControllerTest` 5, `PasswordResetControllerTest` 5 | 화면 렌더·필수값, 아이디 마스킹, 본인 확인 후에만 토큰, 1회 사용·만료·60초 쿨다운, 비밀번호 변경·잠금 초기화 | IP별 요청 제한, 실제 메일 링크 E2E, 운영 SMTP |
| 메일 발송 | `MailServiceImplTest` 10 | 기본 비활성·발신자 없음, 수신 동의 필터, 환영/재설정/공지 메일, HTML escape, 게시글당 1회 선점, 수신자 없음, SMTP 실패 무시 | 실제 SMTP 발송·반송·수신거부, 메일 템플릿, 실패 재시도, 대량 발송 정책 |
| 권한관리 | `PermissionMapperTest` 4, `RoleManageControllerTest` 7, `MenuPermissionControllerTest` 5 | V5 시드·기본 사용자 역할의 시스템·업로드 메뉴 제외·메뉴 권한 OR 집계·SSE 역할 조회, 역할 CRUD/저장, 잘못된 사용자 배정 거부, 사용자 삭제 시 역할 매핑 정리, 조회 전용·쓰기·역할 없음·관리자 우회·사이드바 필터 | 화면 수동 확인, 시스템관리 메뉴의 관리자 전용 정책, 다중 역할 조합, 역할 변경 즉시 반영(세션 캐시 없음), 새 Controller는 `MenuPermissionInterceptor` 매핑 필수(누락 시 검사 제외), `/user`·`/api` 공개 경로는 기존 정책 유지 |
| 사이트맵 | `SitemapControllerTest` 3 | 로그인 필요, 관리자 메뉴 트리 렌더, 역할 없는 사용자의 빈 메뉴 안내 | 메뉴명·순서 변경, 권한별 실제 화면 수동 확인, 외부 사용자 전용 사이트맵 |
| 통합검색 | `SearchMapperTest` 4, `SearchServiceImplTest` 9, `SearchControllerTest` 4, `KoreanSearchUtilTest` 3 | 제목 LIKE의 CK/TOAST/FAQ/QNA 통합, 본문만 일치하는 글 제외, LIMIT/OFFSET 페이징·건수, 초성 색인·검색, ES 색인 조회·실패 시 DB 폴백, 유형 라벨·HTML 제거·CLOB 요약, 로그인 필요·빈 검색어·무결과·결과 렌더 | 실제 ES 운영·동기화, 본문 검색(평문 인덱스 필요), Q&A 공개 범위, 검색 성능·입력 길이 제한 |
| Q&A 내·외부 구분 | `QnaMapperTest` 3, `QnaControllerTest` 3, `UserQnaControllerTest` 6 | 공개 목록의 INTERNAL 제외, 내부글 비로그인 상세 차단, 외부 등록 PUBLIC 강제·sanitize·이중등록, 답변 관리자 제한, CLOB 변환 | 내부글 사용자별 권한, 역할 기반 답변, 목록·상세 화면 수동 확인 |
| FAQ·메모 | `FaqControllerTest` 2, `MemoControllerTest` 2 | FAQ 초성·sanitize 저장 왕복·이중등록, 메모 상태 전이 제한·필수값 | 실제 화면, 첨부 3개 제한 |
| 팝업·배너·메인이미지 | `PopupControllerTest` 2, `BannerControllerTest` 2 | 게시기간·사용여부 필터, 날짜 역전·토큰, 위치 정규화·정렬·MAIN 필터 | 대시보드 진입 시 팝업 연계, 실제 노출 화면 |
| 일정·공휴일 | `ScheduleControllerTest` 3 | 유형·일자 필터, 일시 검증·유형 정규화, 공휴일 병합·삭제 | 월/주/일 화면 JS, 부서·사용자 팝업 |
| 주소록·명함 | `AddressControllerTest` 2, `CardControllerTest` 2 | 연락처 암호화 저장·목록 마스킹·상세 평문, 공개/내 명함 분리·사용등록 | 실제 화면, 부서일정 팝업 연결 |
| 상담·POLL | `CounselControllerTest` 2, `PollControllerTest` 2 | 상담 비밀번호 확인·답변·첨부 검증, POLL 세션당 1회·항목/기간 검증 | 실제 화면 |
| 당직·엑셀 | `DutyControllerTest` 3 | 당직 CRUD, 엑셀 양식/업로드 오류·다운로드 마스킹, 이중등록·일자 검증 | 대용량(1000행) 메모리, 양식 버전 고정, Oracle 함수 이식 |
| 공통코드 | `CommCodeControllerTest` 2 | 마스터·상세 관리, 하위 코드 연쇄 삭제, 필수값 검증 | 실제 화면 |
| 사용자 관리 | `UserManageControllerTest` 4 | 관리자 세션 필수, 등록 시 기본 역할·검증, 수정·삭제, 잠금 해제·임시 비밀번호 | 변경 이력 |
| 로그관리·감사 | `AuditLogServiceTest` 2, `AuditLogControllerTest` 2, `AuditInterceptorIntegrationTest` 4 | 감사 로그 기록·조회·필터·엑셀, 메뉴 접근·PII 조회/다운로드 인터셉터→이벤트→DB 적재 | 인터셉터 대상 경로 전수, 비동기 유실, 보관주기 삭제, 다중 인스턴스 중복 |
| 개인정보 보호 | `MaskingUtilTest` 4, `FieldCryptoServiceTest` 6, `PiiFieldServiceTest` 6, `AddressControllerTest` 2, `CardControllerTest` 2, `UserManageControllerTest` 4 | 이름·이메일·전화·계좌·생년월일·주소 마스킹, AES-GCM 암복호화·키 미설정 거부·이중 암호화 방지, 주소록·명함·사용자 저장값 암호화와 상세 평문 복원 | 모든 상세·엑셀·수동 화면의 마스킹 누락 지점, 기존 평문 데이터 이관 |
| 2차인증 (TOTP) | `TotpServiceTest` 4, `LoginTotpFlowTest` 1 | 설정·복구코드 1회 사용·초기화·잘못된 코드 거부, 로그인 후 TOTP 요구·5회 실패 잠금 | 관리자 초기화 화면, 키 미설정 시 화면 안내 |
| 약관동의 | `TermsServiceTest` 4, `TermsInterceptorTest` 4 | 현재 약관·필수 동의 판정·동의 저장, 화면 409·리다이렉트 | 실제 문구·버전 추가, 선택 동의 철회, 로그인 전 동의 E2E |
| SMS·통계·내부 API·웹분석 | `AligoSmsSenderTest` 7, `SmsServiceImplTest` 5, `StatsControllerTest` 2, `InternalApiControllerTest` 3, `AnalyticsModelAdviceTest` 5 | SMS 비활성·자격증명 미설정·LMS 전환·실패 응답·수신 대상·중복 발송 차단, 통계 집계·기간 보정·엑셀, 내부 API 토큰 401·health UP·개인정보 미포함, 웹분석 ID 검증·동의 정책 | 실제 알리고 계약·발신번호, 실제 GA4/Clarity 측정 ID·도메인, 실패 재시도 |
| OAuth·NICE | `OauthControllerTest` 3, `NiceVerifyControllerTest` 2 | 미지원·미설정 provider와 잘못된 state 거부, 미계약 시 5xx | 실제 외부 계약·콜백 URL, 화면 로그인 연결 |

기능을 추가하거나 이식할 때는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)에 실행·복사·검증 결과를 남기고, 이 표의 테스트 자산도 함께 갱신한다.
테스트를 다른 프로젝트로 옮길 때는 [test-porting-sheet.md](../portable/test-porting-sheet.md)에 복사·수정·검증 결과를 남긴다.

## 유지 기준

- 기능을 추가·변경할 때 실제 화면, Controller, Service, Mapper/SQL, 외부 연계와 호출처를 확인해 행을 갱신한다.
- Arkive는 타 프로젝트로의 기능 이식을 전제로 한다. 기능을 추가할 때는 이식에 필요한 의존성(pom.xml), 설정(Globals 키·application.properties), DB 객체(db/versions), 외부 연계 JAR/키를 함께 기록한다.
- DB 제품이 달라질 수 있으므로(Oracle/PostgreSQL 등) DB 종속 문법과 HSQL 전용 함수는 비고에 표시한다.
- `@RequestMapping` 등 코드상 경로가 있다는 사실과 사용 중인 업무 기능이라는 사실을 구분한다.
- 책임 조직·담당자는 업무 담당자나 공식 문서 근거가 있을 때만 기록한다.
