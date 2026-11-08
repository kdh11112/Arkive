# 테스트 이식 시트 (1기능 1장)

> 스펙 담당자가 테스트를 별도로 준비할 때 쓰는 양식이다. 구현 에이전트는 아래 맞춤 항목을 보고하고 제공된 테스트로 검증하며, 테스트 파일을 만들거나 수정하지 않는다. 기대 결과·assertion 변경은 인간 승인 후 별도 수행한다. 대상 루트 `AGENTS.md`가 우선한다.
> 이 시트를 채우지 못한 테스트는 "이식 완료"로 보지 않는다.

## 0. 복사할 테스트 자산

| 자산 | 대상 맞춤·확인 | 결과/근거 |
| --- | --- | --- |
| `src/test/java/<base>/support/ArkiveIntegrationTest.java` | `IntegrationTest`로 이름 변경, `@SpringBootTest(classes = ...)`는 대상 앱 클래스 |  |
| `src/test/java/<base>/support/TestSessions.java` | 로그인 세션 키·사용자 형태 |  |
| `src/test/resources/application-test.properties.template` | 대상 DB 제품·URL·경로·포트를 채워 `application-test.properties`로 저장. 앱 DB와 분리, 메일 기능은 `arkive.mail.enabled=false` |  |
| 기능별 테스트·fixture | 선택한 클래스, `BoardFixtures`·`MessageFixtures` 등 |  |
| `pom.xml` | 테스트 의존성·JaCoCo `includes`를 대상 클래스로 맞춤. 승인된 게이트를 비우지 않음 |  |
| `verify.cmd` | `mvn clean verify` 실행, 실패 시 exit 1 |  |

`ArkiveSmokeTest`는 Arkive 전용이므로 복사하지 않고, 대상 프로젝트에서 새로 만든다.

## 1. 테스트 분류와 복사 방식

| 종류 | Arkive 예 | 복사 방식 | 대상에서 바꿀 것 |
| --- | --- | --- | --- |
| 순수 단위 | `JwtUtilTest`, `FileTypeUtilTest`, `HtmlSanitizerTest`, `MailServiceImplTest`, `BoardNotificationEventListenerTest`, `SearchServiceImplTest` | 거의 그대로 | 패키지·import·클래스명, 메일 본문·제목·URL, 이벤트 필드, 검색 요약 규칙 |
| 인증 인터셉터 | `AuthInterceptorTest` | 수정 복사 | 세션 키, 공개 경로, 리다이렉트 URL |
| Controller/API | `BoardCommentControllerTest`, `BoardNotificationControllerTest`, `SitemapControllerTest`, `SearchControllerTest`, `MessageControllerTest`, `PasswordResetControllerTest` | 기능과 함께 수정 복사 | URL, 응답 메시지, 세션, seed 데이터, 토큰 테이블, `@RecordApplicationEvents`, `MenuInterceptor` 모델, 검색 결과 링크 |
| 전역 오류·헬스 | `ErrorResponseTest` | 수정 복사 | 오류 view, 상태 코드, 예외 resolver 등록, Actuator·메일 헬스 설정 |
| Mapper/DB | `BoardMapperTest`, `MessageMapperTest`, `SearchMapperTest` | 기능과 함께 수정 복사 | 테이블·컬럼·PK, SQL 문법, seed 데이터, `BOARD_TYPE`·페이징 문법 |
| 스모크 | `ArkiveSmokeTest` | 복사하지 않음 | 대상 앱 클래스로 새로 작성 |

스펙 담당자가 시나리오를 재사용하되 assertion을 대상 업무 규칙과 대조한다. 기대 결과 변경은 인간 승인 없이 수행하지 않는다.

## 2. 기능 정보

- 기능명:
- 테스트 준비 담당자 / 인간 승인 근거:
- Arkive 테스트 클래스:
- 테스트 종류: 단위 / Controller / Mapper / E2E
- 원본 경로:
- 대상 경로:
- 대상 앱 진입 클래스:
- 대상 테스트 패키지:

## 3. 기능 테스트 맞춤

- [ ] URL·컨텍스트 경로
- [ ] 요청 파라미터·JSON 필드
- [ ] 응답 상태 코드·`success`·`message`
- [ ] 로그인·관리자 세션 키
- [ ] 테이블·컬럼·PK 길이
- [ ] 테스트 seed 데이터(`admin` 등)
- [ ] 삭제 정책(soft delete·cascade)
- [ ] 대상 DB 문법(페이징·날짜·`MERGE` 등)
- [ ] 메일 발송 조건(활성화 플래그·발신자·수신 동의)과 1회 발송 선점
- [ ] 게시글 알림 이벤트의 SSE·메일 분기와 채널별 실패 격리
- [ ] 사이트맵의 로그인 필요·권한별 메뉴 필터·빈 메뉴 안내
- [ ] 통합검색의 대상 `BOARD_TYPE`·제목 범위·Q&A 공개 조건·LIKE 페이징 문법·상세 링크
- [ ] 비밀번호 재설정 토큰 만료·1회 사용·재발급 쿨다운·세션 무효화

## 4. 실행과 완료 기준

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

## 5. 남은 위험·메모

-
