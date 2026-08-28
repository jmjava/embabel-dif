#!/usr/bin/env bash
# Sibling dry-run: fold harvested orch canvases and trust .gate.json.
# Does not start Embabel, Guide, or sdlc-engine.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
out=$(mktemp -d)
cleanup() { rm -rf "$out" "${orch_out:-}"; }
trap cleanup EXIT

fold_code() {
  local canvas=$1
  local dest=$2
  set +e
  ./scripts/dif-fold.sh fold --canvas "$canvas" --out "$dest" >&2
  local code=$?
  set -e
  echo "$code"
}

assert_gate() {
  python3 - "$1" "$2" "${3:-}" <<'PY'
import json, sys
path, want_ready = sys.argv[1], sys.argv[2]
must_contain = sys.argv[3] if len(sys.argv) > 3 else ""
gate = json.load(open(path))
ready = gate["readyForImplementation"]
if str(ready).lower() != want_ready:
    raise SystemExit(f"{path}: readyForImplementation={ready}, want {want_ready}")
blob = json.dumps(gate)
if must_contain and must_contain not in blob:
    raise SystemExit(f"{path}: expected {must_contain!r} in {gate}")
print(f"gate {gate['workId']}: ready={ready} missing={gate['missingObligations']}")
PY
}

code=$(fold_code examples/canvases/FEAT-001-order-status-api.md "$out")
test "$code" = 0
assert_gate "$out/FEAT-001-order-status-api.gate.json" true T03
python3 - "$out/FEAT-001-order-status-api.gate.json" <<'PY'
import json, sys
gate = json.load(open(sys.argv[1]))
if gate["blockingConflicts"]:
    raise SystemExit(f"FEAT-001 unexpectedly blocking: {gate['blockingConflicts']}")
PY

code=$(fold_code examples/canvases/FEAT-099-pagination-conflict.md "$out")
test "$code" = 1
assert_gate "$out/FEAT-099-pagination-conflict.gate.json" false paginat
python3 - "$out/FEAT-099-pagination-conflict.gate.json" <<'PY'
import json, sys
gate = json.load(open(sys.argv[1]))
if not gate["blockingConflicts"]:
    raise SystemExit("FEAT-099 expected blockingConflicts")
PY

orch="${ORCH_HOME:-$HOME/github/jmjava/sdlc-spdd-orchestrator}"
orch_canvas="$orch/examples/spring-boot-order-api/spdd/canvas/FEAT-001-order-status-api.md"
if [[ -f "$orch_canvas" ]]; then
  orch_out=$(mktemp -d)
  code=$(fold_code "$orch_canvas" "$orch_out")
  test "$code" = 0
  assert_gate "$orch_out/FEAT-001-order-status-api.gate.json" true T03
fi

attach="$(cd "$(dirname "$0")" && pwd)/orch-attach"
set +e
skip_out=$(DIF_DISABLED=1 "${attach}/check-canvas.sh" --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out")
skip_code=$?
set -e
test "$skip_code" = 0
test "$skip_out" = "dif=skipped"

set +e
ready_out=$("${attach}/check-canvas.sh" --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out")
ready_code=$?
block_out=$("${attach}/check-canvas.sh" --canvas examples/canvases/FEAT-099-pagination-conflict.md --out "$out")
block_code=$?
set -e
test "$ready_code" = 0
test "$block_code" = 1
test "$ready_out" = "dif=ready workId=FEAT-001-order-status-api readyForImplementation=true"
test "$block_out" = "dif=blocked workId=FEAT-099-pagination-conflict readyForImplementation=false conflicts=1"
python3 - "$ready_out" "$block_out" <<'PY'
import sys
for line in sys.argv[1:]:
    if "\n" in line.strip():
        raise SystemExit(f"check-canvas must be one line, got {line!r}")
    if not line.startswith("dif="):
        raise SystemExit(f"expected dif= prefix, got {line!r}")
PY

echo "dif-orch-smoke: OK"
