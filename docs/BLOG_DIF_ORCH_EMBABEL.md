---
purpose: publication-source
audience: blog / generation tool
status: current-plan
topics:
  - deterministic-intent-folding
  - embabel
  - sdlc-spdd-orchestrator
  - dice-vs-dif
  - take-it-only-as-far-as-it-makes-sense
not: merly-reimplementation
---

# Three layers, one day: DIF, the orchestrator, and Embabel

**Publication source.** This page is the plan to quote from: what each
layer is for, where we stop, and why we will not collapse them. Engineering
detail lives in [REASONING.md](REASONING.md),
[RELATIONSHIP_SDLC_SPDD.md](RELATIONSHIP_SDLC_SPDD.md), and
[ORCH_INTEGRATION_ROADMAP.md](ORCH_INTEGRATION_ROADMAP.md).

This is **not** an implementation of any proprietary Merly DIF algorithm.

---

## The sentence

Reliable AI engineering does not require every component to be
deterministic. It requires determinism at the boundaries where
repeatability, traceability, and correctness matter.

Use stochastic reasoning to **discover** knowledge. Use deterministic
representations to **operationalize** knowledge once it is understood.

---

## The failure mode

Coding agents are good at reading a repository and *sounding* like they
understand it. The understanding is usually implicit and disposable:

```text
prompt + files + luck  →  a one-off theory of the system  →  a patch
```

The next session starts from zero. It may decide that `sessionToken` was
incidental, that Google login can move, or that an existing test is
optional. Nothing in the process *remembers* which of those beliefs were
load-bearing.

`sdlc-spdd-orchestrator` already attacks the **process** half: one Work
ID, one REASONS Canvas, one phase at a time. Assistants are not allowed
to invent a parallel workflow.

That is necessary and not sufficient. The canvas is still prose.
Architect, review, and sync still ask an LLM to *compare* the canvas to
a diff. Comparison is exactly where implicit intent creeps back in.

The remaining hole is **checkability**. Process gates ask “do the
prerequisite files exist?” They do not ask “did this canvas contradict
itself?” or “did this diff drop a safeguard?”

---

## Three questions, three systems

```text
Planning / requirements     why are we doing this?
REASONS Canvas              what must ship (human contract)
DIF SemanticModel           what must remain true (machine contract)
Embabel GOAP                what action to take on typed facts (optional)
DICE / Guide graph          what did we learn before (retrieval)
SDLC phases                 who is allowed to act
```

| Layer | Owns this question | Must not own |
| --- | --- | --- |
| **Orchestrator** | Who acts when? One Work ID, one canvas, one phase. `next`, `gate`, claim, slash commands. | Folding facts. Starting a JVM. Being a planner. |
| **DIF** | What must stay true? Same accepted canvas → same model. Conflicts and missing operations fail closed. | Daily orientation. Picking the Work ID. Replacing the canvas. |
| **Embabel** | What action to take on *already folded* facts (optional JVM path). | The fold itself. `sdlc.sh next`. The human contract. |

They answer different questions. Used together, DIF can make the canvas
*checkable* without replacing the canvas.

Git stores what changed. A DIF-style layer stores why it had to, and
what must still be true. Embabel, when present, decides what to do next.
The orchestrator decides who is allowed to act.

---

## DICE is not DIF

The orchestrator already has **Guide DICE** as an optional working store.
It is easy to smash the two acronyms together. They do opposite jobs.

```text
DICE  = retrieve what we already believe
        (lessons, decisions, pitfalls, area subgraphs)

DIF   = freeze what must remain true, then verify it
        (intents, invariants, conflicts, obligations)
```

DICE answers “what did previous work in this area learn?”
DIF answers “may this change proceed, and did it preserve the contract?”

Both can project from the same committed files. Neither replaces the
canvas or the lessons ledger. The ledger stays the system of record;
SQLite, Guide, and `.dif/projections/` are regenerable.

---

## Why three repos, not one runtime

Merging Embabel or DIF into the orchestrator would fight its design: it
is an installable operating model, not a compiled agent runtime.

| If we put DIF inside the orchestrator | Cost |
| --- | --- |
| `sdlc-engine` grows a JVM agent | `next` / `gate` stop being assistant-agnostic |
| Canvas becomes a Java IR | Humans lose the markdown contract |
| Embabel becomes required | Copilot / Claude installs pay for a planner they do not run |

The contract between them is a file:

```text
spdd/canvas/<WORK-ID>.md          human source of truth (commit)
        │
        ▼  fold (deterministic after accept)
.dif/projections/<WORK-ID>.json   machine projection (disposable)
.dif/projections/<WORK-ID>.gate.json
        │
        └─ orch may read the exit code; it does not start the JVM to run next
```

Opt in the same way Guide is opted in: useful when present, ignored when
not. Never make `sdlc-engine` start a JVM to run `next`.

A canvas is already a candidate intent. We do not need a new human
artifact. We need a projection.

---

## Take the idea only as far as it makes sense

The knowledge that actually hurts is not “which slash command is next.”
The orchestrator already answers that from the phase pointer.

The tax is this: you can follow the runbook perfectly and still ship a
contradictory canvas, mark **Ready For Coding** in prose, or pass review
because the change “looks right.”

That is the hole DIF is for. The filter for every attach is:

> Does this make the existing orchestrator commands harder to get wrong,
> without adding a new ritual?

| Do | Do not |
| --- | --- |
| Keep claim → `next` → architect → one T## → review as the only user surface | Add `dif-fold.sh next` as a second daily driver |
| When DIF is installed, architect cannot earn Ready For Coding on a requirement vs non-goal clash | Teach users fold / projection / `.gate.json` as a parallel workflow |
| When DIF is missing, the day is unchanged | Require Embabel, Java, Neo4j, or OpenAI to run `next` |
| Review can fail a dropped safeguard without asking whether it looks correct | Replace `sdlc.sh gate` process checks with the fold (they answer a different question) |

A new orchestrator user who never heard of DIF should have a **better**
day if it is installed, and the **same** day if it is not.

The thing we declined — a second orientation command people must learn —
would have been DIF doing the orchestrator’s job. Silent fail-closed on
existing architect / code is DIF doing DIF’s job: the readiness string
becomes earned. The runbook stays the orchestrator’s. When the hook
runs, the agent sees one line (`dif=ready`, `dif=blocked`, or
`dif=skipped`) — not a fold dump they then have to interpret.

Embabel stays later and optional. Wiring it into `next` would be the
other collapse.

---

## What “ready” is allowed to mean

After a fold:

- A mutually exclusive pair (“must paginate” vs “non-goal: pagination”)
  blocks Ready For Coding. The next command is clarification, not code.
- An open operation (T03) shows up as a `MissingObligation`, not a
  forgotten checklist box.
- Two folds of the same accepted canvas produce the same model.
- Syntax may change (DTO names, test style). Preservation of auth and
  unrelated endpoints must not.
- Review can fail a required safeguard without asking an LLM “does this
  look correct?”

Until those are true, DIF stays a sibling experiment. After they are
true, it becomes a library the orchestrator can opt into the same way it
opts into Guide.

---

## Path (order matters)

```text
1. DIF      canvas → SemanticModel CLI      no Embabel          (working)
2. Orch     architect / code attach         if CLI present      (silent, opt-in)
3. DIF      Embabel GOAP for JVM targets    orch still picks Work ID / T##
4. Optional project invariants into Guide   shared vocabulary, still not required
```

Step 1 first: if the same canvas does not fold the same way twice,
nothing downstream is trustworthy.

Step 2 next: that is where the orchestrator still asks an LLM to be a
verifier. Attaching an exit code is cheaper than inventing a new phase.

Embabel later: planning over typed facts is valuable, and it is the
part that must not leak into the orchestrator’s runtime.

Guide last: retrieval already works. Vocabulary alignment is not a
prerequisite for checkability.

---

## What would falsify this

The work is not “write more markdown.” The idea is wrong if:

1. Two folds of the same accepted canvas disagree.
2. Review still cannot fail a required safeguard without “looks correct.”
3. Syntax changes flip a required invariant.
4. Open operations do not show up as missing obligations.
5. A requirement vs non-goal pair does not block Ready For Coding.
6. Teams stop reading the canvas because they treat the JSON as source
   of truth. The projection must remain regenerable and disposable.
7. `sdlc.sh next` or `gate` starts a JVM.
8. Developers need a second `next` to have a correct day.

---

## What this is not

- Not a Merly reimplementation, and not a claim that we implemented one
- Not a replacement for Embabel’s planner
- Not a replacement for the REASONS Canvas
- Not a replacement for DICE / Guide retrieval
- Not a conventional RAG index
- Not “just another prompt template”
- Not an attempt to make all AI deterministic
- Not a reason to compile the orchestrator into an agent runtime
- Not a new human-facing language

---

## One-line glossary for a post

| Term | Meaning |
| --- | --- |
| REASONS Canvas | Human design contract in the repo. Source of truth. |
| Fold | Deterministic map from an accepted canvas to a typed semantic model. |
| Projection | Regenerable JSON of that model. Never a second contract. |
| Ready For Coding | Orch readiness string. DIF may *earn* it; orch still writes it. |
| Process gate | Files / ledger exist (`sdlc.sh gate`). |
| Semantic gate | The canvas may proceed (`dif-fold.sh architect`, exit 1 = no). Review of an orch canvas uses safeguard snapshots, not login fixtures. |
| Embabel | Optional planner over folded facts. Not the fold. Not `next`. |
