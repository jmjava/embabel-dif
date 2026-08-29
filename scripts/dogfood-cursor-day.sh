#!/usr/bin/env bash
# Hit Cursor against dogfood-api. Wrapper for scripts/agent-day/run.sh.
set -euo pipefail
here=$(cd "$(dirname "$0")/.." && pwd)
dog="${DOGFOOD_HOME:-$HOME/github/jmjava/dogfood-api}"
if [[ ! -x "$dog/scripts/agent-day/run.sh" ]]; then
  echo "FAIL: $dog/scripts/agent-day/run.sh missing" >&2
  exit 1
fi
export DIF_HOME="${DIF_HOME:-$here}"
export ORCH_HOME="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
exec "$dog/scripts/agent-day/run.sh" "$@"
