# 동적 왼쪽 메뉴 (LNB) 렌더링 설계서

## 개요
DB(`MENU` 테이블)에 등록된 메뉴 리스트를 기반으로 애플리케이션의 좌측 사이드바(LNB) 메뉴가 동적으로 렌더링되도록 구현합니다.

## 아키텍처 및 구현 방식
1. **공통 데이터 공급 (`GlobalControllerAdvice`)**:
   - `@ControllerAdvice`를 사용하여 모든 컨트롤러 요청 시 공통으로 `systemService.selectMenuList(new EgovMap())`을 호출하여 `menuList`를 모델에 추가합니다.
2. **Thymeleaf 템플릿 수정 (`left.html`)**:
   - [`left.html`](src/main/resources/templates/thymeleaf/layout/left.html:1)에서 ${menuList}를 받아 `grad == 1`인 상위 메뉴와 해당 상위 메뉴의 `menuId`를 `upMenuId`로 가지는 하위 메뉴들을 계층적으로 렌더링합니다.
   - SB Admin 2 템플릿의 드롭다운/콜랩스(`collapse`) 구조에 맞게 동적 반복문(`th:each`)을 적용합니다.
