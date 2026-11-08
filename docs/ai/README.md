# Arkive 문서 지도

> Arkive 원본용 목차다. 대상에 복사하지 않는다. AI는 [AGENTS.md](../../AGENTS.md)의 작업별 읽기 순서를 따르고, 이식은 [PORTING.md](../../PORTING.md)부터 시작한다.

방법·양식·프로젝트 사실을 분리한다. 실제 값과 검증 수치는 아래 지정한 기록에서만 관리하며 이 목차에 다시 적지 않는다.

## 현황의 원본

| 정보 | 관리 위치 |
| --- | --- |
| 테스트 개수·커버리지·전체 클래스 목록 | [src/test/README.md](../../src/test/README.md) |
| 기능별 코드 위치·검증 범위와 공백 | [function-inventory.md](project/function-inventory.md) |
| 계층·공통 호출 흐름·인증/비동기 경계 | [architecture.md](project/architecture.md) |
| 기능 구현·검증완료·미구현 1차 확인 목록 | [README.md](../../README.md) |
| 항목별 동작·주의점·알림 옵션 | [feature-status.md](project/feature-status.md) |
| GUI·HTTP 확인의 날짜·조건·정리 이력 | [current-state-analysis.md](project/current-state-analysis.md) |
| 설정·DB·운영 미확인 사항 | [project-overview.md](project/project-overview.md), [migration.md](project/migration.md) |

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
