#!/usr/bin/env python3
"""Full flow example: REASONS change → fold → orch Dashboard readout.

Does not start a fold, a JVM, or Embabel from Refresh. The board only
reads ``.dif/projections/<WORK-ID>.gate.json`` that ``dif-fold.sh``
already wrote.

Requires orch on a revision that implements ``read_dif_gate``
(``cursor/dif-dashboard-148e`` / PR 210). Set ``ORCH_HOME``.

Set ``DIF_DASHBOARD_PLAYWRIGHT=1`` to also click Refresh in the Vue
Dashboard (needs ``console-ui/dist`` + Playwright Chromium).
"""
from __future__ import annotations

import json
import os
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path

WORK_ID = "FEAT-DASH-flow"


def _repo() -> Path:
    return Path(__file__).resolve().parents[1]


def _orch() -> Path:
    env = os.environ.get("ORCH_HOME")
    if env:
        return Path(env).expanduser().resolve()
    sibling = Path.home() / "github/jmjava/sdlc-spdd-orchestrator"
    if sibling.is_dir():
        return sibling
    raise SystemExit("ORCH_HOME is not set and sibling orch checkout is missing")


def _fold(canvas: Path, out: Path) -> str:
    script = _repo() / "scripts" / "dif-fold.sh"
    proc = subprocess.run(
        [str(script), "architect", "--quiet", "--canvas", str(canvas), "--out", str(out)],
        cwd=_repo(),
        capture_output=True,
        text=True,
        check=False,
    )
    line = (proc.stdout or "").strip().splitlines()[-1] if proc.stdout.strip() else ""
    return line


def _seed(root: Path, canvas_src: Path) -> None:
    req = root / "requirements" / "milestones" / f"{WORK_ID}.md"
    req.parent.mkdir(parents=True, exist_ok=True)
    req.write_text(
        (_repo() / "examples/dashboard-flow/requirement.md").read_text(encoding="utf-8"),
        encoding="utf-8",
    )
    canvas = root / "spdd" / "canvas" / f"{WORK_ID}.md"
    canvas.parent.mkdir(parents=True, exist_ok=True)
    canvas.write_text(canvas_src.read_text(encoding="utf-8"), encoding="utf-8")
    memory = root / "spdd" / "memory"
    memory.mkdir(parents=True, exist_ok=True)
    (memory / "lessons.jsonl").write_text("", encoding="utf-8")
    (memory / "registry.jsonl").write_text(
        json.dumps(
            {
                "event": "claim",
                "work_id": WORK_ID,
                "status": "active",
                "phase": "architect",
                "operation": "T01",
                "owner": "dif-dashboard-e2e",
                "note": "dashboard full-flow example",
                "ts": "2026-08-29T01:00:00Z",
            }
        )
        + "\n",
        encoding="utf-8",
    )
    sdlc = root / ".sdlc"
    sdlc.mkdir(parents=True, exist_ok=True)
    (sdlc / "pointer").write_text(WORK_ID + "\n", encoding="utf-8")
    staged = sdlc / "staged"
    staged.mkdir(parents=True, exist_ok=True)
    (staged / "lessons.jsonl").write_text("", encoding="utf-8")
    workflows = sdlc / "workflows"
    workflows.mkdir(parents=True, exist_ok=True)
    (workflows / f"{WORK_ID}.history").write_text(
        "2026-08-29T01:00:00Z\tcreate\twork_id=" + WORK_ID + "\n",
        encoding="utf-8",
    )


def _import_orch(orch: Path) -> None:
    src = orch / "engine" / "src"
    if not src.is_dir():
        raise SystemExit(f"orch engine missing at {src}")
    sys.path.insert(0, str(src))


def _require_dif_gate() -> None:
    from sdlc_engine.installer import dashboard as dash

    if not hasattr(dash, "read_dif_gate"):
        raise SystemExit(
            "orch dashboard has no read_dif_gate — use ORCH_HOME on "
            "cursor/dif-dashboard-148e (PR 210) or later"
        )


def _client(root: Path):
    from sdlc_engine.installer.app import create_app

    app = create_app(root, vue_dist=False)
    return app.test_client()


def _post(client, path: str, root: Path) -> dict:
    res = client.post(path, json={"target": str(root)})
    if res.status_code != 200:
        raise SystemExit(f"{path} -> {res.status_code} {res.get_data(as_text=True)[:800]}")
    return res.get_json()


def _stage_adhoc(root: Path) -> None:
    path = root / ".sdlc" / "staged" / "lessons.jsonl"
    rec = {
        "id": f"pitfall:{WORK_ID}:dashboard:adhoc-prompt",
        "kind": "pitfall",
        "work_id": WORK_ID,
        "area": "dashboard",
        "phase": "",
        "ts": "2026-08-29T01:10:00Z",
        "title": "Pagination as requirement and non-goal blocked Ready For Coding",
        "body": "Pagination as requirement and non-goal blocked Ready For Coding",
        "source": "adhoc-prompt",
        "keywords": ["pagination"],
        "commit": "",
        "schema": 1,
    }
    path.write_text(json.dumps(rec) + "\n", encoding="utf-8")


def _playwright(root: Path, orch: Path, expect_blocked: bool) -> None:
    dist = orch / "console-ui" / "dist"
    if not (dist / "index.html").is_file():
        print("playwright skipped: console-ui/dist missing (npm ci && npm run build)")
        return
    try:
        from playwright.sync_api import sync_playwright
        from sdlc_engine.installer.app import create_app
        from werkzeug.serving import make_server
    except ImportError as exc:
        print(f"playwright skipped: {exc}")
        return

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind(("127.0.0.1", 0))
    port = int(sock.getsockname()[1])
    sock.close()
    app = create_app(root, vue_dist=dist)
    server = make_server("127.0.0.1", port, app)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        time.sleep(0.3)
        with sync_playwright() as pw:
            browser = pw.chromium.launch()
            page = browser.new_page()
            page.goto(f"http://127.0.0.1:{port}/?target={root}", wait_until="domcontentloaded")
            page.get_by_test_id("dashboard-panel").wait_for(state="visible")
            page.get_by_test_id("btn-dash-refresh").click()
            page.wait_for_function(
                """() => (document.querySelector('[data-testid="dash-status"]')?.textContent || '')
                  .includes('Loaded')"""
            )
            chip = page.get_by_test_id("dash-work-dif")
            chip.wait_for(state="visible")
            text = chip.inner_text()
            if expect_blocked:
                assert "dif=blocked" in text, text
                suggestions = page.get_by_test_id("dash-suggestions").inner_text()
                assert "fix the REASONS canvas" in suggestions, suggestions
            else:
                assert "dif=ready" in text, text
            browser.close()
        print(f"playwright: OK ({'blocked' if expect_blocked else 'ready'})")
    finally:
        server.shutdown()


def main() -> int:
    orch = _orch()
    _import_orch(orch)
    _require_dif_gate()

    root = Path(os.environ.get("DASH_E2E_ROOT") or "").expanduser() if os.environ.get("DASH_E2E_ROOT") else None
    cleanup = False
    if root is None:
        import tempfile

        root = Path(tempfile.mkdtemp(prefix="dif-dash-e2e-"))
        cleanup = os.environ.get("DASH_E2E_KEEP") != "1"
    examples = _repo() / "examples" / "dashboard-flow"
    out = root / ".dif" / "projections"
    out.mkdir(parents=True, exist_ok=True)

    print("== 1. Jira-shaped stub + blocked REASONS (pagination vs non-goal) ==")
    _seed(root, examples / "FEAT-DASH-flow.blocked.md")
    line = _fold(examples / "FEAT-DASH-flow.blocked.md", out)
    print(line)
    if "dif=blocked" not in line:
        raise SystemExit(f"expected blocked fold, got {line!r}")

    print("== 2. Dashboard reads .gate.json (no fold from Refresh) ==")
    client = _client(root)
    status = _post(client, "/api/dashboard/status", root)
    dif = (status.get("work") or {}).get("dif") or {}
    if not dif.get("present") or dif.get("ready") is not False:
        raise SystemExit(f"dashboard missed blocked gate: {dif}")
    if "dif=blocked" not in str(dif.get("line")):
        raise SystemExit(f"bad gate line: {dif}")
    suggestions = _post(client, "/api/dashboard/suggestions", root).get("suggestions") or []
    by_id = {row.get("id"): row for row in suggestions}
    if "dif-blocked" not in by_id:
        raise SystemExit(f"Today missing dif-blocked: {list(by_id)}")
    print(f"Today: {by_id['dif-blocked']['text']}")

    if os.environ.get("DIF_DASHBOARD_PLAYWRIGHT") == "1":
        print("== 2b. Playwright Refresh on blocked canvas ==")
        _playwright(root, orch, expect_blocked=True)

    print("== 3. Contract changes (Jira/REASONS): drop conflicting non-goal, re-fold ==")
    canvas = root / "spdd" / "canvas" / f"{WORK_ID}.md"
    canvas.write_text(
        (examples / "FEAT-DASH-flow.ready.md").read_text(encoding="utf-8"),
        encoding="utf-8",
    )
    line = _fold(examples / "FEAT-DASH-flow.ready.md", out)
    print(line)
    if "dif=ready" not in line:
        raise SystemExit(f"expected ready fold, got {line!r}")

    print("== 4. Dashboard refresh sees the new freeze ==")
    status = _post(client, "/api/dashboard/status", root)
    dif = (status.get("work") or {}).get("dif") or {}
    if not dif.get("present") or dif.get("ready") is not True:
        raise SystemExit(f"dashboard missed ready gate: {dif}")
    suggestions = _post(client, "/api/dashboard/suggestions", root).get("suggestions") or []
    if any(row.get("id") == "dif-blocked" for row in suggestions):
        raise SystemExit("Today still has dif-blocked after the contract change")
    print(f"Active work: {dif.get('line')}")

    print("== 5. Ad hoc harvest lands on Memory (staged), not a fold ==")
    _stage_adhoc(root)
    memory = _post(client, "/api/dashboard/status", root).get("memory") or {}
    if int(memory.get("staged_count") or 0) < 1:
        raise SystemExit(f"expected staged lesson, got {memory}")
    print(f"Memory staged={memory.get('staged_count')} accepted={memory.get('accepted_count')}")

    if os.environ.get("DIF_DASHBOARD_PLAYWRIGHT") == "1":
        print("== 5b. Playwright Refresh on ready canvas ==")
        _playwright(root, orch, expect_blocked=False)

    print(f"dif-dashboard-e2e: OK workId={WORK_ID} target={root}")
    if cleanup:
        import shutil

        shutil.rmtree(root, ignore_errors=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
