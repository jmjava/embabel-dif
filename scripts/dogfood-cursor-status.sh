#!/usr/bin/env bash
# Did the last dogfood Cursor *day* work (gate/lesson), not merely spawn?
set -euo pipefail
here=$(cd "$(dirname "$0")/.." && pwd)
dog="${DOGFOOD_HOME:-$HOME/github/jmjava/dogfood-api}"
if [[ ! -x "$dog/scripts/agent-day/status.sh" ]]; then
  echo "RESULT=FAILED"
  echo "WHY=missing-status-script: $dog/scripts/agent-day/status.sh"
  exit 1
fi
export DIF_HOME="${DIF_HOME:-$here}"
exec "$dog/scripts/agent-day/status.sh" "$@"
