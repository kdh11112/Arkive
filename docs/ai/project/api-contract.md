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
| `/system` | `admin.audit.web.AuditLogController` | `/auditLogList`, `/downloadAuditLogExcel.do` | 화면 GET+POST, 엑셀 GET |
| `/system`, `/file` | `admin.comm.web.FileController` | `/multipartUpload`, `/tusUpload`, `/tusPopup`, `/dropzoneUpload`, `/dropzonePopup`, `/tus`, `/tus/{fileId}`, `/file/tempFileUpload.json`, `/file/saveTempFiles.json`, `/file/setFileDelete.json`, `file/downloadFile.do` | 화면 GET, TUS에 OPTIONS/HEAD/PATCH/DELETE 있음 |
| `/login` | `admin.auth.web.LoginController` | `/login`, `/login.json`, `/signup`, `/signup.json`, `/findId`, `/findId.json`, `/findPw`, `/findPw.json`, `/resetPw`, `/resetPw.json`, `/logout`, `/apiDemo`, `/totpVerify.json`, `/totpSetup`, `/totpSetup.json`, `/totpConfirm.json`, `/totpDisable.json` | 화면 GET+POST, `.json` POST |
| `/auth` | `admin.auth.web.UserManageController` | `/userList`, `/userWrite`, `/insertUser.json`, `/updateUser.json`, `/deleteUser.json`, `/unlockUser.json`, `/resetPassword.json`, `/resetTotp.json` | 화면 GET+POST, `.json` POST |
| `/terms`, `/login/termsAgree` | `admin.terms.web.TermsController` | `/terms`(공개), `/login/termsAgree`, `/login/termsAgree.json` | 화면 GET, 동의 저장 POST |
| `/auth` | `admin.auth.web.RoleManageController` | `/roleList`, `/roleWrite`, `/saveRole.json`, `/deleteRole.json` | 화면 GET+POST, `.json` POST |
| `/login/oauth` | `admin.auth.web.OauthController` | `/{provider}`, `/{provider}Callback`, `/link.json` | 화면 GET+POST, 연계 POST |
| `/login/nice` | `admin.auth.web.NiceVerifyController` | `/start`, `/callback`, `/status.json`, `/clear.json` | GET+POST 혼합, 해제 POST |
| `/api` | `admin.auth.web.ApiController` | `/login.json`, `/noticeList.json`, `/noticeDetail.json`, `/faqList.json` | POST 또는 GET+POST |
| `/board` | `admin.board.web.BoardController` | `/ckeditorList`, `/toastList`, `/toastList.json`, `/ckeditorWrite`, `/toastWrite`, `/detail`, `/insertBoard.json`, `/updateBoard.json`, `/deleteBoard.json`, `/updateNotice.json`, `/insertComment.json`, `/deleteComment.json`, `/subscribe`, `/editorImageUpload.json` | 화면 GET+POST, `.json` POST, 구독 GET |
| `/sitemap` | `admin.comm.web.SitemapController` | `/sitemap` | 화면 GET, 로그인 사용자 권한별 메뉴 트리 |
| `/search` | `admin.search.web.SearchController`·`SearchReindexController` | `/search`, `/search/reindex.json` | 화면 GET(로그인 필요), 재색인 POST(관리자·ES 활성 시), 제목 LIKE+초성 통합검색 (CK·TOAST·FAQ·QNA) |
| `/message` | `admin.message.web.MessageController` | `/inbox`, `/sent`, `/write`, `/detail`, `/insertMessage.json`, `/deleteMessage.json` | 화면 GET+POST, `.json` POST |
| `/leave` | `admin.leave.web.LeaveController` | `/myLeave`, `/leaveList`, `/balanceList`, `/insertLeave.json`, `/cancelLeave.json`, `/approveLeave.json`, `/rejectLeave.json`, `/saveBalance.json` | 화면 GET+POST, `.json` POST |
| `/meeting` | `admin.meeting.web.MeetingController` | `/roomList`, `/roomWrite`, `/insertRoom.json`, `/updateRoom.json`, `/deleteRoom.json`, `/reserveList`, `/insertReservation.json`, `/cancelReservation.json` | 화면 GET+POST, `.json` POST |
| `/approval` | `admin.approval.web.ApprovalController` | `/approvalList`, `/approvalWrite`, `/approvalView`, `/insertDraft.json`, `/updateDraft.json`, `/deleteDraft.json`, `/submitApproval.json`, `/approveApproval.json`, `/rejectApproval.json`, `/cancelApproval.json` | 화면 GET+POST, `.json` POST |
| `/hr` | `admin.hr.web.HrController` | `/anniversaryList`, `/anniversaryWrite`, `/insertAnniversary.json`, `/updateAnniversary.json`, `/deleteAnniversary.json`, `/eventList`, `/eventWrite`, `/insertEvent.json`, `/updateEvent.json`, `/deleteEvent.json`, `/rewardList`, `/rewardWrite`, `/insertReward.json`, `/updateReward.json`, `/deleteReward.json`, `/personalLeave`, `/savePersonalLeave.json` | 화면 GET+POST, `.json` POST |
| `/survey` | `admin.survey.web.SurveyController` | `/templateList`, `/templateWrite`, `/insertTemplate.json`, `/updateTemplate.json`, `/deleteTemplate.json`, `/surveyList`, `/surveyWrite`, `/insertSurvey.json`, `/updateSurvey.json`, `/deleteSurvey.json`, `/openSurvey.json`, `/closeSurvey.json`, `/questionList`, `/questionWrite`, `/insertQuestion.json`, `/updateQuestion.json`, `/deleteQuestion.json`, `/participateList`, `/surveyTake`, `/submitSurvey.json`, `/stats` | 화면 GET+POST, `.json` POST |
| `/lecture` | `admin.lecture.web.LectureController` | `/lectureList`, `/lectureWrite`, `/myLecture`, `/view`, `/completionList`, `/insertLecture.json`, `/updateLecture.json`, `/deleteLecture.json`, `/openLecture.json`, `/closeLecture.json`, `/progress.json`, `/stream` | 화면 GET+POST, `.json` POST, 스트리밍 GET/HEAD |
| `/map` | `admin.map.web.MapController` | `/mapList`, `/mapWrite`, `/mapView`, `/insertMapGuide.json`, `/updateMapGuide.json`, `/deleteMapGuide.json` | 화면 GET+POST, `.json` POST |
| `/sso` | `admin.sso.web.SsoController` | `/login`, `/callback` | GET+POST, 외부 SSO 리다이렉트, 기본 비활성(404) |
| `/chat` | `admin.chat.web.ChatController`, `admin.chat.websocket.ChatWebSocketHandler` | `/chat/chat`, WebSocket `/ws/chat` | 화면 GET, WebSocket |
| `/batch` | `admin.batch.web.BatchController` | `/batchList`, `/runDailyStats.json` | 화면 GET+POST, 실행 POST |
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
| `/stats` | `admin.stats.web.StatsController` | `/statsList`, `/downloadStatsExcel.do` | 화면 GET+POST, 엑셀 GET |
| `/internal/v1` | `admin.api.web.InternalApiController` | `/health`, `/notices` | GET, `X-Internal-Token` 서비스 토큰(필터) |
| `/user` | `user.main.web.UserMainController` | `/userMain` | GET+POST |
| `/user/board` | `user.board.web.UserBoardController` | `/noticeList`, `/noticeDetail`, `/archiveList`, `/archiveDetail` | GET+POST |
| `/user/faq` | `user.faq.web.UserFaqController` | `/faqList` | GET+POST |
| `/user/qna` | `user.qna.web.UserQnaController` | `/qnaList`, `/qnaDetail`, `/qnaWrite`, `/insertQna.json` | 화면 GET+POST, 등록 POST |
| `/user/poll` | `user.poll.web.UserPollController` | `/pollList`, `/pollDetail`, `/votePoll.json` | 화면 GET+POST, 투표 POST |
| `/user/counsel` | `user.counsel.web.UserCounselController` | `/counselWrite`, `/counselFileUpload.json`, `/insertCounsel.json` | 화면 GET+POST, `.json` POST |
| `/` | `com.controller.ArkiveController` | `/`, `/dashboard` | 미확인 |
| 샘플 | `egovframework.example.sample.web.EgovSampleController` | `/egov`, `/egovSampleList.do`, `/addSample.do` 등 | GET/POST 선언됨. 샘플 기능으로 취급 |

## 추가 인벤토리

- 2026-10-11에 29개 Controller prefix를 대조했고, 2026-10-22에 `/message`를 추가해 30개가 되었다. 2026-10-23에 `/board/updateNotice.json`, `/auth/role*`, `/login/resetPw*`, `/sitemap` 경로를 추가해 31개가 되었고, `/search`를 추가해 32개가 되었다. 2026-10-24에 `/leave`를 추가해 33개가 되었다. 같은 날 3단계에서 `/meeting`, `/approval`, `/hr`, `/survey`, `/lecture`, `/map`, `/sso`, `/chat`(+`/ws/chat`), `/batch` 9개 prefix를 추가했고, 표에는 2단계의 `/system/auditLog*`, `/stats`, `/internal/v1`, `/terms`, `/login/totp*`, `/search/reindex.json`, `/auth/resetTotp.json`도 함께 반영했다. 선언 method는 각 Controller 코드에서 확인한다.
- 위 표는 코드 선언을 훑은 인벤토리이며 전체 계약 목록이 아니다. 각 prefix의 전체 경로·파라미터는 해당 Controller와 화면 JS를 대조해야 한다.
- `NiceCheckController`(`/niceExt`) 외부 계약은 연계 명세를 확인한다.

## 2026-10-22~24 코드에서 확인한 로그인·댓글·쪽지·공지 고정·메일·재설정·휴가 JSON 동작

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
| `/leave/insertLeave.json` | POST | 로그인 + double-submit token(`LEAVE`) | `leaveType`(`FULL`\|`HALF_AM`\|`HALF_PM`), `startDe`·`endDe`(`YYYY-MM-DD`), `reason`(1~500자), `egovframework.double.submit.preventer.parameter.name` | `success:true`, `leaveId` | `success:false`, `message` (유형·날짜 형식·같은 연도·반차 기간·중복 기간·잔여 초과·연차 정보 없음·토큰) |
| `/leave/cancelLeave.json` | POST | 로그인 + 신청 본인 | `leaveId` | `success:true`; 승인 건이면 사용일수 환원 | `success:false` (본인 아님, 대기/승인 상태 아님) |
| `/leave/approveLeave.json` | POST | 로그인 + 휴가관리 메뉴 WRITE(`ME02090200`) | `leaveId` | `success:true`; 대기 건을 승인하고 사용일수 차감 | `success:false` (대기 아님, 잔여 부족, 이미 처리됨) |
| `/leave/rejectLeave.json` | POST | 로그인 + 휴가관리 메뉴 WRITE | `leaveId`, `rejectReason`(1~500자) | `success:true`; 반려 사유 저장, 차감 없음 | `success:false` (사유 누락, 대기 아님) |
| `/leave/saveBalance.json` | POST | 로그인 + 연차관리 메뉴 WRITE(`ME02090300`) | `userId`, `leaveYear`(4자리), `totalDays`(0~999.9, 소수 1자리) | `success:true`; 없으면 등록, 있으면 갱신 | `success:false` (사용자 없음·사용일수보다 적게 부여·형식 오류) |

- `/message/**`, `/sitemap`, `/board/insertComment.json`·`/board/deleteComment.json`·`/board/updateNotice.json`은 `AuthInterceptor`의 공개 경로가 아니므로 세션 `LOGIN_USER`가 필요하다. `/sitemap`은 추가 메뉴 권한 매핑 없이 `MenuInterceptor`가 만든 조회 권한 메뉴만 렌더한다.
- 댓글 삭제의 관리자 예외는 세션 `ADMIN_USER`로 판정한다. 쪽지는 관리자 예외 없이 발신자/수신자 본인만 열람·삭제한다. 공지 고정은 `ADMIN_USER`가 있어야 하며 CK 게시판만 허용한다.
- `/login/login.json` 성공 시 `LoginSessionRegistry`가 사용자별 최신 세션을 유지하고 같은 계정의 이전 세션을 `invalidate()`한다. 폼 로그인·OAuth 콜백·계정연계 모두 동일하며, 다중 인스턴스에서는 공유 세션 저장소가 필요하다.
- 로그인 사용자의 메뉴 조회·쓰기는 `MenuPermissionInterceptor`가 컨트롤러→메뉴 매핑으로 검사한다. 거부 시 `.json`은 HTTP 403 + `{"success":false,"message":"권한이 없습니다."}`, 화면은 403 오류 페이지다. `ADMIN_YN='Y'`는 슈퍼관리자로 통과하며, `/system`·`/auth`는 기존 `AuthInterceptor` 정책상 관리자만 접근한다.
- 매핑되지 않은 URL과 존재하지 않는 정적 리소스는 HTTP 404 + `error/error` 화면으로 응답한다. 미매핑 `.json` 경로도 현재 HTML 404를 반환하며, JSON 오류 본문 규약은 아직 확정되지 않았다.
- 댓글·쪽지·다운로드·SSE 구독은 메뉴 CRUD 권한과 분리해 로그인 사용자 공통 기능으로 둔다. SSE는 구독 시 역할을 저장하고, `BoardNotificationEventListener`가 발송 시 `selectRoleIdsWithMenuRead` 결과와 겹치는 역할에만 보낸다.
- `/login/findPw.json`·`/login/resetPw.json`은 성공·실패 모두 HTTP 200과 `success` 필드로 응답한다. `findPw`는 계정 불일치·SMTP 비활성·발송 실패에서도 동일한 성공 메시지를 반환하므로, 클라이언트는 메일 도착 여부를 응답만으로 판단하지 않는다.
- 메일 발송은 `arkive.mail.enabled=true`, 유효한 `arkive.mail.from`, SMTP 설정이 모두 있어야 한다. 기본값은 비활성이며, 비활성·수신 거부·이메일 없음이면 조용히 건너뛴다. 실제 SMTP 발송·반송·수신거부 계약은 미확인이다.
- 공지/중요글 메일은 `BOARD.MAIL_SENT_YN`을 먼저 `Y`로 선점한 뒤 발송한다. 선점 후 SMTP가 실패해도 API 응답은 성공이며 자동 재시도는 없다. 게시판 저장 API는 `BoardNotificationEvent`만 발행하고, SSE·메일 리스너가 각자 예외를 처리한다.
- `/leave/myLeave`는 로그인 사용자의 본인 연차·신청만 조회하고 취소도 본인만 가능하다. 휴가관리(`ME02090200`)와 연차관리(`ME02090300`)는 `MenuPermissionInterceptor.resolveLeave`가 경로로 메뉴를 판정하며 WRITE 권한이 필요하다. 관리자는 `ADMIN_YN`으로 우회한다.
- 휴가 일수는 주말과 `HOLIDAY`를 제외한 평일 기준이고 반차는 0.5일이다. 신청 기간은 같은 연도만 허용하며, 신청중(`REQUESTED`) 일수도 잔여에서 차감한다. 승인은 `USED_DAYS + DAYS <= TOTAL_DAYS` 조건부 UPDATE로 차감하고, 승인 취소는 사용일수를 환원한다.
- `/leave` 화면·JSON은 성공·실패 모두 HTTP 200과 `success` 필드로 응답하는 기존 패턴을 따른다. 권한 없는 메뉴 접근은 `MenuPermissionInterceptor`가 403(JSON은 `{"success":false,"message":"권한이 없습니다."}`)으로 거부한다.

## 2026-10-24 3단계 코드 관찰 (미검증)

> 3단계(회의실·약식결재·HR·설문·강의·약도·SSO·채팅·배치) 구현 코드를 읽고 정리한 관찰값이다. 2026-10-24 `verify.cmd -o`로 전체 193개 테스트가 통과했으나 실제 기동 HTTP 계약 확인은 하지 않았으므로 승인된 계약이 아니며, 아래 동작은 실제 기동 전까지 `[미검증]`이다. 성공·실패 모두 Controller가 HTTP 상태를 명시하지 않는 한 200과 `success` 필드로 응답하고, 인증·메뉴 권한 거부는 `AuthInterceptor`/`MenuPermissionInterceptor`가 401·403으로 끊는 기존 패턴을 따른다.

| 경로 | method | 인증/권한 (코드 기준) | Request | Response | 오류 |
| --- | --- | --- | --- | --- | --- |
| `/meeting/insertRoom.json` | POST | 로그인 + 회의실관리 WRITE(`ME02050100`) | double-submit token(`MEETING_ROOM`), `roomNm`, `location`, `capacity`, `useYn`, `remark` | `success:true`, `roomId` | 토큰·검증 오류, `success:false`+`message` |
| `/meeting/insertReservation.json` | POST | 로그인 + 회의실예약 WRITE(`ME02050200`) | token(`MEETING_RESERVE`), `roomId`, `reserveDe`(`YYYY-MM-DD`), `startTime`·`endTime`(`HH:mm`), `title`, `purpose` | `success:true`, `reserveId` | 시간대 겹침·비활성 회의실·형식 오류 |
| `/meeting/cancelReservation.json` | POST | 로그인 + 예약자 본인/관리자 (+예약 메뉴 WRITE) | `reserveId` | `success:true` | 본인 아님·없는 예약 |
| `/approval/approvalList` | GET+POST | 로그인, 문서 범위는 `scope`(`my`/`todo`/`all`)로 제한. `all`은 관리자만 | `scope`, `status`, `searchKeyword` | 목록 화면. 결재자·기안자·관리자만 상세 접근 | 비로그인은 로그인 화면으로 리다이렉트 |
| `/approval/approvalView` | GET+POST | 로그인 + 기안자/결재자/관리자 | `docId` | 문서·이력 화면 | 없음 404, 권한 없음 403 |
| `/approval/insertDraft.json` | POST | 로그인 + 약식결재 WRITE(`ME02070100`) | token(`APPROVAL`), `docType`, `title`, `content`, `approverId` | `success:true`, `docId` (임시저장 `DRAFT`) | 토큰·검증 오류 |
| `/approval/submitApproval.json` | POST | 로그인 + 기안자 | `docId` | `success:true` (DRAFT→SUBMITTED) | 기안자 아님·상태 오류 |
| `/approval/approveApproval.json` | POST | 로그인 + 지정 결재자 | `docId` | `success:true` (SUBMITTED→APPROVED) | 결재자 아님·상태 오류 |
| `/approval/rejectApproval.json` | POST | 로그인 + 지정 결재자 | `docId`, `rejectReason` | `success:true` (SUBMITTED→REJECTED) | 결재자 아님·사유 누락 |
| `/approval/cancelApproval.json` | POST | 로그인 + 기안자 | `docId` | `success:true` (SUBMITTED→CANCELLED) | 기안자 아님·상태 오류 |
| `/hr/insert{Anniversary,Event,Reward}.json`·`update…`·`delete…` | POST | 로그인 + 해당 HR 메뉴 WRITE(`ME02090400`~`ME02090600`) | 등록은 token(`HR_ANNIV`/`HR_EVENT`/`HR_REWARD`)과 사용자·유형·일자·제목·금액·비고 | `success:true`, `id` | 토큰·사용자·형식 오류 |
| `/hr/savePersonalLeave.json` | POST | 로그인 + 개인연차 메뉴 WRITE(`ME02090700`) | `userId`, `leaveYear`, `totalDays` | `success:true`; 기존 `LeaveService.saveBalance`로 연차 부여·갱신 | 사용자 없음·숫자/범위 오류 |
| `/survey/insertTemplate.json` 등 관리 JSON | POST | 로그인 + 설문템플릿/설문관리 WRITE(`ME02031600`/`ME02031700`) | 등록 token(`SURVEY_TEMPLATE`/`SURVEY`/`SURVEY_QUESTION`), 설문·질문·보기 입력 | `success:true`, `id` | 토큰·검증 오류 |
| `/survey/submitSurvey.json` | POST | 로그인 + 설문참여 WRITE(`ME02031800`) | token(`SURVEY_TAKE`), `surveyId`, `answers_<questionId>`(복수 선택은 다중 값) | `success:true`; 참여 1회 제한(중복 참여 거부) | 필수 질문 누락·기간 아님·중복 참여·토큰 |
| `/survey/stats` | GET+POST | 로그인 + 설문통계 READ(`ME02031900`) | `surveyId` | 질문·보기별 집계와 주관식 응답 | 설문 없음 |
| `/lecture/insertLecture.json`·`updateLecture.json` | POST | 로그인 + 강의관리 WRITE(`ME020A0100`) | multipart `video`(선택), `title`, `description`, `surveyId`, `durationSec`, `completeRate`, `useYn`; 등록 시 token(`LECTURE`) | `success:true`, `id` | 파일·인코딩·검증 오류 |
| `/lecture/progress.json` | POST | 로그인 + 내 강의 WRITE(`ME020A0200`) | `lectureId`, `watchedSec` | `success:true`, 진도·수료 상태 | 공개 아님·진도 오류 |
| `/lecture/stream` | GET/HEAD | 로그인(공개 강의는 로그인 사용자), 관리자는 상태 무관 | `lectureId`, `Range` 헤더 | 200/206 영상 스트림, `Accept-Ranges` | 없음 404, 범위 불가 416 |
| `/map/insertMapGuide.json` 등 | POST | 로그인 + 약도관리 WRITE(`ME02040200`) | 등록 token(`MAP_GUIDE`), `title`, `address`, `latitude`, `longitude`, `description`, `useYn` | `success:true`, `id` | 토큰·좌표 형식 오류 |
| `/sso/login` | GET+POST | 공개(`/sso/`), `arkive.sso.enabled=true`와 필수 설정이 있어야 동작 | 없음 | 외부 authorize URL로 리다이렉트(state 세션 저장) | 비활성·설정 누락 시 404 |
| `/sso/callback` | GET+POST | 공개(`/sso/`), `state` 일치 필요 | `state`, `code` | 연계 계정(`OAUTH_LINK` PROVIDER=`sso`) 로그인 후 `/dashboard` 또는 `/user/userMain`; TOTP 사용자는 TOTP 단계로 | state 불일치·미연계 계정·토큰 교환 실패는 `sso/ssoError` 화면 |
| `/ws/chat` | WebSocket | 세션 로그인 + 채팅 메뉴 READ·WRITE(`ME020B0100`, 관리자 우회), Origin 검사 | 텍스트 메시지(최대 16KB) | 최근 이력·브로드캐스트, 30분 유휴 타임아웃 | 핸드셰이크 거부, 단일 인스턴스 in-memory |
| `/batch/runDailyStats.json` | POST | 로그인 + 배치관리 WRITE(`ME02010800`) | token(`BATCH_STATS`), `statDate`(`YYYY-MM-DD`, 미래 불가) | `success:true`, `status`, `targetDate`; 같은 날짜 재실행은 기존 집계 삭제 후 재삽입 | 형식·미래 날짜·토큰·Job 실패 |

- 3단계 메뉴 시드 기준으로 일반 역할(`ROLE_USER`)은 회의실예약·약식결재·설문참여·내 강의·채팅만 접근할 수 있고, 회의실관리·HR·설문관리·설문통계·강의관리·수료현황·약도관리·배치관리는 관리자 전용이다. 실제 부여는 `APP_ROLE_MENU` 데이터에 따르며 위 시드는 샘플이다.
- SSO는 기본 비활성(`SSO_ENABLED=false`)이고 자동 가입도 기본 비활성(`SSO_AUTO_CREATE=false`)이다. 외부 SSO의 토큰·사용자 정보 계약과 글로벌 로그아웃은 `미확인`이다.
- 채팅은 DB에 메시지를 남기지만 브로드캐스트는 JVM 메모리 기반이라 다중 인스턴스에서 공유되지 않는다. Sticky session 또는 외부 브로커가 필요하다.
- 동영상 인코딩은 기본 비활성(`FFMPEG_ENABLED=false`)이며 업로드 파일을 그대로 스트리밍한다. FFmpeg 경로·코덱·용량 정책은 대상 환경에서 확인해야 한다.
- 약도는 `Globals.kakao.mapKey`(=`KAKAO_MAP_KEY`)가 비어 있으면 지도 스크립트를 로드하지 않고 주소·좌표만 표시한다.
- 배치는 스케줄러 없이 화면에서 수동 실행만 지원하고, `ResourcelessJobRepository`라 `BATCH_*` 실행 이력을 남기지 않는다. 운영 스케줄·재시도·이력 보존은 `미확인`이다.

## 2단계 코드 관찰 (2026-10-24, 미검증)

> 2단계(감사 로그·마스킹·TOTP·통계·검색·SMS·내부 API·약관·필드 암호화) 구현 코드를 읽고 정리한 관찰값이다. 2026-10-24 `verify.cmd -o`로 전체 193개 테스트가 통과했으나 실제 기동 HTTP 계약 확인은 하지 않았으므로 승인된 계약이 아니며 `[미검증]`이다. 공통 응답 패턴은 기존과 같이 성공·실패 모두 HTTP 200과 `success` 필드이고, 인증·권한 거부는 `AuthInterceptor`/`MenuPermissionInterceptor`가 401·403으로 끊는다.

| 경로 | method | 인증/권한 (코드 기준) | Request | Response | 오류 |
| --- | --- | --- | --- | --- | --- |
| `/search` | GET | 로그인 세션 | `searchKeyword`, `pageIndex` | 검색 화면. CK·TOAST·FAQ·QNA 제목 LIKE + `SEARCH_CHOSUNG` 초성, 본문은 태그 제거 요약 | 빈 검색어·무결과 안내. 검색어 길이 제한 없음 |
| `/search/reindex.json` | POST | 관리자 세션(`ADMIN_USER`) | 없음 | `success:true`, `count`; ES 전체 재색인 | 관리자 아님, ES 비활성(`arkive.search.elasticsearch.enabled=false`), 재색인 실패 |
| `/system/auditLogList` | GET+POST | 로그인 + 관리자(`/system` 정책) | `startDate`, `endDate`, `eventType`, `searchKeyword`, `pageIndex` | 감사 로그 목록·기간/이벤트/키워드 필터 | 관리자 아님 401·403 |
| `/system/downloadAuditLogExcel.do` | GET | 로그인 + 관리자 | 위와 같은 필터, 최대 5000건 | XLS 다운로드, 사용자명 마스킹 | 관리자 아님 |
| `/stats/statsList` | GET+POST | 로그인 + 통계 메뉴 READ(`ME02010700`, 관리자 우회) | `startDate`, `endDate` | 게시판 건수·감사 이벤트·일별 로그인/접속·다운로드 집계 화면 | 메뉴 권한 없음 403 |
| `/stats/downloadStatsExcel.do` | GET | 로그인 + 통계 메뉴 READ | `startDate`, `endDate` | XLS 다운로드 | 메뉴 권한 없음 403 |
| `/login/totpVerify.json` | POST | 로그인 전 TOTP 대기 세션(`PENDING`) | `code` 또는 `recoveryCode` | `success:true`, `isAdmin`, `userNm`, `termsRequired`; 세션 완성 | 대기 정보 없음, 코드 불일치(5회 시 대기 폐기) |
| `/login/totpSetup.json` | POST | 로그인 세션 | 없음 | `success:true`, 비밀키·QR 정보(키는 AES-GCM 저장) | 필드 키 미설정·이미 설정됨 |
| `/login/totpConfirm.json` | POST | 로그인 세션 | `code` | `success:true`, 복구코드 1회 발급(평문은 응답에서만) | 코드 불일치·설정 대기 없음 |
| `/login/totpDisable.json` | POST | 로그인 세션 | `code`(인증 앱 코드 또는 복구코드) | `success:true`, TOTP 해제 | 코드 불일치 |
| `/auth/resetTotp.json` | POST | 로그인 + 관리자(`/auth` 정책) | `userId` | `success:true`, 해당 사용자 TOTP·복구코드 초기화 | 사용자 없음·관리자 아님 |
| `/terms` | GET | 공개 | 없음 | 현재 사용 중인 버전별 약관 목록 | 없음 |
| `/login/termsAgree` | GET | 로그인 세션 | 없음 | 미동의 필수 약관 동의 화면. 대상 없으면 `/`로 리다이렉트 | 비로그인 시 로그인 화면 |
| `/login/termsAgree.json` | POST | 로그인 세션 | `termsIds[]` | `success:true`, 동의 이력 저장·`PENDING_TERMS` 해제 | 필수 약관 누락 |
| `/internal/v1/health` | GET | `X-Internal-Token` 서비스 토큰 | 없음 | `success:true`, `apiVersion:"v1"`, `status:"UP"`, `timestamp` | 토큰 미설정 503, 불일치 401 |
| `/internal/v1/notices` | GET | `X-Internal-Token` 서비스 토큰 | 없음 | `success:true`, `apiVersion:"v1"`, CK 공지 최신 10건 `list`, `total` | 토큰 오류, 조회 실패 `success:false` |

- 감사 이벤트는 `LOGIN_SUCCESS`, `LOGIN_FAIL`, `LOGOUT`, `API_LOGIN`, `API_LOGIN_FAIL`, `TOTP_CHALLENGE`, `TOTP_FAIL`, `TERMS_CONSENT`, `MENU_ACCESS`, `PII_VIEW`, `PII_DOWNLOAD`, `API_ACCESS`를 기록·필터링한다.
- 사용자 휴대전화는 등록·수정 시 `AuthServiceImpl`이 AES-GCM으로 암호화하고 상세 조회 시 복호화한다. 목록에는 `PHONE`을 싣지 않으며, SMS 발송 대상은 `SmsServiceImpl`이 발송 직전에 복호화한다. `APP_USER.EMAIL`은 아이디 찾기·비밀번호 재설정의 `WHERE` 조건이라 평문이다.
- `ARKIVE_FIELD_KEY`가 없으면 TOTP 설정과 휴대전화·연락처 저장이 거부된다. 기존 평문 연락처는 복호화 없이 그대로 읽히며, 자동 암호화 이관은 없다.
- SMS는 별도 API 없이 `BoardNotificationEvent`의 채널로만 동작한다. `arkive.sms.enabled=false` 또는 자격증명·발신번호 누락 시 조용히 건너뛰고, 실제 알리고 계약·실패 재시도는 `미확인`이다.
- 외부 웹분석은 API가 아니라 `AnalyticsModelAdvice`가 화면 모델에 설정을 주입하고 `arkive-analytics.js`가 동의 후 GA4·Clarity를 로드하는 방식이다. `ARKIVE_GA4_ID`·`ARKIVE_CLARITY_ID`가 없으면 비활성이다.

## 계약 기록 양식

| 경로/기능 | HTTP method | 인증/권한 | Request | Response/상태 코드 | 오류/재시도 | 호환성/버전 | 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | 미확인 |  |  |  |  |  |

API 계약을 수정할 때 Controller와 JavaScript/호출 시스템 사용처를 함께 확인한다. 코드 구현을 의도된 계약으로 간주하지 말고 업무/API 담당자 확인을 남긴다.
