# Menu Management UI & CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement UI buttons and AJAX CRUD functionality for both upper menus (jstree) and lower menus (table) in `menuList.html`, using existing backend endpoints (`/system/setInsertMenu.json`, `/system/setUpdateMenu.json`, `/system/setDeleteMenu.json`).

**Architecture:** Enhance `src/main/resources/templates/thymeleaf/system/menuList.html` with Bootstrap buttons, modals/forms for Add/Edit, and JavaScript event handlers interacting with system JSON endpoints.

**Tech Stack:** Thymeleaf, Bootstrap 4, jQuery, jstree, Spring Boot / MyBatis JSON API.

## Global Constraints
- Use exact package structures and existing endpoint URLs (`/system/setInsertMenu.json`, `/system/setUpdateMenu.json`, `/system/setDeleteMenu.json`, etc.).
- Follow existing project coding conventions and Bootstrap UI patterns.

---

### Task 1: Add Upper & Lower Menu Action Buttons and Modals to `menuList.html`

**Files:**
- Modify: `src/main/resources/templates/thymeleaf/system/menuList.html`

**Interfaces:**
- Consumes: `/system/getMenuList.json`, `/system/getMenuDetailList.json`, `/system/setInsertMenu.json`, `/system/setUpdateMenu.json`, `/system/setDeleteMenu.json`.
- Produces: Complete menu management UI with Add, Modify, Delete buttons for both upper and lower menus.

- [ ] **Step 1: Add Action Buttons for Upper Menu (above tree) and Lower Menu (above table)**
- [ ] **Step 2: Add Bootstrap Modals for Menu Add/Edit (with fields for menuId, menuNm, menuCours, ordr, useYn, upMenuId, grad)**
- [ ] **Step 3: Implement JavaScript functions for Add, Edit, Delete of Upper and Lower menus**
- [ ] **Step 4: Test and verify menu CRUD operations**
