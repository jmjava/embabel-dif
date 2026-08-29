#!/usr/bin/env bash
# New Cloud Agent: pick up the dogfood Cursor-linkage work.
# Read scripts/HANDOFF-CURSOR-AGENT-DAY.md first.
set -euo pipefail

DIF_HOME="${DIF_HOME:-$(cd "$(dirname "$0")/.." && pwd)}"
ORCH_HOME="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
DOGFOOD_HOME="${DOGFOOD_HOME:-$HOME/github/jmjava/dogfood-api}"
BRANCH="cursor/dogfood-cursor-agent-148e"

echo "==> handoff bootstrap"
echo "    dif     : $DIF_HOME"
echo "    orch    : $ORCH_HOME"
echo "    dogfood : $DOGFOOD_HOME"

if [[ -z "${CORRECT_CURSOR_KEY:-}" ]]; then
  echo "FAIL: CORRECT_CURSOR_KEY is not in this process." >&2
  echo "  The secret exists on the environment, but this agent booted without it." >&2
  echo "  Do not use CURSOR_API_KEY (sk-proj → 401)." >&2
  echo "  Start another Cloud Agent after confirming the secret is on" >&2
  echo "  https://cursor.com/dashboard/cloud-agents/environments/e/96a8f5cd-a27f-11f1-b532-320a589b8025" >&2
  exit 1
fi
echo "    CORRECT_CURSOR_KEY: present (len=${#CORRECT_CURSOR_KEY})"
if [[ "${CORRECT_CURSOR_KEY}" == sk-* ]]; then
  echo "FAIL: CORRECT_CURSOR_KEY looks like sk-… — need a crsr_… / cursor_… Integrations key" >&2
  exit 1
fi

"$DIF_HOME/scripts/guard-no-secret-leak.sh"

# Spend locks for the SDK spawn (cheapest model, hard time/token caps).
export LIVE_CURSOR_MODEL="${LIVE_CURSOR_MODEL:-composer-2.5}"
export CURSOR_RUN_TIMEOUT_MS="${CURSOR_RUN_TIMEOUT_MS:-480000}"
export CURSOR_MAX_TOTAL_TOKENS="${CURSOR_MAX_TOTAL_TOKENS:-150000}"
export CURSOR_HARD_DEADLINE_SEC="${CURSOR_HARD_DEADLINE_SEC:-720}"
export CURSOR_MAX_SENDS="${CURSOR_MAX_SENDS:-2}"
if [[ "$LIVE_CURSOR_MODEL" != "composer-2.5" ]]; then
  echo "FAIL: refusing LIVE_CURSOR_MODEL=$LIVE_CURSOR_MODEL (locked to composer-2.5)" >&2
  exit 1
fi

# First up.sh path creates orch .venv. Without python3.12-venv the venv is
# a python symlink and no pip; later checks treat that as "already there".
if ! python3.12 -c 'import ensurepip' >/dev/null 2>&1; then
  echo "==> installing python3.12-venv (ensurepip missing)"
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y python3.12-venv
fi

checkout() {
  local dir="$1" repo="$2"
  if [[ ! -d "$dir/.git" ]]; then
    mkdir -p "$(dirname "$dir")"
    git clone "https://github.com/jmjava/${repo}.git" "$dir"
  fi
  git -C "$dir" fetch origin "$BRANCH" main 2>/dev/null || git -C "$dir" fetch origin
  if git -C "$dir" rev-parse --verify "origin/$BRANCH" >/dev/null 2>&1; then
    git -C "$dir" checkout -B "$BRANCH" "origin/$BRANCH"
  else
    echo "WARN: $repo has no $BRANCH — staying on current branch" >&2
  fi
}

checkout "$DIF_HOME" embabel-dif
if [[ ! -d "$ORCH_HOME/.git" ]]; then
  mkdir -p "$(dirname "$ORCH_HOME")"
  git clone "https://github.com/jmjava/sdlc-spdd-orchestrator.git" "$ORCH_HOME"
fi
git -C "$ORCH_HOME" fetch origin main
git -C "$ORCH_HOME" checkout main
git -C "$ORCH_HOME" pull origin main || true
checkout "$DOGFOOD_HOME" dogfood-api

export DIF_HOME ORCH_HOME DOGFOOD_HOME
export DOGFOOD_ROOT="$DOGFOOD_HOME"

if [[ ! -f "$DOGFOOD_HOME/.cursor/commands/sdlc-next.md" ]]; then
  echo "==> full install (up.sh --setup-only)"
  "$DOGFOOD_HOME/scripts/up.sh" --setup-only --no-clone || \
    "$DOGFOOD_HOME/scripts/up.sh" --setup-only
fi

echo "==> spawn Cursor agent (CORRECT_CURSOR_KEY)"
set +e
"$DOGFOOD_HOME/scripts/agent-day/run.sh"
rc=$?
set -e
echo "==> Cursor day verdict (not just spawn)"
if [[ -x "$DOGFOOD_HOME/scripts/agent-day/status.sh" ]]; then
  "$DOGFOOD_HOME/scripts/agent-day/status.sh" || true
fi
if [[ "$rc" -ne 0 ]]; then
  echo "TEST=FAILED"
  exit "$rc"
fi
echo "TEST=WORKED"
exit 0
