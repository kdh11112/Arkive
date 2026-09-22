# 멀티파트 파일 업로드 및 DB 저장 설계서

## 1. 개요
기존 `FileService` 및 `FileMapper`를 활용하여 멀티파트 파일 업로드 시 파일 메타데이터(원본 파일명, 저장 파일명, 파일 경로, 파일 사이즈, 확장자 등)는 데이터베이스(DB)에 저장하고, 실제 파일 바이너리는 로컬 디스크 스토리지에 저장하는 기능을 구현합니다.

## 2. 아키텍처 및 컴포넌트
- **Controller**: `/comm/multipartUpload` 또는 `/system/multipartUpload` 요청 처리 및 파일 업로드 파라미터 처리 (`FileController` 또는 `SystemController`)
- **Service**: `FileService` (`FileServiceImpl`)의 `uploadFileInsert` 또는 파일 업로드/저장 관련 메서드 활용
- **Mapper / XML**: `FileMapper` 및 관련 SQL (`File_SQL.xml` 등)을 통한 파일 그룹 및 상세 정보 DB 저장
- **Storage**: `Globals.FILE_TEMP_PATH` 또는 `Globals.FILE_REAL_PATH` 경로에 실제 파일 저장
- **UI (Thymeleaf)**: `src/main/resources/templates/thymeleaf/comm/multipartUpload.html` 화면 구성 및 업로드 결과 조회

## 3. 데이터 흐름 (Data Flow)
1. 사용자가 웹 화면(`multipartUpload.html`)에서 파일을 선택 후 업로드 요청 전송
2. Controller에서 `MultipartHttpServletRequest` 또는 `MultipartFile` 수신
3. `FileService`를 호출하여:
   - 서버 로컬 디스크 경로에 파일 물리적 저장
   - 파일 메타데이터(File ID, File Group ID, File Name, File Path, File Size 등)를 DB에 INSERT
4. 업로드 성공/실패 여부 및 파일 정보 모델 반환 후 화면에 결과 표시

## 4. 에러 및 예외 처리
- 파일 크기 초과, 허용되지 않는 확장자 업로드 시 예외 처리 (`EgovBizException` 또는 `IOException`)
- 업로드 실패 시 로컬에 저장된 임시 파일 정리(Rollback)

## 5. 테스트 계획
- 단일/다중 파일 업로드 테스트
- DB에 파일 메타데이터 저장 여부 확인 (`COMTNFILE`, `COMTNFILEDETAIL` 등)
- 로컬 스토리지 파일 생성 확인
