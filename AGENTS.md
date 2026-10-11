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
- 인수 조건 작성 양식: [acceptance-template.md](docs/ai/portable/acceptance-template.md)
- 변경 완료 증거 양식: [evidence-report-template.md](docs/ai/portable/evidence-report-template.md)
- Verifier, Guard, Baseline, Snapshot, Runtime 확인 방법: [verification-standard.md](docs/ai/common/verification-standard.md)
- DB 마이그레이션 공통 절차: [migration-standard.md](docs/ai/common/migration-standard.md)
- Boot 트랙(복붙 가능): [boot-apply.md](docs/ai/boot/boot-apply.md), [test-stack-boot.md](docs/ai/boot/test-stack-boot.md)
- 클래식 트랙(가이드형, 대상 확인): [classic-apply.md](docs/ai/classic/classic-apply.md), [test-stack-classic.md](docs/ai/classic/test-stack-classic.md)
- 이식 시 스크립트: `verify.cmd`·`doctor.cmd`가 없으면 [scripts.md](docs/ai/portable/scripts.md) 전문으로 생성한다

### 2. 프로젝트 지식

아래 문서는 Arkive에서 확인된 사실 및 미확인 항목을 관리한다. 기능 소유자, 업무 정책, 계약, 오류 코드, 환경별 운영 값은 근거가 확인될 때 갱신한다.

- 개요와 확인된 설정: [project-overview.md](docs/ai/project/project-overview.md)
- 아키텍처와 코드 흐름: [architecture.md](docs/ai/project/architecture.md)
- 기능 및 책임 현황: [function-inventory.md](docs/ai/project/function-inventory.md)
- 기존 코드의 현행 동작 분석: [current-state-analysis.md](docs/ai/project/current-state-analysis.md)
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

공통 검증 절차는 `verification-standard.md`를 따른다. Verifier·Guard 등 실행 도구는 대상 프로젝트에서 만든다.

### 4. 운영

배포·롤백·Health·Canary Runbook은 대상 프로젝트 운영 환경에서 만든다. 담당자가 확인한 값만 기록한다.

## 이식처 적용 (이 문서 세트를 새 프로젝트에 복사했을 때)

별도 지시가 없어도 아래를 수행한다.

1. `doctor.cmd`로 빠진 항목을 확인한다. `verify.cmd`·`doctor.cmd`가 없으면 `docs/ai/portable/scripts.md` 전문으로 생성한다.
2. `project/` 문서의 Arkive 예시를 대상 코드 실측값으로 교체한다 (패키지·테이블·URL·메뉴·DB 제품·Globals 키).
3. Boot·클래식 중 대상에 맞는 트랙만 남기고, 반대 트랙은 지운다.
4. 코드에서 확인 안 되는 값은 `미확인`으로 두고 추측하지 않는다. 확인자는 대상 담당자로 지정한다.
5. `verify.cmd`로 검증하고 실패하면 고친다.

## 작업 원칙

1. 수정 전 관련 화면, JavaScript, Controller, Service, Mapper/XML, SQL, 설정과 호출처를 찾아 데이터 흐름과 기존 패턴을 확인한다.
2. 저장소 상태와 이미 존재하는 변경을 확인하고, 요청 범위에 필요한 최소한만 수정한다. 다른 변경이나 삭제를 덮어쓰지 않는다.
3. 업무 규칙, 상태값, 권한, DB 의미, 외부 API 계약, 담당 조직 또는 운영 기준을 근거 없이 추정하지 않는다.
4. 테스트와 실행 검증은 변경 범위와 위험도에 맞게 선택한다. 수행하지 않은 검증을 수행한 것처럼 보고하지 않는다.
5. DB나 운영 설정을 바꾸기 전 대상 환경, 영향 범위, 데이터 보존, 복구 방법을 확인한다. 비밀값과 운영 데이터를 문서·로그·증거에 노출하지 않는다.
6. 완료 보고에는 변경 파일, 핵심 변경, 실제 검증 결과, 사용한 버전 식별자(SHA, SVN Revision 등)와 남은 제약을 기록한다.
7. 기존 기능을 이관할 때 Arkive의 공통 기능, 의존성, 설정, 보안 및 DB 구조를 확인한 뒤 필요한 부분만 통합한다.
8. Arkive에서 타 프로젝트로 기능을 이식할 때는 기능별 화면·Controller·Service·Mapper/XML·SQL·의존성·설정(Globals 키 등)·DB 객체를 함께 확인하고, 대상 프로젝트의 패키지·테이블·DB 문법에 맞게 최소한만 조정한다.

## 품질 관문 (Quality Gates)

코드를 직접 전부 검토하는 대신, 아래 관문을 통과해야 완료로 처리한다.
스펙(feature 파일, 기대 결과, 임계값)의 작성·승인은 인간 영역이다.
구현 에이전트는 테스트 파일을 수정할 수 없고, 실패하면 코드만 고친다.
테스트 실패를 숨기기 위한 테스트 삭제·skip, 성공을 가장하기 위한 하드코딩·무분별한 Mock, 권한·업무 검증을 약화해 테스트를 통과시키는 변경을 금지한다.
스펙 변경은 인간 승인 후 별도로 수행한다.

| 관문 | 통과 조건 |
| --- | --- |
| 요구사항 | 변경 목적과 업무 규칙이 명시되어 있음 |
| 빌드 | 컴파일 및 패키징 성공 |
| 테스트 | 관련 단위·통합·회귀 테스트 전부 통과 |
| 품질 | 핵심 로직의 커버리지 기준 충족 |
| 테스트 신뢰성 | 중요 로직은 변이 테스트로 검증 (변경 모듈의 핵심 클래스 한정) |
| 보안 | 인증·인가·민감 데이터 접근 규칙 위반 없음 |
| 배포 | 테스트 환경에서 핵심 업무 시나리오 통과 (변경 위험도에 따라 인간 승인 유지) |

## 작업 완료 시 자동 검증

- 작업이 끝나면 루트의 `verify.cmd`를 실행한다 (`verify.cmd [-Dtest=...]`).
- 실패하면 원인을 고치고 다시 실행한다. 실패 상태에서는 작업을 완료했다고 선언하지 않는다.
- 실행하지 못한 검증은 성공했다고 보고하지 않고, 사유와 남은 위험을 기록한다.
- 이 문서는 선언일 뿐 강제력이 없다. 진짜 차단은 `verify.cmd`의 exit 1과 CI 머지 차단이 담당한다.

## 문서 관리 기준

- 공통적인 수행 방법은 공통 표준에, Arkive에서 확인된 구조·정책·계약·환경별 값은 프로젝트 지식 문서에 둔다.
- 템플릿은 작성 형식을 제공하며 프로젝트별 완료 조건이나 실제 값 자체를 대신하지 않는다.
- 프로젝트 지식에 사실을 추가할 때 출처(파일 경로, API 명세, DB 정의, 결정 기록 등)와 확인 상태를 표시한다. 확인되지 않은 내용은 `미확인`으로 명시한다.
- 공통 지침과 프로젝트 사실이 충돌하면 구체적으로 확인된 프로젝트 규칙을 따른다. 보안 및 데이터 무결성 기준은 유지한다.
- 이 지침과 추가 지시의 우선순위는 저장소 루트 `AGENTS.md`를 기준으로 한다.
