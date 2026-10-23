# 테스트 스택 이식 (Boot용)

> 이식 구분: 복붙 가능(Boot). 공통 테스트 팩은 파일째 복사(패키지·진입 클래스·DB 값만 변경), 기능 테스트는 기능과 함께 수정 복사한다. 절차·판단은 가이드로 읽는다.
> 클래식(전자정부 3.x/4.x)은 [../classic/test-stack-classic.md](../classic/test-stack-classic.md)를 쓴다.

## 현황 (Arkive, 2026-10-24 실측)

- `src/test`에 테스트 클래스 25개, 테스트 105개가 있고 `verify.cmd`가 통과한다.
  - `ArkiveSmokeTest` 1 — 컨텍스트·MyBatis 배선
  - `BoardMapperTest` 3 — 게시판 CRUD, null content, 댓글·cascade
  - `BoardCommentControllerTest` 3 — 댓글 인증·검증·작성자/관리자 삭제
  - `BoardNoticeMapperTest` 2 — 공지 고정 정렬·해제·CK 외 거부
  - `BoardNoticeControllerTest` 4 — 공지 고정 인증·관리자 권한·입력 검증·목록 렌더링
  - `BoardNotificationEventListenerTest` 9 — CK/TOAST SSE 역할, 메일 요청 분기, 채널별 실패 격리
  - `BoardNotificationControllerTest` 4 — 게시글 저장·수정·공지 고정 시 알림 이벤트 발행
  - `MessageMapperTest` 2 — 받은/보낸함 분리, 읽음, 소프트 삭제, 검색
  - `MessageControllerTest` 4 — 쪽지 인증·중복제출·검증·함별 권한
  - `HtmlSanitizerTest` 6 — XSS 허용/차단
  - `FileTypeUtilTest` 5 — 매직바이트·확장자 위조
  - `JwtUtilTest` 4 — JWT 발급·검증, 시크릿 불일치·만료·형식 오류
  - `LoginSessionRegistryTest` 5 — 사용자별 세션 교체·만료, 사용자 전환, 세션 소멸 정리
  - `LoginSessionControllerTest` 1 — 실제 로그인 2회 시 이전 세션 만료
  - `AuthInterceptorTest` 5 — 공개 경로, JSON 401, 화면 redirect, 관리자 경로
  - `ErrorResponseTest` 3 — 미매핑 URL·정적 리소스 404, 메일 비활성 시 health UP
  - `PermissionMapperTest` 4 — V5 역할 시드, 사용자 메뉴 권한 집계, 기본 사용자 역할의 시스템·업로드 메뉴 제외, SSE 대상 역할 조회
  - `RoleManageControllerTest` 7 — 권한관리 화면, 역할 생성·메뉴·사용자 저장, 필수값, 잘못된 사용자 배정 거부, 기본 역할 삭제 거부, 사용자 삭제 시 역할 매핑 정리
  - `MenuPermissionControllerTest` 5 — 조회 전용/쓰기 역할, 역할 없음 거부, 관리자 우회, 사이드바 필터
  - `SitemapControllerTest` 3 — 로그인 필요, 관리자 메뉴 트리, 역할 없는 사용자의 빈 사이트맵
  - `SearchMapperTest` 3 — CK/TOAST/FAQ/QNA 제목 검색, 본문만 일치 제외, 페이징·건수
  - `SearchServiceImplTest` 3 — 유형 라벨, HTML 제거·요약, CLOB 변환
  - `SearchControllerTest` 4 — 로그인 필요, 결과 렌더, 빈 검색어·무결과
  - `MailServiceImplTest` 10 — 비활성·발신자 없음, 수신 동의, 환영/재설정/공지 메일, HTML escape, 1회 선점, SMTP 실패 무시
  - `PasswordResetControllerTest` 5 — 토큰 발급 조건·1회 사용·만료·입력 검증·쿨다운
- 전체 커버리지: instruction 16.5%, branch 15.0%, line 17.4% (2026-10-24 `verify.cmd` 기준). **아직 낮다.**
- JaCoCo `check`는 전체가 아니라 검증된 일부 클래스만 강제한다.
  (`HtmlSanitizer`, `FileTypeUtil`, `JwtUtil`, `AuthInterceptor`, `MessageController`, `MessageServiceImpl`, `BoardServiceImpl`, `MailServiceImpl`, `BoardNotificationEventListener`, `SearchServiceImpl`)
- GitHub Actions `.github/workflows/verify.yml`에서 `mvn -B clean verify`를 실행한다.
- 테스트 DB는 `target/test-db`, 포트 **9002**를 쓴다. 앱이 9001을 쓰는 중에도 테스트가 돈다.
- `ArkiveIntegrationTest` 합성 애노테이션으로 통합 테스트가 같은 Spring 컨텍스트를 공유한다.
- `TestSessions`가 로그인 세션 생성을 공통화한다. 대상에서는 세션 키·사용자 형태만 바꾼다.
- `application-test.properties.template`을 대상 DB에 맞게 채워 `application-test.properties`로 저장한다.
- 테스트별 복사·수정 체크는 [test-porting-sheet.md](../portable/test-porting-sheet.md)를 쓴다.

## 1. 최소 세트 (pom + 스모크 1개)

```xml
<!-- 이미 선언됨. 버전은 parent 상속 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

```java
// src/test/java/arkive/ArkiveSmokeTest.java
@ArkiveIntegrationTest
class ArkiveSmokeTest {
    @Autowired(required = false) SqlSessionFactory sqlSessionFactory;

    @Test
    void contextLoads() {
        assertThat(sqlSessionFactory).isNotNull();
    }
}
```

공통 설정은 합성 애노테이션 하나로 묶는다. 테스트 클래스마다 `@SpringBootTest`를 직접 붙이면 설정 차이로 컨텍스트가 갈라진다.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = EgovBootApplication.class)
@AutoConfigureMockMvc
@TestPropertySource("classpath:/application-test.properties")
public @interface ArkiveIntegrationTest {
}
```

대상 프로젝트에서는 `IntegrationTest`로 이름을 바꾸고 `classes`를 대상 앱 클래스로 바꾼다.

테스트 DB는 main 설정을 덮어쓰는 별도 파일로 분리한다.

```properties
# src/test/resources/application-test.properties
Globals.hsql_server.DatabasePath=./target/test-db/ArkiveDB
Globals.hsql_server.Port=9002
Globals.hsql_server.Url=jdbc:log4jdbc:hsqldb:hsql://127.0.0.1:9002/ArkiveDB
# 실제 SMTP로 나가지 않게 테스트에서는 메일을 끈다. 필요하면 테스트 클래스에서 목으로 대체한다.
arkive.mail.enabled=false
```

주의:

- `src/test/resources/application.properties`를 두면 main 설정 전체가 가려진다. 키가 다른 별도 파일 + `@TestPropertySource`를 쓴다.
- `Globals.hsql_server.Port`와 `Globals.hsql_server.Url`의 포트는 반드시 같아야 한다.
- 운영 `db/versions` 마이그레이션을 테스트에서도 그대로 재사용한다(스키마 단일 진실 공급원).
- 여러 통합 테스트가 서로 다른 `@SpringBootTest` 설정을 쓰면 컨텍스트가 갈라져 같은 DB 포트를 두 번 열 수 있다. 공통 합성 애노테이션으로 설정을 통일한다.

## 2. JaCoCo (핵심 클래스 한정)

전체 19k LOC에 60%를 한 번에 강제하지 않는다. **검증된 클래스부터** 좁게 시작하고, 테스트를 추가할 때마다 `includes`를 늘린다.
아래는 최소 예시다. Arkive `pom.xml`은 `HtmlSanitizer`·`FileTypeUtil`·`JwtUtil`·`AuthInterceptor`·`MessageController`·`MessageServiceImpl`·`BoardServiceImpl`·`MailServiceImpl`·`BoardNotificationEventListener`·`SearchServiceImpl`에 클래스별 룰을 둔다.

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report-on-verify</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>coverage-check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>CLASS</element>
                        <includes>
                            <include>arkive.admin.comm.web.HtmlSanitizer</include>
                            <include>arkive.admin.comm.web.FileTypeUtil</include>
                        </includes>
                        <limits><limit>
                            <counter>BRANCH</counter><value>COVEREDRATIO</value>
                            <minimum>0.75</minimum>
                        </limit></limits>
                    </rule>
                    <!-- 기능별 Service/Controller 룰을 필요할 때 추가한다. -->
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- 라인이 아니라 분기(BRANCH) 기준, 전체가 아니라 핵심 클래스 한정으로 시작한다.
- 임계값은 현재 실측보다 약간 낮게 잡아 **떨어지면 실패**하게 한다. 통과시키려고 기준을 0에 가깝게 낮추지 않는다.

## 3. CI 게이트 (GitHub Actions)

`.github/workflows/verify.yml`:

```yaml
name: verify
on:
  push:
  pull_request:
jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B clean verify
```

AGENTS.md 품질 관문의 실행 주체다. Secrets(DB URL·키)는 프로젝트마다 변수로만 교체한다.

## 4. 이후 단계

- Controller/API 테스트를 기능별로 넓힌다. 최소한 로그인·권한·파일 업로드·엑셀부터.
- Cucumber: feature 파일은 인간 승인 후 고정, 구현 에이전트의 테스트 파일 수정 금지(AGENTS.md 품질 관문 참조).
- PIT: 변경 모듈의 핵심 클래스 한정 + 야간 전체 실행. 전체 상시는 빌드가 길어진다.
- Testcontainers: 운영이 Oracle/PostgreSQL이면 HSQLDB 테스트는 거짓말이 되므로 실엔진 컨테이너로 Mapper 테스트를 돌린다.

## 이식 체크리스트

- [ ] DB 접속·시크릿을 변수·프로파일로 분리했는가 (운영값 하드코딩 금지)
- [ ] 테스트 DB 포트·경로가 앱 실행과 겹치지 않는가
- [ ] 운영 마이그레이션 SQL을 테스트 스키마로 재사용하는가 (중복 정의 금지)
- [ ] 통합 테스트가 같은 Spring 컨텍스트를 공유하는가 (포트 중복 기동 방지)
- [ ] feature 파일이 인간 승인 후 고정되었는가 (구현 에이전트 수정 금지)
- [ ] 커버리지는 분기 기준·핵심 클래스 한정인가 (전체 100% 목표 금지)
- [ ] 실패한 게이트가 머지를 막는가 (경고만 띄우고 통과 금지)
