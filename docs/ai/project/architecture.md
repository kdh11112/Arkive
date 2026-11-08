# 아키텍처 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 인증·인가 적용 범위/역할 매핑, 외부 연계 호출 흐름과 소유자, 프로파일별 구성, 운영 배포 토폴로지, 전체 모듈 경계.
필요한 근거/확인자: Security 설정과 필터·인터셉터, API 명세/호출부, 환경별 설정, 운영 아키텍처 문서 및 담당자.
완성 방법: 코드·설정에서 확인한 흐름을 먼저 보강하고, 운영 구성과 책임은 담당자 확인 자료를 근거로 추가한다.
완료 기준: 업무·공통 모듈 경계와 실제 인증·외부 연계·배포 구성이 출처와 함께 기록되거나, 미지원/미확인이 담당자 확인으로 종결되어 있다.
-->

> 저장소 코드에서 확인한 구현 구조다. 목표 아키텍처나 전체 운영 구성을 의미하지 않는다.

## 애플리케이션 구성

Spring MVC의 기본 흐름은 아래와 같다. 기능별 파일 목록은 [기능 인벤토리](function-inventory.md#확인된-코드-영역)에 두고, 여기에는 공통 계층과 특수 흐름을 기록한다.

```text
Thymeleaf + JS → Controller → Service/Impl → Mapper 인터페이스 → Mapper XML → DB
예: arkive.admin.system.web.SystemController → SystemService/SystemServiceImpl → SystemMapper → System_SQL.xml
```

| 구성 | 위치·확인된 방식 |
| --- | --- |
| 진입 | `egovframework.example.EgovBootApplication` (`src/main/java/egovframework/example/EgovBootApplication.java`) |
| 패키지 | 업무 `arkive.*`, 샘플·설정 `egovframework.example.*`, 공통 eGovFrame `egovframework.com.*` |
| 화면·JS | `src/main/resources/templates/thymeleaf/`, 공통 조각 `layout/`·`comm/`, 정적 파일 `static/`; `th:replace`·jQuery `$.ajax` |
| Mapper XML | `src/main/resources/egovframework/sqlmap/example/mappers/`; 인터페이스·namespace·SQL ID·입출력 타입을 함께 확인 |
| 공통 파일 | `arkive.admin.comm`의 Controller·Service·Mapper·업로드/다운로드 유틸리티 |
| Java Config | `egovframework.example.config`의 DB·Mapper·트랜잭션·웹·비동기 설정. 특수 설정은 아래 채팅·배치 참조 |
| 검증·로깅 | Controller의 `@Valid`·`BindingResult` 예제, Lombok `@Slf4j`·SLF4J `LoggerFactory` 혼재. 수정 시 해당 모듈 방식 유지 |

## 인증·권한과 공통 처리

| 영역 | 호출 흐름·경계 |
| --- | --- |
| 세션 | `LoginController`·`OauthController` → `LoginSessionRegistry`: 사용자별 최신 세션 1개, 이전 세션 만료. `EgovConfigWeb`의 `HttpSessionListener`가 만료 매핑 제거. 단일 인스턴스 메모리 기준 |
| 메뉴 권한 | `APP_USER_ROLE` → `APP_ROLE_MENU` → `PermissionServiceImpl`의 메뉴별 OR 집계 → `MenuPermissionInterceptor`의 READ/WRITE 검사. `MenuInterceptor`는 같은 READ 권한으로 사이드바 필터. `ADMIN_YN='Y'`는 슈퍼관리자이며 `/system`·`/auth`는 `AuthInterceptor`의 관리자 전용 정책 유지 |
| 사이트맵·공통 기능 | `SitemapController`(`/sitemap`)는 `MenuInterceptor`의 `menuList`를 재사용하며 메뉴 권한 매핑 없이 로그인 검사만 적용. 댓글·쪽지·다운로드·SSE 구독도 메뉴 CRUD 권한과 분리한 로그인 사용자 공통 기능 |
| TOTP·약관 | `LoginController`의 대기 세션 → `/login/totpVerify.json` 코드·복구코드 확인. `TermsInterceptor` → `/login/termsAgree` 유도 → `USER_TERMS_CONSENT` 동의 이력 |
| SSO | `SsoController`(`/sso`)의 authorization code·세션 state 비교·토큰 교환·사용자정보 조회 → `OAUTH_LINK`의 `PROVIDER='sso'` 연결 확인 → 기존 `LoginSessionService`·TOTP 단계. 자동 가입 기본 비활성 |
| 재설정 | `LoginController` → `AuthServiceImpl.issuePasswordResetToken` → `PASSWORD_RESET_TOKEN` 해시 저장 → `MailAsyncService` 링크 메일. `/login/resetPw`·`AuthServiceImpl.resetPasswordByToken` 성공 후 Controller가 기존 세션 만료. 세부 규칙은 [기능 상세](feature-status.md) |
| 감사·개인정보 | `AuditInterceptor` → `AuditEvent` → `AuditLogEventListener` → `AuditLogService`/`AUDIT_LOG`. `/system/auditLogList` 조회·엑셀, `AuditRetentionJob` 보관주기 삭제(기본 0=비활성). `MaskingInterceptor`는 `List<EgovMap>` 마스킹, `PiiFieldService`·`FieldCryptoService`는 연락처 AES-GCM(`enc:v1:`) 처리. `AuthServiceImpl`의 사용자 저장은 `phone`만 암호화하고 `EMAIL`은 조회용 평문 |

## 비동기 및 예약 작업

| 작업 | 확인된 호출 흐름 |
| --- | --- |
| 게시글 알림 | `BoardController` → `BoardNotificationEvent` → `BoardNotificationEventListener` → SSE·메일·SMS 채널. 채널별 실패를 격리한다. 구독·수신 동의·선점·실패 규칙은 [알림 상세](feature-status.md#알림-설정과-실패-처리) |
| 메일 | 회원가입·OAuth 가입·관리자 등록·비밀번호 찾기 → `MailAsyncService` → `@Async("mailTaskExecutor")` → `MailServiceImpl` → `JavaMailSender`. Executor는 `EgovConfigAsync`, 기본 비활성 |
| DB 덤프 | `arkive.com.config.QuartzConfig`의 10분 Trigger → `DBJob` → HSQLDB `SCRIPT`. 경로·기존 파일 삭제·복구 미확인 사항은 [실행 설정](project-overview.md#실행-전-확인할-설정) |
| 일일 집계 | `BatchConfig`의 `@EnableBatchProcessing`·`DefaultBatchConfiguration`으로 Boot 자동 구성 대신 `ResourcelessJobRepository`·`txManager` 사용. `dailyStatsJob` → `dailyStatsStep` → `DailyStatsTasklet` → `DailyStatsAggregator` → `BatchMapper`/`Batch_SQL.xml` → `STAT_DAILY`. `AUDIT_LOG`·`LECTURE_PROGRESS`·`SURVEY_PARTICIPATION` 집계, `BatchService.runDailyStats`가 중복 실행 직렬화·미래 날짜 거부. 수동 실행만 지원하며 실행 이력·재시작 없음 |

## 기능별 특수 흐름

표준 CRUD 계층은 위 기본 흐름과 [기능 인벤토리](function-inventory.md)를 따른다. 아래는 화면·권한·공통 서비스가 얽히는 부분이다.

| 기능 | 별도 확인할 연결 |
| --- | --- |
| 댓글·쪽지 | `BoardController` 상세 → `BoardService.selectCommentList/selectCommentCount`; 댓글 API → `BoardService`/`BoardMapper`/`Board_SQL.xml`. `MessageController`(`/message`) 상세는 수신자 읽음 처리 |
| 검색 | `SearchController`(`/search`) → `SearchServiceImpl` → 기본 DB LIKE, `arkive.search.elasticsearch.enabled=true`일 때 `SearchIndexService`의 ES 조회. CK·TOAST·FAQ·QNA 제목/초성 검색, `CONTENT`는 태그 제거 요약만 노출. `AuthInterceptor` 로그인 검사, `/search/reindex.json`은 관리자 전용 |
| 통계·웹분석·내부 API | `/stats`는 게시판·`AUDIT_LOG` 집계/엑셀, `AnalyticsModelAdvice`·`arkive-analytics.js`는 동의 후 GA4·Clarity 로드. `/internal/v1`은 `InternalApiAuthFilter`의 `X-Internal-Token` 검사 |
| 회의실 | `MeetingController` → `MeetingServiceImpl`이 관리·예약을 함께 처리. `MenuPermissionInterceptor`는 경로의 `room` 여부로 `ME02050100`/`ME02050200` 선택, 예약 취소는 본인·관리자만 |
| 약식결재 | `ApprovalController` → `ApprovalServiceImpl`, 목록 범위(`my`/`todo`/`all`)·상세 접근 서버 제한, 문서당 결재자 1명·상태 이력 |
| HR | `HrController` → `HrServiceImpl`, 개인연차는 `LeaveService`·`Duty` 조회 조합. 메뉴 `ME02090400`~`ME02090700`은 관리자 역할 시드 |
| 설문 | `SurveyController` → `SurveyServiceImpl`, 관리·참여·통계 분리. `MenuPermissionInterceptor.resolve`가 경로로 `ME02031600`~`ME02031900` 판정, 설문당 1회 참여 |
| 강의·동영상 | `LectureController` → `LectureServiceImpl`, 파일은 `VideoEncodingService`(기본 `FfmpegVideoEncodingService`). `/lecture/stream`은 `Range` 200/206, 진도 저장 시 시청 완료율·연결 설문 참여로 수료 판정 |
| 약도 | `MapController` → `MapGuideService`/`MapGuideServiceImpl` → `MapGuideMapper`/`MapGuide_SQL.xml`. `Globals.kakao.mapKey`가 없으면 주소·좌표만 표시 |
| 채팅 | `ChatController`(`/chat/chat`) 화면 → `ChatWebSocketConfig`의 `/ws/chat` → `ChatHandshakeInterceptor`의 세션·READ/WRITE·Origin 검사 → `ChatWebSocketHandler`의 최근 이력·`ChatService`/`ChatMapper`/`Chat_SQL.xml` 저장·메모리 브로드캐스트. 실제 서블릿 `ServerContainer`가 있을 때 버퍼·유휴 한도 적용 |

## 관련 기록과 이식 경계

- 로컬 구성·운영 미확인 사항·기존 참조와 구현 선택은 [프로젝트 개요](project-overview.md)에서 관리한다. 역할·메뉴 정책 승인, 외부 계약, 운영 토폴로지·소유 조직은 여전히 미확인이다.
- 기존 자동·GUI·HTTP 확인 날짜와 범위는 [현행 분석](current-state-analysis.md#기존-검증-이력)에 보존한다. 현재 구현·검증 분류는 [README.md](../../../README.md), 기능 제약은 [기능 상세](feature-status.md)를 따른다.
- 이식 순서는 [PORTING.md](../../../PORTING.md), 기능별 추가 의존성·설정 묶음은 [이식 묶음](function-inventory.md#이식-시-함께-확인할-묶음)을 따른다. 테스트 준비·맞춤 수정은 스펙 담당자의 별도 작업이다.

수정 전 관련 호출 흐름과 설정을 검색한다. 이 요약만으로 보안 적용·기능 소유권·다중 서버 지원을 단정하지 않는다.
