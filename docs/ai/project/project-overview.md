# 프로젝트 개요·실행 설정 (Arkive 기록)

> 이식 구분: **Arkive 기록**. 대상 프로젝트로 복사하지 않는다. 이식용 빈 양식은 [project-template-pack.md](../portable/project-template-pack.md), 기능별 절차는 [feature-porting-sheet.md](../portable/feature-porting-sheet.md)를 쓴다.

<!--
문서 상태: 부분 미완성
미완성 항목: 운영 조직·기능 소유권, 운영 프로파일·DB·배포 토폴로지, 보안 정책 승인, 외부 계약, Health·장애 대응·Canary 지원 여부.
필요한 근거/확인자: 업무·보안·운영 담당자, 환경별 설정, 운영 아키텍처 문서, 공식 연계 명세.
완성 방법: 아래 미확인 사항을 담당자 근거로 보강하고 실제 비밀값은 기록하지 않는다.
완료 기준: 운영 구성·소유 역할·외부 계약이 근거와 함께 확인되거나 미확인 항목의 담당자와 후속 확인 경로가 지정되어 있다.
-->

Arkive는 기능 이식용 아카이브/레퍼런스다. 아래는 저장소에서 확인한 로컬 구성이지 승인된 운영 아키텍처가 아니다. 기존 개요·아키텍처의 코드 관찰(2026-09-25~10-24)을 통합했으며 설정·세션·채팅·예약 작업은 2026-11-08 관련 코드와 재대조했다.

## 확인된 기술 및 실행 구성

| 구분 | 확인된 내용 | 근거 |
| --- | --- | --- |
| 빌드 | Maven, artifact `Arkive:Arkive:1.0.0` | `pom.xml` |
| Java / 프레임워크 | Java 17, eGovFrame Boot parent 5.0.0, Spring Boot 의존성 사용 | `pom.xml` |
| 웹 화면 | Thymeleaf, 템플릿은 `src/main/resources/templates/thymeleaf/` | `pom.xml`, `application.properties` |
| 데이터 접근 | MyBatis, Java Config, Mapper/XML | `egovframework.example.config.EgovConfigMapper`, `EgovConfigDatasource` |
| 기본 포트 | 8080 | `application.properties` |
| 현재 설정 DB | `Globals.DbType=hsql_server`; `./data/ArkiveDB` 파일 DB를 `Globals.hsql_server.Port=9001`에서 제공. 테스트는 9002 | `application.properties`, `EgovConfigDatasource.java` |
| SQL 마이그레이션 | `SCHEMA_MIGRATION`에 없는 버전 SQL만 숫자 순으로 적용 | `EgovConfigDatasource`, `src/main/resources/db/versions/`; 상세는 [DB 이력](migration.md) |
| 모니터링 | Actuator `health,info,metrics`, `health.show-details=always`; 메일 헬스 기본 비활성 | `application.properties`; 운영 공개 범위 미확인 |
| 운영 배포 / 실서비스 DB | 미확인. 로컬 설정으로 운영 구성을 추정하지 않는다. | 운영 담당자 확인 필요 |

## 코드 탐색 시작점

디렉터리·계층·공통 호출 흐름은 [아키텍처](architecture.md), 기능별 파일·복사 묶음은 [기능 인벤토리](function-inventory.md)에서 찾는다. 동작·주의점은 [기능 상세](feature-status.md), 요청/응답은 [API 기록](api-contract.md), 공통 변경의 소비자는 [변경 영향](change-impact.md)을 따른다. 이 개요에는 실행 설정과 운영 미확인 사항을 둔다.

## 실행 전 확인할 설정

근거는 `src/main/resources/application.properties`다. 실제 자격증명은 환경변수로 주입하고 문서·로그·이식 파일에 복사하지 않는다. 아래 기본값만으로 실연동·운영 안전성을 보장하지 않는다.

| 기능 | 키 / 기본값 | 확인할 것 |
| --- | --- | --- |
| 메일 | `MAIL_ENABLED=false`, `MAIL_FROM` 빈 값, `MAIL_HOST`·`MAIL_PORT`·인증/TLS·base URL | 발신자와 SMTP 설정. 테스트는 `arkive.mail.enabled=false`로 실제 연결하지 않음 |
| 메일 헬스 | `MAIL_HEALTH_ENABLED=false` | SMTP 사용 환경에서 필요할 때 활성화 |
| SMS / SSO | `ARKIVE_SMS_ENABLED=false`, `SSO_ENABLED=false` | 제공자 계약·발신번호·콜백과 대상 정책 |
| 동영상 / 지도 | `FFMPEG_ENABLED=false`, `KAKAO_MAP_KEY` 빈 값 | FFmpeg 설치·VOD 경로, 카카오 허용 도메인 |
| 웹분석 / 검색 | `ARKIVE_ANALYTICS_ENABLED=false`, `ARKIVE_SEARCH_ES_ENABLED=false` | GA4·Clarity ID와 동의, ES 인프라·동기화 |
| 내부 API | `ARKIVE_INTERNAL_API_TOKEN` 빈 값 | 미설정 시 503; 토큰 발급·회전·소비자 계약 |
| 필드 암호화 | `ARKIVE_FIELD_KEY` 빈 값 | TOTP·연락처·사용자 휴대전화 저장이 거부됨. 키 분실 시 암호문 복구 불가 |
| SSE | `layout.html`의 `ARKIVE_SSE_ENABLED=false` | 환경변수가 아닌 화면 설정; 활성화·화면별 옵션은 [알림 상세](feature-status.md#알림-설정과-실패-처리) |

- DB URL과 `Globals.hsql_server.Port`를 일치시킨다. `data/`는 Git 관리 대상이 아닌 로컬 DB이며 실행 중 `.lck`를 직접 삭제하지 않는다. 메모리 DB에서 자동 이관되지 않으므로 [마이그레이션 기록](migration.md)을 확인한다.
- `QuartzConfig`는 10분 Trigger, `DBJob`은 `Globals.dumpPath`(기본 `./backup/ArkiveDB.sql`)에 HSQLDB `SCRIPT` 덤프를 기록한다. 폴더 생성 후 기존 덤프를 지우므로 다중 이력 보존이 아니며, 삭제 후 실패·파일 권한·복구는 미확인이다.
- 로그인·메뉴 권한·채팅·배치의 제약은 [기능 상세](feature-status.md)에서 확인한다. 로컬 메모리 세션이나 브로드캐스트를 다중 서버용 공유 상태로 보지 않는다.

## 아직 확인되지 않은 운영 지식

| 미확인 항목 | 필요한 근거 / 확인자 |
| --- | --- |
| 실제 업무 목적·운영 대상 기능·전체 모듈 경계·소유권 | 업무 책임자, 화면·배포 대상 목록 |
| 프로파일·배포 토폴로지·운영 DB 제품/버전·공유 상태 | 운영·인프라 담당자, 환경별 설정·운영 구성도 |
| 인증·권한 승인·개인정보 보존/파기·암호화 키 전달/회전 | 보안·개인정보 담당자, 승인 정책 |
| 외부 API/SMTP 계약·타임아웃·오류/재시도·발송 이력 | 연계 담당자·정식 명세. SMTP는 SPF/DKIM·반송·수신거부·대량 발송·1회 선점 후 실패 정책 포함 |
| Health 기준·장애 대응·백업/복구·Canary | 운영 담당자, 실행 증거와 승인된 절차 |

## 기존 참조와 구현 선택

기존 개발 기록의 선택 이유를 보존한다. 동일 구현을 강요하지 않으며 대상 요구사항·현재 코드와 다시 대조한다.

- eGovFrame 공통컴포넌트 v5.0 가이드: https://www.egovframe.go.kr/wiki/doku.php?id=egovframework:com:v5.0:init_guide (명함·상담 등 참고).
- 당시 참조 프로젝트: ADTM, BLMS, gcsms, GIES, GTEMS, pony-main, GCG-master, HTA-main. BLMS의 팝업·배너·카테고리·FAQ/Q&A, pony의 공지·Q&A, gcsms의 당직·엑셀·민원, GTEMS의 설문(qestnr)을 CRUD/스키마 패턴 근거로 참고했다. ADTM의 지역 JOIN·HTA의 raw JDBC는 이식 부적합으로 제외했다. 참조 프로젝트가 현재 저장소에 포함됐다는 뜻은 아니다.

| 기능 | 참조 / Arkive의 선택 |
| --- | --- |
| FAQ·Q&A | BLMS/gcsms 단일 테이블 패턴 → `BOARD`+`BOARD_TYPE` 재사용 |
| Q&A·메모 상태 | pony `answerStatus` → Q&A는 JOIN 판정, 메모는 SQL 조건으로 상태 전이 강제 |
| 카테고리 | BLMS jstree → 네이티브 `<details>`와 jstree 규격 `tree.json` 병행 |
| 메인이미지 | eGov 별도 컴포넌트 → `BANNER`+`POSITION` 통합 |
| 파일 실형식 | Tika 검토안 → 추가 의존성 없는 `FileTypeUtil` 매직바이트 검사 |
| POLL | 로그인 전제 대신 세션 집합으로 중복투표 차단 |
| 댓글 | eGov cop/cmt 대신 `BOARD` 종속 1단 댓글·작성자/관리자 삭제 |
| 쪽지 | eGov uss 대신 `MESSAGE` 단일 테이블·발신/수신 삭제 플래그 |
