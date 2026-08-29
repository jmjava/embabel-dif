# Dashboard full-flow example

One Work ID, two accepted canvases, one board. This is the synergy
path: DIF freezes; the orch Dashboard **reads**. Refresh does not fold.

```text
Jira-shaped stub (DASH-1)
        │
        ▼  human accepts REASONS
FEAT-DASH-flow.blocked.md     pagination AND non-goal Pagination
        │
        ▼  dif-fold.sh architect --quiet
.dif/projections/FEAT-DASH-flow.gate.json     dif=blocked
        │
        ▼  Dashboard Today / Active work
suggestion dif-blocked — fix the canvas
        │
        ▼  contract changes (same Work ID)
FEAT-DASH-flow.ready.md       pagination; non-goal is Auth changes
        │
        ▼  re-fold (old JSON is trash)
dif=ready
        │
        ▼  Dashboard Refresh
no dif-blocked row; chip says dif=ready
        │
        ▼  optional ad hoc harvest
staged pitfall on Memory — not a new freeze
```

```bash
./scripts/dif-dashboard-e2e.sh
# optional Vue click-through (needs console-ui/dist + Playwright):
DIF_DASHBOARD_PLAYWRIGHT=1 ./scripts/dif-dashboard-e2e.sh
```

Separation of work stays in one flow: orch orients and displays; DIF
classifies; Embabel is not started; Guide is not required.
