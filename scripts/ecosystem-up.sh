#!/usr/bin/env bash
# Clone/use dogfood-api and run its ecosystem bootstrap (orch + DIF + Dashboard).
set -euo pipefail
here=$(cd "$(dirname "$0")/.." && pwd)
tools="${DOGFOOD_TOOLS:-$here/.tools}"
dog="${DOGFOOD_HOME:-}"
if [[ -z "$dog" ]]; then
  for candidate in \
    "$HOME/github/jmjava/dogfood-api" \
    "$(cd "$here/.." && pwd)/dogfood-api" \
    "$tools/dogfood-api"
  do
    if [[ -x "$candidate/scripts/up.sh" ]]; then
      dog="$(cd "$candidate" && pwd)"
      break
    fi
  done
fi
if [[ -z "${dog:-}" || ! -x "$dog/scripts/up.sh" ]]; then
  mkdir -p "$tools"
  if [[ ! -d "$tools/dogfood-api/.git" ]]; then
    git clone --depth 1 https://github.com/jmjava/dogfood-api.git "$tools/dogfood-api"
  fi
  dog="$(cd "$tools/dogfood-api" && pwd)"
fi
export DIF_HOME="${DIF_HOME:-$here}"
export ORCH_HOME="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
echo "==> ecosystem-up dogfood=$dog dif=$DIF_HOME orch=$ORCH_HOME"
exec "$dog/scripts/up.sh" "$@"
