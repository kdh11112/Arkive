# Arkive 프로젝트 코딩 및 아키텍처 규칙 (Coding & Architectural Rules)

이 문서는 `Arkive` 프로젝트의 코드 분석 및 실행 환경을 바탕으로 도출된 핵심 규칙 및 컨벤션을 정리한 것입니다. 새로운 기능을 추가하거나 기존 코드를 수정할 때 이 규칙을 준수해야 합니다.

---

## 1. 아키텍처 및 패키지 구조
프로젝트는 **Layered Architecture**를 따르며, 전자정부 표준프레임워크(eGovFrame 4.x/5.x) 및 Spring Boot의 관례를 준수합니다.

*   **Package Root**:
    *   **업무 패키지**: `arkive.{domain}` (예: `arkive.admin.system`, `arkive.admin.comm`, `arkive.com`)
    *   **샘플/공통 예제**: `egovframework.example`
*   **Layers**:
    *   `web`: Controller 클래스 (`@Controller`, `@RestController`)
    *   `service`: Service 인터페이스 및 VO(Value Object) 클래스
    *   `service.impl`: Service 구현체 (`@Service`) 및 MyBatis Mapper 인터페이스 (`@EgovMapper`)
    *   `config`: Java 기반 설정 클래스 (`@Configuration`)
    *   `comm`: 공통 유틸리티, 공통 서비스 및 핸들러

---

## 2. 명명 규칙 (Naming Conventions)
*   **Controller**: `*Controller.java` (예: `SystemController.java`, `EgovSampleController.java`)
*   **Service Interface**: `*Service.java` (예: `SystemService.java`)
*   **Service Implementation**: `*ServiceImpl.java` (예: `SystemServiceImpl.java`)
*   **Mapper Interface**: `*Mapper.java` (예: `SystemMapper.java`)
*   **Value Object / Data Map**: `*VO.java` (예: `SampleVO.java`) 또는 전자정부 표준 `EgovMap`
*   **Mapper XML**: `*SQL.xml` 또는 `*_SQL.xml` (예: `System_SQL.xml`, `EgovSample_Sample_SQL.xml`)

---

## 3. 계층별 구현 규칙

### 3.1. Controller (Web Layer)
*   `@Controller` 어노테이션을 사용합니다.
*   요청 매핑은 `@GetMapping`, `@PostMapping`, `@RequestMapping` 등 목적에 맞게 명확히 부여합니다.
*   **응답 처리**:
    *   **HTML 화면 반환**: Thymeleaf 뷰 경로 문자열 반환 (예: `return "system/menuList";`)
    *   **AJAX / JSON 응답**: Spring ModelMap에 데이터를 등록하고 `return "jsonView";`를 반환하거나 `@ResponseBody` 활용
*   입력 데이터 검증은 `@Valid` 및 `BindingResult`를 우선 활용합니다.
*   로깅은 Lombok의 `@Slf4j` 사용을 원칙으로 합니다.

### 3.2. Service (Business Layer)
*   Service 구현체는 `EgovAbstractServiceImpl`을 상속받아야 합니다.
*   `@Service("beanName")` 형식으로 Bean 이름을 명시적으로 지정합니다. (예: `@Service("systemService")`)
*   트랜잭션 처리가 필요한 비즈니스 로직은 적절한 예외 처리 및 트랜잭션 설정을 준수합니다.
*   비즈니스 예외 발생 시 `processException("message.key")`를 활용하여 표준 메시지를 상원 처리합니다.

### 3.3. Mapper (Data Access Layer)
*   MyBatis를 사용하며, Mapper 인터페이스에 `@EgovMapper("mapperName")`을 부여합니다.
*   Mapper XML의 `namespace`는 해당 Mapper 인터페이스의 Full Package Name과 정확히 일치해야 합니다.
*   파라미터 및 반환 타입으로 구조화된 VO 또는 전자정부 표준 `EgovMap` (카멜케이스 자동 변환)을 활용합니다.
*   동적 SQL 사용 시 MyBatis 태그(`<if>`, `<choose>`, `<where>`)를 적극 활용합니다.

---

## 4. 데이터 및 검증 (VO, EgovMap & Validation)
*   VO 클래스는 `Serializable`을 구현해야 합니다.
*   단순/가변 조회 결과는 `EgovMap` 사용을 허용하며, 명확한 도메인 모델이 존재하는 경우 VO를 활용합니다.
*   선언적 검증 시 전자정부/JSR-303 검증 어노테이션을 적용합니다.

---

## 5. 보안 및 공통 유틸리티 (Security & Utility)
*   **System.out.println 사용 금지**: 모든 로그 출력은 반드시 `@Slf4j` 로거를 사용합니다.
*   **XSS 및 보안**: 사용자가 입력한 데이터는 보안 취약점 방지를 위해 공통 유틸리티(`CommUtil` 등)를 통해 정형화 및 필터링 처리를 수행합니다.
*   **개인정보 처리**: 개인정보 조회/조작 시 `PrivacyLogUtil` 등 프로젝트 내 정의된 공통 로깅 유틸을 적용합니다.

---

## 6. 설정 및 환경 관리 (Configuration)
*   Java Config 클래스는 `arkive.com.config` 또는 `egovframework.example.config` 패키지 하위에서 관리합니다.
*   환경 변수 및 프로젝트 설정값은 `application.properties`에 정의하고, `@Value` 또는 `EgovPropertyService` / `Environment`를 이용해 참조합니다.

---

## 7. 프론트엔드 (UI Layer)
*   **Template Engine**: Thymeleaf를 사용합니다.
*   **Layout**: `Thymeleaf Layout Dialect`를 사용하여 `header`, `footer`, `left` 메뉴 등을 조립합니다. (`layout/layout.html` 참조)
*   **Static Resources**: `/css`, `/js`, `/img`, `/scss` 경로를 사용하여 정적 자원을 관리합니다.

---

## 8. AI 작업 진행 규칙 (Workflow Rules)
    **분량 분할**: 코드를 한 번에 너무 크게 작성하지 말고, [VO/EgovMap -> Mapper/XML -> Service -> Controller -> UI] 순서대로 단계별로 작성 및 확인을 거친다.
    **기존 파일 수정**: 기존 코드를 수정할 때는 주석이나 공통 컴포넌트의 흐름을 깨뜨리지 않는다.
    **설명 방식**: 코드 생성 후 적용된 전자정부프레임워크 및 Arkive 프로젝트 표준 규칙에 대해 간략히 설명한다.

---
*최종 수정일: 2026-08-02*
*작성자: Zoo (AI Software Engineer)*
