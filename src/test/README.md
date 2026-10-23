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
| `MessageMapperTest` | 2 | 받은/보낸함, 읽음, 소프트 삭제, 검색 | 높음(스키마·SQL) |
| `MessageControllerTest` | 4 | 쪽지 인증·중복제출·검증·권한 | 중간(URL·메시지·세션) |
| `HtmlSanitizerTest` | 6 | XSS 허용/차단 | 낮음(거의 그대로) |
| `FileTypeUtilTest` | 5 | 매직바이트·확장자 위조 | 낮음(거의 그대로) |
| `JwtUtilTest` | 4 | JWT 발급·검증, 시크릿 불일치·만료·형식 오류 | 낮음(거의 그대로) |
| `LoginSessionRegistryTest` | 5 | 사용자별 세션 교체·만료, 사용자 전환, 세션 소멸 정리 | 낮음(세션 키만 변경) |
| `LoginSessionControllerTest` | 1 | 실제 로그인 2회 시 이전 세션 만료 | 중간(로그인 URL·seed 계정) |
| `AuthInterceptorTest` | 5 | 공개 경로, JSON 401, 화면 redirect, 관리자 경로 | 중간(세션·경로) |
| `PermissionMapperTest` | 4 | V5 역할 시드, 사용자 메뉴 권한 집계, 기본 사용자 역할의 시스템·업로드 메뉴 제외, SSE 대상 역할 조회 | 높음(스키마·SQL) |
| `RoleManageControllerTest` | 7 | 권한관리 화면, 역할·메뉴·사용자 저장, 필수값, 잘못된 사용자 배정 거부, 기본 역할 삭제 거부, 사용자 삭제 시 역할 매핑 정리 | 중간(URL·메시지·세션) |
| `MenuPermissionControllerTest` | 5 | 조회 전용/쓰기 역할, 역할 없음 거부, 관리자 우회, 사이드바 필터 | 중간(URL·메시지·세션) |
| `MailServiceImplTest` | 10 | 비활성·발신자 없음, 수신 동의, 환영/재설정/공지 메일, HTML escape, 1회 선점, SMTP 실패 무시 | 낮음~중간(Mockito·메일 본문) |
| `PasswordResetControllerTest` | 5 | 토큰 발급 조건·1회 사용·만료·입력 검증·쿨다운 | 중간(URL·메시지·DB) |

합계 89개. 전체 커버리지는 instruction 16.0%, branch 14.5%, line 16.8% (2026-10-23 `verify.cmd` 기준).
JaCoCo 게이트는 검증된 일부 클래스만 강제하며, 메일은 `MailServiceImpl`(branch 70%·line 80%), 게시판 알림은 `BoardNotificationEventListener`(branch 80%·line 90%)를 추가로 검사한다.

## 5. 실행

```cmd
verify.cmd
rem 단독 실행 예: mvn -Dtest=BoardMapperTest test
```

- 테스트 DB는 `target/test-db`, 포트 9002를 써서 앱(9001) 실행 중에도 돈다.
- `@ArkiveIntegrationTest`는 `@SpringBootTest` + `@AutoConfigureMockMvc` + 테스트 properties를 묶은 합성 애노테이션이다.
  통합 테스트마다 직접 `@SpringBootTest`를 붙이면 설정 차이로 컨텍스트가 갈라져 같은 DB 포트를 두 번 열 수 있다.
- `src/test/resources/application.properties`를 두지 않는다. main 설정 전체가 가려진다.
