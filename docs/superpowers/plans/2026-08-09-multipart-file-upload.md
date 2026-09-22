# Multipart File Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a robust single-stage Multipart File Upload feature storing file metadata in the `ATCH_FILE` DB table and saving physical files to C: drive (`C:/upload/arkive/`).

**Architecture:** Spring Boot MVC Controller (`FileController`), Service (`FileService`/`FileServiceImpl`), MyBatis Mapper (`FileMapper`/`File_SQL.xml`), H2/RDBMS Table (`ATCH_FILE`), and Bootstrap HTML view (`multipartUpload.html`).

**Tech Stack:** Java 17, Spring Boot 3.x, eGovFrame 5.0, MyBatis, Thymeleaf, H2 DB / SQL, Bootstrap 4/5.

## Global Naming & Technical Constraints

- Storage Directory: `C:/upload/arkive/{yyyy}/{MM}/` (configurable via `Globals.FILE_REAL_PATH` in `application.properties`)
- Table Name: `ATCH_FILE`
- Naming Rules:
  - **Select (Read)** queries/methods: Prefixed with `get...` (e.g. `getFileInfo`, `getFileInfoList`)
  - **Insert / Update / Delete (Write)** queries/methods: Prefixed with `set...` (e.g. `setInsertAtchFile`, `setDeleteAtchFile`)
- Character Encoding: UTF-8 for Korean filename download headers

---

### Task 1: Create Database Schema DDL for `ATCH_FILE`

**Files:**
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\resources\db\system.sql`

- [ ] **Step 1: Append DDL for ATCH_FILE table to system.sql**

```sql
DROP TABLE IF EXISTS ATCH_FILE;
CREATE TABLE ATCH_FILE (
    FILE_ID VARCHAR(50) NOT NULL,
    ATCH_FILE_GRPID VARCHAR(50) NOT NULL,
    ORGNL_FILE_NM VARCHAR(255) NOT NULL,
    PHYS_FILE_NM VARCHAR(255) NOT NULL,
    FILE_PATH VARCHAR(500) NOT NULL,
    FILE_SZ BIGINT NOT NULL,
    FILE_EXT VARCHAR(20) NULL,
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,
    REG_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    RGTR_ID VARCHAR(50) NULL,
    CONSTRAINT PK_ATCH_FILE PRIMARY KEY (FILE_ID)
);
CREATE INDEX IDX_ATCH_FILE_GRPID ON ATCH_FILE(ATCH_FILE_GRPID);
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/resources/db/system.sql
git commit -m "schema: add ATCH_FILE table DDL to system.sql"
```

---

### Task 2: Configure Application Storage Properties

**Files:**
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\resources\application.properties`

- [ ] **Step 1: Update file upload properties in application.properties**

Set default paths:
```properties
Globals.FILE_TEMP_PATH = C:/upload/arkive/temp
Globals.FILE_REAL_PATH = C:/upload/arkive
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/resources/application.properties
git commit -m "config: update file upload storage paths for C drive"
```

---

### Task 3: Implement MyBatis Mapper SQL XML (`File_SQL.xml`) & Interface (`FileMapper.java`)

**Files:**
- Create: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\resources\egovframework\sqlmap\mappers\comm\File_SQL.xml`
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\java\arkive\admin\comm\service\impl\FileMapper.java`

- [ ] **Step 1: Create `File_SQL.xml` using `get` for selects and `set` for CUD**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="arkive.admin.comm.service.impl.FileMapper">

    <insert id="setInsertAtchFile">
        INSERT INTO ATCH_FILE (
            FILE_ID,
            ATCH_FILE_GRPID,
            ORGNL_FILE_NM,
            PHYS_FILE_NM,
            FILE_PATH,
            FILE_SZ,
            FILE_EXT,
            USE_YN,
            REG_DT,
            RGTR_ID
        ) VALUES (
            #{fileId},
            #{atchFileGrpid},
            #{orgnlFileNm},
            #{physFileNm},
            #{filePath},
            #{fileSz},
            #{fileExt},
            'Y',
            CURRENT_TIMESTAMP,
            #{userId}
        )
    </insert>

    <select id="getFileInfo" resultType="egovMap">
        SELECT
            FILE_ID,
            ATCH_FILE_GRPID,
            ORGNL_FILE_NM,
            PHYS_FILE_NM,
            FILE_PATH,
            FILE_SZ,
            FILE_EXT,
            USE_YN,
            REG_DT,
            RGTR_ID
        FROM ATCH_FILE
        WHERE FILE_ID = #{fileId}
          AND USE_YN = 'Y'
    </select>

    <select id="getFileInfoList" resultType="egovMap">
        SELECT
            FILE_ID,
            ATCH_FILE_GRPID,
            ORGNL_FILE_NM,
            PHYS_FILE_NM,
            FILE_PATH,
            FILE_SZ,
            FILE_EXT,
            USE_YN,
            REG_DT,
            RGTR_ID
        FROM ATCH_FILE
        WHERE ATCH_FILE_GRPID = #{atchFileGrpid}
          AND USE_YN = 'Y'
        ORDER BY REG_DT ASC
    </select>

    <update id="setDeleteAtchFile">
        UPDATE ATCH_FILE
        SET USE_YN = 'N'
        WHERE FILE_ID = #{fileId}
    </update>

</mapper>
```

- [ ] **Step 2: Update `FileMapper.java` with corresponding method definitions**

```java
package arkive.admin.comm.service.impl;

import java.util.List;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

@EgovMapper("fileMapper")
public interface FileMapper {
    void setInsertAtchFile(EgovMap param);
    EgovMap getFileInfo(String fileId);
    List<EgovMap> getFileInfoList(String atchFileGrpid);
    void setDeleteAtchFile(String fileId);
}
```

- [ ] **Step 3: Commit changes**

```bash
git add src/main/resources/egovframework/sqlmap/mappers/comm/File_SQL.xml src/main/java/arkive/admin/comm/service/impl/FileMapper.java
git commit -m "feat: add File_SQL.xml mapper and update FileMapper interface with get/set naming convention"
```

---

### Task 4: Refactor `FileService` & `FileServiceImpl`

**Files:**
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\java\arkive\admin\comm\service\FileService.java`
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\java\arkive\admin\comm\service\impl\FileServiceImpl.java`

- [ ] **Step 1: Define FileService interface methods following get/set rules**

Methods:
- `List<EgovMap> setUploadFiles(List<MultipartFile> files, String atchFileGrpid, String userId)`
- `EgovMap getFileInfo(String fileId)`
- `List<EgovMap> getFileInfoList(String atchFileGrpid)`
- `boolean setDeleteFile(String fileId)`

- [ ] **Step 2: Implement file upload, physical save on C: drive, and DB saving in `FileServiceImpl`**

File save path logic:
`String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));`
`File saveDir = new File(basePath, dateDir); if (!saveDir.exists()) saveDir.mkdirs();`
`String fileId = UUID.randomUUID().toString();`
`String physFileNm = fileId + "." + ext;`
Save file with `multipartFile.transferTo(destFile)`.
Insert into `FileMapper.setInsertAtchFile`.

- [ ] **Step 3: Commit changes**

```bash
git add src/main/java/arkive/admin/comm/service/FileService.java src/main/java/arkive/admin/comm/service/impl/FileServiceImpl.java
git commit -m "feat: implement single-stage file upload service using get/set naming rule"
```

---

### Task 5: Implement `FileController` Upload/Download/Delete Endpoints

**Files:**
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\java\arkive\admin\comm\web\FileController.java`

- [ ] **Step 1: Add `/file/upload.json`, `/file/list.json`, `/file/download/{fileId}`, `/file/delete.json` endpoints**

Endpoints:
1. `@PostMapping("/file/upload.json")`: Calls `setUploadFiles` and returns JSON with list of uploaded file maps.
2. `@GetMapping("/file/list.json")`: Calls `getFileInfoList` and returns JSON list of files for a given `atchFileGrpid`.
3. `@GetMapping("/file/download/{fileId}")`: Stream binary file contents with UTF-8 filename in `Content-Disposition`.
4. `@PostMapping("/file/delete.json")`: Calls `setDeleteFile` by `fileId`.

- [ ] **Step 2: Commit changes**

```bash
git add src/main/java/arkive/admin/comm/web/FileController.java
git commit -m "feat: add multipart file upload, download, and delete endpoints"
```

---

### Task 6: Enhance Frontend Upload Interface (`multipartUpload.html`)

**Files:**
- Modify: `c:\Users\kdh\Documents\workspace-egov\Arkive\src\main\resources\templates\thymeleaf\comm\multipartUpload.html`

- [ ] **Step 1: Upgrade `multipartUpload.html` with Bootstrap UI and AJAX upload/download/delete interactions**

Features:
- File selection input (multiple files supported).
- Upload progress / status alerts.
- Dynamic table rendering list of uploaded files (`파일명`, `크기`, `등록일시`, `다운로드`, `삭제`).
- Immediate download trigger & deletion with UI table auto-refresh.

- [ ] **Step 2: Commit changes**

```bash
git add src/main/resources/templates/thymeleaf/comm/multipartUpload.html
git commit -m "feat: upgrade multipartUpload.html with file upload, download, and delete UI"
```
