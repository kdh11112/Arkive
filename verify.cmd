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
