# How `embabel-dif` could help `sdlc-spdd-orchestrator`

The orchestrator is an **installable operating model**: one Work ID, one REASONS
Canvas, one phase at a time, across Cursor / Copilot / Claude. It is not an
Embabel runtime.

This repo is a **semantic control-plane experiment**: fold observations into
typed intent, let Embabel plan over that state, and verify with deterministic
checks.

They answer different questions. Used together, DIF can make the canvas
*checkable* without replacing the canvas.

```text
Planning / requirements     why are we doing this?
REASONS Canvas              what must ship (human contract)
DIF SemanticModel           what must remain true (machine contract)
Embabel GOAP                what to do next (optional JVM path)
DICE / Guide graph          what did we learn before (retrieval)
SDLC phases                 who acts when
```

## Do not collapse the two repos

| Concern | Orchestrator | This repo |
| --- | --- | --- |
| Primary artifact | Markdown canvas + JSONL ledger | Typed Java records |
| Runtime | Assistant prompts + Python `sdlc-engine` | Embabel 1.5 + fold/verify |
| Must stay assistant-agnostic | Yes | No — Embabel is a later option |
| Source of truth | Canvas + `spdd/memory/lessons.jsonl` | Folded `SemanticModel` as a *projection* |

The orchestrator already uses **Guide DICE** as an optional working store
(`CONTEXT_BACKENDS=…,guide-dice`). DICE retrieves past decisions and pitfalls.
DIF does not retrieve; it **accepts facts and freezes them** into invariants
and conflicts. Those are complementary, not duplicates.

```text
DICE  = query what we already believe
DIF   = decide what must stay true, then verify it
```

Merging Embabel into the orchestrator would fight its design: “not a compiled
agent runtime.” The useful direction is the opposite: **keep DIF here**, expose
a fold/verify CLI or library, and let orchestrator phases *call* it.

## Where the orchestrator is still probabilistic

These phases already have strong *process* gates (`sdlc.sh gate …`). The
comparison work itself is still mostly “LLM, read the canvas and the diff”:

| Phase | Today | Gap DIF can close |
| --- | --- | --- |
| `/sdlc-spdd-plan` | Writes REASONS prose | Extract candidate `Intent` / `Evidence` |
| `/sdlc-spdd-architect` | Hardens canvas in prose | `ConflictDetector` before Ready For Coding |
| `/sdlc-spdd-code` | One T## operation | Invariants as hard constraints on the change |
| `/sdlc-spdd-review` | Checklist vs canvas | `IntentDiff` + `VerificationResult` |
| `/sdlc-spdd-sync` | Reconcile prose drift | Semantic drift vs incidental structure drift |
| `/sdlc-spdd-api-test` | Script per work | `VerificationStrategy.API_CONTRACT` / `JUNIT` |

Review already asks the agent to classify findings as implementation mismatch,
canvas/intent mismatch, or non-behavioral refactor. That taxonomy is DIF’s
intent / semantics / syntax split. The orchestrator names the distinction;
DIF can *compute* it.

## REASONS → DIF fold

A canvas is a good `CandidateIntent`. Fold it; do not replace it.

| Canvas section | DIF type | Example from `FEAT-001-order-status-api` |
| --- | --- | --- |
| R Acceptance Criteria | `Intent` `REQUIREMENT` | `GET /api/orders?email=` returns matches |
| R Non-Goals | `Intent` `CONSTRAINT` / `PRESERVATION` | No pagination, no auth changes, no schema migration |
| R Assumptions | `Evidence` + `Provenance` | `Order.customerEmail` already exists |
| E Entities / files | `SemanticNode` + `AFFECTS` / `IMPLEMENTS` | `OrderController`, `OrderService` |
| A Failure modes | `Invariant` + `CONFLICTS_WITH` | Controller must not call the repository |
| O Operations | obligations; open T## → `MissingObligation` | T03 docs still missing |
| N Norms | `Invariant` (`INTENT_DIFF` / `ARCHUNIT`) | No business logic in controller |
| S Safeguards | `Invariant` `REQUIRED` | Do not change auth; do not change unrelated APIs |

Folded, the order-status example is not “search orders by email.” It is:

```text
INT-001  Add GET /api/orders?email=
INT-002  Preserve existing auth behavior
INT-003  Preserve unrelated API endpoints
INT-004  Invalid email → 400
INV-001  OrderController contains no repository calls
INV-002  Service + WebMvcTest exist
MISSING  T03 documentation
```

Architect can refuse Ready For Coding when `hasBlockingConflicts()`.
Review can fail when a required path disappears in the semantic diff
(`provider.GOOGLE` in the prototype; `auth` / `unrelated endpoints` here).
Sync can see that T03 is a missing obligation, not a vibe.

## Suggested integration shape

Keep the canvas human-owned. Treat DIF output like SQLite and Guide:
**regenerable projection**, never a second source of truth.

```text
spdd/canvas/<WORK-ID>.md          human contract (commit)
        │
        ▼  fold (deterministic after accept)
.dif/projections/<WORK-ID>.json   SemanticModel + VerificationPlan
        │
        ├─ architect  conflict / missing-obligation gate
        ├─ code       constraint set in the operation prompt
        ├─ review     IntentDiff + VerificationResult
        └─ sync       semantic vs structural drift
```

Phase order that respects both repos:

1. **Here:** canvas → `SemanticModel` mapper + CLI (`dif fold --canvas …`).
   No Embabel required. Prove the same canvas always folds the same way.
2. **Orchestrator:** optional review/architect attachment — if `.dif/projections/`
   exists, cite `VerificationResult` instead of only prose findings.
3. **Here, later:** Embabel GOAP for JVM targets only, using the folded model
   as blackboard state. The orchestrator still decides *which Work ID / T##*
   is in play.
4. **Optional DICE:** project folded invariants into Guide as `Decision` /
   `Pitfall` nodes so retrieval and verification share vocabulary.

Do not make `sdlc-engine` depend on a JVM agent to run `next` / `gate`.

## What would count as success for the orchestrator

The prototype success criteria in `DIF_EMBABEL_PROTOTYPE.md` translate directly:

1. Two `/sdlc-spdd-plan` runs on the same accepted canvas produce the same model.
2. `/sdlc-spdd-review` can fail a required safeguard without asking an LLM
   “does this look correct?”
3. A coding session can change syntax (DTO names, test style) while
   `PRESERVE auth` and `PRESERVE unrelated endpoints` stay fixed.
4. An open operation (T03) shows up as `MissingObligation`, not a forgotten
   checklist box.
5. A mutually exclusive pair (“must paginate” vs “non-goal: pagination”)
   blocks Ready For Coding.

Until those are true, DIF stays a sibling experiment. After they are true, it
becomes a library the orchestrator can opt into the same way it opts into
Guide: useful when present, ignored when not.
