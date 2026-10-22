# AI에게 이식 요청하는 방법

> 이 문서는 Arkive 기능을 다른 프로젝트로 이식할 때 AI에게 어떻게 요청하고,
> Arkive와 대상 프로젝트를 어디에 두는지 설명한다.
> 한 번에 전체를 이식하지 말고 **기능 하나씩** 요청한다.

## 1. 권장 배치

Arkive와 대상 프로젝트는 **형제 폴더**에 두는 것을 권장한다.

```text
C:\work\
  Arkive\            ← 레퍼런스, 읽기 전용으로 취급
  target-project\    ← 실제 작업 디렉터리
```

- AI 세션은 **대상 프로젝트에서 시작**한다.
- 프롬프트에 Arkive의 **절대 경로**를 적는다.
- 대상 프로젝트 안에 Arkive를 복사하거나 커밋하지 않는다.
  - 검색·빌드·커밋에 Arkive 파일이 섞일 수 있다.
- AI가 두 경로를 모두 읽을 수 있으면 **MD를 수동으로 옮길 필요가 없다.**

## 2. 수동 복사가 필요한 경우

| 상황 | 방법 |
| --- | --- |
| AI가 Arkive와 대상 프로젝트를 모두 읽을 수 있음 | 대상 프로젝트를 작업 디렉터리로 열고 Arkive 절대 경로만 알려준다. MD 수동 복사 불필요 |
| AI가 Arkive를 읽을 수 없음 | [PORTING.md](../../../PORTING.md)의 `1. 5분 복사`를 먼저 실행한다 |
| 다른 PC·다른 저장소 | 필요한 문서와 코드만 복사한 뒤, 대상 프로젝트에서 AI 세션을 시작한다 |

수동 복사 대상:

- `docs/ai/common/`
- `docs/ai/portable/`
- `docs/ai/boot/` 또는 `docs/ai/classic/` 중 하나
- `AGENTS.md`
- `PORTING.md`
- `verify.cmd`, `doctor.cmd`
- `.github/workflows/verify.yml` (GitHub 사용 시)

복사하지 않는 것:

- `docs/ai/project/` — Arkive의 현재 지식 기록
- `data/`, `target/`, `backup/`
- 개발용 비밀값·운영 DB 접속 정보

대상 프로젝트의 프로젝트 지식은 [project-template-pack.md](project-template-pack.md)로 새로 채운다.

## 3. 따라하기

### 3.1 준비

- [ ] Arkive와 대상 프로젝트를 형제 폴더에 둔다.
- [ ] 대상 프로젝트를 AI의 작업 디렉터리로 연다.
- [ ] Arkive의 절대 경로를 확인한다.
- [ ] 대상 프로젝트의 패키지·DB 제품·인증 방식을 확인한다.
- [ ] 이번에 이식할 기능 하나를 정한다.

### 3.2 복사 여부 결정

- AI가 Arkive를 읽을 수 있으면 → **복사하지 않고 경로만 알려준다.**
- AI가 Arkive를 읽을 수 없으면 → `PORTING.md`의 5분 복사 실행 후 대상에서 작업한다.

### 3.3 요청

아래 프롬프트를 복사해 경로와 기능명만 바꾼다.

```text
Arkive 레퍼런스: C:\work\Arkive
대상 프로젝트: 현재 작업 디렉터리 (C:\work\target-project)

이번 이식 기능: 게시판 댓글
범위: 화면 + Controller + Service + Mapper/XML + SQL + 테스트

1. Arkive의 PORTING.md, feature-porting-sheet.md, test-porting-sheet.md를 먼저 읽어라.
2. Arkive를 실행해 댓글 등록·삭제·권한 동작을 확인하고, 대상과 다른 점을 표로 정리해라.
3. 대상의 패키지·테이블·DB·URL·세션 방식에 맞게 화면·Controller·Service·Mapper/XML·SQL을 옮겨라.
4. 테스트도 함께 옮기고, 대상 기준으로 URL·메시지·세션·seed를 수정해라.
5. 확인되지 않은 업무 규칙·권한·DB 의미는 추측하지 말고 "미확인"으로 남기고 질문해라.
6. 단독 테스트와 verify.cmd를 실행하고, 수정 파일·검증 결과·남은 위험을 보고해라.
```

### 3.4 확인

- [ ] Arkive에서 기능 동작을 먼저 확인했는가
- [ ] 대상 프로젝트에 불필요한 문서·비밀값이 복사되지 않았는가
- [ ] 테스트가 대상 URL·메시지·세션·스키마에 맞게 수정되었는가
- [ ] 단독 테스트가 통과하는가
- [ ] 전체 `verify.cmd`가 통과하는가
- [ ] 수동 화면·API·DB 확인 결과를 기록했는가

## 4. 요청 시 꼭 포함할 것

- Arkive 경로와 대상 프로젝트 경로
- 이식할 기능명과 범위(화면/API/DB/테스트 포함 여부)
- 대상 프로젝트의 패키지·DB 제품·인증 방식
- 대상에서 이미 다른 이름으로 존재하는 테이블·메뉴·URL
- 하지 말아야 할 것
  - `docs/ai/project/` 복사 금지
  - 비밀값·운영 DB 접속 정보 이전 금지
  - 테스트 삭제·skip 금지
- 완료 기준
  - `verify.cmd` 통과
  - 수동 화면 확인
  - 대상 DB 확인

## 5. 자주 하는 실수

- Arkive를 대상 프로젝트 안에 복사한다.
- `docs/ai/project/`까지 복사한다.
- “전체 이식”을 한 번에 요청한다.
- 테스트를 대상에 맞게 수정하지 않고 그대로 복사한다.
- HSQLDB 테스트만 통과하고 Oracle/PostgreSQL 확인을 생략한다.
- 검증 없이 “완료”로 보고한다.
