# Ecosystem bootstrap

One consumer tree, every layer present. The playable product is
[`jmjava/dogfood-api`](https://github.com/jmjava/dogfood-api). This
page is the map so a blog / generation tool can quote it.

```text
                    ┌─────────────────────────────┐
                    │  dogfood-api  (product)     │
                    │  GET /api/orders?email=     │
                    │  auth safeguard + notify    │
                    └──────────────┬──────────────┘
           structured │            │ unstructured
                      ▼            ▼
        FEAT-001 canvas        kind+area+body
        dif-fold architect     persist-lesson
        .gate.json             source=adhoc-prompt
                      │            │
                      ▼            ▼
              sdlc.sh console :5051  (readout)
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
    orch process   DIF fold    Guide marker
    claim / next   required    retrieve (live stack optional)
```

```bash
# from this repo
./scripts/ecosystem-up.sh
# or from a clone of the consumer
git clone https://github.com/jmjava/dogfood-api.git && cd dogfood-api && ./scripts/up.sh
```

| Piece | Repo | What `up.sh` does |
| --- | --- | --- |
| Product | `jmjava/dogfood-api` | This is `--target` |
| Process + Dashboard | `jmjava/sdlc-spdd-orchestrator` | **Full** `init-project --cursor --copilot --claude --with-guide`, then `claim` + `console :5051` |
| Fold | `jmjava/embabel-dif` | Required `dif-fold.sh architect --quiet` → `.gate.json` |
| Retrieve | `jmjava/orch-guide` | Marker always; live Neo4j only with `--with-guide-stack` |

This demo does **not** do a Cursor-only / skip-if-missing install. Missing DIF, a blocked fold, or an orch that still requires `work_id` to capture is a hard fail.

Both modes run in the same `up.sh` pass: a Work ID harvest on `api`,
and an area-only pitfall on `notify` with **no** invented FEAT.
Refresh still does not fold.

`--setup-only` is the install + harvests. The **agent-linkage** test is
`./scripts/up.sh --prove` / `dogfood-api/scripts/agent-day/run.sh`.
It needs Cloud secret **`CORRECT_CURSOR_KEY`** (Integrations user key),
then a new agent run. GitHub `./mvnw test` is the API only.

From this repo: `./scripts/dogfood-cursor-day.sh`.
