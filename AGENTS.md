# Arkive AI 작업 지침

이 파일은 저장소에서 AI가 작업할 때 적용하는 진입 지침이다. 파일명은 Codex 및 관련 도구가 탐색하는 `AGENTS.md`를 사용한다. 작업 전 현재 코드, 설정, 관련 문서와 변경 상태를 확인하고, 확인된 프로젝트 사실과 아래 공통 방법을 구분해 적용한다.

사람을 위한 전체 체계 요약은 [docs/ai/README.md](docs/ai/README.md)를 참고한다.

## 프로젝트 목적 (기능 이식용 레퍼런스)

Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
새 기능은 Arkive에 먼저 축적하고, 타 전자정부프레임워크·Spring·Spring Boot 프로젝트에서 복붙으로 가져갈 수 있게 관리한다.
프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 전제로 한다.

## 작업 체계

작업은 아래 네 층의 자료를 연결한다. 프로젝트 지식은 코드·설정·승인된 업무 자료 등 확인 가능한 근거가 있을 때만 확정한다. 확인되지 않은 규칙은 추측하지 말고 `미확인`으로 남긴다.

### 1. 공통 방법

절차와 템플릿은 공통 표준에 둔다. 실제 업무 조건, URL, 테이블, 운영 임계치는 프로젝트 문서에서 확인한다.

- 코드 구조, 계층, 명명, 검증 및 보안: [coding-standard.md](docs/ai/common/coding-standard.md)
- SQL 및 DB 변경: [sql-standard.md](docs/ai/common/sql-standard.md)
- 테스트 범위와 기록: [test-standard.md](docs/ai/common/test-standard.md)
- 인수 조건 작성 양식: [acceptance-template.md](docs/ai/common/acceptance-template.md)
- 변경 완료 증거 양식: [evidence-report-template.md](docs/ai/common/evidence-report-template.md)
- Verifier, Guard, Baseline, Snapshot, Runtime 확인 방법: [verification-standard.md](docs/ai/common/verification-standard.md)
- DB 마이그레이션 공통 절차: [migration-standard.md](docs/ai/common/migration-standard.md)

### 2. 프로젝트 지식

아래 문서는 Arkive에서 확인된 사실 및 미확인 항목을 관리한다. 기능 소유자, 업무 정책, 계약, 오류 코드, 환경별 운영 값은 근거가 확인될 때 갱신한다.

- 개요와 확인된 설정: [project-overview.md](docs/ai/project/project-overview.md)
- 아키텍처와 코드 흐름: [architecture.md](docs/ai/project/architecture.md)
- 기능 및 책임 현황: [function-inventory.md](docs/ai/project/function-inventory.md)
- 기존 코드의 현행 동작 분석: [current-state-analysis.md](docs/ai/project/current-state-analysis.md)
- 업무 정책: [business-policy.md](docs/ai/project/business-policy.md)
- API 경로와 계약 현황: [api-contract.md](docs/ai/project/api-contract.md)
- DB 스키마와 초기화 상태: [database-schema.md](docs/ai/project/database-schema.md)
- 변경 영향 추적 기준: [change-impact.md](docs/ai/project/change-impact.md)
- 오류 처리 및 코드 현황: [error-code.md](docs/ai/project/error-code.md)
- DB 마이그레이션 현황: [migration.md](docs/ai/project/migration.md)

이 문서들은 프로젝트 사실을 기록하는 장소이지 새로운 규칙을 임의로 만드는 근거가 아니다. 변경 시 근거 파일이나 담당자 확인을 함께 남긴다.

각 프로젝트 지식 문서의 첫 부분에는 HTML 주석으로 문서 상태를 적는다. 상태는 `완료`, `부분 미완성`, `미확인`, `해당 없음` 중에서 고른다. 부분 미완성/미확인 문서에는 필요한 근거, 확인 담당자 또는 출처, 다음 완성 방법, 완료 기준을 구체적으로 적는다. HTML 주석은 화면 렌더링에서는 숨겨져도 AI와 편집자는 읽을 수 있다.

```html
<!--
문서 상태: 부분 미완성
미완성 항목:
필요한 근거/확인자:
완성 방법:
완료 기준:
-->
```

### 3. 자동 검증 및 변경 통제

공통 검증 절차는 `verification-standard.md`, 코드/SQL/테스트별 절차는 각 공통 표준을 따른다. 자동화 도구가 실제로 구성되어 있는지 먼저 확인한다. 문서만으로 존재하지 않는 Verifier, Guard, Baseline, Snapshot 또는 Runtime Test 자동화가 있다고 가정하지 않는다.

Arkive 자동화 도구의 확인/구현 현황은 [자동화 현황](docs/ai/automation/README.md)을 기준으로 한다.

### 4. 운영

배포, 마이그레이션, 롤백, Canary, Health 점검은 실제 프로젝트의 배포·운영 환경에서 지원하는 항목만 문서화하고 수행한다. 점진 배포가 없는 환경에 Canary 절차를 억지로 적용하지 않는다. Health 기준과 중단 임계치는 담당자가 확인한 값만 기록한다.

운영 절차의 확인 상태와 기록 위치는 [운영 현황 및 runbook 안내](docs/ai/operations/README.md)를 기준으로 한다.

## 작업 원칙

1. 수정 전 관련 화면, JavaScript, Controller, Service, Mapper/XML, SQL, 설정과 호출처를 찾아 데이터 흐름과 기존 패턴을 확인한다.
2. 저장소 상태와 이미 존재하는 변경을 확인하고, 요청 범위에 필요한 최소한만 수정한다. 다른 변경이나 삭제를 덮어쓰지 않는다.
3. 업무 규칙, 상태값, 권한, DB 의미, 외부 API 계약, 담당 조직 또는 운영 기준을 근거 없이 추정하지 않는다.
4. 테스트와 실행 검증은 변경 범위와 위험도에 맞게 선택한다. 수행하지 않은 검증을 수행한 것처럼 보고하지 않는다.
5. DB나 운영 설정을 바꾸기 전 대상 환경, 영향 범위, 데이터 보존, 복구 방법을 확인한다. 비밀값과 운영 데이터를 문서·로그·증거에 노출하지 않는다.
6. 완료 보고에는 변경 파일, 핵심 변경, 실제 검증 결과, 사용한 버전 식별자(SHA, SVN Revision 등)와 남은 제약을 기록한다.
7. 기존 기능을 이관할 때 Arkive의 공통 기능, 의존성, 설정, 보안 및 DB 구조를 확인한 뒤 필요한 부분만 통합한다.
8. Arkive에서 타 프로젝트로 기능을 이식할 때는 기능별 화면·Controller·Service·Mapper/XML·SQL·의존성·설정(Globals 키 등)·DB 객체를 함께 확인하고, 대상 프로젝트의 패키지·테이블·DB 문법에 맞게 최소한만 조정한다.

## 문서 관리 기준

- 공통적인 수행 방법은 공통 표준에, Arkive에서 확인된 구조·정책·계약·환경별 값은 프로젝트 지식 문서에 둔다.
- 템플릿은 작성 형식을 제공하며 프로젝트별 완료 조건이나 실제 값 자체를 대신하지 않는다.
- 프로젝트 지식에 사실을 추가할 때 출처(파일 경로, API 명세, DB 정의, 결정 기록 등)와 확인 상태를 표시한다. 확인되지 않은 내용은 `미확인`으로 명시한다.
- 공통 지침과 프로젝트 사실이 충돌하면 구체적으로 확인된 프로젝트 규칙을 따른다. 보안 및 데이터 무결성 기준은 유지한다.
- 이 지침과 추가 지시의 우선순위는 저장소 루트 `AGENTS.md`를 기준으로 한다.
