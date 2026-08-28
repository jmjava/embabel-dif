#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
if [[ $# -eq 0 || "${1:-}" == --* ]]; then
  set -- fold "$@"
fi
exec ./mvnw -q exec:java -Dexec.mainClass=com.embabel.dif.cli.DifCli -Dexec.args="$*"
