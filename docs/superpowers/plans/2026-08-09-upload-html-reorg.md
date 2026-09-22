# Upload HTML Templates Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize file upload related Thymeleaf HTML templates into `src/main/resources/templates/thymeleaf/comm/` folder and update controllers accordingly.

**Architecture:** Move upload HTML files (`multipartUpload.html`, `otherUpload.html`, `tusPopup.html`, `tusUpload.html`) from `system/` to `comm/`, and update `FileController.java` view return paths.

**Tech Stack:** Spring Boot, Thymeleaf, Java

## Global Constraints
- Preserve exact file contents when moving templates.
- Update view return paths in `FileController.java` from `system/...` to `comm/...`.

---

## Task 1: Create comm directory and move HTML template files

**Files:**
- Create: `src/main/resources/templates/thymeleaf/comm/multipartUpload.html`
- Create: `src/main/resources/templates/thymeleaf/comm/otherUpload.html`
- Create: `src/main/resources/templates/thymeleaf/comm/tusPopup.html`
- Create: `src/main/resources/templates/thymeleaf/comm/tusUpload.html`
- Delete: `src/main/resources/templates/thymeleaf/system/multipartUpload.html`
- Delete: `src/main/resources/templates/thymeleaf/system/otherUpload.html`
- Delete: `src/main/resources/templates/thymeleaf/system/tusPopup.html`
- Delete: `src/main/resources/templates/thymeleaf/system/tusUpload.html`

- [ ] **Step 1: Read content of existing upload HTML files from system/**
- [ ] **Step 2: Create files in comm/ with identical content**
- [ ] **Step 3: Delete original files from system/**

---

## Task 2: Update FileController view return paths

**Files:**
- Modify: `src/main/java/arkive/admin/comm/web/FileController.java:816-835`

- [ ] **Step 1: Update return paths in FileController.java from "system/..." to "comm/..."**
- [ ] **Step 2: Verify application compilation / build**
