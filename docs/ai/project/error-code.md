# 오류 처리 및 코드 체계 (가이드형 템플릿)

> 이식 구분: 가이드형 템플릿. 새 프로젝트에 복사한 뒤 양식만 채운다. 중앙 코드체계 없으면 없다고 명시한다.

<!--
문서 상태: 부분 미완성
미완성 항목: 중앙 레지스트리 존부 담당자 확인, .json 오류의 HTTP 상태 승인, 로그 상관 ID·재시도 기준.
필요한 근거/확인자: API 명세, 운영 로그·모니터링 기준, API 및 운영 담당자.
완성 방법: 아래 Arkive 예시는 코드에서 확인됨. 계약으로 승격하려면 담당자 확인을 근거에 남긴다.
완료 기준: 사용 중인 오류 코드와 메시지/HTTP 매핑이 출처에 연결되고, 레지스트리 미사용 여부도 담당자 확인으로 명시되어 있다.
-->

> 중앙 오류 코드 레지스트리는 코드에서 확인되지 않았다. 아래는 실제 코드에서 확인한 현행 방식이다. 계약 승인은 받지 않았으므로 스펙으로 쓰지 않는다.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 처리 구성 (Arkive 예시, 2026-10-11 코드 기준)

- 중앙 레지스트리 없음. `@ControllerAdvice` 전역 처리 없음 (`SystemController.java:12`의 import는 미사용).
- eGovFrame 예외 핸들러 2종(`EgovSampleExcepHndlr`, `EgovSampleOthersExcepHndlr`)은 로그(`debug`)만 남긴다.
- `SimpleMappingExceptionResolver`는 샘플 전용 뷰(`sample/egovSampleError`)로 보낸다 (`EgovConfigWeb.java:221-232`).
- JSON 오류 규약이 2종 혼재한다. `.json` 실패의 HTTP 상태 명시 코드는 확인되지 않았다 (기본 200으로 보이지만 선언 근거 없음).

| 규약 | 형태 | 사용처 |
| --- | --- | --- |
| 신 | `success: true/false` + `message: e.getMessage()` | `UserManageController`, `OauthController`, `user/qna·poll·counsel` 등 |
| 구 | `result: "success"/"fail"` + `message` | `SystemController`, `CommCodeController` |

- 예외 메시지가 그대로 `message`로 전달된다. 스택은 주지 않는다 (`ApiController.java:105` 주석 확인).
- 확인된 도메인 오류 메시지: 로그인 5회 잠금 (`LoginController.java:85`), OAuth 토큰·프로필 실패 (`OauthController.java:189,221`), NICE 미계약 차단 (`NiceVerifyController.java:30`), JWT·인터셉터 401 직접 응답 (`JwtAuthFilter.java:73`, `AuthInterceptor.java:61`), 파일 다운로드 404 (`FileController.java:212` 등).
- 화면 오류: `templates/thymeleaf/error/error.html` (`fail.common.msg`), `static/error/403.html`. 메시지는 `message-common*.properties` 3종(기본·ko·en).
- 로그 상관 ID, 재시도 기준, 알림: 미확인.

## 오류 계약 등록 양식

| 코드 | HTTP 상태/화면 처리 | 의미 | 사용자 표시 메시지 키 | 로그 수준/필드 | 재시도 가능 | 근거/소유자 |
| --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |

오류 체계를 새로 추가하거나 변경할 때는 API 호출자와 화면 사용처, 다국어 메시지, 모니터링 및 개인정보 로그 정책을 확인한다. 프로젝트 담당자 승인 없이 새 코드 체계를 단정하지 않는다.
