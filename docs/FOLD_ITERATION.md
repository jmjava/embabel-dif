# Steal ideas, then iterate the fold

This page records two decisions:

1. We will **not** invent a Merly-style folding algorithm, and we will **not**
   wait for one to be open-sourced. None exists that does
   `REASONS canvas → SemanticModel`.
2. We will **steal checkable ideas** from adjacent open tools, and grow a
   rule catalog behind `IntentFolder` until the orchestrator success tests
   hold.

The prototype spec says what the IR is. [REASONING.md](REASONING.md) says why
the fold is a sibling of Embabel, not Embabel itself. How the attach enters
a developer’s day is [ORCH_INTEGRATION_ROADMAP.md](ORCH_INTEGRATION_ROADMAP.md).
The publication source is [BLOG_DIF_ORCH_EMBABEL.md](BLOG_DIF_ORCH_EMBABEL.md).
How a bullet becomes an intent is [DATA_INGEST.md](DATA_INGEST.md).
This page says how the fold gets better without becoming a research project.

---

## 1. What we already decided

```text
LLM        interpret / generate     (probabilistic)
fold       freeze intent            (what must stay true)
Embabel    plan next action         (what to do next)
verifier   accept or fail           (did we keep the intent)
```

Embabel is a planner. It consumes typed facts; it does not produce them.
Merly DIF is one way to produce them, and it is closed. The seam in this repo
is `IntentFolder`. `RuleBasedIntentFolder` is the first implementation:
normalize, dedupe, classify, derive invariants, detect known contradictions,
emit missing obligations, hash the result.

That is enough of an algorithm. Iteration means **more rules and better
tests**, not a new methodology.

---

## 1a. Where Embabel comes in if fold is not JVM-inside-`next`

The repo is named `embabel-dif` because the idea **is** the pairing:
DIF freezes intent; Embabel plans over the freeze. Fold being
Embabel-free does **not** retire Embabel. It puts Embabel in the only
job it is good at: **choose the next action on facts that already
exist.** Without the fold, Embabel would plan over mush. Without
Embabel (as the intended consumer), the fold is only a canvas linter.

The daily path never starts it:

```text
canvas  →  IntentFolder (FoldWiring, no Spring)  →  SemanticModel
                 ↓
          orch architect / review / --quiet
          (bash + files; sdlc.sh next does not boot a JVM)
```

The optional path starts it *after* that model exists, for JVM targets
only:

```text
already-folded SemanticModel
        ↓
Embabel blackboard  (typed objects, not markdown)
        ↓
GOAP                  capture → interpret → foldIntent → analyze → plan
        ↓
VerificationPlan      what to check next; conflicts stay on the plan
```

`DifEmbabelAgent.foldIntent` is a one-line call to `IntentFolder`. That
is the seam. Embabel **invokes** the folder when the blackboard needs a
`SemanticModel`. It does **not** classify R/N/S, detect conflicts, or
derive obligations inside `@Action` methods. Those rules stay behind
`IntentFolder` so the CLI and the agent see the same freeze.

What Embabel owns, then:

| Owns | Does not own |
| --- | --- |
| Action order on typed facts (GOAP) | The fold algorithm |
| Replanning when a new fact appears | `sdlc.sh next` / process gates |
| `VerificationPlan` as a goal object | The REASONS Canvas as contract |
| Optional live boot (`DIF_LIVE_EMBABEL=1`) | Guide ingest, a second Neo4j |

If you never boot Embabel, the day is still correct: same canvas, same
model, same fail-closed gate. Embabel is how a JVM app *uses* that
model. It is not how the model is made.

---

## 2. Steal ideas, not implementations

There is no library to import as `IntentFolder`. Things named “fold” or
“intent” on GitHub are usually doing a different job (context compaction,
agent runtimes, CRDT state, MCP tool gates).

What *is* worth stealing is the **shape of a check**. Cousins already solved
pieces of “accepted facts must not contradict, and absence must be typed.”
We take the check, not their language, runtime, or file format.

The human contract stays the REASONS Canvas. The machine contract stays
`SemanticModel`. Anything we steal compiles *into* those types or sits
*behind* `IntentFolder`.

| Steal this idea | From | Do not steal |
| --- | --- | --- |
| Split goals from safety / non-goals | [intent-lang](https://github.com/popsicle-lab/intent-lang) (`goal` vs `safety`) | Their `.intent` DSL as the canvas |
| Vacuity: a self-contradictory spec cannot go green | intent-lang SMT gate | Requiring Z3 on day one |
| Coverage dimensions → typed holes | intent-lang `coverage` | Treating a checklist as the model |
| LLM drafts; a deterministic gate accepts | intent-lang “formalize then `intent check`” | Asking the LLM to be the fold |
| Search for a counterexample to an invariant | [Alloy](https://alloytools.org) | Replacing markdown with `.als` for humans |
| Fail the build if a violating state exists | Alloy / [alloyiser](https://github.com/hyperpolymath/alloyiser) | Making architect launch a SAT solver to run `next` |
| `requires` / `ensures` as named contracts the fold can cite | [lhaig/intent](https://github.com/lhaig/intent), [kodo](https://github.com/rfunix/kodo) | Switching the stack to a new language |
| Fold is reversible; source of truth is never the projection | [context-fold](https://github.com/Middlewatch/context-fold) (different “fold”) | Compacting the canvas out of the session |
| Flag idiosyncratic *syntax*, not load-bearing intent | [ControlFlag](https://github.com/IntelLabs/control-flag) (unmaintained; Gottschlich, pre-DIF) | Using anomaly detection as the semantic layer |
| Plan over typed facts after they exist | Embabel GOAP | Putting classify / conflict / obligation inside `@Action` methods |
| Retrieve past beliefs; do not freeze them | Guide DICE in the orchestrator | Merging retrieve and fold |

If a cousin later ships a real canvas→model folder, we still do not merge
repos. We implement `IntentFolder` again.

---

## 3. How we steal in practice

A stolen idea is allowed in only when it maps to an existing type or a new
rule with a failing test:

| Cousin idea | Lands here |
| --- | --- |
| `goal` / `safety` | `IntentType.REQUIREMENT` / `CONSTRAINT` / `PRESERVATION` |
| Vacuity / mutual exclusion | `ConflictDetector` → `IntentConflict` (exit 1) |
| Coverage / implied tests | `ObligationDeriver` → `MissingObligation` (T03 today) |
| Counterexample search | Optional later backend behind the same detector |
| Named contracts | `Invariant` + `VerificationStrategy` |
| Reversible fold | Regenerable `.dif/projections/<WORK-ID>.*` — canvas remains source of truth |
| Syntax vs intent | Invariants must not flip when DTO names or test style change |

Rules of engagement:

1. **Canvas in, model out.** No new human-facing language until the
   orchestrator asks for one.
2. **Test first.** A new rule starts as a canvas (or fixture) that today’s
   fold gets wrong.
3. **Smallest check that fails closed.** Token rules before SMT. SMT only
   when a pair is real and tokens cannot express it.
4. **Do not fold inside Embabel.** Every consumer would then need the JVM
   planner. The CLI path must stay Embabel-free.
5. **Do not let the LLM be the fold.** It may propose candidates. After
   accept, the same facts must hash the same way twice.
6. **Projection is disposable.** If people stop reading the canvas, we
   failed even if the JSON is pretty.

---

## 4. Ten steps to iterate

These steps continue the path in [REASONING.md](REASONING.md) §9. They are
ordered so each one can be wrong in an interesting way before we spend
complexity on the next.

| Step | Status | Where |
| --- | --- | --- |
| 1. Lock the fold contract | Done | `FoldContractTest` names the five checks |
| 2. Steal list next to the code | Done | Javadoc `// vacuity: intent-lang` (and cousins) on the rules |
| 3. Harvest canvases | Done | `examples/canvases/README.md` |
| 4. Classify from headings | Done | `CanvasIntentMapper` + `CanvasClassificationTest` |
| 5. Named conflicts, quoted | Done | `ConflictDetector` explanations quote both intents |
| 6. Obligations from open T## | Done | Open ops + webhook/retry implications (`FEAT-020`) |
| 7. Syntax stays out of invariants | Done | `FEAT-070-dto-rename-*` + `SyntaxVarianceTest` |
| 8. Formal backend after tokens fail | Done | `FormalConflictBackend` + optional `.als` (`FEAT-080`) |
| 9. Architect / review attach | Done | `dif-fold.sh architect --projection` / `review --before --after` |
| 10. Embabel consumes; Guide may quote | Done | `VerificationPlanner` + `guide` JSONL; Guide remains optional |

### Step 1 — Lock the fold contract

`IntentFolder.fold(CandidateIntent) → SemanticModel` is the only swap point.
Success is already defined: same accepted canvas → same model; review can
fail without “looks correct”; syntax variance is legal; open T## is a
`MissingObligation`; requirement vs non-goal blocks Ready For Coding.

**Done when:** those five checks are named tests, not only prose. Adding a
backend does not change the CLI or the canvas schema.

### Step 2 — Keep a steal list next to the code

This document *is* the list. When we copy a check from a cousin, cite it in
the rule’s Javadoc or a one-line comment (`// vacuity: intent-lang`). When a
cousin is a false friend (context-fold, humanlayer/fold, IDF-the-MCP-thing),
leave it in the table above so the next session does not rediscover it.

**Done when:** a new contributor can tell steal vs ignore without rereading
chat history.

### Step 3 — Harvest canvases as the corpus

The fold learns from real REASONS files, not from imagined IR. Start with
`examples/canvases/`, then copy (or slim) canvases from
`sdlc-spdd-orchestrator` that already have requirements, non-goals, PRESERVE
lines, and open T## operations.

**Done when:** every conflict kind and obligation we claim to handle has a
checked-in canvas that demonstrates it.

### Step 4 — Classify from canvas structure, not from vibes

Steal intent-lang’s goal/safety split, but bind it to sections the
orchestrator already writes: requirements vs non-goals vs preserve vs
operations. `CanvasIntentMapper` is the place. The LLM may suggest a type;
the mapper must be able to ignore it when the heading already decided.

**Done when:** moving a bullet from Requirements to Non-goals changes
`IntentType` and can introduce a conflict, without a prompt change.

### Step 5 — Grow conflicts only from escaped pairs

`ConflictDetector` stays a catalog of *named* contradictions (refresh
single-use vs reuse; requirement vs non-goal sharing a token). Do not add
fuzzy “these sentences feel opposite.” When a real pair escapes, write the
canvas first, then the smallest predicate that catches it.

**Done when:** architect can trust exit 1, and we can explain every conflict
in one sentence that quotes the two intents.

### Step 6 — Grow obligations from open operations

Steal coverage: absence is a typed hole. `ObligationDeriver` should read
open T## rows and implied safeguards (rotation → family id, consumed state,
replay test), not only the refresh-token fixture.

**Done when:** T03-style gaps appear as `MissingObligation` on a canvas that
never mentions “refresh.”

### Step 7 — Keep syntax out of the invariants

Steal ControlFlag’s instinct (syntax can be idiosyncratic) and invert it:
the fold must *not* treat DTO names, test style, or file layout as
invariants. `VerificationStrategy.INTENT_DIFF` / `API_CONTRACT` / `JUNIT`
stay bound to preservation statements.

**Done when:** renaming a DTO in a fixture does not flip a required
invariant, and a missing rotation test still does.

### Step 8 — Formal backend only after tokens fail

Steal Alloy / intent-lang’s counterexample search as an *optional*
implementation behind `ConflictDetector` (or a sibling `IntentFolder`).
Emit a temporary `.als` / `.intent` from the already-folded model; never
ask humans to write those files to use the CLI.

**Done when:** there is one canvas whose conflict tokens cannot express and
Z3/Alloy can; every other canvas still folds with no extra binary.

### Step 9 — Attach architect and review

This is orchestrator work, not a new algorithm. If `.dif/projections/`
exists, architect treats blocking `IntentConflict` as not Ready For Coding,
and review cites `VerificationResult` instead of asking whether the diff
looks correct. `sdlc-engine` still must not start a JVM to run `next`.

**Done when:** a dry-run on one Work ID fails closed from the projection
alone.

### Step 10 — Embabel consumes; Guide may quote

Only after steps 1–9: put the folded `SemanticModel` on an Embabel
blackboard for JVM targets. Optionally project invariants into Guide as
`Decision` / `Pitfall` nodes so DICE retrieves the same vocabulary the
verifier checks. Neither step is allowed to own the fold.

**Done when:** GOAP can build a `VerificationPlan` from a folded canvas
without re-interpreting the markdown, and Guide remains optional.

---

## 5. What we will not do while iterating

- Reimplement Merly DIF, or claim we did.
- Replace Embabel’s planner with the fold, or the fold with Embabel.
- Replace the REASONS Canvas with JSON, Alloy, or a new DSL.
- Erase the fold/`@Action` seam. Joining this tree with orch is allowed.
- Add SMT, GOAP, or Guide wiring to make a demo look complete.
- Grow the rule catalog without a canvas that fails today.

If step *n* is not done, do not start step *n+2*. Skipping one step is
allowed when the stolen idea does not apply; skipping the test is not.
