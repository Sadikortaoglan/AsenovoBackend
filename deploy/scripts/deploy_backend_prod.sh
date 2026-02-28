#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-$HOME/sara-backend}"
COMPOSE_FILE="docker-compose.prod.yml"
HEALTH_URL="${2:-http://127.0.0.1:8080/api/health}"

if [[ ! -d "$PROJECT_DIR" ]]; then
  echo "ERROR: project dir not found: $PROJECT_DIR" >&2
  exit 1
fi

cd "$PROJECT_DIR"

echo "[1/6] Pull latest code"
git pull

echo "[2/6] Stop old stack"
docker compose -f "$COMPOSE_FILE" down

echo "[3/6] Build"
docker compose -f "$COMPOSE_FILE" build

echo "[4/6] Start"
docker compose -f "$COMPOSE_FILE" up -d

echo "[5/6] Wait app"
sleep 5

echo "[6/6] Health check: $HEALTH_URL"
set +e
curl -fsS "$HEALTH_URL"
CURL_EXIT=$?
set -e

if [[ $CURL_EXIT -ne 0 ]]; then
  echo "Health check failed. Showing app logs..."
  docker compose -f "$COMPOSE_FILE" logs --tail=120 app
  exit 1
fi

echo "Deploy completed successfully."
