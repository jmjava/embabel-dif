# embabel-dif

Prototype exploring how a **Deterministic Intent Folding (DIF)**-style semantic layer can sit next to Embabel's typed blackboard and planners.

```text
LLM = probabilistic interpreter / generator
DIF = semantic intent substrate
Embabel = deterministic planner / orchestrator
Verifier = deterministic acceptance boundary
```

This is **not** an implementation of any proprietary Merly DIF algorithm. It is an experiment from the spec in [`docs/DIF_EMBABEL_PROTOTYPE.md`](docs/DIF_EMBABEL_PROTOTYPE.md).

Related: [`docs/RELATIONSHIP_SDLC_SPDD.md`](docs/RELATIONSHIP_SDLC_SPDD.md) — how this fold/verify layer could sit under the REASONS Canvas in [`jmjava/sdlc-spdd-orchestrator`](https://github.com/jmjava/sdlc-spdd-orchestrator) without becoming that repo's runtime.

## What is stubbed

| Phase | Status |
| --- | --- |
| 1. Typed semantic model + deterministic fold | Working |
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
./scripts/shell.sh
```

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
