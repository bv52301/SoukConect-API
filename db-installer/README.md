db-installer
============

Purpose
- Package environment-specific SQL bundles (base + patches), Redis initialization scripts, and provide OS-specific installers.

What ships
- SQL files:
  - base: initial schema DDL in lexical order (always included)
  - patches (per env):
    - dev → sql_files/patches/dev/**.sql
    - uat → dev + sql_files/patches/uat/**.sql
    - prod → dev + uat + sql_files/patches/prod/**.sql
  - install_sql_files.sh (Unix) and install_sql_files.bat (Windows)
- Redis files:
  - install_redis.sh (Unix) and install_redis.bat (Windows)
  - redis-init.sh, generate-ssl-certs.sh
  - docker-compose-redis-ssl.yml
  - application-redis-sample.yml (Spring Boot config sample)
  - redis-keys-reference.json (key patterns reference)
- BUNDLE_VERSION file marking the build version.

Build (pick one env)
- PowerShell:
  - mvn -pl db-installer -DskipTests "-Dbundle.env=dev" clean package
  - mvn -pl db-installer -DskipTests "-Dbundle.env=uat" clean package
  - mvn -pl db-installer -DskipTests "-Dbundle.env=prod" clean package

ZIP contents
- db-installer-<ver>/
  - sql/base/*.sql
  - sql/patches/**/*.sql (only if files exist for the env)
  - redis/install_redis.sh, install_redis.bat
  - redis/redis-init.sh, generate-ssl-certs.sh
  - redis/docker-compose-redis-ssl.yml
  - redis/application-redis-sample.yml
  - redis/redis-keys-reference.json
  - redis/README.md
  - BUNDLE_VERSION, install_sql_files.sh, install_sql_files.bat, README.md

Linux install
- export DB_HOST=127.0.0.1 DB_PORT=3306 DB_USERNAME=root DB_PASSWORD=… DB_NAME=soukconect
- unzip db-installer-*-bin.zip && cd db-installer-*/
- ./install_sql_files.sh password
- Output behavior:
  - New script → executed, recorded as applied
  - Same script/checksum → "[skip] <file> already installed; checksum match, ignoring"
  - Changed content (same filename) → aborts

Windows install
- set MYSQL_HOST=127.0.0.1 & set MYSQL_PORT=3306 & set MYSQL_USER=root & set MYSQL_PASS=… & set DB_NAME=soukconect
- unzip and double-click install_sql_files.bat (or run in cmd)

Redis install
- Linux/Unix:
  - cd db-installer-*/redis
  - ./install_redis.sh dev (or staging/prod)
  - For production with SSL:
    - REDIS_HOST=redis-prod.example.com REDIS_PORT=6380 REDIS_USERNAME=default REDIS_PASSWORD=secure-pass REDIS_SSL=true ./install_redis.sh prod
- Windows:
  - cd db-installer-*\redis
  - install_redis.bat dev (or staging/prod)
  - For production:
    - set REDIS_HOST=redis-prod.example.com && set REDIS_PORT=6380 && set REDIS_USERNAME=default && set REDIS_PASSWORD=secure-pass && install_redis.bat prod

Notes
- The registry table (schema_patch_registry) is created by base/0000_schema_patch_registry.sql.
- Checksums are SHA-256; modifying an existing applied file will abort the run.
- Redis initialization is independent of SQL installation and can be run separately.
- See redis/README.md in the package for detailed Redis setup instructions.
