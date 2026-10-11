# 테스트 스택 이식 (Boot용)

> 이식 구분: 복붙 가능(Boot). pom 조각·테스트 뼈대는 파일째 복사(패키지·버전만 변경), 절차·판단은 가이드로 읽는다.
> 클래식(전자정부 3.x/4.x)은 [../classic/test-stack-classic.md](../classic/test-stack-classic.md)를 쓴다.

## 현황 (Arkive, 2026-10 확인)

- `src/test` 있음 (`ArkiveSmokeTest`, `BoardMapperTest`, `HtmlSanitizerTest`. `verify.cmd` 9/9 통과), CI 없음, JaCoCo 있음(check 충족).
- 있는 것: `spring-boot-starter-test` 선언(JUnit5·Mockito·AssertJ transitive),
  `springdoc-openapi-starter-webmvc-ui:2.6.0`.
- Mapper 17개 전부 `@EgovMapper` + 입출력 `EgovMap`. 컨트롤러는 `@Controller` +
  `@ResponseBody Map` 관례, `@RestController`·공통 응답 객체 없음.
- DB는 파일 서버 HSQLDB(`hsql://127.0.0.1:9001`, `EgovConfigDatasource`가 직접 기동).
  테스트에서 같이 띄우면 포트 충돌이 나므로 프로파일 분리가 선행되어야 한다.

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
@SpringBootTest
class ArkiveSmokeTest {
    @Test
    void contextLoads() {
    }
}
```

주의: `EgovConfigDatasource`가 9001 파일 서버를 띄우므로 앱 실행 중에는 테스트가 실패한다.
DB 경로만 `src/test/resources/application-test.properties` 1줄 + `@TestPropertySource`로 덮어쓴다.
`test`에 동명 `application.properties`를 두면 main 파일 전체가 가려지므로(Boot도 중복 파일을
합치지 않는다, first-match) 키가 다른 별도 파일을 쓴다. 파일명 변경 후에는
`target/test-classes`의 옛 파일을 지우거나 `clean`으로 빌드한다.
운영 `db/versions` 마이그레이션은 테스트에서도 그대로 재사용한다(스키마 단일 진실 공급원 유지).

## 2. JaCoCo (핵심 로직 한정)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules><rule><element>BUNDLE</element>
                    <limits><limit>
                        <counter>BRANCH</counter><value>COVEREDRATIO</value>
                        <minimum>0.60</minimum>
                    </limits></rule></rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

라인이 아니라 분기(BRANCH) 기준, 전체가 아니라 핵심 패키지 한정으로 시작한다.

## 3. CI 게이트 (GitHub Actions 뼈대)

```yaml
# .github/workflows/verify.yml
- mvn -B verify   # 컴파일 + 테스트 + JaCoCo check 실패 시 머지 불가
```

AGENTS.md 품질 관문의 실행 주체다. Secrets(DB URL·키)는 프로젝트마다 변수로만 교체한다.

## 4. 이후 단계

- Cucumber: feature 파일은 인간 승인 후 고정, 구현 에이전트의 테스트 파일 수정 금지
  (AGENTS.md 품질 관문 참조).
- PIT: 변경 모듈의 핵심 클래스 한정 + 야간 전체 실행. 전체 상시는 빌드가 길어진다.
- Testcontainers: 운영이 Oracle/PostgreSQL이면 HSQLDB 테스트는 거짓말이 되므로
  실엔진 컨테이너로 Mapper 테스트를 돌린다.

## 이식 체크리스트

- [ ] DB 접속·시크릿을 변수·프로파일로 분리했는가 (운영값 하드코딩 금지)
- [ ] 운영 마이그레이션 SQL을 테스트 스키마로 재사용하는가 (중복 정의 금지)
- [ ] feature 파일이 인간 승인 후 고정되었는가 (구현 에이전트 수정 금지)
- [ ] 커버리지는 분기 기준·핵심 한정인가 (전체 100% 목표 금지)
- [ ] 실패한 게이트가 머지를 막는가 (경고만 띄우고 통과 금지)
