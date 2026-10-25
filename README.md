# Arkive

> Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
> 새 기능은 Arkive에 축적하고, 타 전자정부프레임워크·Spring·Spring Boot 프로젝트에서 복붙으로 가져간다.
> 프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 전제로 한다.

> 이 문서는 Arkive 소개·실행·AI 이식 요청·기능 로드맵이다. **이식 절차는 [PORTING.md](PORTING.md)**, 전체 문서 체계는 [docs/ai/README.md](docs/ai/README.md)로 분리한다.

## 실행

```cmd
mvn spring-boot:run
```

- 기본 주소: `http://localhost:8080` (앱의 HSQLDB 서버는 9001)
- 테스트 DB는 `target/test-db`, 포트 9002를 쓴다. 앱의 HSQLDB(9001)와 분리되어 앱 실행 중에도 `verify.cmd`를 돌릴 수 있다.
- 메일 발송은 기본 비활성(`MAIL_ENABLED=false`)이다. SMTP·발신자 설정 키는 [project-overview.md](docs/ai/project/project-overview.md)를 본다.

## 이식과 문서

- **이식 실행**: [PORTING.md](PORTING.md) — 복사 매트릭스, 5분 복사, 기능별 순서
- **기능별 실행·복사 시트**: [feature-porting-sheet.md](docs/ai/portable/feature-porting-sheet.md)
- **테스트 이식 시트**: [test-porting-sheet.md](docs/ai/portable/test-porting-sheet.md)
- **AI 이식 요청 방법**: [ai-porting-request.md](docs/ai/portable/ai-porting-request.md)
- **전체 문서 체계**: [docs/ai/README.md](docs/ai/README.md) — common/portable/Boot/Classic/Arkive 기록 구분
- **공통 방법**: [docs/ai/common/](docs/ai/common/)
- **이식 양식**: [docs/ai/portable/](docs/ai/portable/)
- **Arkive 기록 (복사 금지)**: [docs/ai/project/](docs/ai/project/)
- AI 작업 지침: [AGENTS.md](AGENTS.md)

핵심 원칙: `docs/ai/project/`는 Arkive의 현재 지식 기록이므로 대상 프로젝트로 복사하지 않고,
대상의 프로젝트 지식은 [project-template-pack.md](docs/ai/portable/project-template-pack.md)로 새로 채운다.

## AI 이식 요청 (복사용)

아래 프롬프트를 복사해서 `<기능명>`과 경로만 바꾼다. 전체를 한 번에 요청하지 말고 **기능 하나씩** 요청한다.
자세한 절차와 체크리스트는 [ai-porting-request.md](docs/ai/portable/ai-porting-request.md)에 있다.

```text
Arkive 레퍼런스: C:\Users\kdh\Documents\workspace-egov\Arkive
대상 프로젝트: 현재 작업 디렉터리

이번 이식 기능: <기능명>
범위: 화면 + Controller + Service + Mapper/XML + SQL + 테스트

1. Arkive의 PORTING.md, feature-porting-sheet.md, test-porting-sheet.md, ai-porting-request.md를 먼저 읽어라.
2. Arkive를 실행해 기능 동작을 확인하고, 대상 프로젝트와 다른 점을 표로 정리해라.
3. 대상의 패키지·테이블·DB·URL·세션 방식에 맞게 화면·Controller·Service·Mapper/XML·SQL을 옮겨라.
4. 테스트도 함께 옮기고, 대상 기준으로 URL·메시지·세션·seed·스키마를 수정해라.
5. 확인되지 않은 업무 규칙·권한·DB 의미는 추측하지 말고 "미확인"으로 남기고 질문해라.
6. 단독 테스트와 verify.cmd를 실행하고, 수정 파일·검증 결과·남은 위험을 보고해라.
```

- Arkive 경로가 다르면 첫 줄만 수정한다.
- AI가 Arkive를 읽을 수 없으면 [PORTING.md](PORTING.md)의 `1. 5분 복사`를 먼저 실행한다.
- `docs/ai/project/`는 Arkive 기록이므로 대상 프로젝트로 복사하지 않는다.

## 만들어야 하는 기능 (로드맵)

설명 + 위험·주의점 함께 적는다. 단계 내 위→아래 순이 권장 순서다.

검증 순서: `[미검증]` 구현은 됐지만 이번 검증에서 실행 확인하지 않음 → `[일부검증]` 화면/목록 응답만 확인했거나 기능 일부만 테스트함 → `[테스트완료]` 자동 테스트로 핵심 동작을 확인함 → `[GUI확인]` 실제 브라우저에서 화면·동작까지 확인함(최상위 단계). GUI 확인은 자동 테스트를 대체하지 않으며, `[GUI확인]`이 없으면 실제 브라우저 확인은 하지 않은 것이다. 공통적으로 등록·수정·삭제 쓰기 흐름을 기능별로 수동 확인한 것은 아니다.

### 완료

- [x] 게시판 (공지사항관리·자료실관리. CK·토스트 에디터, HWP 복붙, 에디터 이미지 업로드) — `BoardControllerTest`로 CK/TOAST 저장·본문 sanitize·이중등록 차단·조회수 1회 증가·에디터 이미지·첨부 삭제를, `BoardPagingControllerTest`로 오프셋/커서 페이징을, `BoardServiceImplTest`로 CLOB 변환·초성·색인 이벤트를 확인했다. HWP 복붙은 브라우저 동작이라 미확인 `[테스트완료]`
- [x] 페이징 (공지사항: 전자정부 오프셋, 자료실: 커서+더보기) `[테스트완료]`
- [x] 이전글/다음글 `[테스트완료]`
- [x] 공통코드 관리 — `CommCodeControllerTest`로 마스터·상세 관리, 하위 코드 연쇄 삭제, 필수값 검증을 확인했다 `[테스트완료]`
- [x] 파일 업로드 (멀티파트·TUS·드롭존, temp→확정 2단계) — `FileControllerTest`로 temp 업로드→확정(MP_ 물리 이동·DB 기록)→다운로드→삭제, 허용 확장자·빈 업로드 거부, TUS 생성·HEAD 오프셋·409 재개·PATCH·삭제 전체 흐름, 묶음 ZIP 중복 이름 처리, 경로 이탈 차단, 세션 없는 업로드 401을 확인했다. TUS CORS 프리플라이트(OPTIONS)는 세션 없이 허용하고 실제 생성·청크·삭제 요청은 관리자 세션을 그대로 요구한다 `[테스트완료]`
- [x] 글쓰기 본문 sanitize (Lucy core 직접 사용. 서블릿 필터는 jakarta 미지원이라 제외) — 위험 해소됨. script·비디오외 iframe·이벤트 속성 제거, 표·이미지·style 유지 `[테스트완료]`
- [x] 실시간 알림 SSE (새 글 등록 시 목록 배너. SseEmitter, 별도 의존성 없음, ALL 단일 연결, 역할 필터 포함) — 현재 `layout.html`의 `ARKIVE_SSE_ENABLED=false`로 기본 비활성. 켜려면 `true`로 바꾸고 화면별 설정·연결 상태를 확인. 위험: 통로 2개면 브라우저 연결 상한 점유로 메뉴 이동 정체. 역할 필터는 구독 시 세션 역할을 저장하고, 발송 시 해당 메뉴 조회 권한이 있는 역할만 대상으로 한다. 대상 역할이 비면 발송하지 않는다. `BoardSseServiceTest`로 게시판 유형·ALL 채널 전달, 유형 간 비전달, 대상 역할 없음·역할 없는 구독자 미전송을, `BoardNotificationEventListenerTest`로 채널 분리 발송을 확인했다. 실제 브라우저 EventSource 연결은 미확인 `[테스트완료]`
- [x] Swagger (springdoc API 문서: /swagger-ui.html) — `OpenApiDocsTest`로 `/v3/api-docs`의 제목·버전과 게시판·파일·시스템 API 경로 노출, `/swagger-ui/index.html` 응답을 확인했다. 위험: 운영 공개 시 민감 API 노출. 내부망 제한 또는 비활성화 검토 `[테스트완료]`

### 1단계 — 로그인 없이 가능 (쉬운 순)

- [x] 이중등록방지 Thymeleaf 적용 (helper setToken 완성 + cmm/doubleSubmit fragment, 게시판·FAQ·Q&A·메모·팝업·배너·일정·주소록·POLL·당직 적용) — `BoardControllerTest`·`UserQnaControllerTest`로 토큰 발급·정상 등록·같은 토큰 재등록 거부를, FAQ·메모·팝업·배너·일정·주소록·명함·POLL·당직·지도 테스트로 폼별 토큰 검증을 확인했다. 위험: 등록 폼마다 토큰 누락 금지, 여러 화면 동시 사용 시 tokenKey 분리 `[테스트완료]`
- [x] FAQ관리 (BOARD+BOARD_TYPE='FAQ' 재사용, 아코디언 목록, 이중등록방지 BOARD_FAQ) — `FaqControllerTest`로 초성·sanitize 저장 왕복, 이중등록·제목 누락 거부를 확인했다. 위험: Q&A와 게시판 패턴 공유, 첨부파일 최대 3개 설정 확인 `[테스트완료]`
- [x] Q&A관리 (BOARD+BOARD_TYPE='QNA' + QNA_ANSWER 1:1, 상태 LEFT JOIN 판정, 질문/답변 분리) — `QnaControllerTest`로 관리자 답변 저장·권한 차단·답변 폼 노출을, `UserQnaControllerTest`로 익명 등록의 PUBLIC 강제·sanitize·이중등록·내부글 로그인 제한·상세 CLOB 변환을, `QnaMapperTest`로 조회 조건을 확인했다. 위험: 처리상태 공통코드 COM028 선행, 일반 등록 + 관리자 답변 분리 `[테스트완료]`
- [x] 메모보고 (MEMO_REPORT, 보고→확인/의견 전이 SQL 강제) — `MemoControllerTest`로 상태 전이 제한, 이중등록·필수값 검증을 확인했다. 위험: 보고자-대상자 상태 전이(확인/의견) 누락 금지 `[테스트완료]`
- [x] 팝업창관리 (POPUP + 게시중 조회, 이미지 실형식 검증) — `PopupControllerTest`로 게시기간·사용여부 조건과 날짜 역전·이중등록 거부를 확인했다. 위험: 게시기간·노출위치 조건 누락 금지, 대시보드 진입 시 조회 연계 필요 `[테스트완료]`
- [x] 배너관리 (BANNER + POSITION 구분, 이미지-링크 매핑) — `BannerControllerTest`로 노출위치 정규화·정렬, 게시중 필터(위치·사용여부·기간)를 확인했다. 위험: 이미지-링크 매핑 누락 금지, 파일업로드 확정본과 연결 `[테스트완료]`
- [x] 메인이미지관리 (BANNER POSITION='MAIN' 통합, 별도 테이블 없음) — `BannerControllerTest`로 MAIN 위치 필터와 게시중 조건을 확인했다. 위험: 배너와 노출위치 구분, 이미지 규격 통일 `[테스트완료]`
- [x] 공지 고정 (BOARD.NOTICE_YN, CK 공지 목록 상단 정렬, 관리자 고정/해제, 관리·사용자 화면 배지) — 위험: TOAST는 커서 정렬이라 제외, 고정 글의 번호는 등록 순서 그대로라 화면에서 '공지' 배지로 구분해야 함 `[테스트완료]`
- [x] 부서일정관리 (SCHEDULE SCHEDULE_TYPE='DEPT', 월/주/일 단일쿼리) — `ScheduleControllerTest`로 유형·일자 필터 왕복, 일시 검증·유형 정규화, 공휴일 병합·삭제를 확인했다. 위험: 월/주/일 3개 목록 쿼리 통일, 부서·사용자 팝업 연결 필요 `[테스트완료]`
- [x] 일정관리(개인)·일지관리·메모할일 (SCHEDULE_TYPE PERSONAL/DAILY/TODO, 부서일정과 동일 테이블·쿼리) — `ScheduleControllerTest`로 유형별 저장·조회와 공휴일 연계를 확인했다. 위험: 부서일정과 테이블·쿼리 통일, 월/주/일 목록 패턴 재사용 `[테스트완료]`
- [x] 공휴일관리(달력) (HOLIDAY 직접 관리, 달력에 표시) — 위험: 당직·부서일정·휴가 공통 달력, HSQLDB 날짜함수 확인 `[GUI확인]`
- [x] 카테고리 설정(jstree형) (CATEGORY 자기참조, 네이티브 트리 + jstree규격 tree.json) — id·상위id·명·순서 구조, N단 확장. 메뉴관리 패턴 재사용. 강의·콘텐츠·설문 분류용 `[GUI확인]`
- [x] 카테고리 설정(컬럼 드릴다운형) (동일 CATEGORY 공유, 경로+하위 목록) — BLMS식 grade 컬럼 UI + GIES식 계층코드·JSON 일괄저장. N단 확장, 3단 고정 아님. jstree형과 동일 테이블 공유, 화면만 2종. 위험: 순환참조 방지, 삭제 시 하위 포함 여부·사용중 콘텐츠 체크 정책 선행 `[GUI확인]`
- [x] 주소록관리 (ADDRESS_BOOK, 이름·부서 검색) — `AddressControllerTest`로 연락처 암호화 저장·목록 마스킹·상세 평문 복원, 이중등록·이름 필수 검증을 확인했다. 위험: 부서일정 팝업(부서·사용자 선택)과 연결되는 기반 `[테스트완료]`
- [x] 명함관리 (BUSINESS_CARD + CARD_USER, 공개여부·사용등록) — `CardControllerTest`로 공개/내 명함 분리, 사용등록 추적, 이중등록·필수값 검증을 확인했다. 위험: 주소록과 세트, 공개·사용등록 흐름 유지 `[테스트완료]`
- [x] 상담관리 (COUNSEL, COM028 접수대기/접수/완료, 비밀번호 확인, 첨부 3개, 답변목록 분리) — `CounselControllerTest`로 비밀번호 확인·답변 관리, 등록·첨부 필수값 검증을 확인했다. 위험: Q&A와 구분(진행상태·비밀번호·첨부), 일반 등록 + 관리자 답변 분리 `[테스트완료]`
- [x] 온라인POLL (POLL + POLL_ITEM, 세션 중복투표 차단) — `PollControllerTest`로 세션당 1회 투표, 수정 시 항목 초기화, 항목·투표기간 검증을 확인했다. 위험: 3단계 설문관리는 별도 `SURVEY_*` 테이블을 쓰며 POLL과 문항 스키마 통합은 미적용 `[테스트완료]`
- [x] 당직관리(단독) (DUTY + 엑셀 일괄등록) — `DutyControllerTest`로 당직 저장·조회, 이중등록·일자 검증, 엑셀 양식 다운로드·업로드의 헤더/행 오류, 다운로드 마스킹을 확인했다. 위험: 승인연동(약식결재)은 제외하고 달력+일지까지만, 엑셀 일괄등록 형식 고정 `[테스트완료]`
- [ ] 목록 썸네일 + 상세 지도 표시 패턴 (보류: 카카오 appkey 환경 분리·JSP 하드코딩 정리 필요. CommUtil.createThumbnail 썸네일 유틸은 있음)
- [x] 엑셀 업로드/다운로드 + 양식 일괄등록 (당직에 적용, ExcelUtil.createWorkBook + POI 판독) — 목록 엑셀 다운로드, 샘플양식 다운로드→업로드→일괄등록. 당직·명단 등록에 재사용. 코드에 ExcelUtil 있음. `DutyControllerTest`로 양식·업로드 오류 처리를, `StatsControllerTest`·`AuditLogControllerTest`로 목록 엑셀 다운로드를 확인했다. 위험: 대용량 업로드 메모리 주의(1000행 제한), 양식 버전 고정 `[테스트완료]`
- [x] 파일 미리보기(뷰어) (preview/fileView, 이미지·PDF inline, fileId만 사용) — 문서·이미지 미리보기. 업로드 다음 단계. 위험: 원본 다운로드 경로 노출 금지, 미리보기용 변환본 분리 `[GUI확인]`
- [x] 파일 실형식 검증 (FileTypeUtil 매직바이트 검사, 팝업·배너 업로드 적용) — 확장자가 아닌 매직바이트로 실제 MIME 판별. text를 .png로 바꿔도 차단. 위험: 풀파서(tika-parsers) 대신 경량 검사(Tika 의존성 추가 없음), 허용 MIME 화이트리스트 선행 `[테스트완료]`

### 2단계 — 로그인 이후

- [x] 로그인 (세션 기반, 실패 잠금, 단일 진입) — ADMIN_YN으로 관리자/일반 분기. 관리자면 ADMIN 세션 추가+대시보드, 일반이면 사용자메인. 별도 관리자 로그인 페이지 없음. 위험: 비밀번호 평문·무솔트 저장 금지(bcrypt), 실패 잠금 미적용 시 무차별 대입 노출 `[테스트완료]`
- [x] 아이디 저장 체크 (localStorage, 서버 미전송) — 실제 `arkive-common.js`를 Node VM으로 실행해 저장·복원·삭제가 서버 호출 없이 동작함을 확인했다. 브라우저 저장소에서의 동작은 미확인 `[일부검증]`
- [x] 댓글 (로그인 이후. 1단, 대댓글 없음. BOARD_COMMENT, 작성자·관리자 삭제) `[테스트완료]`
- [x] 사용자 관리 (등록·수정·삭제·비밀번호 초기화) — `UserManageControllerTest`로 관리자 세션 필수, 등록 시 기본 역할 부여·입력 검증, 수정·삭제, 잠금 해제·임시 비밀번호 발급을 확인했다. 위험: 초기화 규칙(아이디+1234 같은 고정값) 금지, 변경 이력 남기기 `[테스트완료]`
- [x] 관리자 로그인은 단일 로그인으로 통합됨 (위 로그인 항목 참조, 별도 진입 없음) `[테스트완료]`
- [x] 쪽지관리 (eGov v5.0 uss, 받은·보낸, 발송·읽음·함별 삭제) — 위험: 로그인·사용자관리 선행, 발송·수신함 분리 `[테스트완료]`
- [x] 중복로그인방지 (사용자별 최신 세션 1개 유지, 새 로그인 시 이전 세션 만료, 폼·OAuth 공통) — 위험: 단일 인스턴스 메모리 레지스트리라 다중 인스턴스는 공유 세션 저장소 필요, 이전 브라우저는 안내 없이 로그인 화면으로 이동, 기존 로그인을 거부하는 방식이 필요하면 정책 변경 `[테스트완료]`
- [x] 사이트맵 (`/sitemap`, 로그인 사용자 권한별 메뉴 트리, 읽기 전용) — `MenuInterceptor`가 만든 권한 필터 메뉴를 재사용한다. 위험: 메뉴 순서·표시명 변경은 메뉴관리 화면에서 해야 하며 사이트맵 자체에는 저장 기능이 없음 `[테스트완료]`
- [x] 권한관리 (APP_ROLE·APP_USER_ROLE·APP_ROLE_MENU, 역할별 메뉴 조회·쓰기, 사용자 배정, `MenuPermissionInterceptor` 서버 강제, SSE 역할 필터) — 위험: `/system`·`/auth`는 기존 ADMIN_YN 슈퍼관리자만 접근하므로 일반 역할에 해당 메뉴를 부여해도 실제 접근은 열리지 않음. 다중 역할은 메뉴별 OR, 쓰기는 조회를 포함, 역할 없는 사용자는 기본 거부. 화면 숨김과 별개로 서버에서 403을 반환하며 컨트롤러마다 분기를 넣지 않음 `[테스트완료]`
- [x] Q&A 내·외부 구분 (질문자 구분 ASK_TYPE + 내부글 열람 제한 + 답변 권한 분리) — `BOARD.ASK_TYPE`(PUBLIC/INTERNAL)로 외부 질문은 전체 공개, 내부 질문은 로그인 사용자만 열람한다. 외부 화면에서 등록하면 PUBLIC으로 강제하고, 답변 저장은 관리자 세션만 허용한다. 위험: 내부글 열람 제한은 로그인 기준이라 사용자별 세부 권한은 없음, 답변 권한은 ADMIN 전용이라 역할 기반 답변은 미지원 `[테스트완료]`
- [x] 로그관리 (로그인·메뉴·개인정보 열람·다운로드) — `AUDIT_LOG`에 로그인·로그아웃·메뉴 접근·개인정보 조회/다운로드·API 접근을 기록하고 `/system/auditLogList`에서 조회·엑셀 다운로드한다. `AuditInterceptor`가 대상 경로를 판정하고 `AuditLogEventListener`가 비동기로 적재한다. 위험: 보관주기 삭제는 기본 비활성(`arkive.audit.retention-days=0`), 대상 경로·이벤트 목록은 샘플, 로그에 비밀번호·본문 원문은 기록하지 않음 `[테스트완료]`
- [x] 개인정보 보호 (마스킹, 열람·다운로드 로그 연계) — 목록 `EgovMap`을 표시 직전에 마스킹하고(`MaskingInterceptor`), 개인정보 조회·다운로드를 감사 로그로 남긴다. 주소록·명함의 연락처/이메일과 사용자 휴대전화는 AES-GCM(`enc:v1:`)으로 저장한다. `MaskingUtilTest`로 마스킹 규칙을, `AuditInterceptorIntegrationTest`로 주소록 조회 PII_VIEW·통계 엑셀 PII_DOWNLOAD·메뉴 접근 로그 적재를, 주소록·명함·사용자관리 테스트로 저장 암호화와 상세 평문 복원을 확인했다. 위험: 상세·엑셀·수동 화면의 마스킹 누락 지점 점검 필요, `APP_USER.EMAIL`은 아이디 찾기 조회 키라 평문 유지, 키 분실 시 복구 불가, `ARKIVE_FIELD_KEY` 미설정 시 연락처 저장이 거부됨 `[테스트완료]`
- [x] 로그인 (OAuth 2.0, 소셜: 네이버·카카오·구글) (code flow 직접 구현 + OAUTH_LINK 매핑) — `OauthControllerTest`로 미지원·미설정 provider와 잘못된 state 요청이 리다이렉트 없이 5xx로 거부됨을 확인했다. 화면의 소셜 버튼은 `openReadyModal()` 자리표시자라 실제 로그인 UI 연결은 미구현. 위험: 클라이언트 시크릿·콜백 URL 환경 분리, 기존 세션과 회원 매핑 정책 선행 `[일부검증]`
- [x] 로그인 (JWT, API용) (JwtUtil + /api/** 필터) — 세션 로그인과 병행. 화면·관리는 세션, 외부 API·모바일은 JWT. `JwtUtilTest`로 발급·해석, 다른 시크릿·만료·형식 오류 거부, 시크릿 미설정 시 임시 키 생성을, `ApiControllerTest`로 `/api/**` 토큰 검증을 확인했다. 위험: 시크릿·만료시간 환경 분리, 로그아웃 시 블랙리스트(또는 짧은 만료) 정책 선행 `[테스트완료]`
- [x] 2차인증 (TOTP) — 로그인 성공 후 `TOTP_CHALLENGE`로 코드를 검증하고, 설정·복구코드·관리자 초기화를 제공한다. 비밀키는 AES-GCM, 복구코드는 SHA-256 해시로 저장한다. `TotpServiceTest`로 설정 확인·복구코드 1회 사용·초기화·잘못된 코드 거부를, `LoginTotpFlowTest`로 로그인 후 TOTP 요구와 5회 실패 잠금을 확인했다. 위험: 복구코드 분실 시 관리자 초기화 절차·본인확인 정책 확인 필요, `ARKIVE_FIELD_KEY` 미설정 시 TOTP 설정 비활성 `[테스트완료]`
- [x] 통계 (게시판·접속 현황) — `/stats/statsList`에서 기간별 게시판 건수·감사 이벤트·일별 로그인/접속·다운로드 집계를 보여주고 엑셀로 내려받는다. `StatsControllerTest`로 이벤트·일별 집계, 역순 기간 보정, 엑셀 다운로드를 확인했다. 위험: 집계 원천이 `AUDIT_LOG`라 감사 로그 도입 이전 구간은 비어 있고, 방문자 수는 세션/감사 이벤트 기준의 근사치 `[테스트완료]`
- [x] 외부 웹분석 (GA4 + Clarity) — 동의(`arkive.analytics.require-consent=true`) 후에만 스크립트를 로드하고, ID는 `ARKIVE_GA4_ID`·`ARKIVE_CLARITY_ID` 환경값으로 주입한다. 미설정 시 비활성. `AnalyticsModelAdviceTest`로 ID 미설정·형식 오류 차단과 동의값 처리를, Node VM으로 동의 전 미로드·수락 시 GA4/Clarity 삽입·거부 시 미로드를 확인했다. 위험: 실제 측정 ID·허용 도메인·리플레이 개인정보 마스킹 정책 미확인 `[일부검증]`
- [x] 현황·통계 패턴 (ADTM statMng 참고) — 조회→집계→엑셀 다운로드 세트를 `/stats`에 구현해 다른 현황판의 템플릿으로 재사용한다. `StatsControllerTest`로 조회→집계→엑셀 세트를 확인했다. 위험: 도메인별 집계 쿼리는 대상 업무에 맞게 교체 필요 `[테스트완료]`
- [x] 통합검색 + 초성검색 (GIES goSearch 참고, DB LIKE 우선, es-hangul) — `/search`에서 로그인 사용자 대상으로 BOARD CK·TOAST·FAQ·QNA 제목 LIKE와 `SEARCH_CHOSUNG` 초성 검색을 수행하고, 본문은 태그 제거 요약으로 노출한다. Elasticsearch는 `arkive.search.elasticsearch.enabled=false` 기본이며 켜면 색인 조회로 전환되고 `/search/reindex.json`으로 재색인한다. 위험: 본문 검색은 에디터 HTML 오탐으로 보류, ES 운영·동기화 정책 미확인, LIKE 성능 한계, Q&A 공개 범위와 강의 검색은 미적용 `[테스트완료]`
- [x] SMS 보내기 (알리고 기반, 인증번호·알림) — 게시글 알림을 `SmsService`가 `SMS_YN='Y'`·휴대전화 보유·메뉴 조회 권한 사용자에게 발송하고 `BOARD.SMS_SENT_YN`을 원자적으로 선점해 1회만 보낸다. 기본 비활성(`ARKIVE_SMS_ENABLED=false`). `AligoSmsSenderTest`·`SmsServiceImplTest`로 비활성 시 DB 조회 없음, 자격증명 미설정 차단, LMS 전환·길이 제한, 실패 응답, 수신 대상 필터링, 중복 발송 차단을 확인했다. 위험: 실제 알리고 계약·발신번호·요금 정책 미확인, 실패 재시도 없음, 인증번호 발송 용도는 미구현 `[일부검증]`
- [x] Mail 보내기 + @Async (spring-boot-starter-mail, `MailAsyncService`/`MailServiceImpl`, 기본 `MAIL_ENABLED=false`) — 회원가입 환영·공지/중요글은 수신 동의 사용자에게만, 비밀번호 재설정은 거래성이라 동의와 무관하게 발송. 게시글 알림은 `BOARD.MAIL_SENT_YN`을 원자적으로 선점해 1회만 발송. 위험: 실제 SMTP 계정·발신자·대량 발송 정책 미확인, SMTP 장애 시 재시도 없음 `[테스트완료]`
- [x] 중요글 알림 채널 분리 (글쓰기 중요 체크박스 → BOARD 표시 저장 → 채널별 발송) — CK/TOAST 중요글·공지 저장 후 `BoardNotificationEvent`를 발행하고 `BoardNotificationEventListener`가 SSE·메일·SMS를 채널별로 처리한다. 채널 하나가 실패해도 다른 채널과 본문 저장에 영향을 주지 않는다. 위험: 채널 추가 시 기존 저장·조회 로직을 변경하지 말 것, 실제 SSE 구독·SMTP·SMS 발송은 미확인 `[테스트완료]`
- [x] ApplicationEvent 기반 로그 (업무 코드 오염 없이 기록) — 감사 로그(`AuditEvent`)와 게시글 알림(`BoardNotificationEvent`)을 이벤트로 발행하고 리스너에서 적재·발송한다. `AuditInterceptorIntegrationTest`로 인터셉터→이벤트→`AUDIT_LOG` 적재를, `AuditLogServiceTest`로 조회·보관 정리를, `BoardNotificationEventListenerTest`로 알림 채널 분리를 확인했다. 위험: 비동기 리스너라 애플리케이션 종료·큐 포화 시 유실 가능 `[테스트완료]`
- [ ] 외부 API 연계 (수신) — 다른 시스템 API 연결 (pony ImportApiController 아임포트 검증 참고). 위험: 타임아웃·재시도·서킷브레이커 없이 붙이면 장애 전파, 키·URL 환경 분리
- [x] 내부 API 제공 (송신) — `/internal/v1/health`·`/internal/v1/notices`를 `X-Internal-Token` 상수시간 비교로 보호한다. 토큰 미설정 시 503, 불일치 시 401이며 공지 응답에는 개인정보를 포함하지 않는다. Swagger 문서 노출. `InternalApiControllerTest`로 잘못된 토큰 401, 유효 토큰 health UP, 공지 응답의 개인정보 미포함을 확인했고 실행 앱에서도 토큰 미설정 시 503을 확인했다. 위험: 토큰 발급·회전·소비자 계약·버전 관리 정책 미확인 `[테스트완료]`
- [x] 본인인증 (NICE/휴대폰) (CheckPlus 요청·복호화·세션 저장, 미계약 차단) — 회원가입·비밀번호 찾기의 전제. 코드에 NiceCheck 있음. `NiceVerifyControllerTest`와 실행 앱에서 미설정 시 start가 리다이렉트 없이 5xx, status가 verified=false로 응답함을 확인했다. 화면 진입 링크는 없음. 위험: 실계약·운영사용 여부 미확인, 테스트키와 운영키 분리 `[일부검증]`
- [x] 회원가입 + 아이디 찾기(마스킹) + 비밀번호 찾기(임시 10자리 1회 표시·잠금 해제) — 일반 회원 고정(ADMIN_YN 강제 N), 필수 약관 동의 포함. `SignupControllerTest`로 화면 렌더·필수값 검증, 아이디 찾기 마스킹과 미등록 회원 거부, 본인 확인 후에만 비밀번호 재설정 토큰 발급을 확인했다. 위험: 임시 비밀번호 화면 이탈 시 재발급 `[테스트완료]`
- [x] 비밀번호 재설정 토큰 (30분 유효·1회 사용·SHA-256 해시 저장·60초 재발급 제한, `PASSWORD_RESET_TOKEN`) — 이메일 링크로 본인이 새 비밀번호를 설정하고 기존 로그인 세션을 만료한다. 위험: 실제 SMTP 전달·발송 실패 재시도·IP별 요청 제한은 미확인 `[테스트완료]`
- [x] 약관동의 (버전 고정·동의이력 보관) — `TERMS`·`USER_TERMS_CONSENT`로 버전별 필수/선택 약관과 동의 이력을 저장하고, 로그인 후 미동의 필수 약관이 있으면 `TermsInterceptor`가 동의 화면으로 유도한다. 위험: 실제 약관 문구는 자리표시자라 법무·개인정보 담당자 검토 필요 `[테스트완료]`
- [x] DB 암호화 (연락처·계좌 등) — 주소록·명함의 연락처/이메일과 사용자 휴대전화를 AES-GCM(`enc:v1:`, 키 `ARKIVE_FIELD_KEY`)으로 저장하고 조회 시 복호화한다. `FieldCryptoServiceTest`·`PiiFieldServiceTest`로 암복호화·키 미설정 거부·평문 통과·이중 암호화 방지를, 주소록·명함·사용자관리·회원가입 테스트로 저장값 `enc:v1:`과 상세 평문 복원을 확인했다. 위험: 키 관리(분실 시 복구 불가), `APP_USER.EMAIL`·이름 등 조회 조건 컬럼은 평문 유지, 기존 평문 데이터 이관은 미포함(V15·V34 적용 필요), TOTP·연락처 저장은 키 미설정 시 거부 `[테스트완료]`

### 3단계 — 조건 해소 후 (외부·승인·규모)

- [x] 휴가관리 (내 휴가·휴가관리·연차관리 3화면, `LEAVE_BALANCE`/`LEAVE_REQUEST`, 주말·공휴일 제외 평일 일수와 반차 0.5일 계산, 승인 시 차감·승인 취소 시 환원, 신청중 포함 잔여 초과 금지, 메뉴 권한·이중등록방지 적용) — 승인은 자체 승인/반려로 구현했고 약식결재 연계는 분리했다. 위험: 연차 발생·이월·반차 시간 규칙은 샘플 정책이므로 대상 프로젝트에서 교체 필요, 승인 권한은 메뉴 WRITE 권한 기반이라 결재선·대결자는 미지원 `[테스트완료]`
- [x] 회의실관리 + 회의실예약관리 (eGov v5.0 uss:ion:mtg, `MEETING_ROOM`/`MEETING_RESERVATION`, 회의실 마스터 선행·같은 날짜 시간대 중복 차단, 본인·관리자 예약 취소, 메뉴 권한·이중등록방지) — `MeetingControllerTest`로 회의실 CRUD, 예약 보유 시 삭제 차단, 시간 중복·역전 거부, 본인/관리자 취소, 메뉴 권한을 확인했다. 위험: 시간대는 `HH:MM` 문자열 비교라 자정 넘김 예약은 미지원, 예약 변경 이력 없음 `[테스트완료]`
- [x] 약식결재 (eGov v5.0 uss:ion:ism, `APPROVAL_DOC`/`APPROVAL_HISTORY`, 기안→상신→승인/반려/상신취소, 지정 결재자 서버 확인, 이력 기록) — `ApprovalControllerTest`로 기안→상신→승인/반려/상신취소, 지정 결재자 서버 확인, 이력 기록, 비참여자 열람 차단을 확인했다. 위험: 결재선·대결자·대상 업무 연계는 미지원, 문서구분은 샘플 상수라 대상 프로젝트의 공통코드 COM75로 교체 필요 `[테스트완료]`
- [x] 기념일·경조사·포상·개인연차 (eGov v5.0 uss, HR 세트, `HR_ANNIVERSARY`/`HR_EVENT`/`HR_REWARD`, 개인연차는 기존 LEAVE_BALANCE·LEAVE_REQUEST·DUTY를 사용자 단위로 조회) — `HrControllerTest`로 기념일·경조사·포상 CRUD와 날짜/금액/구분 검증, 개인연차 조회, 관리자 메뉴 권한을 확인했다. 위험: 유형 코드는 샘플 상수, HR 화면은 관리자 역할 전용, 약식결재·휴가 승인과의 자동 연계는 없음 `[테스트완료]`
- [x] 설문관리 세트 (eGov v5.0 uss:olp, 템플릿→설문→질문→항목→참여→통계 6종, 단일/복수/주관식, 익명 여부, 공개·마감, 문항별 통계) — `SurveyControllerTest`로 템플릿·설문·질문·항목 CRUD, 공개 후 잠금, 참여 응답 검증·중복 방지, 통계 화면 응답을 확인했다. 위험: 설문은 작성중(DRAFT)일 때만 수정·삭제, 공개 후 문항 변경 금지, 통계는 참여 원본 기준 단순 집계 `[테스트완료]`
- [x] 강의-설문 연계형 수료 (동영상 1개 + 설문 1세트, 시청 기준 비율과 설문 참여로 수료 판정, `LECTURE`/`LECTURE_PROGRESS`, Range 스트리밍·이어보기·수료현황) — `LectureControllerTest`로 최대 시청 위치 저장·수료 판정(시청+설문), 이어보기·수료현황, 등록·상태 전환·스트리밍 조건 검증을 확인했다. 위험: 수료 판정은 저장된 최대 시청 위치 기준이라 실제 시청 시간과 다를 수 있음, 설문이 삭제되면 설문 조건은 제외되고 시청 조건만 적용 `[테스트완료]`
- [x] 동영상 인코딩 + 강의형 콘텐츠 (BLMS 이식, FFmpeg 선택 실행, `arkive.ffmpeg.enabled=false` 기본, mp4/webm은 인코딩 없이 업로드, VOD 경로 `arkive.vod.path`) — `FfmpegVideoEncodingServiceTest`로 비활성 모드의 mp4 저장·길이 유지, 확장자/길이/크기/경로 이탈 거부, 삭제를 확인했다. 이 환경에는 FFmpeg가 없어 실제 인코딩은 미확인. 위험: FFmpeg 미설치·비활성 시 재생 길이를 직접 입력해야 하고 썸네일·다중 화질은 미지원, VOD 경로는 웹 루트 밖으로 분리 권장 `[일부검증]`
- [x] 약도관리 (eGov v5.0 uss:ion:rmm, `MAP_GUIDE`, 주소·위도·경도·설명, 카카오 지도 SDK는 `Globals.kakao.mapKey`가 있을 때만 로드) — `MapControllerTest`로 약도 CRUD·사용여부 필터·제목/좌표 검증·이중등록 차단을 확인했다. 위험: 키·허용 도메인 없이는 지도 없이 주소·좌표만 표시, 주소→좌표 지오코딩은 외부 계약 미확인이라 미적용 `[테스트완료]`
- [x] SSO연계 (eGov v2 uat, OAuth2 authorization code, `OAUTH_LINK.PROVIDER='sso'`, state 검증·토큰 교환·사용자정보 조회·기존 연계 계정 로그인, TOTP 지원) — `SsoControllerTest`와 실행 앱에서 SSO 비활성 시 `/sso/login`·`/sso/callback`이 404로 응답함을 확인했다. 화면 진입 링크는 없음. 위험: 외부 SSO 서버 계약(URL·필드·글로벌 로그아웃) 미확인, 기본은 사전 연계 계정만 허용하고 자동 가입은 `arkive.sso.auto-create=false` `[일부검증]`
- [x] WebSocket (양방향 채팅, `/ws/chat`, 세션 쿠키 인증 + 채팅 메뉴 WRITE 검사, Origin 허용목록, `CHAT_MESSAGE` 최근 100건 이력) — `ChatWebSocketTest`로 세션·채팅 WRITE 권한 핸드셰이크, Origin 허용목록, 최근 이력 전송, 세션 신원으로 저장·브로드캐스트, 공백/길이 초과 차단, 저장 실패 오류 응답을 확인했다. 위험: 단일 인스턴스 in-memory 브로드캐스트라 다중 인스턴스는 공유 브로커 필요, DB는 HSQLDB 문법 기준이라 Oracle 함수 기반 쿼리 이식 금지 `[테스트완료]`
- [x] Spring Batch (일일 통계 집계 `dailyStatsJob`/`dailyStatsStep`, `STAT_DAILY`, 화면 수동 실행 `/batch/batchList`, 같은 날짜 재집계 멱등) — `BatchControllerTest`로 일일 집계, 같은 날짜 재집계 멱등(중복 행 없음), 날짜·토큰 검증, 목록 조회를 확인했다. 위험: `ResourcelessJobRepository`라 실행 이력·재시작·자동 스케줄링이 없고, 집계는 게시판 건수·로그인·접속·강의 수료·설문 참여로 한정. 대량 발송·분산 배치는 미적용 `[테스트완료]`

## 검증 현황

기능별 상태는 위 로드맵의 표시를 그대로 집계한 것이다. 상세 확인 이력은 코드·테스트·`docs/ai/project/`를 따르고, 이 절은 현재 상태만 나열한다.

검증 순서: `미검증 → 일부검증 → 테스트완료 → GUI확인`. 아래 절도 이 순서로 정렬했다. `보류`는 구현 전이라 검증 순서에 포함하지 않고 맨 뒤에 둔다. GUI확인은 마지막 단계이며, 자동 테스트(테스트완료)를 대체하지 않는다.

최근 실행(2026-10-25): `verify.cmd` 테스트 314개 통과, 실패·오류 0, JaCoCo 기준 충족(`BUILD SUCCESS`). 이번 보강에서 `BoardSseServiceTest`(5), `FileControllerTest`(7), `OpenApiDocsTest`(2), `AuditInterceptorIntegrationTest`(4), `BoardControllerTest`(5), `UserQnaControllerTest`(6)를 추가·확장했다. 테스트 중 실제 결함 2건을 발견해 수정했다: TUS CORS 프리플라이트(OPTIONS)가 관리자 세션 없이 302로 막히던 문제(OPTIONS만 허용, 실제 쓰기 요청은 인증 유지)와 묶음 ZIP 다운로드가 첫 파일만 포함되던 문제(Spring 6 `FileCopyUtils.copy`가 ZIP 출력 스트림까지 닫아 `InputStream.transferTo`로 교체). `[테스트완료]`는 자동 테스트 기준이며 실제 브라우저 화면 동작은 `[GUI확인]`과 별개다.

### 미검증 (0)

해당 항목 없음.

### 일부검증 (7)

외부 계약·실장비·브라우저 저장소가 필요해 자동 테스트로 핵심 동작을 닫지 못한 항목이다.

- 아이디 저장 체크 (localStorage)
- 로그인 (OAuth 2.0, 소셜: 네이버·카카오·구글)
- 외부 웹분석 (GA4 + Clarity)
- SMS 보내기
- 본인인증 (NICE/휴대폰)
- 동영상 인코딩 + 강의형 콘텐츠
- SSO연계

### 테스트완료 (58)

자동 테스트로 핵심 동작을 확인한 항목이다.

- 글쓰기 본문 sanitize
- 공지 고정
- 파일 실형식 검증
- 로그인 (세션)
- 댓글
- 관리자 로그인 단일화
- 쪽지관리
- 중복로그인방지
- 사이트맵
- 권한관리
- Q&A 내·외부 구분
- 로그관리
- DB 암호화 (연락처·계좌 등)
- 통계 (게시판·접속 현황)
- 현황·통계 패턴
- 내부 API 제공 (송신)
- Mail 보내기 + @Async
- 비밀번호 재설정 토큰
- 약관동의
- 휴가관리
- 회의실관리 + 회의실예약관리
- 약식결재
- 기념일·경조사·포상·개인연차
- 설문관리 세트
- 강의-설문 연계형 수료
- 약도관리
- WebSocket
- Spring Batch
- 페이징
- 이전글/다음글
- 통합검색 + 초성검색
- 중요글 알림 채널 분리
- 게시판
- 공통코드 관리
- 파일 업로드
- 실시간 알림 SSE
- Swagger
- 이중등록방지 Thymeleaf 적용
- FAQ관리
- Q&A관리
- 메모보고
- 팝업창관리
- 배너관리
- 메인이미지관리
- 부서일정관리
- 일정관리(개인)·일지관리·메모할일
- 주소록관리
- 명함관리
- 상담관리
- 온라인POLL
- 당직관리(단독)
- 엑셀 업로드/다운로드 + 양식 일괄등록
- 사용자 관리
- 개인정보 보호
- 로그인 (JWT, API용)
- 2차인증 (TOTP)
- ApplicationEvent 기반 로그
- 회원가입 + 아이디 찾기 + 비밀번호 찾기

### GUI확인 (4)

실제 브라우저에서 화면과 동작을 확인한 항목이다. 마지막 검증 단계다.

- 공휴일관리(달력)
- 카테고리 설정(jstree형)
- 카테고리 설정(컬럼 드릴다운형)
- 파일 미리보기(뷰어)

### 보류 (2)

아직 구현하지 않았거나 조건 해소 후 진행할 항목이다.

- 목록 썸네일 + 상세 지도 표시 패턴
- 외부 API 연계 (수신)
