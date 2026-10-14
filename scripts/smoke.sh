#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - "${1:-}" <<'PYTHON'
import json, urllib.request, sys
for url, expected in [('http://127.0.0.1:8080/actuator/health/readiness', {'status':'UP'}), ('http://127.0.0.1:8000/health', {'status':'ok','service':'ai-service'})]:
    with urllib.request.urlopen(url, timeout=10) as response:
        assert response.status == 200
        assert json.load(response) == expected
with urllib.request.urlopen('http://127.0.0.1:4200/', timeout=10) as response:
    assert b'<app-root>' in response.read()
with urllib.request.urlopen('http://127.0.0.1:4200/system/health', timeout=10) as response:
    assert json.load(response) == {'status':'UP'}
if sys.argv[1] != '--reading':
    with urllib.request.urlopen('http://127.0.0.1:9000/minio/health/ready', timeout=10) as response:
        assert response.status == 200
    print('Santé Spring/PostgreSQL, FastAPI, frontend et MinIO validée.')
else:
    print('Santé du socle Reading et proxy Angular → Spring validés ; MinIO hors de ce contrôle.')
PYTHON
