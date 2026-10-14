#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${CODEX_PROXY_CERT:-}" && -n "${HTTPS_PROXY:-}" ]]; then
  export BUILDX_CONFIG="${BUILDX_CONFIG:-/workspace/.cache/buildx}"
  python3 scripts/cloud-config.py >/dev/null
  exec docker compose -f compose.yaml -f "${TOEFL_CLOUD_CONFIG_DIR:-/workspace/.tools/toefl-cloud}/compose.json" "$@"
else
  exec docker compose "$@"
fi
