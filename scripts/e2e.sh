#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Seulement les ressources du projet de test sont supprimées à la fin.
cleanup() { docker compose -p toefl-simulator-e2e -f compose.yaml -f infra/compose.e2e.yaml down -v >/dev/null; }
trap cleanup EXIT
docker compose -p toefl-simulator-e2e -f compose.yaml -f infra/compose.e2e.yaml up -d postgres backend frontend --wait --wait-timeout 180
python3 - <<'PY'
import os, subprocess
from pathlib import Path
values = dict(line.split('=',1) for line in Path('.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
run_env = dict(os.environ)
run_env.update({'E2E_EMAIL': values['LOCAL_USER_EMAIL'], 'E2E_PASSWORD': values['LOCAL_USER_PASSWORD'], 'FRONTEND_URL': 'http://127.0.0.1:4210'})
raise SystemExit(subprocess.run(['npm','run','test:e2e'], cwd='apps/web', env=run_env).returncode)
PY
