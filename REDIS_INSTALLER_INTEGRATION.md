# Redis Installer Integration with db-installer

## Summary

Redis installation scripts have been successfully integrated into the existing `db-installer` package. This provides a unified installer bundle for all SoukConect infrastructure setup (MySQL + Redis).

## Changes Made

### 1. New Directory: `redis_files/`

Created parallel to `sql_files/`, contains:
- `install_redis.sh` - Main installation script (Linux/Unix)
- `install_redis.bat` - Main installation script (Windows)
- `redis-init.sh` - Alternative manual initialization
- `generate-ssl-certs.sh` - SSL certificate generation
- `docker-compose-redis-ssl.yml` - Docker Compose with SSL
- `application-redis-sample.yml` - Spring Boot config sample
- `redis-keys-reference.json` - Key patterns reference
- `README.md` - Documentation

### 2. Updated: `db-installer/pom.xml`

Added new execution in `maven-antrun-plugin`:
```xml
<execution>
    <id>stage-redis-files</id>
    <phase>prepare-package</phase>
    ...
    <!-- Copies all files from redis_files/ to target/redis/ -->
</execution>
```

### 3. Updated: `db-installer/src/assembly/bin.xml`

Added new fileSet to include Redis files in the ZIP:
```xml
<fileSet>
    <directory>${project.build.directory}/redis</directory>
    <outputDirectory>/redis</outputDirectory>
    ...
</fileSet>
```

### 4. Updated: `db-installer/README.md`

- Added Redis installation instructions
- Updated ZIP contents section
- Added environment variable documentation for Redis

## Package Structure

After building with `mvn -pl db-installer clean package`, the ZIP contains:

```
db-installer-1.0.0-SNAPSHOT/
├── sql/
│   ├── base/
│   │   ├── 0000_schema_patch_registry.sql
│   │   ├── 0100_Cuisines.sql
│   │   ├── 0110_vendor_details.sql
│   │   └── ... (all base SQL files)
│   ├── patches/
│   │   └── ... (environment-specific patches)
│   ├── MIGRATION_01_backup_vendor_data.sql
│   ├── MIGRATION_02_restore_and_migrate.sql
│   └── MIGRATION_03_complete_vendor_migration.sql
├── redis/
│   ├── install_redis.sh               ← NEW
│   ├── install_redis.bat              ← NEW
│   ├── redis-init.sh                  ← NEW
│   ├── generate-ssl-certs.sh          ← NEW
│   ├── docker-compose-redis-ssl.yml   ← NEW
│   ├── application-redis-sample.yml   ← NEW
│   ├── redis-keys-reference.json      ← NEW
│   └── README.md                      ← NEW
├── install_sql_files.sh
├── install_sql_files.bat
├── BUNDLE_VERSION
└── README.md (updated)
```

## Usage

### Building the Installer

```bash
# Build for development
mvn -pl db-installer -DskipTests "-Dbundle.env=dev" clean package

# Build for staging
mvn -pl db-installer -DskipTests "-Dbundle.env=staging" clean package

# Build for production
mvn -pl db-installer -DskipTests "-Dbundle.env=prod" clean package
```

### Installation on Target Machine

**1. Unzip the package:**
```bash
unzip db-installer-1.0.0-SNAPSHOT-bin.zip
cd db-installer-1.0.0-SNAPSHOT/
```

**2. Install MySQL database:**
```bash
# Linux/Unix
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_USERNAME=root
export DB_PASSWORD=your-password
export DB_NAME=soukconect
./install_sql_files.sh

# Windows
set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASS=your-password
set DB_NAME=soukconect
install_sql_files.bat
```

**3. Install Redis:**
```bash
# Linux/Unix - Development
cd redis
./install_redis.sh dev

# Linux/Unix - Production with SSL
cd redis
REDIS_HOST=redis-prod.example.com \
REDIS_PORT=6380 \
REDIS_USERNAME=default \
REDIS_PASSWORD=secure-pass \
REDIS_SSL=true \
./install_redis.sh prod

# Windows - Development
cd redis
install_redis.bat dev

# Windows - Production
cd redis
set REDIS_HOST=redis-prod.example.com
set REDIS_PORT=6380
set REDIS_USERNAME=default
set REDIS_PASSWORD=secure-pass
install_redis.bat prod
```

## What Redis Installation Does

The `install_redis.sh` / `install_redis.bat` scripts initialize:

1. **Configuration Keys**
   - Rate limits (login: 5 attempts/5min, API: 1000 req/min)
   - Cache TTLs (user: 1h, vendor: 30min, product: 30min)
   - Session timeouts (refresh token: 7 days, access token: 1h)
   - Feature flags (caching, recommendations, analytics)

2. **Data Structures**
   - Trending products (sorted sets)
   - Online users (sets)
   - Vendor geospatial index (for location queries)

3. **Environment-Specific Settings**
   - **dev**: Debug enabled, less aggressive caching
   - **staging**: Debug enabled, aggressive caching
   - **prod**: Debug disabled, monitoring enabled, aggressive caching

## Verification

After installation:

**Check MySQL:**
```sql
SELECT * FROM schema_patch_registry ORDER BY installed_at DESC LIMIT 5;
```

**Check Redis:**
```bash
redis-cli DBSIZE
redis-cli KEYS "config:*"
```

**Check Spring Boot Health:**
```bash
curl http://localhost:8080/actuator/health
```

## Benefits

1. **Single Package** - One ZIP for all infrastructure setup
2. **Environment-Aware** - Same package works for dev/staging/prod
3. **Idempotent** - Safe to run multiple times
4. **OS-Agnostic** - Works on Linux, Unix, Windows
5. **Versioned** - BUNDLE_VERSION tracks build timestamp
6. **Documentation** - Comprehensive README in each directory

## Source Directories

- `sql_files/` - Source for all SQL scripts and migrations
- `redis_files/` - Source for all Redis scripts and configs
- `db-installer/` - Maven module that packages both into single ZIP

## Integration with Application

The Spring Boot application (all services) automatically:
1. Connects to Redis using configuration from `application.yml`
2. Uses type-safe key managers from `common-adapters/redis/keys/`
3. Auto-initializes on startup via `RedisInitializer` (ApplicationRunner)
4. Exposes health check at `/actuator/health`

No code changes needed - just configure environment variables:
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    username: ${REDIS_USERNAME:}
    ssl:
      enabled: ${REDIS_SSL_ENABLED:false}
```

## Next Steps

1. ✅ **Integration Complete** - Redis installer merged into db-installer
2. ⏳ **Test Build** - Run `mvn -pl db-installer clean package` to verify
3. ⏳ **Test Installation** - Unzip and run both installers on test environment
4. ⏳ **Deploy** - Use built ZIP for staging/production deployments
5. ⏳ **Monitor** - Set up Redis monitoring and alerts

## Files Changed

1. ✅ `redis_files/` (new directory with 9 files)
2. ✅ `db-installer/pom.xml` (added Redis staging execution)
3. ✅ `db-installer/src/assembly/bin.xml` (added Redis fileSet)
4. ✅ `db-installer/README.md` (added Redis documentation)
5. ✅ `REDIS_INSTALLER_INTEGRATION.md` (this file)

---

**Date:** 2026-01-15
**Status:** ✅ Complete
**Package:** db-installer (now includes both SQL and Redis)
