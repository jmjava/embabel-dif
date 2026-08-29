# Reasoning

This document is the **why**. The prototype spec says what to build. The
orchestrator note says how the two repos attach. The
[fold iteration note](FOLD_ITERATION.md) says how we steal checkable ideas
and grow the rule catalog. The
[integration roadmap](ORCH_INTEGRATION_ROADMAP.md) says how the attach
enters a developer’s day and how we test that before touching
`sdlc-engine`. The
[publication source](BLOG_DIF_ORCH_EMBABEL.md) is the same plan in a
form a blog / generation tool can quote. This page is the argument
that justifies those.

It is also a record of decisions so a later session does not reconstruct a
slightly different story from the code.

---

## 1. The failure mode

Coding agents are good at reading a repository and *sounding* like they
understand it. The understanding is usually implicit and disposable:

```text
prompt + files + luck  →  a one-off theory of the system  →  a patch
```

The next session starts from zero. It may decide that `sessionToken` was
incidental, that Google login can move, or that an existing test is optional.
Nothing in the process *remembers* which of those beliefs were load-bearing.

`sdlc-spdd-orchestrator` already attacks the process half of that problem: one
Work ID, one REASONS Canvas, one phase at a time. Assistants are not allowed to
invent a parallel workflow.

That is necessary and not sufficient. The canvas is still prose. Review,
architect, and sync still ask an LLM to *compare* the canvas to a diff.
Comparison is exactly where implicit intent creeps back in.

This repository exists to make the load-bearing part of that comparison
**typed, foldable, and checkable**.

---

## 2. The hypothesis

Reliable AI engineering does not require every component to be deterministic.

It requires determinism at the boundaries where repeatability, traceability,
and correctness matter.

| Probabilistic (keep) | Deterministic (require) |
| --- | --- |
| Interpret a messy request | Represent accepted facts |
| Propose candidate evidence | Detect conflicting intent |
| Generate code | Check preconditions |
| Explain a failure | Validate acceptance criteria |
| Explore an unknown domain | Decide whether the goal was met |

So the split is:

```text
LLM      = probabilistic interpreter / generator
DIF      = semantic intent substrate
Embabel  = deterministic planner / orchestrator   (optional, JVM)
Verifier = deterministic acceptance boundary
```

This is **not** “make the model deterministic.” It is “stop asking the model to
be the system of record.”

The prototype is inspired by public discussion of Deterministic Intent Folding.
It does **not** claim to implement any proprietary Merly algorithm.

---

## 3. Intent, semantics, syntax

A software change is three different things. Mixing them is why agents thrash.

```text
INTENT       what should be true
   │
   v
SEMANTICS    what system properties follow
   │
   v
SYNTAX       which files and tokens implement that
```

Example from the prototype:

| Layer | Statement |
| --- | --- |
| Intent | Refresh tokens must rotate. Existing Google and Apple login must not change. `sessionToken` stays. |
| Semantics | A consumed refresh token cannot authenticate again. OAuth flows remain registered. The JWT still has `sessionToken`. |
| Syntax | `TokenService.java`, a migration, an integration test |

The LLM is allowed to vary syntax. It is not allowed to silently drop a
semantic invariant. A source-code diff cannot tell you that distinction. A
**semantic diff** can:

```text
+ refresh-token.rotates
- refresh-token.reusable
= provider.GOOGLE
= provider.APPLE
= jwt.claim.sessionToken
```

That is more useful to an autonomous loop than `+17 / -9` in a Java file.

The orchestrator already *names* this split. `/sdlc-spdd-review` classifies
findings as implementation mismatch, canvas/intent mismatch, or non-behavioral
refactor. DIF’s job is to **compute** that classification instead of hoping
the reviewer prompt does.

---

## 4. What “folding” means here

Folding is not a novel mathematical transform in this prototype. It means:

> Reduce many observations to a smaller set of canonical assertions, and keep
> the pointers back to the observations.

```text
E1 existing test asserts sessionToken
E2 mobile client reads sessionToken
E3 commit message: added for mobile compatibility
E4 current request: do not change login
        │
        v
     FOLD
        │
        v
INV-JWT-SESSION-TOKEN
  sessionToken remains present and compatible
```

The fold is deterministic **after candidate facts are accepted**. The LLM may
propose candidates. The canonical model is produced by rules.

Unfolding is the reverse: every invariant must explain *why it exists*. That is
provenance, not decoration. Without it the model becomes another opaque
artifact people stop trusting.

Absence is part of the fold. If rotation implies a rotation integration test
and the repository (or canvas operations) does not have one, that is a
`MissingObligation` — a typed hole, not a forgotten bullet.

---

## 5. Why Embabel is in the picture

Embabel already does the part of agent design this experiment needs:

- typed domain objects on a blackboard
- actions selected from preconditions / effects (GOAP)
- replanning when new facts appear
- JVM / Spring, which matches the user’s other work

Embabel should **not** own the semantic layer. If it did, every consumer would
have to be an Embabel app. The orchestrator cannot take that dependency: it is
an installable operating model for Cursor, Copilot, and Claude, and it is
explicitly *not* a compiled agent runtime.

So Embabel is a **consumer** of the folded model, not its home. The
project is still Embabel DIF: the fold exists so Embabel (and anything
else that can read the same types) has facts instead of mush.

```text
milestone 1 (no repo edits)
ChangeRequest → CandidateIntent → SemanticModel → VerificationPlan

canvas path (no Embabel process)
REASONS.md → CandidateIntent → SemanticModel → .dif/projections/
```

The second path is the one that can help the orchestrator first. The first
path proves the same types can sit on an Embabel blackboard later, for JVM
targets only.

---

## 6. Why the orchestrator is the first consumer

`sdlc-spdd-orchestrator` is the place where intent is already supposed to live
in the repo:

| Layer | Question | Artifact |
| --- | --- | --- |
| Planning | Why are we doing this? | `ROADMAP.md`, requirements |
| SPDD | What ships, and what does not? | `spdd/canvas/<WORK-ID>.md` |
| SDLC | Who acts when? | phase commands, gates, hot briefs |
| Memory | What did we learn? | `spdd/memory/lessons.jsonl` + optional Guide |

The remaining hole is **checkability**. Process gates (`sdlc.sh gate review`)
ask “do the prerequisite files exist?” They do not ask “did this diff preserve
the non-goals?”

That hole is exactly DIF:

| Phase | Today | After a fold |
| --- | --- | --- |
| Architect | Harden prose | Block Ready For Coding on `IntentConflict` |
| Code | One T## + a prompt | Invariants as a hard constraint set |
| Review | LLM vs canvas | `IntentDiff` + `VerificationResult` |
| Sync | Reconcile prose | Semantic drift vs structural drift |

A canvas is already a `CandidateIntent`. We do not need a new human artifact.
We need a projection.

---

## 7. DICE is not DIF

The orchestrator already has **Guide DICE** as an optional working store
(`CONTEXT_BACKENDS=…,guide-dice`). It is easy to smash the two acronyms
together. They do opposite jobs.

```text
DICE  = retrieve what we already believe
        (lessons, decisions, pitfalls, area subgraphs)

DIF   = freeze what must remain true, then verify it
        (intents, invariants, conflicts, obligations)
```

DICE answers “what did previous work in this area learn?”
DIF answers “may this change proceed, and did it preserve the contract?”

Both can project from the same committed files. Neither replaces the canvas or
the lessons ledger. The ledger stays the system of record; SQLite, Guide, and
`.dif/projections/` are regenerable.

---

## 8. Why two repositories

Merging this code into the orchestrator would fight both designs.

| If we put DIF inside the orchestrator | Cost |
| --- | --- |
| `sdlc-engine` grows a JVM agent | `next` / `gate` stop being assistant-agnostic |
| Canvas becomes a Java IR | Humans lose the markdown contract |
| Embabel becomes required | Copilot / Claude installs pay for a planner they do not run |

| If we put the orchestrator inside this repo | Cost |
| --- | --- |
| Slash commands and Python engine live next to a prototype IR | The experiment cannot move without dragging a product |
| “Not a compiled runtime” is no longer true | Operators cannot install process without the fold |

The contract between them is a file:

```text
spdd/canvas/<WORK-ID>.md          human source of truth
        │
        ▼  ./scripts/dif-fold.sh --canvas …
.dif/projections/<WORK-ID>.json   machine projection
```

Opt in the same way Guide is opted in: useful when present, ignored when not.
Never make `sdlc-engine` start a JVM to run `next`.

The other collapse is social, not technical: orch’s full loop versus
opening a prompt and asking the agent to solve it. People skip the
runbook because the chat is faster. That skip is the real daily
driver. DIF attaches only when a canvas already exists. It does not
get to make the prompt the wrong way to work.

We do not have to lose the day. A skipped runbook can still stage a
`decision` / `pitfall` / `pattern` keyed by **area**. That is the
ledger / DICE. It is not a fold. Inventing a Work ID so `capture`
validates is the wrong harvest.

---

## 9. The path, and why this order

```text
1. Here     canvas → SemanticModel CLI      no Embabel
2. Orch     architect / review attach       if projection exists
3. Here     Embabel GOAP for JVM targets    orchestrator still picks Work ID / T##
4. Optional project invariants into Guide   shared vocabulary, still not required
```

**Step 1 first** because it is the smallest thing that can be wrong in an
interesting way. If the same canvas does not fold the same way twice, nothing
downstream is trustworthy. If a requirement vs non-goal clash does not fail
closed, architect cannot use the exit code. If an open T03 does not show up as
a `MissingObligation`, sync still depends on a checklist.

**Step 2 next** because that is where the orchestrator still asks an LLM to be
a verifier. Attaching a projection is cheaper than inventing a new phase.

**Embabel later** because planning over typed facts is valuable, and also
because it is the part that must not leak into the orchestrator’s runtime.

**Guide last** because retrieval already works. Wiring invariants into DICE is
vocabulary alignment, not a prerequisite for checkability.

---

## 10. Worked examples

### Refresh-token rotation (prototype fixture)

Request:

> Add refresh-token rotation without changing existing login behavior.

Folded meaning:

- `INT-001` add rotation
- `INT-002`–`INT-005` preserve Google, Apple, authorization-code, `sessionToken`
- `INV-001` a consumed refresh token cannot be reused
- missing, if the repo has no rotation test: that test

A generator may rewrite `TokenService` any way it likes. If `sessionToken`
disappears from the semantic snapshot, verification fails even when unit tests
were updated to match the new (wrong) world.

### Order-status canvas (orchestrator example)

`examples/canvases/FEAT-001-order-status-api.md` is not “search orders by
email.” After fold it is:

- requirements for GET-by-email, 400, empty 200
- constraints: no pagination, no auth changes, no schema migration
- safeguards: do not change auth; do not touch unrelated endpoints
- `MissingObligation`: `T03 - Document API behavior`

T01 and T02 are complete, so they do not appear as holes. That is absence
reasoning over the canvas operations list, which is something the orchestrator
already writes and today only *displays*.

### Pagination conflict (architect gate)

`examples/canvases/FEAT-099-pagination-conflict.md` accepts “results must be
paginated” and also lists Pagination as a non-goal. Fold exits `1`. Ready For
Coding is the wrong readiness. The right next command is clarification, not
`/sdlc-spdd-code`.

---

## 11. What would falsify this approach

The work is not “write more markdown.” It is testable. The idea is wrong if:

1. Two folds of the same accepted canvas disagree.
2. Review still cannot fail a required safeguard without asking whether the
   change “looks correct.”
3. Syntax changes (DTO names, test style) flip a required invariant.
4. Open operations do not show up as missing obligations.
5. A requirement vs non-goal pair does not block Ready For Coding.
6. Teams stop reading the canvas because they treat the JSON as source of
   truth. The projection must remain regenerable and disposable.

If (1)–(5) hold and (6) does not happen, the orchestrator can opt in the same
way it opts into Guide.

---

## 12. What this is not

- Not a Merly reimplementation
- Not a replacement for Embabel’s planner
- Not a replacement for the REASONS Canvas
- Not a replacement for DICE / Guide retrieval
- Not a conventional RAG index
- Not “just another prompt template”
- Not an attempt to make all AI deterministic
- Not a reason to compile the orchestrator into an agent runtime
- Not a hunt for an open-source Merly folder (there isn’t one)
- Not a new human-facing language; stolen checks compile into `SemanticModel`

How the fold iterates — steal the *check*, not the cousin’s runtime — is
[FOLD_ITERATION.md](FOLD_ITERATION.md). How that fold is tested against the
orchestrator’s daily loop is
[ORCH_INTEGRATION_ROADMAP.md](ORCH_INTEGRATION_ROADMAP.md).

The experiment is specifically this:

> Use stochastic reasoning to **discover** knowledge. Use deterministic
> representations to **operationalize** knowledge once it is understood.

Git stores what changed. A DIF-style layer stores why it had to, and what must
still be true. Embabel, when present, decides what to do next. The
orchestrator decides who is allowed to act.
