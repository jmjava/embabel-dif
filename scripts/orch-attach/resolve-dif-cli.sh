#!/usr/bin/env bash
# Print the path to dif-fold.sh if a DIF checkout is available.
# Exit 1 when DIF is disabled or not found — callers treat that as skip.
#
# This is not a user-facing "next". Existing orch commands call it.
set -euo pipefail

if [[ "${DIF_DISABLED:-}" == "1" || "${DIF_CLI:-}" == "off" ]]; then
  exit 1
fi

if [[ -n "${DIF_CLI:-}" && -x "${DIF_CLI}" ]]; then
  printf '%s\n' "${DIF_CLI}"
  exit 0
fi

candidates=()
if [[ -n "${DIF_HOME:-}" ]]; then
  candidates+=("${DIF_HOME}/scripts/dif-fold.sh")
fi
here=$(cd "$(dirname "$0")/../.." && pwd)
candidates+=("${here}/scripts/dif-fold.sh")
candidates+=("${PWD}/../embabel-dif/scripts/dif-fold.sh")
candidates+=("${HOME}/github/jmjava/embabel-dif/scripts/dif-fold.sh")

for path in "${candidates[@]}"; do
  if [[ -x "${path}" ]]; then
    printf '%s\n' "$(cd "$(dirname "${path}")" && pwd)/$(basename "${path}")"
    exit 0
  fi
done
exit 1
