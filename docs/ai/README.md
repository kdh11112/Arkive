# Arkive 문서 지도

> Arkive 원본용 목차다. 대상에 복사하지 않는다. AI는 [AGENTS.md](../../AGENTS.md)의 작업별 읽기 순서를 따르고, 이식은 [PORTING.md](../../PORTING.md)부터 시작한다.

방법·양식·프로젝트 사실을 분리한다. 실제 값과 검증 수치는 아래 지정한 기록에서만 관리하며 이 목차에 다시 적지 않는다.

작업에 필요한 문서만 읽고, 작업이 다른 계층에 영향을 줄 때만 추가 문서를 연다.

## 현황의 원본

| 정보 | 관리 위치 |
| --- | --- |
| 테스트 개수·커버리지·전체 클래스 목록 | [src/test/README.md](../../src/test/README.md) |
| 기능 구현·검증완료·미구현 1차 확인 목록 | [README.md](../../README.md) |
| 시스템 개요·실행 설정·아키텍처·공유 지점·오류 체계 | [프로젝트 컨텍스트](project/project-context.md) |
| 기능별 코드 위치·동작 주의점·API 인벤토리·알림 설정·이식 묶음 | [기능 카탈로그](project/feature-catalog.md) |
| DB 스키마·테이블/컬럼 정의·인덱스·시드 | [DB 스키마](project/database-schema.md) |
| 기능 흐름·GUI 검증 이력·작업별 변경 영향·DB 마이그레이션 | [검증 및 변경 로그](project/verification-log.md) |
| DB SQL 배치 기준·13.3인치 필수 CSS 확인 | [schema·seed·업무 SQL](common/sql-standard.md#db-변경을-어느-파일에-넣을지) · [반응형 화면](common/coding-standard.md#css반응형-화면) · [실기기/대체 검사](common/test-standard.md#화면-크기-확인) |

## 문서 목록·이식 구분

| 구분 | 전체 문서 | 대상에 적용할 때 |
| --- | --- | --- |
| 공통 방법 | [coding-standard](common/coding-standard.md) · [sql-standard](common/sql-standard.md) · [test-standard](common/test-standard.md) · [verification-standard](common/verification-standard.md) · [migration-standard](common/migration-standard.md) | 파일째 복사하되 대상 기준으로 판단 |
| AI 운영 | [ai-orchestration](common/ai-orchestration.md) | Orca·Freebuff·Sol 환경일 때만 복사 |
| 복붙 양식 | [acceptance-template](portable/acceptance-template.md) · [evidence-report-template](portable/evidence-report-template.md) · [portable-regression-checklist](portable/portable-regression-checklist.md) · [scripts](portable/scripts.md) · [project-template-pack](portable/project-template-pack.md) · [feature-porting-sheet](portable/feature-porting-sheet.md) · [test-porting-sheet](portable/test-porting-sheet.md) · [ai-porting-request](portable/ai-porting-request.md) | 빈 양식·스크립트·이식 시트만 복사 |
| Boot 트랙 | [boot-apply](boot/boot-apply.md) · [test-stack-boot](boot/test-stack-boot.md) | Spring Boot 대상에 선택 |
| 클래식 트랙 | [classic-apply](classic/classic-apply.md) · [test-stack-classic](classic/test-stack-classic.md) | 전자정부 3.x/4.x 대상에서 관례 확인 후 선택 |
| Arkive 기록 | [project-context](project/project-context.md) · [feature-catalog](project/feature-catalog.md) · [database-schema](project/database-schema.md) · [verification-log](project/verification-log.md) | **복사 금지**, 원본에서 참고 |

> 문서 구조 최적화(10개 파일 → 4개 파일 통폐합) 이력과 분석 내용은 [최적화 계획 및 결과](docs-optimization-plan.md)를 참조한다.

Boot·클래식은 대상에 맞는 한 트랙만 선택한다. 대상 지식은 기존 기록을 재사용하거나 [빈 양식](portable/project-template-pack.md) 한 파일로 시작한다. 이식에는 기능·테스트 시트, 승인·검증에는 인수·증거 양식을 필요할 때만 사용하며 모든 문서를 매번 읽거나 작성하지 않는다.

프로젝트 기록의 상태·출처를 유지하고, 코드 관찰을 승인된 계약이나 운영 검증으로 바꾸지 않는다. Verifier·Guard·Runbook은 필요한 대상에서 만드는 도구/기록이며 현재 구현 여부는 코드에서 별도 확인한다.
