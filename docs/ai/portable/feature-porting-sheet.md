# 기능 이식 시트 (1기능 1장)

> Arkive에서 기능을 실행해 확인한 뒤, 복사 대상과 대상 프로젝트 맞춤 값을 적는다.
> 이 시트를 채우지 못한 기능은 "이식 완료"로 보지 않는다.

## 0. Arkive에서 먼저 확인

- 기능명:
- Arkive 기준 버전(Commit SHA / Revision):
- 실행 방법: `mvn spring-boot:run` / `java -jar ...`
- 화면 URL:
- API URL:
- 로그인 계정/권한:
- 준비 데이터:
- 정상 결과:
- 오류·권한 결과:
- DB 확인:
- 확인일/확인자:
- 증거 위치(캡처·로그):

## 1. 복사 목록

- [ ] 화면 템플릿:
- [ ] 화면 JavaScript/CSS:
- [ ] Controller:
- [ ] Service / ServiceImpl:
- [ ] 이벤트 / 리스너:
- [ ] Mapper 인터페이스:
- [ ] Mapper XML:
- [ ] `db/schema.sql` 중 기능별 테이블·컬럼·제약조건·인덱스:
- [ ] `db/seed.sql` 중 필요한 메뉴·역할·사용자-역할·메뉴 권한·필수 초기값:
- [ ] 기존 대상 DB 갱신 절차·데모 HSQLDB 서버/초기화기 제외 여부:
- [ ] `application.properties`의 `Globals.*` 키:
- [ ] 메일·비동기 설정(`spring.mail.*`, `arkive.mail.*`, Executor):
- [ ] `pom.xml` 의존성:
- [ ] 검색 기능이면 검색 Controller/Service/Mapper/XML·결과 템플릿·상세 링크:
- [ ] 테스트 클래스:
- [ ] 스펙 담당자의 별도 테스트 준비·승인 근거(구현 에이전트는 테스트 파일 생성·수정 금지):
- [ ] 테스트 이식 시트: [test-porting-sheet.md](test-porting-sheet.md)

## 2. 대상 프로젝트 맞춤

- 대상 기준 버전/브랜치:
- 패키지:
- 테이블·컬럼:
- DB 제품/문법:
- 인증·권한 방식:
- 파일 업로드/다운로드 경로:
- 메뉴 ID:
- 외부 연계·키 주입 방식:

## 3. 복사 후 검증

실행 조건·실제 결과·근거·미수행 사유는 [Evidence](evidence-report-template.md#검증-결과)에 연결한다. 적용되지 않는 항목과 미수행은 [판정 기준](../common/test-standard.md#실행과-기록)으로 구분한다.

- [ ] `doctor.cmd` 통과
- [ ] `verify.cmd` 통과(빌드·테스트·JaCoCo)
- [ ] ego-browser로 화면 정상 입력/필수값 누락/권한 없음 확인
- [ ] 화면/CSS 변경이면 13.3인치 노트북 필수 확인·해상도/배율/viewport 기록 및 나머지 화면 유동 대응 확인(실기기 미확인은 별도 `미수행`):
- [ ] API 정상/401/권한 없음/잘못된 입력 확인
- [ ] 검색 기능이면 빈 검색어·무결과·본문 HTML 요약·상세 링크 확인
- [ ] DB insert·update·delete(soft delete 포함) 확인
- [ ] 메일 기능이면 제공된 테스트로 실제 SMTP 발송 없이 발송 조건·1회 선점·채널 실패 격리 확인
- [ ] 기존 기능 회귀 확인
- [ ] 테스트 데이터 정리·롤백 확인

## 4. 남은 위험·메모

-
