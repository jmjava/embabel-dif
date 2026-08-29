# embabel-dif

Prototype exploring how a **Deterministic Intent Folding (DIF)**-style semantic layer can sit next to Embabel's typed blackboard and planners. The idea **is** that pairing: fold freezes intent; Embabel plans over the freeze. The fold is Embabel-free so other consumers (the orchestrator) can use the same model.

```text
LLM = probabilistic interpreter / generator
DIF = semantic intent substrate
Embabel = deterministic planner / orchestrator
Verifier = deterministic acceptance boundary
```

This is **not** an implementation of any proprietary Merly DIF algorithm.

**Start with the reasoning, then the spec:**

| Doc | Job |
| --- | --- |
| [`docs/REASONING.md`](docs/REASONING.md) | Why this exists, why Embabel, why the orchestrator, DICE vs DIF, the path |
| [`docs/DIF_EMBABEL_PROTOTYPE.md`](docs/DIF_EMBABEL_PROTOTYPE.md) | What to build (IR, fold, planner, verifier, phases) |
| [`docs/RELATIONSHIP_SDLC_SPDD.md`](docs/RELATIONSHIP_SDLC_SPDD.md) | How a REASONS Canvas becomes a checkable projection |
| [`docs/FOLD_ITERATION.md`](docs/FOLD_ITERATION.md) | Steal ideas from cousins; ten steps to iterate the fold |
| [`docs/ORCH_INTEGRATION_ROADMAP.md`](docs/ORCH_INTEGRATION_ROADMAP.md) | Daily orch loop, attach rules, integration test ladder |
| [`docs/BLOG_DIF_ORCH_EMBABEL.md`](docs/BLOG_DIF_ORCH_EMBABEL.md) | Publication source: three layers, how far we take the idea |
| [`docs/DATA_INGEST.md`](docs/DATA_INGEST.md) | How a canvas or change-request becomes each model |

Mermaid for the working test flow and each system's data model is in [Systems, tests, and data models](#systems-tests-and-data-models) below.

Index: [`docs/README.md`](docs/README.md).

## What is stubbed

| Phase | Status |
| --- | --- |
| 1. Typed semantic model + deterministic fold | Working |
| 1b. REASONS canvas → SemanticModel CLI | Working (`./scripts/dif-fold.sh`) |
| 2. Embabel GOAP to `VerificationPlan` | Working (fixture path, no LLM required) |
| 3. LLM code generation | Stub (`LaterPhaseActions`) |
| 4. Deterministic verification | Semantic diff works; code/test checks stubbed |
| 5. Repair loop from `VerificationFailure` | Types only |
| 6. Persistent `.dif/` memory | Seed files + write snapshot stub |

Milestone 1 path:

```text
ChangeRequest → CandidateIntent → SemanticModel → RepositoryAnalysis → VerificationPlan
```

The first use case is:

> Add refresh-token rotation without changing existing login behavior.

## Layout

```text
docs/DIF_EMBABEL_PROTOTYPE.md   spec
src/main/java/com/embabel/dif/
  domain/                       typed IR and blackboard objects
  dif/                          fold, conflicts, missing obligations
  agent/                        Embabel actions + interpreters
  verifier/                     intent diff + verification rules
  memory/                       .dif/ persistence stub
  scenario/                     refresh-token fixture
.dif/                           versioned semantic memory seed
```

## Requirements

- Java 21
- Maven (wrapper included)
- Optional: `OPENAI_API_KEY` or `ANTHROPIC_API_KEY` for non-fixture change requests

## Commands

```bash
./mvnw test
./scripts/dif-orch-smoke.sh
./scripts/dif-orch-day.sh
./scripts/dif-live-e2e.sh   # reuses orch Guide+Neo4j; optional live Embabel
./scripts/dif-dashboard-e2e.sh   # fold → orch Dashboard readout (no JVM on Refresh)
./scripts/dif-dual-mode-e2e.sh   # structured Work ID + unstructured area capture
./scripts/ecosystem-up.sh        # full install on dogfood-api (no Cursor-only / skip-if-missing)
./scripts/dif-fold.sh --canvas examples/canvases/FEAT-001-order-status-api.md
./scripts/dif-fold.sh architect --projection .dif/projections/FEAT-099-pagination-conflict.json
./scripts/dif-fold.sh review --before examples/snapshots/login-before.json --after examples/snapshots/login-auth-broken.json
./scripts/dif-fold.sh plan --canvas examples/canvases/FEAT-001-order-status-api.md
./scripts/shell.sh
```

`dif-fold` does not start Embabel. `fold` writes `.dif/projections/<WORK-ID>.json`
and a stable `.gate.json` (`readyForImplementation`, `blockingConflicts`,
`missingObligations`) that a script can read without parsing stdout.
`architect` and `review` fail closed from a projection or `IntentDiff` (exit `1`
= not Ready For Coding / invariants not preserved). `plan` builds a
`VerificationPlan` from the folded model. `--guide` / `guide` writes optional
Guide JSONL. `--alloy` writes an Alloy sketch; no extra binary is required.
`dif-orch-smoke.sh` folds the harvested orch canvases and asserts those exit
codes plus `.gate.json`. Existing orch commands may call
`scripts/orch-attach/check-canvas.sh` as a silent gate: one line
`dif=ready|blocked|skipped` (missing DIF is skip, not a new daily `next`).
`fold` / `architect --quiet` print that same line. Review of an orch
canvas uses `examples/snapshots/order-status-*.json` (safeguard paths),
not the login fixtures. `check-review.sh` skips when those files are
absent. `plan --projection` builds a `VerificationPlan` from a folded
model without re-reading markdown. `./scripts/dif-orch-day.sh` is the
scripted day. `./scripts/dif-live-e2e.sh` reuses the orchestrator's
existing `tests/test-guide-stack-live.sh` to boot Guide+Neo4j, quotes
DIF JSONL through `GuideClient`, then boots the Embabel Spring
platform. It does not put Embabel or Guide inside `sdlc.sh next`.

In the Embabel shell:

```text
fold
fold-local
intent-diff
x "Add refresh-token rotation without changing existing login behavior."
```

The known refresh-token wording is handled by `FixtureIntentInterpreter`, so fold does not need an LLM. Other requests go through `LlmIntentInterpreter`.

## Design rule

Use stochastic reasoning to discover knowledge. Once a fact is accepted, fold it into typed intents, invariants, relations, and deterministic checks that Embabel can plan over.

## Systems, tests, and data models

Three systems stay separate. Tests prove they can talk without collapsing into one runtime.

```mermaid
flowchart LR
  subgraph orch [Orchestrator]
    canvas[REASONS Canvas]
    next["sdlc.sh next"]
    gate["sdlc.sh gate"]
    lesson[LessonRecord]
  end
  subgraph dif [DIF]
    fold["dif-fold.sh"]
    model[SemanticModel]
    gatejson[".gate.json"]
  end
  subgraph embabel [Embabel]
    goap[GOAP planner]
    plan[VerificationPlan]
  end
  subgraph guide [Guide / DICE]
    neo4j[(Neo4j)]
    named[WorkId Canvas Decision Pitfall]
  end
  canvas -->|accepted markdown| fold
  fold --> model
  model --> gatejson
  gatejson -->|dif=ready or blocked| next
  next -.->|never starts a JVM| goap
  gate -.->|files exist?| canvas
  model -->|optional JSONL quote| lesson
  lesson -->|project_load| neo4j
  neo4j --> named
  model -->|already-folded facts| goap
  goap --> plan
```

| System | Owns | Must not own |
| --- | --- | --- |
| **Orchestrator** | Who acts when. Work ID, canvas file, `next`, process gates, lesson ledger. | The fold. Starting Embabel. Being a planner. |
| **DIF** | What must stay true. Same accepted canvas → same `SemanticModel`. | Daily orientation. Picking the Work ID. Replacing the canvas. |
| **Embabel** | What action to take on *already folded* facts (optional JVM path). | The fold itself. `sdlc.sh next`. |
| **Guide** | What we learned before (retrieve-only graph). | Ready For Coding. Fail-closed review. |

### Working test flow

What we actually run. Default CI is the first box. Live Guide/Embabel is opt-in and reuses the orch harness.

```mermaid
flowchart TB
  unit["./mvnw test<br/>unit + CLI + fold gates<br/>EmbabelLivePlatformTest skipped unless DIF_LIVE_EMBABEL=1"]
  smoke["./scripts/dif-orch-smoke.sh<br/>FEAT-001 ready + T03<br/>FEAT-099 exit 1"]
  day["./scripts/dif-orch-day.sh<br/>fold twice / architect / review / plan --projection<br/>skip when CLI or snapshots missing"]
  attach["orch tests/test-optional-dif.sh<br/>+ test-command-specs.sh<br/>stub: skipped / ready / blocked"]
  live["./scripts/dif-live-e2e.sh<br/>orch test-guide-stack-live.sh<br/>quote JSONL via GuideClient<br/>Embabel GOAP UserInput → VerificationPlan"]
  dash["./scripts/dif-dashboard-e2e.sh<br/>FEAT-DASH-flow blocked → ready<br/>Dashboard reads .gate.json"]
  dual["./scripts/dif-dual-mode-e2e.sh<br/>FEAT-001 + area-only persist<br/>dogfood-api / ecosystem-up.sh"]
  guideMvn["orch-guide ./mvnw test<br/>194 tests; needs Docker socket"]

  unit --> smoke --> day
  day --> attach
  day --> live
  live --> dash
  dash --> dual
  dual --> guideMvn
```

Live E2E sequence (the three-way path that already passed here):

```mermaid
sequenceDiagram
  participant Day as dif-orch-day
  participant Fold as dif-fold.sh
  participant Orch as orch installer
  participant Guide as Guide :21337
  participant Graph as Neo4j
  participant Quote as GuideClient
  participant Emb as Embabel platform

  Day->>Fold: fold / architect / review / plan --quiet
  Fold-->>Day: dif=ready or dif=blocked
  Orch->>Graph: neo4j/start embabel-neo4j
  Orch->>Guide: guide/start no_ingest
  Orch->>Guide: POST spdd-projection/load spring-boot-order-api
  Fold->>Fold: guide JSONL Decision + Pitfall
  Quote->>Guide: persist_lesson + project_load unique Work ID
  Guide->>Graph: merge WorkId / Canvas / Pitfall
  Quote->>Guide: GET work/{workId}
  Guide-->>Quote: subgraph contains DIF-LIVE marker
  Emb->>Emb: captureRequest → interpretIntent → foldIntent
  Emb->>Emb: analyzeRepository → planVerification
  Emb-->>Day: VerificationPlan readyForImplementation
```

### DIF data model

Human contract is still the canvas. The machine contract is a regenerable `SemanticModel`. `.gate.json` is what orch reads.

```mermaid
erDiagram
  REASONS_CANVAS ||--|| SemanticModel : "deterministic fold"
  SemanticModel ||--o{ Intent : contains
  SemanticModel ||--o{ Invariant : contains
  SemanticModel ||--o{ SemanticRelation : contains
  SemanticModel ||--o{ IntentConflict : contains
  SemanticModel ||--o{ MissingObligation : contains
  Intent ||--|| Provenance : sourced_from
  Intent {
    string id
    enum type "REQUIREMENT CONSTRAINT PRESERVATION GOAL"
    string statement
    enum priority
  }
  Invariant {
    string id
    string description
    enum strategy
  }
  IntentConflict {
    Intent left
    Intent right
    string explanation
    bool blocking
  }
  MissingObligation {
    string obligation
    string derivedFromIntent
  }
  SemanticModel ||--|| GateReport : projects
  GateReport {
    string workId
    bool readyForImplementation
    list blockingConflicts
    list missingObligations
  }
  SemanticModel ||--|| VerificationPlan : planned_over
  VerificationPlan ||--o{ VerificationRule : checks
  SemanticSnapshot ||--o{ SemanticProperty : "path = value"
  SemanticSnapshot ||--|| IntentDiff : review
```

### How data gets in

Hop-by-hop map: [docs/DATA_INGEST.md](docs/DATA_INGEST.md). Tests:
`DataModelIngestTest`.

```mermaid
sequenceDiagram
  participant Human
  participant Parser as ReasonsCanvasParser
  participant Mapper as CanvasIntentMapper
  participant Folder as IntentFolder
  participant Gate as GateReport
  participant Emb as Embabel GOAP
  participant Guide as GuideLedger

  Human->>Parser: accepted canvas markdown
  Note over Parser: User Goal prose dropped
  Parser->>Mapper: ReasonsCanvas (R/N/S/O bullets)
  Mapper->>Folder: CandidateIntent (type from heading)
  Folder->>Gate: SemanticModel + open T##
  Gate-->>Human: dif=ready or blocked
  Folder->>Guide: optional Decision / Pitfall JSONL
  Folder->>Emb: already-folded facts
  Emb-->>Human: VerificationPlan
```

A second ingest path is Embabel-only: `UserInput` → `ChangeRequest` →
`FixtureIntentInterpreter` or `LlmIntentInterpreter` → the same
`IntentFolder`. The LLM writes `CandidateIntent`, never the freeze.

Canvas sections map like this: **R** → `REQUIREMENT`, **N** non-goals → `CONSTRAINT`, **S** safeguards → `PRESERVATION`. A requirement vs non-goal pair becomes a blocking `IntentConflict`. An operation with no matching work (T03 on FEAT-001) becomes a `MissingObligation`. Review compares two `SemanticSnapshot`s; safeguard paths come from the canvas **S** lines, not login fixtures.

Optional Guide quote from the same model (`GuideLedger`):

```json
{"kind":"Decision","workId":"FEAT-001-…","text":"<invariant>","source":"INV-…"}
{"kind":"Pitfall","workId":"FEAT-001-…","text":"<conflict or Missing: T03 …>","source":"conflict"}
```

### Orchestrator data model

Orch owns process, not fold. Files and ledger rows are the contract `sdlc.sh gate` already understands.

```mermaid
erDiagram
  WORK_ID ||--|| REASONS_CANVAS : "spdd/canvas/WORK_ID.md"
  WORK_ID ||--o{ OPERATION : "T01 T02 T03"
  WORK_ID ||--o{ LessonRecord : ledger
  REASONS_CANVAS {
    string workId
    string readiness "Ready For Coding or Needs Clarification"
    string requirements
    string nonGoals
    string safeguards
  }
  LessonRecord {
    string id
    string kind "decision or pitfall"
    string work_id
    string area
    string body
    string source
  }
  LessonRecord ||--o| SQLITE : upsert
  LessonRecord ||--o| GUIDE : "project_load when guide-dice on"
  CHECK_CANVAS ||--|| GateReport : "architect --quiet"
  CHECK_REVIEW ||--|| IntentDiff : "review --quiet"
  CHECK_CANVAS {
    string line "dif=ready or blocked or skipped"
    int exit "0 skip/ready  1 blocked"
  }
```

`next` / `architect` / `code` / `review` call `check-canvas.sh` or `check-review.sh` when the CLI is on PATH. Missing CLI or missing review snapshots → `dif=skipped` (exit 0). Present CLI + conflict or dropped safeguard → `dif=blocked` (exit 1). Orch CI uses `tests/fixtures/dif-fold-stub.sh` so it does not need Maven.

### Guide and Embabel data models

Guide is a retrieve-only NamedEntity graph in Neo4j. Embabel is a GOAP blackboard over DIF types. Neither replaces the canvas.

```mermaid
erDiagram
  WorkId ||--o| Canvas : has_canvas
  WorkId ||--o| Area : in_area
  Canvas ||--o{ Operation : "T01 T02"
  Decision }o--o| WorkId : recorded_for
  Pitfall }o--o| WorkId : recorded_for
  Decision }o--o| Area : about
  Pitfall }o--o| Area : about
  Pattern }o--o| WorkId : recorded_for
  WorkId {
    string id "FEAT-001-order-status-api"
    string workType
    string status
  }
  Canvas {
    string id
    string path
    string readiness
  }
  Decision {
    string id
    string description
  }
  Pitfall {
    string id
    string description
  }
```

```mermaid
flowchart LR
  UI[UserInput] --> CR[ChangeRequest]
  CR --> CI[CandidateIntent]
  CI --> SM[SemanticModel]
  SM --> RA[RepositoryAnalysis]
  SM --> VP[VerificationPlan]
  RA --> VP
  FI[FixtureIntentInterpreter] -.-> CI
```

Live Embabel (`EmbabelLivePlatformTest`, `DIF_LIVE_EMBABEL=1`) runs:

`captureRequest` → `interpretIntent` → `foldIntent` → `analyzeRepository` → `planVerification`

The refresh-token wording uses `FixtureIntentInterpreter` (no LLM). Conflicts stay on the `VerificationPlan` (`readyForImplementation=false`); they are not a GOAP precondition Embabel 1.5 cannot treat as an action post.

Live Guide quote uses a **unique** Work ID (`FEAT-DIF-LIVE-…`) so it does not collide with orch’s already-projected `FEAT-001` from `examples/spring-boot-order-api`.
