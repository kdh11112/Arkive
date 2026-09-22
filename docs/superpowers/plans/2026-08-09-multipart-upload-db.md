# Multipart File Upload with DB Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement multipart file upload that saves file metadata to DB and actual file to local disk using existing `FileService` and `FileMapper`.

**Architecture:** Extend controller / service methods to handle multipart uploads, saving file metadata to DB via `FileMapper` and storing physical files on local disk via `FileService`.

**Tech Stack:** Spring Boot, MyBatis, Thymeleaf, eGovFrame.

## Global Constraints
- Follow existing `FileService` and `FileMapper` patterns.
- Use camelCase in Java and HTML.
- Use SLF4J `@Slf4j` for logging.
- Store metadata in DB and physical files on local disk.

---

### Task 1: Controller & Service File Upload Extension

**Files:**
- Modify: `src/main/java/arkive/admin/comm/web/FileController.java`
- Modify: `src/main/java/arkive/admin/comm/service/FileService.java`
- Modify: `src/main/java/arkive/admin/comm/service/impl/FileServiceImpl.java`

**Interfaces:**
- Consumes: `FileService`, `FileMapper`
- Produces: Multipart upload request handling and DB insertion logic.

- [ ] **Step 1: Implement multipart upload handling in FileController / FileService**

Ensure `FileService` has methods to process multipart files (`MultipartFile` or `MultipartHttpServletRequest`), store them on local disk, and save records via `FileMapper`.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/arkive/admin/comm/web/FileController.java src/main/java/arkive/admin/comm/service/FileService.java src/main/java/arkive/admin/comm/service/impl/FileServiceImpl.java
git commit -m "feat: add multipart upload handling with DB metadata and local storage"
```
