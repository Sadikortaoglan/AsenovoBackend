#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-$HOME/sara-backend}"
COMPOSE_FILE="docker-compose.prod.yml"
HEALTH_URL="${2:-http://127.0.0.1:8080/api/health}"
ENV_FILE="${ENV_FILE:-.env.prod}"
HEALTH_RETRIES="${HEALTH_RETRIES:-24}"
HEALTH_SLEEP_SECONDS="${HEALTH_SLEEP_SECONDS:-5}"

if [[ ! -d "$PROJECT_DIR" ]]; then
  echo "ERROR: project dir not found: $PROJECT_DIR" >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: env file not found: $PROJECT_DIR/$ENV_FILE" >&2
  echo "Create it first from deploy/env/backend.prod.env.example" >&2
  exit 1
fi

echo "[1/8] Pull latest code"
git pull

echo "[2/8] Show revision"
git rev-parse --short HEAD

echo "[3/8] Stop old stack"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down --remove-orphans

echo "[4/8] Build"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" build

echo "[5/8] Start"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d

echo "[6/8] Wait for app health: $HEALTH_URL"
ATTEMPT=1
until curl -fsS "$HEALTH_URL" >/dev/null 2>&1; do
  if [[ "$ATTEMPT" -ge "$HEALTH_RETRIES" ]]; then
    echo "Health check failed after $HEALTH_RETRIES attempts. Showing app logs..." >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app
    exit 1
  fi
  echo "Health not ready yet (attempt $ATTEMPT/$HEALTH_RETRIES). Waiting ${HEALTH_SLEEP_SECONDS}s..."
  sleep "$HEALTH_SLEEP_SECONDS"
  ATTEMPT=$((ATTEMPT + 1))
done

echo "[7/8] Verify Flyway/migration startup"
if docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app | rg -i "migration failed|flyway exception|validate failed|error creating bean" >/dev/null; then
  echo "Detected migration/startup errors in app logs. Showing recent logs..." >&2
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app
  exit 1
fi

echo "[8/8] Deployment healthy"
curl -fsS "$HEALTH_URL"

echo "Deploy completed successfully."
