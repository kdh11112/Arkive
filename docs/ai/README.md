# Arkive 문서 지도

> Arkive 원본용 목차다. 대상에 복사하지 않는다. AI는 [AGENTS.md](../../AGENTS.md)의 작업별 읽기 순서를 따르고, 이식은 [PORTING.md](../../PORTING.md)부터 시작한다.

방법·양식·프로젝트 사실을 분리한다. 실제 값과 검증 수치는 아래 지정한 기록에서만 관리하며 이 목차에 다시 적지 않는다.

아래 표에서 필요한 절만 읽는다. 큰 표·기록은 기능명·URL·클래스명으로 관련 행을 찾은 뒤 코드·호출처와 대조한다. 작업이 다른 계층에 영향을 줄 때만 추가 문서를 연다.

## 현황의 원본

| 정보 | 관리 위치 |
| --- | --- |
| 테스트 개수·커버리지·전체 클래스 목록 | [src/test/README.md](../../src/test/README.md) |
| 기능별 코드 위치·검증 범위와 공백 | [코드 영역](project/function-inventory.md#확인된-코드-영역) · [자동 테스트 범위](project/function-inventory.md#테스트-자산-현황-2026-11-07) |
| 계층·공통 호출 흐름·인증/비동기 경계 | [애플리케이션 구성](project/architecture.md#애플리케이션-구성) · [인증·권한](project/architecture.md#인증권한과-공통-처리) · [비동기](project/architecture.md#비동기-및-예약-작업) |
| 기능 구현·검증완료·미구현 1차 확인 목록 | [README.md](../../README.md) |
| 항목별 동작·주의점·알림 옵션 | [기능 상세](project/feature-status.md#기능-구현-현황) · [알림 설정](project/feature-status.md#알림-설정과-실패-처리) |
| GUI·HTTP 확인의 날짜·조건·정리 이력 | [기존 검증 이력](project/current-state-analysis.md#기존-검증-이력) |
| 실행 설정·운영 미확인 사항 | [실행 설정](project/project-overview.md#실행-전-확인할-설정) · [운영 미확인](project/project-overview.md#아직-확인되지-않은-운영-지식) |
| DB 연결·최신 객체·초기화/갱신 | [연결·초기화](project/database-schema.md#확인된-연결초기화-설정) · [기능별 객체](project/database-schema.md#추가-기능별-객체-최신-정의) · [개발자 사용법](project/migration.md#개발자-사용법) |
| DB SQL 배치·13.3인치 필수 CSS 확인 | [schema·seed·업무 SQL](common/sql-standard.md#db-변경을-어느-파일에-넣을지) · [반응형 화면](common/coding-standard.md#css반응형-화면) · [실기기/대체 검사](common/test-standard.md#화면-크기-확인) |
| API 경로·입출력·오류 처리 | [경로](project/api-contract.md#경로-인벤토리) · [JSON 동작](project/api-contract.md#로그인게시판쪽지휴가-json-동작) · [오류 기록](project/error-code.md) |
| 공통 변경의 사용처·영향 | [공유 지점](project/change-impact.md#확인된-공유-지점) |
| 기능 이식의 추가 의존성·설정 | [이식 묶음](project/function-inventory.md#이식-시-함께-확인할-묶음) |

## 문서 목록·이식 구분

| 구분 | 전체 문서 | 대상에 적용할 때 |
| --- | --- | --- |
| 공통 방법 | [coding-standard](common/coding-standard.md) · [sql-standard](common/sql-standard.md) · [test-standard](common/test-standard.md) · [verification-standard](common/verification-standard.md) · [migration-standard](common/migration-standard.md) | 파일째 복사하되 대상 기준으로 판단 |
| 복붙 양식 | [acceptance-template](portable/acceptance-template.md) · [evidence-report-template](portable/evidence-report-template.md) · [portable-regression-checklist](portable/portable-regression-checklist.md) · [scripts](portable/scripts.md) · [project-template-pack](portable/project-template-pack.md) · [feature-porting-sheet](portable/feature-porting-sheet.md) · [test-porting-sheet](portable/test-porting-sheet.md) · [ai-porting-request](portable/ai-porting-request.md) | 빈 양식·스크립트·이식 시트만 복사 |
| Boot 트랙 | [boot-apply](boot/boot-apply.md) · [test-stack-boot](boot/test-stack-boot.md) | Spring Boot 대상에 선택 |
| 클래식 트랙 | [classic-apply](classic/classic-apply.md) · [test-stack-classic](classic/test-stack-classic.md) | 전자정부 3.x/4.x 대상에서 관례 확인 후 선택 |
| Arkive 기록 | [project-overview](project/project-overview.md) · [architecture](project/architecture.md) · [function-inventory](project/function-inventory.md) · [feature-status](project/feature-status.md) · [current-state-analysis](project/current-state-analysis.md) · [api-contract](project/api-contract.md) · [database-schema](project/database-schema.md) · [change-impact](project/change-impact.md) · [error-code](project/error-code.md) · [migration](project/migration.md) | **복사 금지**, 원본에서 참고 |

Boot·클래식은 대상에 맞는 한 트랙만 선택한다. 대상 지식은 기존 기록을 재사용하거나 [빈 양식](portable/project-template-pack.md) 한 파일로 시작한다. 이식에는 기능·테스트 시트, 승인·검증에는 인수·증거 양식을 필요할 때만 사용하며 모든 문서를 매번 읽거나 작성하지 않는다.

프로젝트 기록의 상태·출처를 유지하고, 코드 관찰을 승인된 계약이나 운영 검증으로 바꾸지 않는다. Verifier·Guard·Runbook은 필요한 대상에서 만드는 도구/기록이며 현재 구현 여부는 코드에서 별도 확인한다.
