# Arkive 아키텍처 현황

<!--
문서 상태: 부분 미완성
미완성 항목: 인증·인가 적용 범위/역할 매핑, 외부 연계 호출 흐름과 소유자, 프로파일별 구성, 운영 배포 토폴로지, 전체 모듈 경계.
필요한 근거/확인자: Security 설정과 필터·인터셉터, API 명세/호출부, 환경별 설정, 운영 아키텍처 문서 및 담당자.
완성 방법: 코드·설정에서 확인한 흐름을 먼저 보강하고, 운영 구성과 책임은 담당자 확인 자료를 근거로 추가한다.
완료 기준: 업무·공통 모듈 경계와 실제 인증·외부 연계·배포 구성이 출처와 함께 기록되거나, 미지원/미확인이 담당자 확인으로 종결되어 있다.
-->

> 저장소 코드에서 확인한 구현 구조다. 목표 아키텍처나 전체 운영 구성을 의미하지 않는다.

## 애플리케이션 구성

- 진입 클래스: `egovframework.example.EgovBootApplication`
- Spring 기반 MVC 및 Thymeleaf 템플릿 렌더링을 사용한다.
- 업무 코드에는 `arkive.*`, eGovFrame 샘플·설정에는 `egovframework.example.*`, 공통 eGovFrame 코드에는 `egovframework.com.*` 패키지가 함께 있다.
- Arkive 시스템 기능 예: `arkive.admin.system.web.SystemController` → `SystemService` / `SystemServiceImpl` → `SystemMapper` → `System_SQL.xml`.
- Mapper XML은 `src/main/resources/egovframework/sqlmap/example/mappers/`에 있다. 인터페이스, XML namespace, SQL ID, 파라미터와 반환 타입을 함께 확인한다.
- Thymeleaf 화면은 `src/main/resources/templates/thymeleaf/` 아래에 있으며 레이아웃·공통 조각은 같은 디렉터리의 `layout/` 등에서 관리한다.
- 템플릿에서 `th:replace` 조각 사용과 화면 JavaScript의 jQuery `$.ajax` 호출이 확인된다. 요청·오류 표시의 세부 동작은 대상 화면과 호출 API에서 확인한다.
- 공통 파일 기능은 `arkive.admin.comm` 아래 Controller, Service, Mapper, 업로드/다운로드 유틸리티로 구성되어 있다.
- DB, Mapper, 트랜잭션 및 웹 설정은 `egovframework.example.config`에 Java Config로 존재한다.
- Controller에 `@Valid`와 `BindingResult`를 사용하는 입력 검증 예제가 있다. 실제 변경은 해당 기능의 서버 검증 패턴을 확인한다.
- 로깅은 Lombok `@Slf4j`와 SLF4J `LoggerFactory` 직접 사용이 함께 확인된다. 수정 시 대상 모듈의 기존 방식을 따른다.

## 비동기 및 예약 작업

- `arkive.com.config.QuartzConfig`에서 `DBJob`을 10분 간격으로 실행하는 Trigger를 구성한다.
- `DBJob`은 DB 덤프를 파일로 기록하는 코드를 포함한다. 스케줄 활성화, 경로, 보존 및 복구 정책은 [project-overview.md](project-overview.md)와 운영 담당자 확인을 참조한다.

## 확인이 필요한 점

- 인증·인가 필터/인터셉터 적용 범위와 실제 역할 매핑
- 외부 시스템 연계별 호출 흐름, 소유자, 타임아웃 및 오류 처리 계약
- 프로파일별 구성과 운영 배포 토폴로지
- 전체 모듈 경계 및 기능 소유 조직

수정 전 실제 관련 호출 흐름과 설정을 검색한다. 이 요약만으로 보안 적용이나 기능 소유권을 단정하지 않는다.
