#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

HOST="127.0.0.1"
MOCK_HOST_MSHIP_LED="$HOST"
MOCK_PORT_MSHIP_LED="8001"

python $SCRIPT_DIR/simple-osc-server.py \
  --ip="${MOCK_HOST_MSHIP_LED}" \
  --port="${MOCK_PORT_MSHIP_LED}"