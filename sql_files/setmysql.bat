@echo off
:: ================================================================
:: MySQL Database Initialization Script (Auth + PATH + Verbose + Safe Abort + Empty Password Support)
:: ================================================================
:: Usage:
::   init_db_secure.bat <auth_mode> [show]
::     <auth_mode> : ssl | password
::     [show]      : optional, enables verbose output (echo commands)
::
:: Example:
::   init_db_secure.bat password
::   init_db_secure.bat ssl show
:: ================================================================

:: --- Basic Connection Settings ---
set MYSQL_USER=root
set MYSQL_PASS=
set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set DB_NAME=soukconect

:: --- SSL Certificate Paths ---
set SSL_CA=C:\mysql_certs\ca.pem
set SSL_CERT=C:\mysql_certs\client-cert.pem
set SSL_KEY=C:\mysql_certs\client-key.pem

:: --- Script Paths ---
set SCRIPT_PATH=%~dp0
set VENDOR_SQL=%SCRIPT_PATH%vendor.sql
set PRODUCTS_SQL=%SCRIPT_PATH%products.sql
set CUISINES_SQL=%SCRIPT_PATH%cusines.sql

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
    echo 🔍 Verbose mode enabled — showing all commands.
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
            echo ❌ ERROR: MYSQL_HOME is set to "%MYSQL_HOME%" but mysql.exe not found in "bin" folder.
            echo Please check your MYSQL_HOME environment variable.
            pause
            exit /b 1
        )
    ) else (
        echo ❌ ERROR: MySQL command not found in PATH and MYSQL_HOME is not set or invalid.
        echo Please either:
        echo    1. Add MySQL's bin folder to your PATH, or
        echo    2. Set MYSQL_HOME to your MySQL installation directory for this session.
        echo Example:
        echo    set MYSQL_HOME="C:\Program Files\MySQL\MySQL Server 8.0"
        pause
        exit /b 1
    )
)

echo ✅ Found MySQL client: %MYSQL_CMD%

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
    echo ❌ Invalid option "%MODE%".
    echo Please specify "ssl" or "password".
    exit /b 1
)

echo ===========================================
echo Initializing MySQL database: %DB_NAME%
echo Authentication mode: %AUTH_MODE%
echo Verbose mode: %VERBOSE%
echo Using MySQL client: %MYSQL_CMD%
echo ===========================================

:: Helper macro to run command and check for errors
setlocal enabledelayedexpansion
set EXITCODE=0
set "run_mysql=call :run_mysql_sub"

:: ===========================================================
::  STEP 3: Create Database
:: ===========================================================


echo Creating database...
set SQL_CMD=CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
echo ---------------------------------------
echo Executing SQL:
echo "%MYSQL_CMD%" %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -e %SQL_CMD%"
echo ---------------------------------------
%run_mysql% "%MYSQL_CMD%" %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -e %SQL_CMD%"
:: ===========================================================
::  STEP 4: Import vendor.sql
:: ===========================================================
echo Importing vendor.sql...
%run_mysql% "%MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% %DB_NAME% < \"%VENDOR_SQL%\""
if %EXITCODE% neq 0 exit /b %EXITCODE%

:: ===========================================================
::  STEP 5: Import products.sql
:: ===========================================================
echo Importing products.sql...
%run_mysql% "%MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% %DB_NAME% < \"%PRODUCTS_SQL%\""
if %EXITCODE% neq 0 exit /b %EXITCODE%

:: ===========================================================
::  STEP 6: Import cusines.sql
:: ===========================================================
echo Importing cusines.sql...
%run_mysql% "%MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% %DB_NAME% < \"%CUISINES_SQL%\""
if %EXITCODE% neq 0 exit /b %EXITCODE%

:: ===========================================================
::  STEP 7: Verify SSL (if applicable)
:: ===========================================================
if /I "%MODE%"=="ssl" (
    echo Verifying SSL connection...
    %run_mysql% "%MYSQL_CMD% %AUTH_ARGS% -h %MYSQL_HOST% -P %MYSQL_PORT% -e \"SHOW SESSION STATUS LIKE 'Ssl_version';\""
    if %EXITCODE% neq 0 exit /b %EXITCODE%
)

echo ===========================================
echo ✅ Database setup completed successfully!
echo ===========================================
pause
exit /b 0


:: ===========================================================
::  Subroutine: run_mysql_sub
:: ===========================================================
:run_mysql_sub
setlocal
set CMD=%~1
cmd /c "%CMD%"
set CODE=%errorlevel%
if %CODE% neq 0 (
    echo ❌ ERROR: Command failed with exit code %CODE%
    echo ❌ Failing command: %CMD%
    endlocal & set EXITCODE=%CODE%
    exit /b %CODE%
)
endlocal & set EXITCODE=0
exit /b 0