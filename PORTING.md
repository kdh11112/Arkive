# Arkive 이식 가이드 (복붙용)

> 이 문서는 대상에도 복사하는 실행 매뉴얼이다. Arkive 소개·기능 현황은 원본의 `README.md`에서 확인한다(대상에는 복사하지 않아도 된다).
> 이 문서만 보고 이식할 수 있게 만든 실행 매뉴얼이다. 세부 기준은 링크된 문서를 본다.
> 원칙: **Arkive를 먼저 실행해 기능을 확인하고, 확인한 기능만 복사한다.**

## 0. 결론: 복사할 것과 복사하지 않을 것

| 구분 | 복사 | 이유 |
| --- | --- | --- |
| `docs/ai/common/` 5종 | **예** | 코딩·SQL·테스트·검증·마이그레이션 공통 방법 |
| `docs/ai/portable/` | **예** | 빈 양식·스크립트·기능 이식 시트 |
| `docs/ai/boot/` 또는 `docs/ai/classic/` | **택 1** | 대상이 Spring Boot인지 전자정부 클래식인지 |
| `docs/ai/project/` | **아니오** | Arkive 기록이다. 대상의 기존 지식 또는 [빈 양식](docs/ai/portable/project-template-pack.md)을 사용한다 |
| `AGENTS.md`, `PORTING.md` | **예** | 프로젝트명·패키지·DB만 대상에 맞게 수정 |
| `verify.cmd`, `doctor.cmd` | **예** | 없으면 [scripts.md](docs/ai/portable/scripts.md) 전문으로 생성 |
| `.github/workflows/verify.yml` | **예 (GitHub 사용 시)** | CI 게이트. GitLab/Jenkins면 대상 CI로 옮긴다 |
| `README.md`, `docs/ai/README.md`, `src/test/README.md` | **아니오** | Arkive 전용 소개·문서 지도·테스트 현황. 원본에서 참고한다 |
| `.github/modernize/`, `data/`, `target/`, `backup/` | **아니오** | 도구 로그·로컬 DB·빌드 산출물 |

대상에 없는 반대 트랙·Arkive 전용 문서는 링크 대신 참고 경로로 표기한다. 대상의 기존 파일과 기록은 임의로 덮어쓰거나 삭제하지 않는다.

## 1. 5분 복사 (Windows)

```cmd
set "TARGET=C:\work\my-project"

rem 기존 파일은 보존하고 누락 파일만 복사한다. 수정본 통합은 별도로 비교한다.
robocopy docs\ai\common   "%TARGET%\docs\ai\common"   /E /XC /XN /XO
robocopy docs\ai\portable "%TARGET%\docs\ai\portable" /E /XC /XN /XO
robocopy docs\ai\boot     "%TARGET%\docs\ai\boot"     /E /XC /XN /XO
rem 클래식이면 위 줄 대신: robocopy docs\ai\classic "%TARGET%\docs\ai\classic" /E /XC /XN /XO
rem GitHub 사용 시에만 실행한다.
robocopy .github\workflows "%TARGET%\.github\workflows" /E /XC /XN /XO

if not exist "%TARGET%\AGENTS.md" copy AGENTS.md "%TARGET%\AGENTS.md"
if not exist "%TARGET%\PORTING.md" copy PORTING.md "%TARGET%\PORTING.md"
if not exist "%TARGET%\verify.cmd" copy verify.cmd "%TARGET%\verify.cmd"
if not exist "%TARGET%\doctor.cmd" copy doctor.cmd "%TARGET%\doctor.cmd"
if not exist "%TARGET%\docs\ai\project" mkdir "%TARGET%\docs\ai\project"

cd /d "%TARGET%"
rem 누락 항목을 알려준다. 2번에서 보완한 뒤 다시 실행한다.
doctor.cmd
```

- `robocopy`는 exit code 0~7이 정상이다. 8 이상이면 실패다.
- `AGENTS.md`의 `Arkive`·`arkive.*`·예시 테이블명을 대상 값으로 바꾼다.
- 대상의 기존 문서는 비교·통합한다. 반대 트랙을 새로 복사하지 않으며, 기존 사용자 문서는 임의 삭제하지 않는다.
- 대상의 프로젝트 지식은 한 파일로 시작할 수 있다. 아래 준비 단계에서 실제 값과 확인 상태를 채운다.

## 2. 대상 프로젝트에서 먼저 할 일

1. `doctor.cmd`로 빠진 항목(테스트 스타터, JaCoCo, 테스트 DB 격리, 회귀 체크리스트)을 확인한다.
2. `pom.xml`에 `spring-boot-starter-test`(클래식은 `spring-test`)와 JaCoCo를 넣는다.
3. `src/test/resources/application-test.properties`에 **대상 DB와 다른 테스트 DB 경로/포트**를 둔다.
4. 스펙 담당자가 [테스트 이식 시트](docs/ai/portable/test-porting-sheet.md)로 테스트 팩을 별도 준비한다. 공통 합성 애노테이션으로 컨텍스트를 통일하고 테스트 DB 오버라이드를 적용한다. 구현 에이전트는 테스트 파일을 만들거나 수정하지 않고 필요한 맞춤 항목을 보고한다.
5. `verify.cmd`가 실제로 `mvn clean verify`를 돌리고 실패 시 exit 1을 반환하는지 확인한다.
6. 대상의 기존 `docs/ai/project/` 기록을 재사용한다. 기록이 없으면 [빈 양식](docs/ai/portable/project-template-pack.md)으로 `project-context.md` 한 파일을 만들고 실제 값(패키지·DB·URL·메뉴)을 채운다. 필요할 때만 주제별로 분리한다.

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
→ Mapper 인터페이스 → Mapper XML → schema.sql의 필요한 DDL → 설정 키 → pom 의존성
→ seed.sql의 메뉴·역할·권한 등 필수값 → 스펙 담당자의 별도 테스트 준비 → verify.cmd
```

1. **Arkive에서 먼저 실행한다.** 로그인·권한·정상/오류 케이스를 화면과 API로 확인한다.
2. 화면(Thymeleaf/JSP)과 화면 전용 JavaScript를 복사한다.
3. Controller → Service/Impl → Mapper 인터페이스 → Mapper XML 순서로 복사한다.
4. `src/main/resources/db/schema.sql`에서 해당 기능의 테이블·컬럼·제약조건·인덱스를, `db/seed.sql`에서 메뉴·역할·권한 등 필요한 초기값을 골라 대상 DB 문법으로 옮긴다. 기존 DB에는 전체 CREATE/시드를 다시 실행하지 않고 대상의 승인된 갱신 절차를 따른다. 분류 기준은 [SQL 표준](docs/ai/common/sql-standard.md#db-변경을-어느-파일에-넣을지)을 참고한다.
5. `application.properties`의 `Globals.*` 키와 `pom.xml` 의존성을 확인한다.
6. 관련 테스트의 복사 대상·맞춤 항목을 보고한다. 스펙 담당자가 테스트를 별도 준비·승인한 뒤 제공된 테스트로 검증한다. 테스트가 없거나 준비가 끝나지 않으면 이식을 완료로 보고하지 않는다.
7. 대상에서 `verify.cmd`를 실행하고, 화면·API·DB를 다시 확인한다.

Arkive의 HSQLDB 자동 시작과 `HsqlDemoDatabaseInitializer`·`ARKIVE_DB_INIT`은 **로컬 데모 인프라**다. 기존 DB가 있는 대상에는 복사하지 않고 대상 DataSource·마이그레이션 방식을 유지한다. 빈 DB 초기화용 최신 정의는 기존 DB를 자동 업그레이드하는 SQL이 아니다. 개발용 admin과 약관 자리표시자는 실제 서비스에 그대로 가져가지 않는다.

### 3.1 테스트 준비의 책임

분류·공통 팩·맞춤 항목은 [테스트 이식 시트](docs/ai/portable/test-porting-sheet.md) 한 곳에서 관리한다. Arkive의 실제 테스트 목록은 원본의 `src/test/README.md`에서 찾아 필요한 기능만 선택한다.

테스트의 기대 결과·assertion 변경은 인간 승인 후 스펙 담당자의 별도 작업이다. 구현 에이전트의 테스트 수정 금지와 실패 시 코드만 수정하는 원칙은 대상 루트 `AGENTS.md`를 따른다.

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
| DB 연결·초기화 | Arkive 데모 HSQLDB 자동 시작, 빈 DB에 `schema.sql` → `seed.sql` 1회 | 기존 DB 대상은 대상 DataSource·갱신 절차 사용. 데모 서버/초기화기는 제외 |
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
- 화면/CSS는 13.3인치 노트북을 필수 확인하고, 그 밖의 화면은 해상도·배율·창 크기에 유동 대응하는지 확인한다. [화면 크기 판정](docs/ai/common/test-standard.md#화면-크기-확인)에서 실기기와 대체 viewport 검사를 구분한다.

## 6. 적용 트랙과 기록

| 목적 | 문서 |
| --- | --- |
| 이식 실행 | 이 문서, [feature-porting-sheet.md](docs/ai/portable/feature-porting-sheet.md) |
| 테스트 이식 | [test-porting-sheet.md](docs/ai/portable/test-porting-sheet.md) |
| AI에게 이식 요청 | [ai-porting-request.md](docs/ai/portable/ai-porting-request.md) |
| 대상 프로젝트 지식 작성 | [project-template-pack.md](docs/ai/portable/project-template-pack.md) |
| 공통 방법 | [docs/ai/common/](docs/ai/common/) |
| Boot 적용 | `docs/ai/boot/boot-apply.md`, `docs/ai/boot/test-stack-boot.md` |
| 클래식 적용 | `docs/ai/classic/classic-apply.md`, `docs/ai/classic/test-stack-classic.md` |
| 대상의 실제 구현 기록 | [docs/ai/project/](docs/ai/project/) — 대상에서 작성, Arkive 기록 복사 금지 |

트랙 경로는 선택표다. 대상에는 둘 중 한 트랙만 존재하므로 반대 트랙에 무조건 연결되는 링크를 두지 않는다. Arkive 원본 전체 목차는 `docs/ai/README.md`에서 확인한다.

## 7. 하지 말 것

- `docs/ai/project/`의 Arkive 예시를 대상 프로젝트에 그대로 복사하지 않는다.
- `data/`, `target/`, `backup/`, `.lck` 파일을 복사하지 않는다.
- 비밀값·운영 DB 접속 정보를 복사하거나 문서에 적지 않는다.
- 테스트를 삭제·skip하거나, 커버리지 숫자만 맞추려고 빈 테스트를 추가하지 않는다.
- HSQLDB에서만 동작하는 SQL을 대상 DB에서 확인 없이 그대로 쓰지 않는다.

## 8. AI 요청

[AI 이식 요청](docs/ai/portable/ai-porting-request.md)에 배치·복사 여부·복사용 프롬프트를 모아둔다. Arkive와 대상은 형제 폴더에 두고, 대상에서 세션을 시작해 **기능 하나씩** 요청한다.
