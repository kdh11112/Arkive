# src/test — 수정 복사 가능한 테스트 팩

이 폴더를 통째로 복사하지 않는다. **공통 테스트 팩**과 **기능별 테스트**를 나눠서 가져간다.
테스트 코드는 업무 규칙·URL·스키마를 알고 있으므로, 대상 프로젝트에서 그대로 실행되는 경우는 거의 없다.

## 1. 공통 테스트 팩 (거의 그대로 복사)

| 자산 | 대상에서 바꿀 것 |
| --- | --- |
| `support/ArkiveIntegrationTest.java` | `IntegrationTest`로 이름 변경, `classes = 대상Application.class` |
| `support/TestSessions.java` | 세션 키, 사용자 객체 형태(`EgovMap` → 대상 VO/UserDetails) |
| `resources/application-test.properties.template` | DB 제품·URL·경로·포트 |
| `pom.xml` 테스트 의존성 | 대상 프로젝트에 없는 의존성만 추가 |
| `pom.xml` JaCoCo 설정 | `includes`를 대상 클래스로 교체 |
| `verify.cmd` | 명령·모듈 경로가 다르면 수정 |

`application-test.properties.template`은 복사 후 `application-test.properties`로 이름을 바꾼다.
main 설정을 덮어쓰므로 테스트에 필요한 키만 둔다.

## 2. 기능별 테스트 (기능 코드와 함께 수정 복사)

- `admin/board/BoardFixtures.java` — 게시판 테스트 데이터
- `admin/board/service/impl/BoardMapperTest.java` — 게시판·댓글 SQL
- `admin/board/web/BoardCommentControllerTest.java` — 댓글 API 권한·검증
- `admin/message/MessageFixtures.java` — 쪽지 테스트 데이터
- `admin/message/service/impl/MessageMapperTest.java` — 받은/보낸함·읽음·삭제
- `admin/message/web/MessageControllerTest.java` — 쪽지 API 권한·검증
- `admin/comm/web/SitemapControllerTest.java` — 사이트맵 로그인·권한별 메뉴 표시
- `admin/search/service/impl/SearchMapperTest.java` — BOARD 제목 LIKE 통합검색·페이징
- `admin/search/service/impl/SearchServiceImplTest.java` — 유형 라벨·HTML 요약·CLOB 변환
- `admin/search/web/SearchControllerTest.java` — 로그인·빈 검색어·무결과·결과 렌더
- `admin/board/event/BoardNotificationEventListenerTest.java` — SSE·메일 채널 분리, 실패 격리 (Mockito 단위)
- `admin/board/web/BoardNotificationControllerTest.java` — 게시글 저장 API의 알림 이벤트 발행
- `admin/mail/service/impl/MailServiceImplTest.java` — 메일 발송 조건·1회 발송·SMTP 실패 (Mockito 단위)
- `admin/auth/web/PasswordResetControllerTest.java` — 비밀번호 재설정 토큰 발급·소비·만료

기능별 fixture는 공통 `support`에 두지 않는다. 게시판만 이식할 때 쪽지 fixture까지 끌고 오지 않기 위해서다.

## 3. 복사 순서

1. `ArkiveIntegrationTest`와 `TestSessions`를 대상 패키지로 복사한다.
2. `application-test.properties.template`을 대상 DB에 맞게 채운다.
3. 기능 코드(Controller → Service → Mapper/XML → SQL)를 복사한다.
4. 해당 기능의 fixture와 테스트를 함께 복사한다.
5. 패키지·앱 클래스·URL·메시지·세션·테이블·seed를 대상에 맞게 바꾼다.
6. 단독 테스트 → 전체 `verify.cmd` 순서로 확인한다.

상세 체크는 [test-porting-sheet.md](../../docs/ai/portable/test-porting-sheet.md)에 기록한다.

## 4. 현재 테스트

| 클래스 | 개수 | 범위 | 복사 난이도 |
| --- | ---: | --- | --- |
| `ArkiveSmokeTest` | 1 | 컨텍스트·MyBatis 배선 | 대상에서 새로 작성 |
| `BoardMapperTest` | 3 | 게시판 CRUD, 댓글, cascade | 높음(스키마·SQL) |
| `BoardCommentControllerTest` | 3 | 댓글 인증·검증·삭제 권한 | 중간(URL·메시지·세션) |
| `BoardNoticeMapperTest` | 2 | 공지 고정 정렬·해제·CK 외 거부 | 높음(스키마·SQL) |
| `BoardNoticeControllerTest` | 4 | 공지 고정 인증·관리자 권한·입력 검증·목록 렌더링 | 중간(URL·메시지·세션) |
| `BoardNotificationEventListenerTest` | 9 | CK/TOAST SSE 역할, 메일 요청 분기, 채널별 실패 격리 | 낮음~중간(Mockito) |
| `BoardNotificationControllerTest` | 4 | 등록·수정·공지 고정 시 알림 이벤트 발행 조건 | 중간(API·세션·이벤트) |
| `SitemapControllerTest` | 3 | 로그인 필요, 관리자 메뉴 트리 렌더, 역할 없는 사용자의 빈 메뉴 | 낮음~중간(세션·interceptor) |
| `SearchMapperTest` | 3 | CK/TOAST/FAQ/QNA 제목 검색, 본문만 일치 제외, LIMIT/OFFSET·건수 | 높음(스키마·SQL) |
| `SearchServiceImplTest` | 3 | 유형 라벨, HTML 제거·160자 요약, CLOB 변환 | 낮음(Mockito) |
| `SearchControllerTest` | 4 | 로그인 필요, 결과·요약 렌더, 빈 검색어·무결과 안내 | 중간(URL·세션·템플릿) |
| `MessageMapperTest` | 2 | 받은/보낸함, 읽음, 소프트 삭제, 검색 | 높음(스키마·SQL) |
| `MessageControllerTest` | 4 | 쪽지 인증·중복제출·검증·권한 | 중간(URL·메시지·세션) |
| `HtmlSanitizerTest` | 6 | XSS 허용/차단 | 낮음(거의 그대로) |
| `FileTypeUtilTest` | 5 | 매직바이트·확장자 위조 | 낮음(거의 그대로) |
| `JwtUtilTest` | 4 | JWT 발급·검증, 시크릿 불일치·만료·형식 오류 | 낮음(거의 그대로) |
| `LoginSessionRegistryTest` | 5 | 사용자별 세션 교체·만료, 사용자 전환, 세션 소멸 정리 | 낮음(세션 키만 변경) |
| `LoginSessionControllerTest` | 1 | 실제 로그인 2회 시 이전 세션 만료 | 중간(로그인 URL·seed 계정) |
| `AuthInterceptorTest` | 5 | 공개 경로, JSON 401, 화면 redirect, 관리자 경로 | 중간(세션·경로) |
| `ErrorResponseTest` | 3 | 미매핑 URL·정적 리소스 404, 메일 비활성 시 health UP | 낮음~중간(전역 예외·설정) |
| `PermissionMapperTest` | 4 | V5 역할 시드, 사용자 메뉴 권한 집계, 기본 사용자 역할의 시스템·업로드 메뉴 제외, SSE 대상 역할 조회 | 높음(스키마·SQL) |
| `RoleManageControllerTest` | 7 | 권한관리 화면, 역할·메뉴·사용자 저장, 필수값, 잘못된 사용자 배정 거부, 기본 역할 삭제 거부, 사용자 삭제 시 역할 매핑 정리 | 중간(URL·메시지·세션) |
| `MenuPermissionControllerTest` | 5 | 조회 전용/쓰기 역할, 역할 없음 거부, 관리자 우회, 사이드바 필터 | 중간(URL·메시지·세션) |
| `MailServiceImplTest` | 10 | 비활성·발신자 없음, 수신 동의, 환영/재설정/공지 메일, HTML escape, 1회 선점, SMTP 실패 무시 | 낮음~중간(Mockito·메일 본문) |
| `PasswordResetControllerTest` | 5 | 토큰 발급 조건·1회 사용·만료·입력 검증·쿨다운 | 중간(URL·메시지·DB) |
| `BoardControllerTest` | 5 | CK/TOAST 저장·sanitize·이중등록·조회수·에디터 이미지 | 중간(URL·메시지·세션) |
| `BoardPagingControllerTest` | — | 오프셋/커서 페이징 | 중간(URL·세션) |
| `BoardServiceImplTest` | — | CLOB 변환·초성·색인 이벤트 | 낮음(Mockito) |
| `BoardSseServiceTest` | 5 | 게시판 유형·ALL 채널·대상 역할 없음·미전송 | 낮음~중간(Mockito) |
| `FileControllerTest` | 7 | temp→확정·다운로드·삭제, TUS 전체 흐름, 묶음 ZIP, 경로 이탈 | 중간(URL·세션·파일) |
| `OpenApiDocsTest` | 2 | Swagger `/v3/api-docs` 경로·제목, `/swagger-ui` 응답 | 낮음(거의 그대로) |
| `FaqControllerTest` | — | FAQ 초성·sanitize·이중등록·제목 누락 | 중간(URL·메시지·세션) |
| `QnaControllerTest` | — | 관리자 답변 저장·권한·답변 폼 | 중간(URL·메시지·세션) |
| `QnaMapperTest` | — | QNA 조회 조건 | 높음(스키마·SQL) |
| `UserQnaControllerTest` | 6 | 익명 등록 PUBLIC 강제·sanitize·이중등록·내부글 | 중간(URL·메시지·세션) |
| `MemoControllerTest` | — | 메모보고 상태 전이·이중등록·필수값 | 중간(URL·메시지·세션) |
| `PopupControllerTest` | — | 팝업 게시기간·사용여부·날짜 역전·이중등록 | 중간(URL·메시지·세션) |
| `BannerControllerTest` | — | 배너 노출위치·정렬·게시중 필터 | 중간(URL·메시지·세션) |
| `ScheduleControllerTest` | — | 일정 유형·일자 필터·공휴일 병합·삭제 | 중간(URL·메시지·세션) |
| `AddressControllerTest` | — | 연락처 암호화·마스킹·평문 복원·이중등록·이름 필수 | 중간(URL·암호화) |
| `CardControllerTest` | — | 명함 공개/내 명함·사용등록·이중등록·필수값 | 중간(URL·메시지·세션) |
| `CounselControllerTest` | — | 상담 비밀번호 확인·답변·등록·첨부 필수값 | 중간(URL·메시지·세션) |
| `PollControllerTest` | — | POLL 세션당 1회 투표·항목 초기화·투표기간 | 중간(URL·메시지·세션) |
| `DutyControllerTest` | — | 당직 저장·이중등록·엑셀 양식·업로드·다운로드 마스킹 | 중간(URL·엑셀) |
| `CommCodeControllerTest` | — | 공통코드 마스터·상세·연쇄 삭제·필수값 | 중간(URL·메시지·세션) |
| `UserManageControllerTest` | — | 사용자 관리 CRUD·기본 역할·잠금 해제·임시 비밀번호 | 중간(URL·메시지·세션) |
| `AuditInterceptorIntegrationTest` | 4 | 인터셉터→이벤트→AUDIT_LOG 적재, PII_VIEW·PII_DOWNLOAD | 중간(세션·인터셉터) |
| `AuditLogControllerTest` | — | 감사 로그 조회·엑셀 다운로드 | 중간(URL·세션) |
| `AuditLogServiceTest` | — | 감사 로그 서비스·보관 정리 | 낮음(Mockito) |
| `MaskingUtilTest` | — | 마스킹 규칙 | 낮음(거의 그대로) |
| `FieldCryptoServiceTest` | — | AES-GCM 암복호화·키 미설정 거부·이중 암호화 방지 | 낮음(거의 그대로) |
| `PiiFieldServiceTest` | — | PII 필드 서비스·평문 통과 | 낮음(거의 그대로) |
| `OauthControllerTest` | — | OAuth 미지원·미설정·잘못된 state 거부 | 중간(URL·세션) |
| `ApiControllerTest` | — | `/api/**` JWT 토큰 검증 | 낮음~중간(URL·JWT) |
| `InternalApiControllerTest` | — | 내부 API 토큰 401·health UP·개인정보 미포함 | 낮음~중간(URL·토큰) |
| `TotpServiceTest` | — | TOTP 설정·복구코드 1회 사용·초기화·잘못된 코드 | 낮음~중간(Mockito) |
| `LoginTotpFlowTest` | — | 로그인 후 TOTP 요구·5회 실패 잠금 | 중간(세션·로그인) |
| `TermsInterceptorTest` | — | 약관 미동의 시 리다이렉트 | 중간(세션·인터셉터) |
| `TermsServiceTest` | — | 약관 버전·동의 이력 | 낮음(Mockito) |
| `SignupControllerTest` | — | 회원가입·아이디 찾기 마스킹·비밀번호 재설정 토큰 | 중간(URL·메시지·세션) |
| `StatsControllerTest` | — | 통계 집계·역순 기간 보정·엑셀 다운로드 | 중간(URL·세션) |
| `AnalyticsModelAdviceTest` | — | GA4/Clarity ID 미설정·형식 오류·동의값 | 낮음(Mockito) |
| `AligoSmsSenderTest` | — | 알리고 비활성·자격증명·LMS 전환·실패 응답 | 낮음~중간(Mockito) |
| `SmsServiceImplTest` | — | SMS 수신 대상 필터·중복 발송 차단 | 낮음~중간(Mockito) |
| `NiceVerifyControllerTest` | — | NICE 미설정 시 5xx, 미인증 상태 | 중간(URL·세션) |
| `KoreanSearchUtilTest` | — | 한글 초성 검색 유틸 | 낮음(거의 그대로) |
| `LeaveControllerTest` | — | 휴가 신청·승인·차감·환원·잔여 초과 금지 | 중간(URL·메시지·세션) |
| `LeaveDaysCalculatorTest` | — | 평일 일수·반차 0.5일 계산 | 낮음(거의 그대로) |
| `MeetingControllerTest` | — | 회의실 CRUD·예약 시간 중복·역전·취소 | 중간(URL·메시지·세션) |
| `ApprovalControllerTest` | — | 약식결재 기안→상신→승인/반려·결재자 확인·이력 | 중간(URL·메시지·세션) |
| `HrControllerTest` | — | HR 기념일·경조사·포상·개인연차 | 중간(URL·메시지·세션) |
| `SurveyControllerTest` | — | 설문 템플릿·질문·항목·참여·통계 | 중간(URL·메시지·세션) |
| `LectureControllerTest` | — | 강의 시청 위치·수료 판정·이어보기·스트리밍 | 중간(URL·메시지·세션) |
| `FfmpegVideoEncodingServiceTest` | — | FFmpeg 비활성 mp4 저장·확장자/길이/크기 거부 | 낮음~중간(Mockito) |
| `MapControllerTest` | — | 약도 CRUD·사용여부·좌표 검증·이중등록 | 중간(URL·메시지·세션) |
| `SsoControllerTest` | — | SSO 비활성 시 404 | 중간(URL·세션) |
| `ChatWebSocketTest` | — | WebSocket 세션·권한·Origin·이력·브로드캐스트 | 높음(WebSocket) |
| `BatchControllerTest` | — | 일일 통계 집계·멱등 재집계·날짜·토큰 검증 | 중간(URL·메시지·세션) |

합계 314개, 테스트 클래스 78개. 전체 커버리지는 instruction ~51%, branch ~51% (2026-10-25 `verify.cmd` 기준).
JaCoCo 게이트는 검증된 일부 클래스만 강제하며, 메일은 `MailServiceImpl`(branch 70%·line 80%), 게시판 알림은 `BoardNotificationEventListener`(branch 80%·line 90%), 통합검색은 `SearchServiceImpl`(branch 80%·line 90%)를 추가로 검사한다.

## 5. 실행

```cmd
verify.cmd
rem 단독 실행 예: mvn -Dtest=BoardMapperTest test
```

- 테스트 DB는 `target/test-db`, 포트 9002를 써서 앱(9001) 실행 중에도 돈다.
- `@ArkiveIntegrationTest`는 `@SpringBootTest` + `@AutoConfigureMockMvc` + 테스트 properties를 묶은 합성 애노테이션이다.
  통합 테스트마다 직접 `@SpringBootTest`를 붙이면 설정 차이로 컨텍스트가 갈라져 같은 DB 포트를 두 번 열 수 있다.
- `src/test/resources/application.properties`를 두지 않는다. main 설정 전체가 가려진다.
