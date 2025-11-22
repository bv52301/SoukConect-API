# Database SQL guidance

- `base/` scripts are immutable after release; do **not** edit them. Any schema/data changes must go into `patches/` as new numbered patches.
- Before committing, ensure no `sql_files/base/` files are modified or staged.
- Suggested guard (add to `.git/hooks/pre-commit`, `chmod +x`):
  ```sh
  if git diff --cached --name-only | grep -q '^sql_files/base/'; then
    echo "Block: base SQL files are read-only. Use sql_files/patches/ instead."
    exit 1
  fi
  ```
