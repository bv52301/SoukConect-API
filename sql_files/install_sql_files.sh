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
SQL_DIR="${SCRIPT_DIR}"

if [[ ${#} -lt 1 ]]; then
  echo "Usage: $0 ssl|password [show]" >&2
  exit 1
fi

MODE=$1
VERBOSE=${2:-}

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
echo "SQL directory: ${SCRIPT_DIR} (detecting .sql location)"
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
SQL_CANDIDATES=("${SCRIPT_DIR}"/*.sql)
if [[ ${#SQL_CANDIDATES[@]} -eq 0 && -d "${SCRIPT_DIR}/sql" ]]; then
  SQL_DIR="${SCRIPT_DIR}/sql"
  SQL_CANDIDATES=("${SQL_DIR}"/*.sql)
fi

if [[ ${#SQL_CANDIDATES[@]} -eq 0 ]]; then
  echo "ERROR: No .sql files found in '${SCRIPT_DIR}' or '${SCRIPT_DIR}/sql'." >&2
  exit 1
fi

echo "Importing .sql files from: ${SQL_DIR}"

# Build sorted list safely
mapfile -t SQL_FILES < <(printf '%s\n' "${SQL_CANDIDATES[@]}" | sort)

for f in "${SQL_FILES[@]}"; do
  echo "---------------------------------------"
  echo "Importing $(basename "$f") ..."
  if ! run_mysql_file "$f"; then
    echo "Warning: $(basename "$f") returned non-zero exit; continuing" >&2
  fi
done

echo "==========================================="
echo "Database setup completed successfully!"
echo "==========================================="

exit 0
