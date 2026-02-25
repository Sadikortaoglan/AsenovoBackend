#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$ROOT_DIR/.env.local"
LOG_FILE="$ROOT_DIR/.local-backend.log"
PID_FILE="$ROOT_DIR/.local-backend.pid"
WATCH_MODE="${1:-}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd mvn
require_cmd curl
require_cmd awk
require_cmd grep

if [[ ! -f "$ENV_FILE" ]]; then
  cat >&2 <<'EOF'
Missing .env.local.
Create /backend/.env.local with local values first.
Required keys:
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
QR_SECRET_KEY
QR_BASE_URL
CORS_ALLOWED_ORIGINS
EOF
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_vars=(
  SPRING_PROFILES_ACTIVE
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  JWT_SECRET
  QR_SECRET_KEY
  QR_BASE_URL
  CORS_ALLOWED_ORIGINS
)

for key in "${required_vars[@]}"; do
  if [[ -z "${!key:-}" ]]; then
    echo "Required env var is empty: $key" >&2
    exit 1
  fi
done

echo "==> Starting local Docker PostgreSQL"
if [[ -f "$ROOT_DIR/docker-compose.local.yml" ]]; then
  (cd "$ROOT_DIR" && docker compose -f docker-compose.local.yml up -d postgres)
elif [[ -f "$ROOT_DIR/docker-compose.yml" ]]; then
  (cd "$ROOT_DIR" && docker compose up -d postgres)
else
  echo "No docker-compose file found in $ROOT_DIR" >&2
  exit 1
fi

echo "==> Building backend"
(cd "$ROOT_DIR" && mvn clean package -DskipTests)

stop_backend() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [[ -n "${pid}" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
      sleep 1
    fi
  fi
  pkill -f "sara-asansor-api-1.0.0.jar" >/dev/null 2>&1 || true
}

start_backend() {
  echo "==> Starting backend"
  stop_backend
  nohup env $(grep -v '^[[:space:]]*$' "$ENV_FILE" | grep -v '^[[:space:]]*#' | xargs) \
    java -jar "$ROOT_DIR/target/sara-asansor-api-1.0.0.jar" > "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
}

wait_health() {
  local attempts=40
  local url="http://localhost:8080/api/health"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

verify_endpoints() {
  local health_url="http://localhost:8080/api/health"
  local qr_url="http://localhost:8080/api/elevators/1/qr"

  echo "==> Verifying endpoints"
  curl -fsS "$health_url" >/dev/null

  local qr_anon_code
  qr_anon_code="$(curl -s -o /dev/null -w "%{http_code}" "$qr_url")"
  if [[ "$qr_anon_code" != "401" && "$qr_anon_code" != "403" ]]; then
    echo "Unexpected anonymous QR response code: $qr_anon_code" >&2
    exit 1
  fi

  local login_json token qr_auth_code
  login_json="$(curl -sS -X POST "http://localhost:8080/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"patron","password":"password"}')"
  token="$(echo "$login_json" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
  if [[ -z "$token" ]]; then
    echo "Could not get access token for QR verification." >&2
    echo "Login response: $login_json" >&2
    exit 1
  fi

  qr_auth_code="$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $token" "$qr_url")"
  if [[ "$qr_auth_code" != "200" ]]; then
    echo "Authenticated QR check failed with HTTP $qr_auth_code" >&2
    exit 1
  fi
}

start_backend

if ! wait_health; then
  echo "Backend failed health check. Last logs:"
  tail -n 120 "$LOG_FILE" || true
  exit 1
fi

echo "==> Local backend is UP"
curl -sS http://localhost:8080/api/health
echo
verify_endpoints
echo "==> Endpoint verification passed (health + QR)"

if [[ "$WATCH_MODE" == "--watch" ]]; then
  echo "==> Watch mode active. Checking every 15s (Ctrl+C to stop)"
  while true; do
    if ! curl -fsS http://localhost:8080/api/health >/dev/null 2>&1; then
      echo "Health check failed, restarting backend..."
      tail -n 80 "$LOG_FILE" || true
      start_backend
      if ! wait_health; then
        echo "Restart failed; leaving logs below:"
        tail -n 120 "$LOG_FILE" || true
        exit 1
      fi
      echo "Backend recovered."
    fi
    sleep 15
  done
fi
