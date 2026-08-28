#!/usr/bin/env bash
# Live three-way E2E: reuse orch's Guide+Neo4j harness, then DIF fold + Embabel platform.
# Does not put Embabel or Guide inside sdlc.sh next.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
orch="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
export GUIDE_HOME="${GUIDE_HOME:-$HOME/github/jmjava/orch-guide}"
export GUIDE_PORT="${GUIDE_PORT:-21337}"
export GUIDE_BASE_URL="${GUIDE_BASE_URL:-http://127.0.0.1:${GUIDE_PORT}}"
export ORCH_HOME="$orch"

echo "== 0. process gate: sdlc-engine is not a JVM =="
command -v sdlc-engine >/dev/null
sdlc-engine --version
if sdlc-engine --help 2>&1 | grep -qi 'spring\|embabel'; then
  echo "FAIL: sdlc-engine help mentions Embabel/Spring" >&2
  exit 1
fi

echo "== 1. Neo4j (already the orch/Guide graph) =="
neo_status="$(sudo docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' embabel-neo4j 2>/dev/null || true)"
if [[ "${neo_status}" != "healthy" ]]; then
  echo "neo4j not healthy (got ${neo_status:-missing}); starting via orch installer"
fi

echo "== 2. boot Guide with orch's live harness if needed =="
if curl -sf --max-time 3 "${GUIDE_BASE_URL}/actuator/health" >/dev/null; then
  echo "Guide already healthy at ${GUIDE_BASE_URL}"
else
  if [[ ! -x "${orch}/tests/test-guide-stack-live.sh" ]]; then
    echo "FAIL: missing ${orch}/tests/test-guide-stack-live.sh" >&2
    exit 1
  fi
  echo "calling orch test-guide-stack-live.sh (keep running; skip extra pytest/playwright)"
  SDLC_GUIDE_STACK_LIVE=1 \
    SDLC_GUIDE_INTEGRATION=0 \
    SDLC_GUIDE_E2E=0 \
    GUIDE_KEEP=1 \
    GUIDE_HOME="${GUIDE_HOME}" \
    GUIDE_PORT="${GUIDE_PORT}" \
    GUIDE_START_TIMEOUT_SEC="${GUIDE_START_TIMEOUT_SEC:-900}" \
    "${orch}/tests/test-guide-stack-live.sh"
fi
curl -sf --max-time 3 "${GUIDE_BASE_URL}/actuator/health" >/dev/null

echo "== 3. scripted DIF day (fold / architect / review / plan) =="
./scripts/dif-orch-day.sh

echo "== 4. quote DIF JSONL through orch GuideClient =="
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
./scripts/dif-fold.sh guide --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out" >/dev/null
python3 ./scripts/dif-live-guide-quote.py \
  "$out/FEAT-001-order-status-api.guide.jsonl" \
  examples/canvases/FEAT-001-order-status-api.md

echo "== 5. live Embabel Spring platform (no OpenAI starter; fixture path) =="
# OPENAI_API_KEY auto-activates embabel-agent-starter-openai, which fails to
# load ChatClient auto-config here. Live Embabel tonight is the GOAP platform,
# not an LLM call.
env -u OPENAI_API_KEY -u ANTHROPIC_API_KEY \
  DIF_LIVE_EMBABEL=1 \
  ./mvnw -q -P-openai-models -P-anthropic-models -Dtest=EmbabelLivePlatformTest test

echo "dif-live-e2e: OK"
