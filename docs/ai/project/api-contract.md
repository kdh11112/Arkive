# API 계약 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

## 작성 가이드 (Arkive 기록 참고)

1. Controller의 `@RequestMapping` 경로를 prefix별로 묶어 인벤토리 표에 적는다. method 미지정이면 임의로 정하지 않는다.
2. 각 경로는 호출 화면·JS와 대조한 뒤 계약 기록 양식에 Request/Response·인증·오류를 적는다.
3. 코드 선언만으로 계약을 확정하지 말고 명세·담당자 확인을 근거에 남긴다.

## 양식 (Arkive 기록 참고)

| 경로/기능 | HTTP method | 인증/권한 | Request | Response/상태 코드 | 오류/재시도 | 근거 |
| --- | --- | --- | --- | --- | --- | --- |
| 예: `/biz/list` | 미지정 (임의기재 금지) | 미확인 |  |  |  | Controller·화면 경로 |

<!--
문서 상태: 부분 미완성
미완성 항목: Request/Response 스키마, 인증·권한, 상태/오류 코드, 호환성 및 외부 API 명세. 경로·method 인벤토리는 2026-10-11 대조분에 2026-10-22 댓글·쪽지 경로를 반영.
필요한 근거/확인자: 실제 호출 화면·클라이언트, 승인된 OpenAPI/API 명세, 연계 시스템 담당자.
완성 방법: 경로 인벤토리를 호출처와 대조하고 계약 기록 양식을 사용해 필드·상태·오류를 확인한다. 코드 선언만으로 계약을 확정하지 않는다.
완료 기준: 운영/지원 대상 API가 명세 또는 확인된 현행 계약에 연결되고, 미지원·미확인 경로가 구분되어 있다.
-->

> 아래는 코드에 선언된 경로의 일부 인벤토리다. 정식 계약 문서가 아니며 Request/Response 스키마, 인증, 오류 응답은 코드와 승인된 명세를 추가 확인해야 한다. `@RequestMapping`에 HTTP method가 지정되지 않은 경우 메서드를 임의로 정하지 않는다.

## 작성 예시 (Arkive) — 아래는 참고용

## 확인된 경로 인벤토리 (Arkive 예시, 2026-10-11·2026-10-22·2026-10-23 코드 기준)

클래스 prefix 기준이다. 화면 경로는 GET+POST, `.json`은 POST가 기본이다 (`/system`·`/file` 예외 별기).

| prefix | Controller | 대표 경로 | HTTP method (코드 선언) |
| --- | --- | --- | --- |
| `/system` | `admin.system.web.SystemController` | `/menuList`, `/getMenuList.json`, `/getMenuDetailList.json`, `/getChkMenuId.json`, `/getChkUpMenuId.json`, `/setInsertMenu.json`, `/setUpdateMenu.json`, `/setDeleteMenu.json` | 화면 GET, 조회·등록·수정·삭제별 GET/POST 선언 (코드 확인) |
| `/system` | `admin.system.web.CommCodeController` | `/commCodeList`, `/getMasterList.json`, `/getDetailList.json`, `/getCmmnCodeList.json`, `/getCodeInfo.json`, `/setInsertCode.json`, `/setUpdateCode.json`, `/setDeleteCode.json` | 화면 GET, `.json` POST |
| `/system`, `/file` | `admin.comm.web.FileController` | `/multipartUpload`, `/tusUpload`, `/tusPopup`, `/dropzoneUpload`, `/dropzonePopup`, `/tus`, `/tus/{fileId}`, `/file/tempFileUpload.json`, `/file/saveTempFiles.json`, `/file/setFileDelete.json`, `file/downloadFile.do` | 화면 GET, TUS에 OPTIONS/HEAD/PATCH/DELETE 있음 |
| `/login` | `admin.auth.web.LoginController` | `/login`, `/login.json`, `/signup`, `/signup.json`, `/findId`, `/findId.json`, `/findPw`, `/findPw.json`, `/resetPw`, `/resetPw.json`, `/logout`, `/apiDemo` | 화면 GET+POST, `.json` POST |
| `/auth` | `admin.auth.web.UserManageController` | `/userList`, `/userWrite`, `/insertUser.json`, `/updateUser.json`, `/deleteUser.json`, `/unlockUser.json`, `/resetPassword.json` | 화면 GET+POST, `.json` POST |
| `/auth` | `admin.auth.web.RoleManageController` | `/roleList`, `/roleWrite`, `/saveRole.json`, `/deleteRole.json` | 화면 GET+POST, `.json` POST |
| `/login/oauth` | `admin.auth.web.OauthController` | `/{provider}`, `/{provider}Callback`, `/link.json` | 화면 GET+POST, 연계 POST |
| `/login/nice` | `admin.auth.web.NiceVerifyController` | `/start`, `/callback`, `/status.json`, `/clear.json` | GET+POST 혼합, 해제 POST |
| `/api` | `admin.auth.web.ApiController` | `/login.json`, `/noticeList.json`, `/noticeDetail.json`, `/faqList.json` | POST 또는 GET+POST |
| `/board` | `admin.board.web.BoardController` | `/ckeditorList`, `/toastList`, `/toastList.json`, `/ckeditorWrite`, `/toastWrite`, `/detail`, `/insertBoard.json`, `/updateBoard.json`, `/deleteBoard.json`, `/updateNotice.json`, `/insertComment.json`, `/deleteComment.json`, `/subscribe`, `/editorImageUpload.json` | 화면 GET+POST, `.json` POST, 구독 GET |
| `/sitemap` | `admin.comm.web.SitemapController` | `/sitemap` | 화면 GET, 로그인 사용자 권한별 메뉴 트리 |
| `/search` | `admin.search.web.SearchController` | `/search` | 화면 GET, 로그인 필요, 제목 LIKE 통합검색 (CK·TOAST·FAQ·QNA) |
| `/message` | `admin.message.web.MessageController` | `/inbox`, `/sent`, `/write`, `/detail`, `/insertMessage.json`, `/deleteMessage.json` | 화면 GET+POST, `.json` POST |
| `/popup` | `admin.popup.web.PopupController` | `/popupList`, `/popupWrite`, `/activePopup.json`, `/popupImageUpload.json`, `/insertPopup.json`, `/updatePopup.json`, `/deletePopup.json` | 화면 GET+POST, `.json` POST |
| `/banner` | `admin.banner.web.BannerController` | `/bannerList`, `/bannerWrite`, `/activeBanner.json`, `/bannerImageUpload.json`, `/insertBanner.json`, `/updateBanner.json` | 화면 GET+POST, `.json` POST |
| `/category` | `admin.category.web.CategoryController` | `/tree`, `/drill`, `/tree.json`, `/saveCategory.json`, `/deleteCategory.json` | 화면·조회 GET+POST, 저장·삭제 POST |
| `/schedule` | `admin.schedule.web.ScheduleController` | `/scheduleList`, `/scheduleWrite`, `/insertSchedule.json`, `/updateSchedule.json`, `/deleteSchedule.json`, `/holidayList`, `/insertHoliday.json`, `/deleteHoliday.json` | 화면 GET+POST, `.json` POST |
| `/poll` | `admin.poll.web.PollController` | `/pollList`, `/pollWrite`, `/pollDetail`, `/insertPoll.json`, `/updatePoll.json`, `/deletePoll.json`, `/votePoll.json` | 화면 GET+POST, `.json` POST |
| `/counsel` | `admin.counsel.web.CounselController` | `/counselList`, `/counselWrite`, `/counselDetail`, `/answerList`, `/checkPassword.json`, `/counselFileUpload.json`, `/insertCounsel.json`, `/updateCounsel.json`, `/deleteCounsel.json`, `/updateCounselAnswer.json` | 화면 GET+POST, `.json` POST |
| `/card` | `admin.card.web.CardController` | `/cardList`, `/cardWrite`, `/insertCard.json`, `/updateCard.json`, `/deleteCard.json`, `/useCard.json`, `/cancelUseCard.json` | 화면 GET+POST, `.json` POST |
| `/address` | `admin.address.web.AddressController` | `/addressList`, `/addressWrite`, `/insertAddress.json`, `/updateAddress.json`, `/deleteAddress.json` | 화면 GET+POST, `.json` POST |
| `/duty` | `admin.duty.web.DutyController` | `/dutyList`, `/dutyWrite`, `/insertDuty.json`, `/updateDuty.json`, `/deleteDuty.json`, `/downloadDutyExcel.do`, `/downloadDutyTemplate.do`, `/uploadDutyExcel.json` | 화면 GET+POST, `.json` POST, 엑셀 GET |
| `/faq` | `admin.faq.web.FaqController` | `/faqList`, `/faqWrite`, `/insertFaq.json`, `/updateFaq.json`, `/deleteFaq.json` | 화면 GET+POST, `.json` POST |
| `/memo` | `admin.memo.web.MemoController` | `/memoList`, `/memoWrite`, `/memoDetail`, `/insertMemo.json`, `/updateMemo.json`, `/deleteMemo.json`, `/confirmMemo.json`, `/opinionMemo.json` | 화면 GET+POST, `.json` POST |
| `/qna` | `admin.qna.web.QnaController` | `/qnaList`, `/qnaWrite`, `/qnaDetail`, `/insertQna.json`, `/updateQna.json`, `/deleteQna.json`, `/saveAnswer.json` | 화면 GET+POST, `.json` POST |
| `/niceExt` | `admin.comm.web.NiceCheckController` | `/niceCheckAjax.do`, `/niceOpen.do` | method 미지정 (임의기재 금지). 외부 계약은 연계 명세 확인 |
| `/preview` | `admin.preview.web.PreviewController` | `/fileView` (GET, `fileId` 파라미터), `/canPreview.json` (GET+POST, `success`+`canPreview`) | 코드 선언 확인 |
| `/user` | `user.main.web.UserMainController` | `/userMain` | GET+POST |
| `/user/board` | `user.board.web.UserBoardController` | `/noticeList`, `/noticeDetail`, `/archiveList`, `/archiveDetail` | GET+POST |
| `/user/faq` | `user.faq.web.UserFaqController` | `/faqList` | GET+POST |
| `/user/qna` | `user.qna.web.UserQnaController` | `/qnaList`, `/qnaDetail`, `/qnaWrite`, `/insertQna.json` | 화면 GET+POST, 등록 POST |
| `/user/poll` | `user.poll.web.UserPollController` | `/pollList`, `/pollDetail`, `/votePoll.json` | 화면 GET+POST, 투표 POST |
| `/user/counsel` | `user.counsel.web.UserCounselController` | `/counselWrite`, `/counselFileUpload.json`, `/insertCounsel.json` | 화면 GET+POST, `.json` POST |
| `/` | `com.controller.ArkiveController` | `/`, `/dashboard` | 미확인 |
| 샘플 | `egovframework.example.sample.web.EgovSampleController` | `/egov`, `/egovSampleList.do`, `/addSample.do` 등 | GET/POST 선언됨. 샘플 기능으로 취급 |

## 추가 인벤토리

- 위 표가 전체다. 2026-10-11에 29개 Controller prefix를 대조했고, 2026-10-22에 `/message`를 추가해 30개가 되었다. 2026-10-23에 `/board/updateNotice.json`, `/auth/role*`, `/login/resetPw*`, `/sitemap` 경로를 추가해 31개가 되었고, `/search`를 추가해 32개가 되었다. 선언 method는 각 Controller 코드에서 확인한다.
- `NiceCheckController`(`/niceExt`) 외부 계약은 연계 명세를 확인한다.

## 2026-10-22~23 코드에서 확인한 로그인·댓글·쪽지·공지 고정·메일·재설정 JSON 동작

> 아래는 코드 관찰값이며 승인된 API 계약이 아니다. 성공·실패 모두 Controller에서 HTTP 상태를 명시하지 않아 200으로 보이며, 인증 실패는 `AuthInterceptor`가 401 JSON을 반환한다.

| 경로 | method | 인증/권한 | Request | Response | 오류 |
| --- | --- | --- | --- | --- | --- |
| `/login/login.json` | POST | 공개 | `userId`, `password` | `success:true`, `userNm`, `isAdmin` (새 세션) | `success:false`, `message`; 같은 계정의 이전 세션은 만료 |
| `/login/findPw.json` | POST | 공개 | `userId`, `userNm`, `email` | `success:true`, 고정 안내 메시지. 아이디·이름·이메일이 모두 일치하면 1회용 토큰을 만들고 `MailAsyncService`로 재설정 링크 메일을 비동기 요청 | `success:false`, `message` (필수값 누락); 계정 불일치도 동일한 `success:true` 안내로 존재 여부를 숨김. 같은 사용자 60초 쿨다운 |
| `/login/resetPw` | GET | 공개(토큰) | `token` | 유효하면 새 비밀번호 입력 화면, 아니면 만료 안내 화면 | 토큰 원문은 URL에만 있고 DB에는 SHA-256 해시만 저장 |
| `/login/resetPw.json` | POST | 공개(토큰) | `token`, `password`(8자 이상), `passwordConfirm` | `success:true`, 비밀번호 변경·토큰 사용 처리·실패 횟수/잠금 초기화·기존 로그인 세션 만료 | `success:false`, `message` (만료·사용됨, 8자 미만, 확인 불일치); 입력 검증 실패 시 토큰은 소비하지 않음 |
| `/board/insertBoard.json` | POST | 로그인 + 관리자(중요글 지정 시) | 기존 게시글 입력 + `importantYn`(관리자만 Y) | 저장 성공. 항상 `BoardNotificationEvent(notifySse=true)`를 발행하고, CK 공지/TOAST 중요글이면 `notifyMail=true`도 함께 요청. 실제 발송 실패는 리스너에서 격리 | 기존 오류 + 관리자 아님·중복 제출·제목/내용 검증 |
| `/board/updateBoard.json` | POST | 로그인 + 관리자(중요글 전환 시) | 기존 게시글 입력 + `importantYn` | `BoardNotificationEvent(notifySse=false)`를 발행하고, 중요글 N→Y 전환 시에만 `notifyMail=true`. `BOARD.MAIL_SENT_YN` 선점으로 글당 1회 | 기존 오류 + 관리자 아님·중요글 지정 오류 |
| `/board/updateNotice.json` | POST | 로그인 + 관리자 | `boardId`, `noticeYn` | `BoardNotificationEvent(notifySse=false)`를 발행하고, 공지 Y일 때만 `notifyMail=true`. 이미 발송된 글이면 `MAIL_SENT_YN` 선점 실패로 추가 발송 없음 | 관리자 아님, CK 아님, 게시글 없음, 값 오류 |
| `/board/insertComment.json` | POST | 로그인 세션 | `boardId`, `content`(trim 후 1~1000자) | `success:true`, `commentId` | `success:false`, `message` |
| `/board/deleteComment.json` | POST | 로그인 + 작성자/관리자 | `commentId` | `success:true` | `success:false`, `message` |
| `/message/insertMessage.json` | POST | 로그인 세션 | `receiverId`, `title`(1~200자), `content`(1~4000자), double-submit token | `success:true`, `msgId` | `success:false`, `message` |
| `/message/deleteMessage.json` | POST | 로그인 + 해당 함의 소유자(받은함=수신자, 보낸함=발신자) | `msgId`, `folder`(`inbox`\|`sent`) | `success:true` | `success:false`, `message` |
| `/auth/saveRole.json` | POST | 로그인 + 관리자 세션 (`/auth` 기존 정책) | `mode`(`insert`\|`update`), `roleId`(수정), `roleNm`(1~100자), `description`(0~500자), `useYn`(Y/N), `readMenuIds[]`, `writeMenuIds[]`, `userIds[]` | `success:true`, `roleId` | `success:false`, `message`; 메뉴·사용자 매핑 전체 교체, 쓰기는 조회 포함 |
| `/auth/deleteRole.json` | POST | 로그인 + 관리자 세션 | `roleId` | `success:true` | `success:false`, `message`; `ROLE_ADMIN`·`ROLE_USER` 삭제 거부 |

- `/message/**`, `/sitemap`, `/board/insertComment.json`·`/board/deleteComment.json`·`/board/updateNotice.json`은 `AuthInterceptor`의 공개 경로가 아니므로 세션 `LOGIN_USER`가 필요하다. `/sitemap`은 추가 메뉴 권한 매핑 없이 `MenuInterceptor`가 만든 조회 권한 메뉴만 렌더한다.
- 댓글 삭제의 관리자 예외는 세션 `ADMIN_USER`로 판정한다. 쪽지는 관리자 예외 없이 발신자/수신자 본인만 열람·삭제한다. 공지 고정은 `ADMIN_USER`가 있어야 하며 CK 게시판만 허용한다.
- `/login/login.json` 성공 시 `LoginSessionRegistry`가 사용자별 최신 세션을 유지하고 같은 계정의 이전 세션을 `invalidate()`한다. 폼 로그인·OAuth 콜백·계정연계 모두 동일하며, 다중 인스턴스에서는 공유 세션 저장소가 필요하다.
- 로그인 사용자의 메뉴 조회·쓰기는 `MenuPermissionInterceptor`가 컨트롤러→메뉴 매핑으로 검사한다. 거부 시 `.json`은 HTTP 403 + `{"success":false,"message":"권한이 없습니다."}`, 화면은 403 오류 페이지다. `ADMIN_YN='Y'`는 슈퍼관리자로 통과하며, `/system`·`/auth`는 기존 `AuthInterceptor` 정책상 관리자만 접근한다.
- 댓글·쪽지·다운로드·SSE 구독은 메뉴 CRUD 권한과 분리해 로그인 사용자 공통 기능으로 둔다. SSE는 구독 시 역할을 저장하고, `BoardNotificationEventListener`가 발송 시 `selectRoleIdsWithMenuRead` 결과와 겹치는 역할에만 보낸다.
- `/login/findPw.json`·`/login/resetPw.json`은 성공·실패 모두 HTTP 200과 `success` 필드로 응답한다. `findPw`는 계정 불일치·SMTP 비활성·발송 실패에서도 동일한 성공 메시지를 반환하므로, 클라이언트는 메일 도착 여부를 응답만으로 판단하지 않는다.
- 메일 발송은 `arkive.mail.enabled=true`, 유효한 `arkive.mail.from`, SMTP 설정이 모두 있어야 한다. 기본값은 비활성이며, 비활성·수신 거부·이메일 없음이면 조용히 건너뛴다. 실제 SMTP 발송·반송·수신거부 계약은 미확인이다.
- 공지/중요글 메일은 `BOARD.MAIL_SENT_YN`을 먼저 `Y`로 선점한 뒤 발송한다. 선점 후 SMTP가 실패해도 API 응답은 성공이며 자동 재시도는 없다. 게시판 저장 API는 `BoardNotificationEvent`만 발행하고, SSE·메일 리스너가 각자 예외를 처리한다.

## 계약 기록 양식

| 경로/기능 | HTTP method | 인증/권한 | Request | Response/상태 코드 | 오류/재시도 | 호환성/버전 | 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | 미확인 |  |  |  |  |  |

API 계약을 수정할 때 Controller와 JavaScript/호출 시스템 사용처를 함께 확인한다. 코드 구현을 의도된 계약으로 간주하지 말고 업무/API 담당자 확인을 남긴다.
