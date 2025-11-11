#!/usr/bin/env bash
set -euo pipefail

# Defaults can be overridden via environment variables
API_HOST="${API_HOST:-ec2-13-250-197-155.ap-southeast-1.compute.amazonaws.com}"
API_PORT="${API_PORT:-8080}"
# Optional per-service overrides
VENDORS_HOST="${VENDORS_HOST:-}"
VENDORS_PORT="${VENDORS_PORT:-}"
PRODUCTS_HOST="${PRODUCTS_HOST:-}"
PRODUCTS_PORT="${PRODUCTS_PORT:-}"
CUSTOMERS_HOST="${CUSTOMERS_HOST:-}"
CUSTOMERS_PORT="${CUSTOMERS_PORT:-}"
UI_ROOT="${UI_ROOT:-/var/www}"
SITE_CONF_PATH="${SITE_CONF_PATH:-/etc/nginx/conf.d/souk-admin.conf}"
API_CONF_DIR="${API_CONF_DIR:-/etc/souk-admin}"
API_CONF_FILE="$API_CONF_DIR/api.conf"
ROUTES_DIR="$API_CONF_DIR/routes.d"

if [[ $EUID -ne 0 ]]; then
  echo "Please run as root (sudo)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EXTRACTED_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Installing web-admin from: $EXTRACTED_DIR"

# 1) Ensure runtime API config exists and is set
mkdir -p "$API_CONF_DIR"
mkdir -p "$ROUTES_DIR"
if [[ ! -f "$API_CONF_FILE" ]]; then
  cp -f "$SCRIPT_DIR/api.conf.sample" "$API_CONF_FILE"
fi
# Normalize API_HOST if it includes a scheme or trailing slash
API_HOST_NOSCHEME="$API_HOST"
API_HOST_NOSCHEME="${API_HOST_NOSCHEME#http://}"
API_HOST_NOSCHEME="${API_HOST_NOSCHEME#https://}"
API_HOST_NOSCHEME="${API_HOST_NOSCHEME%/}"
sed -i "s|http://HOST:PORT|http://$API_HOST_NOSCHEME:$API_PORT|g" "$API_CONF_FILE"
# Ensure per-service defaults exist even if the file predates multi-origins
grep -q '\$vendors_origin'   "$API_CONF_FILE" || echo 'set $vendors_origin   $api_origin;' >> "$API_CONF_FILE"
grep -q '\$products_origin'  "$API_CONF_FILE" || echo 'set $products_origin  $api_origin;' >> "$API_CONF_FILE"
grep -q '\$customers_origin' "$API_CONF_FILE" || echo 'set $customers_origin $api_origin;' >> "$API_CONF_FILE"
# If per-service envs are provided, append overrides so they take precedence
if [[ -n "$VENDORS_HOST" ]]; then
  host="$VENDORS_HOST"; host="${host#http://}"; host="${host#https://}"; host="${host%/}"
  echo "set \$vendors_origin http://$host:${VENDORS_PORT:-$API_PORT};" >> "$API_CONF_FILE"
fi
if [[ -n "$PRODUCTS_HOST" ]]; then
  host="$PRODUCTS_HOST"; host="${host#http://}"; host="${host#https://}"; host="${host%/}"
  echo "set \$products_origin http://$host:${PRODUCTS_PORT:-$API_PORT};" >> "$API_CONF_FILE"
fi
if [[ -n "$CUSTOMERS_HOST" ]]; then
  host="$CUSTOMERS_HOST"; host="${host#http://}"; host="${host#https://}"; host="${host%/}"
  echo "set \$customers_origin http://$host:${CUSTOMERS_PORT:-$API_PORT};" >> "$API_CONF_FILE"
fi
echo "Configured API origin in $API_CONF_FILE"

# 2) Place nginx site config
install -m 0644 "$SCRIPT_DIR/souk-admin.conf" "$SITE_CONF_PATH"
echo "Installed nginx site to $SITE_CONF_PATH"

# 3) Point stable symlink to this extracted version
ln -sfn "$EXTRACTED_DIR" "$UI_ROOT/souk-admin"
echo "Linked $UI_ROOT/souk-admin -> $EXTRACTED_DIR"

# 4) SELinux: content labels + allow outbound from nginx to API
if command -v getenforce >/dev/null 2>&1; then
  mode=$(getenforce || true)
  if [[ "$mode" != "Disabled" ]]; then
    if ! command -v semanage >/dev/null 2>&1; then
      if command -v dnf >/dev/null 2>&1; then
        dnf -y install policycoreutils-python-utils || true
      elif command -v yum >/dev/null 2>&1; then
        yum -y install policycoreutils-python || true
      fi
    fi
    semanage fcontext -a -t httpd_sys_content_t "$UI_ROOT(/.*)?" 2>/dev/null || true
    restorecon -Rv "$UI_ROOT" || true
    setsebool -P httpd_can_network_connect on || true
  fi
fi

# 5) Permissions sane for static hosting
find "$UI_ROOT" -type d -exec chmod 755 {} +
find "$UI_ROOT" -type f -exec chmod 644 {} +

# 6) Reload nginx
nginx -t
systemctl reload nginx || systemctl restart nginx

# Print a helpful URL summary to visit and test
if [[ -n "${PUBLIC_URL:-}" ]]; then
  base_url="$PUBLIC_URL"
else
  # Try AWS metadata first (public hostname, then IP), with short timeouts
  meta_host=$(curl -fsS --max-time 1 http://169.254.169.254/latest/meta-data/public-hostname || true)
  if [[ -n "$meta_host" ]]; then
    base_url="http://$meta_host"
  else
    meta_ip=$(curl -fsS --max-time 1 http://169.254.169.254/latest/meta-data/public-ipv4 || true)
    if [[ -n "$meta_ip" ]]; then
      base_url="http://$meta_ip"
    else
      # Fallbacks if instance has no public address
      hn=$(hostname -f 2>/dev/null || hostname || echo localhost)
      base_url="http://$hn"
    fi
  fi
fi

# Resolve origin variables from API conf
resolve_origin() {
  local var="$1"
  local raw="$(awk -v v="\$${var}" '$1=="set" && $2==v {print $3}' "$API_CONF_FILE" | tr -d ';' | tail -n1)"
  if [[ -z "$raw" ]]; then
    echo ""
    return
  fi
  if [[ "$raw" == \$* ]]; then
    local ref="${raw#\$}"
    awk -v v="\$${ref}" '$1=="set" && $2==v {print $3}' "$API_CONF_FILE" | tr -d ';' | tail -n1
  else
    echo "$raw"
  fi
}

vendors_o="$(resolve_origin vendors_origin)";  [[ -z "$vendors_o"  ]] && vendors_o="$(resolve_origin api_origin)"
products_o="$(resolve_origin products_origin)"; [[ -z "$products_o" ]] && products_o="$(resolve_origin api_origin)"
customers_o="$(resolve_origin customers_origin)";[[ -z "$customers_o" ]]&& customers_o="$(resolve_origin api_origin)"
default_o="$(resolve_origin api_origin)"

echo "Deployment complete. UI and endpoints:"
echo "  UI:           ${base_url}/"
echo "  /vendors  ->  ${vendors_o}"
echo "  /products ->  ${products_o}"
echo "  /customers->  ${customers_o}"
echo "  other paths -> ${default_o} (via catch-all)"

# List any custom route files if present
if ls "$ROUTES_DIR"/*.conf >/dev/null 2>&1; then
  echo "  custom routes in: $ROUTES_DIR/"
  for f in "$ROUTES_DIR"/*.conf; do
    [ -e "$f" ] || continue
    # Try to extract first regex path and proxy_pass target (best-effort)
    pfx=$(sed -n 's/^\s*location\s*~\s*\^\(\/[A-Za-z0-9_-]\+\).*/\1/p' "$f" | head -n1)
    up=$(sed -n 's/.*proxy_pass\s\+\([^;]*\).*/\1/p' "$f" | head -n1)
    if [[ -n "$pfx" && -n "$up" ]]; then
      echo "    ${pfx} -> ${up} (from $(basename "$f"))"
    else
      echo "    $(basename "$f") (custom routes)"
    fi
  done
fi
