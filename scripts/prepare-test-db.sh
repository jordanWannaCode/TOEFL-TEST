#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/compose.sh up -d postgres --wait
exists=$(./scripts/compose.sh exec -T postgres psql -U toefl -d postgres -tAX -c "SELECT 1 FROM pg_database WHERE datname='toefl_test'")
if [[ "$exists" != "1" ]]; then
  ./scripts/compose.sh exec -T postgres psql -U toefl -d postgres -v ON_ERROR_STOP=1 -c 'CREATE DATABASE toefl_test'
fi
