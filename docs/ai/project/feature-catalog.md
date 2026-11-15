# 기능 카탈로그 및 API 명세 (Arkive 기록)

> **Arkive 기록 · 대상에 복사 금지.** 대상 지식은 [빈 양식](../portable/project-template-pack.md)을 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 각 기능의 실제 브라우저(ego-browser) GUI 전수 확인, 실제 외부 연동(SMS/SMTP/OAuth/SSO/NICE)과 운영 정책 검증.
필요한 근거/확인자: ego-browser 실행 증거, 기능·외부 연계·운영 담당자 확인, 정식 API 계약 명세.
완성 방법: 자동 테스트와 해당 기능의 GUI·연동 결과를 구분해 기록하고, 신규/수정 기능의 경로·파라미터를 대조한다.
완료 기준: 대상 기능의 검증 범위·코드 위치·API 계약·남은 위험이 명시되고, 미수행 항목이 실제 확인 결과로 갱신되어 있다.
-->

기존 `feature-status.md`, `function-inventory.md`, `api-contract.md`의 내용을 통합한 기능별 단일 참조 카탈로그다.
기능 구현 여부의 1차 기준은 [README.md](../../../README.md)를 따르며, 최신 자동 테스트 실행 수치는 [테스트 자산](../../../src/test/README.md), 실제 GUI/전환 검증 이력은 [검증 및 변경 로그](verification-log.md)에서 확인한다.

---

## 1. 기능 구현 현황 및 주의점

### 1.1. 구현 및 검증완료 (자동 테스트 + GUI 확인)

- [x] **게시판 조회수 통계**
  - **위치:** `arkive.admin.board.web.BoardController` (`/board/viewStats`), `BoardMapper`, `Board_SQL.xml`, `templates/thymeleaf/board/viewStats.html`, `db/schema.sql` (`BOARD_VIEW_LOG`)
  - **동작 및 주의점:** 24시간 집계 창을 통과할 때만 로그 1건 적재. 일별 추이는 `CAST(VIEW_DT AS DATE)` 그룹핑, 빈 날짜는 0, 막대는 최대값 대비 % 표시. 순위의 `BOARD.VIEW_CNT`에는 로그 도입 전 누적값도 포함. 640px 이하 모바일 폭 대응 적용(격일 라벨 표시, 오늘 항상 표시). 누적 순위 표는 5열로 좁은 화면에서 가로 스크롤 발생. 조회 로그 보관·정리 정책 미확인.
  - **테스트:** `BoardViewStatsControllerTest` (3)
- [x] **게시판 검색 범위 분리 (제목/내용/제목+내용)**
  - **위치:** `Board_SQL.xml` (`selectBoardList`, `selectBoardListCount`, `selectToastBoardCursorList`)
  - **동작 및 주의점:** `searchCondition` 분기(`title`/`content`/`all`) 적용, 미지원 값은 `title`로 폴백. 본문 검색은 CONTENT 원문 LIKE라 `table`·`style` 태그 문자열도 검색될 수 있음(평문 컬럼 또는 DB별 `REGEXP_REPLACE` 필요).
  - **테스트:** `BoardSearchScopeControllerTest` (7)
- [x] **게시판 기본 기능 (공지사항·자료실, CK·TOAST, HWP 복붙, 이미지 업로드, 댓글)**
  - **위치:** `arkive.admin.board.*`, `Board_SQL.xml`, `templates/thymeleaf/board/*`, `db/schema.sql` (`BOARD`, `BOARD_COMMENT`)
  - **동작 및 주의점:** HTML sanitize, 이중등록 방지, 첨부파일 삭제, 오프셋/커서 페이징, CLOB 변환, 초성 색인 이벤트 구현. 조회수는 게시글당 브라우저 24시간 1회 쿠키·세션 복합 집계. 댓글은 1단 평문 저장(대댓글 없음), 작성자 및 관리자만 삭제 가능.
  - **테스트:** `BoardMapperTest` (4), `BoardServiceImplTest` (11), `BoardControllerTest` (5), `BoardCommentControllerTest` (3), `BoardViewCountDailyWindowTest` (6)
- [x] **GUI 추가 확인 (2026-11-13, ego-browser):** 로그인·약관동의·통합검색·공지/게시판관리·FAQ·Q&A·공통코드·Swagger·멀티파트/TUS 업로드·사이트맵·TOTP 화면 렌더링 정상. README의 "구현 및 검증완료" 이동 항목과 동일 범위. 상세는 [검증 및 변경 로그](verification-log.md#25-2026-11-13-gui-추가-확인) 참조.

---

### 1.2. 구현 (자동 테스트 통과 · GUI 확인 대기)

- [x] **페이징 (공지사항: 오프셋, 자료실: 커서+더보기)**
  - **위치:** `BoardController`, `Board_SQL.xml`, `user/board/*`
  - **주의점:** 공지는 eGov `paginationInfo` + KRDS 블록(10건 단위, 검색 조건 유지). 자료실은 `toastList.json` 커서(`BOARD_ID` 기준, `hasNext`가 false면 더보기 숨김). 관리자·사용자 화면 모두 GUI 확인 완료 이력 보존됨.
  - **테스트:** `BoardPagingControllerTest` (2)
- [x] **이전글 / 다음글**
  - **위치:** `Board_SQL.xml` (`selectPrevBoard`, `selectNextBoard`), `boardDetail.html`, `user/board/*`
  - **주의점:** 동일 `BOARD_TYPE` 내 `REGIST_DT`·`BOARD_ID` 기준 1건씩 노출, 대상 없으면 "없습니다" 안내. 관리자 상세 및 사용자 공지/자료실 상세에 적용.
- [x] **공통코드 관리**
  - **위치:** `arkive.admin.system.web.CommCodeController`, `CommCode_SQL.xml`, `templates/thymeleaf/system/`
  - **주의점:** 마스터·상세 코드 관리, 하위 코드 연쇄 삭제, 필수값 검증.
  - **테스트:** `CommCodeControllerTest` (2)
- [x] **파일 업로드 (멀티파트·TUS·드롭존, temp→확정 2단계)**
  - **위치:** `arkive.admin.comm.web.FileController`, `FileService`, `File_SQL.xml`, `templates/thymeleaf/comm/`
  - **주의점:** 임시 업로드 후 글 저장 시 확정(물리 이동 `MP_` 및 DB 기록). TUS 프로토콜(청크 전송·이어올리기·삭제) 지원. OPTIONS CORS 프리플라이트는 무인증 허용, 실제 요청은 세션 필수. 경로 이탈 차단.
  - **테스트:** `FileControllerTest` (7)
- [x] **본문 HTML Sanitize**
  - **위치:** `HtmlSanitizer` (Lucy core 직접 사용, jakarta 미지원 서블릿 필터 대체)
  - **주의점:** `<script>`, 위험 `<iframe>`, 이벤트 핸들러 제거. 표, 이미지, 안전한 스타일 유지.
  - **테스트:** `HtmlSanitizerTest` (6)
- [x] **실시간 알림 SSE**
  - **위치:** `BoardSseService`, `BoardNotificationEventListener`, `layout/layout.html`
  - **주의점:** `ARKIVE_SSE_ENABLED=false` 기본(화면 설정). ALL 단일 연결 및 메뉴 조회 역할 필터링 발송. 단일 서버 메모리 기반.
  - **테스트:** `BoardSseServiceTest` (5), `BoardNotificationEventListenerTest` (13)
- [x] **Swagger (OpenAPI 3.0)**
  - **위치:** `springdoc-openapi`, `/swagger-ui.html`, `/v3/api-docs`
  - **주의점:** 운영 배포 시 민감 API 노출 위험(내부망 제한 또는 비활성화 검토).
  - **테스트:** `OpenApiDocsTest` (2)
- [x] **이중등록 방지 (Double Submit Preventer)**
  - **위치:** Thymeleaf fragment `comm/doubleSubmit`, `DoubleSubmitPreventer`
  - **주의점:** 게시판, FAQ, Q&A, 메모, 팝업, 배너, 일정, 주소록, 명함, POLL, 당직, 지도 전반 적용. 여러 폼 동시 사용 시 tokenKey 분리 필요.
- [x] **FAQ 관리 / Q&A 관리**
  - **위치:** `arkive.admin.faq.*`, `arkive.admin.qna.*`, `arkive.user.qna.*`, `Qna_SQL.xml`
  - **주의점:** FAQ는 `BOARD_TYPE='FAQ'` 아코디언 목록. Q&A는 `BOARD_TYPE='QNA'` + `QNA_ANSWER` 1:1 결합(답변 유무로 상태 판정). `ASK_TYPE`으로 PUBLIC/INTERNAL 구분(외부 작성은 PUBLIC 강제, 답변은 관리자만).
  - **테스트:** `FaqControllerTest` (2), `QnaControllerTest` (3), `UserQnaControllerTest` (6), `QnaMapperTest` (3)
- [x] **메모보고**
  - **위치:** `arkive.admin.memo.*`, `Memo_SQL.xml`, `templates/thymeleaf/memo/`
  - **주의점:** `MEMO_REPORT`. 보고→확인/의견 상태 전이를 SQL 조건으로 강제.
  - **테스트:** `MemoControllerTest` (2)
- [x] **팝업창 / 배너 / 메인이미지 관리**
  - **위치:** `arkive.admin.popup.*`, `arkive.admin.banner.*`, `templates/thymeleaf/popup/`, `banner/`
  - **주의점:** 팝업은 게시기간·사용여부 필터. 배너는 `POSITION`으로 위치 구분하며, 메인이미지는 `POSITION='MAIN'`으로 배너 테이블에 통합 관리.
  - **테스트:** `PopupControllerTest` (2), `BannerControllerTest` (2)
- [x] **공지 고정**
  - **위치:** `BOARD.NOTICE_YN`, `BoardController` (`/board/updateNotice.json`)
  - **주의점:** CK 공지 목록 상단 우선 정렬(`NOTICE_YN DESC, REGIST_DT DESC`). 관리자만 변경 가능. 화면에서 '공지' 배지로 식별.
  - **테스트:** `BoardNoticeMapperTest` (2), `BoardNoticeControllerTest` (4)
- [x] **부서일정 / 개인일정 / 일지 / 메모할일 / 공휴일**
  - **위치:** `arkive.admin.schedule.*`, `Schedule_SQL.xml`, `templates/thymeleaf/schedule/`, `db/schema.sql` (`SCHEDULE`, `HOLIDAY`)
  - **주의점:** `SCHEDULE_TYPE`(`DEPT`/`PERSONAL`/`DAILY`/`TODO`) 단일 테이블 및 공통 쿼리 재사용. 공휴일(`HOLIDAY`)과 연계 렌더링.
  - **테스트:** `ScheduleControllerTest` (3)
- [x] **카테고리 설정 (jstree형 / 컬럼 드릴다운형)**
  - **위치:** `arkive.admin.category.*`, `templates/thymeleaf/category/` (`CATEGORY` 테이블)
  - **주의점:** 자기참조(`UP_CAT_ID`) N단 트리. 동일 테이블을 공유하며 jstree JSON 규격 및 컬럼 드릴다운 화면 2종 제공.
- [x] **주소록 / 명함 관리**
  - **위치:** `arkive.admin.address.*`, `arkive.admin.card.*`, `templates/thymeleaf/address/`, `card/`
  - **주의점:** 연락처·이메일 AES-GCM 암호화 저장, 목록 조회 시 마스킹 처리, 상세 조회 시 복호화. 명함은 공개/내 명함 분리 및 사용 등록 추적.
  - **테스트:** `AddressControllerTest` (2), `CardControllerTest` (2)
- [x] **상담 관리 / 온라인 POLL**
  - **위치:** `arkive.admin.counsel.*`, `arkive.admin.poll.*`, `user/counsel/`, `user/poll/`
  - **주의점:** 상담은 비밀번호 확인 후 답변/상세 열람. POLL은 세션 기반 중복 투표 차단(문항 스키마는 `POLL`/`POLL_ITEM`).
  - **테스트:** `CounselControllerTest` (2), `PollControllerTest` (2)
- [x] **당직 관리 / 엑셀 일괄 등록**
  - **위치:** `arkive.admin.duty.*`, `ExcelUtil`, `templates/thymeleaf/duty/` (`DUTY` 테이블)
  - **주의점:** 당직 CRUD, 엑셀 샘플 다운로드 및 POI 기반 일괄 업로드(1000행 제한 메모리 보호).
  - **테스트:** `DutyControllerTest` (3)
- [x] **파일 미리보기 / 파일 실형식 검증**
  - **위치:** `PreviewController` (`/preview/fileView`), `FileTypeUtil`
  - **주의점:** 이미지·PDF inline 뷰어. 확장자 위조 방지를 위한 매직바이트 검사.
  - **테스트:** `FileTypeUtilTest` (5)
- [x] **단일 로그인 / 세션 레지스트리 / 중복 로그인 방지**
  - **위치:** `LoginController`, `LoginSessionRegistry`, `AuthInterceptor`
  - **주의점:** `ADMIN_YN` 플래그로 대시보드/사용자메인 분기(별도 관리자 로그인 URL 없음). 사용자당 최신 1개 세션만 유지(새 로그인 시 이전 세션 만료).
  - **테스트:** `AuthInterceptorTest` (5), `LoginSessionRegistryTest` (5), `LoginSessionControllerTest` (1)
- [x] **사용자 관리 / 권한 관리 / 사이트맵**
  - **위치:** `UserManageController`, `RoleManageController`, `PermissionService`, `SitemapController`
  - **주의점:** 사용자 등록/수정/삭제/잠금해제. `APP_ROLE`·`APP_USER_ROLE`·`APP_ROLE_MENU` 기반 메뉴별 READ/WRITE 권한 OR 집계 및 서버 인터셉터 강제. `/system`·`/auth`는 슈퍼관리자 전용. 사이트맵은 권한 필터 메뉴 재사용.
  - **테스트:** `UserManageControllerTest` (4), `PermissionMapperTest` (4), `RoleManageControllerTest` (7), `MenuPermissionControllerTest` (5), `SitemapControllerTest` (3)
- [x] **감사 로그 / 개인정보 마스킹 및 암호화**
  - **위치:** `AuditInterceptor`, `AuditEvent`, `AuditLogService`, `MaskingInterceptor`, `FieldCryptoService`, `PiiFieldService`
  - **주의점:** 비동기 감사 로그 적재(`AUDIT_LOG`), 화면 모델 `EgovMap` 자동 마스킹, DB 필드 암호화(연락처/휴대전화 AES-GCM, `ARKIVE_FIELD_KEY` 필수). `APP_USER.EMAIL`은 평문 유지.
  - **테스트:** `AuditInterceptorIntegrationTest` (4), `AuditLogServiceTest` (2), `MaskingUtilTest` (4), `FieldCryptoServiceTest` (6), `PiiFieldServiceTest` (6)
- [x] **소셜 로그인 (OAuth 2.0) / API JWT**
  - **위치:** `OauthController`, `OAUTH_LINK`, `JwtUtil`, `JwtAuthFilter`
  - **주의점:** 네이버·카카오·구글 code flow 직접 구현. API 접근은 JWT 토큰 검증 필터(`/api/**`).
  - **테스트:** `OauthControllerTest` (3), `JwtUtilTest` (4), `ApiControllerTest` (2)
- [x] **2차 인증 (TOTP)**
  - **위치:** `LoginController` (`/login/totp*`), `TotpService`, `APP_USER_TOTP`
  - **주의점:** 로그인 후 TOTP 검증 단계, 비밀키 AES-GCM 암호화, 복구코드 SHA-256 해시 저장, 5회 실패 시 세션 폐기.
  - **테스트:** `TotpServiceTest` (4), `LoginTotpFlowTest` (1)
- [x] **약관 동의**
  - **위치:** `TermsController`, `TermsService`, `TermsInterceptor`, `TERMS`, `USER_TERMS_CONSENT`
  - **주의점:** 버전별 필수/선택 약관 관리 및 동의 이력 보관. 미동의 시 동의 화면으로 리다이렉트.
  - **테스트:** `TermsServiceTest` (4), `TermsInterceptorTest` (4)
- [x] **통합검색 + 초성검색**
  - **위치:** `SearchController`, `SearchService`, `KoreanSearchUtil`, `ElasticsearchSearchIndexService`
  - **주의점:** BOARD(CK·TOAST·FAQ·QNA) 제목 LIKE + `SEARCH_CHOSUNG` 초성 검색. 본문은 태그 제거 요약 표시. Elasticsearch 선택 연동 지원.
  - **테스트:** `SearchMapperTest` (4), `SearchServiceImplTest` (9), `SearchControllerTest` (4), `KoreanSearchUtilTest` (3)
- [x] **메일 발송 / SMS 발송 (알림 채널 격리)**
  - **위치:** `MailAsyncService`, `MailServiceImpl`, `SmsServiceImpl`, `BoardNotificationEventListener`
  - **주의점:** 게시판 중요글 등록 시 `BoardNotificationEvent`를 발행해 SSE·메일·SMS로 독립 발송. `BOARD.MAIL_SENT_YN`, `BOARD.SMS_SENT_YN`으로 1회 선점 발송. 비동기 Executor 분리.
  - **테스트:** `MailServiceImplTest` (10), `AligoSmsSenderTest` (7), `SmsServiceImplTest` (5), `BoardNotificationEventListenerTest` (13)
- [x] **비밀번호 재설정**
  - **위치:** `LoginController` (`/login/findPw*`, `/login/resetPw*`), `AuthServiceImpl`, `PASSWORD_RESET_TOKEN`
  - **주의점:** 아이디·이름·이메일 일치 시 1회용 토큰 해시 저장(SHA-256), 30분 유효, 60초 쿨다운, 메일 링크 전송, 성공 시 세션 만료.
  - **테스트:** `PasswordResetControllerTest` (5)
- [x] **내부 API 제공**
  - **위치:** `InternalApiController`, `InternalApiAuthFilter` (`/internal/v1/*`)
  - **주의점:** `X-Internal-Token` 헤더 검증. 미설정 시 503, 불일치 시 401. 헬스 체크 및 공지 최신 10건(개인정보 제외) 제공.
  - **테스트:** `InternalApiControllerTest` (3)
- [x] **휴가 관리**
  - **위치:** `LeaveController`, `LeaveService`, `LeaveDaysCalculator`, `Leave_SQL.xml`, `LEAVE_BALANCE`, `LEAVE_REQUEST`
  - **주의점:** 내 휴가, 휴가관리, 연차관리 3개 화면. 주말·공휴일 제외 평일 일수 계산, 반차 0.5일. 신청중 포함 잔여 초과 금지, 승인 시 차감·취소 시 환원.
  - **테스트:** `LeaveDaysCalculatorTest` (5), `LeaveControllerTest` (7)
- [x] **회의실 관리 및 예약**
  - **위치:** `MeetingController`, `MeetingService`, `MEETING_ROOM`, `MEETING_RESERVATION`
  - **주의점:** 회의실 마스터 선행, 동일 회의실·날짜 시간대(`HH:mm`) 중복 차단, 본인 및 관리자만 예약 취소 가능.
  - **테스트:** `MeetingControllerTest` (5)
- [x] **약식결재**
  - **위치:** `ApprovalController`, `ApprovalService`, `APPROVAL_DOC`, `APPROVAL_HISTORY`
  - **주의점:** 기안(DRAFT) → 상신(SUBMITTED) → 승인(APPROVED)/반려(REJECTED)/취소(CANCELLED). 지정 결재자 서버 검증, 이력 추적.
  - **테스트:** `ApprovalControllerTest` (5)
- [x] **HR 세트 (기념일·경조사·포상·개인연차)**
  - **위치:** `HrController`, `HrService`, `HR_ANNIVERSARY`, `HR_EVENT`, `HR_REWARD`
  - **주의점:** 기념일·경조사·포상 CRUD 및 개인연차 통합 조회(휴가·당직 연계). 관리자 역할 전용.
  - **테스트:** `HrControllerTest` (4)
- [x] **설문 관리 세트**
  - **위치:** `SurveyController`, `SurveyService`, `SURVEY_TEMPLATE`, `SURVEY`, `SURVEY_QUESTION`, `SURVEY_ITEM`, `SURVEY_PARTICIPATION`, `SURVEY_ANSWER`
  - **주의점:** 템플릿·설문·질문·보기·참여·통계 6종. 단일/복수/주관식, 1인 1회 참여, 공개 후 수정 잠금, 문항별 통계.
  - **테스트:** `SurveyControllerTest` (5)
- [x] **동영상 인코딩 및 강의형 콘텐츠**
  - **위치:** `LectureController`, `LectureService`, `VideoEncodingService`, `LECTURE`, `LECTURE_PROGRESS`
  - **주의점:** 동영상 업로드(FFmpeg 선택), HTTP Range 스트리밍(200/206), 진도율 저장, 시청 완료율 + 설문 참여 결합 수료 판정.
  - **테스트:** `LectureControllerTest` (2), `FfmpegVideoEncodingServiceTest` (7)
- [x] **약도 관리**
  - **위치:** `MapController`, `MapGuideService`, `MAP_GUIDE`
  - **주의점:** 주소·위도·경도·설명. 카카오 지도 키가 설정된 경우에만 JS SDK 로드, 미설정 시 주소/좌표 텍스트만 표시.
  - **테스트:** `MapControllerTest` (2)
- [x] **SSO 연계**
  - **위치:** `SsoController`, `OAUTH_LINK` (`PROVIDER='sso'`)
  - **주의점:** OAuth2 authorization code 흐름. 토큰 교환 및 사전 연계 계정 로그인. 기본 비활성(404).
  - **테스트:** `SsoControllerTest` (1)
- [x] **WebSocket 채팅**
  - **위치:** `ChatController`, `ChatWebSocketHandler`, `ChatWebSocketConfig`, `CHAT_MESSAGE`
  - **주의점:** `/ws/chat`, 세션 쿠키 인증 및 메뉴 권한 검사, 최근 100건 이력 전송, 메모리 브로드캐스트.
  - **테스트:** `ChatWebSocketTest` (6)
- [x] **Spring Batch 일일 집계**
  - **위치:** `BatchController`, `DailyStatsTasklet`, `DailyStatsAggregator`, `STAT_DAILY`
  - **주의점:** `dailyStatsJob`/`dailyStatsStep` 수동 실행. 게시판 건수·로그인·접속·강의수료·설문참여 집계, 멱등 재실행 지원.
  - **테스트:** `BatchControllerTest` (2)

---

### 1.3. 미구현 항목

- [ ] **목록 썸네일 + 상세 지도 표시 패턴** (카카오 appkey 환경 분리 및 JSP 정리 필요. `CommUtil.createThumbnail` 유틸은 존재)
- [ ] **외부 API 연계 (수신)** (타 시스템 수신 API 연동. 타임아웃, 재시도, 서킷브레이커 설계 선행 필요)

---

## 2. API 경로 인벤토리 및 계약

### 2.1. 컨트롤러별 경로 인벤토리

| Prefix | Controller 클래스 | 대표 경로 | HTTP Method 선언 |
| --- | --- | --- | --- |
| `/system` | `admin.system.web.SystemController` | `/menuList`, `/getMenuList.json`, `/getMenuDetailList.json`, `/getChkMenuId.json`, `/setInsertMenu.json`, `/setUpdateMenu.json`, `/setDeleteMenu.json` | 화면 GET, AJAX GET/POST 혼합 |
| `/system` | `admin.system.web.CommCodeController` | `/commCodeList`, `/getMasterList.json`, `/getDetailList.json`, `/getCmmnCodeList.json`, `/getCodeInfo.json`, `/setInsertCode.json`, `/setUpdateCode.json`, `/setDeleteCode.json` | 화면 GET, `.json` POST |
| `/system` | `admin.audit.web.AuditLogController` | `/auditLogList`, `/downloadAuditLogExcel.do` | 화면 GET+POST, 엑셀 GET |
| `/system`, `/file` | `admin.comm.web.FileController` | `/multipartUpload`, `/tusUpload`, `/tusPopup`, `/dropzoneUpload`, `/dropzonePopup`, `/tus`, `/tus/{fileId}`, `/file/tempFileUpload.json`, `/file/saveTempFiles.json`, `/file/setFileDelete.json`, `/file/downloadFile.do` | 화면 GET, TUS(OPTIONS/HEAD/PATCH/DELETE), 다운로드 GET |
| `/login` | `admin.auth.web.LoginController` | `/login`, `/login.json`, `/signup`, `/signup.json`, `/findId`, `/findId.json`, `/findPw`, `/findPw.json`, `/resetPw`, `/resetPw.json`, `/logout`, `/apiDemo`, `/totpVerify.json`, `/totpSetup`, `/totpSetup.json`, `/totpConfirm.json`, `/totpDisable.json` | 화면 GET+POST, `.json` POST |
| `/auth` | `admin.auth.web.UserManageController` | `/userList`, `/userWrite`, `/insertUser.json`, `/updateUser.json`, `/deleteUser.json`, `/unlockUser.json`, `/resetPassword.json`, `/resetTotp.json` | 화면 GET+POST, `.json` POST |
| `/terms` | `admin.terms.web.TermsController` | `/terms`, `/login/termsAgree`, `/login/termsAgree.json` | 화면 GET, 동의 POST |
| `/auth` | `admin.auth.web.RoleManageController` | `/roleList`, `/roleWrite`, `/saveRole.json`, `/deleteRole.json` | 화면 GET+POST, `.json` POST |
| `/login/oauth` | `admin.auth.web.OauthController` | `/{provider}`, `/{provider}Callback`, `/link.json` | 화면 GET+POST, 연계 POST |
| `/login/nice` | `admin.auth.web.NiceVerifyController` | `/start`, `/callback`, `/status.json`, `/clear.json` | GET+POST 혼합, 해제 POST |
| `/api` | `admin.auth.web.ApiController` | `/login.json`, `/noticeList.json`, `/noticeDetail.json`, `/faqList.json` | POST 또는 GET+POST |
| `/board` | `admin.board.web.BoardController` | `/ckeditorList`, `/toastList`, `/toastList.json`, `/ckeditorWrite`, `/toastWrite`, `/detail`, `/viewStats`, `/insertBoard.json`, `/updateBoard.json`, `/deleteBoard.json`, `/updateNotice.json`, `/insertComment.json`, `/deleteComment.json`, `/subscribe`, `/editorImageUpload.json` | 화면 GET+POST, `.json` POST, SSE GET |
| `/sitemap` | `admin.comm.web.SitemapController` | `/sitemap` | 화면 GET (로그인 권한별 메뉴) |
| `/search` | `admin.search.web.SearchController` | `/search`, `/search/reindex.json` | 화면 GET(로그인 필수), 재색인 POST(관리자) |
| `/message` | `admin.message.web.MessageController` | `/inbox`, `/sent`, `/write`, `/detail`, `/insertMessage.json`, `/deleteMessage.json` | 화면 GET+POST, `.json` POST |
| `/leave` | `admin.leave.web.LeaveController` | `/myLeave`, `/leaveList`, `/balanceList`, `/insertLeave.json`, `/cancelLeave.json`, `/approveLeave.json`, `/rejectLeave.json`, `/saveBalance.json` | 화면 GET+POST, `.json` POST |
| `/meeting` | `admin.meeting.web.MeetingController` | `/roomList`, `/roomWrite`, `/insertRoom.json`, `/updateRoom.json`, `/deleteRoom.json`, `/reserveList`, `/insertReservation.json`, `/cancelReservation.json` | 화면 GET+POST, `.json` POST |
| `/approval` | `admin.approval.web.ApprovalController` | `/approvalList`, `/approvalWrite`, `/approvalView`, `/insertDraft.json`, `/updateDraft.json`, `/deleteDraft.json`, `/submitApproval.json`, `/approveApproval.json`, `/rejectApproval.json`, `/cancelApproval.json` | 화면 GET+POST, `.json` POST |
| `/hr` | `admin.hr.web.HrController` | `/anniversaryList`, `/anniversaryWrite`, `/insertAnniversary.json`, `/updateAnniversary.json`, `/deleteAnniversary.json`, `/eventList`, `/eventWrite`, `/insertEvent.json`, `/rewardList`, `/rewardWrite`, `/insertReward.json`, `/personalLeave`, `/savePersonalLeave.json` | 화면 GET+POST, `.json` POST |
| `/survey` | `admin.survey.web.SurveyController` | `/templateList`, `/templateWrite`, `/insertTemplate.json`, `/surveyList`, `/surveyWrite`, `/insertSurvey.json`, `/openSurvey.json`, `/closeSurvey.json`, `/questionList`, `/questionWrite`, `/insertQuestion.json`, `/participateList`, `/surveyTake`, `/submitSurvey.json`, `/stats` | 화면 GET+POST, `.json` POST |
| `/lecture` | `admin.lecture.web.LectureController` | `/lectureList`, `/lectureWrite`, `/myLecture`, `/view`, `/completionList`, `/insertLecture.json`, `/updateLecture.json`, `/openLecture.json`, `/closeLecture.json`, `/progress.json`, `/stream` | 화면 GET+POST, `.json` POST, 스트리밍 GET/HEAD |
| `/map` | `admin.map.web.MapController` | `/mapList`, `/mapWrite`, `/mapView`, `/insertMapGuide.json`, `/updateMapGuide.json`, `/deleteMapGuide.json` | 화면 GET+POST, `.json` POST |
| `/sso` | `admin.sso.web.SsoController` | `/login`, `/callback` | GET+POST, 기본 비활성(404) |
| `/chat` | `admin.chat.web.ChatController`, `ChatWebSocketHandler` | `/chat/chat`, WebSocket `/ws/chat` | 화면 GET, WebSocket 핸드셰이크 |
| `/batch` | `admin.batch.web.BatchController` | `/batchList`, `/runDailyStats.json` | 화면 GET+POST, 실행 POST |
| `/popup` | `admin.popup.web.PopupController` | `/popupList`, `/popupWrite`, `/activePopup.json`, `/popupImageUpload.json`, `/insertPopup.json`, `/updatePopup.json`, `/deletePopup.json` | 화면 GET+POST, `.json` POST |
| `/banner` | `admin.banner.web.BannerController` | `/bannerList`, `/bannerWrite`, `/activeBanner.json`, `/bannerImageUpload.json`, `/insertBanner.json`, `/updateBanner.json` | 화면 GET+POST, `.json` POST |
| `/category` | `admin.category.web.CategoryController` | `/tree`, `/drill`, `/tree.json`, `/saveCategory.json`, `/deleteCategory.json` | 화면·조회 GET+POST, 저장·삭제 POST |
| `/schedule` | `admin.schedule.web.ScheduleController` | `/scheduleList`, `/scheduleWrite`, `/insertSchedule.json`, `/updateSchedule.json`, `/deleteSchedule.json`, `/holidayList`, `/insertHoliday.json`, `/deleteHoliday.json` | 화면 GET+POST, `.json` POST |
| `/poll` | `admin.poll.web.PollController` | `/pollList`, `/pollWrite`, `/pollDetail`, `/insertPoll.json`, `/updatePoll.json`, `/deletePoll.json`, `/votePoll.json` | 화면 GET+POST, `.json` POST |
| `/counsel` | `admin.counsel.web.CounselController` | `/counselList`, `/counselWrite`, `/counselDetail`, `/answerList`, `/checkPassword.json`, `/counselFileUpload.json`, `/insertCounsel.json`, `/updateCounsel.json`, `/deleteCounsel.json`, `/updateCounselAnswer.json` | 화면 GET+POST, `.json` POST |
| `/card` | `admin.card.web.CardController` | `/cardList`, `/cardWrite`, `/insertCard.json`, `/updateCard.json`, `/deleteCard.json`, `/useCard.json`, `/cancelUseCard.json` | 화면 GET+POST, `.json` POST |
| `/address` | `admin.address.web.AddressController` | `/addressList`, `/addressWrite`, `/insertAddress.json`, `/updateAddress.json`, `/deleteAddress.json` | 화면 GET+POST, `.json` POST |
| `/duty` | `admin.duty.web.DutyController` | `/dutyList`, `/dutyWrite`, `/insertDuty.json`, `/updateDuty.json`, `/deleteDuty.json`, `/downloadDutyExcel.do`, `/downloadDutyTemplate.do`, `/uploadDutyExcel.json` | 화면 GET+POST, `.json` POST, 엑셀 GET |
| `/faq` | `admin.faq.web.FaqController` | `/faqList`, `/faqWrite`, `/insertFaq.json`, `/updateFaq.json`, `/deleteFaq.json` | 화면 GET+POST, `.json` POST |
| `/memo` | `admin.memo.web.MemoController` | `/memoList`, `/memoWrite`, `/memoDetail`, `/insertMemo.json`, `/updateMemo.json`, `/deleteMemo.json`, `/confirmMemo.json`, `/opinionMemo.json` | 화면 GET+POST, `.json` POST |
| `/qna` | `admin.qna.web.QnaController` | `/qnaList`, `/qnaWrite`, `/qnaDetail`, `/insertQna.json`, `/updateQna.json`, `/deleteQna.json`, `/saveAnswer.json` | 화면 GET+POST, `.json` POST |
| `/preview` | `admin.preview.web.PreviewController` | `/fileView`, `/canPreview.json` | 파일뷰 GET, 미리보기 판정 GET+POST |
| `/stats` | `admin.stats.web.StatsController` | `/statsList`, `/downloadStatsExcel.do` | 화면 GET+POST, 엑셀 GET |
| `/internal/v1` | `admin.api.web.InternalApiController` | `/health`, `/notices` | GET, `X-Internal-Token` 인증 |
| `/user/**` | 사용자단 컨트롤러 | `/userMain`, `/user/board/*`, `/user/faq/*`, `/user/qna/*`, `/user/poll/*`, `/user/counsel/*` | 화면 GET+POST, 참여/등록 POST |

---

### 2.2. 핵심 기능 상세 JSON 동작 규약

| 경로 | Method | 인증/권한 | Request 파라미터 | Response | 오류 응답 및 주의점 |
| --- | --- | --- | --- | --- | --- |
| `/login/login.json` | POST | 공개 | `userId`, `password` | `{"success":true, "userNm":"...", "isAdmin":...}` | `{"success":false, "message":"..."}`; 이전 세션 무효화 |
| `/login/findPw.json` | POST | 공개 | `userId`, `userNm`, `email` | `{"success":true, "message":"..."}` | 계정 불일치 시에도 동일한 안내 반환(존재 여부 은닉), 60초 쿨다운 |
| `/login/resetPw.json` | POST | 공개 (토큰) | `token`, `password`, `passwordConfirm` | `{"success":true}` | `{"success":false, "message":"..."}`; 30분 만료·1회 사용 시 거부 |
| `/board/insertBoard.json` | POST | 로그인 + 관리자(중요글 시) | 게시글 폼 + `importantYn`, 토큰 | `{"success":true}` | `BoardNotificationEvent` 발행(SSE 및 중요글 메일 요청) |
| `/board/updateNotice.json` | POST | 로그인 + 관리자 | `boardId`, `noticeYn` | `{"success":true}` | CK 게시판만 허용, 공지 Y 전환 시 메일 선점 발송 |
| `/board/insertComment.json` | POST | 로그인 세션 | `boardId`, `content`(1~1000자) | `{"success":true, "commentId":...}` | `{"success":false, "message":"..."}` |
| `/board/deleteComment.json` | POST | 로그인 + 작성자/관리자 | `commentId` | `{"success":true}` | 본인 및 관리자 외 거부 |
| `/message/insertMessage.json` | POST | 로그인 + 토큰 | `receiverId`, `title`(1~200자), `content`(1~4000자) | `{"success":true, "msgId":...}` | 자기 자신 발송 불가, 수신자 검증 |
| `/message/deleteMessage.json` | POST | 로그인 + 소유자 | `msgId`, `folder`(`inbox`\|`sent`) | `{"success":true}` | 함별 소프트 삭제 플래그 갱신 |
| `/leave/insertLeave.json` | POST | 로그인 + 토큰 | `leaveType`, `startDe`, `endDe`, `reason` | `{"success":true, "leaveId":...}` | 주말·공휴일 제외 계산, 잔여 초과 금지, 중복 기간 차단 |
| `/leave/cancelLeave.json` | POST | 로그인 + 본인 | `leaveId` | `{"success":true}` | 승인 건이면 사용일수 자동 환원 |
| `/leave/approveLeave.json` | POST | 로그인 + 휴가 WRITE | `leaveId` | `{"success":true}` | `USED_DAYS + DAYS <= TOTAL_DAYS` 조건부 UPDATE |
| `/meeting/insertReservation.json` | POST | 로그인 + 예약 WRITE | 토큰, `roomId`, `reserveDe`, `startTime`, `endTime`, `title` | `{"success":true, "reserveId":...}` | 동일 회의실·일자 시간 중복 시 거부 |
| `/approval/submitApproval.json` | POST | 로그인 + 기안자 | `docId` | `{"success":true}` | `DRAFT` 상태에서만 상신 가능 |
| `/approval/approveApproval.json` | POST | 로그인 + 지정 결재자 | `docId` | `{"success":true}` | `SUBMITTED` 상태에서 지정 결재자만 승인 가능 |
| `/survey/submitSurvey.json` | POST | 로그인 + 참여 WRITE | 토큰, `surveyId`, `answers_<questionId>` | `{"success":true}` | 중복 참여 차단(1회 제한), 필수 문항 검증 |
| `/lecture/progress.json` | POST | 로그인 + 내강의 WRITE | `lectureId`, `watchedSec` | `{"success":true, "completed":...}` | 시청률 + 설문 참여 결합 수료 판정 |
| `/ws/chat` | WS | 세션 + 채팅 WRITE | 텍스트 메시지 (최대 16KB) | 최근 이력 및 브로드캐스트 | Origin 검사, 단일 인스턴스 메모리 브로드캐스트 |
| `/batch/runDailyStats.json` | POST | 로그인 + 배치 WRITE | 토큰, `statDate`(`YYYY-MM-DD`, 미래 불가) | `{"success":true, "status":...}` | 당일 재집계 시 기존 데이터 삭제 후 재삽입 (멱등) |
| `/internal/v1/health` | GET | `X-Internal-Token` | 없음 | `{"status":"UP", ...}` | 토큰 불일치 401, 미설정 503 |
| `/internal/v1/notices` | GET | `X-Internal-Token` | 없음 | `{"list":[...], "total":...}` | CK 공지 최신 10건 반환, 개인정보 제외 |

---

## 3. 알림 설정과 실패 처리 (SSE, Mail, SMS)

근거: `BoardController`, `BoardNotificationEventListener`, `BoardSseService`, `MailServiceImpl`, `SmsServiceImpl`, `layout.html`.

| 항목 | 현재 규칙 및 주의점 |
| --- | --- |
| **SSE 활성화** | `layout.html`의 `ARKIVE_SSE_ENABLED=false`는 화면 수준 설정이다. 활성화 전 화면별 `window.ARKIVE_SSE` 선언이 layout보다 먼저 로드되는지 확인 필요. |
| **화면별 SSE 옵션** | `window.ARKIVE_SSE = { banner: false };`로 배너 비활성화 가능. `hideAfterMs: 30000`은 30초 후 배너 숨김. 기본값은 계속 표시. |
| **SSE 구독 및 역할 필터** | `GET /board/subscribe` 호출 시 30분 만료 `SseEmitter` 등록. 새 글 등록 시 `selectRoleIdsWithMenuRead`로 해당 게시판 조회 권한 역할을 구해 통로가 겹치는 사용자에게만 브로드캐스트. |
| **메일 기본 설정** | `application.properties`의 `arkive.mail.enabled=false`가 기본값. 활성화 시 발신자(`arkive.mail.from`) 및 SMTP 계정 주입 필요. |
| **메일 수신 및 선점** | 환영 메일 및 공지 알림은 `APP_USER.MAIL_YN='Y'`인 사용자에게만 발송. 비밀번호 재설정은 거래성으로 동의와 무관하게 발송. 게시글 알림은 `BOARD.MAIL_SENT_YN='N'`→`Y` 원자적 선점으로 1회만 발송. |
| **SMS 발송 및 선점** | `ARKIVE_SMS_ENABLED=false` 기본. 활성화 시 `BOARD.SMS_SENT_YN` 선점 후 메뉴 조회 권한·`SMS_YN='Y'`·휴대전화 보유자에게 발송. |
| **채널 격리 및 오류 처리** | SSE, 메일, SMS는 각 채널별 리스너에서 독립 처리되며, 특정 채널 실패가 게시글 저장이나 타 채널 발송에 영향을 주지 않음. 선점 후 발송 실패 시 자동 재시도는 지원하지 않음. |

---

## 4. 테스트 자산 현황 (2026-11-07 기준)

최신 실행 통계 및 클래스 목록은 [src/test/README.md](../../../src/test/README.md)에서 관리한다.

| 기능/영역 | 주요 테스트 클래스 | 검증 범위 | 남은 공백 및 주의점 |
| --- | --- | --- | --- |
| 컨텍스트·MyBatis | `ArkiveSmokeTest` | 컨텍스트 로딩, SqlSessionFactory 배선 | - |
| 게시판 CRUD·조회수 | `BoardMapperTest`, `BoardServiceImplTest`, `BoardControllerTest`, `BoardViewCountDailyWindowTest`, `BoardViewStatsControllerTest` | CRUD, CLOB, 초성, Sanitize, 이중등록, 24시간 조회수 1회 집계, 통계 14일 그래프 | HWP 복붙 브라우저 동작, 조회 로그 정리 정책 |
| 페이징·검색범위 | `BoardPagingControllerTest`, `BoardSearchScopeControllerTest` | 공지 오프셋 페이징, 자료실 커서 페이징, 제목/내용/전체 검색 분리 | 본문 검색 HTML 태그 오탐 |
| 댓글·공지고정 | `BoardCommentControllerTest`, `BoardNoticeMapperTest`, `BoardNoticeControllerTest` | 1단 댓글 등록/삭제, 공지 상단 고정 정렬, 권한 검증 | 댓글 신고/수정 기능 미구현 |
| 알림·SSE | `BoardNotificationEventListenerTest`, `BoardNotificationControllerTest`, `BoardSseServiceTest` | SSE/메일 이벤트 분리, 역할 필터링, 채널 격리, 1회 선점 | 브라우저 실제 EventSource E2E, 실제 SMTP E2E |
| 파일·TUS | `FileControllerTest`, `FileTypeUtilTest` | temp→확정, TUS 청크 업로드/재개, 매직바이트 검사 | 드롭존 UI 실제 동작, 운영 스토리지 권한 |
| 세션·보안 | `AuthInterceptorTest`, `LoginSessionRegistryTest`, `JwtUtilTest`, `TotpServiceTest`, `LoginTotpFlowTest` | 중복 로그인 방지, JWT 발급/검증, TOTP 코드 검증 및 5회 실패 잠금 | 다중 서버 공유 세션, 세션 타임아웃 |
| 권한·사이트맵 | `PermissionMapperTest`, `RoleManageControllerTest`, `MenuPermissionControllerTest`, `SitemapControllerTest` | 역할 CRUD, 메뉴별 READ/WRITE OR 집계, 사이드바 필터 | 시스템 메뉴 권한 예외 처리 |
| 휴가·회의실·결재 | `LeaveDaysCalculatorTest`, `LeaveControllerTest`, `MeetingControllerTest`, `ApprovalControllerTest` | 평일 계산, 반차 0.5일, 회의실 시간 중복 차단, 기안→상신→승인/반려 | 결재선 다단계·대결자 미지원 |
| 설문·강의·채팅 | `SurveyControllerTest`, `LectureControllerTest`, `FfmpegVideoEncodingServiceTest`, `ChatWebSocketTest` | 설문 6종 CRUD/참여, 영상 스트리밍/진도, 채팅 핸드셰이크/이력 | FFmpeg 미설치 환경, 채팅 다중 서버 브로커 |
| 감사·개인정보 | `AuditInterceptorIntegrationTest`, `AuditLogServiceTest`, `MaskingUtilTest`, `FieldCryptoServiceTest`, `PiiFieldServiceTest` | 감사 로그 적재/엑셀, 화면 마스킹, AES-GCM 암복호화 | 상세 화면 마스킹 누락 지점 점검 |
| 외부연계(Mock) | `AligoSmsSenderTest`, `SmsServiceImplTest`, `InternalApiControllerTest`, `AnalyticsModelAdviceTest`, `OauthControllerTest` | SMS 자격증명 검증, 내부 API 토큰 보호, GA4/Clarity 동의 주입 | 실제 서비스 계정 연동 검증 |

---

## 5. 기능 이식 시 함께 확인할 묶음 (Porting Bundles)

| 기능 | 함께 가져갈 필수 자산 및 설정 |
| --- | --- |
| **메일 및 비밀번호 재설정** | `spring-boot-starter-mail`, `EgovConfigAsync`, `MailService`/`MailAsyncService`/`MailServiceImpl`, `AuthServiceImpl` 토큰 로직, `LoginController` 재설정 엔드포인트, `schema.sql` (`PASSWORD_RESET_TOKEN`, `APP_USER.MAIL_YN`), `application.properties` (`spring.mail.*`, `arkive.mail.*`). 게시판 알림 연계 시 `BoardNotificationEvent` 세트 포함. |
| **사이트맵** | `SitemapController`, `templates/thymeleaf/comm/sitemap.html`, `layout.html` 링크, `MenuInterceptor`의 `menuList` 모델 속성. 메뉴 테이블 및 역할 권한 체계 선행 필수. |
| **통합검색** | `SearchController`, `SearchService`/`SearchServiceImpl`, `SearchMapper`·`Search_SQL.xml`, `search/searchList.html`, CSS `.search-snippet`. 초성 검색 필요 시 `KoreanSearchUtil` 및 `BOARD.SEARCH_CHOSUNG` 컬럼 포함. ES 연동은 선택 사항. |
| **감사 및 개인정보 보호** | `AuditInterceptor`/`AuditEvent`/`AuditLogEventListener`/`AuditLogService`, `MaskingInterceptor`/`MaskingUtil`, `PiiFieldService`/`FieldCryptoService`, `schema.sql` (`AUDIT_LOG`, 암호화 컬럼 `VARCHAR(500)`), `application.properties` (`arkive.crypto.field-key`, `arkive.audit.*`). 대상 환경에 암호화 키 주입 필수. |

---

## 6. 기능 유지 기준

- 기능을 추가하거나 변경할 때는 실제 화면(HTML/JS), Controller, Service, Mapper/SQL, DDL(`db/schema.sql`), 시드(`db/seed.sql`) 및 의존성을 함께 갱신한다.
- Arkive는 타 프로젝트 이식을 위한 레퍼런스이므로 특정 DB 전용 문법(HSQLDB 전용 함수 등) 사용을 지양하고 표준 SQL을 우선한다.
- `@RequestMapping` 선언만 있고 화면이나 비즈니스 로직이 미완성인 경우 '구현완료'로 표시하지 않고 주의점에 제약을 명시한다.
- 이식 수행 기록은 [feature-porting-sheet.md](../portable/feature-porting-sheet.md), 테스트 이식은 [test-porting-sheet.md](../portable/test-porting-sheet.md)에 남긴다.
