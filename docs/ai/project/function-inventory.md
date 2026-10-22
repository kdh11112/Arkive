# 기능 목록 및 소유권 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. 기능 단위로 1행을 만든다. 화면·Controller·Service·Mapper·외부연계를 추적해 적는다.
2. 책임자/소유권은 담당자 확인 있을 때만 적고, 없으면 미확인으로 둔다.
3. 이식용으로 가져갈 기능은 복사 대상(이식 시 복사 범위) 칸에 함께 적는다.
4. DB 종속 문법(HSQL 전용 함수 등)은 비고에 표시한다.

## 양식 (Arkive 기록 참고)

| 기능/영역 | 구현 위치 (화면·Controller·Service·Mapper) | 범위 | 책임자/소유권 | 이식 시 복사 대상 |
| --- | --- | --- | --- | --- |
| 예: 메뉴 관리 | `biz.menu.*`, `Menu_SQL.xml` | 목록·등록·수정·삭제 | 미확인 | 화면+Controller+Service+Mapper+SQL+V2 |

<!--
문서 상태: 부분 미완성
미완성 항목: 전체 업무 기능 목록, 기능별 책임 팀/담당자, 기능별 지원 범위와 사용 여부.
필요한 근거/확인자: 업무 담당 조직, 운영 담당자, 사용자/화면 목록, 배포 대상 기능 목록.
완성 방법: 기능별 화면·API·Service·Mapper·외부 연계를 조사하고 담당 조직이 소유권을 확인한 뒤 각 행을 추가·검토한다.
완료 기준: 운영 대상 기능이 빠짐없이 분류되고 모든 기능 소유자가 확인되었거나, 소유권 미정 항목에 의사결정 담당과 기한이 기록되어 있다.
-->

## Arkive 작성 예시

> 2026-10-11 및 2026-10-22 저장소 검색으로 확인한 주요 코드 영역의 목록이다. 전체 기능 목록이나 조직별 소유권을 확정한 문서가 아니다. 소유자는 근거가 없어 미확인으로 둔다.

| 기능/영역 | 확인된 구현 위치 | 확인된 범위 | 책임자/소유권 |
| --- | --- | --- | --- |
| 시스템 메뉴·공통코드 관리 | `arkive.admin.system.*`, `System_SQL.xml`, `CommCode_SQL.xml`, `templates/thymeleaf/system/` | 메뉴·코드 목록·상세·등록·수정·삭제 (`/system` prefix) | 미확인 |
| 로그인·회원·계정 찾기 | `arkive.admin.auth.web.LoginController`, `Auth_SQL.xml`(APP_USER), `templates/thymeleaf/login/` | 로그인·회원가입·ID/PW찾기·로그아웃 (`/login`) | 미확인 |
| 사용자관리 | `arkive.admin.auth.web.UserManageController`, `templates/thymeleaf/auth/` | 목록·등록·수정·삭제·잠금해제·비번초기화 (`/auth`). 메뉴 `ME02010400` | 미확인 |
| 소셜 로그인 연계 | `arkive.admin.auth.web.OauthController`, `OAUTH_LINK` | 제공자 연결·콜백·연계 (`/login/oauth`) | 미확인 |
| NICE 본인 확인 | `NiceCheckController`(`/niceExt`), `NiceVerifyController`(`/login/nice`) | 연계·시작·콜백·상태 | 미확인 (외부 계약은 연계 명세 확인) |
| 공통 파일 처리 | `arkive.admin.comm.web.FileController`, `FileService*`, `File_SQL.xml` | 멀티파트·TUS·드롭존 업로드, 다운로드, 삭제 (`/system`, `/file`) | 미확인 |
| 게시판 (관리자) | `arkive.admin.board.*`, `Board_SQL.xml` | CK·토스트 목록·쓰기·상세·등록·수정·삭제·구독 (`/board`) | 미확인 |
| 팝업·배너·카테고리·일정·POLL·상담·명함·주소록·당직·메모·FAQ·Q&A (관리자) | `arkive.admin.popup/banner/category/schedule/poll/counsel/card/address/duty/memo/faq/qna.*` | 각 `/popup`·`/banner`·`/category`·`/schedule`·`/poll`·`/counsel`·`/card`·`/address`·`/duty`·`/faq`·`/memo`·`/qna` prefix | 미확인 |
| 외부포털 (사용자) | `arkive.user.board/faq/qna/poll/counsel/main.*` | 공지·자료·FAQ·Q&A·POLL·상담·메인 (`/user/**`) | 미확인 |
| 샘플 기능 | `egovframework.example.sample.*`, `EgovSample_Sample_SQL.xml` | eGovFrame 샘플 CRUD 및 페이징 코드가 있음 | 샘플 코드, 업무 소유권 해당 없음 |
| 시스템 설정·예약 DB 작업 | `egovframework.example.config.*`, `arkive.com.config.*` | DB·Mapper 설정, 10분 Trigger와 덤프 작업 코드가 있음 | 운영 책임자 미확인 |
| 게시글 댓글 | `arkive.admin.board.web.BoardController`(댓글 API), `BoardService`/`BoardServiceImpl`, `BoardMapper`, `Board_SQL.xml`, `templates/thymeleaf/board/boardDetail.html` | 로그인 사용자 1단 댓글 목록·등록·삭제, 작성자/관리자 삭제 (`/board/insertComment.json`, `/board/deleteComment.json`) | 미확인 |
| 쪽지관리 | `arkive.admin.message.*`, `Message_SQL.xml`, `templates/thymeleaf/message/` | 받은/보낸 목록·검색·페이징·발송·상세·읽음·함별 소프트 삭제 (`/message`) | 미확인 |

## 테스트 자산 현황 (2026-10-22)

전체 33개 테스트, instruction 9.1% / branch 6.4%. 커버리지 숫자만으로 품질을 판단하지 않고, 아래처럼 기능별 검증 범위와 공백을 함께 본다.

| 기능/영역 | 자동 테스트 | 검증 범위 | 남은 공백 |
| --- | --- | --- | --- |
| 컨텍스트·MyBatis | `ArkiveSmokeTest` 1 | 컨텍스트 로딩, SqlSessionFactory 배선 | - |
| 게시판 CRUD | `BoardMapperTest` 3 | insert/select/delete, null content, 댓글 cascade | 페이징 경계, 검색 조건, 첨부 연결 |
| 게시글 댓글 API | `BoardCommentControllerTest` 3 | 401, 게시글/내용 검증, 작성자·관리자 삭제 | 수정·신고, 관리자 판정 범위 |
| 쪽지 | `MessageMapperTest` 2, `MessageControllerTest` 4 | 받은/보낸함 분리, 읽음, 소프트 삭제, 검색, 401, 중복제출, 함별 권한 | 보존·파기 정책, 수신자 삭제 후 발신자 표시, 메일·알림 연계 |
| HTML sanitize | `HtmlSanitizerTest` 6 | script·이벤트·iframe·표·style | 실제 에디터 출력 회귀 |
| 파일 실형식 | `FileTypeUtilTest` 5 | 매직바이트, 확장자 위조, 허용 목록 | 업로드 경로·다운로드 권한·TUS |
| JWT | `JwtUtilTest` 4 | 발급·검증, 시크릿 불일치, 만료·형식 오류 | 키 회전·로그아웃 블랙리스트 |
| 로그인 인터셉터 | `AuthInterceptorTest` 5 | 공개 경로, JSON 401, 화면 redirect, 관리자 경로 | 세션 고정·CSRF·API 필터 |
| 나머지 기능 | 없음 | - | 로그인·잠금·OAuth·NICE, 파일, 엑셀, POLL 중복, 상담 비밀번호, Q&A 상태 등 |

기능을 추가하거나 이식할 때는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)에 실행·복사·검증 결과를 남기고, 이 표의 테스트 자산도 함께 갱신한다.
테스트를 다른 프로젝트로 옮길 때는 [test-porting-sheet.md](../portable/test-porting-sheet.md)에 복사·수정·검증 결과를 남긴다.

## 유지 기준

- 기능을 추가·변경할 때 실제 화면, Controller, Service, Mapper/SQL, 외부 연계와 호출처를 확인해 행을 갱신한다.
- Arkive는 타 프로젝트로의 기능 이식을 전제로 한다. 기능을 추가할 때는 이식에 필요한 의존성(pom.xml), 설정(Globals 키·application.properties), DB 객체(db/versions), 외부 연계 JAR/키를 함께 기록한다.
- DB 제품이 달라질 수 있으므로(Oracle/PostgreSQL 등) DB 종속 문법과 HSQL 전용 함수는 비고에 표시한다.
- `@RequestMapping` 등 코드상 경로가 있다는 사실과 사용 중인 업무 기능이라는 사실을 구분한다.
- 책임 조직·담당자는 업무 담당자나 공식 문서 근거가 있을 때만 기록한다.
