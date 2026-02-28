#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage:
  export PGPASSWORD='<DB_PASSWORD>'
  ./deploy/scripts/provision_sara_and_test_tenants.sh --db-host <DB_HOST> [--db-port 5432] [--db-name sara] [--db-user sara]

Example:
  export PGPASSWORD='StrongPassword123'
  ./deploy/scripts/provision_sara_and_test_tenants.sh --db-host 127.0.0.1 --db-port 5432 --db-name sara --db-user sara
USAGE
}

DB_HOST=""
DB_PORT="5432"
DB_NAME="sara"
DB_USER="sara"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: PGPASSWORD is required. Example: export PGPASSWORD='DB_PASSWORD'" >&2
  exit 1
fi

if [[ -z "$DB_HOST" ]]; then
  echo "ERROR: --db-host is required" >&2
  usage
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROVISION_SCRIPT="$SCRIPT_DIR/provision_shared_tenant.sh"

if [[ ! -x "$PROVISION_SCRIPT" ]]; then
  echo "ERROR: provision_shared_tenant.sh not found or not executable: $PROVISION_SCRIPT" >&2
  exit 1
fi

echo "[1/2] Provisioning 'sara' tenant..."
"$PROVISION_SCRIPT" \
  --db-host "$DB_HOST" \
  --db-port "$DB_PORT" \
  --db-name "$DB_NAME" \
  --db-user "$DB_USER" \
  --tenant-subdomain sara \
  --tenant-name 'Sara Tenant' \
  --tenant-schema tenant_sara

echo "[2/2] Provisioning 'test' tenant..."
"$PROVISION_SCRIPT" \
  --db-host "$DB_HOST" \
  --db-port "$DB_PORT" \
  --db-name "$DB_NAME" \
  --db-user "$DB_USER" \
  --tenant-subdomain test \
  --tenant-name 'Test Tenant' \
  --tenant-schema tenant_test

echo "Done: sara and test tenants provisioned."
