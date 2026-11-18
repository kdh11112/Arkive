# verify.cmd 이식

> 이식 구분: 복붙 가능(원본 보관). 아래 전문으로 없으면 만든다.
> 정본은 루트의 `verify.cmd`다. 이 문서의 전문은 대상에 파일이 없을 때만 사용하고, 수정하면 양쪽을 함께 갱신한다.

## 없을 때 만드는 방법

1. 이 파일의 `verify.cmd` 전문을 대상 루트 `verify.cmd`로 저장한다 (개행 CRLF, UTF-8 BOM 없음).
2. [PORTING.md](../../../PORTING.md) 2절의 준비 항목(테스트 의존성, JaCoCo, 테스트 DB 분리)을 채운 뒤 대상 루트에서 `verify.cmd`로 검증한다.

## verify.cmd 전문

```cmd
@echo off
rem PORTABLE: copy this file as-is to the target project root.
rem Usage: verify.cmd [-Dtest=...]
rem Runs clean verify so JaCoCo report+check also execute.
setlocal
cd /d "%~dp0"

set "MVN=mvn"
if exist "mvnw.cmd" set "MVN=mvnw.cmd"

echo [1/1] %MVN% clean verify %*
call %MVN% clean verify %*
if errorlevel 1 goto :fail

if exist "target\site\jacoco\index.html" (
  echo JaCoCo report: %CD%\target\site\jacoco\index.html
) else (
  echo WARNING: JaCoCo report not found. Check plugin configuration.
)

echo Verification completed.
exit /b 0

:fail
echo Verification FAILED. Do not report the task as verified.
exit /b 1
```
