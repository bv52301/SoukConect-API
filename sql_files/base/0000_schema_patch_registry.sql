-- Registry table to track applied SQL patches per database
-- This file MUST run first. Name ensures lexical ordering.

CREATE TABLE IF NOT EXISTS schema_patch_registry (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  script_name      VARCHAR(255) NOT NULL,
  checksum_sha256  CHAR(64)     NOT NULL,
  bundle_version   VARCHAR(32)  NOT NULL,
  applied_by       VARCHAR(64)  NULL,
  applied_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status           ENUM('applied','skipped','failed') NOT NULL DEFAULT 'applied',
  CONSTRAINT uk_schema_patch_registry_name UNIQUE (script_name)
);

-- Optional helper index if you query by applied_at frequently
CREATE INDEX IF NOT EXISTS idx_schema_patch_registry_applied_at
  ON schema_patch_registry (applied_at);

-- Notes:
-- - The installer will compute the SHA-256 of each script and insert a row into
--   schema_patch_registry after successful execution.
-- - If a script with the same name already exists but with a different checksum,
--   the installer MUST abort to prevent tampering; create a new patch file instead.
