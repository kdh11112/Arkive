# src/test — 복붙 가능

이 폴더째 복사해서 쓴다. 바꿀 것은 3개뿐이다.

1. 패키지: `arkive` → 대상 프로젝트 패키지
2. `application-test.properties`: DB 격리 경로(`Globals.hsql_server.DatabasePath`)
3. `ArkiveSmokeTest`의 `classes = ...`: 대상 진입 클래스
   (테스트 클래스가 진입 클래스와 다른 패키지에 있을 때만 필요)

주의: `application.properties` 동명 파일을 두지 않는다.
Boot도 중복 파일을 합치지 않아 main 설정 전체가 가려진다.
반드시 키가 다른 별도 파일 + `@TestPropertySource` 조합을 쓴다.
