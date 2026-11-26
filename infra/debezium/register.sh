#!/usr/bin/env bash
# Registers (or updates) the Debezium outbox connectors on Kafka Connect.
# Start stock-exchange first so the order_outbox / trade_outbox tables exist.
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://localhost:8084}"
DIR="$(cd "$(dirname "$0")" && pwd)"

for config in "$DIR"/*.json; do
  name="$(basename "$config" .json)"
  echo "Registering connector '$name' on $CONNECT_URL"
  curl --fail --silent --show-error -X PUT \
    -H 'Content-Type: application/json' \
    --data @"$config" \
    "$CONNECT_URL/connectors/$name/config"
  echo
done
