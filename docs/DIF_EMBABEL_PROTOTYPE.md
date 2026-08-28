# DIF + Embabel Prototype

> **Status:** Concept / prototype specification  
> **Repository:** `jmjava/embabel-dif`  
> **Goal:** Explore how a Deterministic Intent Folding (DIF)-style semantic layer could complement Embabel's typed domain model, blackboard, and deterministic planners.

## 1. Purpose

This prototype explores a hybrid agent architecture in which:

- an **LLM** interprets natural language and generates implementation candidates,
- a **DIF-style layer** converts observations and intent into a stable semantic representation,
- **Embabel** plans and executes typed actions against that representation,
- a **deterministic verifier** checks whether the resulting state preserves declared intent,
- failures become new facts that cause Embabel to re-plan.

The key idea is:

```text
LLM = probabilistic interpreter / generator
DIF = semantic intent substrate
Embabel = deterministic planner / orchestrator
Verifier = deterministic acceptance boundary
```

The prototype is not intended to reproduce any proprietary Merly implementation of Deterministic Intent Folding. It is an experiment inspired by public discussion of DIF concepts such as intent, semantic relationships, deterministic refinement, folding, and verification.

---

## 2. Problem Statement

Most coding agents repeatedly infer architectural intent from raw source code, documentation, tickets, prompts, and repository history.

A typical loop is:

```text
User Request
    |
    v
LLM reads repository
    |
    v
LLM infers intent
    |
    v
LLM edits code
    |
    v
Tests
```

The problem is that the semantic model is mostly implicit. On every run, the model may reconstruct a slightly different understanding of:

- why a component exists,
- which behavior must be preserved,
- which constraints are architectural rather than incidental,
- which tests express real intent,
- which relationships are missing or inconsistent,
- which earlier decisions still constrain a new change.

The prototype moves important knowledge out of transient prompts and into explicit typed state.

```text
Intent / Evidence
      |
      v
Semantic Folding
      |
      v
Canonical Intent Model
      |
      v
Embabel Blackboard
      |
      v
GOAP Planning
      |
      v
Actions / LLM / Tools
      |
      v
Verification
      |
   +--+--+
   |     |
 PASS   FAIL
   |     |
 DONE   re-plan
```

---

## 3. Core Hypothesis

Reliable AI engineering does not require every component to be deterministic.

Instead, the system should place determinism at the boundaries where repeatability, traceability, and correctness matter.

### Probabilistic components are good at

- interpreting natural language,
- extracting candidate intent,
- generating code,
- proposing explanations,
- identifying possible relationships,
- exploring unfamiliar domains.

### Deterministic components are good at

- representing accepted facts,
- enforcing invariants,
- checking preconditions,
- selecting legal actions,
- maintaining state,
- validating acceptance criteria,
- detecting semantic regressions.

The prototype therefore uses the LLM as a **probabilistic compiler front end and code generator**, while DIF and Embabel provide the semantic and execution control plane.

---

## 4. Intent -> Semantics -> Syntax

The architecture treats software changes as three different layers.

```text
              INTENT
                |
                v
             SEMANTICS
                |
                v
              SYNTAX
```

### Intent

What should be true?

Example:

```text
Refresh tokens must rotate after use.
Existing Google login behavior must remain unchanged.
Existing Apple login behavior must remain unchanged.
Existing JWT claims must remain backward compatible.
```

### Semantics

What system properties follow from that intent?

```text
A used refresh token cannot be accepted again.
A token family must be identifiable.
Existing OAuth flows remain available.
The sessionToken claim remains present.
Legacy clients continue to authenticate.
```

### Syntax

Which source-code changes implement the semantics?

```text
TokenService.java
RefreshTokenEntity.java
OAuth2AuthorizationRepository.java
database migration
integration tests
```

The prototype attempts to preserve the first two layers independently of the generated syntax.

---

## 5. Proposed Architecture

```text
                       User / Issue / Event
                               |
                               v
                    +-----------------------+
                    |   Intent Interpreter  |
                    |         LLM           |
                    +-----------+-----------+
                                |
                         Candidate Facts
                                |
                                v
                    +-----------------------+
                    |      DIF Engine       |
                    |                       |
                    | normalize             |
                    | fold                  |
                    | relate                |
                    | derive                |
                    | detect conflicts      |
                    | preserve provenance   |
                    +-----------+-----------+
                                |
                         Semantic IR
                                |
                                v
                    +-----------------------+
                    |       Embabel         |
                    |                       |
                    | typed blackboard      |
                    | actions               |
                    | conditions            |
                    | goals                 |
                    | GOAP / Utility        |
                    +-----------+-----------+
                                |
                          planned action
                                |
                                v
              +-------------------------------------+
              |          Execution Layer            |
              |                                     |
              | repository reader / writer          |
              | LLM code generation                 |
              | compiler                            |
              | tests                               |
              | static analysis                     |
              | Git                                 |
              +------------------+------------------+
                                 |
                          Observed Result
                                 |
                                 v
                    +-----------------------+
                    |  Semantic Verifier    |
                    |                       |
                    | invariants            |
                    | intent diff           |
                    | evidence checks       |
                    | acceptance rules      |
                    +-----------+-----------+
                                |
                          +-----+-----+
                          |           |
                        PASS         FAIL
                          |           |
                          v           v
                        Goal      Failure Facts
                        Done           |
                                       v
                                  Embabel re-plan
```

---

## 6. Semantic Intermediate Representation

The first implementation should keep the DIF representation deliberately simple.

It does **not** need to implement a novel mathematical folding algorithm to test the architecture.

A useful MVP semantic IR can be represented as typed immutable objects.

### 6.1 Intent

```java
public record Intent(
    String id,
    IntentType type,
    String statement,
    Priority priority,
    Provenance provenance
) {}
```

Example:

```text
INT-001
type: REQUIREMENT
statement: "Refresh tokens must rotate after use."
priority: REQUIRED
source: issue-42
```

### 6.2 Invariant

```java
public record Invariant(
    String id,
    String description,
    VerificationStrategy strategy,
    Set<String> relatedIntentIds
) {}
```

Example:

```text
INV-001:
A consumed refresh token must never authenticate again.
```

### 6.3 Semantic Relation

```java
public record SemanticRelation(
    SemanticNode from,
    RelationType type,
    SemanticNode to,
    Provenance provenance
) {}
```

Initial relation types:

```text
REQUIRES
PRESERVES
CONFLICTS_WITH
IMPLEMENTS
VERIFIED_BY
DERIVED_FROM
DEPENDS_ON
AFFECTS
SUPERSEDES
```

### 6.4 Evidence

```java
public record Evidence(
    String id,
    EvidenceType type,
    String source,
    String observation
) {}
```

Evidence can come from:

- source code,
- tests,
- Git history,
- ADRs,
- issue descriptions,
- documentation,
- user statements,
- runtime results.

### 6.5 Verification Result

```java
public record VerificationResult(
    String invariantId,
    VerificationStatus status,
    String evidence,
    Optional<String> failureReason
) {}
```

---

## 7. What "Folding" Means in the Prototype

For this prototype, **folding** means reducing multiple observations into a canonical semantic assertion while preserving traceability back to the observations.

Example evidence:

```text
E1: Existing test asserts sessionToken claim exists.
E2: Mobile authentication code reads sessionToken.
E3: Commit history says claim was added for mobile compatibility.
E4: User request says existing login behavior must not change.
```

The folding engine derives:

```text
Invariant:
PRESERVE sessionToken JWT claim.
```

Conceptually:

```text
E1
E2 ----+
E3 ----+----> FOLD ----> INV-SESSION-TOKEN
E4 ----+
```

The fold is deterministic **after the candidate facts are accepted**.

The LLM may propose candidate evidence or relations, but the canonical representation should be produced by deterministic rules wherever practical.

### Unfolding

The reverse operation should also be possible.

```text
INV-SESSION-TOKEN
       |
       +--> E1 existing test
       +--> E2 mobile dependency
       +--> E3 historical decision
       +--> E4 current requirement
```

This gives every semantic constraint an explanation.

---

## 8. Embabel's Role

Embabel should not duplicate the semantic layer.

Embabel consumes the resulting typed artifacts and decides **what action to perform next**.

Embabel's blackboard is a natural place for objects such as:

```text
ChangeRequest
RepositorySnapshot
CandidateIntent
AcceptedIntent
Invariant
SemanticRelation
ImplementationPlan
CodeChange
TestResult
VerificationFailure
VerifiedChange
```

The planner reasons over object availability and conditions.

Example:

```text
ChangeRequest
     |
     v
ExtractCandidateIntent
     |
     v
CandidateIntent
     |
     v
FoldIntent
     |
     v
AcceptedIntent + Invariants
     |
     v
AnalyzeRepository
     |
     v
AffectedComponents
     |
     v
GenerateImplementation
     |
     v
CodeChange
     |
     v
RunTests
     |
     v
TestResult
     |
     v
VerifyIntent
     |
     +-------- PASS -------> VerifiedChange
     |
     +-------- FAIL -------> VerificationFailure
                                  |
                                  v
                             Embabel re-plans
```

---

## 9. Planner Strategy

The prototype should demonstrate at least two Embabel planner modes.

### GOAP

Use GOAP when the semantic requirements are known.

```text
Known intent
Known invariants
Known actions
Known goal
      |
      v
GOAP
```

This is the main deterministic execution path.

### Supervisor / LLM-directed planning

Use a flexible planner only when the domain is not yet sufficiently understood.

```text
Unknown domain
      |
      v
Supervisor exploration
      |
      v
new evidence
      |
      v
DIF folding
      |
      v
new typed knowledge
      |
      v
future GOAP execution
```

A long-term design principle is:

> As the domain becomes better understood, behavior should migrate from prompts into typed facts, rules, invariants, and deterministic actions.

---

## 10. Prototype Use Case

The first end-to-end scenario should be deliberately narrow.

### User request

```text
Add refresh-token rotation without changing existing login behavior.
```

### Existing repository evidence

```text
Google OAuth login exists.
Apple OAuth login exists.
Authorization Code Flow is used.
JWT contains sessionToken.
Integration tests assert existing login behavior.
Refresh tokens currently remain valid after use.
```

### Derived intent

```text
INT-001 Add refresh-token rotation.
INT-002 Preserve Google login.
INT-003 Preserve Apple login.
INT-004 Preserve Authorization Code Flow.
INT-005 Preserve sessionToken claim.
```

### Derived invariants

```text
INV-001 A consumed refresh token cannot be reused.
INV-002 Google OAuth integration tests continue to pass.
INV-003 Apple OAuth integration tests continue to pass.
INV-004 sessionToken remains present and compatible.
INV-005 Existing authorization-code clients continue to authenticate.
```

### Planner goal

```java
public record RefreshTokenRotationImplemented(
    CodeChange change,
    VerificationReport verificationReport
) {}
```

The goal is achieved only when all required invariants pass.

---

## 11. Example Embabel Domain Objects

```java
public record ChangeRequest(String text) {}

public record CandidateIntent(
    List<Intent> intents,
    List<Evidence> evidence
) {}

public record SemanticModel(
    List<Intent> intents,
    List<Invariant> invariants,
    List<SemanticRelation> relations
) {}

public record RepositoryAnalysis(
    List<String> affectedFiles,
    List<Evidence> evidence
) {}

public record ProposedChange(
    List<FileChange> files
) {}

public record TestExecution(
    boolean passed,
    List<TestResult> results
) {}

public record SemanticVerification(
    boolean passed,
    List<VerificationResult> results
) {}

public record VerificationFailure(
    List<VerificationResult> failures
) {}

public record VerifiedChange(
    ProposedChange change,
    SemanticVerification verification
) {}
```

---

## 12. Example Embabel Actions

The exact Embabel APIs can evolve; the prototype should follow the version selected in the build.

Conceptually:

```java
@Action
CandidateIntent interpretIntent(ChangeRequest request, Ai ai) {
    // LLM produces structured candidate intent and evidence.
}

@Action
SemanticModel foldIntent(CandidateIntent candidate) {
    // Deterministic normalization, relation building,
    // conflict checks, and invariant derivation.
}

@Action
RepositoryAnalysis analyzeRepository(
        ChangeRequest request,
        SemanticModel semanticModel) {
    // Inspect repository and gather concrete evidence.
}

@Action
ProposedChange generateChange(
        SemanticModel semanticModel,
        RepositoryAnalysis analysis,
        Ai ai) {
    // LLM generates implementation constrained by semantic model.
}

@Action
TestExecution runTests(ProposedChange change) {
    // Build/test execution.
}

@Action
SemanticVerification verify(
        SemanticModel semanticModel,
        ProposedChange change,
        TestExecution tests) {
    // Deterministic verification.
}

@AchievesGoal
@Action
VerifiedChange accept(
        ProposedChange change,
        SemanticVerification verification) {

    if (!verification.passed()) {
        throw new IllegalStateException("Semantic verification failed");
    }

    return new VerifiedChange(change, verification);
}
```

A failure path should emit typed failure artifacts instead of merely throwing an opaque error.

```java
VerificationFailure
```

That object becomes new blackboard state from which Embabel can select a repair action.

---

## 13. Deterministic Verification

The most important boundary in the prototype is verification.

The verifier should not ask:

```text
"LLM, does this change look correct?"
```

It should ask deterministic questions whenever possible.

Examples:

```text
Did required tests pass?
Does the generated JWT still contain sessionToken?
Can an already consumed refresh token authenticate?
Are Google OAuth endpoints still registered?
Are Apple OAuth endpoints still registered?
Did a public API signature disappear?
Did a prohibited dependency appear?
```

Verification strategies can include:

- JUnit tests,
- ArchUnit,
- compiler checks,
- schema inspection,
- API contract tests,
- AST inspection,
- static analysis,
- deterministic repository queries.

LLM review may be included as **additional evidence**, but should not be the only verifier of required invariants.

---

## 14. Intent Diff

A useful prototype feature is a semantic equivalent of `git diff`.

### Before

```text
Refresh token:
  reusable = true

Login providers:
  GOOGLE
  APPLE

JWT required claims:
  sessionToken
```

### Desired

```text
Refresh token:
  reusable = false
  rotates = true

Login providers:
  GOOGLE
  APPLE

JWT required claims:
  sessionToken
```

### Proposed implementation verification

```text
SEMANTIC DIFF

+ refresh-token.rotates
- refresh-token.reusable

UNCHANGED:
= provider.GOOGLE
= provider.APPLE
= jwt.claim.sessionToken

RESULT: PASS
```

This is more useful to an autonomous agent than a source-code diff alone.

---

## 15. Repository Intent Memory

A future phase should persist semantic intent across runs.

Possible structure:

```text
.dif/
  intents/
  invariants/
  evidence/
  relations/
  snapshots/
```

Example:

```text
.dif/
  intents/
    auth.yaml
  invariants/
    auth.yaml
  relations/
    auth.yaml
  evidence/
    git.yaml
```

An alternative is a small graph database.

The initial prototype should begin with files because they are:

- versioned with the repository,
- inspectable by humans,
- diffable,
- reproducible,
- simple to test.

---

## 16. Semantic Provenance

Every accepted intent or invariant should explain **why it exists**.

Example:

```yaml
id: INV-JWT-SESSION-TOKEN
statement: sessionToken claim must remain backward compatible

derivedFrom:
  - type: TEST
    source: src/test/.../JwtClaimsTest.java

  - type: CODE
    source: mobile-client/AuthTokenReader.java

  - type: GIT_COMMIT
    source: 7e93982

  - type: USER_REQUIREMENT
    source: CHANGE-REQUEST-42
```

This creates an architectural memory layer that an agent can inspect rather than reconstruct from scratch.

---

## 17. Conflict Detection

The folding engine should identify incompatible intent.

Example:

```text
INT-100:
"Refresh tokens must be single-use."

INT-101:
"Existing clients must be able to reuse the same refresh token indefinitely."
```

Result:

```text
IntentConflict {
    left: INT-100
    right: INT-101
    reason: MUTUALLY_EXCLUSIVE
}
```

Embabel should not proceed to implementation while a required conflict is unresolved.

The planner could instead select:

```text
RequestClarification
SearchRepositoryForCompatibilityEvidence
FindMigrationPath
```

---

## 18. Absence Reasoning

One of the most interesting experiments is reasoning about what **should exist but does not**.

Example:

```text
Intent:
Refresh tokens rotate.

Derived requirements:
token family identifier
consumed-token state
replay detection
rotation integration test
```

Repository inspection finds:

```text
token family identifier     PRESENT
consumed-token state        PRESENT
replay detection            PRESENT
rotation integration test   MISSING
```

The missing relationship becomes a typed fact:

```java
public record MissingObligation(
    String obligation,
    String derivedFromIntent
) {}
```

Embabel can then plan an action to satisfy it.

---

## 19. Suggested Project Structure

```text
embabel-dif/
|
+-- README.md
+-- pom.xml
|
+-- docs/
|   +-- DIF_EMBABEL_PROTOTYPE.md
|
+-- src/main/java/.../
|   |
|   +-- domain/
|   |   +-- Intent.java
|   |   +-- Invariant.java
|   |   +-- Evidence.java
|   |   +-- SemanticRelation.java
|   |   +-- SemanticModel.java
|   |
|   +-- dif/
|   |   +-- IntentFolder.java
|   |   +-- RuleBasedIntentFolder.java
|   |   +-- ConflictDetector.java
|   |   +-- ObligationDeriver.java
|   |
|   +-- agent/
|   |   +-- DifEmbabelAgent.java
|   |   +-- IntentActions.java
|   |   +-- RepositoryActions.java
|   |   +-- VerificationActions.java
|   |
|   +-- verifier/
|       +-- SemanticVerifier.java
|       +-- VerificationRule.java
|
+-- src/test/java/.../
|
+-- .dif/
    +-- intents/
    +-- invariants/
    +-- evidence/
    +-- relations/
```

---

## 20. MVP Phases

### Phase 1 - Typed semantic model

Build:

- `Intent`
- `Invariant`
- `Evidence`
- `SemanticRelation`
- `SemanticModel`
- deterministic folding rules
- provenance tracking

No autonomous code writing yet.

### Phase 2 - Embabel planning

Add Embabel actions for:

```text
ChangeRequest
-> CandidateIntent
-> SemanticModel
-> RepositoryAnalysis
-> VerificationPlan
```

Demonstrate that the GOAP planner derives the execution sequence from typed dependencies.

### Phase 3 - LLM implementation generation

Allow the LLM to generate a proposed code change from:

```text
SemanticModel + RepositoryAnalysis
```

The prompt should receive the semantic model as a hard constraint set.

### Phase 4 - deterministic verification

Add deterministic checks and emit:

```text
SemanticVerification
```

or:

```text
VerificationFailure
```

### Phase 5 - automatic repair loop

Allow failure facts to drive a new Embabel plan.

```text
generate
   |
   v
verify
   |
 FAIL
   |
   v
VerificationFailure
   |
   v
repair
   |
   v
verify
```

### Phase 6 - persistent intent memory

Persist accepted semantic objects under `.dif/`.

Compare semantic state across commits.

---

## 21. Prototype Success Criteria

The prototype is successful if it demonstrates all of the following:

1. The same accepted intent produces the same canonical semantic model.
2. Embabel receives typed semantic facts rather than only a natural-language prompt.
3. The planner selects actions from typed preconditions/effects.
4. Generated implementation can vary while required semantic invariants remain fixed.
5. Verification failures become explicit typed facts.
6. Embabel can re-plan from those failures.
7. Every important invariant can be traced to evidence.
8. The semantic model can be versioned independently of source-code implementation.
9. A semantic diff can describe what behavior changed and what was preserved.
10. At least one missing obligation can be discovered from intent plus repository evidence.

---

## 22. What This Prototype Is Not

This prototype is **not**:

- a claim to implement Merly's proprietary DIF algorithm,
- a replacement for Embabel's planner,
- a new LLM,
- a conventional RAG system,
- just another prompt template,
- an attempt to make all AI deterministic.

The experiment is specifically about creating a **deterministic semantic control layer around probabilistic AI behavior**.

---

## 23. Key Design Principle

The architecture should continuously try to move stable knowledge from probabilistic reasoning into explicit representations.

```text
                 DOMAIN MATURITY

LOW                                              HIGH
 |                                                |
 v                                                v

LLM exploration
      |
      v
candidate evidence
      |
      v
DIF folding
      |
      v
typed semantic knowledge
      |
      v
Embabel deterministic planning
      |
      v
deterministic verification
```

In other words:

> **Use stochastic reasoning to discover knowledge. Use deterministic representations and planners to operationalize knowledge once it is understood.**

---

## 24. Long-Term Vision

The larger idea is a coding agent with persistent architectural memory.

Instead of asking an LLM to infer the entire repository's purpose during every task, the repository gradually accumulates a machine-readable semantic layer:

```text
Git stores:
WHAT changed.

DIF-style semantic memory stores:
WHY it changed.
WHAT must remain true.
WHAT depends on it.

Embabel determines:
WHAT to do next.

LLMs provide:
interpretation, exploration, and generation.
```

The resulting loop is:

```text
                  +----------------+
                  | Human / Event  |
                  +-------+--------+
                          |
                          v
                  +----------------+
                  |      LLM       |
                  | interpretation |
                  +-------+--------+
                          |
                          v
                  +----------------+
                  |      DIF       |
                  | semantic model |
                  +-------+--------+
                          |
                          v
                  +----------------+
                  |    Embabel     |
                  | plan / execute |
                  +-------+--------+
                          |
                          v
                  +----------------+
                  | LLM + Tools    |
                  | implementation |
                  +-------+--------+
                          |
                          v
                  +----------------+
                  | DIF Verifier   |
                  +-------+--------+
                          |
                     +----+----+
                     |         |
                   PASS       FAIL
                     |         |
                     v         v
                    DONE    RE-PLAN
```

This is the architecture the `embabel-dif` repository should attempt to validate.

---

## 25. References

- Prototype inspiration: *LLMs Have Plateaued — Why Determinism (DIF) Is the Substrate Stochastic AI Needs*  
  https://youtu.be/q4CIvRBCogs
- Embabel Agent Framework documentation  
  https://docs.embabel.com/embabel-agent/
- Repository  
  https://github.com/jmjava/embabel-dif

---

## 26. Next Implementation Step

The first code milestone should be intentionally small:

```text
ChangeRequest
    |
    v
CandidateIntent              <-- LLM
    |
    v
SemanticModel                <-- deterministic fold
    |
    v
Embabel Blackboard
    |
    v
VerificationPlan             <-- GOAP
```

No repository editing is required for milestone one.

The purpose of the first milestone is to prove the architectural boundary:

> **Can probabilistically extracted intent be converted into stable typed semantic state that a deterministic Embabel planner can reason over?**

If the answer is yes, code generation and repository verification can be layered on afterward.
