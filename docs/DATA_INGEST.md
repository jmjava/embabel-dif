# How data gets into the models

The pairing is Embabel DIF. This page is the ingest map: **what is allowed
to write each type, and what is not.** Shapes live in the README mermaid.
How the fold grows lives in [FOLD_ITERATION.md](FOLD_ITERATION.md).

Nothing invents a second source of truth. The REASONS Canvas (or an
accepted `ChangeRequest`) is the only human input. Every other model is
derived, quoted, or planned.

---

## The rule

```text
human writes          →  canvas markdown  or  change-request text
parser / interpreter  →  ReasonsCanvas    or  CandidateIntent
folder                →  SemanticModel          (the only freeze)
CLI / planner         →  GateReport, FoldProjection, VerificationPlan
optional quote        →  GuideLedger JSONL  →  LessonRecord  →  Neo4j
```

If a field is not in that chain, it is not in the model. User Goal
prose, commit messages, and Guide retrieval do not write intents.

---

## Hop 1 — markdown becomes `ReasonsCanvas`

**Writer:** `ReasonsCanvasParser`. **Input:** accepted REASONS markdown.
**Output:** `ReasonsCanvas`. The file stays the contract; this record is
only the structured view.

| Canvas heading | Lands on | Ignored if |
| --- | --- | --- |
| Metadata `Work ID` / title `FEAT-…` | `workId` | blank → `UNKNOWN` |
| Metadata `Readiness` / `Status` | `readiness` | other metadata keys |
| **R** → Acceptance Criteria bullets | `acceptanceCriteria` | User Goal / Business Goal prose |
| **R** → Non-Goals bullets | `nonGoals` | — |
| **R** → Assumptions bullets | `assumptions` | — |
| **S** safeguard bullets | `safeguards` | — |
| **N** norm bullets | `norms` | — |
| **E** domain entities | `entities` | — |
| **E** files likely affected | `filesLikelyAffected` | backticks stripped |
| **O** `### Tnn - name` | `operations` | H3 that is not `Tnn` |
| Unknown `##` / `###` | — | dropped |

Checkboxes (`- [ ]`) are stripped. Operation `Status: Complete` /
`Done` marks the op complete; anything else stays open.

Orch does not parse this. It stores the file under
`spdd/canvas/<WORK-ID>.md` and later reads a gate line.

---

## Hop 2 — canvas becomes `CandidateIntent`

**Writer:** `CanvasIntentMapper`. No LLM. **Heading decides type**, not
the wording of the bullet.

| `ReasonsCanvas` field | `CandidateIntent` | Provenance |
| --- | --- | --- |
| `acceptanceCriteria` | `Intent` `REQUIREMENT` | `ISSUE` / `WORK-ID:R` |
| `nonGoals` | `Intent` `CONSTRAINT` (`Non-goal: …`) | `ISSUE` / `WORK-ID:R-nongoal` |
| `safeguards` | `Intent` `PRESERVATION` | `ADR` / `WORK-ID:S` |
| `norms` | `Intent` `PRESERVATION` | `ADR` / `WORK-ID:N` |
| `assumptions` | `Evidence` `DOCUMENTATION` | `WORK-ID:assumption` |
| `entities` | `Evidence` `SOURCE_CODE` | `WORK-ID:entity` |
| `filesLikelyAffected` | `Evidence` `SOURCE_CODE` | the file path |
| `operations` | *not here* | open T## become obligations in hop 3b |

The other way into `CandidateIntent` is Embabel’s interpreter, not the
parser:

| Writer | When | What it writes |
| --- | --- | --- |
| `FixtureIntentInterpreter` | request text matches refresh-token rotation | `RefreshTokenScenario.candidateIntent()` (five intents, nine evidence rows) |
| `LlmIntentInterpreter` | any other `ChangeRequest` | LLM-shaped `CandidateIntent` (not yet accepted) |

Both still have to pass hop 3. The LLM does not write `SemanticModel`.

---

## Hop 3 — candidate becomes `SemanticModel`

**Writer:** `RuleBasedIntentFolder` (the `IntentFolder` seam).
**Input:** `CandidateIntent` only. It never opens the markdown file.

| Model field | How it gets there |
| --- | --- |
| `intents` | canonicalize: collapse whitespace, dedupe by normalized statement, keep first id/type/priority/provenance |
| `invariants` | token rules (`refresh`+`rotat`, `google`, `apple`, `sessionToken`, authorization code); else `Preserve: …` for `PRESERVATION`; else the requirement text. **Non-goals do not become invariants.** |
| `relations` | evidence↔intent on shared tokens; invariant↔intent (`PRESERVES` or `DERIVED_FROM`); conflict pairs (`CONFLICTS_WITH`) |
| `conflicts` | `ConflictDetector`: single-use vs reusable; `REQUIREMENT`/`GOAL` vs `CONSTRAINT` sharing a significant token; optional formal backend for the rest |
| `missingObligations` | empty here. Hop 3b fills them. |

Same accepted candidate → same model. That is the freeze.

### Hop 3b — canvas folder adds absence

**Writer:** `CanvasFolder` after the folder returns.

| Source | Lands as |
| --- | --- |
| `CanvasOperation` not complete | `MissingObligation(Tnn - name: description, workId)` |
| `ObligationDeriver` on the folded intents | implied holes (`rotation integration test`, `idempotency key`, …) when repository evidence does not already say they exist |

`VerificationPlanner` may merge more derived holes when Embabel (or
`dif-fold plan`) has a `RepositoryAnalysis`. It does not re-parse
markdown.

---

## Hop 4 — the freeze is projected, not rewritten

These writers **read** `SemanticModel`. They do not classify.

| Writer | Writes | Used by |
| --- | --- | --- |
| `DifCli.writeFold` | `.dif/projections/<WORK-ID>.json` (`FoldProjection`) | `architect --projection`, `plan --projection` |
| `GateReport.from` | `.gate.json` + `dif=ready\|blocked` one-liner | orch `check-canvas.sh` |
| `VerificationPlanner` | `VerificationPlan` (rules = one per invariant) | Embabel goal; `dif-fold plan` |
| `GuideLedger.jsonl` | JSONL `Decision` (invariants) / `Pitfall` (conflicts + missing) | optional Guide quote |
| `SafeguardPaths` | review snapshot paths from **S** lines | `check-review.sh` |

Orch’s process gate (`sdlc.sh gate`) still only asks whether files
exist. The semantic gate is the one-liner.

---

## Hop 5 — optional quote into Guide, never a freeze

**Writer:** `scripts/dif-live-guide-quote.py` using orch’s existing
`ContextStore.persist_lesson` + `GuideClient.project_load`.

```text
GuideLedger JSONL
    → LessonRecord (kind=decision|pitfall, area=dif-fold, accept=true)
    → context-index.md row
    → project_load
    → Neo4j NamedEntity (WorkId, Decision, Pitfall)
    → work_subgraph retrieve
```

Guide does not write `SemanticModel`. A later retrieve must not be
treated as newly accepted intent. Unique live Work IDs
(`FEAT-DIF-LIVE-…`) avoid colliding with orch’s already-projected
`FEAT-001`.

---

## Embabel’s ingest (optional JVM path)

Embabel does not have its own IR. It puts DIF types on a blackboard
and GOAP-orders the actions.

```text
UserInput
  → captureRequest          ChangeRequest(text)
  → interpretIntent         CandidateIntent     (fixture or LLM)
  → foldIntent              SemanticModel       (calls IntentFolder)
  → analyzeRepository       RepositoryAnalysis  (fixture evidence today)
  → planVerification        VerificationPlan    (VerificationPlanner)
```

`foldIntent` is a one-line call. Classify / conflict / obligation stay
behind `IntentFolder`. `RepositoryAnalyzer` today returns
`RefreshTokenScenario.repositoryAnalysis()` for the known wording;
other requests get an empty analysis. That is a stub, not a second
fold.

A day that never boots Embabel still has a complete ingest through
hops 1–4.

---

## What never writes a model

| Tempting source | Why it does not write |
| --- | --- |
| User Goal / Business Goal prose | parser only takes mapped bullets |
| Guide `work_subgraph` | retrieve-only |
| `lessons.jsonl` / SQLite | orch memory; may quote a fold, not freeze one |
| `sdlc.sh next` | process, not ingest |
| LLM in `LlmIntentInterpreter` | writes `CandidateIntent` only; fold still required |
| Alloy `.als` | emit-only from an already-folded model |
| A second Neo4j / second ingest | forbidden; live E2E reuses orch’s Guide |

---

## Will ingest evolve?

The **hops** stay. New data gets in by:

1. A new mapped canvas section or bullet rule (`ReasonsCanvasParser` /
   `CanvasIntentMapper`) with a failing canvas test.
2. A new derive / conflict / obligation rule behind `IntentFolder`.
3. A richer `RepositoryAnalyzer` that adds evidence, not intents.

Do not add a hop. Do not let Guide or Embabel become a writer of
`SemanticModel`. Tests that lock the hops are
`src/test/java/com/embabel/dif/ingest/DataModelIngestTest.java`.
