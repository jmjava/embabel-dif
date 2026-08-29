# Orchestrator integration roadmap

How `embabel-dif` gets into a developer’s day without becoming the
orchestrator — and how we prove that path with tests before anyone
changes `sdlc-engine`.

Attachment shape and success criteria stay in
[RELATIONSHIP_SDLC_SPDD.md](RELATIONSHIP_SDLC_SPDD.md). Why the two
repos stay separate is in [REASONING.md](REASONING.md) §8–9. How the
fold itself grows is [FOLD_ITERATION.md](FOLD_ITERATION.md). The
blog / generation-tool cut of the same plan is
[BLOG_DIF_ORCH_EMBABEL.md](BLOG_DIF_ORCH_EMBABEL.md). This page
is the **operating and test plan** for step 2 of that path:

```text
1. Here     canvas → SemanticModel CLI      (working)
2. Orch     architect / review attach       ← this roadmap
3. Here     Embabel GOAP for JVM targets    (later)
4. Optional project invariants into Guide   (last)
```

The public cut of this plan is
[BLOG_DIF_ORCH_EMBABEL.md](BLOG_DIF_ORCH_EMBABEL.md). **How far:** DIF
does not become a second daily driver. Users keep `sdlc.sh next`. When
the CLI is present, architect / code fail closed. When it is absent, the
day is unchanged. Do not add `dif-fold.sh next`.

---

## 1. What “full integration” means

Not a merge. Not a JVM inside `next` / `gate`. Not a second source of
truth.

A developer already has a daily loop in an installed project
(`sdlc-spdd-orchestrator` [daily runbook](https://github.com/jmjava/sdlc-spdd-orchestrator/blob/main/docs/daily-runbook.md)
and [workflow](https://github.com/jmjava/sdlc-spdd-orchestrator/blob/main/docs/workflow.md)):

```text
claim → next → analysis → plan (canvas) → architect →
code one T## → api-test → review → retro → sync → accept
```

Process gates already ask “do the files exist?” and “does the canvas
say Ready For Coding?” They do **not** ask “did this canvas contradict
itself?” or “did this diff drop a safeguard?”

Full integration is true when those two questions are answered by a
**sibling CLI exit code**, the same way Guide is answered by a
runtime-resolved backend: useful when present, ignored when not.

| Layer | Owner | Question |
| --- | --- | --- |
| Process gate | `sdlc.sh gate <phase>` | Are prerequisite files / ledger rows there? |
| Semantic gate | `./scripts/dif-fold.sh architect\|review` | May this Work ID proceed? |
| Retrieval | `sdlc-engine context …` / optional Guide | What did we learn before? |
| Planner | Embabel (optional, JVM targets only) | What action to take on typed facts? |

`sdlc-engine` stays Python and assistant-agnostic. It may *see* a
projection file or an exit code. It must not start a JVM to run
`next` or `gate`.

---

## 1a. Honest constraint: the prompt is the daily driver

Working with orch has a standing friction: **full process versus just
getting the agent to do the work.** People open a prompt and skip
`claim` → `next` → architect → one T##. That is expected. The runbook
is for a Work ID that has to outlive the chat. It is not a gate in
front of typing.

Attach rules that follow from that:

- A raw agent session with no canvas is a correct day. Do not invent
  a Work ID or a fold to make the process feel used.
- If `spdd/canvas/<WORK-ID>.md` is already in the tree, a silent
  `dif=ready|blocked|skipped` may run. The human did not ask for a
  ritual; the hook may still fail closed.
- `sdlc.sh next` stays the orientation command *for people who want
  orientation*. It is not the only way work is allowed to start.
- Embabel stays off this path. Nobody boots a JVM to “just fix it.”
- The day may still leave **lessons**. After an ad hoc session,
  stage 1–3 `decision` / `pitfall` / `pattern` rows keyed by **area**
  (`source=adhoc-prompt`). Do not invent a canvas so `capture` has a
  Work ID. Do not fold the chat. Retrieve next time with
  `context retrieve --area` / `spdd_areaLessons`.
- Today `LessonRecord.validate` still requires `work_id` to write.
  That is the remaining orch tax. The intended write is
  `kind + area + body`; Work ID is optional when one already exists.

If attach only helps people who already ran `next`, it does not help
the day we actually have. If ad hoc work leaves no sentence in the
ledger, we kept the speed and threw away the only part that would
have helped tomorrow.

---

## 2. What a developer day looks like after attach

Nothing new is invented. DIF sits on the phases that already compare
prose to reality. The prompt path in §1a is first-class; this section
is the runbook path for when a Work ID already exists.

### Morning (unchanged, then one optional fold)

```text
./sdlc-spdd/scripts/sdlc.sh next
./sdlc-spdd/scripts/sdlc.sh claim <WORK-ID>     # or resume
./sdlc-spdd/scripts/sdlc.sh start               # paste Resume Prompt
```

If a canvas already exists and DIF is on the PATH / sibling checkout:

```text
./scripts/dif-fold.sh fold --canvas spdd/canvas/<WORK-ID>.md
```

Same accepted canvas → same `.dif/projections/<WORK-ID>.json`. The JSON
is regenerable. Humans keep reading the markdown.

If DIF is not installed, the day continues exactly as today.

### Plan — write the contract (LLM, then fold)

`/sdlc-spdd-plan` still writes the REASONS Canvas. The human accepts
the prose. Only then is the canvas a `CandidateIntent`.

Fold is not a planning assistant. It does not invent requirements.
It classifies headings the orchestrator already writes (requirements,
non-goals, preserve, open T##).

**Done for the day when:** the canvas is committed (or at least
accepted in the working tree) and a re-fold matches the first fold.

### Architect — two gates, one readiness

1. Process: `sdlc.sh gate architect --work-id <WORK-ID>`
   (requirement + canvas exist).
2. Semantic, if DIF is present:
   `dif-fold.sh architect --canvas spdd/canvas/<WORK-ID>.md`
   (or `--projection` of a just-written file).

| CLI exit | Canvas readiness the architect may set | Next command |
| --- | --- | --- |
| `0` | Ready For Coding (if the rest of architect agrees) | `/sdlc-spdd-code` one T## |
| `1` | Needs Clarification — **not** Ready For Coding | Fix the canvas, re-fold |
| missing CLI | Today’s prose-only architect | Same as now |

`sdlc.sh advance` into `code` already refuses when readiness is not
Ready For Coding. DIF does not replace that string. It makes the
string *earned*.

Worked example we already fold: `FEAT-099` (“must paginate” +
non-goal Pagination) exits `1`. The right next command is
clarification, not code.

### Code — one operation, constraints in the prompt

`/sdlc-spdd-code` still implements exactly one approved T##.
The projection does not pick the Work ID or the operation;
`sdlc.sh next` / the pointer still do.

When a projection exists, the coding prompt cites it as a
**constraint set**, not as a task list:

- `PRESERVATION` / safeguard invariants are hard: do not drop auth,
  unrelated endpoints, required tests.
- Open T## rows that are *not* this operation stay
  `MissingObligation` — they are not scope creep for this session.
- Non-goals stay `CONSTRAINT`. They are not “Preserve: Non-goal: …”
  invariants (today’s fold still does that; see §4).

Syntax (DTO names, test style, file layout) may change. That is legal.

### Review — LLM writes the artifact; CLI owns fail-closed

1. Process: `sdlc.sh gate review` (canvas + ledger evidence).
2. Semantic, if DIF is present:
   `dif-fold.sh review --before <snap> --after <snap> [--canvas …]`.

The review agent still classifies findings (implementation mismatch,
canvas/intent mismatch, non-behavioral refactor). That taxonomy is
already in the orchestrator. DIF *computes* the intent/semantics
half. The agent must cite `VerificationResult`. It must not pass
review when the CLI exits `1`, and it must not ask “does this look
correct?” as the acceptance test.

### Sync / retro — typed holes, then lessons

Open operations (T03 on the order-status canvas) show up as
`MissingObligation`, not a forgotten checkbox. Sync records accepted
drift in the canvas; the next fold must match the new canvas.
`sdlc.sh accept` still promotes lessons. DIF does not write
`lessons.jsonl`.

### Guide (optional, never required)

If `CONTEXT_BACKEND=guide-dice`, DICE still retrieves. `dif-fold.sh
guide` may emit Decision / Pitfall JSONL that *quotes* the same
vocabulary the verifier checks. Absence of Neo4j or
`OPENAI_API_KEY` is not a failed day.

---

## 3. How the attach is wired (when we touch the orch repo)

Keep the contract a **file + exit code**, the same shape as Guide:

```text
spdd/canvas/<WORK-ID>.md                 human contract (commit)
        │
        ▼  dif-fold.sh fold|architect|review
.dif/projections/<WORK-ID>.json          SemanticModel (regenerable)
.dif/projections/<WORK-ID>.gate.json     stable {ready, conflicts, missing}
        │
        └─ orch templates: if CLI missing, skip; if exit 1, fail closed
```

Concrete rules for the orchestrator change (a later PR *there*, not
here):

1. **Detect, do not require.** A helper such as
   `resolve-dif-cli.sh` looks for `DIF_HOME`, a sibling
   `../embabel-dif`, or `dif-fold` on `PATH`. Missing → proceed.
2. **Do not put Java in `sdlc-engine`.** A shell wrapper next to
   `sdlc.sh gate` is enough: run process gate, then optional semantic
   gate. Parse `.gate.json` if present; otherwise treat exit `1` as
   blocked.
3. **Templates cite the result.** Architect / review / `next` copy
   already say “if Guide is absent, continue.” Add the same sentence
   for DIF, plus: exit `1` ⇒ Needs Clarification / review FAIL.
4. **Never commit the projection as source of truth.** `.dif/` may be
   gitignored in consumer apps, or committed only as a cache. The
   canvas remains the review surface.
5. **One Work ID dry-run first.** Prefer
   `examples/spring-boot-order-api` `FEAT-001-order-status-api` and
   the live-consumer seed `FEAT-001-hello-live` over inventing a
   parallel demo app.

---

## 4. What is true today (honest baseline)

Proven on this repo and on a Cloud Agent environment that also has
Docker, Neo4j, and sibling clones of orch + Guide:

| Check | Status |
| --- | --- |
| Same orch canvas folds the same way twice | Working (`FEAT-001-order-status-api`) |
| Open T03 → `MissingObligation` | Working |
| Requirement vs non-goal → architect exit `1` | Working (`FEAT-099`) |
| Review CLI can fail a fixture snapshot | Working (`login-auth-broken`) |
| `sdlc-engine` 2.0.0a6 + `embabel-neo4j` healthy + fold | Working in the three-way env |
| Orch templates / `sdlc.sh gate` call DIF | Hook scripts here; orch specs next |
| Stable `.gate.json` for a Python helper to read | Working |
| `IntentDiff.render()` RESULT line | Working (`RESULT: FAIL` when a required path is removed) |
| Non-goals classified as `CONSTRAINT` only | Working (no `Preserve: Non-goal:` invariants) |
| Review against a *real* orch diff, not login fixtures | Working (`order-status-auth-broken` + canvas safeguards) |
| Embabel required for `next` | Must stay false |

Until the orch attach exists, “integration” is a sibling checkout and
a human running `dif-fold.sh` beside `sdlc.sh next`. That is enough
to test the fold. It is not enough to change a developer’s day.

---

## 5. Integration test ladder

Each rung must be able to fail in an interesting way before we spend
complexity on the next. Skipping a rung is allowed when the stolen
idea does not apply; skipping the test is not.

| Rung | Where | What we prove | Done when |
| --- | --- | --- | --- |
| **A. Fold contract** | this repo | Five checks in `FoldContractTest` | Already done |
| **B. Harvested orch canvases** | this repo `examples/canvases/` | Real REASONS files, not imagined IR | Started; keep adding when a pair escapes |
| **C. Honest gates** | this repo CLI | Exit codes and render a machine can trust | Done (`.gate.json`; honest `IntentDiff`; non-goals stay CONSTRAINT) |
| **D. Sibling dry-run** | this VM / Cloud Agent env | `dif-fold.sh` / `check-canvas.sh` on orch canvases | `dif-orch-smoke.sh` |
| **E. Orch detect-and-skip** | orch repo | Missing CLI does not break `gate` / slash commands | A unit test with `DIF_HOME` unset matches today’s output |
| **F. Orch fail-closed** | orch repo | Fixture CLI exit `1` blocks Ready For Coding / review | Architect template + `advance` into `code` refuse; review command stops |
| **G. One Work ID loop** | orch example or live-consumer seed | Plan → fold → architect → (fake) code → review on one canvas | A scripted session, no Embabel, no Guide required |
| **H. Three-way env** | Cloud Agent | Docker + Neo4j + `sdlc-engine` + compiled `DifCli` | Fresh agent: `hello-world`, healthy `embabel-neo4j`, fold orch canvas |
| **I. Optional Guide quote** | orch-guide | `guide` JSONL can be appended; retrieve still works without it | Live ingest remains optional (`OPENAI_API_KEY`) |
| **J. Embabel on JVM targets** | this repo | GOAP reads the folded model, does not re-parse markdown | Only after G; orch still picks Work ID / T## |

Rungs E–G are orchestrator PRs. They depend on C (honest machine
output) more than they depend on Embabel or Guide.

A cheap “full day” script for G, once C exists:

```text
# 1. process gates still own files
sdlc.sh gate architect --work-id FEAT-001-order-status-api

# 2. semantic gate
dif-fold.sh architect --canvas spdd/canvas/FEAT-001-order-status-api.md
# expect 0, T03 missing, readyForImplementation=true

# 3. conflict corpus
dif-fold.sh architect --canvas <copy of FEAT-099>
# expect 1 — do not mark Ready For Coding

# 4. review fixture (until real orch snapshots exist)
dif-fold.sh review --before login-before.json --after login-auth-broken.json
# expect 1

# 5. Guide absent is OK
# 6. sdlc.sh next still prints a slash command, not a Java stack
```

---

## 6. Build order (the actual roadmap)

Ordered so each slice can ship without the next.

### Slice 1 — Make the CLI a contract a script can trust

**Here.** Finish the honest-gate work that fold iteration left open:

- `IntentDiff.passed()` / render must fail when required paths are
  removed.
- Canvas `CONSTRAINT` (non-goals) must not become Preserve invariants.
- Write `.dif/projections/<WORK-ID>.gate.json` with a stable schema,
  for example:

  ```json
  {
    "workId": "FEAT-001-order-status-api",
    "readyForImplementation": true,
    "blockingConflicts": [],
    "missingObligations": ["T03"]
  }
  ```

- Add `scripts/dif-orch-smoke.sh` that folds the copied orch
  `FEAT-001` / `FEAT-099` canvases and asserts exit codes.

**Done when:** a Python one-liner can decide Ready For Coding from
`.gate.json` alone, and `login-auth-broken` cannot print `RESULT: PASS`.
**Done.**

### Slice 2 — Script the sibling dry-run in the three-way env

**Here + environment.** The Cloud Agent env already clones orch and
Guide, starts Docker/Neo4j, and compiles `DifCli`. Check in (or
document) the smoke:

- `sdlc-engine --version`
- fold orch `FEAT-001` (T03, ready)
- architect `FEAT-099` (exit 1)
- `docker inspect embabel-neo4j` healthy

Do not start Embabel or Guide MCP for this slice.

**Done when:** a fresh agent can run the smoke after `start.sh`
without a human recalling the commands.

### Slice 3 — Silent detect-and-skip on existing commands

**Here + orch repo.** `scripts/orch-attach/check-canvas.sh` is the hook
existing architect / code / `next` templates may call. It runs
`architect --quiet` and prints one line (`dif=ready`, `dif=blocked`, or
`dif=skipped`). Missing CLI is skip (exit 0). Exit 1 means Needs
Clarification — not Ready For Coding. Agents do not get a fold dump.
Do not add a user-facing `dif-fold.sh next`. Do not wire orch review to
the login snapshot fixtures. `sdlc.sh gate` behavior is unchanged when
the CLI is absent.

**Done when:** orch CI with no `embabel-dif` checkout is green, and a
present CLI on FEAT-099 stops architect from setting Ready For Coding.
**Done** (`sdlc-spdd-orchestrator` #208).

### Slice 4 — Fail-closed attach on one Work ID

**Orch repo**, after slice 1. If the CLI is present:

- `gate architect` / architect command: exit `1` ⇒ cannot set Ready
  For Coding; prefer Needs Clarification.
- `gate review` / review command: exit `1` ⇒ stop; cite
  `VerificationResult`.
- `next` already prefers architect when readiness blocks coding;
  keep that, now backed by a projection when one exists.

Use a stub `dif-fold` in orch tests (exit 0 / 1 / missing) so orch CI
does not need Maven.

**Done when:** RELATIONSHIP success criteria 2 and 5 are true on one
real Work ID without asking an LLM whether the canvas “looks ready.”
**In progress here:** `review --quiet` on orch `order-status-*.json`;
orch review templates skip when snapshots are absent. Stub
`tests/test-optional-dif.sh` covers missing / exit 0 / exit 1 without Maven.

### Slice 5 — One scripted developer day

**Orch example or `tests/live-consumer` seed.** Drive
`FEAT-001-hello-live` or order-status through plan (fixture canvas) →
fold → architect → code *no-op or recorded diff* → review.

Still no Embabel. Guide remains optional.

**Done when:** RELATIONSHIP success criteria 1, 3, and 4 are true in
that loop (same fold twice; syntax can change; T03 is a
`MissingObligation`).
**Done** as `scripts/dif-orch-day.sh` (no Embabel process, Guide JSONL
only as an optional quote).

### Slice 6 — Optional Guide vocabulary, then Embabel

Only after slice 5. Project invariants into Guide as Decision /
Pitfall nodes. Then, for JVM targets only, put the folded
`SemanticModel` on an Embabel blackboard. The orchestrator still
chooses the Work ID and T##.

**Done when:** GOAP builds a `VerificationPlan` from the projection
without re-interpreting markdown, and a day without Guide or Embabel
still works.
**CLI half done:** `plan --projection` calls `VerificationPlanner` on
the folded model. Live Embabel / Guide ingest stay optional and must
not be required for `next`.

**Live E2E reuses the orch environment, does not invent a second
stack.** The orchestrator already boots Guide+Neo4j via
`SDLC_GUIDE_STACK_LIVE=1 ./tests/test-guide-stack-live.sh` (installer
APIs, NamedEntity projection, `GuideClient` persist/retrieve,
`engine/tests_e2e/test_guide_projection_roundtrip.py`). DIF's
`scripts/dif-live-e2e.sh` calls that harness with `GUIDE_KEEP=1`, then
quotes a fold through the same `ContextStore.persist_lesson` /
`GuideClient.work_subgraph` path, then boots Embabel
(`EmbabelLivePlatformTest`, `DIF_LIVE_EMBABEL=1`). Do not add a
second Neo4j, a second ingest, or a JVM inside `next`.

---

## 7. What would count as a failed integration

The idea is wrong — or we stop — if:

1. Developers stop reading the canvas because the JSON looks official.
2. `sdlc.sh next` or `gate` starts a JVM.
3. Copilot / Claude installs fail because Embabel is required.
4. Review can still go green while a required safeguard disappeared,
   as long as the agent says it looks correct.
5. A machine-ready `.gate.json` and a human-ready canvas disagree
   after a fold of the same file.
6. We merge the repos to make a demo look complete.

---

## 8. What we will not do on this path

- Merge `embabel-dif` into `sdlc-spdd-orchestrator`.
- Make `sdlc-engine` depend on Java, Embabel, Neo4j, or OpenAI.
- Replace `sdlc.sh gate` process checks with the fold (they answer a
  different question).
- Add a new human-facing language or a second canvas.
- Require Guide or `OPENAI_API_KEY` for architect / review.
- Treat `.dif/projections/` as the design contract.
- Start slice 3 in the orch repo before slice 1 is honest.
- Add a second `next` / daily ritual users must learn.

When someone *is* on the runbook, it stays claim → next → one phase →
one T##. DIF only changes what “Ready For Coding” and “review passed”
are allowed to mean. A day that never opened `next` is still a
correct day.
