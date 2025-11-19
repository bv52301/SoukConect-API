#!/usr/bin/env bash

# ====================================================================
# MySQL Database Initialization Script (Linux/Bash)
# - Detects mysql client
# - Supports SSL or password authentication
# - Creates database if missing
# - Imports ALL .sql files in this folder (sorted)
# - Continues on per-file errors (logs warnings)
# ====================================================================
# Usage:
#   ./install_sql_files.sh <auth_mode> [show]
#     <auth_mode> : ssl | password
#     [show]      : optional, enables verbose output (set -x)
#
# Examples:
#   ./install_sql_files.sh password
#   ./install_sql_files.sh ssl show
#
# Notes:
# - You can override defaults via environment variables below.
# - For password auth, prefer exporting MYSQL_PWD instead of passing -p.

set -o errexit
set -o nounset
set -o pipefail

# --- Defaults (override with env vars) ---
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASS=${MYSQL_PASS:-}
MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
DB_NAME=${DB_NAME:-soukconect}

# SSL certificate paths (override if needed)
SSL_CA=${SSL_CA:-/etc/mysql/certs/ca.pem}
SSL_CERT=${SSL_CERT:-/etc/mysql/certs/client-cert.pem}
SSL_KEY=${SSL_KEY:-/etc/mysql/certs/client-key.pem}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# In packaged ZIP, SQL lives under ./sql relative to this script
SQL_DIR="${SCRIPT_DIR}/sql"

# Convenience: map DB_* env vars to MYSQL_* if provided
if [[ -n "${DB_USERNAME:-}" && -z "${MYSQL_USER:-}" ]]; then MYSQL_USER="${DB_USERNAME}"; fi
if [[ -n "${DB_PASSWORD:-}" && -z "${MYSQL_PASS:-}" ]]; then MYSQL_PASS="${DB_PASSWORD}"; fi
if [[ -n "${DB_HOST:-}" && -z "${MYSQL_HOST:-}" ]]; then MYSQL_HOST="${DB_HOST}"; fi
if [[ -n "${DB_PORT:-}" && -z "${MYSQL_PORT:-}" ]]; then MYSQL_PORT="${DB_PORT}"; fi
if [[ -n "${DB_NAME:-}" ]]; then DB_NAME="${DB_NAME}"; fi

if [[ ${#} -lt 1 ]]; then
  echo "Usage: $0 ssl|password [show]" >&2
  exit 1
fi

MODE=$1
VERBOSE=${2:-}

# Auto-detect bundle version if a version marker file is present beside this script
if [[ -z "${BUNDLE_VERSION:-}" && -f "${SCRIPT_DIR}/BUNDLE_VERSION" ]]; then
  BUNDLE_VERSION=$(tr -d '\r\n' < "${SCRIPT_DIR}/BUNDLE_VERSION")
fi

if [[ "$VERBOSE" == "show" ]]; then
  echo "Verbose mode enabled - showing all commands."
  set -x
fi

# ------------------------------------------------------------
# Locate mysql client
# ------------------------------------------------------------
MYSQL_CMD=""
if command -v mysql >/dev/null 2>&1; then
  MYSQL_CMD="$(command -v mysql)"
elif [[ -n "${MYSQL_HOME:-}" && -x "${MYSQL_HOME}/bin/mysql" ]]; then
  MYSQL_CMD="${MYSQL_HOME}/bin/mysql"
else
  echo "ERROR: mysql client not found in PATH, and MYSQL_HOME/bin/mysql not found." >&2
  echo "Please install MySQL client or set MYSQL_HOME to your MySQL installation." >&2
  exit 1
fi
echo "Found MySQL client: ${MYSQL_CMD}"

# ------------------------------------------------------------
# Build auth args
# ------------------------------------------------------------
AUTH_ARGS=( -u "$MYSQL_USER" )
case "$MODE" in
  ssl|SSL)
    AUTH_MODE=SSL
    AUTH_ARGS+=( --ssl-mode=VERIFY_CA --ssl-ca="$SSL_CA" --ssl-cert="$SSL_CERT" --ssl-key="$SSL_KEY" )
    ;;
  password|PASSWORD)
    AUTH_MODE=PASSWORD
    if [[ -n "$MYSQL_PASS" ]]; then
      AUTH_ARGS+=( -p"$MYSQL_PASS" )
    fi
    ;;
  *)
    echo "ERROR: Invalid option '$MODE'. Use 'ssl' or 'password'." >&2
    exit 1
    ;;
esac

echo "==========================================="
echo "Initializing MySQL database: ${DB_NAME}"
echo "Authentication mode: ${AUTH_MODE}"
echo "Verbose mode: ${VERBOSE}"
echo "Using MySQL client: ${MYSQL_CMD}"
echo "SQL directory: ${SQL_DIR}"
echo "==========================================="

run_mysql() {
  # Usage: run_mysql <mysql-args...>
  set +e
  "${MYSQL_CMD}" "${AUTH_ARGS[@]}" -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" "$@"
  local code=$?
  set -e
  return $code
}

run_mysql_file() {
  # Usage: run_mysql_file /path/to/file.sql
  local file=$1
  set +e
  "${MYSQL_CMD}" "${AUTH_ARGS[@]}" -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" "${DB_NAME}" < "$file"
  local code=$?
  set -e
  return $code
}

# ------------------------------------------------------------
# Helpers: checksum + registry
# ------------------------------------------------------------
sha256_of() {
  # Usage: sha256_of /path/to/file
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    openssl dgst -sha256 "$1" | awk '{print $NF}'
  fi
}

mysql_query() {
  # Usage: mysql_query "SELECT ..."  (outputs rows)
  set +e
  local out
  out=$("${MYSQL_CMD}" "${AUTH_ARGS[@]}" -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -N -B -e "$1" 2>/dev/null)
  local code=$?
  set -e
  echo "$out"
  return $code
}

ensure_registry() {
  # Only ensure the database exists; base/0000_schema_patch_registry.sql creates the table
  run_mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
}

process_sql_file() {
  # Usage: process_sql_file /abs/path/to/file.sql
  local file="$1"
  local rel
  rel="${file#${SQL_DIR}/}"
  local sum
  sum=$(sha256_of "$file")
  local esc_name
  esc_name=$(printf %s "$rel" | sed "s/'/''/g")
  local existing
  existing=$(mysql_query "SELECT checksum_sha256 FROM \`${DB_NAME}\`.schema_patch_registry WHERE script_name='${esc_name}' LIMIT 1;")

  if [[ -n "$existing" ]]; then
    if [[ "$existing" == "$sum" ]]; then
      echo "[skip] $rel already installed; checksum match, ignoring"
      return 0
    else
      echo "[ABORT] $rel was already applied with a different checksum ($existing != $sum). Create a new patch file."
      exit 1
    fi
  fi

  echo "Executing $rel ..."
  if run_mysql_file "$file"; then
    local ver="${BUNDLE_VERSION:-unversioned}"
    local by="${APPLIED_BY:-${USER:-$(whoami 2>/dev/null || echo unknown)}}"
    run_mysql -e "INSERT INTO \`${DB_NAME}\`.schema_patch_registry(script_name,checksum_sha256,bundle_version,applied_by,status) VALUES ('${esc_name}','${sum}','${ver}','${by}','applied');"
    echo "[ok] $rel applied"
  else
    echo "[fail] $rel failed to apply"
    exit 1
  fi
}

# Backfill helper intentionally removed (manual backfill preferred)

# ------------------------------------------------------------
# Create database
# ------------------------------------------------------------
echo "Creating database (if missing)..."
echo "---------------------------------------"
echo "Executing SQL: CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`"
echo "---------------------------------------"
run_mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# ------------------------------------------------------------
# Import all .sql files in sorted order
# ------------------------------------------------------------
shopt -s nullglob
# Resolve file list: base first, then everything else under patches/* in lex order
BASE_DIR="${SQL_DIR}/base"
PATCHES_DIR="${SQL_DIR}/patches"
if [[ ! -d "$BASE_DIR" ]]; then BASE_DIR="${SQL_DIR}"; fi

echo "Importing base .sql files from: ${BASE_DIR} (lexical order)"
ensure_registry
mapfile -t SQL_BASE < <(find "$BASE_DIR" -maxdepth 1 -type f -name '*.sql' | sort)
for f in "${SQL_BASE[@]}"; do
  echo "---------------------------------------"
  process_sql_file "$f"
done

# Patches (optional)
if [[ -d "$PATCHES_DIR" ]]; then
  echo "Importing patch .sql files from: ${PATCHES_DIR} (recursive lexical order)"
  mapfile -t SQL_PATCHES < <(find "$PATCHES_DIR" -type f -name '*.sql' | sort)
  for f in "${SQL_PATCHES[@]}"; do
    echo "---------------------------------------"
    process_sql_file "$f"
  done
fi

echo "==========================================="
echo "Database setup completed successfully!"
echo "==========================================="

exit 0
