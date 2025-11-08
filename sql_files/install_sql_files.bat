@echo off
:: ================================================================
:: MySQL Database Initialization Script
:: (Auth + PATH + Verbose + Safe Abort + Empty Password Support)
:: ================================================================
:: Usage:
::   %~n0 <auth_mode> [show]
::     <auth_mode> : ssl | password
::     [show]      : optional, enables verbose output (echo on)
::
:: What it does:
::   - Ensures MySQL client is available
::   - Creates database if missing
::   - Imports ALL .sql files in this folder (sql_files) in sorted order
::
:: Examples:
::   %~n0 password
::   %~n0 ssl show
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
echo SQL directory: %SCRIPT_PATH% (detecting .sql location)
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
::  STEP 4: Import ALL .sql files in sorted order
:: ===========================================================
set "SQL_DIR=%SCRIPT_PATH%"
dir /b "%SQL_DIR%*.sql" >nul 2>nul
if errorlevel 1 (
    if exist "%SCRIPT_PATH%sql\" (
        set "SQL_DIR=%SCRIPT_PATH%sql\"
    )
)
dir /b "%SQL_DIR%*.sql" >nul 2>nul
if errorlevel 1 (
    echo ERROR: No .sql files found in "%SCRIPT_PATH%" or "%SCRIPT_PATH%sql\"
    pause
    exit /b 1
)

echo Importing .sql files from: %SQL_DIR%
for /f "delims=" %%F in ('dir /b /on "%SQL_DIR%*.sql"') do (
    echo ---------------------------------------
    echo Importing %%~nxF ...
    call :run_mysql_file "%SQL_DIR%%%~nxF"
    if errorlevel 1 (
        echo Warning: %%~nxF returned errorlevel !ERRORLEVEL! - continuing
    )
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
