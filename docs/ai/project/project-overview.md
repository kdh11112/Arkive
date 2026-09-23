# Arkive 프로젝트 개요

<!--
문서 상태: 부분 미완성
미완성 항목: 업무 목적/운영 조직, 운영 프로파일·배포 방식, 운영 DB 제품·버전, 권한·개인정보 요구사항, 외부 연계 계약, Health·장애 대응·Canary 지원 여부.
필요한 근거/확인자: 업무 책임자, 운영·인프라 담당자, 보안 담당자, 공식 연계 명세.
완성 방법: 확인된 담당자/문서와 환경별 배포 설정을 검토하고, 민감값 없이 실제 운영 기준만 표에 추가한다.
완료 기준: 필수 운영 환경과 소유 역할의 출처가 적혀 있고, 미확인 항목에는 담당자와 후속 확인 경로가 지정되어 있다.
-->

> 확인일: 2026-09-23. 아래 내용은 저장소 코드와 설정에서 확인한 현황이다. 운영 환경 및 업무 목적은 별도 근거가 확인될 때 갱신한다.

## 확인된 기술 및 실행 구성

| 구분 | 확인된 내용 | 근거 |
| --- | --- | --- |
| 빌드 | Maven, artifact `Arkive:Arkive:1.0.0` | `pom.xml` |
| Java / 프레임워크 | Java 17, eGovFrame Boot parent 5.0.0, Spring Boot 의존성 사용 | `pom.xml` |
| 웹 화면 | Thymeleaf, 템플릿은 `src/main/resources/templates/thymeleaf/` | `pom.xml`, `application.properties` |
| 데이터 접근 | MyBatis 및 eGovFrame 데이터 접근 설정, Mapper XML | `EgovConfigMapper.java`, `src/main/resources/egovframework/sqlmap/` |
| 기본 포트 | 8080 | `application.properties` |
| 현재 설정 DB | `Globals.DbType=hsql_server`; 설정과 코드상 HSQLDB 메모리 서버를 포트 9001에서 시작 | `application.properties`, `EgovConfigDatasource.java` |
| SQL 초기화 | `spring.sql.init.mode=always`; `sampledb.sql`, `system.sql` 지정 | `application.properties` |
| 운영 배포 / 실서비스 DB | 미확인. 현재 로컬 설정만으로 운영 구성을 추정하지 않는다. | 운영 설정 근거 미확인 |

## 주의할 실행 동작

- `src/main/resources/db/system.sql`은 `menu`, `menu_author`, `user`, `ATCH_FILE` 테이블을 `DROP TABLE IF EXISTS` 후 생성한다. `sampledb.sql`도 `SAMPLE`, `IDS` 삭제 및 재생성 구문으로 시작한다.
- 현재 설정처럼 SQL 초기화를 항상 실행하는 환경에서는 시작 시 기존 데이터가 초기화될 수 있다. 설정된 환경과 스크립트를 확인하기 전 운영 DB에서 애플리케이션을 실행하지 않는다.
- `QuartzConfig.java`는 10분 주기의 Trigger를 정의하고, `DBJob.java`는 DB `SCRIPT` 명령으로 `/db/data.sql`에 덤프를 기록하도록 구현되어 있다. 실제 실행 조건, 쓰기 권한, 백업 보관·보안 정책은 운영 담당자 확인이 필요하다.
- 설정 파일에는 외부 연계 및 암호화 관련 값이 포함될 수 있다. 문서, 로그, 증거 자료에 실제 비밀값을 복사하지 않는다. 환경별 설정 전달 방법은 미확인이다.

## 아직 확인되지 않은 프로젝트 지식

- 업무 목적, 운영 조직 및 기능별 책임자
- 운영 프로파일, 배포 방식, 대상 DB와 DB 버전
- 인증·권한 정책 및 개인정보 처리 요구사항
- 외부 API의 정식 계약, 오류 코드 체계와 재시도 정책
- Health 기준, 장애 대응, Canary 지원 여부

미확인 항목은 담당자 확인 및 근거 문서가 확보되면 관련 프로젝트 지식 문서에 기록한다.
