# AI 오케스트레이션 운영 규칙

> 이식 구분: 환경형. Orca·Freebuff·OpenCode(Sol)를 쓰는 작업 환경에서만 복사한다. 이 도구가 없는 대상이면 복사하지 않거나 `해당 없음`으로 둔다.
> 기준: 2026-11-18 `D:\project\eGovFrameDev_RECS\workspace`에서 실측한 내용을 Arkive에 맞게 옮긴 것이다. 모델·요금·시간 규칙이 바뀌면 이 문서를 갱신한다.
> 재확인: 2026-11-18 Arkive 작업 PC(`C:` 드라이브만 있음)에서 Freebuff·OpenCode2 읽기 전용 Worker 테스트로 다시 확인했다(9장). 원 기록의 `D:\` 경로는 이 PC에 없으므로 해당 항목은 `미확인`으로 표시한다.

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
- Worker 쪽 모델·강도는 총괄이 직접 지정할 수 없다. 지정 방법은 3장 "모델·강도 지정"에 있다.

## 3. 역할 구성과 모델 (2026-11-18 실측 기준)

| 역할 | 에이전트 (Orca agent id) | 실제 모델 | 사용 기준 |
| --- | --- | --- | --- |
| 총괄 | 현재 세션의 메인 에이전트 (기본: Claude Code) | - | 요청 분석, 작업 분배, 결과 비교·검증, `verify.cmd`·GUI 최종 확인, 최종 판단과 보고 |
| Worker 1 (주력) | Freebuff (`freebuff`) | DeepSeek V4.1 Flash · high (2x, 기본값. 2026-11-18 이 PC의 Freebuff 화면 하단 표시로 확인) | 대부분의 분석·구현·검증. 무료, 아래 시간 규칙 적용 |
| Worker 2 (보조·대체) | OpenCode2 (`opencode2`) | GPT-6.1 Sol (AgentRouter, `ar-sol/gpt-6.1-sol`) | Freebuff가 불가할 때, 또는 어렵고 중요한 판단·최종 리뷰 |

- Worker는 **Freebuff를 우선** 사용한다. Freebuff가 시작 실패, 제한 도달, 반복 오류일 때만 OpenCode2(GPT-6.1 Sol)로 전환한다.
- **GPT-6.1 Sol은 유료(AgentRouter 잔액 차감)이며 하루 약 $20 이내로 사용한다.** 범위를 좁히고 읽을 파일을 지정해 토큰을 줄인다. 오늘 사용량이 한도에 가까운지 판단이 서지 않으면 Sol에 맡기기 전에 사용자에게 확인한다.
- 실행된 모델이 위 표와 다르면 임의로 진행하지 말고 먼저 사용자에게 알린다.
- **모델·강도 지정:** Freebuff는 Orca `--model`/`--effort`로 지정할 수 없다(Orca `worker-start --help`: 지원 목록에 없고 자체 설정 모델로 실행). 바꿔야 하면 사용자에게 요청한다. Freebuff 화면에서는 `/model` → ↑↓로 모델 선택, Tab으로 추론 강도(reasoning) 변경, Enter로 확정한다(2026-11-18 사용자 화면 확인). OpenCode(Sol)는 Orca `--effort`가 지원되지 않는다.
- **OpenCode2의 전제 조건: KR bridge가 켜져 있어야 한다.** OpenCode 설정(`~/.config/opencode/opencode.jsonc`)의 `ar-sol` provider는 AgentRouter에 직접 붙지 않고 로컬 KR bridge(`http://127.0.0.1:8790/v1`, `~/.config/opencode/kr-bridge/proxy.js`)를 거친다. 브리지는 한글 때문에 `400 content-blocked`가 나면 이스케이프로 1회 재시도한다.
  - 브리지가 꺼졌거나 멈추면 OpenCode2는 오류 없이 응답을 기다리기만 한다. Sol을 배정하기 전에 `netstat -ano | findstr "127.0.0.1:8790"`으로 LISTENING을 확인한다.
  - 실행: 바탕화면 바로가기 `AgentRouter KR bridge` → `kr-bridge/start-bg.cmd`(창 없이 실행, 로그 `kr-bridge/bridge.log`, 이미 실행 중이면 알림만 표시). 종료: `kr-bridge/stop.cmd`. 재부팅하면 다시 실행해야 한다.
  - 콘솔 창 방식(`kr-bridge/start.cmd`)은 창 안을 클릭해 제목에 "선택"이 붙으면 로그 출력이 막혀 프록시 전체가 멈춘다(2026-11-18 관측). 이 경우 창에서 `Esc`를 누르거나 브리지를 다시 시작한다.

## 4. Freebuff 사용 규칙

### 4.1 시간과 차감

- 하루 사용 가능 시간은 15시간(기본 12 + 연속 사용 보너스 3)이며 00:00(Asia/Seoul)에 리셋된다. 1시간 = 5 freebucks.
- 세션 1개는 실제 1시간 동안 쓸 수 있고, 시작할 때 모델 배수만큼 일일 시간이 차감된다 (Flash Fast 3x = 3시간, Flash 2x = 2시간, Muse Spark 3x = 3시간).
- 세션 만료 시각은 시작 시점에 고정된다. 터미널을 닫아도 남은 시간은 계속 흐른다.
- Freebuff 작업은 세션이 살아 있는 1시간 안에 몰아서 배정하고, 짧은 작업 하나를 위해 새 세션을 열지 않는다.
- 남은 일일 시간이 다음 세션 차감량보다 적거나, 세션이 없는데 작업이 짧다면 Freebuff 대신 총괄이 직접 처리하거나 사용자에게 확인한다.
- 사용자 허락 없이 Freebuff의 `End session`을 누르거나 세션을 종료하지 않는다. 종료 후 새로 시작하면 남은 시간을 잃고 배수만큼 다시 차감된다.

### 4.2 세션 소유와 이어받기

- **세션은 살아 있는 Freebuff 프로세스 하나가 소유한다** (2026-11-18 이 PC 실측, Freebuff 0.2.36).
  - 소유 정보는 `~/.config/manicode/freebuff-live-<PID>.json`의 `instanceId`·`ownerPid`·`expiresAt`·`model`에 있다(같은 파일의 `tokenKey` 등 인증 값은 출력·기록하지 않는다).
  - 소유 프로세스가 꺼진 세션은 새 Freebuff 프로세스가 **이어받는다.** 같은 `instanceId`·`expiresAt`을 그대로 쓰며 일일 시간이 차감되지 않는다(사용자가 23:02에 연 탭이 22:44 시작 세션을 이어받음, 23:30에 메시지 없이 띄운 Freebuff가 `35m left`·`2/15 hours` 그대로 이어받음 확인).
  - 소유 프로세스가 살아 있는 세션은 공유하지 못한다. 그 상태에서 새로 띄운 Worker는 **새 세션**을 열어 배수만큼 차감했다(23:04 Worker가 사용자 탭 세션과 별도로 00:04 만료 세션을 받음).
  - 새 프로세스의 첫 화면에 `○m left`가 보이면 이어받은 것이고, "Your first message starts the session"이 보이면 첫 메시지에서 새 세션이 시작된다.
- **세션을 아끼는 절차:**
  1. 띄우기 전에 `~/.config/manicode/freebuff-live-*.json`과 `Get-Process freebuff`로 세션별 만료 시각과 소유 프로세스 생존 여부를 확인한다. 소유 프로세스가 없고 만료 전인 세션이 있으면 이어받을 수 있다. 다만 라이브 파일은 세션이 실제로 시작된 뒤(첫 메시지 제출 무렵) 기록되므로 Freebuff를 막 띄운 직후에는 아직 없을 수 있다(2026-11-21 관측, 9장). 이때는 첫 화면의 `○m left` / "Your first message starts the session" 표시로 판단하고, 화면에 세션이 살아 있으면 새 Worker/Freebuff를 띄우지 않는다.
  2. 이어받을 세션이 있으면 `worker-start --agent freebuff`로 띄운다. Flash Fast 창이 뜨면 4.3절 방법으로 Flash로 제출하고, 첫 화면의 `○m left`와 `○/15 hours remaining`이 그대로인지 확인한다.
  3. 한 Worker가 끝나 자동으로 닫히면 그 세션이 다시 주인 없는 상태가 되므로, 다음 묶음은 새 Worker로 띄워 이어받는다. Freebuff Worker는 `--keep-open`을 쓰지 않는다(5장).
  4. 이어받을 세션이 없을 때만 새 세션을 열며, 일일 시간 차감을 사용자에게 먼저 알린다.
- **실행 중인 Freebuff에 이어서 작업을 넣는 방식은 쓰지 않는다.** `orchestration send`(follow-up)나 `worker-start --terminal`로 넣은 지시는 입력창에 첨부(📄)로만 붙고 제출되지 않았으며, Orca로 보낸 Enter·지우기 키에도 반응하지 않았다(2026-11-18 23:17~23:27, 원인 미확인).

### 4.3 모델 고정과 Flash Fast 제출

- **Freebuff 기본 모델은 DeepSeek V4.1 Flash · high (2x)로 고정한다.** 사용자가 다른 모델을 지시하지 않는 한 Flash Fast(3x)나 다른 모델로 세션을 시작하지 않는다(2026-11-18 사용자 지시). Freebuff 설정(`~/.config/manicode/settings.json`의 `freebuffModelKey`)에는 이미 Flash가 저장돼 있지만, Freebuff에는 시작 모델을 지정하는 실행 옵션이 없고(`--model` 없음, 0.2.36 확인) 카탈로그를 불러오기 전 첫 제출은 기본값 Flash Fast를 쓴다. 그래서 아래처럼 총괄이 띄울 때마다 확인한다.
- **Orca로 새로 띄운 Worker의 첫 제출은 Flash Fast(3x)로 시도된다.** 하단 표시는 Flash(2x)여도, 일일 남은 시간이 3시간 미만이면 "Not enough hours for DeepSeek V4.1 Flash Fast" 창이 뜨고 멈춘다. 이때 `orca terminal send --terminal <handle> --text $'\r'`(Git Bash) 또는 ``orca terminal send --terminal <handle> --text "`r"``(PowerShell)로 Enter를 보내 모델 선택 창을 열고(Flash가 선택된 상태), 한 번 더 Enter를 보내면 Flash로 제출되어 작업을 시작한다(2026-11-18 A1·H Worker에서 확인).
  - 이어받은 Flash 세션이 있는 상태에서 Flash Fast로 제출되면 Freebuff 코드상 "새 세션 시작" 확인 창이 뜬다. 이 창에서도 진행하지 말고 모델 선택에서 Flash를 고른다.
  - **위험(미확인):** 이어받을 세션이 없고 남은 시간이 3시간 이상이면 창 없이 Flash Fast 새 세션(3시간 차감)으로 바로 시작될 수 있다. 확인 전까지는 이 조건에서 Worker를 띄우기 전에 사용자에게 알리고, 띄운 직후 하단 모델과 `○/15 hours remaining` 감소량(2시간인지)을 확인한다.

### 4.4 동시 실행 시 계정 분리

- **Worker 여러 개를 동시에 돌릴 때 같은 계정을 쓰지 않는다.** Arkive는 중복 로그인 방지가 있어 한 Worker가 admin으로 로그인하면 다른 Worker의 admin 세션이 끊긴다(2026-11-18 A1·H 동시 실행 중 H가 로그인 화면으로 튕김). 접속 호스트(`127.0.0.1`/`localhost`)를 나눠도 막을 수 없다. 관리자 기능 묶음은 한 번에 하나씩 돌리거나 Worker별 관리자 테스트 계정을 따로 둔다.

### 4.5 잔여 시간·만료 확인

- 남은 시간과 현재 세션 만료 시각 확인 (읽기 전용, 인증 정보는 출력하지 않음). 원 기록 PC에서는 RECS workspace의 스크립트를 썼다:
  `node D:/project/eGovFrameDev_RECS/workspace/.freebuff-usage/usage.mjs --once --full`
  - **이 PC에서는 일일 잔여 시간 확인이 `미확인`:** 위 경로가 없다(D 드라이브 없음, 2026-11-18 확인). **세션 만료 시각과 세션 남은 시간은 라이브 파일의 `expiresAt`으로 확인할 수 있다**(2026-11-21 실측: 21:42:51 − 20:44:51 = 약 57분). 다만 **일일 15시간 중 남은 양**을 알 수 없으므로, Freebuff Worker를 새로 띄우기 전에 일일 시간 차감(Flash 2x 기준 2시간) 가능성을 사용자에게 알린다.
    - 미완성 항목: 일일 15시간 잔량 사전 확인 수단 없음.
    - 필요한 근거/확인자: 일일 사용량을 읽을 수 있는 스크립트·설정 위치(사용자 환경 확인).
    - 완성 방법: `usage.mjs`를 이 PC로 옮기거나 일일 잔여 시간을 읽는 방법을 찾아 갱신.
    - 완료 기준: 새 세션을 열기 전에 일일 잔여 시간을 알 수 있어 차감 가능 여부를 미리 판단할 수 있게 되는 것.
  - **2026-11-21 관측 (확인 방법 정상 동작):** 20:35 확인에서는 `Get-Process freebuff` 결과가 없고 라이브 파일도 11-20 만료분뿐이었으나, 이는 그 시점에 세션이 아직 등록되기 전이었다(프로세스 20:38:23 시작, 세션 등록 20:42:51). 이후 `freebuff.exe`(PID 14484)와 라이브 파일(ownerPid 14484, 만료 21:42:51)이 확인됐다. 프로세스·파일 확인 방식은 정상이며, 웹/클라우드 세션 등 원인 추측은 기각한다.

## 5. Worker 종료·재개 동작 (실측)

- Orca가 띄운 Worker는 작업이 끝나면 **자동으로 닫히고**, 닫힌 Freebuff는 대화 세션이 사라진다.
- 닫힌 Worker에 `send`를 보내면 Orca가 **새 Freebuff를 띄운다** (`delivery: started_fresh`). 이전 대화 기억은 없고, Orca가 이전 보고 요약만 전달한다.
- Worker는 작업을 마치면 `reclaimable` 상태가 되고, 약 10분 뒤(`autoReleaseAt`) Orca가 자동으로 닫는다.
- 같은 맥락에서 여러 번 이어서 일을 시킬 Worker는 처음부터 `--keep-open`으로 시작하고, 후속 작업은 `send` 또는 `--terminal <handle>`로 같은 터미널에 보낸다. **단, Freebuff는 이어서 넣은 지시가 제출되지 않으므로(4.2절) 이 방식을 쓰지 않는다.**
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
7. `verify.cmd` 실행 결과와 화면 확인(ego-browser)은 **총괄이 직접 최종 확인**한다. Worker의 "통과" 보고만으로 완료 처리하지 않는다. GUI 검증 분담은 아래 6.1절을 따른다.
8. 커밋·푸시는 Worker가 하지 않는다. 사용자가 요청할 때 총괄이 변경을 검토한 뒤 수행한다.
9. 최종 보고에는 실제로 실행된 Worker, 검증 결과, 미완료 항목만 포함한다. 확인하지 않은 작업을 완료했다고 보고하지 않는다.

### 6.1. GUI 검증 분담 (ego-browser)

Worker는 화면 조작을 수행하고, 검증완료 판정은 총괄만 한다. "총괄이 직접 확인"은 Worker 보고서를 읽는 것이 아니라 **총괄이 증거를 직접 열어 보고 결과 상태를 다시 조회해 대조하는 것**을 뜻한다.

| 단계 | 담당 | 내용 |
| --- | --- | --- |
| 시나리오 수행 | Worker | ego-browser로 spec의 시나리오(데이터 등록 → 흐름 진행 → 결과 확인 → 테스트 데이터 정리)를 수행한다. 단계별 스크린샷을 spec에 지정한 경로에 저장하고, 단계마다 URL·계정/권한·입력 데이터·기대 결과·실제 결과를 보고한다. |
| 대조 확인 | 총괄 | Worker 스크린샷을 직접 연다. 결과 화면 또는 DB 상태를 총괄이 ego-browser로 다시 조회해 보고와 일치하는지 확인한다. 하나라도 다르거나 증거가 없으면 불합격으로 보고 재수행하거나 `미수행`으로 둔다. |
| 판정·기록 | 총괄 | [검증 및 변경 로그](../project/verification-log.md)에 항목별 절을 쓰고, `README.md` 기능 상태를 바꾼다. |

- 스크린샷 등 증거 파일은 저장소에 커밋하지 않는 `target/gui-evidence/<묶음>/`에 두고, 총괄이 대조·판정을 마치고 로그에 기록한 뒤 삭제한다. 불합격이나 판정 보류인 묶음은 원인 확인이 끝날 때까지 남긴다.
- Worker는 `README.md`, `docs/ai/project/verification-log.md` 등 검증 상태 기록을 수정하지 않는다. spec의 수정 허용 범위에서 제외하고 금지 항목에 적는다.
- 렌더링 확인("화면이 뜬다")만으로 기능 검증완료로 보지 않는다. 시나리오의 핵심 동작(등록·수정·삭제·권한 차단 등)과 결과가 확인돼야 한다.
- 여러 기능을 한 줄로 묶어 기록하지 않는다. 기능마다 시나리오·결과·증거를 따로 남긴다.
- GUI 검증 Worker는 기본으로 Freebuff를 쓴다. 신뢰성은 총괄의 대조 확인으로 확보하므로, 단순 화면 조작에 유료 Sol을 쓰지 않는다. Sol은 3장 기준대로 Freebuff가 불가할 때만 쓴다.

## 7. Worker spec 작성 기준

각 spec은 이전 대화에 기대지 않도록 **자기완결적으로** 쓴다. Worker에게도 [AGENTS.md](../../../AGENTS.md)의 작업 원칙·품질 관문이 똑같이 적용되므로 필요한 제한을 spec에 다시 적는다.

- **문체:** 서브에이전트(Claude Agent 등)에게 지시할 때는 `Task:`·`Requirements:` 같은 딱딱한 머리표 대신, 동료 개발자에게 설명하듯 자연스러운 한국어 문장으로 쓴다(2026-11-18 사용자 지시).
- **내용은 줄이지 않는다:** 문체만 바꾸고, 아래 표의 항목(목적, 읽을 파일, 수정 허용 범위, 금지, 근거 규칙, 완료 증거)과 시나리오의 URL·입력값·기대 결과·정리 단계·보고 형식은 길어지더라도 빠뜨리지 않는다. 보고 형식처럼 대조에 쓰는 부분은 표 형태를 유지해도 된다.
- 이 문체 규칙은 서비스 정책 감지를 피하는 용도가 아니다. 이용 정책상 Worker로 쓸 수 없는 도구(3장 Freebuff 경고)에는 적용하지 않는다.

| 항목 | 적을 내용 |
| --- | --- |
| 목적 | 해결할 문제 한두 문장 |
| 읽을 파일 | 경로를 지정한다 (토큰 절약). 소스 프로젝트는 절대경로로 적는다(원 기록 PC는 `D:\ai\<프로젝트>`, 이 PC의 소스 프로젝트 위치는 `미확인`) |
| 수정 허용 범위 | 수정해도 되는 파일·폴더. 조사·검증 Worker는 "파일을 수정하지 말 것" |
| 금지 | 테스트 파일(`src/test/**`) 생성·수정·삭제·skip 금지, 소스 프로젝트(`D:\ai\ADTM` 등) 수정 금지, `data/`·`target/`·`backup/` 수정 금지, 실행 중인 DB에 직접 쓰기 금지(스키마 변경은 `schema.sql` 등 파일로만), 비밀값 출력 금지, 커밋·푸시 금지 |
| 근거 규칙 | 업무 규칙·권한·상태값·DB 의미는 근거 파일과 줄 번호로 보고하고, 근거가 없으면 `미확인`으로 남긴다 |
| 완료 증거 | 변경 파일 목록, 핵심 변경, 실행한 명령과 결과(예: `verify.cmd` exit code), 미완료·미확인 항목 |

## 8. Orca 명령 (실측 확인된 것)

```powershell
# 상태 확인
orca status --json
orca terminal list --json          # 열린 Freebuff/OpenCode 터미널 handle 확인 (agentIdentity 필드)

# Freebuff Worker 새로 시작 (확인됨). Freebuff는 --keep-open을 쓰지 않는다(5장)
# spec이 길거나 따옴표가 많으면 --spec 대신 --spec-file <파일>을 쓴다 (확인됨)
orca orchestration worker-start --agent freebuff --worktree current --spec "<자기완결적 작업 지시>" --json

# OpenCode2(Sol) Worker 새로 시작 (확인됨, 2026-11-18). 설정 모델 GPT-6.1 Sol로 뜬다. KR bridge 필요(3장)
# 같은 Run에 묶으려면 --run <runId>를 붙인다 (확인됨)
orca orchestration worker-start --agent opencode2 --worktree current --run <runId> --spec-file <spec 파일> --json

# 이미 열린 터미널(OpenCode2 Sol 등)에 작업 넣기 (확인됨). --worktree, --model 같이 쓰지 않음
# 단, 실행 중인 Freebuff에 넣은 지시는 제출되지 않으므로 쓰지 않는다(4.2절). 사용자가 대화 중인 터미널에도 넣지 않는다
orca orchestration worker-start --terminal <term_handle> --spec "<작업 지시>" --json

# 결과 수신: Worker를 모두 시작한 뒤 총괄은 차례를 끝낸다. Worker가 보고하면 Orca가 총괄을 깨우고
# 실행할 check 명령을 알려준다 (확인됨). 그 명령만 실행한다
orca orchestration check --run <runId> --terminal <총괄 terminal handle>

# 진행 상황 보기 (확인됨). 깨움을 기다리는 동안 한 번 확인할 때만 쓴다
orca orchestration worker-read --dispatch <dispatchId> --source auto

# 후속 지시 (확인됨). 닫힌 Worker면 새로 시작되며 이전 기억 없음
orca orchestration send --to dispatch:<dispatchId> --subject "<제목>" --body "<지시>" --json

# Worker 상태 목록
orca orchestration worker-list --run <runId> --json
```

- **결과를 기다리려고 `check --wait`를 돌리거나 `check`를 반복 실행하지 않는다.** 현재 Orca 가이드(`orca skills get orchestration`)에 따르면 직접 실행한 `check`가 메일을 가져가 버려 Orca가 총괄을 깨울 신호가 사라진다. 원 기록의 `check --wait` 방식은 쓰지 않는다.
- `--agent opencode`(OpenCode 1)는 Orca에서 비활성 상태다 (`agent_unconfigured`).
- `worker-start`가 실패하면 같은 명령을 그대로 다시 실행하지 말고 `failedStage`를 확인한다 (`orca skills get orchestration --reference references/recovery-and-cleanup.md`).
- 별도 Orca 데스크톱 화면 없이 CLI만으로 제어할 수 있다. 상세 사용법은 `orca skills get orchestration --full`. Worker 종료 상태(`reclaimable`·`autoReleaseAt`)는 5장에 있다.

## 9. 동작 테스트 기록

| 일시 | 환경 | 내용 | 결과 |
| --- | --- | --- | --- |
| 2026-11-18 | Arkive 작업 PC, Orca 1.4.263, Freebuff 0.2.36, OpenCode v2.0.22, 기준 SHA `3dcc202` | Run `run_7feeb672f354`에 Freebuff·OpenCode2 Worker를 `--worktree current`로 각각 시작. 같은 읽기 전용 spec(`src/test/java`의 테스트 클래스 수·`@Test` 수 집계, 수정·빌드·커밋 금지) | 둘 다 `worker_done`·`succeeded`, 둘 다 84개 / 338개 / `@ParameterizedTest` 0개로 정답(`src/test/README.md`)과 일치. 작업 전후 `git status` 동일(수정 없음). Orca 깨움으로 결과 수신 확인. Freebuff는 빠르고 첫 집계 오류(`@TestPropertySource` 혼입)를 스스로 수정, OpenCode2는 느리지만 기준 SHA·전후 상태까지 보고. Sol 비용은 진행 중 화면 기준 $0.07 이상(최종 금액 미확인) |
| 2026-11-18 | Arkive 작업 PC, Orca 1.4.263, Freebuff(DeepSeek V4.1 Flash · high), ego-browser 0.6.3.14, 기준 SHA `c4614c7` | Run `run_e61903ccce8c`, Dispatch `ctx_721fbb5df7ed`. 6.1절 분담 첫 적용: 휴가관리 GUI 시나리오(신청→빈 사유 오류→승인→사용 연차 +1→취소 원복) | Worker가 ego-browser 작업 공간을 만들어 6단계 수행, 스크린샷 12장 저장, 단계별 표 보고(약 5분). 총괄이 스크린샷 대조와 ego-browser 재조회로 최종 상태 일치 확인. Worker는 png 외 파일 수정 없음(`git status` 확인). 같은 시점 사용자 Freebuff 탭과 남은 시간이 달라 새 세션이 시작된 것으로 판단(4장) |
| 2026-11-21 | Arkive 작업 PC, Freebuff(`~/.config/manicode/freebuff.exe`), 기준 HEAD `f0c5da4` | 문서 4장 사전 확인 방법 재검증 및 세션 이어받기 확인 (Dispatch `ctx_9c9ff1468c44`) | `Get-Process freebuff`·라이브 파일 정상 동작. 20:35에는 세션 등록 전이라 프로세스·파일이 없었으나(프로세스 20:38:23 시작, 세션 20:42:51 등록), 이후 PID 14484·ownerPid 14484·만료 21:42:51 확인. 탭을 끈 뒤 Worker로 `hi` 전달 → 같은 `instanceId`·`expiresAt` 이어받기, 2시간 차감 없음(화면 `39m left`). 단순 지시에서 정책 거부 없었으나 11-18 경고는 유지. 일일 잔여 시간 스크립트(`usage.mjs`)는 이 PC에 없어 해당 항목은 `미확인` 유지 |

- 아직 검증하지 않은 것: `--worktree new-child`(별도 작업 폴더) 생성·병합·정리 흐름, 파일을 수정하는 Worker와 검증 Worker를 나눈 실제 작업, 이 PC의 Freebuff 일일 남은 시간 확인 방법, `worker-start --terminal`로 사용자가 연 Freebuff 터미널에 넣었을 때 세션 추가 차감이 없는지.
- 확인된 것: Orca Worker(Freebuff)가 ego-browser를 실제로 조작하고 스크린샷을 저장할 수 있다(위 2026-11-18 휴가관리 시험).
- 2026-11-18 23:45 Run `run_e61903ccce8c` Dispatch `ctx_6889111ab75e`(E 일정·근무 묶음): Flash 새 세션(2시간 차감, 남은 시간 0/15)이 열린 뒤 첫 지시가 Freebuff 이용 정책("Driven by another agent")으로 거부되고 strike 1이 기록됨. 총괄은 추가 전송 없이 Dispatch를 abandon 처리했다. 같은 Run의 A1(`ctx_defe583019a5`)·H(`ctx_ef8279d920c7`)는 앞서 정상 수행·보고됐다.
- 참고 사례: 커밋 `6a7ad0d`(2026-11-16)는 Worker가 항목별 GUI 기록 없이 `README.md`의 33개 항목을 「구현 및 검증완료」로 직접 옮겼다. 6.1절은 이 재발을 막기 위한 규칙이다.
