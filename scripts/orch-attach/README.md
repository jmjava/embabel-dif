# Silent orch attach

Existing SDLC-SPDD commands may call these scripts. Users keep
`sdlc.sh next` and the slash commands. There is no second daily driver.

| Script | Job |
| --- | --- |
| `resolve-dif-cli.sh` | Print `dif-fold.sh` if present; exit 1 if `DIF_DISABLED=1` or missing |
| `check-canvas.sh --canvas <file>` | Run `architect --canvas`; exit 0 if ready or skipped, 1 if blocked |

Orch templates should treat a missing CLI as normal, the same way they
treat a missing Guide backend.

The orchestrator repo change (architect / code / next specs + generated
adapters) is in
`sdlc-spdd-orchestrator-optional-dif.patch`. Apply it in that repo:

```text
git apply scripts/orch-attach/sdlc-spdd-orchestrator-optional-dif.patch
```

This checkout could not push to `jmjava/sdlc-spdd-orchestrator` (403).
