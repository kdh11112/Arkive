# 동적 왼쪽 메뉴 (LNB) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DB(`MENU` 테이블)에 등록된 메뉴를 기반으로 좌측 사이드바(LNB) 메뉴가 동적으로 렌더링되도록 구현합니다.

**Architecture:** `@ControllerAdvice`를 활용해 모든 컨트롤러 요청 시 공통으로 `systemService.selectMenuList`를 호출해 `menuList`를 모델에 담고, [`left.html`](src/main/resources/templates/thymeleaf/layout/left.html:1)에서 이를 순회하여 동적 메뉴를 구성합니다.

**Tech Stack:** Spring Boot, MyBatis, Thymeleaf, Bootstrap / SB Admin 2

---

## Task 1: 글로벌 모델에 메뉴 리스트 추가 (`GlobalControllerAdvice`)

**Files:**
- Create: [`src/main/java/arkive/admin/system/web/GlobalControllerAdvice.java`](src/main/java/arkive/admin/system/web/GlobalControllerAdvice.java)
- Test: 빌드 및 기동 테스트

**Interfaces:**
- Consumes: [`SystemService`](src/main/java/arkive/admin/system/service/SystemService.java:8)
- Produces: 모든 컨트롤러의 모델에 `menuList` 속성 추가

- [ ] **Step 1: `GlobalControllerAdvice.java` 작성**

```java
package arkive.admin.system.web;

import java.util.List;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import arkive.admin.system.service.SystemService;
import jakarta.annotation.Resource;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Resource(name = "systemService")
    private SystemService systemService;

    @ModelAttribute
    public void addGlobalAttributes(ModelMap model) throws Exception {
        EgovMap egovMap = new EgovMap();
        List<EgovMap> menuList = systemService.selectMenuList(egovMap);
        model.addAttribute("menuList", menuList);
    }
}
```

- [ ] **Step 2: 컴파일 및 빌드 확인**

Run: `mvn compile`
Expected: SUCCESS

---

## Task 2: 동적 왼쪽 메뉴 템플릿 구현 (`left.html`)

**Files:**
- Modify: [`src/main/resources/templates/thymeleaf/layout/left.html`](src/main/resources/templates/thymeleaf/layout/left.html:33)

**Interfaces:**
- Consumes: `menuList` (Model attribute from GlobalControllerAdvice)

- [ ] **Step 1: [`left.html`](src/main/resources/templates/thymeleaf/layout/left.html:33)의 주석을 해제하고 Thymeleaf 동적 렌더링 구현**

상위 메뉴(`grad == 1`)와 하위 메뉴(`upMenuId` 매칭)를 동적으로 출력하도록 수정합니다.

- [ ] **Step 2: 애플리케이션 실행 및 UI 검증**
