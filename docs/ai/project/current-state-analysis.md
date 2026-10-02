# 기존 코드 현행 분석 (가이드형 템플릿)

> 이식 구분: 가이드형 템플릿. 새 프로젝트에 복사한 뒤 양식만 채운다. 아래 Arkive 예시는 참고용이다.

## 작성 가이드 (새 프로젝트에서 이렇게 채우기)

1. 기능별로 입력→처리→결과·오류를 추적해 1행씩 적는다. 코드 관찰과 담당자 확인을 구분한다.
2. 근거(파일·화면·로그)와 미확인 사항을 반드시 함께 적는다.

<!--
문서 상태: 부분 미완성
미완성 항목: 메뉴·파일·예약 작업의 실제 화면 호출과 업무 흐름, 역할별 처리, 예외/오류 응답 및 데이터 상태 전이. 현재 기록은 저장소에서 찾은 일부 코드 흐름이다.
필요한 근거/확인자: 현행 화면·API 실행 결과, 운영 로그(민감정보 마스킹), 업무 담당자 확인, 기존 시스템 문서.
완성 방법: 기능별로 보이는 입력부터 DB/외부 연계까지 추적하고, 코드 관찰과 담당자가 확인한 실제 현행 동작을 구분해 표에 기록한다.
완료 기준: 분석 범위의 각 기능에 호출자·사전조건·처리·결과·오류·근거가 작성되고 미확인 동작이 별도 표시되어 있다.
-->

> 이 문서는 현재 저장소 구현에서 관찰한 동작 흐름을 기록한다. 과거 운영 시스템의 동작, 업무 의도 또는 승인된 정책을 뜻하지 않는다. 저장소 분석 시점: 2026-09-25.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 기능 흐름 (Arkive 예시)

| 영역 | 코드에서 확인한 흐름 | 확인된 출처 | 추가 확인 필요 |
| --- | --- | --- | --- |
| 메뉴 관리 | `/system/menuList` 화면에서 Controller가 Service를 호출하고 Mapper/XML로 조회·등록·수정·삭제를 연결한다. | `SystemController`, `SystemServiceImpl`, `SystemMapper`, `System_SQL.xml` | 화면 호출 상세, 인증/권한, 메뉴 정책, 오류 계약 |
| 파일 처리 | 공통 FileController가 업로드·목록·다운로드·삭제 및 TUS 경로를 제공하고 파일 Service/Mapper 및 파일 저장 경로 설정을 사용한다. | `FileController`, `FileServiceImpl`, `File_SQL.xml`, `application.properties` | 화면별 호출 관계, 파일 접근 권한, 보존·삭제 정책, 외부 연계 |
| DB 초기화/마이그레이션 | Spring SQL 초기화는 꺼져 있다. 애플리케이션 시작 시 `SCHEMA_MIGRATION`에 없는 `db/V숫자__설명.sql`만 숫자 순서로 실행한다. | `application.properties`, `EgovConfigDatasource`, `src/main/resources/db/V*.sql` | 운영 DB 제품·버전과 백업/복구 절차 |
| 레거시 초기화 SQL | `sampledb.sql`, `system.sql`은 저장소에 남아 있으나 기본 설정에서는 실행되지 않는다. 수동 실행 시 기존 테이블을 삭제할 수 있다. | `application.properties`, `src/main/resources/db/sampledb.sql`, `system.sql` | 별도 수동 사용 여부 미확인 |
| 예약 DB 작업 | Quartz 설정이 `DBJob`을 10분마다 실행하도록 Trigger를 선언하고, Job은 HSQLDB `SCRIPT` 명령에 `/db/data.sql` 경로를 전달한다. 이 절대 경로가 저장소의 `src/main/resources/db/data.sql`을 뜻한다고 보장되지 않는다. | `QuartzConfig`, `DBJob`, `src/main/resources/db/data.sql` | 실제 서버 파일 권한·출력 위치, 백업 보관/복구 정책 |
| eGovFrame 샘플 | Controller, Service, Mapper/XML, 샘플 CRUD 및 페이지 처리 코드가 있다. V5 마이그레이션이 `SAMPLE`, `IDS`와 기본 레코드를 준비한다. | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml`, `db/V5__create_sample_tables.sql` | 실제 업무 사용 여부 |

## 현행 분석 보완 양식

| 기능 | 사용자/호출자 | 사전 조건 | 현재 입력/처리/상태 전이 | 현재 결과/오류 | 근거(경로·화면·로그) | 미확인 사항 |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

코드에서 확인되지 않는 정책·역할·데이터 의미는 담당자나 승인된 문서로 확인한다. 변경 전·후 차이는 해당 작업의 [Acceptance Criteria](../common/acceptance-template.md), Baseline 및 [Evidence](../common/evidence-report-template.md)와 연결해 기록한다.
