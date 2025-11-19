@echo off
:: ================================================================
:: MySQL Database Initialization Script (Windows)
:: - Ensures mysql client available
:: - Creates database if missing
:: - Processes sql\base\*.sql then sql\patches\**\*.sql
:: - For each file: computes SHA256, consults schema_patch_registry
::     * If not present -> executes file, records 'applied'
::     * If present AND checksum same -> skip
::     * If present AND checksum different -> abort (force new filename)
:: - Reads bundle version from env BUNDLE_VERSION or from BUNDLE_VERSION file
:: ================================================================
:: Usage:
::   %~n0 <auth_mode> [show]
::     <auth_mode> : ssl | password
::     [show]      : optional, echo on
:: Env (DB + version):
::   MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASS / DB_NAME
::   BUNDLE_VERSION (else reads .\BUNDLE_VERSION)
::   APPLIED_BY (default: %USERNAME%)
:: ================================================================

:: --- Basic Connection Settings (overridable via env) ---
if not defined MYSQL_USER   set MYSQL_USER=root
if not defined MYSQL_PASS   set MYSQL_PASS=
if not defined MYSQL_HOST   set MYSQL_HOST=127.0.0.1
if not defined MYSQL_PORT   set MYSQL_PORT=3306
if not defined DB_NAME      set DB_NAME=soukconect

:: --- SSL Certificate Paths ---
set SSL_CA=C:\mysql_certs\ca.pem
set SSL_CERT=C:\mysql_certs\client-cert.pem
set SSL_KEY=C:\mysql_certs\client-key.pem

:: --- Script Paths ---
set SCRIPT_PATH=%~dp0
set SQL_ROOT=%SCRIPT_PATH%sql\

:: --- Parse Arguments ---
if "%~1"=="" (
    echo Usage:
    echo   %~n0 ssl        ^| Use SSL certificate authentication
    echo   %~n0 password   ^| Use username/password authentication
    echo   Optional 2nd argument: show  ^| Enable verbose command output
    exit /b 1
)
set MODE=%~1
set VERBOSE=%~2

if /I "%VERBOSE%"=="show" (
    echo Verbose mode enabled - showing all commands.
    echo on
) else (
    @echo off
)

:: ===========================================================
::  STEP 1: Locate MySQL client
:: ===========================================================
where mysql >nul 2>nul
if %errorlevel%==0 (
    set MYSQL_CMD=mysql
) else (
    if defined MYSQL_HOME (
        if exist "%MYSQL_HOME%\bin\mysql.exe" (
            set "MYSQL_CMD=%MYSQL_HOME%\bin\mysql.exe"
        ) else (
            echo ERROR: MYSQL_HOME is set to "%MYSQL_HOME%" but mysql.exe not found in "bin" folder.
            echo Please check your MYSQL_HOME environment variable.
            pause
            exit /b 1
        )
    ) else (
        echo ERROR: MySQL command not found in PATH and MYSQL_HOME is not set or invalid.
        echo Please either:
        echo    1. Add MySQL's bin folder to your PATH, or
        echo    2. Set MYSQL_HOME to your MySQL installation directory for this session.
        echo Example:
        echo    set MYSQL_HOME="C:\Program Files\MySQL\MySQL Server 8.0"
        pause
        exit /b 1
    )
)

echo Found MySQL client: %MYSQL_CMD%

:: ===========================================================
::  STEP 2: Determine Authentication Mode
:: ===========================================================
if /I "%MODE%"=="ssl" (
    set AUTH_MODE=SSL
    set AUTH_ARGS=-u %MYSQL_USER% --ssl-mode=VERIFY_CA --ssl-ca="%SSL_CA%" --ssl-cert="%SSL_CERT%" --ssl-key="%SSL_KEY%"
) else if /I "%MODE%"=="password" (
    set AUTH_MODE=PASSWORD
    if "%MYSQL_PASS%"=="" (
        set AUTH_ARGS=-u %MYSQL_USER%
    ) else (
        set AUTH_ARGS=-u %MYSQL_USER% -p%MYSQL_PASS%
    )
) else (
    echo ERROR: Invalid option "%MODE%".
    echo Please specify "ssl" or "password".
    exit /b 1
)

echo ===========================================
echo Initializing MySQL database: %DB_NAME%
echo Authentication mode: %AUTH_MODE%
echo Verbose mode: %VERBOSE%
echo Using MySQL client: %MYSQL_CMD%
echo SQL root: %SQL_ROOT%
echo ===========================================

:: Helper macro to run command and check for errors
setlocal enabledelayedexpansion
set EXITCODE=0
set "run_mysql=call :run_mysql_sub"

:: ===========================================================
::  STEP 3: Create Database
:: ===========================================================

echo Creating database (if missing)...
echo ---------------------------------------
echo Executing SQL: CREATE DATABASE IF NOT EXISTS `%DB_NAME%`
echo ---------------------------------------
%run_mysql% %MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -e "CREATE DATABASE IF NOT EXISTS `%DB_NAME%` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

:: ===========================================================
::  STEP 4a: Determine bundle version
:: ===========================================================
if not defined BUNDLE_VERSION (
  if exist "%SCRIPT_PATH%BUNDLE_VERSION" (
    set /p BUNDLE_VERSION=<"%SCRIPT_PATH%BUNDLE_VERSION"
  ) else (
    set BUNDLE_VERSION=unversioned
  )
)
if not defined APPLIED_BY set APPLIED_BY=%USERNAME%

:: ===========================================================
::  STEP 4b: Registry created by base/0000_schema_patch_registry.sql
:: ===========================================================

:: ===========================================================
::  STEP 5: Process base then patches
:: ===========================================================
if not exist "%SQL_ROOT%" (
  echo ERROR: SQL root not found: %SQL_ROOT%
  exit /b 1
)

echo Processing base SQL from %SQL_ROOT%base\
if exist "%SQL_ROOT%base\" (
  for /f "delims=" %%F in ('dir /b /on "%SQL_ROOT%base\*.sql" 2^>nul') do (
    call :process_sql "%SQL_ROOT%base\%%~nxF"
    if %EXITCODE% neq 0 exit /b %EXITCODE%
  )
) else (
  echo (no base directory; skipping)
)

echo Processing patches (recursive) from %SQL_ROOT%patches\
if exist "%SQL_ROOT%patches\" (
  for /f "delims=" %%F in ('dir /b /s /on "%SQL_ROOT%patches\*.sql" 2^>nul') do (
    call :process_sql "%%~fF"
    if %EXITCODE% neq 0 exit /b %EXITCODE%
  )
) else (
  echo (no patches directory; skipping)
)

:: ===========================================================
::  STEP 7: Verify SSL (if applicable)
:: ===========================================================
if /I "%MODE%"=="ssl" (
    echo Verifying SSL connection...
    %run_mysql% %MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -e "SHOW SESSION STATUS LIKE 'Ssl_version';"
    if %EXITCODE% neq 0 exit /b %EXITCODE%
)

echo ===========================================
echo Database setup completed successfully!
echo ===========================================
pause
exit /b 0


:: ===========================================================
::  Subroutine: run_mysql_sub
:: ===========================================================
:run_mysql_sub
setlocal
cmd /c %*
set CODE=%errorlevel%
if %CODE% neq 0 (
    echo ERROR: Command failed with exit code %CODE%
    endlocal & set EXITCODE=%CODE%
    exit /b %CODE%
)
endlocal & set EXITCODE=0
exit /b 0
:: ===========================================================
::  Subroutine: run_mysql_file (executes a .sql file)
:: ===========================================================
:run_mysql_file
setlocal
set "SQLFILE=%~1"
"%MYSQL_CMD%" %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% %DB_NAME% < "%SQLFILE%"
set CODE=%errorlevel%
endlocal & exit /b %CODE%

:: ===========================================================
::  Subroutine: sha256 (uses PowerShell)
:: ===========================================================
:sha256
setlocal
set "_in=%~1"
for /f "usebackq tokens=*" %%H in (`powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%_in%').Hash"`) do set "_sum=%%H"
endlocal & set "_SUM=%_sum%"
exit /b 0

:: ===========================================================
::  Subroutine: process_sql (checksum + registry + execute)
:: ===========================================================
:process_sql
setlocal EnableDelayedExpansion
set "FULL=%~1"
set "REL=%FULL:%SQL_ROOT%=%"
call :sha256 "%FULL%"
set "SUM=%_SUM%"

for /f "usebackq tokens=*" %%E in (`"%MYSQL_CMD%" %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -N -B %DB_NAME% -e "SELECT checksum_sha256 FROM schema_patch_registry WHERE script_name='!REL!' LIMIT 1;"`) do set "EXIST=%%E"

if defined EXIST (
  if /I "!EXIST!"=="!SUM!" (
    echo [skip] !REL! already installed; checksum match, ignoring
    endlocal & set EXITCODE=0 & exit /b 0
  ) else (
    echo [ABORT] !REL! already applied with different checksum (!EXIST! ^!= !SUM!)
    endlocal & set EXITCODE=1 & exit /b 1
  )
)

echo Executing !REL! ...
call :run_mysql_file "%FULL%"
set CODE=%errorlevel%
if not "%CODE%"=="0" (
  echo [fail] !REL! failed to apply
  endlocal & set EXITCODE=%CODE% & exit /b %CODE%
)
"%MYSQL_CMD%" %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% %DB_NAME% -e "INSERT INTO schema_patch_registry(script_name,checksum_sha256,bundle_version,applied_by,status) VALUES ('!REL!','!SUM!','%BUNDLE_VERSION%','%APPLIED_BY%','applied');"
endlocal & set EXITCODE=0 & exit /b 0
