# 테스트 자산·실행 현황 (Arkive)

이 문서는 Arkive 전용 테스트 목록과 최신 실행 수치의 원본이다. 대상에 복사하지 않으며 다른 문서에는 수치·전체 목록을 다시 적지 않고 이 문서를 참조한다.

## 최신 실행 결과

2026-11-08 `verify.cmd` 기준: **84개 클래스·338개 테스트 통과**, 실패·오류·skip 0. JaCoCo instruction **52.0%**, branch **43.9%**, line **52.7%**, 설정된 클래스별 게이트 통과.

근거: `target/surefire-reports/TEST-*.xml`, `target/site/jacoco/jacoco.xml`, `pom.xml`. 갱신할 때 전체 검증을 실행하고 보고서와 아래 클래스별 개수를 함께 대조한다. 숫자는 업무·GUI·외부 연동의 검증완료를 뜻하지 않는다.

## 이식 책임과 선택

이 폴더를 통째로 가져가지 않고 공통 팩과 해당 기능의 테스트·fixture만 선택한다. 테스트 준비·맞춤 수정은 스펙 담당자의 별도 작업이며 기대 결과 변경은 인간 승인이 필요하다. 구현 에이전트는 테스트 파일을 만들거나 수정하지 않고 필요한 변경점을 보고한다. 아래 맞춤 항목은 수정 권한이 아니다.

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

기능별 fixture는 해당 기능과 함께 선택한다(`admin/board/BoardFixtures.java`, `admin/message/MessageFixtures.java`). 공통 `support`에 넣어 다른 기능까지 끌고 오지 않는다. 복사 순서·분류·맞춤 항목은 [테스트 이식 시트](../../docs/ai/portable/test-porting-sheet.md)에서 관리한다.

## 2. 현재 테스트

| 클래스 | 개수 | 범위 | 복사 난이도 |
| --- | ---: | --- | --- |
| `ArkiveSmokeTest` | 1 | 컨텍스트·MyBatis 배선 | 대상에서 새로 작성 |
| `BoardMapperTest` | 4 | 게시판 CRUD, 댓글, cascade | 높음(스키마·SQL) |
| `BoardCommentControllerTest` | 3 | 댓글 인증·검증·삭제 권한 | 중간(URL·메시지·세션) |
| `BoardNoticeMapperTest` | 2 | 공지 고정 정렬·해제·CK 외 거부 | 높음(스키마·SQL) |
| `BoardNoticeControllerTest` | 4 | 공지 고정 인증·관리자 권한·입력 검증·목록 렌더링 | 중간(URL·메시지·세션) |
| `BoardNotificationEventListenerTest` | 13 | CK/TOAST SSE 역할, 메일·SMS 요청 분기, 채널별 실패 격리 | 낮음~중간(Mockito) |
| `BoardNotificationControllerTest` | 4 | 등록·수정·공지 고정 시 알림 이벤트 발행 조건 | 중간(API·세션·이벤트) |
| `SitemapControllerTest` | 3 | 로그인 필요, 관리자 메뉴 트리 렌더, 역할 없는 사용자의 빈 메뉴 | 낮음~중간(세션·interceptor) |
| `SearchMapperTest` | 4 | CK/TOAST/FAQ/QNA 제목 검색, 본문만 일치 제외, LIMIT/OFFSET·건수 | 높음(스키마·SQL) |
| `SearchServiceImplTest` | 9 | 유형 라벨, HTML 제거·160자 요약, CLOB 변환 | 낮음(Mockito) |
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
| `BoardPagingControllerTest` | 2 | 오프셋/커서 페이징 | 중간(URL·세션) |
| `BoardSearchScopeControllerTest` | 7 | 제목/내용/제목+내용 분리 검색, 잘못된 값 title 폴백, CK·TOAST·사용자 공지/자료실 | 중간(URL·SQL·세션) |
| `BoardServiceImplTest` | 11 | CLOB 변환·초성·색인 이벤트 | 낮음(Mockito) |
| `BoardViewCountDailyWindowTest` | 6 | 세션·쿠키 24시간 중복 집계, 23/25시간 경계, 사용자 공지 동일 규칙 | 중간(URL·세션·SQL) |
| `BoardViewStatsControllerTest` | 3 | 14일 그래프·표·순위, 집계 시 로그 1건, 로그 없을 때 0% 렌더 | 중간(URL·SQL·템플릿) |
| `BoardEditorImageCleanupJobTest` | 3 | 참조 없는 오래된 이미지 정리, 비활성 시 미삭제, 빈 참조 ID 처리 | 중간(DB·파일) |
| `BoardMarkupAccessibilityTest` | 3 | 공지 고정 버튼 접근성 이름, 상세 링크, 댓글 입력 이름 | 중간(URL·템플릿) |
| `BoardWriteDraftTemplateTest` | 2 | CK/TOAST 임시저장 배너와 자동 타이머·confirm 제외 | 중간(URL·템플릿) |
| `BoardSseServiceTest` | 5 | 게시판 유형·ALL 채널·대상 역할 없음·미전송 | 낮음~중간(Mockito) |
| `FileControllerTest` | 7 | temp→확정·다운로드·삭제, TUS 전체 흐름, 묶음 ZIP, 경로 이탈 | 중간(URL·세션·파일) |
| `OpenApiDocsTest` | 2 | Swagger `/v3/api-docs` 경로·제목, `/swagger-ui` 응답 | 낮음(거의 그대로) |
| `FaqControllerTest` | 2 | FAQ 초성·sanitize·이중등록·제목 누락 | 중간(URL·메시지·세션) |
| `QnaControllerTest` | 3 | 관리자 답변 저장·권한·답변 폼 | 중간(URL·메시지·세션) |
| `QnaMapperTest` | 3 | QNA 조회 조건 | 높음(스키마·SQL) |
| `UserQnaControllerTest` | 6 | 익명 등록 PUBLIC 강제·sanitize·이중등록·내부글 | 중간(URL·메시지·세션) |
| `MemoControllerTest` | 2 | 메모보고 상태 전이·이중등록·필수값 | 중간(URL·메시지·세션) |
| `PopupControllerTest` | 2 | 팝업 게시기간·사용여부·날짜 역전·이중등록 | 중간(URL·메시지·세션) |
| `BannerControllerTest` | 2 | 배너 노출위치·정렬·게시중 필터 | 중간(URL·메시지·세션) |
| `ScheduleControllerTest` | 3 | 일정 유형·일자 필터·공휴일 병합·삭제 | 중간(URL·메시지·세션) |
| `AddressControllerTest` | 2 | 연락처 암호화·마스킹·평문 복원·이중등록·이름 필수 | 중간(URL·암호화) |
| `CardControllerTest` | 2 | 명함 공개/내 명함·사용등록·이중등록·필수값 | 중간(URL·메시지·세션) |
| `CounselControllerTest` | 2 | 상담 비밀번호 확인·답변·등록·첨부 필수값 | 중간(URL·메시지·세션) |
| `PollControllerTest` | 2 | POLL 세션당 1회 투표·항목 초기화·투표기간 | 중간(URL·메시지·세션) |
| `DutyControllerTest` | 3 | 당직 저장·이중등록·엑셀 양식·업로드·다운로드 마스킹 | 중간(URL·엑셀) |
| `CommCodeControllerTest` | 2 | 공통코드 마스터·상세·연쇄 삭제·필수값 | 중간(URL·메시지·세션) |
| `UserManageControllerTest` | 4 | 사용자 관리 CRUD·기본 역할·잠금 해제·임시 비밀번호 | 중간(URL·메시지·세션) |
| `AuditInterceptorIntegrationTest` | 4 | 인터셉터→이벤트→AUDIT_LOG 적재, PII_VIEW·PII_DOWNLOAD | 중간(세션·인터셉터) |
| `AuditLogControllerTest` | 2 | 감사 로그 조회·엑셀 다운로드 | 중간(URL·세션) |
| `AuditLogServiceTest` | 2 | 감사 로그 서비스·보관 정리 | 낮음(Mockito) |
| `MaskingUtilTest` | 4 | 마스킹 규칙 | 낮음(거의 그대로) |
| `FieldCryptoServiceTest` | 6 | AES-GCM 암복호화·키 미설정 거부·이중 암호화 방지 | 낮음(거의 그대로) |
| `PiiFieldServiceTest` | 6 | PII 필드 서비스·평문 통과 | 낮음(거의 그대로) |
| `OauthControllerTest` | 3 | OAuth 미지원·미설정·잘못된 state 거부 | 중간(URL·세션) |
| `ApiControllerTest` | 2 | `/api/**` JWT 토큰 검증 | 낮음~중간(URL·JWT) |
| `InternalApiControllerTest` | 3 | 내부 API 토큰 401·health UP·개인정보 미포함 | 낮음~중간(URL·토큰) |
| `TotpServiceTest` | 4 | TOTP 설정·복구코드 1회 사용·초기화·잘못된 코드 | 낮음~중간(Mockito) |
| `LoginTotpFlowTest` | 1 | 로그인 후 TOTP 요구·5회 실패 잠금 | 중간(세션·로그인) |
| `TermsInterceptorTest` | 4 | 약관 미동의 시 리다이렉트 | 중간(세션·인터셉터) |
| `TermsServiceTest` | 4 | 약관 버전·동의 이력 | 낮음(Mockito) |
| `SignupControllerTest` | 5 | 회원가입·아이디 찾기 마스킹·비밀번호 재설정 토큰 | 중간(URL·메시지·세션) |
| `StatsControllerTest` | 2 | 통계 집계·역순 기간 보정·엑셀 다운로드 | 중간(URL·세션) |
| `AnalyticsModelAdviceTest` | 5 | GA4/Clarity ID 미설정·형식 오류·동의값 | 낮음(Mockito) |
| `AligoSmsSenderTest` | 7 | 알리고 비활성·자격증명·LMS 전환·실패 응답 | 낮음~중간(Mockito) |
| `SmsServiceImplTest` | 5 | SMS 수신 대상 필터·중복 발송 차단 | 낮음~중간(Mockito) |
| `NiceVerifyControllerTest` | 2 | NICE 미설정 시 5xx, 미인증 상태 | 중간(URL·세션) |
| `KoreanSearchUtilTest` | 3 | 한글 초성 검색 유틸 | 낮음(거의 그대로) |
| `LeaveControllerTest` | 7 | 휴가 신청·승인·차감·환원·잔여 초과 금지 | 중간(URL·메시지·세션) |
| `LeaveDaysCalculatorTest` | 5 | 평일 일수·반차 0.5일 계산 | 낮음(거의 그대로) |
| `MeetingControllerTest` | 5 | 회의실 CRUD·예약 시간 중복·역전·취소 | 중간(URL·메시지·세션) |
| `ApprovalControllerTest` | 5 | 약식결재 기안→상신→승인/반려·결재자 확인·이력 | 중간(URL·메시지·세션) |
| `HrControllerTest` | 4 | HR 기념일·경조사·포상·개인연차 | 중간(URL·메시지·세션) |
| `SurveyControllerTest` | 5 | 설문 템플릿·질문·항목·참여·통계 | 중간(URL·메시지·세션) |
| `LectureControllerTest` | 2 | 강의 시청 위치·수료 판정·이어보기·스트리밍 | 중간(URL·메시지·세션) |
| `FfmpegVideoEncodingServiceTest` | 7 | FFmpeg 비활성 mp4 저장·확장자/길이/크기 거부 | 낮음~중간(Mockito) |
| `MapControllerTest` | 2 | 약도 CRUD·사용여부·좌표 검증·이중등록 | 중간(URL·메시지·세션) |
| `SsoControllerTest` | 1 | SSO 비활성 시 404 | 중간(URL·세션) |
| `ChatWebSocketTest` | 6 | WebSocket 세션·권한·Origin·이력·브로드캐스트 | 높음(WebSocket) |
| `BatchControllerTest` | 2 | 일일 통계 집계·멱등 재집계·날짜·토큰 검증 | 중간(URL·메시지·세션) |

JaCoCo 게이트는 전체가 아니라 `HtmlSanitizer`, `FileTypeUtil`, `JwtUtil`, `AuthInterceptor`, `MessageController`, `MessageServiceImpl`, `BoardServiceImpl`, `MailServiceImpl`, `BoardNotificationEventListener`, `SearchServiceImpl`만 강제한다. 클래스별 임계값은 `pom.xml`이 원본이다.

## 3. 실행

```cmd
verify.cmd
rem 단독 실행 예: mvn -Dtest=BoardMapperTest test
```

- 테스트 DB는 `target/test-db`, 포트 9002를 써서 앱(9001) 실행 중에도 돈다.
- `@ArkiveIntegrationTest`는 `@SpringBootTest` + `@AutoConfigureMockMvc` + 테스트 properties를 묶은 합성 애노테이션이다.
  통합 테스트마다 직접 `@SpringBootTest`를 붙이면 설정 차이로 컨텍스트가 갈라져 같은 DB 포트를 두 번 열 수 있다.
- `src/test/resources/application.properties`를 두지 않는다. main 설정 전체가 가려진다.
