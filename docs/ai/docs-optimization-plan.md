# Arkive 문서 구조 최적화 계획 및 결과 (B안 적용 완료)

> **상태:** ✅ **B안(기능 중심 대폭 통합) 적용 완료** (2026-11-13)  
> **검증:** `verify.cmd` 338/338 테스트 및 JaCoCo 기준 통과 (`BUILD SUCCESS`)

---

## 1. 적용 완료 결과 (B안 기준)

사용자 결정("통크게 가자 B안으로")에 따라 **B안: 기능 중심 대폭 통합**이 적용되었습니다.

### 1.1. `docs/ai/project/` 재편 (10개 파일 → 4개 파일)

```
docs/ai/project/
├── project-context.md     ← 개요 + 실행설정 + 아키텍처(Mermaid 흐름도) + 공유지점 + 오류체계
├── feature-catalog.md     ← 기능별 상세·주의점 + API 인벤토리·JSON동작 + 테스트현황 + 알림설정 + 이식묶음
├── verification-log.md    ← 확인된 기능흐름 + 날짜별 검증이력 + 작업별 변경영향 + DB마이그레이션 현황/사용법
└── database-schema.md     ← 최신 DB 스키마·테이블·시드 정의 (유지 및 내부 링크 갱신)
```

| 신규 통합 파일 | 병합된 기존 파일 | 크기 | 주요 역할 |
|---|---|---|---|
| `project-context.md` | `project-overview.md`<br>`architecture.md`<br>`change-impact.md` (공유지점)<br>`error-code.md` | ~16 KB | 시스템 전반의 기술 스택, 실행 설정, 아키텍처 다이어그램, 공유 컴포넌트, 에러 규약 |
| `feature-catalog.md` | `feature-status.md`<br>`function-inventory.md`<br>`api-contract.md` | ~38 KB | 기능 구현 현황(3단 분류), 경로 인벤토리(72개 행), JSON 동작 계약, 테스트 자산, 이식 묶음 |
| `verification-log.md` | `current-state-analysis.md`<br>`migration.md`<br>`change-impact.md` (작업기록) | ~19 KB | 기능 흐름, 날짜별 GUI/전환 검증 이력, 작업별 영향 표, DB 마이그레이션 및 개발자 사용법 |
| `database-schema.md` | (기존 유지) | ~26 KB | 테이블/컬럼/인덱스 상세 정의 및 시드 내역 |

- **기존 구 파일 9개 안전 삭제 완료**
- **저장소 전체 링크 무결성 검증 완료** (깨진 링크 0건)
- **`docs/ai/README.md` (문서 지도) 최적화 완료**
- **루트 `README.md` 링크 최신화 완료** (기능 체크리스트 100% 보존)

---

## 2. 기존 현황 분석 (최적화 전)

최적화 전 `docs/ai/` 아래에 **28개 마크다운 파일**, 총 **약 280 KB**가 있었습니다.

| 디렉토리 | 파일 수 | 합계 크기 | 역할 |
|---|---|---|---|
| `project/` | 10개 | **207 KB** | Arkive 전용 기록 (복사 금지) |
| `common/` | 5개 | **30 KB** | 공통 표준 (이식 시 복사) |
| `portable/` | 8개 | **28 KB** | 빈 양식·스크립트 (이식 시 복사) |
| `boot/` | 2개 | **10 KB** | Boot 트랙 |
| `classic/` | 2개 | **5 KB** | 클래식 트랙 |
| `README.md` | 1개 | **5 KB** | 문서 지도 |

> [!NOTE]
> 전체 280 KB 중 **74%** (207 KB)가 `project/` 디렉토리에 집중되어 있었습니다.

---

## 3. 핵심 문제점 (개선 전 분석)

### 3.1. `project/` 안의 문서 간 내용 중복이 심각

가장 큰 4개 파일이 같은 기능을 서로 다른 관점에서 반복 기술했습니다:

| 파일 | 크기 | 주요 내용 |
|---|---|---|
| `feature-status.md` | 34 KB | 기능별 **동작·주의점·테스트 범위** |
| `api-contract.md` | 34 KB | 기능별 **API 경로·입출력** |
| `current-state-analysis.md` | 33 KB | 기능별 **코드 흐름·GUI 확인 이력** |
| `function-inventory.md` | 31 KB | 기능별 **코드 위치·테스트 자산·이식 의존성** |

예를 들어 "쪽지관리" 하나를 확인하려면 4개 파일 모두를 찾아보아야 했으며, 각 파일마다 Controller·Service·Mapper·테스트를 중복 나열하고 있었습니다.

### 3.2. 문서 수가 필요 이상으로 많음

- `project-overview.md`와 `architecture.md`는 기술 스택·계층·설정을 다루며 내용이 크게 중복됨
- `change-impact.md`의 "공유 지점" 표와 "작업별 기록" 표가 분리되어 있어 맥락 단절
- `error-code.md`(4.5 KB)는 단독 파일로 유지하기에 내용이 매우 짧았음

---

## 4. 검토되었던 최적화 방안 (비교)

### A안: 가벼운 정리 (중복 서술 제거 중심)
- 기존 파일 구조(10개)를 유지하며 중복 텍스트만 링크로 치환
- 예상 감소: 10개 파일 → 8개 파일, ~158 KB

### B안: 기능 중심 통합 (대폭 정리) — ★ 최종 채택 및 적용
- 역할별 4개 파일 체계로 전면 통폐합
- `project-context.md`, `feature-catalog.md`, `verification-log.md`, `database-schema.md`
- 예상 감소: 10개 파일 → 4개 파일, 약 100 KB 압축 및 정보 집중도 대폭 향상

### C안: README.md 체크리스트와 feature-status 통합
- README의 체크리스트를 유지하되 상세 설명만 일원화

---

## 5. 변경하지 않고 보존한 원칙

| 영역 | 보존 이유 |
|---|---|
| `README.md` 기능 체크리스트 | AGENTS.md 품질 관문 규칙 ("기능 체크 목록은 삭제하지 않는다") 준수 |
| `common/` 5종 | 관심사 분리 양호, 타 프로젝트 이식 시 필수 복사 대상 |
| `portable/` 8종 | 범용 빈 양식으로 적정 크기 유지 |
| `boot/`, `classic/` | 프레임워크 트랙별 분리 유지 필요 |
| `AGENTS.md`, `PORTING.md` | 최상위 작업 진입 지침으로 핵심 규칙 보존 |
