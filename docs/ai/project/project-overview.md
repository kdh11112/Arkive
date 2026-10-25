# 프로젝트 개요 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 업무 목적/운영 조직, 운영 프로파일·배포 방식, 운영 DB 제품·버전, 권한·개인정보 요구사항, 외부 연계 계약, Health·장애 대응·Canary 지원 여부.
필요한 근거/확인자: 업무 책임자, 운영·인프라 담당자, 보안 담당자, 공식 연계 명세.
완성 방법: 확인된 담당자/문서와 환경별 배포 설정을 검토하고, 민감값 없이 실제 운영 기준만 표에 추가한다.
완료 기준: 필수 운영 환경과 소유 역할의 출처가 적혀 있고, 미확인 항목에는 담당자와 후속 확인 경로가 지정되어 있다.
-->

> 확인일: 2026-09-25, 2026-10-23 보강. 아래 내용은 저장소 코드와 설정에서 확인한 현황이다. 운영 환경 및 업무 목적은 별도 근거가 확인될 때 갱신한다.
> Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
> 타 전자정부프레임워크·Spring·Spring Boot 프로젝트로 복붙 이식을 전제로 하며, 프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 허용한다.

## 작성 가이드 (Arkive 기록 참고)

1. `pom.xml`에서 빌드·Java버전·프레임워크를 확인해 양식 1~4행에 적는다.
2. `application.properties`·`EgovConfigDatasource.java`에서 포트·DB종류·DB경로를 확인해 5~6행에 적는다.
3. 마이그레이션 방식(`db/` 규칙)은 7행에 적는다. 운영DB는 추측하지 말고 미확인으로 둔다.
4. 비밀값(SMS키·JWT·private.key 등)은 절대 복사하지 않는다.

## 양식 (Arkive 기록 참고)

| 구분 | 내용 | 근거 (파일·담당자·확인일) |
| --- | --- | --- |
| 빌드 | 예: Maven, artifact `group:artifact:version` | `pom.xml` |
| Java / 프레임워크 | 예: Java 17, eGovFrame Boot 5.0.0 | `pom.xml` |
| 웹 화면 | 예: Thymeleaf, 템플릿 경로 | `pom.xml`, `application.properties` |
| 데이터 접근 | 예: MyBatis, Mapper XML 경로 | `EgovConfigMapper.java`, `sqlmap/` |
| 기본 포트 | 예: 8080 | `application.properties` |
| 현재 설정 DB | 예: `Globals.DbType=OOO`, 경로·포트 | `application.properties`, `EgovConfigDatasource.java` |
| SQL 마이그레이션 | 예: 시작 시 미적용 버전만 적용 | `EgovConfigDatasource`, `db/` |
| 운영 배포 / 실서비스 DB | 미확인 (추측 금지) | 운영 설정 근거 미확인 |

## Arkive 작성 예시

## 확인된 기술 및 실행 구성 (Arkive 예시)

| 구분 | 확인된 내용 | 근거 |
| --- | --- | --- |
| 빌드 | Maven, artifact `Arkive:Arkive:1.0.0` | `pom.xml` |
| Java / 프레임워크 | Java 17, eGovFrame Boot parent 5.0.0, Spring Boot 의존성 사용 | `pom.xml` |
| 웹 화면 | Thymeleaf, 템플릿은 `src/main/resources/templates/thymeleaf/` | `pom.xml`, `application.properties` |
| 데이터 접근 | MyBatis 및 eGovFrame 데이터 접근 설정, Mapper XML | `EgovConfigMapper.java`, `src/main/resources/egovframework/sqlmap/` |
| 기본 포트 | 8080 | `application.properties` |
| 현재 설정 DB | `Globals.DbType=hsql_server`; `./data/ArkiveDB` 파일 DB를 `Globals.hsql_server.Port=9001`에서 제공. 테스트는 9002 | `application.properties`, `EgovConfigDatasource.java` |
| SQL 마이그레이션 | 시작 시 미적용 버전만 숫자 순으로 적용 | `EgovConfigDatasource`, `db/` |
| 운영 모니터링 | Actuator `health,info,metrics` 노출, `health.show-details=always`. 메일 헬스는 `MAIL_HEALTH_ENABLED=false`가 기본 | `application.properties` |
| 메일 발송 | `spring-boot-starter-mail`, `MailAsyncService`/`MailServiceImpl`, `EgovConfigAsync`의 `mailTaskExecutor`. 게시판 공지/중요글은 `BoardNotificationEvent`/`BoardNotificationEventListener`가 SSE·메일을 분리. 기본 `arkive.mail.enabled=false`; SMTP·발신자·base URL은 환경변수 | `pom.xml`, `application.properties`, `MailServiceImpl`, `EgovConfigAsync`, `BoardNotificationEvent`, `BoardNotificationEventListener` |
| 운영 배포 / 실서비스 DB | 미확인. 현재 로컬 설정만으로 운영 구성을 추정하지 않는다. | 운영 설정 근거 미확인 |

- 2026-10-22 기준 게시판 댓글(1단)과 쪽지관리(받은/보낸, 읽음, 함별 소프트 삭제)가 구현되어 있고, 2026-10-23에 공지 고정(CK 공지 목록 상단 정렬), 중복 로그인 방지(사용자별 최신 세션 유지), 역할 기반 메뉴 권한(서버 인터셉터 강제·SSE 역할 필터), 메일 발송·비밀번호 재설정 토큰, 게시판 알림 이벤트(Mail/SSE 채널 분리), 로그인 사용자 권한별 읽기 전용 사이트맵, 로그인 사용자 제목 LIKE 통합검색 1단계를 추가했다. 2026-10-24에는 미매핑 URL·정적 리소스를 404로 응답하도록 전역 예외 매핑을 보완하고, 메일 비활성 시 메일 헬스 인디케이터가 전체 health를 DOWN으로 만들지 않게 했다. 저장소에는 2단계(Q&A 내·외부 구분, 감사 로그, 개인정보 마스킹·필드 암호화, TOTP, 약관, 초성 검색·ES 선택 전환, SMS, 중요글 채널 분리, 통계, GA4·Clarity 동의 연계, 내부 API)와 3단계(회의실·약식결재·HR·설문·강의·약도·SSO·WebSocket 채팅·Spring Batch 일일 집계) 코드가 추가되어 있다. 같은 날 2단계 잔여 항목(검색 재색인 관리자 권한, 감사 이벤트 필터, 사용자 휴대전화 암호화)을 보완하고 `verify.cmd -o`로 전체 193개 테스트와 JaCoCo 클래스별 게이트가 통과했으며, 검증을 막던 3단계 `BatchConfig`·`ChatWebSocketConfig` 기동 오류도 수정했다. 브라우저 GUI·실행 HTTP·외부 연동 검증 범위는 `README.md`의 `검증 현황`을 따른다. 메일·SMS·SSO·OAuth·NICE 외부 계약은 미확인이다. 상세 위치는 [function-inventory.md](function-inventory.md), [database-schema.md](database-schema.md), [api-contract.md](api-contract.md)를 참조한다.

## 주의할 실행 동작

- 로그인 성공 시 `LoginSessionRegistry`가 사용자별 최신 세션 1개만 유지한다. 같은 계정으로 다시 로그인하면 이전 세션이 만료되며, 이 레지스트리는 단일 애플리케이션 인스턴스의 메모리 기준이다. 다중 인스턴스 운영 시 공유 세션 저장소나 스티키 세션 정책이 필요하다.
- 일반 사용자의 메뉴 조회·쓰기는 역할(`APP_ROLE`/`APP_ROLE_MENU`)로 판단하고 `MenuPermissionInterceptor`가 서버에서 강제한다. `ADMIN_YN='Y'`는 슈퍼관리자로 통과하며 `/system`·`/auth`는 기존 정책상 관리자만 접근한다. 역할이 없는 사용자는 메뉴 접근이 거부되고, 신규 사용자는 `ROLE_USER`를 기본 배정받는다.
- DB 스키마와 초기 데이터는 모두 `db/versions/V*__*.sql`로 관리한다. 현재 파일은 V1~V18, V30~V34이며 V19~V29는 없다. V8까지는 기존 검증 이력이 있고, 2단계 V7·V9~V12·V34와 3단계 V13~V18·V30~V33은 2026-10-24 `mvn -o test`의 테스트 DB 기동에서 적용됐다. 운영 DB 적용·데이터 이관은 확인하지 않았다. 다음 신규 변경은 V35부터 추가한다.
- 메일은 `arkive.mail.enabled=false`가 기본이다. 켜려면 `MAIL_ENABLED=true`, `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, 인증·TLS 환경값을 설정해야 하며 값이 없으면 발송을 건너뛴다. 테스트 설정은 `arkive.mail.enabled=false`라 실제 SMTP에 연결하지 않는다.
- 메일 발송은 `MailAsyncService`의 `mailTaskExecutor`에서 비동기로 실행된다. 환영·공지/중요글은 `APP_USER.MAIL_YN='Y'`인 사용자에게만, 비밀번호 재설정은 거래성 메일로 동의와 무관하게 이메일이 있으면 발송한다. 공지/중요글은 `BOARD.MAIL_SENT_YN`을 먼저 선점하므로 같은 글에 두 번 발송하지 않지만, 선점 후 SMTP 실패 시 자동 재시도는 없다. 게시판 저장 API는 `BoardNotificationEvent`만 발행하고, SSE·메일 발송 실패는 리스너에서 격리해 저장 응답을 실패시키지 않는다.
- `db/system.sql`·`db/sampledb.sql`·`db/baseline_schema.sql`은 현재 저장소에 없다(2026-10-11 정리). 새 스키마 변경에는 버전 마이그레이션을 사용한다.
- 기본 실행은 파일 DB(`./data/ArkiveDB`)를 열어 기존 데이터를 유지하고, 시작 시 버전 이력에 없는 마이그레이션 SQL만 적용한다.
- 기존 메모리 DB는 종료 시 사라졌으며 파일 DB로 자동 이관되지 않는다. 초기화/마이그레이션 설명은 [migration.md](migration.md)를 참고한다.
- Actuator는 `health`, `info`, `metrics`를 노출하고 `health.show-details=always`다. 서버 생존·DB 상태·요청 수·JVM 메모리를 볼 수 있지만, 민감 엔드포인트의 운영 공개 범위와 내부망 제한 정책은 미확인이다. 메일 헬스는 `management.health.mail.enabled=${MAIL_HEALTH_ENABLED:false}`로 기본 비활성이며, SMTP를 켠 환경에서만 `MAIL_HEALTH_ENABLED=true`로 활성화한다.
- 미매핑 URL·정적 리소스는 `NoResourceFoundException`·`NoHandlerFoundException` 매핑으로 HTTP 404와 `error/error` 화면을 반환한다. 그 외 처리되지 않은 `Throwable`은 기존 `sample/egovSampleError`(500) 경로를 유지한다. 미매핑 `.json` 경로도 현재는 HTML 404를 반환하므로 JSON 오류 규약이 필요한 API에는 별도 처리가 필요하다.
- `QuartzConfig.java`는 10분 주기의 Trigger를 정의하고, `DBJob.java`는 DB `SCRIPT` 명령에 `/db/data.sql` 절대 경로를 전달한다. 이것이 저장소의 `src/main/resources/db/data.sql`을 가리킨다고 보장되지 않으며, 실제 출력 위치와 쓰기 권한·백업 정책은 확인이 필요하다.
- HSQLDB 런타임 파일은 `./data/ArkiveDB`를 기준으로 생성된다. `data/`는 로컬 DB 상태이며 Git 관리 대상이 아니다. DB가 실행 중일 때 생기는 `.lck`는 잠금 파일이므로 직접 삭제하지 않는다.
- 3단계 기능의 기본값은 외부 연동 없이 기동되도록 두었다. SSO(`SSO_ENABLED=false`), 동영상 인코딩(`FFMPEG_ENABLED=false`), SMS(`ARKIVE_SMS_ENABLED=false`), 카카오 지도(`KAKAO_MAP_KEY` 빈 값)는 비활성·미설정 상태에서 해당 기능만 제한된다. 2단계도 GA4·Clarity(`ARKIVE_ANALYTICS_ENABLED=false`), Elasticsearch(`ARKIVE_SEARCH_ES_ENABLED=false`), 내부 API(`ARKIVE_INTERNAL_API_TOKEN` 빈 값 → 503)를 기본 비활성으로 둔다.
- `ARKIVE_FIELD_KEY`는 TOTP 비밀키·주소록/명함 연락처·사용자 휴대전화의 AES-GCM 키다. 값이 없으면 해당 저장 기능이 거부되고 기존 평문은 읽히며, 키를 잃으면 암호문을 복구할 수 없다. `APP_USER.EMAIL`은 아이디 찾기·비밀번호 재설정의 조회 조건이라 암호화하지 않는다.
- WebSocket 채팅은 세션 로그인과 채팅 메뉴 READ·WRITE, Origin 검사를 통과해야 연결되며, 메시지는 JVM 메모리로 브로드캐스트한다. 다중 인스턴스에서는 공유 브로커나 스티키 세션이 필요하다.
- Spring Batch 일일 집계는 화면에서 수동 실행만 가능하고 스케줄러가 없다. `ResourcelessJobRepository`라 `BATCH_*` 실행 이력을 남기지 않으며, 같은 날짜 재실행은 집계 행을 지우고 다시 넣는다.
- 설정 파일에는 외부 연계 및 암호화 관련 값이 포함될 수 있다. 문서, 로그, 증거 자료에 실제 비밀값을 복사하지 않는다. 환경별 설정 전달 방법은 미확인이다.

## 아직 확인되지 않은 프로젝트 지식

- 업무 목적, 운영 조직 및 기능별 책임자
- 운영 프로파일, 배포 방식, 대상 DB와 DB 버전
- 인증·권한 정책 및 개인정보 처리 요구사항
- 외부 API의 정식 계약, 오류 코드 체계와 재시도 정책
- SMTP 제공자·발신 도메인·SPF/DKIM·반송/수신거부·대량 발송 정책
- 메일 발송 실패 재시도, 게시글 알림 1회 선점 후 실패 처리, 수신 동의 철회·보존 정책
- Health 기준, 장애 대응, Canary 지원 여부

미확인 항목은 담당자 확인 및 근거 문서가 확보되면 관련 프로젝트 지식 문서에 기록한다.
