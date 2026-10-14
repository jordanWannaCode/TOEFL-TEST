#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${CODEX_PROXY_CERT:-}" ]]; then
  export npm_config_cache="${npm_config_cache:-/workspace/.cache/npm}"
  export UV_CACHE_DIR="${UV_CACHE_DIR:-/workspace/.cache/uv}"
  if [[ -z "${PLAYWRIGHT_EXECUTABLE_PATH:-}" ]] && command -v chromium >/dev/null; then
    export PLAYWRIGHT_EXECUTABLE_PATH="$(command -v chromium)"
  fi
fi
./scripts/prepare-test-db.sh
./scripts/compose.sh run --rm backend-check
(cd apps/ai-service && uv sync --frozen && uv run --frozen pytest)
(cd apps/web && npm ci && npm run build && ../../scripts/e2e.sh)
