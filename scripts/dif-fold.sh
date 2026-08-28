#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
exec ./mvnw -q exec:java -Dexec.mainClass=com.embabel.dif.cli.DifCli -Dexec.args="fold $*"
