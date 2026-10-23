# Arkive 이식 가이드 (복붙용)

> 이 문서는 이식 실행만 다룬다. 프로젝트 소개·실행·기능 로드맵은 [README.md](README.md)를 본다.
> 이 문서만 보고 이식할 수 있게 만든 실행 매뉴얼이다. 세부 기준은 링크된 문서를 본다.
> 원칙: **Arkive를 먼저 실행해 기능을 확인하고, 확인한 기능만 복사한다.**

## 0. 결론: 복사할 것과 복사하지 않을 것

| 구분 | 복사 | 이유 |
| --- | --- | --- |
| `docs/ai/common/` 5종 | **예** | 코딩·SQL·테스트·검증·마이그레이션 공통 방법 |
| `docs/ai/portable/` | **예** | 빈 양식·스크립트·기능 이식 시트 |
| `docs/ai/boot/` 또는 `docs/ai/classic/` | **택 1** | 대상이 Spring Boot인지 전자정부 클래식인지 |
| `docs/ai/project/` 9종 | **아니오** | Arkive의 현재 지식 기록이다. 대상에는 [project-template-pack.md](docs/ai/portable/project-template-pack.md)만 가져간다 |
| `AGENTS.md`, `PORTING.md` | **예** | 프로젝트명·패키지·DB만 대상에 맞게 수정 |
| `verify.cmd`, `doctor.cmd` | **예** | 없으면 [scripts.md](docs/ai/portable/scripts.md) 전문으로 생성 |
| `.github/workflows/verify.yml` | **예 (GitHub 사용 시)** | CI 게이트. GitLab/Jenkins면 대상 CI로 옮긴다 |
| `README.md` | 선택 | Arkive 소개·로드맵이라 대상에는 보통 불필요 |
| `.github/modernize/`, `data/`, `target/`, `backup/` | **아니오** | 도구 로그·로컬 DB·빌드 산출물 |

핵심은 **`docs/ai/project/`는 복사 대상이 아니라 Arkive 기록**이라는 점이다.
대상 프로젝트의 프로젝트 지식은 빈 템플릿 팩으로 새로 채운다.

## 1. 5분 복사 (Windows)

```cmd
set TARGET=C:\work\my-project

robocopy docs\ai\common   "%TARGET%\docs\ai\common"   /E
robocopy docs\ai\portable "%TARGET%\docs\ai\portable" /E
robocopy docs\ai\boot     "%TARGET%\docs\ai\boot"     /E
rem 클래식이면 위 줄 대신: robocopy docs\ai\classic "%TARGET%\docs\ai\classic" /E
robocopy .github\workflows "%TARGET%\.github\workflows" /E

copy AGENTS.md  "%TARGET%\AGENTS.md"
copy PORTING.md "%TARGET%\PORTING.md"
copy verify.cmd "%TARGET%\verify.cmd"
copy doctor.cmd "%TARGET%\doctor.cmd"

cd /d "%TARGET%"
rem 누락 항목을 알려준다. 2번에서 보완한 뒤 다시 실행한다.
doctor.cmd
```

- `robocopy`는 exit code 0~7이 정상이다. 8 이상이면 실패다.
- `AGENTS.md`의 `Arkive`·`arkive.*`·예시 테이블명을 대상 값으로 바꾼다.
- `docs/ai/project/`는 복사하지 않는다. 필요하면 `project-template-pack.md`를 복사해
  `docs/ai/project/` 아래로 나눠 저장한다.

## 2. 대상 프로젝트에서 먼저 할 일

1. `doctor.cmd`로 빠진 항목(테스트 스타터, JaCoCo, 테스트 DB 격리, 회귀 체크리스트)을 확인한다.
2. `pom.xml`에 `spring-boot-starter-test`(클래식은 `spring-test`)와 JaCoCo를 넣는다.
3. `src/test/resources/application-test.properties`에 **대상 DB와 다른 테스트 DB 경로/포트**를 둔다.
4. `src/test/java/arkive/support/ArkiveIntegrationTest.java`와 `TestSessions.java`를 대상 패키지·진입 클래스로 바꿔 복사한다.
   `ArkiveSmokeTest`는 복사하지 말고 대상 프로젝트에서 새로 만든다.
   `src/test/resources/application-test.properties.template`은 대상 DB에 맞게 채워 `application-test.properties`로 저장한다.
   통합 테스트는 이 합성 애노테이션을 공유해 테스트 DB 서버가 중복 기동되지 않게 한다.
5. `verify.cmd`가 실제로 `mvn clean verify`를 돌리고 실패 시 exit 1을 반환하는지 확인한다.
6. [project-template-pack.md](docs/ai/portable/project-template-pack.md)를 대상 프로젝트의 `docs/ai/project/`로 나눠 저장하고, 실제 값(패키지·DB·URL·메뉴)을 채운다.

## 3. 기능 하나를 이식하는 순서

기능별로 [feature-porting-sheet.md](docs/ai/portable/feature-porting-sheet.md)를 한 장씩 채운다.

먼저 Arkive를 실행한다.

```cmd
mvn spring-boot:run
rem 또는 저장소 루트에서: java -jar target\Arkive-1.0.0.jar
```

브라우저에서 `http://localhost:8080`을 열고, 이식할 기능의 화면·API·권한을 확인한 뒤 시트에 기록한다.
테스트는 앱이 9001을 쓰는 중에도 9002 포트로 돌 수 있다.

```text
Arkive 실행 → 기능 동작 확인 → 화면/JS 복사 → Controller → Service/Impl
→ Mapper 인터페이스 → Mapper XML → db/versions SQL → 설정 키 → pom 의존성
→ 메뉴 seed → 테스트 복사/수정 → verify.cmd
```

1. **Arkive에서 먼저 실행한다.** 로그인·권한·정상/오류 케이스를 화면과 API로 확인한다.
2. 화면(Thymeleaf/JSP)과 화면 전용 JavaScript를 복사한다.
3. Controller → Service/Impl → Mapper 인터페이스 → Mapper XML 순서로 복사한다.
4. `db/versions/V*__*.sql`에서 해당 테이블·메뉴 seed를 찾아 대상 DB 문법으로 옮긴다.
5. `application.properties`의 `Globals.*` 키와 `pom.xml` 의존성을 확인한다.
6. 관련 테스트를 함께 복사한다. 테스트가 없으면 이식 전에 먼저 만든다.
7. 대상에서 `verify.cmd`를 실행하고, 화면·API·DB를 다시 확인한다.

### 3.1 테스트 복사 매트릭스

테스트는 통째로 복사하지 않는다. 공통 팩과 기능 테스트를 나눈다.

| 테스트 자산 | 복사 | 대상에서 바꿀 것 |
| --- | --- | --- |
| `support/ArkiveIntegrationTest.java` | **예** | `IntegrationTest`로 이름 변경, 대상 앱 클래스·패키지 |
| `support/TestSessions.java` | **예** | 세션 키, 사용자 객체 형태 |
| `application-test.properties.template` | **예** | DB 제품·URL·경로·포트 |
| `JwtUtilTest`, `FileTypeUtilTest`, `HtmlSanitizerTest` | **예** | 패키지·import 정도 |
| `AuthInterceptorTest` | **수정 복사** | 세션 키, 공개 경로, 리다이렉트 URL |
| `ErrorResponseTest` | **수정 복사** | 전역 예외 resolver·오류 view·health 설정, 미매핑 URL·정적 리소스 경로 |
| `BoardCommentControllerTest`, `MessageControllerTest` | **기능과 함께** | URL, 응답 메시지, 세션, seed 데이터 |
| `BoardNotificationEventListenerTest`, `BoardNotificationControllerTest` | **기능과 함께** | 이벤트 클래스·리스너, 게시판 메뉴 ID, API, `@RecordApplicationEvents` |
| `SitemapControllerTest` | **기능과 함께** | `MenuInterceptor`의 `menuList`, 세션 키, 메뉴 seed, 사이트맵 URL |
| `SearchMapperTest` | **기능과 함께** | `BOARD`·`BOARD_TYPE` 값, `LIMIT/OFFSET` 문법, 결과 정렬 |
| `SearchServiceImplTest`, `SearchControllerTest` | **기능과 함께** | 검색 서비스·컨트롤러, 결과 링크 URL, 로그인 세션, 빈 검색어 문구 |
| `BoardMapperTest`, `MessageMapperTest` | **기능과 함께** | 테이블·컬럼·PK, SQL 문법, seed 데이터 |
| `LoginSessionRegistryTest`, `LoginSessionControllerTest` | **기능과 함께** | 세션 키, 로그인 URL, seed 계정, 단일/다중 인스턴스 세션 정책 |
| `PermissionMapperTest`, `RoleManageControllerTest`, `MenuPermissionControllerTest` | **기능과 함께** | 역할·메뉴 ID, 관리자 판정, 403 응답, 사이드바 메뉴, seed 역할 |
| `MailServiceImplTest` | **수정 복사** | `JavaMailSender` 목, 수신 동의·1회 선점·SMTP 실패, 메일 제목·본문·URL |
| `PasswordResetControllerTest` | **기능과 함께** | 토큰 테이블·컬럼, 만료·쿨다운·1회 사용, 응답 메시지, seed 사용자, 테스트 메일 비활성 |
| `ArkiveSmokeTest` | **아니오** | 대상에서 새로 작성 |
| `pom.xml` JaCoCo `includes` | **예** | 대상 클래스명으로 교체 |

- 테스트 시나리오는 복사하되, assertion은 대상 업무 규칙에 맞게 다시 확인한다.
- 기능 코드와 테스트를 같은 작업 단위에 넣는다.
- 상세 체크는 [test-porting-sheet.md](docs/ai/portable/test-porting-sheet.md)에 기록한다.

## 4. 복사 후 반드시 바꿀 것

| 항목 | Arkive 값 | 대상에서 확인할 것 |
| --- | --- | --- |
| 패키지 | `arkive.*`, `egovframework.example.*` | 대상 패키지·컴포넌트 스캔 |
| 테이블 | `BOARD`, `MESSAGE`, `APP_USER` 등 | 대상 테이블명·스키마·대소문자 |
| DB 문법 | HSQLDB `LIMIT/OFFSET`, `CURRENT_TIMESTAMP`, `MERGE` | Oracle/PostgreSQL/MySQL 문법 |
| 메뉴 ID | `ME0208xxxx` 등 | 대상 메뉴 체계·중복 ID |
| 인증 | 세션 `LOGIN_USER`, `ADMIN_USER` | 대상 인증·권한 방식 |
| 파일 경로 | `Globals.FILE_*`, `Globals.FFMPEG_PATH` | 대상 서버 경로·권한 |
| 메일 설정 | `spring.mail.*`, `arkive.mail.enabled/from/base-url`, `MAIL_*` 환경변수 | 대상 SMTP·발신자·base URL. 테스트는 `arkive.mail.enabled=false` |
| 비동기 실행 | `EgovConfigAsync`의 `mailTaskExecutor` | 대상 Executor·스레드 풀·종료 정책 |
| 전역 오류·헬스 | `EgovConfigWeb`의 404/500 예외 매핑, `management.health.mail.enabled` | 오류 view·상태 코드 계약, 메일 헬스 활성화 여부 |
| DB 포트 | `Globals.hsql_server.Port=9001` | `Globals.hsql_server.Url`의 포트와 일치 |
| 테스트 DB | `target/test-db`, 9002 | 앱 DB와 경로·포트 분리 |
| 테스트 세션 | `LOGIN_USER`, `ADMIN_USER`, `EgovMap` | 대상 세션 키·사용자 모델 |
| 테스트 URL·메시지 | `/board/...`, `"게시글이 없습니다."` | 대상 API 경로·응답 계약 |
| 테스트 스키마 | `BOARD`, `MESSAGE`, `APP_USER` | 대상 테이블·컬럼·PK·seed |
| JaCoCo 게이트 | `pom.xml`의 Arkive 클래스 `includes` | 대상 클래스로 교체. 게이트 자체를 비우지 않는다 |
| 화면 경로 | `file:./src/main/resources/templates/thymeleaf/` | 대상 템플릿/JSP 경로 |
| 비밀값 | SMS/JWT/NICE/OAuth/`MAIL_PASSWORD`/`private.key` | **그대로 복사 금지.** 환경변수·시크릿 저장소 |

`application.properties`에는 개발용 키·비밀값이 들어 있을 수 있다.
문서·로그·Git에 값을 남기지 말고, 대상 환경의 환경변수나 시크릿 저장소로 주입한다.

## 5. 검증

```cmd
verify.cmd
```

- 빌드·테스트·JaCoCo가 한 번에 돌아야 한다.
- 테스트 DB는 운영 DB와 분리하고, 가능하면 대상 DB와 같은 제품으로 검증한다.
- HSQLDB 테스트만 통과했다고 Oracle/PostgreSQL 이식을 완료했다고 말하지 않는다.
- 화면·API·DB 확인 결과는 [evidence-report-template.md](docs/ai/portable/evidence-report-template.md)에 남긴다.

## 6. 문서 지도

| 목적 | 문서 |
| --- | --- |
| 이식 실행 | 이 문서, [feature-porting-sheet.md](docs/ai/portable/feature-porting-sheet.md) |
| 테스트 이식 | [test-porting-sheet.md](docs/ai/portable/test-porting-sheet.md) |
| AI에게 이식 요청 | [ai-porting-request.md](docs/ai/portable/ai-porting-request.md) |
| 대상 프로젝트 지식 작성 | [project-template-pack.md](docs/ai/portable/project-template-pack.md) |
| 공통 방법 | [docs/ai/common/](docs/ai/common/) |
| Boot 적용 | [boot-apply.md](docs/ai/boot/boot-apply.md), [test-stack-boot.md](docs/ai/boot/test-stack-boot.md) |
| 클래식 적용 | [classic-apply.md](docs/ai/classic/classic-apply.md), [test-stack-classic.md](docs/ai/classic/test-stack-classic.md) |
| Arkive의 실제 구현 기록 | [docs/ai/project/](docs/ai/project/) — **복사 금지** |

## 7. 하지 말 것

- `docs/ai/project/`의 Arkive 예시를 대상 프로젝트에 그대로 복사하지 않는다.
- `data/`, `target/`, `backup/`, `.lck` 파일을 복사하지 않는다.
- 비밀값·운영 DB 접속 정보를 복사하거나 문서에 적지 않는다.
- 테스트를 삭제·skip하거나, 커버리지 숫자만 맞추려고 빈 테스트를 추가하지 않는다.
- HSQLDB에서만 동작하는 SQL을 대상 DB에서 확인 없이 그대로 쓰지 않는다.

## 8. AI에게 이식 요청하는 방법

### 8.1 권장 배치

상세 따라하기와 복사용 프롬프트는 [ai-porting-request.md](docs/ai/portable/ai-porting-request.md)에 정리되어 있다.

Arkive와 대상 프로젝트는 **형제 폴더**에 두는 것을 권장한다.

```text
C:\work\
  Arkive\            ← 레퍼런스, 읽기 전용으로 취급
  target-project\    ← 실제 작업 디렉터리
```

AI 세션은 **대상 프로젝트에서 시작**하고, 프롬프트에 Arkive의 절대 경로를 적는다.
대상 프로젝트 안에 Arkive를 복사하거나 커밋하지 않는다.

AI가 두 경로를 모두 읽을 수 있으면 MD를 수동으로 옮길 필요가 없다.
AI가 Arkive를 읽을 수 없는 환경이면 [1. 5분 복사](#1-5분-복사-windows)를 먼저 실행해
`common/`, `portable/`, Boot 또는 Classic 트랙, `AGENTS.md`, `PORTING.md`, `verify.cmd`만 대상에 복사한다.
`docs/ai/project/`는 복사하지 않는다.

### 8.2 요청 예시

한 번에 전체를 이식하지 말고 **기능 하나씩** 요청한다.

```text
Arkive 레퍼런스: C:\work\Arkive
대상 프로젝트: 현재 작업 디렉터리 (C:\work\target-project)

이번 이식 기능: 게시판 댓글

1. Arkive의 PORTING.md, feature-porting-sheet.md, test-porting-sheet.md를 먼저 읽어라.
2. Arkive를 실행해 댓글 등록·삭제·권한 동작을 확인하고, 대상과 다른 점을 표로 정리해라.
3. 대상의 패키지·테이블·DB·URL·세션 방식에 맞게 화면·Controller·Service·Mapper/XML·SQL을 옮겨라.
4. 테스트도 함께 옮기고, 대상 기준으로 URL·메시지·세션·seed를 수정해라.
5. 확인되지 않은 업무 규칙·권한·DB 의미는 추측하지 말고 "미확인"으로 남기고 질문해라.
6. 단독 테스트와 verify.cmd를 실행하고, 수정 파일·검증 결과·남은 위험을 보고해라.
```

### 8.3 요청 시 꼭 포함할 것

- Arkive 경로와 대상 프로젝트 경로
- 이식할 기능명과 범위(화면/API/DB/테스트 포함 여부)
- 대상 프로젝트의 패키지·DB 제품·인증 방식
- 대상에서 이미 다른 이름으로 존재하는 테이블·메뉴·URL
- 하지 말아야 할 것(예: `docs/ai/project/` 복사 금지, 비밀값 이전 금지)
- 완료 기준(`verify.cmd` 통과, 수동 화면 확인, 대상 DB 확인)
