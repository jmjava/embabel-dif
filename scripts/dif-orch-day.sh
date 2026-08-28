#!/usr/bin/env bash
# One scripted developer day: plan → fold → architect → review.
# No Embabel process. No Guide MCP. sdlc.sh next is not replaced.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
attach="$(cd "$(dirname "$0")" && pwd)/orch-attach"
fold=./scripts/dif-fold.sh

echo "== 1. same accepted canvas folds the same way twice =="
"$fold" fold --quiet --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out/a"
"$fold" fold --quiet --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out/b"
cmp "$out/a/FEAT-001-order-status-api.gate.json" "$out/b/FEAT-001-order-status-api.gate.json"

echo "== 2. architect: FEAT-001 ready, T03 missing =="
ready=$("$fold" architect --quiet --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out")
test "$ready" = "dif=ready workId=FEAT-001-order-status-api readyForImplementation=true"
python3 - "$out/FEAT-001-order-status-api.gate.json" <<'PY'
import json, sys
gate = json.load(open(sys.argv[1]))
assert gate["readyForImplementation"] is True
assert any("T03" in row for row in gate["missingObligations"])
PY

echo "== 3. architect: FEAT-099 blocked =="
set +e
blocked=$("$fold" architect --quiet --canvas examples/canvases/FEAT-099-pagination-conflict.md --out "$out")
block_code=$?
set -e
test "$block_code" = 1
test "$blocked" = "dif=blocked workId=FEAT-099-pagination-conflict readyForImplementation=false conflicts=1"

echo "== 4. review: orch order-status auth drop fails =="
set +e
auth=$("$fold" review --quiet \
  --canvas examples/canvases/FEAT-001-order-status-api.md \
  --before examples/snapshots/order-status-before.json \
  --after examples/snapshots/order-status-auth-broken.json)
auth_code=$?
set -e
test "$auth_code" = 1
test "$auth" = "dif=blocked workId=FEAT-001-order-status-api passed=false"

echo "== 5. review: DTO rename still passes =="
syntax=$("$fold" review --quiet \
  --canvas examples/canvases/FEAT-001-order-status-api.md \
  --before examples/snapshots/order-status-before.json \
  --after examples/snapshots/order-status-syntax-ok.json)
test "$syntax" = "dif=ready workId=FEAT-001-order-status-api passed=true"

echo "== 6. plan from projection (no markdown re-parse) =="
plan=$("$fold" plan --quiet --projection "$out/FEAT-001-order-status-api.json" --out "$out")
test "$plan" = "dif=ready workId=FEAT-001-order-status-api readyForImplementation=true"
python3 - "$out/FEAT-001-order-status-api.plan.json" <<'PY'
import json, sys
plan = json.load(open(sys.argv[1]))
assert plan["readyForImplementation"] is True
blob = json.dumps(plan)
assert "T03" in blob
PY

echo "== 7. guide JSONL is optional quote, not a gate =="
"$fold" guide --canvas examples/canvases/FEAT-099-pagination-conflict.md --out "$out" >/dev/null
grep -q '"kind":"Pitfall"' "$out/FEAT-099-pagination-conflict.guide.jsonl"

echo "== 8. missing CLI / missing snapshots are skip =="
skip=$(DIF_DISABLED=1 "${attach}/check-canvas.sh" --canvas examples/canvases/FEAT-001-order-status-api.md --out "$out")
test "$skip" = "dif=skipped"
skip_rev=$(DIF_DISABLED=1 "${attach}/check-review.sh" \
  --before examples/snapshots/order-status-before.json \
  --after examples/snapshots/order-status-auth-broken.json \
  --canvas examples/canvases/FEAT-001-order-status-api.md)
test "$skip_rev" = "dif=skipped"
skip_nosnap=$("${attach}/check-review.sh" --before "$out/missing-before.json" --after "$out/missing-after.json")
test "$skip_nosnap" = "dif=skipped"

echo "== 9. check-review fail-closed when snapshots exist =="
set +e
hook=$("${attach}/check-review.sh" \
  --before examples/snapshots/order-status-before.json \
  --after examples/snapshots/order-status-auth-broken.json \
  --canvas examples/canvases/FEAT-001-order-status-api.md)
hook_code=$?
set -e
test "$hook_code" = 1
test "$hook" = "dif=blocked workId=FEAT-001-order-status-api passed=false"

echo "dif-orch-day: OK"
