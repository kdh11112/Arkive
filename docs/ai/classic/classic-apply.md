# 클래식 적용 보충 (전자정부 3.x/4.x)

> 이식 구분: 가이드형. 일반 전자정부(클래식) 관례 기준이다. 버전·XML 구조· WAS는 대상 프로젝트에서 확인한다.
> Arkive 코드는 Boot 기준이라 아래 대응은 관례 매핑이며, 코드 실측 사실이 아니다.

## 진입·설정 대응

| Boot (Arkive) | 클래식 (관례, 대상 확인) |
| --- | --- |
| `EgovBootApplication` + `@ComponentScan` | `web.xml` DispatcherServlet + `context-*.xml` 컴포넌트 스캔. 스캔 패키지를 대상 패키지로 지정 |
| `application.properties` + `Globals.*` | `context-properties.xml` + `*.properties`. 키명은 대상 규칙을 따름 |
| Security 자동설정 exclude | `context-security.xml` 또는 filter 설정. 자동설정이 없으니 명시 선언 |
| `@EnableCaching` | `context-cache.xml` + ehcache 설정 (버전은 Spring에 맞춤) |

## 패키징·화면 대응

| Boot (Arkive) | 클래식 (관례, 대상 확인) |
| --- | --- |
| packaging `jar`, 내장 Tomcat | packaging `war`, 외장 WAS 배포. `webapp/WEB-INF` 구조 필요 |
| Thymeleaf + layout-dialect | JSP + JSTL/EL + Tiles(사용 시). 화면 테스트는 JSON·Model 값까지만 |
| `static/**`, `templates/**` | `webapp/css·js·images`, `WEB-INF/jsp` 관례. 리소스 핸들러 매핑 확인 |

## 코드 대응

- `jakarta.*` → `javax.*`로 import 교체가 필요하다. Boot 코드를 그대로 못 쓴다.
- `@EgovMapper`·`EgovMap`·`EgovAbstractMapper` 관례는 rte 버전에 맞으면 그대로 쓴다.
- 테스트는 [test-stack-classic.md](test-stack-classic.md)를 쓴다. 운영 XML을 건드리지 말고
  테스트용 `context-test-datasource.xml`을 추가한다.

## 미확인 (대상에서 정한다)

- 대상 Spring·rte·Java·WAS 버전, DataSource·트랜잭션 XML 실체, Tiles 사용 여부, 인증·캐시 설정 실체.
