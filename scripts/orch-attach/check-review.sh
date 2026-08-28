#!/usr/bin/env bash
# Silent semantic review for existing orch review commands.
#
# Exit 0: passed, skipped (no CLI), or skipped (no snapshots).
# Exit 1: required safeguard not preserved.
# Exit 2: usage error.
#
# Snapshots are optional. Missing files are skip — do not invent a ritual.
# Do not wire login fixtures. Do not start Embabel.
set -euo pipefail

canvas=""
before=""
after=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --canvas)
      canvas="${2:-}"
      shift 2
      ;;
    --before)
      before="${2:-}"
      shift 2
      ;;
    --after)
      after="${2:-}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: check-review.sh --before <file> --after <file> [--canvas <file>]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "${before}" || -z "${after}" ]]; then
  echo "check-review: --before and --after are required (or omit the hook)" >&2
  exit 2
fi

if [[ ! -f "${before}" || ! -f "${after}" ]]; then
  echo "dif=skipped"
  exit 0
fi

attach_dir=$(cd "$(dirname "$0")" && pwd)
cli=""
if cli="$("${attach_dir}/resolve-dif-cli.sh")"; then
  :
else
  echo "dif=skipped"
  exit 0
fi

args=(review --quiet --before "${before}" --after "${after}")
if [[ -n "${canvas}" && -f "${canvas}" ]]; then
  args+=(--canvas "${canvas}")
fi

set +e
"${cli}" "${args[@]}"
code=$?
set -e
exit "${code}"
