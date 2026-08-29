# Handoff: dogfood Cursor linkage (new Cloud Agent)

**Proven** on Cloud Agent `bc-25b69a3b-12d5-5c12-944d-fc8f5c484a65`
(2026-08-29): `CORRECT_CURSOR_KEY` was present (`crsr_…`, len=69, not
`sk-`). `./scripts/bootstrap-cursor-agent-day.sh` spawned a real Cursor
agent and both runs finished:

| | |
| --- | --- |
| agentId | `agent-3bbf59ed-caea-4e44-b8b4-3d56d5a45476` |
| `/sdlc-next` | `run-62666a22-b417-4ed6-97a3-286c3a36b52a` `finished` |
| unstructured persist | `run-9a3308a7-748d-41de-978c-dde56952378f` `finished` |
| receipt | `hitCursor=true` `mode=sdk-spawn` |
| staged lesson | `pitfall:(none):notify:dogfood-agent-day` (no `FEAT-ADHOC`) |

`agent-day verify: OK` / `dogfood Cursor agent day: OK`.

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

## Spend locks

The spawn is locked to **`composer-2.5`** (cheapest standard Cursor model, not Fast).
`run.sh` refuses any other model. Hard caps: 2 sends, 8 minutes per send,
150k tokens total, 12 minute wall clock, then `timeout` kills the process.
Do not raise these. Do not spawn extra Cloud Agents.

## Secrets

Never write `CORRECT_CURSOR_KEY` / `CURSOR_API_KEY` values into git, receipts,
or logs. GitGuardian coverage is the **GitHub App** on the dashboard (no
repo Actions key). Do not add `ggshield` CI unless someone wants a second
gate and is willing to store `GITGUARDIAN_API_KEY`.
`scripts/guard-no-secret-leak.sh` fails if a live secret value is in
tracked files.

## Rules

- Full install: Cursor+Copilot+Claude+Guide marker. No partial / skip-if-missing.
- Refuse `CURSOR_API_KEY` unless it starts with `cursor_` or `crsr_`.
  This environment's Integrations key is `crsr_…`. `sk-proj…` → 401.
- `./mvnw test` is the API only. It does not prove the Cursor link.
- Do not commit generated `.cursor/commands/sdlc-*` from `up.sh`.
- After a green spawn: commit any harness fixes, push, update the two PRs.

## If `CORRECT_CURSOR_KEY` is still empty

The secret is on the environment but this run was not given it. Stop and
tell the user the new agent also booted without `CORRECT_CURSOR_KEY`.
Do not fall back to the stand-in `run-in-env.sh` and call that the link.
