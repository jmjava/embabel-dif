# Documentation

Read these in order if you are new to the repo.

| Doc | Job |
| --- | --- |
| [REASONING.md](REASONING.md) | **Why this exists.** The argument: implicit intent, determinism at boundaries, Embabel, the orchestrator, DICE vs DIF, and the path we are following. |
| [DIF_EMBABEL_PROTOTYPE.md](DIF_EMBABEL_PROTOTYPE.md) | **What to build.** The original prototype spec: IR, fold, planner, verifier, phases, success criteria. |
| [RELATIONSHIP_SDLC_SPDD.md](RELATIONSHIP_SDLC_SPDD.md) | **How it attaches.** REASONS Canvas → typed model, without turning the orchestrator into an Embabel runtime. |
| [FOLD_ITERATION.md](FOLD_ITERATION.md) | **How the fold grows.** Steal checkable ideas from cousins; ten steps (now implemented) behind `IntentFolder`. |

The README in the repo root is the operator surface (`./mvnw test`, `./scripts/dif-fold.sh`).
