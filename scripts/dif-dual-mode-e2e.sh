#!/usr/bin/env bash
# Structured Work ID + unstructured area capture on one consumer tree.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
export ORCH_HOME="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
export DOGFOOD_HOME="${DOGFOOD_HOME:-$HOME/github/jmjava/dogfood-api}"
if [[ ! -d "${ORCH_HOME}/engine/src" ]]; then
  echo "FAIL: set ORCH_HOME to an sdlc-spdd-orchestrator checkout" >&2
  exit 1
fi
if [[ -x /tmp/orch-venv/bin/python ]]; then
  py=/tmp/orch-venv/bin/python
elif [[ -x "${ORCH_HOME}/.venv/bin/python" ]]; then
  py="${ORCH_HOME}/.venv/bin/python"
else
  py="${PYTHON:-python3}"
fi
echo "== dual-mode E2E (structured Work ID + unstructured area) =="
"$py" ./scripts/dif-dual-mode-e2e.py
