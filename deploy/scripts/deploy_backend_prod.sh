#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-/opt/asenovo/backend}"
COMPOSE_FILE="docker-compose.prod.yml"
HEALTH_URL="${2:-http://127.0.0.1:8080/api/health}"
ENV_FILE="${ENV_FILE:-.env.prod}"
HEALTH_RETRIES="${HEALTH_RETRIES:-24}"
HEALTH_SLEEP_SECONDS="${HEALTH_SLEEP_SECONDS:-5}"
VERIFY_CONTROL_PLANE_TABLES="${VERIFY_CONTROL_PLANE_TABLES:-auto}"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif docker-compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "ERROR: neither 'docker compose' nor 'docker-compose' is available" >&2
  exit 1
fi

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
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down --remove-orphans

echo "[4/8] Build"
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" build

echo "[5/8] Start"
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d

echo "[6/8] Wait for app health: $HEALTH_URL"
ATTEMPT=1
until curl -fsS "$HEALTH_URL" >/dev/null 2>&1; do
  if [[ "$ATTEMPT" -ge "$HEALTH_RETRIES" ]]; then
    echo "Health check failed after $HEALTH_RETRIES attempts. Showing app logs..." >&2
    "${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app
    exit 1
  fi
  echo "Health not ready yet (attempt $ATTEMPT/$HEALTH_RETRIES). Waiting ${HEALTH_SLEEP_SECONDS}s..."
  sleep "$HEALTH_SLEEP_SECONDS"
  ATTEMPT=$((ATTEMPT + 1))
done

echo "[7/8] Verify Flyway/migration startup"
if "${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app | grep -Eqi "migration failed|flyway exception|validate failed|error creating bean|flywaymigrateexception|sql state|beancreationexception"; then
  echo "Detected migration/startup errors in app logs. Showing recent logs..." >&2
  "${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app
  exit 1
fi

echo "[8/8] Deployment healthy"
curl -fsS "$HEALTH_URL"

if [[ "$VERIFY_CONTROL_PLANE_TABLES" == "true" ]] || [[ "$VERIFY_CONTROL_PLANE_TABLES" == "auto" && -f "src/main/resources/db/migration/V51__backfill_tenant_provisioning_control_plane.sql" ]]; then
  echo "[9/9] Verify tenant provisioning control-plane tables"
  JOB_TABLE_STATUS="$(docker exec asenovo-postgres-prod psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -tAc "SELECT to_regclass('public.tenant_provisioning_jobs');" | tr -d '[:space:]')"
  AUDIT_TABLE_STATUS="$(docker exec asenovo-postgres-prod psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -tAc "SELECT to_regclass('public.tenant_provisioning_audit_logs');" | tr -d '[:space:]')"
  if [[ "$JOB_TABLE_STATUS" != *"tenant_provisioning_jobs" ]] || [[ "$AUDIT_TABLE_STATUS" != *"tenant_provisioning_audit_logs" ]]; then
    echo "Tenant provisioning control-plane tables are missing after deploy." >&2
    echo "tenant_provisioning_jobs=$JOB_TABLE_STATUS" >&2
    echo "tenant_provisioning_audit_logs=$AUDIT_TABLE_STATUS" >&2
    "${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=200 app
    exit 1
  fi
fi

echo "Deploy completed successfully."
