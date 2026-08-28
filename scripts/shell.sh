#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd "$(dirname "$0")" && pwd)
export AGENT_APPLICATION="${script_dir}/.."
cd "${AGENT_APPLICATION}"
exec ./mvnw -q spring-boot:run
