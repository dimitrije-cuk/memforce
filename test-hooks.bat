@echo off
setlocal

:: Runs .githooks/commit-msg against sample messages. Run from the repository root.

set "HOOK=.githooks/commit-msg"
set "TMP_FILE=%TEMP%\commit-msg-test-%RANDOM%.txt"
set "OUT_FILE=%TEMP%\commit-msg-test-%RANDOM%.out"
set "FAILURES=0"

:: The hook is a shell script, so Git's bundled sh runs it.
set "SH=sh"
where sh >nul 2>&1
if not errorlevel 1 goto :run
set "SH=%ProgramFiles%\Git\bin\sh.exe"
if exist "%SH%" goto :run
echo ERROR: sh not found. Install Git for Windows or run test-hooks.sh from Git Bash.
exit /b 1

:run
set "MSG=feat: add login"
call :accept
set "MSG=fix(auth): handle expiry"
call :accept
set "MSG=feat(api)!: drop v1"
call :accept
set "MSG=feat!: drop v1"
call :accept
set "MSG=Merge branch 'main'"
call :accept
set "MSG=Revert "feat: add login""
call :accept
set "MSG=fixup! feat: add login"
call :accept

>"%TMP_FILE%" echo # Please enter the commit message.
>>"%TMP_FILE%" echo.
>>"%TMP_FILE%" echo feat: add login
set "LABEL=accept: comment and blank line before the message"
set "EXPECTED=0"
call :check

set "MSG=WIP"
call :reject
set "MSG=: no type"
call :reject
set "MSG=feat:"
call :reject
set "MSG=feat:    "
call :reject
set "MSG=banana: not a type"
call :reject

type nul >"%TMP_FILE%"
set "LABEL=reject: empty message"
set "EXPECTED=1"
call :check

del /q "%TMP_FILE%" "%OUT_FILE%" 2>nul

if not "%FAILURES%"=="0" goto :failed
echo All tests passed.
exit /b 0

:failed
echo %FAILURES% test(s) failed.
exit /b 1

:accept
>"%TMP_FILE%" echo %MSG%
set "LABEL=accept: %MSG%"
set "EXPECTED=0"
goto :check

:reject
>"%TMP_FILE%" echo %MSG%
set "LABEL=reject: %MSG%"
set "EXPECTED=1"
goto :check

:check
"%SH%" "%HOOK%" "%TMP_FILE%" >"%OUT_FILE%" 2>&1
set "STATUS=%ERRORLEVEL%"
if not "%STATUS%"=="%EXPECTED%" goto :fail_status
if not "%EXPECTED%"=="0" goto :pass
:: An accepted commit must stay silent.
for %%A in ("%OUT_FILE%") do set "SIZE=%%~zA"
if not "%SIZE%"=="0" goto :fail_output

:pass
echo ok    %LABEL%
exit /b 0

:fail_status
echo FAIL  %LABEL% (expected exit %EXPECTED%, got %STATUS%)
set /a FAILURES+=1
exit /b 0

:fail_output
echo FAIL  %LABEL% (expected no output)
type "%OUT_FILE%"
set /a FAILURES+=1
exit /b 0
