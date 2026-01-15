@echo off
REM =============================================================================
REM Redis Installation and Initialization Script for SoukConect (Windows)
REM =============================================================================
REM
REM Purpose: Initialize Redis with configuration keys, feature flags, and
REM          data structures needed by the SoukConect application.
REM
REM All keys are prefixed with "soukconect:" for namespace isolation.
REM
REM Usage:
REM   install_redis.bat [environment]
REM
REM Arguments:
REM   environment - Optional: dev, staging, prod (default: dev)
REM
REM Environment Variables:
REM   REDIS_HOST     - Redis host (default: localhost)
REM   REDIS_PORT     - Redis port (default: 6379)
REM   REDIS_PASSWORD - Redis password (optional)
REM   REDIS_USERNAME - Redis username for ACL (optional, Redis 6+)
REM
REM Examples:
REM   install_redis.bat dev
REM   set REDIS_PASSWORD=mypassword && install_redis.bat staging
REM
REM =============================================================================

setlocal enabledelayedexpansion

REM Key prefix for namespace isolation
set KEY_PREFIX=soukconect

REM Default values
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev

if not defined REDIS_HOST set REDIS_HOST=localhost
if not defined REDIS_PORT set REDIS_PORT=6379
if not defined REDIS_PASSWORD set REDIS_PASSWORD=
if not defined REDIS_USERNAME set REDIS_USERNAME=

echo ========================================
echo Redis Installation for SoukConect
echo ========================================
echo.
echo Environment: %ENVIRONMENT%
echo Redis Host:  %REDIS_HOST%
echo Redis Port:  %REDIS_PORT%
echo Key Prefix:  %KEY_PREFIX%:*
echo.

REM Build redis-cli command
set REDIS_CLI=redis-cli -h %REDIS_HOST% -p %REDIS_PORT%

REM Add authentication if provided
if not "%REDIS_PASSWORD%"=="" (
    set REDIS_CLI=!REDIS_CLI! -a %REDIS_PASSWORD%
    if not "%REDIS_USERNAME%"=="" (
        set REDIS_CLI=!REDIS_CLI! --user %REDIS_USERNAME%
    )
)

REM Test connection
echo Testing Redis connection...
%REDIS_CLI% PING >nul 2>&1
if errorlevel 1 (
    echo ERROR: Cannot connect to Redis at %REDIS_HOST%:%REDIS_PORT%
    echo Please check that Redis is running and credentials are correct
    exit /b 1
)
echo [OK] Connected to Redis
echo.

REM Get Redis version
for /f "tokens=2 delims=:" %%v in ('%REDIS_CLI% INFO server ^| findstr redis_version') do set REDIS_VERSION=%%v
echo Redis Version: %REDIS_VERSION%
echo.

REM Initialize configuration keys
echo Initializing configuration keys...

REM Rate limit configurations
%REDIS_CLI% SET "%KEY_PREFIX%:config:rate_limit:login:max_attempts" "5" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:rate_limit:login:window_seconds" "300" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:rate_limit:api:max_requests" "1000" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:rate_limit:api:window_seconds" "60" >nul

REM Cache TTL configurations (in seconds)
%REDIS_CLI% SET "%KEY_PREFIX%:config:cache:user_profile:ttl" "3600" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:cache:vendor_details:ttl" "1800" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:cache:product_details:ttl" "1800" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:cache:category_list:ttl" "7200" >nul

REM Session configurations
%REDIS_CLI% SET "%KEY_PREFIX%:config:session:refresh_token:ttl" "604800" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:session:access_token:ttl" "3600" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:session:email_verification:ttl" "86400" >nul

REM Feature flags
%REDIS_CLI% SET "%KEY_PREFIX%:config:features:recommendations_enabled" "true" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:features:realtime_inventory_enabled" "true" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:features:analytics_enabled" "true" >nul
%REDIS_CLI% SET "%KEY_PREFIX%:config:features:cache_enabled" "true" >nul

echo [OK] Configuration keys initialized
echo.

REM Initialize persistent data structures
echo Initializing persistent data structures...

REM Initialize sorted sets for trending products
%REDIS_CLI% ZADD "%KEY_PREFIX%:analytics:trending:products:today" 0 "placeholder" >nul
%REDIS_CLI% ZREM "%KEY_PREFIX%:analytics:trending:products:today" "placeholder" >nul

REM Initialize sets for online users
%REDIS_CLI% SADD "%KEY_PREFIX%:realtime:online_users" "placeholder" >nul
%REDIS_CLI% SREM "%KEY_PREFIX%:realtime:online_users" "placeholder" >nul

REM Initialize geospatial index for vendor locations
%REDIS_CLI% GEOADD "%KEY_PREFIX%:vendor:locations:geo" 0 0 "placeholder" >nul
%REDIS_CLI% ZREM "%KEY_PREFIX%:vendor:locations:geo" "placeholder" >nul

echo [OK] Data structures initialized
echo.

REM Environment-specific initialization
if /i "%ENVIRONMENT%"=="dev" (
    echo Development environment setup...
    %REDIS_CLI% SET "%KEY_PREFIX%:config:environment" "development" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:debug:enabled" "true" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:cache:aggressive" "false" >nul
    echo [OK] Development configuration applied
) else if /i "%ENVIRONMENT%"=="staging" (
    echo Staging environment setup...
    %REDIS_CLI% SET "%KEY_PREFIX%:config:environment" "staging" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:debug:enabled" "true" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:cache:aggressive" "true" >nul
    echo [OK] Staging configuration applied
) else if /i "%ENVIRONMENT%"=="prod" (
    echo Production environment setup...
    %REDIS_CLI% SET "%KEY_PREFIX%:config:environment" "production" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:debug:enabled" "false" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:cache:aggressive" "true" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:monitoring:enabled" "true" >nul
    %REDIS_CLI% SET "%KEY_PREFIX%:config:logging:level" "WARN" >nul
    echo [OK] Production configuration applied
) else (
    echo Unknown environment: %ENVIRONMENT%, using defaults
    %REDIS_CLI% SET "%KEY_PREFIX%:config:environment" "%ENVIRONMENT%" >nul
)
echo.

REM Display summary
echo ========================================
echo Redis Initialization Complete
echo ========================================
echo.

REM Get key count
for /f %%k in ('%REDIS_CLI% DBSIZE') do set KEY_COUNT=%%k
echo Total keys in database: %KEY_COUNT%
echo.

echo Initialized key patterns:
echo   * %KEY_PREFIX%:config:*      - Configuration keys
echo   * %KEY_PREFIX%:analytics:*   - Analytics data
echo   * %KEY_PREFIX%:realtime:*    - Real-time features
echo   * %KEY_PREFIX%:vendor:*      - Vendor data
echo.

echo [OK] Redis is ready for use!
echo.

echo Next steps:
echo   1. Start your SoukConect services
echo   2. Check Redis health at: http://localhost:8080/actuator/health
echo   3. Monitor Redis: redis-cli -h %REDIS_HOST% -p %REDIS_PORT% MONITOR
echo   4. List all keys: redis-cli KEYS "%KEY_PREFIX%:*"
echo.

endlocal
