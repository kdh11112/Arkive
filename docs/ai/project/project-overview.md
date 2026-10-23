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
| 운영 모니터링 | Actuator `health,info,metrics` 노출, `health.show-details=always` | `application.properties` |
| 메일 발송 | `spring-boot-starter-mail`, `MailAsyncService`/`MailServiceImpl`, `EgovConfigAsync`의 `mailTaskExecutor`. 게시판 공지/중요글은 `BoardNotificationEvent`/`BoardNotificationEventListener`가 SSE·메일을 분리. 기본 `arkive.mail.enabled=false`; SMTP·발신자·base URL은 환경변수 | `pom.xml`, `application.properties`, `MailServiceImpl`, `EgovConfigAsync`, `BoardNotificationEvent`, `BoardNotificationEventListener` |
| 운영 배포 / 실서비스 DB | 미확인. 현재 로컬 설정만으로 운영 구성을 추정하지 않는다. | 운영 설정 근거 미확인 |

- 2026-10-22 기준 게시판 댓글(1단)과 쪽지관리(받은/보낸, 읽음, 함별 소프트 삭제)가 구현되어 있고, 2026-10-23에 공지 고정(CK 공지 목록 상단 정렬), 중복 로그인 방지(사용자별 최신 세션 유지), 역할 기반 메뉴 권한(서버 인터셉터 강제·SSE 역할 필터), 메일 발송·비밀번호 재설정 토큰, 게시판 알림 이벤트(Mail/SSE 채널 분리), 로그인 사용자 권한별 읽기 전용 사이트맵, 로그인 사용자 제목 LIKE 통합검색 1단계를 추가했다. 메일은 기본 비활성이며 실제 SMTP 계약과 SMS 채널은 미확인이다. 상세 위치는 [function-inventory.md](function-inventory.md), [database-schema.md](database-schema.md), [api-contract.md](api-contract.md)를 참조한다.

## 주의할 실행 동작

- 로그인 성공 시 `LoginSessionRegistry`가 사용자별 최신 세션 1개만 유지한다. 같은 계정으로 다시 로그인하면 이전 세션이 만료되며, 이 레지스트리는 단일 애플리케이션 인스턴스의 메모리 기준이다. 다중 인스턴스 운영 시 공유 세션 저장소나 스티키 세션 정책이 필요하다.
- 일반 사용자의 메뉴 조회·쓰기는 역할(`APP_ROLE`/`APP_ROLE_MENU`)로 판단하고 `MenuPermissionInterceptor`가 서버에서 강제한다. `ADMIN_YN='Y'`는 슈퍼관리자로 통과하며 `/system`·`/auth`는 기존 정책상 관리자만 접근한다. 역할이 없는 사용자는 메뉴 접근이 거부되고, 신규 사용자는 `ROLE_USER`를 기본 배정받는다.
- DB 스키마와 초기 데이터는 모두 `db/versions/V*__*.sql`로 관리한다. V1 baseline, V2 게시글 댓글, V3 쪽지·쪽지 메뉴, V4 공지 고정, V5 권한 테이블·메뉴·역할 시드, V6 메일 수신 동의·중요글·비밀번호 재설정 토큰이 적용되어 있고, 다음 신규 변경은 V7부터 추가한다.
- 메일은 `arkive.mail.enabled=false`가 기본이다. 켜려면 `MAIL_ENABLED=true`, `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, 인증·TLS 환경값을 설정해야 하며 값이 없으면 발송을 건너뛴다. 테스트 설정은 `arkive.mail.enabled=false`라 실제 SMTP에 연결하지 않는다.
- 메일 발송은 `MailAsyncService`의 `mailTaskExecutor`에서 비동기로 실행된다. 환영·공지/중요글은 `APP_USER.MAIL_YN='Y'`인 사용자에게만, 비밀번호 재설정은 거래성 메일로 동의와 무관하게 이메일이 있으면 발송한다. 공지/중요글은 `BOARD.MAIL_SENT_YN`을 먼저 선점하므로 같은 글에 두 번 발송하지 않지만, 선점 후 SMTP 실패 시 자동 재시도는 없다. 게시판 저장 API는 `BoardNotificationEvent`만 발행하고, SSE·메일 발송 실패는 리스너에서 격리해 저장 응답을 실패시키지 않는다.
- `db/system.sql`·`db/sampledb.sql`·`db/baseline_schema.sql`은 현재 저장소에 없다(2026-10-11 정리). 새 스키마 변경에는 버전 마이그레이션을 사용한다.
- 기본 실행은 파일 DB(`./data/ArkiveDB`)를 열어 기존 데이터를 유지하고, 시작 시 버전 이력에 없는 마이그레이션 SQL만 적용한다.
- 기존 메모리 DB는 종료 시 사라졌으며 파일 DB로 자동 이관되지 않는다. 초기화/마이그레이션 설명은 [migration.md](migration.md)를 참고한다.
- Actuator는 `health`, `info`, `metrics`를 노출하고 `health.show-details=always`다. 서버 생존·DB 상태·요청 수·JVM 메모리를 볼 수 있지만, 민감 엔드포인트의 운영 공개 범위와 내부망 제한 정책은 미확인이다.
- `QuartzConfig.java`는 10분 주기의 Trigger를 정의하고, `DBJob.java`는 DB `SCRIPT` 명령에 `/db/data.sql` 절대 경로를 전달한다. 이것이 저장소의 `src/main/resources/db/data.sql`을 가리킨다고 보장되지 않으며, 실제 출력 위치와 쓰기 권한·백업 정책은 확인이 필요하다.
- HSQLDB 런타임 파일은 `./data/ArkiveDB`를 기준으로 생성된다. `data/`는 로컬 DB 상태이며 Git 관리 대상이 아니다. DB가 실행 중일 때 생기는 `.lck`는 잠금 파일이므로 직접 삭제하지 않는다.
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
