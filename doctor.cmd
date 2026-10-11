@echo off
rem PORTABLE: copy this file as-is to the target project root.
rem Checks whether the shared testing setup is applied in this project.
rem Usage: doctor.cmd  (run from repo root; exit 1 if anything is missing)
setlocal
cd /d "%~dp0"
set FAIL=0

call :checkFile "verify.cmd" "verify script"
rem 테스트 DB 격리: Boot는 properties 오버라이드, 클래식은 테스트용 XML 중 하나만 있으면 된다.
set DBISO=0
if exist "src\test\resources\application-test.properties" set DBISO=1
if exist "src\test\resources\context-test-datasource.xml" set DBISO=1
if %DBISO%==1 (echo [APPLIED] test DB isolation) else (echo [MISSING] test DB isolation ^(application-test.properties or context-test-datasource.xml^) & set FAIL=1)
call :checkFile "docs\ai\portable\portable-regression-checklist.md" "regression checklist"
rem 이식 가이드는 Boot·클래식 중 하나만 있으면 된다.
set GUIDE=0
if exist "docs\ai\boot\test-stack-boot.md" set GUIDE=1
if exist "docs\ai\classic\test-stack-classic.md" set GUIDE=1
if %GUIDE%==1 (echo [APPLIED] porting guide ^(boot/classic^)) else (echo [MISSING] porting guide ^(docs\ai\boot or docs\ai\classic^) & set FAIL=1)
call :checkContent "pom.xml" "jacoco-maven-plugin" "JaCoCo plugin in pom"
rem 테스트 starter: Boot는 starter-test, 클래식은 spring-test 중 하나만 있으면 된다.
set STARTER=0
findstr /c:"spring-boot-starter-test" "pom.xml" >nul 2>&1 && set STARTER=1
findstr /c:"spring-test" "pom.xml" >nul 2>&1 && set STARTER=1
if %STARTER%==1 (echo [APPLIED] test starter in pom) else (echo [MISSING] test starter in pom & set FAIL=1)
call :checkContent "AGENTS.md" "Quality Gates" "quality gates in AGENTS.md"

for /f %%f in ('dir /s /b src\test\*.java 2^>nul') do goto :hasTests
echo [MISSING] test sources under src\test
set FAIL=1
goto :doneTests
:hasTests
echo [APPLIED] test sources under src\test
:doneTests

if %FAIL%==1 (
  echo RESULT: INCOMPLETE. Apply the missing items, then rerun.
  exit /b 1
)
echo RESULT: APPLIED. All shared testing items present.
exit /b 0

:checkFile
if exist "%~1" (echo [APPLIED] %~2) else (echo [MISSING] %~2 & set FAIL=1)
exit /b 0

:checkContent
findstr /c:"%~2" "%~1" >nul 2>&1
if errorlevel 1 (echo [MISSING] %~3 & set FAIL=1) else (echo [APPLIED] %~3)
exit /b 0
