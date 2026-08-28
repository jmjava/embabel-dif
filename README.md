# embabel-dif

Prototype exploring how a **Deterministic Intent Folding (DIF)**-style semantic layer can sit next to Embabel's typed blackboard and planners.

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
codes plus `.gate.json`.

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
