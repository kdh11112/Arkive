# AI 오케스트레이션 운영 규칙

> 이식 구분: 환경형. Orca·Freebuff·OpenCode(Sol)를 쓰는 작업 환경에서만 복사한다. 이 도구가 없는 대상이면 복사하지 않거나 `해당 없음`으로 둔다.
> 기준: 2026-11-18 `D:\project\eGovFrameDev_RECS\workspace`에서 실측한 내용을 Arkive에 맞게 옮긴 것이다. 모델·요금·시간 규칙이 바뀌면 이 문서를 갱신한다.

이 문서는 총괄(현재 세션의 메인 에이전트)이 Worker에게 작업을 나눌 때만 읽는다. 모드 전환·자동 제안·강도 제안의 핵심 규칙은 [AGENTS.md](../../../AGENTS.md#ai-오케스트레이션)에 있으며, 여기에는 상세 기준을 둔다.

## 1. 모드 전환

- 기본 상태는 **일반 단독 처리 모드**다. 사용자 승인 없이 Worker를 생성하거나 Orca orchestration 명령을 실행하지 않는다.
- **자동 제안:** 작업이 크거나 독립 단위로 나눌 수 있으면 총괄이 착수 전에 한 번 묻는다.
  - 형식: "Worker에게 나눌까요? (배정안: Freebuff ○○ / Sol ○○, 예상 소모: Freebuff 세션 ○ / Sol 사용 여부)"
  - Arkive 대상 예: 소스 프로젝트 조사와 Arkive 구현을 나눌 수 있는 기능 이식, 여러 화면·계층(Thymeleaf·Controller·Service·Mapper XML·SQL)에 걸친 구현, 구현과 별도 검증이 필요한 작업, 대량 조사(여러 소스 프로젝트 비교 등).
  - 승인하면 오케스트레이션으로 진행하고, 거절하거나 답이 없으면 단독 처리한다.
- 간단한 질문, 한두 파일의 작은 수정, 조회·설명 작업에는 제안하지 않는다.
- 활성화 명령: `오케스트레이션으로 진행해줘`, `오케스트라로 해줘`, `Worker에게 나눠서 진행해줘`. 활성화 후에는 현재 세션의 후속 작업도 계속 오케스트레이션으로 진행한다.
- 해제 명령: `일반 모드로 전환해줘`, `오케스트레이션 중지`, `Worker를 사용하지 마`. 즉시 일반 단독 처리로 전환하고, 그 세션에서는 자동 제안도 하지 않는다.
- 새 세션은 일반 단독 처리 + 자동 제안 상태로 시작한다.

## 2. 추론 강도 제안

- 총괄은 자신의 세션 추론 강도(effort)를 직접 바꿀 수 없다. 강도는 사용자가 설정한다.
- 어렵거나 중요한 작업이면 착수 전에 한 번 제안한다. 형식: "이 작업은 강도를 올리는 게 좋겠습니다 (이유: ○○). 높여서 진행할까요?" 해당 요청에 `ultrathink`를 붙이거나 세션 effort 설정을 올리는 방법을 함께 안내한다.
  - 대상 예: 원인 불명 오류 추적, 권한·인증·데이터 정합성에 영향을 주는 수정, 여러 계층에 걸친 변경, 소스 프로젝트 기능의 Arkive 이식 설계, DB 스키마·마이그레이션 변경, 설계 판단.
- 단순 조회, 설명, 한두 줄 수정에는 제안하지 않는다. 같은 작업에서 반복해서 제안하지 않는다.
- 사용자가 거절하면 현재 강도로 진행하되, 검증을 더 꼼꼼히 하고 불확실한 점을 보고에 명시한다.
- Worker 강도: Freebuff는 Orca에서 지정할 수 없으므로(모델 선택 화면에서 Tab으로 사용자가 변경) 필요하면 사용자에게 변경을 요청한다. OpenCode(Sol)는 Orca `--effort`가 지원되지 않는다.

## 3. 역할 구성 (2026-11-18 실측 기준)

| 역할 | 에이전트 (Orca agent id) | 실제 모델 | 사용 기준 |
| --- | --- | --- | --- |
| 총괄 | 현재 세션의 메인 에이전트 (기본: Claude Code) | - | 요청 분석, 작업 분배, 결과 비교·검증, `verify.cmd`·GUI 최종 확인, 최종 판단과 보고 |
| Worker 1 (주력) | Freebuff (`freebuff`) | DeepSeek V4.1 Flash Fast | 대부분의 분석·구현·검증. 무료, 아래 시간 규칙 적용 |
| Worker 2 (보조·대체) | OpenCode2 (`opencode2`) | GPT-6.1 Sol (AgentRouter, `ar-sol/gpt-6.1-sol`) | Freebuff가 불가할 때, 또는 어렵고 중요한 판단·최종 리뷰 |

- Worker는 **Freebuff를 우선** 사용한다. Freebuff가 시작 실패, 제한 도달, 반복 오류일 때만 OpenCode2(GPT-6.1 Sol)로 전환한다.
- **GPT-6.1 Sol은 유료(AgentRouter 잔액 차감)이며 하루 약 $20 이내로 사용한다.** 범위를 좁히고 읽을 파일을 지정해 토큰을 줄인다. 오늘 사용량이 한도에 가까운지 판단이 서지 않으면 Sol에 맡기기 전에 사용자에게 확인한다.
- 실행된 모델이 위 표와 다르면 임의로 진행하지 말고 먼저 사용자에게 알린다.

## 4. Freebuff 사용 시간 규칙

- 하루 사용 가능 시간은 15시간(기본 12 + 연속 사용 보너스 3)이며 00:00(Asia/Seoul)에 리셋된다. 1시간 = 5 freebucks.
- 세션 1개는 실제 1시간 동안 쓸 수 있고, 시작할 때 모델 배수만큼 일일 시간이 차감된다 (Flash Fast 3x = 3시간, Flash 2x = 2시간, Muse Spark 3x = 3시간).
- 세션 만료 시각은 시작 시점에 고정된다. 터미널을 닫아도 남은 시간은 돌려받지 못하고 계속 흐른다.
- 세션은 계정 단위다. 세션이 살아 있을 때 새 Freebuff 터미널(Orca Worker 포함)을 띄우면 **기존 세션에 합류하고 만료 시각을 그대로 이어받는다** (2026-11-18 관측: 17:37:41 시작 프로세스의 만료가 18:23:29로, 새 1시간이 아니었음).
- 따라서 Worker를 띄우기 전에 활성 세션이 있는지 먼저 확인하고, **활성 세션이 있으면 그 안에서 진행**한다. Freebuff 작업은 세션이 살아 있는 1시간 안에 몰아서 배정하고, 짧은 작업 하나를 위해 새 세션을 열지 않는다.
- 사용자 허락 없이 Freebuff의 `End session`을 누르거나 세션을 종료하지 않는다. 종료 후 새로 시작하면 남은 시간을 잃고 배수만큼 다시 차감된다.
- 남은 시간과 현재 세션 만료 시각 확인 (읽기 전용, 인증 정보는 출력하지 않음). 스크립트는 RECS workspace에 있으며 설정 경로가 절대경로라 어느 폴더에서 실행해도 된다:
  `node D:/project/eGovFrameDev_RECS/workspace/.freebuff-usage/usage.mjs --once --full`
- 남은 일일 시간이 다음 세션 차감량보다 적거나, 세션이 없는데 작업이 짧다면 Freebuff 대신 총괄이 직접 처리하거나 사용자에게 확인한다.

## 5. Freebuff 세션 유지 규칙 (실측)

- Orca가 띄운 Worker는 작업이 끝나면 **자동으로 닫히고**, 닫힌 Freebuff는 대화 세션이 사라진다.
- 닫힌 Worker에 `send`를 보내면 Orca가 **새 Freebuff를 띄운다** (`delivery: started_fresh`). 이전 대화 기억은 없고, Orca가 이전 보고 요약만 전달한다.
- 같은 맥락에서 여러 번 이어서 일을 시킬 Worker는 처음부터 `--keep-open`으로 시작하고, 후속 작업은 `send` 또는 `--terminal <handle>`로 같은 터미널에 보낸다.
- 1회성 작업(조사, 검증)은 `--keep-open` 없이 시작해 자동으로 닫히게 둔다.
- `--keep-open`으로 연 Worker는 작업이 모두 끝나면 사용자에게 알리고, 사용자가 원하면 닫는다.

## 6. 작업 배분 원칙

1. 요청을 분석하고 독립 작업 단위로 나눈다. 이식이면 [AGENTS.md](../../../AGENTS.md)·[PORTING.md](../../../PORTING.md)의 "기능 하나씩" 원칙을 유지하고, 한 기능 안에서 조사·구현·검증으로 나눈다.
2. Worker 실제 상태를 확인한다 (`orca status --json`, `orca terminal list --json`).
3. 작업은 기본적으로 Freebuff에 배정하고, Sol은 3장 사용 기준에 맞는 경우에만 배정한다.
4. **실제 파일 수정은 한 Worker만 담당한다.** 같은 파일을 여러 Worker가 동시에 수정하지 않는다.
   - Arkive는 git 저장소라 `--worktree new-child`를 쓸 수 있지만 아직 검증하지 않았다. 검증 전에는 `--worktree current`로 같은 폴더를 공유한다. 처음 쓸 때는 작은 작업으로 생성·병합·정리 흐름을 확인하고 이 문서에 결과를 남긴다.
   - 착수 전에 `git status`로 기존 변경을 확인하고, Worker가 기존 변경을 덮어쓰지 않도록 spec에 수정 허용 파일을 적는다.
5. 수정이 끝나면 다른 Worker(또는 총괄)가 검증한다. 검증 Worker의 spec에는 "파일을 수정하지 말 것"을 명시한다.
6. 결과가 서로 다르면 코드, 테스트, 로그 등 근거로 재검토하고 총괄이 최종 결정한다.
7. `verify.cmd` 실행 결과와 화면 확인(ego-browser)은 **총괄이 직접 최종 확인**한다. Worker의 "통과" 보고만으로 완료 처리하지 않는다.
8. 커밋·푸시는 Worker가 하지 않는다. 사용자가 요청할 때 총괄이 변경을 검토한 뒤 수행한다.
9. 최종 보고에는 실제로 실행된 Worker, 검증 결과, 미완료 항목만 포함한다. 확인하지 않은 작업을 완료했다고 보고하지 않는다.

## 7. Worker spec 작성 기준

각 spec은 이전 대화에 기대지 않도록 **자기완결적으로** 쓴다. Worker에게도 [AGENTS.md](../../../AGENTS.md)의 작업 원칙·품질 관문이 똑같이 적용되므로 필요한 제한을 spec에 다시 적는다.

| 항목 | 적을 내용 |
| --- | --- |
| 목적 | 해결할 문제 한두 문장 |
| 읽을 파일 | 경로를 지정한다 (토큰 절약). 소스 프로젝트는 `D:\ai\<프로젝트>` 절대경로 |
| 수정 허용 범위 | 수정해도 되는 파일·폴더. 조사·검증 Worker는 "파일을 수정하지 말 것" |
| 금지 | 테스트 파일(`src/test/**`) 생성·수정·삭제·skip 금지, 소스 프로젝트(`D:\ai\ADTM` 등) 수정 금지, `data/`·`target/`·`backup/` 수정 금지, 실행 중인 DB에 직접 쓰기 금지(스키마 변경은 `schema.sql` 등 파일로만), 비밀값 출력 금지, 커밋·푸시 금지 |
| 근거 규칙 | 업무 규칙·권한·상태값·DB 의미는 근거 파일과 줄 번호로 보고하고, 근거가 없으면 `미확인`으로 남긴다 |
| 완료 증거 | 변경 파일 목록, 핵심 변경, 실행한 명령과 결과(예: `verify.cmd` exit code), 미완료·미확인 항목 |

## 8. Orca 명령 (실측 확인된 것)

```powershell
# 상태 확인
orca status --json
orca terminal list --json          # 열린 Freebuff/OpenCode 터미널 handle 확인 (agentIdentity 필드)

# Freebuff Worker 새로 시작 (확인됨). 이어서 쓸 거면 --keep-open 추가
orca orchestration worker-start --agent freebuff --worktree current --spec "<자기완결적 작업 지시>" --json

# 이미 열린 터미널(OpenCode2 Sol 등)에 작업 넣기 (확인됨). --worktree, --model 같이 쓰지 않음
orca orchestration worker-start --terminal <term_handle> --spec "<작업 지시>" --json

# 결과 대기 (확인됨). run id는 worker-start 결과의 runId
orca orchestration check --wait --run <runId> --types worker_done,escalation,question --timeout-ms 600000 --json

# 후속 지시 (확인됨). 닫힌 Worker면 새로 시작되며 이전 기억 없음
orca orchestration send --to dispatch:<dispatchId> --subject "<제목>" --body "<지시>" --json

# Worker 상태 목록
orca orchestration worker-list --run <runId> --json
```

- `worker-start --agent opencode2`로 새 OpenCode2 터미널을 띄우는 방식은 아직 검증하지 않았다. 새로 띄우면 OpenCode2 설정의 모델을 쓰므로, 처음 쓸 때 작은 읽기 전용 작업으로 모델을 확인한다.
- `--agent opencode`(OpenCode 1)는 Orca에서 비활성 상태다 (`agent_unconfigured`).
- `worker-start`가 실패하면 같은 명령을 그대로 다시 실행하지 말고 `failedStage`를 확인한다 (`orca skills get orchestration --reference references/recovery-and-cleanup.md`).
- 별도 Orca 데스크톱 화면 없이 CLI만으로 제어할 수 있다. 상세 사용법은 `orca skills get orchestration --full`.
