# 기능 목록 및 소유권 현황

<!--
문서 상태: 부분 미완성
미완성 항목: 전체 업무 기능 목록, 기능별 책임 팀/담당자, 기능별 지원 범위와 사용 여부.
필요한 근거/확인자: 업무 담당 조직, 운영 담당자, 사용자/화면 목록, 배포 대상 기능 목록.
완성 방법: 기능별 화면·API·Service·Mapper·외부 연계를 조사하고 담당 조직이 소유권을 확인한 뒤 각 행을 추가·검토한다.
완료 기준: 운영 대상 기능이 빠짐없이 분류되고 모든 기능 소유자가 확인되었거나, 소유권 미정 항목에 의사결정 담당과 기한이 기록되어 있다.
-->

> 2026-09-23 저장소 검색으로 확인한 주요 코드 영역의 시작 목록이다. 전체 기능 목록이나 조직별 소유권을 확정한 문서가 아니다. 소유자는 근거가 없어 미확인으로 둔다.

| 기능/영역 | 확인된 구현 위치 | 확인된 범위 | 책임자/소유권 |
| --- | --- | --- | --- |
| 시스템 메뉴 관리 | `arkive.admin.system.*`, `System_SQL.xml`, `templates/thymeleaf/system/` | 메뉴 목록·상세 조회, 중복 조회, 등록·수정·삭제 경로가 있음 | 미확인 |
| 공통 파일 처리 | `arkive.admin.comm.web.FileController`, `FileService*`, `File_SQL.xml` | 파일 업로드·다운로드·목록·삭제, TUS 업로드 관련 코드가 있음 | 미확인 |
| NICE 본인 확인 연계 | `NiceCheckController`, `NiceCheck`, 관련 VO | NICE 연계 Controller/모델이 있음. 실제 계약과 운영 사용 여부는 미확인 | 미확인 |
| 샘플 기능 | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml` | eGovFrame 샘플 CRUD 및 페이징 코드가 있음 | 샘플 코드, 업무 소유권 해당 없음 |
| 시스템 설정·예약 DB 작업 | `egovframework.example.config.*`, `arkive.com.config.*` | DB·Mapper 설정, 10분 Trigger와 덤프 작업 코드가 있음 | 운영 책임자 미확인 |
| 기타 업무 기능 | `src/main/java/arkive/` 전체 | 본 목록에서 완전 분류하지 않음 | 미확인 |

## 유지 기준

- 기능을 추가·변경할 때 실제 화면, Controller, Service, Mapper/SQL, 외부 연계와 호출처를 확인해 행을 갱신한다.
- `@RequestMapping` 등 코드상 경로가 있다는 사실과 사용 중인 업무 기능이라는 사실을 구분한다.
- 책임 조직·담당자는 업무 담당자나 공식 문서 근거가 있을 때만 기록한다.
