# SI/SM 웹개발 AI 작업 체계

이 문서는 AI와 개발자가 함께 일할 때 **무엇을 공통 기준으로 삼고, 어떤 정보는 프로젝트마다 확인해야 하는지** 한눈에 보기 위한 안내다.

핵심 원칙은 간단하다.

> **작업 방법과 기록 형식은 공통으로 정하고, 업무 규칙·API·DB·운영 값은 해당 프로젝트의 근거로 채운다.**

루트 [AGENTS.md](../../AGENTS.md)는 AI가 먼저 읽는 작업 지침이다. 이 페이지는 사람을 위한 체계 개요이며 세부 규칙은 아래 연결 문서가 기준이다.

## 4단계 한눈에 보기

### [1차 : 공통 규칙](#coding-standard)

- [AGENTS.md](#agentsmd)
  - docs/ai/common/
    - [coding-standard.md](#coding-standard)
    - [sql-standard.md](#sql-standard)
    - [test-standard.md](#test)
    - [acceptance-template.md](#acceptance)
    - [evidence-report-template.md](#evidence)
    - [verification-standard.md](#verifier)
    - [migration-standard.md](#migration)

### [2차 : 프로젝트 지식](#project-overview)

- docs/ai/project/
  - [project-overview.md](#project-overview)
  - [architecture.md](#architecture)
  - [function-inventory.md](#기능-소유권)
  - [business-policy.md](#업무-정책)
  - [api-contract.md](#api-contract)
  - [database-schema.md](#db-schema)
  - [change-impact.md](#change-impact)
  - [error-code.md](#error-code)
  - [current-state-analysis.md](#현행-분석-as-is)
  - [migration.md](#migration)

### [3차 : AI 자동화](#자동화-도구-구현)

- [docs/ai/automation/README.md](#자동화-도구-구현) (현황 및 구현 기준)
  - [verifier](#verifier)
  - [guard](#guard)
  - [baseline](#baseline)
  - [snapshot](#snapshot)
  - [runtime verification](#runtime-test)

### [4차 : 운영](#health)

- [docs/ai/operations/README.md](#health) (현황 및 Runbook 기준)
  - [health](#health)
  - [error monitoring](#error-monitoring)
  - [migration](#migration)
  - [deployment](#deployment)
  - [rollback](#rollback)
  - [canary (지원 프로젝트만)](#canary)

이 구조는 문서와 자동화·운영 항목의 위치를 보여준다. 3차 도구와 4차 Runbook이 실제 구현되었는지는 아래 현황에서 따로 확인한다.

## 항목별 기준

각 항목은 단계별로 한 줄씩 표시했습니다. 단계가 적용되지 않는 항목은 해당 단계만 적습니다.

### `AGENTS.md`

**단계별 적용**
- 1차: 공통 지침 틀 작성
- 2차: Arkive별 규칙·경로 입력

- **공통/프로젝트 구분:** 형식은 공통, 지침 내용은 프로젝트별
- **SI/SM 의미:** AI에게 저장소 규칙, 탐색 방법, 우선순위를 전달
- **문서 위치:** [루트 AGENTS.md](../../AGENTS.md)
- **현재 상태·다음 단계:** [부분 미완성: 프로젝트 지식 문서의 주석 참고](#project-overview)

### Coding Standard

**단계별 적용**
- 1차: 기존 코드 스타일·계층별 구현 원칙 정의
- 2차: 프로젝트의 실제 구조와 예외 규칙 확인

- **공통/프로젝트 구분:** 기본 작성 방법은 공통, 실제 구조와 예외는 프로젝트별
- **SI/SM 의미:** 기존 시스템의 일관성과 유지보수성을 지키며 변경
- **문서 위치:** [코딩 표준](common/coding-standard.md), [프로젝트 구조](project/architecture.md)
- **현재 상태·다음 단계:** [공통 기준 마련; 프로젝트 구조 대조 필요](common/coding-standard.md)

### SQL Standard

**단계별 적용**
- 1차: 안전한 SQL·Mapper 작성 기준 정의
- 2차: DB 종류·스키마·프로젝트 Mapper 규칙 확인

- **공통/프로젝트 구분:** 작성 방법은 공통, DB 종류·스키마·실제 매퍼 구조는 프로젝트별
- **SI/SM 의미:** DB 정합성·성능·보안을 고려해 SQL 변경
- **문서 위치:** [SQL 표준](common/sql-standard.md), [DB 스키마](project/database-schema.md)
- **현재 상태·다음 단계:** [공통 기준 마련; 운영 스키마와 대조 필요](common/sql-standard.md)
### Test

**단계별 적용**
- 1차: 공통 테스트 방법·기록 형식
- 2차: 기능별 케이스·기대값 정의
- 3차: 반복 검사를 도구로 실행(선택)

- **공통/프로젝트 구분:** 실행·기록 방법은 공통, 케이스·기대값은 프로젝트별, 자동화는 선택
- **SI/SM 의미:** 기능 요구와 예외 조건이 맞는지 검증
- **문서 위치:** [테스트 표준](common/test-standard.md), [Acceptance](common/acceptance-template.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [부분 미완성: 테스트 케이스/자동화 없음](automation/README.md)

### Verifier

**단계별 적용**
- 1차: 결과 비교 방법 정의
- 2차: 정책·계약·Acceptance를 정답 기준으로 지정
- 3차: 실제/기대 결과 비교기 구현(선택)

- **공통/프로젝트 구분:** 방법은 공통, 정답 기준은 프로젝트별, 실행기는 자동화 단계
- **SI/SM 의미:** 실제 결과를 업무 정책·계약·Acceptance와 비교
- **문서 위치:** [검증 방법](common/verification-standard.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [자동화 미구현](automation/README.md)

### Guard

**단계별 적용**
- 1차: 위험 검토 방법 정의
- 2차: 프로젝트별 차단 규칙 결정
- 3차: CI/Hook 검사와 차단 동작 구현(선택)

- **공통/프로젝트 구분:** 검사 방법은 공통, 차단 규칙은 프로젝트별, CI/Hook 적용은 자동화 단계
- **SI/SM 의미:** 보안·데이터 손실·호환성 위험 변경을 사전 차단
- **문서 위치:** [검증 방법](common/verification-standard.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [자동화 미구현; 차단 규칙도 미완성](automation/README.md)

### Snapshot

**단계별 적용**
- 1차: 보존·마스킹 방법 정의
- 2차: 보존 대상·접근·기간 결정
- 3차: 수집/비교 도구 구현(선택)

- **공통/프로젝트 구분:** 보존 방법은 공통, 대상·접근·보관 기준은 프로젝트별, 수집기는 선택 자동화
- **SI/SM 의미:** 변경 전 화면·응답·데이터 등의 상태를 보존
- **문서 위치:** [검증 방법](common/verification-standard.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [수집기 미구현; 보관 기준 미확정](automation/README.md)

### Runtime Test

**단계별 적용**
- 1차: 실행·기록 방법 정의
- 2차: 검증 URL·계정·데이터 지정
- 3차: Runtime 검사 실행/자동화

- **공통/프로젝트 구분:** 실행·기록 방법은 공통, 실행 대상은 프로젝트별, 실행기는 선택 자동화
- **SI/SM 의미:** 소스 검토를 넘어 실제 실행 결과를 확인
- **문서 위치:** [검증 방법](common/verification-standard.md), [테스트 표준](common/test-standard.md)
- **현재 상태·다음 단계:** [자동 실행 미구현; 환경·대상 미확정](automation/README.md)

### DOM

**단계별 적용**
- 1차: 브라우저 확인 방법 정의
- 2차: 화면·DOM·사용자 동작 지정
- 3차: 브라우저 검사 자동화(선택)

- **공통/프로젝트 구분:** 확인 방법은 공통, 화면·컴포넌트와 검증 조건은 프로젝트별
- **SI/SM 의미:** 브라우저에서 렌더링과 사용자 동작을 검증
- **문서 위치:** [테스트 표준](common/test-standard.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [화면 기준·브라우저 자동화 미완성](project/current-state-analysis.md), [자동화 현황](automation/README.md)

### API 확인

**단계별 적용**
- 1차: API 검증 방법 정의
- 2차: URL·계정·계약·기대 응답 지정
- 3차: API 검사 실행/자동화

- **공통/프로젝트 구분:** 확인 방법은 공통, URL·계정·계약은 프로젝트별
- **SI/SM 의미:** Request, Response, 상태 코드와 오류를 확인
- **문서 위치:** [API 계약](project/api-contract.md), [검증 방법](common/verification-standard.md)
- **현재 상태·다음 단계:** [계약·테스트 대상 부분 미완성](project/api-contract.md), [검증기 미구현](automation/README.md)

### DB 확인

**단계별 적용**
- 1차: DB 검증 방법 정의
- 2차: DB·객체·기대 데이터·안전 조건 지정
- 3차: SQL 검사 실행/자동화

- **공통/프로젝트 구분:** 확인 방법은 공통, DB·객체·기대 데이터는 프로젝트별
- **SI/SM 의미:** SQL 결과, 영향 건수와 데이터 정합성을 확인
- **문서 위치:** [DB 스키마](project/database-schema.md), [SQL 표준](common/sql-standard.md)
- **현재 상태·다음 단계:** [운영 스키마·검증 환경 미완성](project/database-schema.md), [DB 검사기 미구현](automation/README.md)

### Acceptance

**단계별 적용**
- 1차: 인수 조건 양식 제공
- 2차: 기능별 완료·검수 조건과 승인자 정의

- **공통/프로젝트 구분:** 양식은 공통, 완료 조건은 프로젝트·기능별
- **SI/SM 의미:** 개발 완료 및 업무 검수 기준을 정함
- **문서 위치:** [Acceptance 템플릿](common/acceptance-template.md)
- **현재 상태·다음 단계:** [공통 템플릿 구현; 기능별 작성 필요](common/acceptance-template.md)

### Evidence

**단계별 적용**
- 1차: 증거 기록 양식 제공
- 3차/4차: 작업 검증·운영 결과와 자료 기록

- **공통/프로젝트 구분:** 기록 방법은 공통, 결과·자료는 작업별
- **SI/SM 의미:** 변경과 검증이 실제로 끝났다는 근거를 남김
- **문서 위치:** [Evidence 템플릿](common/evidence-report-template.md)
- **현재 상태·다음 단계:** [공통 템플릿 구현; 작업별 작성 필요](common/evidence-report-template.md)

### SHA / Revision

**단계별 적용**
- 1차: 버전 식별 원칙 정의
- 2차: Git/SVN 등 사용 도구 확인
- 3차: 검증 빌드와 버전 연결
- 4차: 배포 버전 연결

- **공통/프로젝트 구분:** 식별 원칙은 공통, 실제 버전관리 도구는 프로젝트별
- **SI/SM 의미:** 코드와 검증·배포 버전을 연결해 추적
- **문서 위치:** [Evidence 템플릿](common/evidence-report-template.md), [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [수동 기록 기준 있음; 자동 연결 미구현](automation/README.md)
### Migration

**단계별 적용**
- 1차: 안전한 변경 절차 정의
- 2차: DB 도구·스크립트·승인·복구 기준 확인
- 4차: 운영 DB 적용·사후 검증

- **공통/프로젝트 구분:** 공통 절차, 프로젝트 도구·스크립트·승인·운영 순서는 프로젝트별
- **SI/SM 의미:** 데이터 보존을 고려한 DB 변경과 이력 관리
- **문서 위치:** [공통 절차](common/migration-standard.md), [프로젝트 현황](project/migration.md), [운영 현황](operations/README.md)
- **현재 상태·다음 단계:** [공통 절차 구현; 운영 Runbook 미완성](project/migration.md), [운영 현황](operations/README.md)

### Project Overview

**단계별 적용**
- 2차: 시스템 목적·범위·사용자·기술 스택과 환경 현황 기록

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 시스템을 이해하는 데 필요한 기본 사실과 범위를 공유
- **문서 위치:** [프로젝트 개요](project/project-overview.md)
- **현재 상태·다음 단계:** [미완성 항목은 문서 내 주석 확인](project/project-overview.md)

### Architecture

**단계별 적용**
- 2차: 실제 계층·호출 흐름·연계·배포 구조를 근거와 함께 기록

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 변경 위치와 영향 범위를 판단할 구조적 근거 제공
- **문서 위치:** [아키텍처](project/architecture.md)
- **현재 상태·다음 단계:** [미완성 항목은 문서 내 주석 확인](project/architecture.md)

### Change Impact

**단계별 적용**
- 2차: 기능·화면·API·DB 간 영향 관계와 조사 근거 기록

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 변경 전 연관 기능과 회귀 위험을 빠뜨리지 않도록 함
- **문서 위치:** [변경 영향 분석](project/change-impact.md)
- **현재 상태·다음 단계:** [미완성 항목은 문서 내 주석 확인](project/change-impact.md)
### 기능 소유권

**단계별 적용**
- 2차: 기능별 책임 팀·담당자·경계 조사 및 확인

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 기능·모듈의 책임 팀과 담당 범위를 명확히 함
- **문서 위치:** [기능 목록](project/function-inventory.md)
- **현재 상태·다음 단계:** [부분 미완성: 소유자·전체 목록 확인 필요](project/function-inventory.md)

### 업무 정책

**단계별 적용**
- 2차: 담당자가 업무 규칙·상태 전이·권한을 근거와 함께 확정

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 상태 전이, 권한, 예외 등 실제 업무 규칙
- **문서 위치:** [업무 정책](project/business-policy.md)
- **현재 상태·다음 단계:** [미확인: 업무 담당자 확인 필요](project/business-policy.md)

### API Contract

**단계별 적용**
- 2차: 제공자·호출자가 Request/Response·오류·호환성을 합의

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 호출자와 제공자 사이의 경로·입출력·오류 약속
- **문서 위치:** [API 계약](project/api-contract.md)
- **현재 상태·다음 단계:** [부분 미완성: 계약 명세 필요](project/api-contract.md)

### DB Schema

**단계별 적용**
- 2차: 환경별 DB DDL·컬럼 의미·제약조건·데이터 기준 확인

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 대상 환경의 테이블·컬럼·제약조건과 데이터 의미
- **문서 위치:** [DB 스키마](project/database-schema.md)
- **현재 상태·다음 단계:** [부분 미완성: 현재는 개발 초기화 SQL만 정리](project/database-schema.md)

### Error Code

**단계별 적용**
- 2차: 오류 코드·메시지·HTTP 매핑·소유자 확정

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 오류 코드, 메시지와 API 응답 매핑을 정의
- **문서 위치:** [오류 체계](project/error-code.md)
- **현재 상태·다음 단계:** [미확인: 공식 오류 체계 여부 확인 필요](project/error-code.md)

### Baseline

**단계별 적용**
- 2차: 기능별 버전·환경·데이터 비교 기준 지정
- 3차: 기준 저장·비교 자동화(선택)

- **공통/프로젝트 구분:** 기준은 프로젝트·기능별, 저장·비교 도구는 자동화 단계
- **SI/SM 의미:** 비교할 코드 버전, 환경, 데이터 상태를 정함
- **문서 위치:** [검증 방법](common/verification-standard.md), 작업별 Evidence
- **현재 상태·다음 단계:** [기준·저장 방식 부분 미완성](automation/README.md)

### 현행 분석 (As-Is)

**단계별 적용**
- 2차: 기존 화면·업무 흐름·연계 동작을 근거로 조사

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 기존 시스템의 화면·업무·연계 동작을 근거와 함께 파악
- **문서 위치:** [현행 분석](project/current-state-analysis.md)
- **현재 상태·다음 단계:** [부분 미완성: 일부 코드 흐름만 조사](project/current-state-analysis.md)

### Health

**단계별 적용**
- 2차: 점검 지표·수준·임계치·담당자 합의
- 4차: 운영 모니터링과 대응

- **공통/프로젝트 구분:** 점검 방법은 공통, 지표·수준·임계치는 프로젝트별
- **SI/SM 의미:** 서비스 생존·준비 상태와 장애 징후를 확인
- **문서 위치:** [운영 현황](operations/README.md)
- **현재 상태·다음 단계:** [미확인: Health 기준과 endpoint 설정 여부](operations/README.md)

### 자동화 도구 구현

**단계별 적용**
- 3차: Verifier·Guard·Snapshot·Baseline·Runtime 검사 도구와 CI/실행 설정 구현

- **공통/프로젝트 구분:** 공통 실행 방식, 프로젝트별 기준·환경 설정
- **SI/SM 의미:** 반복 검증과 위험 변경 차단을 자동 실행
- **문서 위치:** [자동화 현황](automation/README.md)
- **현재 상태·다음 단계:** [미구현: 실행 스크립트/CI 연동 없음](automation/README.md)

### Error Monitoring

**단계별 적용**
- 4차: 로그·대시보드·알림 경로와 대응자를 정해 운영

- **공통/프로젝트 구분:** 방법은 공통, 로그·대시보드·알림·담당은 프로젝트별
- **SI/SM 의미:** 운영 중 오류를 탐지하고 대응자에게 전달
- **문서 위치:** [오류 체계](project/error-code.md), [운영 현황](operations/README.md)
- **현재 상태·다음 단계:** [미확인: 모니터링 경로·대응자 없음](operations/README.md)

### Deployment

**단계별 적용**
- 4차: 환경별 승인·배포 순서·사후 확인 Runbook 작성 및 실행

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 환경별 승인, 배포 순서와 사후 확인을 수행
- **문서 위치:** [운영 현황](operations/README.md)
- **현재 상태·다음 단계:** [미확인: 저장소에서 배포 환경·절차 확인 필요](operations/README.md)

### Rollback

**단계별 적용**
- 4차: 실패 조건·복구 절차·담당·검증 Runbook 작성 및 확인

- **공통/프로젝트 구분:** 프로젝트별
- **SI/SM 의미:** 실패 시 복구 조건·절차·담당과 데이터 영향을 관리
- **문서 위치:** [운영 현황](operations/README.md), [Migration](project/migration.md)
- **현재 상태·다음 단계:** [미확인: 복구 절차·책임자 확인 필요](operations/README.md)

### Canary

**단계별 적용**
- 4차: 배포 플랫폼 지원과 관찰·진행·중단 기준이 확인된 경우에만 적용

- **공통/프로젝트 구분:** 지원 프로젝트에만 적용
- **SI/SM 의미:** 일부 트래픽으로 안전성을 관찰한 뒤 확대 또는 중지
- **문서 위치:** [운영 현황](operations/README.md)
- **현재 상태·다음 단계:** [미확인: 지원 여부부터 확인 필요](operations/README.md)


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
| 2차 프로젝트 지식 | 문서 구조와 확인된 일부 사실이 기록됨. 각 문서 HTML 주석에 미완성 항목과 완성 방법이 적혀 있음 |
| 3차 AI 자동화 | 공통 방법과 현황표만 마련됨. 실제 Verifier/Guard/Runtime 자동 도구는 확인되지 않음 |
| 4차 운영 | 현황과 기록 기준만 마련됨. 배포·롤백·운영 임계치·Canary 실행 Runbook은 미확인 |

Arkive의 미확인 항목은 [프로젝트 지식 문서](#project-overview)와 [운영 현황](operations/README.md), [자동화 현황](automation/README.md)에서 확인한다. 확인된 근거와 담당자 검토가 생기면 해당 문서와 이 요약의 상태를 함께 갱신한다.
