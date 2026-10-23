# 오류 처리 및 코드 체계 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 중앙 레지스트리 존부 담당자 확인, .json 오류의 HTTP 상태 승인, 로그 상관 ID·재시도 기준.
필요한 근거/확인자: API 명세, 운영 로그·모니터링 기준, API 및 운영 담당자.
완성 방법: 아래 Arkive 예시는 코드에서 확인됨. 계약으로 승격하려면 담당자 확인을 근거에 남긴다.
완료 기준: 사용 중인 오류 코드와 메시지/HTTP 매핑이 출처에 연결되고, 레지스트리 미사용 여부도 담당자 확인으로 명시되어 있다.
-->

> 중앙 오류 코드 레지스트리는 코드에서 확인되지 않았다. 아래는 실제 코드에서 확인한 현행 방식이다. 계약 승인은 받지 않았으므로 스펙으로 쓰지 않는다.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 처리 구성 (Arkive 예시, 2026-10-11·2026-10-22 코드 기준)

- 중앙 레지스트리 없음. `@ControllerAdvice` 전역 처리 없음.
- eGovFrame 예외 핸들러 2종(`EgovSampleExcepHndlr`, `EgovSampleOthersExcepHndlr`)은 로그(`debug`)만 남긴다.
- `SimpleMappingExceptionResolver`는 샘플 전용 뷰(`sample/egovSampleError`)로 보낸다 (`EgovConfigWeb.java:221-232`).
- JSON 오류 규약이 2종 혼재한다. `.json` 실패의 HTTP 상태 명시 코드는 확인되지 않았다 (기본 200으로 보이지만 선언 근거 없음).

| 규약 | 형태 | 사용처 |
| --- | --- | --- |
| 신 | `success: true/false` + `message: e.getMessage()` | `UserManageController`, `OauthController`, `BoardController`(댓글), `MessageController`, `user/qna·poll·counsel` 등 |
| 구 | `result: "success"/"fail"` + `message` | `SystemController`, `CommCodeController` |

- 예외 메시지가 그대로 `message`로 전달된다. 스택은 주지 않는다 (`ApiController.java:105` 주석 확인). 댓글·쪽지 JSON 실패는 Controller에서 HTTP 상태를 명시하지 않아 200 + `success:false`로 보이며, 인증 실패는 `AuthInterceptor`가 401 JSON을 반환한다.
- 댓글·쪽지의 예상 검증 실패(로그인·권한·길이·중복)도 `BoardController`·`MessageController`에서 `logger.error`로 스택을 남긴다. 운영 로그에서 예상 오류와 장애를 구분하려면 로그 수준 조정이 필요하다(정책 미확인).
- 확인된 도메인 오류 메시지: 로그인 5회 잠금 (`LoginController.java:85`), OAuth 토큰·프로필 실패 (`OauthController.java:189,221`), NICE 미계약 차단 (`NiceVerifyController.java:30`), JWT·인터셉터 401 직접 응답 (`JwtAuthFilter.java:73`, `AuthInterceptor.java:61`), 메뉴 권한 403 (`MenuPermissionInterceptor`), 파일 다운로드 404 (`FileController.java:212` 등).
- 메뉴 권한 거부: `.json` 요청은 HTTP 403 + `{"success":false,"message":"권한이 없습니다."}`, 화면 요청은 `sendError(403)`으로 처리한다. 로그인하지 않은 요청은 `AuthInterceptor`가 401 JSON 또는 로그인 화면 redirect를 담당한다.
- 게시글 댓글 메시지 예: "로그인이 필요합니다.", "게시글이 없습니다.", "댓글 내용을 입력하세요.", "댓글은 1000자 이내로 입력하세요.", "삭제 권한이 없습니다."
- 쪽지 메시지 예: "받는 사람 아이디를 입력하세요.", "제목을 입력하세요.", "내용을 입력하세요.", "제목은 200자 이내로 입력하세요.", "내용은 4000자 이내로 입력하세요.", "자신에게는 쪽지를 보낼 수 없습니다.", "받는 사람을 찾을 수 없습니다.", "중복 제출이 감지되었습니다. 보낸쪽지함에서 발송 여부를 확인하세요.", "삭제 권한이 없습니다.", "이미 삭제된 쪽지입니다."
- 화면 오류: `templates/thymeleaf/error/error.html` (`fail.common.msg`), `static/error/403.html`. 메시지는 `message-common*.properties` 3종(기본·ko·en).
- 로그 상관 ID, 재시도 기준, 알림: 미확인.

## 오류 계약 등록 양식

| 코드 | HTTP 상태/화면 처리 | 의미 | 사용자 표시 메시지 키 | 로그 수준/필드 | 재시도 가능 | 근거/소유자 |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

오류 체계를 새로 추가하거나 변경할 때는 API 호출자와 화면 사용처, 다국어 메시지, 모니터링 및 개인정보 로그 정책을 확인한다. 프로젝트 담당자 승인 없이 새 코드 체계를 단정하지 않는다.
