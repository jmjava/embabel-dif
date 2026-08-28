# Review snapshots

| File | What it is |
| --- | --- |
| `login-*.json` | Prototype refresh-token / JWT paths (`provider.GOOGLE`, …) |
| `order-status-before.json` | Harvested from orch `FEAT-001-order-status-api` **S - Safeguards** |
| `order-status-auth-broken.json` | Same, with `safeguard.auth-behavior` removed |
| `order-status-syntax-ok.json` | DTO rename only; safeguards unchanged |

Paths are `SafeguardPaths` slugs of the canvas safeguard lines, not login keys.
Orch review must not be wired to the login fixtures.
