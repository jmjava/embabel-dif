#!/usr/bin/env bash
# Full-flow example: fold a changing REASONS canvas and read it on the
# orch Dashboard. Refresh does not start a fold or a JVM.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
export ORCH_HOME="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
if [[ ! -d "${ORCH_HOME}/engine/src" ]]; then
  echo "FAIL: set ORCH_HOME to an sdlc-spdd-orchestrator checkout" >&2
  exit 1
fi
if [[ -x /tmp/orch-venv/bin/python ]]; then
  py=/tmp/orch-venv/bin/python
else
  py="${PYTHON:-python3}"
fi
echo "== dashboard E2E (API; Playwright if DIF_DASHBOARD_PLAYWRIGHT=1) =="
"$py" ./scripts/dif-dashboard-e2e.py
