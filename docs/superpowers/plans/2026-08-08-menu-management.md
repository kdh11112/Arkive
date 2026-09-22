# Menu Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement database-backed left navigation menu management with web UI controls for dynamic menu creation, updating, deletion, and rendering.

**Architecture:** Extend existing `SystemController`, `SystemServiceImpl`, `SystemMapper`, and `System_SQL.xml` to support CRUD operations on the `menu` table, and integrate dynamic menu loading in Thymeleaf layouts (`left.html` or controllers).

**Tech Stack:** Spring Boot, MyBatis, Thymeleaf, H2/PostgreSQL (DB), Bootstrap / SB Admin 2 / KRDS.

## Global Constraints
- Use exact package structures (`arkive.admin.system.*`).
- SQL statements must be compatible with the database schema (`menu` table).
- Follow existing project coding conventions.

---

### Task 1: Database Schema and Sample Data (`system.sql` / `data.sql`)

**Files:**
- Modify: `src/main/resources/db/system.sql`
- Modify: `src/main/resources/db/data.sql` (if applicable)

**Interfaces:**
- Consumes: `menu` table definition.
- Produces: Initial hierarchical menu data inserted into `menu` table.

- [ ] **Step 1: Update `src/main/resources/db/system.sql` with comprehensive menu sample data**
- [ ] **Step 2: Verify SQL execution against database initialization**

### Task 2: Service and Mapper Implementation for Menu CRUD

**Files:**
- Modify: `src/main/java/arkive/admin/system/service/impl/SystemMapper.java`
- Modify: `src/main/resources/egovframework/sqlmap/example/mappers/System_SQL.xml`
- Modify: `src/main/java/arkive/admin/system/service/SystemService.java`
- Modify: `src/main/java/arkive/admin/system/service/impl/SystemServiceImpl.java`

**Interfaces:**
- Consumes: `SystemMapper` methods (`selectMenuList`, `selectMenuDetailList`, `getChkMenuId`, `getChkUpMenuId`, `setInsertMenu`, `setUpdateMenu`, `setDeleteMenu`).
- Produces: Service methods for menu management.

- [ ] **Step 1: Ensure all menu CRUD methods are present in `SystemMapper.java` and `System_SQL.xml`**
- [ ] **Step 2: Implement service methods in `SystemService.java` and `SystemServiceImpl.java`**

### Task 3: Controller and Web UI Integration for Left Menu Management

**Files:**
- Modify: `src/main/java/arkive/admin/system/web/SystemController.java`
- Modify: `src/main/resources/templates/thymeleaf/layout/left.html`

**Interfaces:**
- Consumes: `SystemService` menu queries and mutations.
- Produces: Web endpoints for menu management and dynamic left menu rendering.

- [ ] **Step 1: Add menu management endpoints in `SystemController.java`**
- [ ] **Step 2: Update `left.html` to dynamically render menus from database**
- [ ] **Step 3: Build and verify application startup and menu functionality**
