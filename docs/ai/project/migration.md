# DB 마이그레이션 및 배포 (Arkive 기록)

> **Arkive 기록 · 대상에 복사 금지.** 대상 지식은 [빈 양식](../portable/project-template-pack.md)을 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 마이그레이션 도구/스크립트, 대상 환경별 실행 순서, 승인, 백업·복구 담당자와 사후 검증 방법.
필요한 근거/확인자: 배포 저장소·파이프라인, DBA/운영 담당자, 환경별 DB 설정과 승인된 변경 기록.
완성 방법: 저장소 밖의 배포 절차까지 확인하고, 실행 가능한 명령과 권한은 비밀값 없이 기록한다. 공통 방법은 `../common/migration-standard.md`를 참조한다.
완료 기준: 각 운영 환경의 적용/검증/복구 책임과 절차가 승인된 근거에 연결되어 있다. 미사용 도구는 담당자 확인을 남긴다.
-->

## 현재 확인된 상태

- 기본 DB는 `./data/ArkiveDB` 파일 HSQLDB이며 서버 재시작 뒤에도 파일의 스키마와 데이터를 재사용한다.
- 2026-11-08부터 최신 `src/main/resources/db/schema.sql`과 필수 `db/seed.sql`로 빈 로컬 데모 DB를 한 번 초기화한다. 버전 체인을 실행하지 않고 `spring.sql.init.mode=never`를 유지한다. 근거: `EgovConfigDatasource.java`, `HsqlDemoDatabaseInitializer.java`, `application.properties`.
- DataSource를 반환하기 전에 초기화/호환성 확인을 마치므로 Mapper·웹 요청·DB 예약 작업이 먼저 미완성 DB를 쓰지 않는다. HSQLDB 서버 시작 결과를 확인하며 종전의 고정 2초 대기는 없다.
- `ARKIVE_DB_INIT`은 데모 초기화기 전용 상태표다. ID=1에 초기화 파일의 SHA-256 체크섬, 객체 메타데이터 서명, 상태(`INITIALIZING`/`READY`), 시작·완료 시각을 기록한다. 체크섬은 Git의 CRLF/LF 차이를 정규화한다.
- 일반 Java·화면 수정 후 재시작은 완료 상태·파일 체크섬·객체 메타데이터만 읽는다. 실제 DB에 CREATE/ALTER/시드를 실행하지 않고, 비교용 메모리 DB도 만들지 않는다. 비밀번호·메뉴·권한·업무 데이터·동의 이력은 덮어쓰지 않는다.
- 전환 직전 구 체인 V1~V18·V30~V35의 24개 파일 적용 이력이 모두 있고 구조가 최신 정의와 같은 DB는 첫 전환 때만 인수한다. 비교용 메모리 DB를 해제한 뒤 상태표만 추가하고 `SCHEMA_MIGRATION`과 기존 데이터를 유지한다. 구 버전명 상수는 인수 조건이지 실행 목록이 아니다.
- 초기화 기록 누락/미완료, 불완전한 구 이력, 구조 또는 초기화 SQL 불일치는 **시작을 중단**한다. 자동 삭제·재생성·시드 재실행·증분 업그레이드는 하지 않는다. HSQLDB DDL은 롤백 뒤에도 남을 수 있어 `INITIALIZING`을 먼저 커밋하고, 시드 DML과 `READY` 전환만 같은 트랜잭션으로 완료한다.
- `db/versions/`의 24개 SQL과 덤프 표시용 주석 `db/data.sql`은 최신 파일로 대체해 제거했다. 구 SQL은 Git 이력에 남으며 예약 덤프의 실체는 여전히 `Globals.dumpPath`(기본 `./backup/ArkiveDB.sql`)다.
- 운영 배포, DDL 승인, 백업 및 롤백 담당자·절차: 미확인. 아래 개발자 절차는 로컬 데모용이며 운영 갱신 절차가 아니다.

## 개발자 사용법

1. Java 17·Maven을 준비한 Git checkout에서 `mvn spring-boot:run`을 실행한다. 별도 DB 설치나 기본 접속 설정 없이 HSQLDB가 자동 시작하고, DB가 비어 있으면 `schema.sql` → `seed.sql`을 실행한다.
2. Java·JavaScript·화면 코드만 고쳤다면 평소처럼 재시작한다. 초기화 SQL은 실행되지 않는다.
3. 객체 정의는 `schema.sql`의 최종 CREATE에, 메뉴·역할·권한 등 필수 초기값은 `seed.sql`에 반영한다. 업무 등록/수정/삭제는 Mapper/XML·Service에 둔다. [SQL 배치 기준](../common/sql-standard.md#db-변경을-어느-파일에-넣을지)을 따른다.
4. 초기화 SQL을 바꾸면 기존 DB는 자동으로 바뀌지 않고 체크섬 불일치로 시작이 중단된다. **데모 데이터를 보존하지 않아도 되는 경우 앱을 종료하고 새 DB 경로를 지정한다.** 이전 파일을 지우거나 상태표를 임의로 `READY`로 바꾸지 않는다.

   ```cmd
   mvn spring-boot:run "-Dspring-boot.run.arguments=--Globals.hsql_server.DatabasePath=./data/ArkiveDB-new"
   rem 패키징한 앱도 같은 방식으로 새 경로 지정
   java -jar target\Arkive-1.0.0.jar --Globals.hsql_server.DatabasePath=./data/ArkiveDB-new
   ```

   다음 기동도 선택한 경로를 유지한다(로컬 실행 설정 또는 실행 인자). 기본 경로로 돌아가면 이전 DB를 다시 선택한다. 여러 앱을 동시에 실행할 때는 DB 포트와 URL, 웹 포트도 서로 일치하게 분리한다.
5. 기존 데이터 보존이 필요하면 대상 DB·DDL·백필·초기값 변경·복구를 확인한 **별도 갱신 계획**을 담당자가 승인해야 한다. 이 초기화기는 기존 DB 갱신이나 갱신 후 상태표 재승인 도구를 제공하지 않는다. DDL만 수동으로 적용해도 기존 체크섬/서명과 달라져 자동 인수되지 않는다. 상태표 삭제나 체크섬 수동 위조를 갱신 절차로 사용하지 않는다.
6. 최초 초기화 실패 시 앱을 종료하고 로그와 상태표·부분 생성 객체를 확인한다. 파일은 자동 삭제되지 않는다. 필요하면 이전 파일을 보존한 채 새 경로에서 다시 실행한다. 데이터가 있는 경로에는 전체 `schema.sql`/`seed.sql`을 재실행하지 않는다.
7. 검증은 테스트 DB와 예약 덤프를 함께 격리한다. 현재 테스트 설정은 DB 경로·9002 포트만 분리하므로 `verify.cmd -DGlobals.dumpPath=./target/test-backup/ArkiveDB.sql`처럼 덤프 경로도 명시한다. 테스트 설정 변경은 스펙 담당자의 별도 작업이다.

## DB 템플릿 적용 (대상 프로젝트)

- 빈 DB용 정의는 `schema.sql`, 필수 초기값은 `seed.sql`이다. 기능별 테이블·컬럼·PK/FK·인덱스와 메뉴·역할·권한·설정을 함께 골라 패키지·테이블명·대상 DB 문법에 맞춘다. 전체 복사보다 [PORTING.md](../../../PORTING.md)의 기능별 통합 절차를 우선한다.
- **기존 DB가 있는 대상에는 HSQLDB 서버 자동 시작, `HsqlDemoDatabaseInitializer`, `ARKIVE_DB_INIT`을 복사하지 않는다.** 대상의 DataSource·Flyway/Liquibase/배포 SQL 등 승인된 갱신 방식을 유지한다. 최신 CREATE 정의는 기존 DB 업그레이드 스크립트가 아니다.
- 개발용 admin과 약관 자리표시자는 서비스 승인값이 아니다. 실제 계정·권한·문구로 준비하고 사용자 동의는 사용자에게 받는다. 새 시드는 샘플 CRUD 6행·회의실 `MR001`·현재 연도 15일 연차 부여를 제외하지만 관련 테이블과 ID 생성기 시작값은 유지한다. 이미 있는 DB의 해당 데이터는 제거하지 않는다.

## 보존·복구 주의

- `DBJob`은 기존 SQL 덤프를 삭제한 뒤 새 덤프를 쓴다. 교체 실패 시 이전 덤프가 없어질 수 있고, HSQLDB `SCRIPT` SQL 파일만으로 CLOB까지 독립 복구된다고 가정할 수 없다. 이 결함은 이번 초기화 전환에서 수정하지 않았다.
- HSQLDB 2.7.4의 격리 관찰에서는 SQL 덤프 단독 CLOB 복구 실패, 네이티브 백업 복구 성공, DDL rollback 후 객체 잔존을 확인했다. 실제 `data/ArkiveDB`·기존 백업의 복구 검증이나 운영 승인 근거가 아니며, 파일 복사만으로 일관된 백업이라고 간주하지 않는다.
- 보존 DB를 바꾸기 전에 해당 환경의 일관된 백업·복구를 별도로 검증한다. `.gitignore`의 `data/` 제외는 삭제 권한을 뜻하지 않는다. 로컬 `.lck`는 실행 중 잠금 파일이다.

## 이전 버전 실행기 이력 (전환 전)

다음은 당시 SQL·실행·검증 기록이다. 경로는 현재 삭제된 파일의 Git 이력이며 아래의 증분 기동 방식이나 시드를 새 개발 절차로 사용하지 않는다.

- 2026-10-11 스쿼시: 구 체인(V1,V2,V6~V29, 당시 빈 번호 V3·삭제된 V4·V5 포함)을 단일 V1으로 교체했다. 구 이력 DB에서는 새 V1이 스킵될 수 있으므로 당시 개발 DB 재생성이 필요했다. 운영·보존 데이터는 백업·복구·승인 확인 없이 삭제하지 않으며 `data/`의 Git 제외는 삭제 권한을 뜻하지 않는다.
- 2026-10-22 `V2__board_comment.sql`(`BOARD_COMMENT`)과 `V3__message.sql`(`MESSAGE`·쪽지 메뉴 4행)을 추가했다. 기존 `data/ArkiveDB`에는 앱 기동 시 증분 적용되므로 `data/` 삭제가 필요 없다.
- 2026-10-23 `V4__board_notice.sql`(`BOARD.NOTICE_YN`·`IDX_BOARD_NOTICE`)과 `V5__permission.sql`(`APP_ROLE`·`APP_USER_ROLE`·`APP_ROLE_MENU`, 권한관리 메뉴, 기본 역할·사용자 매핑)을 추가했다. V4 기존 행은 `DEFAULT 'N'`으로 채워지고, V5는 기존 `ADMIN_YN`에 따라 사용자 역할을 자동 매핑한다. 둘 다 앱 기동 시 증분 적용된다.
- 2026-10-23 `V6__mail_notification.sql`을 추가했다. `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`·`MAIL_SENT_YN`·`MAIL_SENT_DT`, `PASSWORD_RESET_TOKEN`을 생성·추가하며 기존 행은 `DEFAULT 'N'`으로 채워진다. 토큰 테이블은 원문 토큰을 저장하지 않고 SHA-256 해시만 보관한다. 앱 기동 시 증분 적용된다.
- 2026-10-24 `V8__leave.sql`을 추가했다. `LEAVE_BALANCE`·`LEAVE_REQUEST`와 인사관리 메뉴 4행, 역할-메뉴 5행을 만들고 기존 사용자에게 현재 연도 15일을 시드한다(이미 있으면 건너뜀). 신청 기간·일수 계산·승인 차감·취소 환원은 애플리케이션이 담당한다.
- 2단계 `V7__stage2_qna_audit.sql`(Q&A 공개 구분·감사 로그), `V9__totp.sql`, `V10__terms.sql`, `V11__search_chosung.sql`, `V12__user_contact.sql`은 3단계 추가 이전의 버전이다. 추가 당시에는 파일 존재만 기록했으며 이후 테스트 DB 적용 근거는 아래 2026-10-24 이력에 남겼다. 운영 적용 이력은 미확인이다.
- 2026-10-24 3단계 마이그레이션을 추가했다: `V13__meeting.sql`(회의실·예약), `V14__approval.sql`(약식결재), `V15__field_crypto.sql`(연락처 컬럼 확장), `V16__survey.sql`(설문 6개 테이블), `V17__sms.sql`(SMS 발송 선점), `V18__hr.sql`(HR 3개 테이블), `V30__lecture.sql`(강의·진도), `V31__map_guide.sql`(약도), `V32__chat.sql`(채팅 메시지), `V33__batch.sql`(일일 집계). V19~V29는 번호를 쓰지 않았다.
- 같은 날 2단계 보강으로 `V34__user_phone_crypto.sql`을 추가했다. `APP_USER.PHONE`을 `VARCHAR(500)`으로 확장해 AES-GCM 암호문을 저장하며, `AuthServiceImpl`의 암호화·복호화와 `SmsServiceImpl`의 발송 대상 복호화가 함께 배포되어야 한다. `EMAIL`은 조회 조건이라 평문을 유지한다.
- V13~V18·V30~V33은 각 기능 메뉴와 `APP_ROLE_MENU` 시드를 포함한다. 시드 권한은 샘플이며 운영 역할 설계에 맞게 조정해야 한다. `CREATE TABLE/INDEX IF NOT EXISTS`·`ALTER TABLE ... ADD COLUMN`·`ALTER COLUMN`은 HSQLDB 기준 문법이다.
- 2단계·3단계 마이그레이션은 2026-10-24 `mvn -o test`의 테스트 DB(`target/test-db`) 기동에서 전체 체인(V1→V34) 적용을 확인했다. 운영 DB 적용은 확인하지 않았다. 특히 `V15`는 기존 평문 연락처의 암호화 이관을 포함하지 않고, `V34`도 기존 `APP_USER.PHONE` 평문의 암호화 이관을 포함하지 않으며, `V33`은 Spring Batch 실행 이력 테이블을 만들지 않는다.
- 전환 전 `db/data.sql`은 시작 마이그레이션이 아니라 `DBJob` 덤프 대상 표시용 주석 파일이었다. 당시 로더는 `db/versions/V*__*.sql`만 읽었고 덤프 실체는 `backup/ArkiveDB.sql`이었다.
- `db/baseline_schema.sql`(구 밑그림)과 `db/sampledb.sql`(레거시, `DROP TABLE` 포함)은 2026-10-11에 삭제했다. 새 변경에는 사용하지 않는다.
- `./data/ArkiveDB`는 로컬 HSQLDB의 실제 저장 경로다. 런타임 DB 파일은 `.gitignore`로 Git 추적에서 제외하고, `.lck` 파일은 DB 실행 중 잠금 용도로 사용한다.
- 2026-10-22~24 `verify.cmd`의 테스트 DB(`target/test-db`, 포트 9002)에 당시 기준 V1→V8이 순서대로 적용되는 것을 확인했다. 2026-10-24에는 테스트 DB에서 V1→V34 전체 체인 적용과 `verify.cmd -o` 전체 193개 테스트·JaCoCo 게이트 통과를 확인했다. 로컬 `data/ArkiveDB`는 2026-11-07에 V35까지 적용된 상태를 확인했다(`BOARD_VIEW_LOG` 조회가 성공하고 `SCHEMA_MIGRATION`에 V35가 있어 재기동 시 스킵됨). 실제 운영 DB 적용 이력은 미확인.
- 삭제된 구 체인은 Git 이력에 남아 있다. 과거 버전별 변경 내역이 필요하면 2026-10-11 이전 커밋을 본다.

 구체적인 운영 승인 조건과 보관 기준은 운영 담당자가 확인해 이 문서에 추가한다. 공통 수행 절차 [migration-standard.md](../common/migration-standard.md) 및 SQL 원칙 [sql-standard.md](../common/sql-standard.md)를 함께 따른다.
