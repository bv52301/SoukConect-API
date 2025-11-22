#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${DB_HOST:-}" || -z "${DB_USER:-}" || -z "${DB_NAME:-}" ]]; then
  echo "Usage: set DB_HOST, DB_USER, DB_PASS (optional), DB_NAME [DB_PORT]" >&2
  echo "Optional SSL: DB_SSL_MODE (REQUIRED|VERIFY_CA|VERIFY_IDENTITY), DB_SSL_CA, DB_SSL_CERT, DB_SSL_KEY" >&2
  exit 1
fi

PORT="${DB_PORT:-3306}"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)"

build_common_opts() {
  local -a opts
  opts+=(-h "$DB_HOST" -P "$PORT" -u "$DB_USER")
  if [[ -n "${DB_PASS:-}" ]]; then
    opts+=(-p"$DB_PASS")
  fi
  if [[ -n "${DB_SSL_MODE:-}" ]]; then
    opts+=(--ssl-mode="$DB_SSL_MODE")
  fi
  if [[ -n "${DB_SSL_CA:-}" ]]; then
    opts+=(--ssl-ca="$DB_SSL_CA")
  fi
  if [[ -n "${DB_SSL_CERT:-}" ]]; then
    opts+=(--ssl-cert="$DB_SSL_CERT")
  fi
  if [[ -n "${DB_SSL_KEY:-}" ]]; then
    opts+=(--ssl-key="$DB_SSL_KEY")
  fi
  printf '%s\n' "${opts[@]}"
}

COMMON_OPTS=()
while IFS= read -r line; do COMMON_OPTS+=("$line"); done < <(build_common_opts)

mysql_cmd=(mysql "${COMMON_OPTS[@]}" "$DB_NAME")
mysqldump_cmd=(mysqldump "${COMMON_OPTS[@]}" --no-data --skip-comments --single-transaction "$DB_NAME")

mapfile -t tables < <("${mysql_cmd[@]}" -N -e "SHOW TABLES;")

for tbl in "${tables[@]}"; do
  [[ -z "$tbl" ]] && continue
  echo "Exporting $tbl ..."
  "${mysqldump_cmd[@]}" "$tbl" > "${OUT_DIR}/create_${tbl}.sql"
done

echo "Done. Files written to ${OUT_DIR}"
