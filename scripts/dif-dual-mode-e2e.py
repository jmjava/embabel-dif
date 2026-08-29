#!/usr/bin/env python3
"""Both usage modes on one consumer tree.

Structured: real Work ID + canvas + fold + Dashboard chip.
Unstructured: persist-lesson kind+area+body with no FEAT.

Does not start Embabel. Dashboard Refresh is not invoked as a folder.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

WORK_ID = "FEAT-001-order-status-api"


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


def _dogfood() -> Path | None:
    env = os.environ.get("DOGFOOD_HOME")
    if env:
        path = Path(env).expanduser().resolve()
        return path if path.is_dir() else None
    sibling = Path.home() / "github/jmjava/dogfood-api"
    return sibling if sibling.is_dir() else None


def _py() -> str:
    for candidate in (
        Path("/tmp/orch-venv/bin/python"),
        _orch() / ".venv/bin/python",
    ):
        if candidate.is_file() and os.access(candidate, os.X_OK):
            return str(candidate)
    return os.environ.get("PYTHON", "python3")


def _engine(root: Path, *args: str) -> subprocess.CompletedProcess[str]:
    orch = _orch()
    env = os.environ.copy()
    env["PYTHONPATH"] = str(orch / "engine" / "src") + (
        os.pathsep + env["PYTHONPATH"] if env.get("PYTHONPATH") else ""
    )
    return subprocess.run(
        [_py(), "-m", "sdlc_engine", "--root", str(root), *args],
        cwd=orch,
        capture_output=True,
        text=True,
        check=False,
        env=env,
    )


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
    if "dif=ready" not in line:
        raise SystemExit(f"structured fold failed: {line!r}\n{proc.stderr}")
    return line


def _canvas() -> Path:
    dog = _dogfood()
    if dog is not None:
        live = dog / "sdlc-spdd" / "spdd" / "canvas" / f"{WORK_ID}.md"
        if live.is_file():
            return live
    return _repo() / "examples" / "canvases" / f"{WORK_ID}.md"


def _seed(root: Path, canvas: Path) -> None:
    dog = _dogfood()
    req_src = None
    if dog is not None:
        cand = dog / "sdlc-spdd" / "requirements" / "milestones" / f"{WORK_ID}.md"
        if cand.is_file():
            req_src = cand
    req = root / "sdlc-spdd" / "requirements" / "milestones" / f"{WORK_ID}.md"
    req.parent.mkdir(parents=True, exist_ok=True)
    if req_src is not None:
        req.write_text(req_src.read_text(encoding="utf-8"), encoding="utf-8")
    else:
        req.write_text(f"# Requirement: {WORK_ID}\n", encoding="utf-8")
    dest = root / "sdlc-spdd" / "spdd" / "canvas" / f"{WORK_ID}.md"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(canvas.read_text(encoding="utf-8"), encoding="utf-8")
    memory = root / "sdlc-spdd" / "spdd" / "memory"
    memory.mkdir(parents=True, exist_ok=True)
    (memory / "lessons.jsonl").write_text("", encoding="utf-8")
    (memory / "registry.jsonl").write_text(
        json.dumps(
            {
                "event": "claim",
                "work_id": WORK_ID,
                "status": "active",
                "phase": "architect",
                "operation": "T03",
                "owner": "dual-mode-e2e",
                "note": "structured + unstructured on one tree",
                "ts": "2026-08-29T02:00:00Z",
            }
        )
        + "\n",
        encoding="utf-8",
    )
    sdlc = root / "sdlc-spdd" / ".sdlc"
    sdlc.mkdir(parents=True, exist_ok=True)
    (sdlc / "pointer").write_text(WORK_ID + "\n", encoding="utf-8")
    (sdlc / "staged").mkdir(parents=True, exist_ok=True)


def _import_orch() -> None:
    src = _orch() / "engine" / "src"
    sys.path.insert(0, str(src))


def main() -> int:
    _import_orch()
    from sdlc_engine.installer.app import create_app

    canvas = _canvas()
    print(f"canvas: {canvas}")
    root = Path(tempfile.mkdtemp(prefix="dif-dual-mode-"))
    keep = os.environ.get("DUAL_E2E_KEEP") == "1"
    try:
        _seed(root, canvas)
        out = root / ".dif" / "projections"
        out.mkdir(parents=True, exist_ok=True)

        print("== 1. structured: fold FEAT-001 ==")
        line = _fold(canvas, out)
        print(line)
        if f"workId={WORK_ID}" not in line:
            raise SystemExit("structured fold used the wrong Work ID")

        print("== 2. structured: persist-lesson with Work ID ==")
        structured = _engine(
            root,
            "context",
            "persist-lesson",
            "--kind",
            "decision",
            "--work-id",
            WORK_ID,
            "--area",
            "api",
            "--source",
            "dual-mode-e2e",
            "--body",
            "GET /api/orders?email= stays unpaginated; invalid email is 400.",
            "--no-guide",
        )
        if structured.returncode != 0:
            raise SystemExit(structured.stderr or structured.stdout)
        print(structured.stdout.strip().splitlines()[-1] if structured.stdout.strip() else "ok")

        print("== 3. unstructured: persist-lesson without Work ID ==")
        unstructured = _engine(
            root,
            "context",
            "persist-lesson",
            "--kind",
            "pitfall",
            "--area",
            "notify",
            "--source",
            "adhoc-prompt",
            "--body",
            "Retry without an idempotency key double-posts webhook deliveries.",
            "--no-guide",
        )
        if unstructured.returncode != 0:
            raise SystemExit(
                "unstructured persist failed (orch must allow --area without --work-id):\n"
                + (unstructured.stderr or unstructured.stdout)
            )
        payload = json.loads(unstructured.stdout)
        rec_id = (payload.get("git") or {}).get("id") or ""
        if rec_id != "pitfall:(none):notify:adhoc-prompt":
            raise SystemExit(f"expected unscoped id, got {rec_id!r}")
        if "FEAT-ADHOC" in unstructured.stdout or "FEAT-ADHOC" in rec_id:
            raise SystemExit("unstructured capture invented a FEAT")

        print("== 4. retrieve both modes ==")
        by_work = json.loads(_engine(root, "context", "retrieve", "--work-id", WORK_ID).stdout)
        by_area = json.loads(_engine(root, "context", "retrieve", "--area", "notify").stdout)
        work_ids = [row["id"] for row in by_work.get("ledger") or []]
        area_ids = [row["id"] for row in by_area.get("ledger") or []]
        if not any(WORK_ID in i for i in work_ids):
            raise SystemExit(f"structured retrieve missed Work ID: {work_ids}")
        if rec_id not in area_ids:
            raise SystemExit(f"unstructured retrieve --area missed {rec_id}: {area_ids}")
        if any(row.get("work_id") for row in by_area.get("ledger") or [] if row["id"] == rec_id):
            raise SystemExit("unstructured row should have empty work_id")

        print("== 5. Dashboard reads structured gate; Memory sees ad hoc ==")
        app = create_app(root, vue_dist=False)
        client = app.test_client()
        status = client.post("/api/dashboard/status", json={"target": str(root)}).get_json()
        dif = (status.get("work") or {}).get("dif") or {}
        if not dif.get("present") or dif.get("ready") is not True:
            raise SystemExit(f"dashboard missed structured gate: {dif}")
        memory = status.get("memory") or {}
        if int(memory.get("staged_count") or 0) < 2:
            raise SystemExit(f"expected both staged harvests, got {memory}")
        print(f"Active work: {dif.get('line')}")
        print(f"Memory staged={memory.get('staged_count')}")

        print(f"dif-dual-mode-e2e: OK workId={WORK_ID} adhoc={rec_id} target={root}")
        return 0
    finally:
        if not keep:
            shutil.rmtree(root, ignore_errors=True)


if __name__ == "__main__":
    raise SystemExit(main())
