#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-$HOME/sara-backend}"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE="${ENV_FILE:-.env.prod}"
POST_RESET_HEALTH_URL="${2:-http://127.0.0.1:8080/api/health}"

if [[ ! -d "$PROJECT_DIR" ]]; then
  echo "ERROR: project dir not found: $PROJECT_DIR" >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: env file not found: $PROJECT_DIR/$ENV_FILE" >&2
  exit 1
fi

echo "WARNING: This will DELETE the production PostgreSQL data volume and recreate the database from scratch."
echo "PROJECT_DIR=$PROJECT_DIR"
echo "ENV_FILE=$ENV_FILE"
read -r -p "Type RESET-ASENOVO to continue: " CONFIRMATION

if [[ "$CONFIRMATION" != "RESET-ASENOVO" ]]; then
  echo "Aborted."
  exit 1
fi

echo "[1/6] Stop stack"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down --remove-orphans

echo "[2/6] Remove postgres volume"
docker volume rm "${PWD##*/}_postgres_data" 2>/dev/null || true

echo "[3/6] Start fresh stack"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build

echo "[4/6] Wait for health"
ATTEMPT=1
MAX_ATTEMPTS=30
until curl -fsS "$POST_RESET_HEALTH_URL" >/dev/null 2>&1; do
  if [[ "$ATTEMPT" -ge "$MAX_ATTEMPTS" ]]; then
    echo "Health check failed after reset. Recent logs:" >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=250 app
    exit 1
  fi
  echo "Health not ready yet (attempt $ATTEMPT/$MAX_ATTEMPTS). Waiting 5s..."
  sleep 5
  ATTEMPT=$((ATTEMPT + 1))
done

echo "[5/6] Show recent app logs"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=120 app

echo "[6/6] Reset completed"
echo "Health: $POST_RESET_HEALTH_URL"
