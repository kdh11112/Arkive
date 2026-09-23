# API 계약 현황

<!--
문서 상태: 부분 미완성
미완성 항목: 전체 API 경로, 허용 HTTP method, Request/Response 스키마, 인증·권한, 상태/오류 코드, 호환성 및 외부 API 명세.
필요한 근거/확인자: Controller와 실제 호출 화면·클라이언트, 승인된 OpenAPI/API 명세, 연계 시스템 담당자.
완성 방법: 경로 인벤토리를 호출처와 대조하고 계약 기록 양식을 사용해 필드·상태·오류를 확인한다. 코드 선언만으로 계약을 확정하지 않는다.
완료 기준: 운영/지원 대상 API가 명세 또는 확인된 현행 계약에 연결되고, 미지원·미확인 경로가 구분되어 있다.
-->

> 아래는 코드에 선언된 경로의 일부 인벤토리다. 정식 계약 문서가 아니며 Request/Response 스키마, 인증, 오류 응답은 코드와 승인된 명세를 추가 확인해야 한다. `@RequestMapping`에 HTTP method가 지정되지 않은 경우 메서드를 임의로 정하지 않는다.

## 확인된 시스템 메뉴 경로

Controller 기준 prefix는 `/system`이다.

| 경로 | 코드상 기능 설명 | HTTP method | 계약 상태 |
| --- | --- | --- | --- |
| `/system/menuList` | 메뉴 관리 화면 | 미지정 | 화면 경로만 확인 |
| `/system/getMenuList.json` | 메뉴 목록 조회 | 미지정 | 파라미터/응답 스키마 미확인 |
| `/system/getMenuDetailList.json` | 하위 메뉴 조회 | 미지정 | `menuId` 읽는 코드 확인, 스키마 미확인 |
| `/system/getChkMenuId.json` | 메뉴 중복 조회 | 미지정 | Request/응답 의미 확인 필요 |
| `/system/getChkUpMenuId.json` | 상위 메뉴 중복 조회 | 미지정 | Request/응답 의미 확인 필요 |
| `/system/setInsertMenu.json` | 메뉴 등록 | 미지정 | 입력/오류 계약 확인 필요 |
| `/system/setUpdateMenu.json` | 메뉴 수정 | 미지정 | 입력/오류 계약 확인 필요 |
| `/system/setDeleteMenu.json` | 메뉴 삭제 | 미지정 | 입력/오류 계약 확인 필요 |

## 추가 인벤토리

- `arkive.admin.comm.web.FileController`에는 파일 업로드·다운로드, TUS, 파일 목록/삭제 등 다수 경로가 선언되어 있다. 정식 목록은 Controller 전체와 클라이언트 호출부를 대조해 유지한다.
- `arkive.admin.comm.web.NiceCheckController`에는 `/niceExt` prefix와 본인 확인 관련 경로가 선언되어 있다. 외부 계약은 연계 명세를 확인한다.
- `egovframework.example.sample.web.EgovSampleController` 경로는 샘플 기능으로 취급한다.

## 계약 기록 양식

| 경로/기능 | HTTP method | 인증/권한 | Request | Response/상태 코드 | 오류/재시도 | 호환성/버전 | 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | 미확인 |  |  |  |  |  |

API 계약을 수정할 때 Controller와 JavaScript/호출 시스템 사용처를 함께 확인한다. 코드 구현을 의도된 계약으로 간주하지 말고 업무/API 담당자 확인을 남긴다.
