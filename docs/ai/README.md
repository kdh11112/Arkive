# SI/SM 웹개발 AI 작업 체계

> Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
> 새 기능은 Arkive에 축적하고, 타 전자정부프레임워크·Spring·Spring Boot 프로젝트에서 복붙으로 가져가는 것을 전제로 한다.
> 프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 허용한다.

이 문서는 AI와 개발자가 함께 일할 때 **무엇을 공통 기준으로 삼고, 어떤 정보는 프로젝트마다 확인해야 하는지** 한눈에 보기 위한 안내다.

핵심 원칙은 간단하다.

> **작업 방법과 기록 형식은 공통으로 정하고, 업무 규칙·API·DB·운영 값은 해당 프로젝트의 근거로 채운다.**

## 이식 구분 (복붙 기준)

| 구분 | 의미 | 해당 문서 |
| --- | --- | --- |
| 복붙 가능 | 그대로 복사해서 타 프로젝트에 사용 | `common/` 7종 (coding/sql/test/verification/migration-standard, acceptance/evidence-template) |
| 가이드형 템플릿 | 파일째 복사한 뒤 양식만 채운다. Arkive 내용은 작성 예시로 참고 | `project/` 10종 (overview·architecture·function·current-state·business-policy·api-contract·database-schema·change-impact·error-code·migration) |
| 선택 | 이식에 필수가 아니라 대상 프로젝트에서 필요할 때 작성 | 3차 자동화(Verifier/Guard/Snapshot/Runtime 도구), 4차 운영(배포/롤백/Health/Canary Runbook) |

2차(project/)는 값 자체를 공통으로 뺀 것이 아니라, **작성 가이드+빈 양식+Arkive 작성 예시** 형태로 만든 가이드형 템플릿이다.
새 프로젝트에서는 파일을 복사한 뒤 양식만 채우고, Arkive 예시는 지우거나 참고용으로 남긴다.
인사기록카드처럼 “이 칸에 무엇을 적는다”가 각 문서 상단의 작성 가이드에 적혀 있다.

루트 [AGENTS.md](../../AGENTS.md)는 AI가 먼저 읽는 작업 지침이다. 이 페이지는 사람을 위한 체계 개요이며 세부 규칙은 아래 연결 문서가 기준이다.

## 4단계 한눈에 보기

### 1차 : 공통 규칙 (복붙 가능)

- [AGENTS.md](../../AGENTS.md)
  - docs/ai/common/
    - [coding-standard.md](common/coding-standard.md)
    - [sql-standard.md](common/sql-standard.md)
    - [test-standard.md](common/test-standard.md)
    - [acceptance-template.md](common/acceptance-template.md)
    - [evidence-report-template.md](common/evidence-report-template.md)
    - [verification-standard.md](common/verification-standard.md)
    - [migration-standard.md](common/migration-standard.md)

### 2차 : 프로젝트 지식 (가이드형 템플릿)

- docs/ai/project/
  - [project-overview.md](project/project-overview.md)
  - [architecture.md](project/architecture.md)
  - [function-inventory.md](project/function-inventory.md)
  - [business-policy.md](project/business-policy.md)
  - [api-contract.md](project/api-contract.md)
  - [database-schema.md](project/database-schema.md)
  - [change-impact.md](project/change-impact.md)
  - [error-code.md](project/error-code.md)
  - [current-state-analysis.md](project/current-state-analysis.md)
  - [migration.md](project/migration.md)

### 3차 : AI 자동화 (선택)

- [docs/ai/automation/README.md](automation/README.md) (현황 및 구현 기준)

### 4차 : 운영 (선택)

- [docs/ai/operations/README.md](operations/README.md) (현황 및 Runbook 기준)

이 구조는 문서와 자동화·운영 항목의 위치를 보여준다. 3차 도구와 4차 Runbook이 실제 구현되었는지는 아래 현황에서 따로 확인한다.

## 항목별 기준 (한눈에 보기)

| 항목 | 이식 구분 | 문서 위치 | 상태 |
| --- | --- | --- | --- |
| AGENTS.md | 복붙 후 수정 | [루트 AGENTS.md](../../AGENTS.md) | 부분 미완성 |
| Coding Standard | 복붙 가능 | [common/coding-standard.md](common/coding-standard.md) | 공통 마련 |
| SQL Standard | 복붙 가능 | [common/sql-standard.md](common/sql-standard.md) | 공통 마련 |
| Test | 복붙 가능(방법) / 선택(도구) | [common/test-standard.md](common/test-standard.md) | 템플릿 있음, 자동화 없음 |
| Verifier / Guard / Snapshot / Baseline / Runtime / DOM / API·DB 확인 | 복붙 가능(방법) / 선택(도구) | [common/verification-standard.md](common/verification-standard.md), [automation/README.md](automation/README.md) | 방법만 문서화, 도구 미구현 |
| Acceptance / Evidence | 복붙 가능 | [common/acceptance-template.md](common/acceptance-template.md), [common/evidence-report-template.md](common/evidence-report-template.md) | 템플릿 구현 |
| SHA / Revision | 복붙 가능(원칙) | [common/evidence-report-template.md](common/evidence-report-template.md) | 수동 기록 |
| Migration | 복붙 가능(절차) / 가이드형(현황) | [common/migration-standard.md](common/migration-standard.md), [project/migration.md](project/migration.md) | 절차 구현, 운영 미확인 |
| Project Overview / Architecture / Change Impact / 기능 소유권 / 업무 정책 / API Contract / DB Schema / Error Code / 현행 분석 | 가이드형 템플릿 | [project/](project/project-overview.md) | 양식+예시 마련, 미완성은 문서내 주석 참고 |
| Health / Error Monitoring / Deployment / Rollback / Canary / 자동화 도구 | 선택 | [operations/README.md](operations/README.md), [automation/README.md](automation/README.md) | 미확인·미구현 |

상세한 단계별 적용·SI/SM 의미는 각 문서에 있다. 이 표는 위치와 이식 여부만 본다.

상태 표기: `구현됨`은 해당 단계의 방법/양식이 준비됐다는 뜻이고, `부분 미완성`은 일부 프로젝트 정보가 확인됐지만 채울 항목이 남았다는 뜻이다. `미구현`은 이 저장소에서 실행 가능한 도구나 절차를 확인하지 못했다는 뜻이며, `미확인`은 적용 여부나 사실을 판단할 근거가 부족하다는 뜻이다. 링크된 문서에서 미완성 내용과 완성 방법을 확인한다.

## 3차 자동화와 4차 운영의 차이

- **3차 자동화**는 정해진 기준을 도구가 반복해서 검사하도록 만드는 단계다. 절차 문서만 있고 스크립트·CI 연동·실행 결과가 없다면 자동화가 구현됐다고 보지 않는다.
- **4차 운영**은 실제 환경에서 누가 어떤 승인과 순서로 배포·확인·복구하는지 정하는 단계다. 운영 도구, 담당자, 임계치가 확인되기 전에는 예시를 실제 Runbook처럼 사용하지 않는다.
- Canary는 모든 프로젝트의 기본 절차가 아니다. 배포 환경이 지원하고 진행·중단 기준이 합의된 경우에만 둔다.
- Snapshot이나 DB 확인에는 민감정보가 섞일 수 있다. 수집·마스킹·저장·접근 기준을 프로젝트에서 확인한다.

## 현재 Arkive 문서 체계 상태

| 단계 | 상태 |
| --- | --- |
| 1차 공통 규칙 | 기본 표준과 템플릿이 마련됨. 기술·작업 방식이 달라지면 갱신 가능 |
| 2차 프로젝트 지식 | 가이드형 템플릿으로 전환됨 (작성 가이드+빈 양식+Arkive 예시). 미완성은 각 문서 HTML 주석 참고 |
| 3차 AI 자동화 | 공통 방법과 현황표만 마련됨. 실제 Verifier/Guard/Runtime 자동 도구는 확인되지 않음 |
| 4차 운영 | 현황과 기록 기준만 마련됨. 배포·롤백·운영 임계치·Canary 실행 Runbook은 미확인 |

Arkive의 미확인 항목은 [프로젝트 지식 문서](project/project-overview.md)와 [운영 현황](operations/README.md), [자동화 현황](automation/README.md)에서 확인한다. 확인된 근거와 담당자 검토가 생기면 해당 문서와 이 요약의 상태를 함께 갱신한다.