Vendor Admin UI (React + Vite + TypeScript)

What it is
- A lightweight SPA to manage Vendors via your existing REST API.
- Screens included: list, create, edit, delete. JSON editor for supportedCategories.

Quick start
1) Node 18+ installed
2) From web-admin/:
   - npm install
   - cp .env.example .env and set VITE_API_BASE_URL (e.g., http://localhost:8080)
   - npm run dev

Build for prod
- npm run build  # outputs to dist/

Config
- API base URL: VITE_API_BASE_URL
- Optional dev proxy: set VITE_PROXY_TARGET and Vite will proxy /vendors,/products,/customers

Deploy (Linux + nginx)
- Build with proxy profile so the UI uses relative paths (no CORS):
  - mvn -pl web-admin -am -Pproxy -DskipTests clean package
- Copy ZIP to server and unzip, then run installer:
  - sudo unzip web-admin-1.0.0-SNAPSHOT-dist.zip -d /var/www
  - sudo API_HOST=<your-api-host> API_PORT=8080 bash /var/www/web-admin-1.0.0-SNAPSHOT/deploy/install.sh
    - API_HOST can be with or without scheme (example.com or http://example.com); the installer normalizes it.
    - The installer prints a usable URL by detecting AWS public hostname/IP. You can override with PUBLIC_URL, e.g., PUBLIC_URL=http://admin.example.com
- The installer:
  - Writes nginx site to /etc/nginx/conf.d/souk-admin.conf
  - Site config includes a resolver (AWS VPC DNS + public) so variable proxy_pass hostnames resolve correctly.
  - Allows uploads up to 20 MB via `client_max_body_size 20m;` (adjust as needed).
  - Proxies `/uploads/**` and `/preview/**` to the API before static file rules so image/video previews and uploaded files work even for `.jpg/.png`.
  - Creates /etc/souk-admin/api.conf. By default:
    - set $api_origin http://HOST:PORT;
    - set $vendors_origin   $api_origin;
    - set $products_origin  $api_origin;
    - set $customers_origin $api_origin;
    - Edit any of the above to split services across different hosts/ports.
  - Symlinks /var/www/souk-admin -> extracted folder
  - Applies SELinux labels and reloads nginx
  - Prints a summary with your UI URL and the effective upstreams for /vendors, /products, /customers, plus any custom routes.
- Change API later: edit /etc/souk-admin/api.conf and run: sudo nginx -t && sudo systemctl reload nginx

Optional per-service overrides at install time
- You can supply env vars to point specific paths at different upstreams without editing the file:
  - sudo API_HOST=api.example.com API_PORT=8080 \
         VENDORS_HOST=api.example.com VENDORS_PORT=8080 \
         PRODUCTS_HOST=api2.example.com PRODUCTS_PORT=8081 \
         CUSTOMERS_HOST=api3.example.com CUSTOMERS_PORT=8082 \
         bash /var/www/web-admin-1.0.0-SNAPSHOT/deploy/install.sh
- This appends set lines to /etc/souk-admin/api.conf so those services proxy to the specified origins. Hosts can be given with or without scheme; the installer normalizes them.

Add new endpoints later (no rebuild needed)
- Drop a file under `/etc/souk-admin/routes.d/` with a `location` block. Example:
  - File: `/etc/souk-admin/routes.d/orders.conf`
    - location ~ ^/orders(/|$) {
      - proxy_set_header Host $host;
      - proxy_set_header X-Forwarded-Proto $scheme;
      - proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      - proxy_http_version 1.1;
      - proxy_pass http://orders-api.internal:9090;
      - }
- Then reload: `sudo nginx -t && sudo systemctl reload nginx`
- Any unknown top-level path automatically proxies to `$api_origin` via a catch-all, while `assets/` remains served statically.
