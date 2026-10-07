# 기능 목록 및 소유권 (가이드형 템플릿)

> 이식 구분: 가이드형 템플릿. 새 프로젝트에 복사한 뒤 양식만 채운다. 아래 Arkive 예시는 참고용이다.

## 작성 가이드 (새 프로젝트에서 이렇게 채우기)

1. 기능 단위로 1행을 만든다. 화면·Controller·Service·Mapper·외부연계를 추적해 적는다.
2. 책임자/소유권은 담당자 확인 있을 때만 적고, 없으면 미확인으로 둔다.
3. 이식용으로 가져갈 기능은 복사 대상(이식 시 복사 범위) 칸에 함께 적는다.
4. DB 종속 문법(HSQL 전용 함수 등)은 비고에 표시한다.

## 양식 (복사해서 채우기)

| 기능/영역 | 구현 위치 (화면·Controller·Service·Mapper) | 범위 | 책임자/소유권 | 이식 시 복사 대상 |
| --- | --- | --- | --- | --- |
| 예: 메뉴 관리 | `biz.menu.*`, `Menu_SQL.xml` | 목록·등록·수정·삭제 | 미확인 | 화면+Controller+Service+Mapper+SQL+V2 |

<!--
문서 상태: 부분 미완성
미완성 항목: 전체 업무 기능 목록, 기능별 책임 팀/담당자, 기능별 지원 범위와 사용 여부.
필요한 근거/확인자: 업무 담당 조직, 운영 담당자, 사용자/화면 목록, 배포 대상 기능 목록.
완성 방법: 기능별 화면·API·Service·Mapper·외부 연계를 조사하고 담당 조직이 소유권을 확인한 뒤 각 행을 추가·검토한다.
완료 기준: 운영 대상 기능이 빠짐없이 분류되고 모든 기능 소유자가 확인되었거나, 소유권 미정 항목에 의사결정 담당과 기한이 기록되어 있다.
-->

## 작성 예시 (Arkive) — 아래는 참고용, 새 프로젝트에서는 지우고 다시 채운다.

> 2026-09-25 저장소 검색으로 확인한 주요 코드 영역의 시작 목록이다. 전체 기능 목록이나 조직별 소유권을 확정한 문서가 아니다. 소유자는 근거가 없어 미확인으로 둔다.

| 기능/영역 | 확인된 구현 위치 | 확인된 범위 | 책임자/소유권 |
| --- | --- | --- | --- |
| 시스템 메뉴 관리 | `arkive.admin.system.*`, `System_SQL.xml`, `templates/thymeleaf/system/` | 메뉴 목록·상세 조회, 중복 조회, 등록·수정·삭제 경로가 있음 | 미확인 |
| 공통 파일 처리 | `arkive.admin.comm.web.FileController`, `FileService*`, `File_SQL.xml` | 파일 업로드·다운로드·목록·삭제, TUS 업로드 관련 코드가 있음 | 미확인 |
| NICE 본인 확인 연계 | `NiceCheckController`, `NiceCheck`, 관련 VO | NICE 연계 Controller/모델이 있음. 실제 계약과 운영 사용 여부는 미확인 | 미확인 |
| 샘플 기능 | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml` | eGovFrame 샘플 CRUD 및 페이징 코드가 있음 | 샘플 코드, 업무 소유권 해당 없음 |
| 시스템 설정·예약 DB 작업 | `egovframework.example.config.*`, `arkive.com.config.*` | DB·Mapper 설정, 10분 Trigger와 덤프 작업 코드가 있음 | 운영 책임자 미확인 |
| 기타 업무 기능 | `src/main/java/arkive/` 전체 | 본 목록에서 완전 분류하지 않음 | 미확인 |
| 소셜 로그인 | 미적용(보류). `ext/oauth` + scribejava + 로그인/사용자관리 세트 필요 | Naver 중심 코드, `Sns.*` 자리만 있음 | 보류: 로그인 체계 후에 진행 |
| 댓글 관리 | 미적용(제외). `cop/cmt` + 게시판(`cop/bbs`) 세트 필요 | `BoardMaster` 직접 import 확인 | 제외: 게시판 이식 시 함께 진행 |

## 유지 기준

- 기능을 추가·변경할 때 실제 화면, Controller, Service, Mapper/SQL, 외부 연계와 호출처를 확인해 행을 갱신한다.
- Arkive는 타 프로젝트로의 기능 이식을 전제로 한다. 기능을 추가할 때는 이식에 필요한 의존성(pom.xml), 설정(Globals 키·application.properties), DB 객체(db/versions), 외부 연계 JAR/키를 함께 기록한다.
- DB 제품이 달라질 수 있으므로(Oracle/PostgreSQL 등) DB 종속 문법과 HSQL 전용 함수는 비고에 표시한다.
- `@RequestMapping` 등 코드상 경로가 있다는 사실과 사용 중인 업무 기능이라는 사실을 구분한다.
- 책임 조직·담당자는 업무 담당자나 공식 문서 근거가 있을 때만 기록한다.
