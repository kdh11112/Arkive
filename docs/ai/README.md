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
| 가이드형 | 파일째 복사하되 본문 판단해서 적용 | `common/` 5종 (coding/sql/test/verification/migration-standard) |
| 복붙 가능 | 빈 양식 그대로 복사해서 채운다. 스크립트 원본 포함 | `portable/` 4종 (acceptance/evidence-template, regression-checklist, scripts) |
| 복붙 가능(Boot) | Boot 프로젝트에 그대로 복사 | `boot/` 2종 (boot-apply, test-stack-boot) |
| 가이드형(클래식) | 클래식 관례 기준. 대상 프로젝트에서 확인 후 적용 | `classic/` 2종 (classic-apply, test-stack-classic) |
| 가이드형 템플릿 | 파일째 복사한 뒤 양식만 채운다. Arkive 내용은 작성 예시로 참고 | `project/` 9종 (overview·architecture·function·current-state·api-contract·database-schema·change-impact·error-code·migration) |
| 선택 | 이식에 필수가 아니라 대상 프로젝트에서 필요할 때 작성 | 3차 자동화(Verifier/Guard/Snapshot/Runtime 도구), 4차 운영(배포/롤백/Health/Canary Runbook) |

2차(project/)는 값 자체를 공통으로 뺀 것이 아니라, **작성 가이드+빈 양식+Arkive 작성 예시** 형태로 만든 가이드형 템플릿이다.
새 프로젝트에서는 파일을 복사한 뒤 양식만 채우고, Arkive 예시는 지우거나 참고용으로 남긴다.
인사기록카드처럼 “이 칸에 무엇을 적는다”가 각 문서 상단의 작성 가이드에 적혀 있다.

루트 [AGENTS.md](../../AGENTS.md)는 AI가 먼저 읽는 작업 지침이다. 이 페이지는 사람을 위한 체계 개요이며 세부 규칙은 아래 연결 문서가 기준이다.

## 체계 한눈에 보기

### 1차 : 공통 규칙 (가이드형 5종 + 양식 4종)

- [AGENTS.md](../../AGENTS.md)
  - docs/ai/common/
    - [coding-standard.md](common/coding-standard.md)
    - [sql-standard.md](common/sql-standard.md)
    - [test-standard.md](common/test-standard.md)
    - [verification-standard.md](common/verification-standard.md)
    - [migration-standard.md](common/migration-standard.md)
  - docs/ai/portable/
    - [acceptance-template.md](portable/acceptance-template.md)
    - [evidence-report-template.md](portable/evidence-report-template.md)
    - [portable-regression-checklist.md](portable/portable-regression-checklist.md)
    - [scripts.md](portable/scripts.md)

### Boot 트랙 : Spring Boot용 (복붙 가능)

- docs/ai/boot/
  - [boot-apply.md](boot/boot-apply.md)
  - [test-stack-boot.md](boot/test-stack-boot.md)

### 클래식 트랙 : 전자정부 3.x/4.x용 (가이드형)

- docs/ai/classic/
  - [classic-apply.md](classic/classic-apply.md)
  - [test-stack-classic.md](classic/test-stack-classic.md)

### 2차 : 프로젝트 지식 (가이드형 템플릿)

- docs/ai/project/
  - [project-overview.md](project/project-overview.md)

- 3차 자동화(삭제됨): Verifier·Guard 등 실행 도구는 대상 프로젝트에서 필요할 때 만든다.
- 4차 운영(삭제됨): 배포·롤백·Health·Canary Runbook은 대상 프로젝트 운영 환경에서 만든다.

이 구조는 문서와 자동화·운영 항목의 위치를 보여준다. 3차·4차 폴더가 현재 없는 것은 의도된 상태다.

## 항목별 기준 (한눈에 보기)

| 항목 | 이식 구분 | 문서 위치 | 상태 |
| --- | --- | --- | --- |
| AGENTS.md | 복붙 후 수정 | [루트 AGENTS.md](../../AGENTS.md) | 부분 미완성 |
| Coding Standard | 가이드형 | [common/coding-standard.md](common/coding-standard.md), [boot/boot-apply.md](boot/boot-apply.md), [classic/classic-apply.md](classic/classic-apply.md) | 공통 마련, Boot 적용됨, 클래식은 관례 기준 |
| SQL Standard | 가이드형 | [common/sql-standard.md](common/sql-standard.md) | 공통 마련 |
| Test | 가이드형(공통) + Boot 복붙 / 클래식 가이드 | [common/test-standard.md](common/test-standard.md), [boot/test-stack-boot.md](boot/test-stack-boot.md), [classic/test-stack-classic.md](classic/test-stack-classic.md) | Boot 적용됨, 클래식은 관례 기준 |
| Verifier / Guard / Snapshot / Baseline / Runtime / DOM / API·DB 확인 | 가이드형 / 선택(도구) | [common/verification-standard.md](common/verification-standard.md) | 방법만 문서화, 도구 미구현(현황 폴더 삭제됨) |
| Acceptance / Evidence | 복붙 가능 | [portable/acceptance-template.md](portable/acceptance-template.md), [portable/evidence-report-template.md](portable/evidence-report-template.md) | 템플릿 구현 |
| SHA / Revision | 복붙 가능(원칙) | [portable/evidence-report-template.md](portable/evidence-report-template.md) | 수동 기록 |
| Migration | 가이드형(절차/현황) | [common/migration-standard.md](common/migration-standard.md), [project/migration.md](project/migration.md) | 절차 구현, 운영 미확인 |
| Project Overview / Architecture / Change Impact / 기능 소유권 / 업무 정책 / API Contract / DB Schema / Error Code / 현행 분석 | 가이드형 템플릿 | [project/](project/project-overview.md) | 양식+예시 마련, 미완성은 문서내 주석 참고 |

상세한 단계별 적용·SI/SM 의미는 각 문서에 있다. 이 표는 위치와 이식 여부만 본다.

상태 표기: `구현됨`은 해당 단계의 방법/양식이 준비됐다는 뜻이고, `부분 미완성`은 일부 프로젝트 정보가 확인됐지만 채울 항목이 남았다는 뜻이다. `미구현`은 이 저장소에서 실행 가능한 도구나 절차를 확인하지 못했다는 뜻이며, `미확인`은 적용 여부나 사실을 판단할 근거가 부족하다는 뜻이다. 링크된 문서에서 미완성 내용과 완성 방법을 확인한다.

## 3차·4차 (삭제됨)

- 3차 자동화: 정해진 기준을 도구가 반복 검사하는 단계. 도구는 대상 프로젝트에서 만든다.
- 4차 운영: 배포·확인·복구를 정하는 단계. Runbook은 대상 운영 환경에서 만든다.
- Canary는 배포 환경이 지원하고 기준이 합의된 경우에만 둔다. 준비되지 않은 항목은 `미확인`으로 유지한다.

## 현재 Arkive 문서 체계 상태

| 단계 | 상태 |
| --- | --- |
| 1차 공통 규칙 | 가이드형 5종 + 양식 4종(common 5 + portable 4). 기술·작업 방식이 달라지면 갱신 가능 |
| Boot/클래식 트랙 | Boot 2종 적용됨(코드 실측). 클래식 2종은 관례 기준, 대상 프로젝트에서 확인 |
| 2차 프로젝트 지식 | 가이드형 템플릿으로 전환됨 (작성 가이드+빈 양식+Arkive 예시). 미완성은 각 문서 HTML 주석 참고 |

Arkive의 미확인 항목은 [프로젝트 지식 문서](project/project-overview.md)에서 확인한다. 확인된 근거와 담당자 검토가 생기면 해당 문서와 이 요약의 상태를 함께 갱신한다.