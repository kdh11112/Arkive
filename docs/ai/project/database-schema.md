# DB 스키마 (가이드형 템플릿)

> 이식 구분: 가이드형 템플릿. 새 프로젝트에 복사한 뒤 양식만 채운다. 아래 Arkive 예시는 참고용이다.

## 작성 가이드 (새 프로젝트에서 이렇게 채우기)

1. `application.properties`·Datasource 설정에서 DB 제품·버전·연결방식을 확인해 적는다. 운영DB는 추측 금지.
2. 버전 SQL(`db/V*`)별로 테이블·용도를 1행씩 적는다. 컬럼 의미는 데이터사전·담당자 근거 있을 때만 적는다.
3. Oracle/PG 등 대상 DB와 다른 문법(HSQL 전용 등)은 주의 칸에 표시한다.
4. 레거시 SQL(DROP 포함)은 자동실행 여부를 확인해 주의에 적는다.

## 양식 (복사해서 채우기)

| 테이블 | 스크립트 | 용도/구조 | 주의 (DB종속·보존·백업) |
| --- | --- | --- | --- |
| 예: `menu` | `db/V2__....sql` | 메뉴 ID·이름·경로 등 | 기존 ID 보존 |

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 DB 제품·버전, 운영 스키마와 컬럼 의미, 외래키/인덱스/제약조건의 전체 현황, 환경별 차이와 데이터 보존 기준.
필요한 근거/확인자: 운영 DDL/DB 카탈로그, 승인된 데이터 사전, DBA 및 데이터 소유자 확인.
완성 방법: 초기화용 개발 스크립트와 실제 환경의 스키마를 분리해 기록하고, 업무 의미는 데이터 사전/담당자 근거를 연결한다.
완료 기준: 대상 환경과 기준 버전이 명시되고, 운영 대상 객체와 제약조건이 확인된 출처에 연결되어 있다.
-->

> 이 문서는 저장소에 있는 초기화 SQL을 기준으로 한 개발 스키마 현황이며 운영 스키마 문서가 아니다. 실제 대상 DB와 버전은 환경 설정으로 별도 확인한다.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 연결·초기화 설정 (Arkive 예시)

- `application.properties`는 `Globals.DbType=hsql_server`를 설정한다. `EgovConfigDatasource.java`는 이 값에서 `./data/ArkiveDB` 파일 HSQLDB 서버를 시작한다.
- 기본 설정은 `spring.sql.init.mode=never`이며 스키마 전체를 `db/versions/V1__baseline.sql` 하나로 관리한다. 빈 DB에서 시작기가 1회 적용하고 `SCHEMA_MIGRATION`에 `V1`으로 기록한다.
- 2026-10-11에 구 체인(V1,V2,V6~V29)을 최종 상태로 합쳐 단일 V1으로 교체했다. 구 버전 이력이 남은 DB에서는 새 V1이 스킵되므로 `data/`를 지우고 시작해야 한다. 이후 변경은 V2부터 새 버전으로 추가한다 (적용된 SQL 수정 금지).
- `db/data.sql`은 시작 마이그레이션이 아니다. `DBJob` 덤프의 대상 파일명 표시용(143바이트 주석)이며 로더는 `db/versions/V*__*.sql`만 읽는다.
- `db/baseline_schema.sql`(구 복사용 밑그림, V10 시절에서 멈춤)과 `db/sampledb.sql`(레거시, `DROP TABLE` 포함)은 2026-10-11에 삭제했다. `db/system.sql`은 저장소에 이미 없고 문서에만 남아 있던 언급이라 함께 정리했다.
- HSQLDB 런타임 파일은 프로젝트 `data/` 아래에 만들어지며 `.lck`는 실행 중 잠금 파일이다. 현재 `.gitignore`에서 `data/`를 제외해 DB 파일은 Git에 올리지 않는다.
- 운영 DB 제품/버전과 운영 스키마: 미확인.

## 초기화 스크립트의 테이블 (`db/versions/V1__baseline.sql`)

| 테이블 | 확인된 용도/구조 | 주의 |
| --- | --- | --- |
| `menu` | 메뉴 ID·이름·경로·상위·순서·사용 여부. 시드 32행 (사용자관리 `ME02010400` 포함) | 기존 메뉴 ID는 보존. `ME02010300`은 Swagger 유지 |
| `ATCH_FILE` | 파일 ID·그룹 ID·원본/물리 파일명·경로·크기·확장자·등록 정보 (`USE_YN` 없음) | hard delete. 기존 `OTHER_` 그룹은 `DROPZONE_`으로 이전된 상태 |
| `SAMPLE` | eGovFrame CRUD 대상. 기본 레코드 6건 포함 | 샘플 매퍼가 살아 있어 유지 |
| `IDS` | eGovFrame 샘플 ID 생성기. `SAMPLE` 다음 ID 7로 초기화 |  |
| `BOARD` | 게시판. `BOARD_TYPE`: CK·TOAST·FAQ·QNA. 첨부는 `ATCH_FILE_GRPID`(`BOARD_` 접두사) |  |
| `COMM_CODE` | 공통코드. 마스터는 `MASTR_CODE = DETAIL_CODE`인 행 | 별도 마스터 테이블 없음 |
| `QNA_ANSWER` | Q&A 답변. `BOARD`와 1:1, 질문 삭제 시 cascade | 답변 존재 여부로 상태 판정 |
| `MEMO_REPORT` | 메모보고. 상태 보고→확인/의견 |  |
| `POPUP` | 팝업. 이미지는 `ATCH_FILE`(`POPUP_` 그룹) |  |
| `BANNER` | 배너. 메인이미지는 `POSITION='MAIN'`으로 통합 |  |
| `SCHEDULE` | 일정. 날짜는 문자열(`YYYY-MM-DD HH24:MI`) | `SCHEDULE_TYPE`으로 부서/개인 등 구분 |
| `HOLIDAY` | 공휴일. 달력 공용 |  |
| `CATEGORY` | 카테고리. `UP_CAT_ID` 자기참조 N단 |  |
| `ADDRESS_BOOK` | 주소록 |  |
| `POLL` / `POLL_ITEM` | 온라인POLL 질문·보기. 비로그인 중복투표 차단은 세션 |  |
| `DUTY` | 당직. 엑셀 일괄등록: 날짜(YYYYMMDD)\|당직자\|비고 |  |
| `BUSINESS_CARD` / `CARD_USER` | 명함·사용등록. 공개 명함만 목록 노출 |  |
| `COUNSEL` | 상담. 비밀번호 SHA 해시. 상태 접수대기/접수/완료 |  |
| `APP_USER` | 사용자. `ADMIN_YN`으로 관리자/일반 구분. 초기 admin 1건 포함 | 초기 비번은 개발용. 운영 전 변경 |
| `OAUTH_LINK` | 소셜 로그인 연계 (`PROVIDER`, `PROVIDER_ID` PK) |  |

제외한 테이블: 구 V2의 `menu_author`·`user`는 매퍼·자바 참조 0건이라 baseline에서 뺐다. 로그인은 `APP_USER`를 쓴다.

## 스키마 변경 기록

| 변경 ID/일자 | 객체 | 변경 이유 | DDL/마이그레이션 경로 | 영향 기능/Mapper | 검증·복구 근거 |
| --- | --- | --- | --- | --- | --- |
| 2026-10-11 | 스키마 전체 | 구 체인(V1,V2,V6~V29)을 최종 상태 단일 V1으로 합침. `menu_author`·`user` 제외, 사용자관리 `ME02010400` 신규, 메뉴 공백 정리 | `db/versions/V1__baseline.sql` | 전체 (경로·매퍼는 불변) | `verify.cmd` 성공 (Tests 9/9, JaCoCo 충족). 빈 테스트 DB에 V1 1회 적용 + `SCHEMA_MIGRATION` 기록 확인. 구 이력 DB는 `data/` 삭제 후 재생성 |
| 2026-10-11 | `baseline_schema.sql`, `sampledb.sql` | 밑그림이 V1에 흡수됨. 레거시는 `DROP` 위험으로 삭제 | 삭제 (Git 이력에 남음) | 없음 (로더 미참조) | 참조 grep 0건 확인 후 삭제 |

이후 신규 SQL은 `db/versions/` 바로 아래에 V2부터 추가한다.

컬럼 의미, 제약조건 및 실제 운영 데이터는 SQL 파일만으로 추정하지 않는다. 변경은 [sql-standard.md](../common/sql-standard.md) 및 [migration.md](migration.md)를 따른다.
