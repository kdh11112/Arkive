# Boot 적용 보충 (Spring Boot 3.x)

> 이식 구분: 복붙 가능(Boot). 아래는 Arkive 코드에서 확인한 사실이다. 파일 경로·패키지는 대상에 맞게 바꾼다.
> 공통 코딩 방법은 [../common/coding-standard.md](../common/coding-standard.md), 테스트는 [test-stack-boot.md](test-stack-boot.md)를 쓴다.

## 진입점

- `egovframework.example.EgovBootApplication`이 `@SpringBootApplication`이다.
  Security 자동설정은 exclude, `@EnableCaching`, `@ComponentScan({"egovframework","arkive"})`을 둔다.
  패키지가 다르면 스캔에 안 걸리므로 이식 시 basePackages를 대상 패키지로 바꾼다.

## 패키징·실행

- `pom.xml` packaging은 `jar`다. 내장 Tomcat으로 뜬다. WAR·외장 WAS 전제는 없다.
- `verify.cmd`가 `mvn clean verify`로 빌드·테스트·JaCoCo를 한 번에 돌린다.

## 설정

- `src/main/resources/application.properties` + `Globals.*` 키가 설정 진실 공급원이다.
  확인된 키: `Globals.DbType`, `Globals.hsql_server.*`(Driver·Url·User·DatabasePath),
  `Globals.dumpPath`, `Globals.jwt.*`, `Globals.naver.*`, `Globals.kakao.*`.
  비밀값(시크릿·키)은 커밋하지 말고 환경·시크릿 저장소에서 주입한다.
- 테스트 오버라이드는 `src/test/resources/application-test.properties` 1줄 + `@TestPropertySource`다.
  동명 `application.properties`를 test에 두지 않는다(main 전체가 가려진다).

## 화면·정적자원

- Thymeleaf + `thymeleaf-layout-dialect`다. 화면은 `templates/thymeleaf/**`, 정적자원은 `static/**`다.
- JSP·Tiles·`webapp/WEB-INF`가 없다. 클래식에 이식하면 [../classic/classic-apply.md](../classic/classic-apply.md)를 본다.

## 의존성

- 확인된 starters: web, thymeleaf, validation, mail, quartz, actuator, log4j2, devtools, test.
  `selenium-java(test)`, `springdoc-openapi`가 있다. 버전은 parent 상속이라 pom에 명시가 없다.
- `NiceID_v1_1.jar`는 `src/main/resources/lib` systemPath 참조다. 타 프로젝트로 가져가면 저장소·좌표부터 해결한다.
