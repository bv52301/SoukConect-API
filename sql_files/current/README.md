# Current schema exports (one file per table)

Purpose: keep a ready-to-use snapshot of the database schema (post base + all patches) so a fresh DB can be created without replaying patches.

How to regenerate:
1) Point to a database that already has all patches applied.
2) Set env vars: `DB_HOST`, `DB_PORT` (optional, defaults 3306), `DB_USER`, `DB_PASS`, `DB_NAME`.
   Optional SSL: `DB_SSL_MODE` (e.g., REQUIRED/VERIFY_CA/VERIFY_IDENTITY), `DB_SSL_CA`, `DB_SSL_CERT`, `DB_SSL_KEY`.
3) Run either:
   - Linux/macOS: `./export_schema.sh`
   - Windows PowerShell: `./export_schema.ps1`
4) The script will drop per-table `create_<table>.sql` files into this folder (overwriting existing).

Notes:
- Base files remain immutable; this folder is a convenience snapshot.
- If new patches are added, regenerate these exports to keep them in sync.
