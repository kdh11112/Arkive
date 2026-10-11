# 테스트 스택 이식 (클래식용)

> 이식 구분: 가이드형. 일반 전자정부 3.x/4.x 관례 기준이다. 버전·XML 구조는 대상 프로젝트에서 확인한다.
> Boot(Spring Boot 3.x)는 [../boot/test-stack-boot.md](../boot/test-stack-boot.md)를 쓴다.

## 대응표 (ADTM 3.10·GTEMS 4.3급 구조 기준)

| 항목 | Boot용 | 클래식용 |
| --- | --- | --- |
| 단위 테스트 | JUnit5 (`starter-test`) | JUnit4 + `spring-test`(버전은 프로젝트 Spring에 맞춤) |
| 컨텍스트 로딩 | `@SpringBootTest` | `@RunWith(SpringJUnit4ClassRunner.class)` + `@ContextConfiguration`에 기존 `context-*.xml` 지정 |
| Mapper 테스트 | 위와 동일 + HSQLDB/Testcontainers | `SqlSessionFactory`를 XML 설정에서 로드. `EgovAbstractMapper` 상속 DAO는 그대로 쓴다 |
| Controller 테스트 | `@WebMvcTest` 또는 MockMvc | `spring-test` MockMvc + `@WebAppConfiguration`, JSP 렌더링 대신 JSON·Model 값 검증 |
| BDD | Cucumber-JUnit5 (`cucumber-junit-platform-engine`) | Cucumber-JVM 6.x + `cucumber-junit` (JUnit4 Runner). feature 문법 동일이라 feature 파일은 공유 가능 |
| 커버리지 | JaCoCo 0.8.11 | 동일. 단 Java 8 프로젝트는 JaCoCo 0.8.8 상한 |
| 뮤테이션 | PIT (JUnit5 플러그인 필요) | PIT 기본이 JUnit4 지원. 버전은 pitest 최신의 Java 8 호환 범위 확인 |
| CI | 동일 워크플로 | 동일. `mvn -B verify` + Secrets만 교체 |

## 클래식 주의점

- `javax.*`라 Boot용 테스트 코드를 그대로 못 쓴다. 컨텍스트 로딩부와 import만 교체하고
  테스트 본문(given-when-then)은 재사용한다.
- XML 컨텍스트에 테스트용 DataSource를 덮어쓰려면 별도 `context-test-datasource.xml`을
  만들고 `@ContextConfiguration`에 추가한다. 운영 XML은 건드리지 않는다.
- Tiles/JSP 화면은 단위 테스트 범위에서 제외하고 JSON·리다이렉트 값까지만 본다.
  화면 회귀는 핵심 시나리오 한정.

## 이식 체크리스트

- [ ] DB 접속·시크릿을 변수·프로파일로 분리했는가 (운영값 하드코딩 금지)
- [ ] 운영 마이그레이션 SQL을 테스트 스키마로 재사용하는가 (중복 정의 금지)
- [ ] feature 파일이 인간 승인 후 고정되었는가 (구현 에이전트 수정 금지)
- [ ] 커버리지는 분기 기준·핵심 한정인가 (전체 100% 목표 금지)
- [ ] 실패한 게이트가 머지를 막는가 (경고만 띄우고 통과 금지)
