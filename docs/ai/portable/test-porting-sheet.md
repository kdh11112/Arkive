# 테스트 이식 시트 (1기능 1장)

> 기능 코드와 테스트를 함께 복사한다. 테스트 코드를 그대로 붙이지 말고, 대상의 URL·세션·스키마·메시지에 맞춘다.
> 이 시트를 채우지 못한 테스트는 "이식 완료"로 보지 않는다.

## 0. 복사할 테스트 자산

- [ ] `src/test/java/<base>/support/ArkiveIntegrationTest.java` — 대상에서는 `IntegrationTest`로 이름을 바꾼다
- [ ] `src/test/java/<base>/support/TestSessions.java` — 로그인 세션 생성
- [ ] `src/test/resources/application-test.properties.template` — 대상 DB 값으로 채운 뒤 `application-test.properties`로 저장
- [ ] 기능별 테스트 클래스
- [ ] 기능별 fixture (`BoardFixtures`, `MessageFixtures` 등)
- [ ] `pom.xml`의 테스트 의존성·JaCoCo 설정
- [ ] `verify.cmd`

`ArkiveSmokeTest`는 Arkive 전용이므로 복사하지 않고, 대상 프로젝트에서 새로 만든다.

## 1. 테스트 분류와 복사 방식

| 종류 | Arkive 예 | 복사 방식 | 대상에서 바꿀 것 |
| --- | --- | --- | --- |
| 순수 단위 | `JwtUtilTest`, `FileTypeUtilTest`, `HtmlSanitizerTest` | 거의 그대로 | 패키지·import·클래스명 |
| 인증 인터셉터 | `AuthInterceptorTest` | 수정 복사 | 세션 키, 공개 경로, 리다이렉트 URL |
| Controller/API | `BoardCommentControllerTest`, `MessageControllerTest` | 기능과 함께 수정 복사 | URL, 응답 메시지, 세션, seed 데이터 |
| Mapper/DB | `BoardMapperTest`, `MessageMapperTest` | 기능과 함께 수정 복사 | 테이블·컬럼·PK, SQL 문법, seed 데이터 |
| 스모크 | `ArkiveSmokeTest` | 복사하지 않음 | 대상 앱 클래스로 새로 작성 |

핵심은 **테스트 시나리오는 복사하고, assertion은 대상 업무 규칙에 맞게 다시 확인**하는 것이다.

## 2. 기능 정보

- 기능명:
- Arkive 테스트 클래스:
- 테스트 종류: 단위 / Controller / Mapper / E2E
- 원본 경로:
- 대상 경로:
- 대상 앱 진입 클래스:
- 대상 테스트 패키지:

## 3. 공통 팩 적용

- [ ] `ArkiveIntegrationTest` → `IntegrationTest` 이름 변경
- [ ] `@SpringBootTest(classes = ...)`를 대상 앱 클래스로 변경
- [ ] `TestSessions`의 세션 키·사용자 형태 변경
- [ ] `application-test.properties`의 DB 제품·URL·경로·포트 변경
- [ ] 앱 DB와 테스트 DB 분리 확인
- [ ] JaCoCo `includes`를 대상 클래스로 변경
- [ ] `verify.cmd`가 `mvn clean verify`를 실행하고 실패 시 exit 1을 반환하는지 확인

## 4. 기능 테스트 맞춤

- [ ] URL·컨텍스트 경로
- [ ] 요청 파라미터·JSON 필드
- [ ] 응답 상태 코드·`success`·`message`
- [ ] 로그인·관리자 세션 키
- [ ] 테이블·컬럼·PK 길이
- [ ] 테스트 seed 데이터(`admin` 등)
- [ ] 삭제 정책(soft delete·cascade)
- [ ] 대상 DB 문법(페이징·날짜·`MERGE` 등)

## 5. 실행과 완료 기준

```cmd
mvn -Dtest=대상테스트클래스 test
verify.cmd
```

- [ ] 대상 테스트가 단독으로 통과한다
- [ ] 전체 `verify.cmd`가 통과한다
- [ ] 테스트 삭제·skip·권한 약화로 통과시키지 않았다
- [ ] 테스트 데이터가 롤백되거나 정리된다
- [ ] HSQLDB만이 아니라 대상 DB에서도 Mapper 테스트를 확인했다
- [ ] 실패 시 원인을 코드에서 고쳤다

## 6. 남은 위험·메모

-
