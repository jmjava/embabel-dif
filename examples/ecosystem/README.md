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
    orch process   DIF fold    Guide (opt)
    claim / next   no JVM      retrieve
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
| Process + Dashboard | `jmjava/sdlc-spdd-orchestrator` | `init-project`, `claim`, `console :5051` |
| Fold | `jmjava/embabel-dif` | `dif-fold.sh architect --quiet` → `.gate.json` |
| Retrieve | `jmjava/orch-guide` | Only with `--with-guide` |

Both modes run in the same `up.sh` pass: a Work ID harvest on `api`,
and an area-only pitfall on `notify` with **no** invented FEAT.
Refresh still does not fold.

`--setup-only` is what `./scripts/dif-dual-mode-e2e.sh` uses.
