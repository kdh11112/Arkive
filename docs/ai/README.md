# SI/SM 웹개발 AI 작업 체계

> Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
> 새 기능은 Arkive에 축적하고, 타 전자정부프레임워크·Spring·Spring Boot 프로젝트에서 복붙으로 가져가는 것을 전제로 한다.
> 프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 허용한다.
>
> **이식만 할 때는 [PORTING.md](../../PORTING.md)를 먼저 본다.** 이 문서는 전체 체계를 설명하는 안내다.

핵심 원칙은 간단하다.

> **작업 방법과 기록 형식은 공통으로 정하고, 업무 규칙·API·DB·운영 값은 해당 프로젝트의 근거로 채운다.**

## 이식 구분 (복붙 기준)

| 구분 | 의미 | 해당 문서 |
| --- | --- | --- |
| 공통 방법 | 파일째 복사하되 본문 판단해서 적용 | `common/` 5종 (coding/sql/test/verification/migration-standard) |
| 복붙 양식 | 빈 양식·스크립트·이식 시트 | `portable/` 8종 (acceptance/evidence-template, regression-checklist, scripts, project-template-pack, feature-porting-sheet, test-porting-sheet, ai-porting-request) |
| Boot 트랙 | Boot 프로젝트에 그대로 복사 | `boot/` 2종 (boot-apply, test-stack-boot) |
| 클래식 트랙 | 클래식 관례 기준. 대상에서 확인 후 적용 | `classic/` 2종 (classic-apply, test-stack-classic) |
| **Arkive 기록** | **대상 프로젝트로 복사하지 않는다** | `project/` 9종 |
| 선택 | 필요할 때 대상 프로젝트에서 작성 | 3차 자동화(Verifier/Guard/Snapshot/Runtime), 4차 운영(Runbook) |

`project/` 9종은 Arkive의 현재 구현·분석 기록이다. 대상 프로젝트의 프로젝트 지식은
[project-template-pack.md](portable/project-template-pack.md)의 빈 양식으로 새로 채운다.

## 체계 한눈에 보기

### 1차 : 공통 규칙 + 복붙 양식

- [AGENTS.md](../../AGENTS.md)
- [PORTING.md](../../PORTING.md) — 이식 실행 매뉴얼
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
  - [project-template-pack.md](portable/project-template-pack.md)
  - [feature-porting-sheet.md](portable/feature-porting-sheet.md)
  - [test-porting-sheet.md](portable/test-porting-sheet.md)
  - [ai-porting-request.md](portable/ai-porting-request.md)

### Boot 트랙 : Spring Boot용

- [boot-apply.md](boot/boot-apply.md)
- [test-stack-boot.md](boot/test-stack-boot.md)

### 클래식 트랙 : 전자정부 3.x/4.x용

- [classic-apply.md](classic/classic-apply.md)
- [test-stack-classic.md](classic/test-stack-classic.md)

### Arkive 기록 (복사 금지)

- [project-overview.md](project/project-overview.md)
- [architecture.md](project/architecture.md)
- [function-inventory.md](project/function-inventory.md)
- [current-state-analysis.md](project/current-state-analysis.md)
- [api-contract.md](project/api-contract.md)
- [database-schema.md](project/database-schema.md)
- [change-impact.md](project/change-impact.md)
- [error-code.md](project/error-code.md)
- [migration.md](project/migration.md)

## 항목별 기준 (한눈에 보기)

| 항목 | 이식 구분 | 문서 위치 | 상태 |
| --- | --- | --- | --- |
| AGENTS.md | 복붙 후 수정 | [AGENTS.md](../../AGENTS.md) | 부분 미완성 |
| 이식 실행 | 복붙 | [PORTING.md](../../PORTING.md) | 마련 |
| Coding Standard | 가이드형 | [common/coding-standard.md](common/coding-standard.md), [boot/boot-apply.md](boot/boot-apply.md), [classic/classic-apply.md](classic/classic-apply.md) | 공통 마련, Boot 적용됨, 클래식은 관례 기준 |
| SQL Standard | 가이드형 | [common/sql-standard.md](common/sql-standard.md) | 공통 마련 |
| Test | 가이드형(공통) + Boot 복붙 / 클래식 가이드 | [common/test-standard.md](common/test-standard.md), [boot/test-stack-boot.md](boot/test-stack-boot.md), [classic/test-stack-classic.md](classic/test-stack-classic.md) | Boot 적용됨, 클래식은 관례 기준 |
| Verifier / Guard / Snapshot / Runtime | 가이드형 / 선택 | [common/verification-standard.md](common/verification-standard.md) | 방법만 문서화, 도구 미구현 |
| Acceptance / Evidence | 복붙 가능 | [portable/acceptance-template.md](portable/acceptance-template.md), [portable/evidence-report-template.md](portable/evidence-report-template.md) | 템플릿 구현 |
| 프로젝트 지식 | 복붙 양식 | [portable/project-template-pack.md](portable/project-template-pack.md) | 대상에서 채움 |
| Migration | 가이드형(절차/현황) | [common/migration-standard.md](common/migration-standard.md), [project/migration.md](project/migration.md) | 절차 구현, 운영 미확인 |

상태 표기: `구현됨`은 방법/양식이 준비됐다는 뜻이고, `부분 미완성`은 일부 프로젝트 정보가 확인됐지만 채울 항목이 남았다는 뜻이다. `미구현`은 실행 가능한 도구나 절차를 확인하지 못했다는 뜻이며, `미확인`은 사실 판단 근거가 부족하다는 뜻이다.

## 현재 Arkive 문서 체계 상태

| 단계 | 상태 |
| --- | --- |
| 1차 공통 규칙 | common 5 + portable 6. 기술·작업 방식이 바뀌면 갱신 |
| Boot/클래식 트랙 | Boot 2종 적용됨(코드 실측). 클래식 2종은 관례 기준, 대상에서 확인 |
| Arkive 기록 | project 9종. 복사 금지, 구현·분석 근거로 유지 |
| 테스트 | JUnit5 105개 통과, JaCoCo 게이트는 검증된 일부 클래스만 강제, GitHub Actions `verify` 추가 |
| 3차·4차 | 도구·Runbook은 대상 프로젝트에서 필요할 때 작성 |

## 테스트 현황

- 전체: 105개 테스트 통과 (`ArkiveSmokeTest`, `BoardMapperTest`, `BoardCommentControllerTest`, `BoardNoticeMapperTest`, `BoardNoticeControllerTest`, `BoardNotificationEventListenerTest`, `BoardNotificationControllerTest`, `MessageMapperTest`, `MessageControllerTest`, `HtmlSanitizerTest`, `FileTypeUtilTest`, `JwtUtilTest`, `LoginSessionRegistryTest`, `LoginSessionControllerTest`, `AuthInterceptorTest`, `ErrorResponseTest`, `PermissionMapperTest`, `RoleManageControllerTest`, `MenuPermissionControllerTest`, `SitemapControllerTest`, `SearchMapperTest`, `SearchServiceImplTest`, `SearchControllerTest`, `MailServiceImplTest`, `PasswordResetControllerTest`)
- 커버리지: instruction 16.5%, branch 15.0%, line 17.4% (2026-10-24 `verify.cmd` 기준). 전체 19k LOC 대비 아직 낮다.
- JaCoCo `check`는 `HtmlSanitizer`, `FileTypeUtil`, `JwtUtil`, `AuthInterceptor`, `MessageController`, `MessageServiceImpl`, `BoardServiceImpl`, `MailServiceImpl`, `BoardNotificationEventListener`, `SearchServiceImpl`만 검사한다.
- 테스트 DB는 `target/test-db`, 포트 9002를 써서 앱(9001) 실행 중에도 돈다.
- 상세: [test-stack-boot.md](boot/test-stack-boot.md)

## 3차·4차 (대상 프로젝트에서 작성)

- 3차 자동화: 정해진 기준을 도구가 반복 검사하는 단계. 도구는 대상 프로젝트에서 만든다.
- 4차 운영: 배포·확인·복구를 정하는 단계. Runbook은 대상 운영 환경에서 만든다.
- Canary는 배포 환경이 지원하고 기준이 합의된 경우에만 둔다. 준비되지 않은 항목은 `미확인`으로 유지한다.

Arkive의 미확인 항목은 [프로젝트 지식 문서](project/project-overview.md)에서 확인한다. 확인된 근거와 담당자 검토가 생기면 해당 문서와 이 요약의 상태를 함께 갱신한다.
