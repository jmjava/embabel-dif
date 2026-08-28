#!/usr/bin/env python3
"""Quote a DIF guide JSONL into a live Guide via orch's existing client.

Does not invent a second ingest path. Uses ContextStore.persist_lesson +
GuideClient.project_load / work_subgraph — the same APIs as
engine/tests_e2e/test_guide_projection_roundtrip.py.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import sys
import uuid
from pathlib import Path

ORCH = Path(os.environ.get("ORCH_HOME", Path.home() / "github/jmjava/sdlc-spdd-orchestrator"))
sys.path.insert(0, str(ORCH / "engine" / "src"))

from sdlc_engine.context_store import ContextStore  # noqa: E402
from sdlc_engine.guide_client import GuideClient, resolve_guide_base_url  # noqa: E402
from sdlc_engine.persistence import save_config  # noqa: E402
from sdlc_engine.project import Project  # noqa: E402


def main() -> int:
    jsonl_path = Path(sys.argv[1])
    canvas_path = Path(sys.argv[2])
    rows = [json.loads(line) for line in jsonl_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not rows:
        raise SystemExit(f"empty jsonl: {jsonl_path}")

    base = resolve_guide_base_url()
    client = GuideClient(base, timeout=10.0)
    if not client.health_ok():
        raise SystemExit(f"Guide not healthy at {base} — boot via orch test-guide-stack-live.sh")

    # Unique Work ID: Guide's graph already has FEAT-001 from orch's
    # spring-boot-order-api projection. Same as orch's live roundtrip.
    source_work = rows[0]["workId"]
    work_id = f"FEAT-DIF-LIVE-{uuid.uuid4().hex[:8]}"
    marker = f"DIF-LIVE-{uuid.uuid4().hex[:8]}"
    root = ORCH / ".sdlc" / "test-fixtures" / f"dif-live-{uuid.uuid4().hex[:8]}"
    root.mkdir(parents=True, exist_ok=True)
    try:
        req = root / "requirements" / "milestones" / f"{work_id}.md"
        req.parent.mkdir(parents=True, exist_ok=True)
        req.write_text(f"# Requirement: {work_id}\n\n## Summary\nDIF live quote of {source_work}.\n", encoding="utf-8")
        canvas = root / "spdd" / "canvas" / f"{work_id}.md"
        canvas.parent.mkdir(parents=True, exist_ok=True)
        canvas_text = canvas_path.read_text(encoding="utf-8")
        canvas_text = re.sub(r"(?m)^- Work ID:.*$", f"- Work ID: {work_id}", canvas_text, count=1)
        canvas.write_text(canvas_text, encoding="utf-8")
        memory = root / "spdd" / "memory"
        memory.mkdir(parents=True, exist_ok=True)
        index_path = memory / "context-index.md"
        index_path.write_text(
            "# Context Index\n\n"
            "| Area | Kind | Work ID | Phase | Timestamp | Source | Entry |\n"
            "|------|------|---------|-------|-----------|--------|-------|\n",
            encoding="utf-8",
        )
        save_config(
            root,
            {"backends": ["git-pointers", "sqlite", "guide-dice"], "guide_base_url": base},
        )
        store = ContextStore(Project(root))
        quoted = 0
        for row in rows:
            kind = "decision" if row.get("kind") == "Decision" else "pitfall"
            body = f"{marker} {row.get('text', '')}"
            store.persist_lesson(
                kind=kind,
                work_id=work_id,
                area="dif-fold",
                body=body,
                source="dif-live-e2e",
                accept=True,
                project_guide=False,
            )
            with index_path.open("a", encoding="utf-8") as fh:
                fh.write(f"| dif-fold | {kind} | {work_id} | test | 2026-08-28T00:00:00Z | dif-live-e2e | {body} |\n")
            quoted += 1
        loaded = client.project_load(str(root))
        if not loaded.get("ok"):
            raise SystemExit(f"project_load failed: {loaded}")
        subgraph = client.work_subgraph(work_id)
        blob = json.dumps(subgraph)
        if marker not in blob:
            raise SystemExit(
                f"DIF quote not in work subgraph for {work_id}: load={loaded} subgraph={blob[:2000]}"
            )
        if source_work.startswith("FEAT-001") and "T03" not in blob and "Document API" not in blob:
            raise SystemExit(f"expected T03 obligation in subgraph: {blob[:2000]}")
        print(
            f"dif-live-guide-quote: OK quoted={quoted} source={source_work} "
            f"workId={work_id} marker={marker}"
        )
        return 0
    finally:
        shutil.rmtree(root, ignore_errors=True)


if __name__ == "__main__":
    raise SystemExit(main())
