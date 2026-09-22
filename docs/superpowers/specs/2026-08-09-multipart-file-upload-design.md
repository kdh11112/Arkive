# Multipart File Upload Architecture & Spec

## Naming Naming Conventions
- **Select (Read)** methods & query IDs: Prefixed with `get...` (e.g., `getFileInfo`, `getFileInfoList`)
- **Insert / Update / Delete (Write)** methods & query IDs: Prefixed with `set...` (e.g., `setInsertAtchFile`, `setDeleteAtchFile`)

## Database Schema (`ATCH_FILE`)

```sql
DROP TABLE IF EXISTS ATCH_FILE;
CREATE TABLE ATCH_FILE (
    FILE_ID VARCHAR(50) NOT NULL,          -- 파일 고유 ID (UUID)
    ATCH_FILE_GRPID VARCHAR(50) NOT NULL,   -- 파일 그룹 ID
    ORGNL_FILE_NM VARCHAR(255) NOT NULL,   -- 원본 파일명
    PHYS_FILE_NM VARCHAR(255) NOT NULL,    -- 물리 저장 파일명 (UUID.확장자)
    FILE_PATH VARCHAR(500) NOT NULL,       -- 저장 경로 (예: C:/upload/arkive/2026/08/)
    FILE_SZ BIGINT NOT NULL,               -- 파일 크기 (Byte)
    FILE_EXT VARCHAR(20) NULL,             -- 파일 확장자
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,   -- 사용 여부
    REG_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 등록일시
    RGTR_ID VARCHAR(50) NULL,              -- 등록자 ID
    CONSTRAINT PK_ATCH_FILE PRIMARY KEY (FILE_ID)
);
CREATE INDEX IDX_ATCH_FILE_GRPID ON ATCH_FILE(ATCH_FILE_GRPID);
```

## Physical File Storage
- **Base Directory**: `C:/upload/arkive/` (configured in `application.properties` via `Globals.FILE_REAL_PATH`)
- **Subdirectory Structure**: `C:/upload/arkive/{yyyy}/{MM}/`
- **File Naming**: Physical filenames use unique UUID strings to prevent filename collisions and security vulnerabilities (e.g. `550e8400-e29b-41d4-a716-446655440000.png`).
- Directory creation is handled automatically at runtime if directories do not exist.

## Component & API Design

### 1. Spring Controller (`FileController.java`)
- `GET /system/multipartUpload`: Renders Thymeleaf view `comm/multipartUpload.html`.
- `POST /file/upload.json`: Accepts `MultipartFile[] files` and optional `atchFileGrpid`. Saves physical files, records metadata in `ATCH_FILE`, and returns JSON response with uploaded file metadata.
- `GET /file/list.json`: Fetches list of files by `atchFileGrpid`.
- `GET /file/download/{fileId}`: Streams file to browser with HTTP `Content-Disposition` header and UTF-8 filename encoding for correct Korean filename support.
- `POST /file/delete.json`: Deletes DB record and removes physical file from disk.

### 2. Service Layer (`FileService.java` & `FileServiceImpl.java`)
- `setUploadFiles(List<MultipartFile> files, String atchFileGrpid, String userId)`: Generates IDs, writes physical files to disk, builds `ATCH_FILE` records, and calls mapper `setInsertAtchFile`.
- `getFileInfo(String fileId)`: Fetches single file record.
- `getFileInfoList(String atchFileGrpid)`: Fetches file list by group.
- `setDeleteFile(String fileId)`: Deletes DB record (`setDeleteAtchFile`) and deletes file from disk safely.

### 3. Mapper Layer (`FileMapper.java` & `File_SQL.xml`)
- MyBatis XML mapper mapping SQL queries for `ATCH_FILE` table:
  - `setInsertAtchFile`: Inserts file record into `ATCH_FILE`.
  - `getFileInfo`: Selects single file record by `FILE_ID`.
  - `getFileInfoList`: Selects file list by `ATCH_FILE_GRPID`.
  - `setDeleteAtchFile`: Soft-deletes file record by `FILE_ID`.

### 4. UI Layer (`multipartUpload.html`)
- Clean interface in Thymeleaf (`templates/thymeleaf/comm/multipartUpload.html`).
- AJAX file upload via `FormData`.
- File selection, upload button, uploaded files table with download links and delete actions.

## Verification Plan
1. Start application or test MyBatis mapping with H2 database.
2. Execute DDL in `system.sql` to create `ATCH_FILE` table.
3. Test file upload via `/system/multipartUpload`: verify physical file creation under `C:/upload/arkive/yyyy/MM/` and DB insertion in `ATCH_FILE`.
4. Test file download: verify downloaded file matches original and filename is preserved.
5. Test file deletion: verify DB record and physical file are removed.
