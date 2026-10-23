# DB 마이그레이션 및 배포 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. 적용방식(이력테이블·버전규칙·실행순서), 대상DB, 백업·복구·승인자를 적는다.
2. 버전별 변경이력은 1행씩 추가한다. 적용된 SQL은 수정하지 않고 새 버전으로 추가한다.
3. 운영 적용 전 대상 DB의 DDL 트랜잭션 동작을 확인해 적는다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 마이그레이션 도구/스크립트, 대상 환경별 실행 순서, 승인, 백업·복구 담당자와 사후 검증 방법.
필요한 근거/확인자: 배포 저장소·파이프라인, DBA/운영 담당자, 환경별 DB 설정과 승인된 변경 기록.
완성 방법: 저장소 밖의 배포 절차까지 확인하고, 실행 가능한 명령과 권한은 비밀값 없이 기록한다. 공통 방법은 `../common/migration-standard.md`를 참조한다.
완료 기준: 각 운영 환경의 적용/검증/복구 책임과 절차가 승인된 근거에 연결되어 있다. 미사용 도구는 담당자 확인을 남긴다.
-->

## 작성 예시 (Arkive) — 아래는 참고용

## 현재 확인된 상태 (Arkive 예시)

- 기본 DB는 `./data/ArkiveDB` 파일 HSQLDB이며 서버 재시작 뒤에도 파일의 스키마와 데이터를 재사용한다.
- 애플리케이션 시작 시 `SCHEMA_MIGRATION` 테이블을 준비하고, `db/versions/V*__*.sql` 중 미적용 버전을 숫자 순으로 실행한 뒤 버전과 파일명을 기록한다.
- 2026-10-11 스쿼시: 구 체인(V1,V2,V6~V29, 당시 빈 번호 V3·삭제된 V4·V5 포함)을 최종 상태 단일 V1으로 교체했다. 구 이력이 남은 DB에서는 새 V1이 스킵되므로 `data/`를 지우고 시작해야 한다 (`data/`는 `.gitignore`).
- 2026-10-22 `V2__board_comment.sql`(`BOARD_COMMENT`)과 `V3__message.sql`(`MESSAGE`·쪽지 메뉴 4행)을 추가했다. 기존 `data/ArkiveDB`에는 앱 기동 시 증분 적용되므로 `data/` 삭제가 필요 없다.
- 2026-10-23 `V4__board_notice.sql`(`BOARD.NOTICE_YN`·`IDX_BOARD_NOTICE`)과 `V5__permission.sql`(`APP_ROLE`·`APP_USER_ROLE`·`APP_ROLE_MENU`, 권한관리 메뉴, 기본 역할·사용자 매핑)을 추가했다. V4 기존 행은 `DEFAULT 'N'`으로 채워지고, V5는 기존 `ADMIN_YN`에 따라 사용자 역할을 자동 매핑한다. 둘 다 앱 기동 시 증분 적용된다.
- 2026-10-23 `V6__mail_notification.sql`을 추가했다. `APP_USER.MAIL_YN`, `BOARD.IMPORTANT_YN`·`MAIL_SENT_YN`·`MAIL_SENT_DT`, `PASSWORD_RESET_TOKEN`을 생성·추가하며 기존 행은 `DEFAULT 'N'`으로 채워진다. 토큰 테이블은 원문 토큰을 저장하지 않고 SHA-256 해시만 보관한다. 앱 기동 시 증분 적용된다.
- `db/data.sql`은 시작 마이그레이션이 아니다. `DBJob` 덤프 대상 파일명 표시용 주석 파일이며, 로더는 `db/versions/V*__*.sql`만 읽는다. 덤프 실체는 `backup/ArkiveDB.sql`이다.
- `db/baseline_schema.sql`(구 밑그림)과 `db/sampledb.sql`(레거시, `DROP TABLE` 포함)은 2026-10-11에 삭제했다. 새 변경에는 사용하지 않는다.
- `./data/ArkiveDB`는 로컬 HSQLDB의 실제 저장 경로다. 런타임 DB 파일은 `.gitignore`로 Git 추적에서 제외하고, `.lck` 파일은 DB 실행 중 잠금 용도로 사용한다.
- 2026-10-22~23 `verify.cmd`의 테스트 DB(`target/test-db`, 포트 9002)에 V1→V6이 순서대로 적용되는 것을 확인했다. 로컬 `data/ArkiveDB`에는 V2·V3이 적용된 상태이며 V4→V6은 다음 기동 시 증분 적용된다. 실제 운영 DB 적용 이력은 미확인.
- 운영 배포, DDL 승인, 백업 및 롤백 담당자·절차: 미확인.

## DB 템플릿 적용 (대상 프로젝트)

- `db/versions/V1__baseline.sql`, `V2__board_comment.sql`, `V3__message.sql`, `V4__board_notice.sql`, `V5__permission.sql`, `V6__mail_notification.sql`을 순서대로 적용하면 빈 DB가 현재 스키마가 된다. 복사 후 패키지·테이블명만 대상에 맞게 바꾼다.
- 이력이 있는 DB에는 baseline을 쓰지 않는다. 이미 배포된 버전은 수정하지 않고, 이후 변경은 새 버전으로 추가한다. 현재 V5·V6은 최신 개발 버전으로, 테스트 DB를 재생성해 최종 시드를 다시 적용했다.
- 삭제된 구 체인은 Git 이력에 남아 있다. 과거 버전별 변경 내역이 필요하면 2026-10-11 이전 커밋을 본다.

## 개발자 사용법

1. `src/main/resources/db/versions/`에 `V7__add_file_description.sql`처럼 다음 버전 SQL 파일을 추가한다 (이미 배포된 버전은 건드리지 않는다).
2. 파일은 이미 적용한 뒤에는 수정하지 않는다. 수정이 필요하면 더 높은 버전의 새 SQL을 추가한다.
3. 프로젝트를 시작하면 미적용 버전만 실행된다. 적용 결과는 `SCHEMA_MIGRATION`의 `VERSION`, `SCRIPT_NAME`, `APPLIED_AT`에서 확인한다.
4. SQL 구문 실패 시 해당 버전은 기록되지 않으며 시작이 실패한다. 원인을 수정한 뒤 SQL을 검증하고 재시작한다.
5. V5는 기존 `APP_USER`의 `ADMIN_YN`을 기준으로 `ROLE_ADMIN`/`ROLE_USER`를 매핑한다. 이후 사용자 삭제는 애플리케이션이 `APP_USER_ROLE`도 함께 정리한다.
6. V6은 기존 사용자·게시글에 `MAIL_YN`·`IMPORTANT_YN`·`MAIL_SENT_YN` 기본값 `N`을 채우고 `PASSWORD_RESET_TOKEN`을 만든다. 토큰 원문은 저장하지 않는다.

운영 적용 전에는 대상 DB 제품과 DDL 트랜잭션 동작을 확인한다. 이 간단한 실행기는 오류 시 롤백을 시도하지만 DB별 DDL 자동 커밋 동작까지 보장하지 않는다.

구체적인 운영 승인 조건과 보관 기준은 운영 담당자가 확인해 이 문서에 추가한다. 공통 수행 절차 [migration-standard.md](../common/migration-standard.md) 및 SQL 원칙 [sql-standard.md](../common/sql-standard.md)를 함께 따른다.
