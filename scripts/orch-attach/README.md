# Silent orch attach

Existing SDLC-SPDD commands may call these scripts. Users keep
`sdlc.sh next` and the slash commands. There is no second daily driver.

| Script | Job |
| --- | --- |
| `resolve-dif-cli.sh` | Print `dif-fold.sh` if present; exit 1 if `DIF_DISABLED=1` or missing |
| `check-canvas.sh --canvas <file>` | Run `architect --quiet --canvas`; one line `dif=ready\|blocked\|skipped`; exit 0 if ready or skipped, 1 if blocked |
| `check-review.sh --before --after [--canvas]` | Run `review --quiet`; skip if CLI or snapshot files are missing. Do not use login fixtures. |

Orch templates should treat a missing CLI as normal, the same way they
treat a missing Guide backend. Missing review snapshots are also skip —
do not invent a snapshot ritual.

Architect / code / next attach landed in
`sdlc-spdd-orchestrator` [#208](https://github.com/jmjava/sdlc-spdd-orchestrator/pull/208).
The patch file is historical.
