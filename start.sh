#!/usr/bin/env bash
# start.sh — Load .env and run the app (Linux/macOS)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: .env file not found. Copy .env.example to .env and fill in values." >&2
  exit 1
fi

set -a
# shellcheck source=.env
source "$ENV_FILE"
set +a

mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
