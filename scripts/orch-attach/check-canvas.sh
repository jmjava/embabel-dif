#!/usr/bin/env bash
# Silent semantic gate for existing orch architect / code / next commands.
#
# Exit 0: ready, or DIF not installed (print dif=skipped).
# Exit 1: not Ready For Coding (blocking conflicts).
# Exit 2: usage error.
#
# Not a daily-driver "next". Do not start Embabel. Do not replace sdlc.sh next.
set -euo pipefail

canvas=""
out=".dif/projections"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --canvas)
      canvas="${2:-}"
      shift 2
      ;;
    --out)
      out="${2:-}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: check-canvas.sh --canvas <file> [--out <dir>]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "${canvas}" || ! -f "${canvas}" ]]; then
  echo "check-canvas: --canvas <existing file> is required" >&2
  exit 2
fi

attach_dir=$(cd "$(dirname "$0")" && pwd)
cli=""
if cli="$("${attach_dir}/resolve-dif-cli.sh")"; then
  :
else
  echo "dif=skipped"
  exit 0
fi

set +e
"${cli}" architect --canvas "${canvas}" --out "${out}"
code=$?
set -e
exit "${code}"
