#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${CODEX_PROXY_CERT:-}" ]]; then
  export npm_config_cache="${npm_config_cache:-/workspace/.cache/npm}"
  export UV_CACHE_DIR="${UV_CACHE_DIR:-/workspace/.cache/uv}"
fi
./scripts/init-local.sh
(cd apps/web && npm ci)
(cd apps/ai-service && uv sync --frozen)
# Limiter la consommation de disque et réutiliser les couches valides.
for service in backend frontend ai-service; do ./scripts/compose.sh build "$service"; done
