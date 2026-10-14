#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
import os, secrets
from pathlib import Path
path = Path('.env')
existing = path.read_text() if path.exists() else ''
names = {line.split('=', 1)[0] for line in existing.splitlines() if '=' in line and not line.startswith('#')}
defaults = {'POSTGRES_PASSWORD': secrets.token_hex(24), 'MINIO_ROOT_USER': 'toefl-local', 'MINIO_ROOT_PASSWORD': secrets.token_hex(24), 'LOCAL_USER_EMAIL': 'student@localhost', 'LOCAL_USER_PASSWORD': secrets.token_hex(24), 'READING_PRACTICE_DURATION_SECONDS': '0', 'AI_INTERNAL_TOKEN': secrets.token_hex(32)}
missing = {k:v for k,v in defaults.items() if k not in names}
if missing:
    flags = os.O_WRONLY | os.O_APPEND | os.O_CREAT
    with os.fdopen(os.open(path, flags, 0o600), 'a') as f:
        if existing and not existing.endswith('\n'): f.write('\n')
        for key, value in missing.items(): f.write(key+'='+value+'\n')
    path.chmod(0o600)
print('Configuration locale préparée ; valeurs existantes conservées et secrets non affichés.')
PY
