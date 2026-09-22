# Design Specification: Reorganize Upload HTML Templates into comm Folder

## Overview
Reorganize file upload related Thymeleaf HTML templates (`multipartUpload.html`, `otherUpload.html`, `tusPopup.html`, `tusUpload.html`) from `src/main/resources/templates/thymeleaf/system/` to `src/main/resources/templates/thymeleaf/comm/`, aligning with common utility/upload backend components (`comm`).

## Proposed Changes

### 1. Template File Movement
Move the following files from `src/main/resources/templates/thymeleaf/system/` to `src/main/resources/templates/thymeleaf/comm/`:
- `multipartUpload.html`
- `otherUpload.html`
- `tusPopup.html`
- `tusUpload.html`

### 2. Controller Update (`FileController.java`)
Update view return paths in `arkive.admin.comm.web.FileController`:
- `return "system/tusUpload";` -> `return "comm/tusUpload";`
- `return "system/tusPopup";` -> `return "comm/tusPopup";`
- `return "system/multipartUpload";` -> `return "comm/multipartUpload";`
- `return "system/otherUpload";` -> `return "comm/otherUpload";`

### 3. References and Links
Verify navigation links (e.g., in `left.html`) point correctly to the endpoints (`/system/multipartUpload`, etc.). Since endpoints remain `/system/...` (or can be mapped accordingly), routing remains seamless while template rendering resolves from `comm/`.
