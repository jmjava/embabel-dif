# Handoff: dogfood Cursor linkage (new Cloud Agent)

You are a **new** Cloud Agent. The previous VM could not see
`CORRECT_CURSOR_KEY` because that secret was added after boot.

## Goal

Prove **real Cursor linkage** on `jmjava/dogfood-api`: spawn a Cursor
agent (`@cursor/sdk`) that executes `/sdlc-next` and an unstructured
`persist-lesson` (no invented `FEAT-ADHOC`). Do **not** use the
injected `CURSOR_API_KEY` (`sk-proj…` → 401). Use **`CORRECT_CURSOR_KEY`**.

## Do this first

```bash
# from embabel-dif checkout
./scripts/bootstrap-cursor-agent-day.sh
```

That script: checks the secret, checks out both feature branches,
installs the full ecosystem if needed, then runs the spawn.

## Branches / PRs

| Repo | Branch | PR |
| --- | --- | --- |
| `jmjava/embabel-dif` | `cursor/dogfood-cursor-agent-148e` | https://github.com/jmjava/embabel-dif/pull/18 |
| `jmjava/dogfood-api` | `cursor/dogfood-cursor-agent-148e` | https://github.com/jmjava/dogfood-api/pull/2 |
| `jmjava/sdlc-spdd-orchestrator` | `main` | already merged (persist without Work ID) |

## Rules

- Full install: Cursor+Copilot+Claude+Guide marker. No partial / skip-if-missing.
- Refuse `CURSOR_API_KEY` unless it starts with `cursor_`.
- `./mvnw test` is the API only. It does not prove the Cursor link.
- Do not commit generated `.cursor/commands/sdlc-*` from `up.sh`.
- After a green spawn: commit any harness fixes, push, update the two PRs.

## If `CORRECT_CURSOR_KEY` is still empty

The secret is on the environment but this run was not given it. Stop and
tell the user the new agent also booted without `CORRECT_CURSOR_KEY`.
Do not fall back to the stand-in `run-in-env.sh` and call that the link.
