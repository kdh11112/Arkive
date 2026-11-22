# 테스트 스택 이식 (Boot용)

> Boot 대상의 테스트 준비 가이드다. 맞춤 작업은 스펙 담당자가 별도로 수행하며 구현 에이전트의 테스트 수정 금지는 대상 루트 `AGENTS.md`를 따른다. 아래 예제도 수정 권한을 부여하지 않는다.
> 트랙 선택은 [이식 가이드](../../../PORTING.md), 준비·검증 기록은 [테스트 이식 시트](../portable/test-porting-sheet.md)를 따른다.

Arkive의 최신 테스트 수치와 전체 클래스 목록은 원본의 `src/test/README.md`에서만 관리한다. 이 복사용 가이드에는 현황 사본을 두지 않는다.

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
@ContextConfiguration(initializers = PostgresTestDatabaseInitializer.class)
public @interface ArkiveIntegrationTest {
}
```

대상 프로젝트에서는 `IntegrationTest`로 이름을 바꾸고 `classes`를 대상 앱 클래스로 바꾼다. 초기화기와 테스트 DB 의존성은 대상 DB에 맞춰 스펙 담당자가 별도로 준비한다.

테스트 설정은 `application-test.properties`로 분리하고 실제 값은 원본의 `src/test/resources/application-test.properties.template`을 참고한다. Arkive는 `PostgresTestDatabaseInitializer`가 컨텍스트 시작 전에 로컬 PostgreSQL을 기동하고 `spring.datasource.*`를 격리 접속 정보로 덮어쓴다. 초기화기의 `embedded-postgres` 테스트 전용 의존성과 바이너리 버전은 원본 `pom.xml`에서 확인한다.

주의:

- `src/test/resources/application.properties`를 두면 main 설정 전체가 가려진다. 키가 다른 별도 파일 + `@TestPropertySource`를 쓴다.
- Arkive 테스트 DB는 `target/test-postgres/<UUID>`·자동 선택 포트를 사용하며 실제 Supabase와 분리된다. Docker·별도 PostgreSQL 설치는 필요 없다.
- 초기화기는 새 테스트 DB에 `db/postgres/schema.sql` → `db/postgres/seed.sql`을 한 번 적용한다. 테스트 JVM 안에서 DB를 공유하며 `spring.sql.init.mode=never`로 Boot의 중복 적용을 막는다. 대상은 해당 프로젝트의 마이그레이션·스키마를 재사용한다.
- 테스트에서는 메일·SMS 등 외부 발송과 예약 작업을 비활성화한다. 운영 접속 정보·비밀값을 테스트 설정에 넣지 않는다.
- 여러 통합 테스트가 서로 다른 `@SpringBootTest` 설정을 쓰면 컨텍스트가 갈라져 같은 DB 포트를 두 번 열 수 있다. 공통 합성 애노테이션으로 설정을 통일한다.

## 2. JaCoCo (핵심 클래스 한정)

전체 LOC에 60%를 한 번에 강제하지 않는다. **검증된 클래스부터** 좁게 시작하고, 테스트를 추가할 때마다 `includes`를 늘린다.
아래는 최소 예시다. Arkive의 실제 클래스별 룰은 원본 `pom.xml`과 `src/test/README.md`에서 확인한다. 대상 클래스·임계값은 승인된 기준에 맞게 별도로 준비한다.

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
- 대상 DB에 맞는 격리 인스턴스 또는 Testcontainers로 Mapper/SQL을 검증한다. 다른 DB 제품의 테스트 결과만으로 이식 완료를 판단하지 않는다.

## 이식 체크리스트

- [ ] DB 접속·시크릿을 변수·프로파일로 분리했는가 (운영값 하드코딩 금지)
- [ ] 테스트 DB 포트·경로가 앱 실행과 겹치지 않는가
- [ ] 운영 마이그레이션 SQL을 테스트 스키마로 재사용하는가 (중복 정의 금지)
- [ ] 통합 테스트가 같은 Spring 컨텍스트를 공유하는가 (포트 중복 기동 방지)
- [ ] feature 파일이 인간 승인 후 고정되었는가 (구현 에이전트 수정 금지)
- [ ] 커버리지는 분기 기준·핵심 클래스 한정인가 (전체 100% 목표 금지)
- [ ] 실패한 게이트가 머지를 막는가 (경고만 띄우고 통과 금지)
