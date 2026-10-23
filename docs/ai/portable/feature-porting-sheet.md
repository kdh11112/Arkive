# 기능 이식 시트 (1기능 1장)

> Arkive에서 기능을 실행해 확인한 뒤, 복사 대상과 대상 프로젝트 맞춤 값을 적는다.
> 이 시트를 채우지 못한 기능은 "이식 완료"로 보지 않는다.

## 0. Arkive에서 먼저 확인

- 기능명:
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
- [ ] Mapper 인터페이스:
- [ ] Mapper XML:
- [ ] `db/versions/V*__*.sql`:
- [ ] 메뉴 seed:
- [ ] `application.properties`의 `Globals.*` 키:
- [ ] 메일·비동기 설정(`spring.mail.*`, `arkive.mail.*`, Executor):
- [ ] `pom.xml` 의존성:
- [ ] 테스트 클래스:
- [ ] 테스트 이식 시트: [test-porting-sheet.md](test-porting-sheet.md)

## 2. 대상 프로젝트 맞춤

- 패키지:
- 테이블·컬럼:
- DB 제품/문법:
- 인증·권한 방식:
- 파일 업로드/다운로드 경로:
- 메뉴 ID:
- 외부 연계·키 주입 방식:

## 3. 복사 후 검증

- [ ] `doctor.cmd` 통과
- [ ] `verify.cmd` 통과(빌드·테스트·JaCoCo)
- [ ] 화면 정상 입력/필수값 누락/권한 없음 확인
- [ ] API 정상/401/권한 없음/잘못된 입력 확인
- [ ] DB insert·update·delete(soft delete 포함) 확인
- [ ] 메일 기능이면 실제 SMTP 대신 테스트 비활성/목으로 발송 조건·1회 발송·실패 무시 확인
- [ ] 기존 기능 회귀 확인
- [ ] 테스트 데이터 정리·롤백 확인

## 4. 남은 위험·메모

-
