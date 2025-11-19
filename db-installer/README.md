db-installer
============

Purpose
- Package environment-specific SQL bundles (base + patches) and provide OS-specific installers.

What ships
- base: initial schema DDL in lexical order (always included)
- patches (per env):
  - dev → sql_files/patches/dev/**.sql
  - uat → dev + sql_files/patches/uat/**.sql
  - prod → dev + uat + sql_files/patches/prod/**.sql
- BUNDLE_VERSION file marking the build version.
- install_sql_files.sh (Unix) and install_sql_files.bat (Windows).

Build (pick one env)
- PowerShell:
  - mvn -pl db-installer -DskipTests "-Dbundle.env=dev" clean package
  - mvn -pl db-installer -DskipTests "-Dbundle.env=uat" clean package
  - mvn -pl db-installer -DskipTests "-Dbundle.env=prod" clean package

ZIP contents
- db-installer-<ver>/
  - sql/base/*.sql
  - sql/patches/**/*.sql (only if files exist for the env)
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

Notes
- The registry table (schema_patch_registry) is created by base/0000_schema_patch_registry.sql.
- Checksums are SHA-256; modifying an existing applied file will abort the run.
