#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage:
  PGPASSWORD='<db_password>' ./deploy/scripts/provision_shared_tenant.sh \
    --db-host <host> \
    --db-port <port> \
    --db-name <db_name> \
    --db-user <db_user> \
    --tenant-subdomain <subdomain> \
    --tenant-name <tenant_name> \
    [--tenant-schema <schema>] \
    [--plan-code <code>] \
    [--plan-type <type>] \
    [--max-users <n>] \
    [--max-assets <n>] \
    [--rate-limit <n>] \
    [--max-storage-mb <n>] \
    [--priority-support <true|false>]

Example:
  PGPASSWORD='StrongPassword123' ./deploy/scripts/provision_shared_tenant.sh \
    --db-host 127.0.0.1 --db-port 5432 --db-name sara --db-user sara \
    --tenant-subdomain sara --tenant-name 'Sara Tenant'
USAGE
}

DB_HOST=""
DB_PORT="5432"
DB_NAME=""
DB_USER=""
TENANT_SUBDOMAIN=""
TENANT_NAME=""
TENANT_SCHEMA=""
PLAN_CODE="PRO-DEFAULT"
PLAN_TYPE="PRO"
MAX_USERS="25"
MAX_ASSETS="250"
RATE_LIMIT="1000"
MAX_STORAGE_MB="1024"
PRIORITY_SUPPORT="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    --tenant-subdomain) TENANT_SUBDOMAIN="$2"; shift 2 ;;
    --tenant-name) TENANT_NAME="$2"; shift 2 ;;
    --tenant-schema) TENANT_SCHEMA="$2"; shift 2 ;;
    --plan-code) PLAN_CODE="$2"; shift 2 ;;
    --plan-type) PLAN_TYPE="$2"; shift 2 ;;
    --max-users) MAX_USERS="$2"; shift 2 ;;
    --max-assets) MAX_ASSETS="$2"; shift 2 ;;
    --rate-limit) RATE_LIMIT="$2"; shift 2 ;;
    --max-storage-mb) MAX_STORAGE_MB="$2"; shift 2 ;;
    --priority-support) PRIORITY_SUPPORT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: PGPASSWORD is required in environment." >&2
  exit 1
fi

if [[ -z "$DB_HOST" || -z "$DB_NAME" || -z "$DB_USER" || -z "$TENANT_SUBDOMAIN" || -z "$TENANT_NAME" ]]; then
  echo "ERROR: Missing required args." >&2
  usage
  exit 1
fi

if [[ -z "$TENANT_SCHEMA" ]]; then
  TENANT_SCHEMA="tenant_${TENANT_SUBDOMAIN//-/_}"
fi

if ! [[ "$TENANT_SUBDOMAIN" =~ ^[a-z0-9-]+$ ]]; then
  echo "ERROR: --tenant-subdomain must match ^[a-z0-9-]+$" >&2
  exit 1
fi

if ! [[ "$TENANT_SCHEMA" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
  echo "ERROR: --tenant-schema must be a valid SQL identifier" >&2
  exit 1
fi

if ! [[ "$PRIORITY_SUPPORT" =~ ^(true|false)$ ]]; then
  echo "ERROR: --priority-support must be true or false" >&2
  exit 1
fi

if ! [[ "$MAX_USERS" =~ ^[0-9]+$ && "$MAX_ASSETS" =~ ^[0-9]+$ && "$RATE_LIMIT" =~ ^[0-9]+$ && "$MAX_STORAGE_MB" =~ ^[0-9]+$ ]]; then
  echo "ERROR: numeric options must be integers" >&2
  exit 1
fi

TENANT_NAME_SQL=$(printf "%s" "$TENANT_NAME" | sed "s/'/''/g")
PLAN_CODE_SQL=$(printf "%s" "$PLAN_CODE" | sed "s/'/''/g")
PLAN_TYPE_SQL=$(printf "%s" "$PLAN_TYPE" | sed "s/'/''/g")
TENANT_SUBDOMAIN_SQL=$(printf "%s" "$TENANT_SUBDOMAIN" | sed "s/'/''/g")
TENANT_SCHEMA_SQL=$(printf "%s" "$TENANT_SCHEMA" | sed "s/'/''/g")
REDIS_NAMESPACE_SQL=$(printf "%s" "tenant:${TENANT_SUBDOMAIN}" | sed "s/'/''/g")

SQL=$(cat <<SQL_EOF
DO \$\$
BEGIN
  IF to_regclass('public.plans') IS NULL THEN
    RAISE EXCEPTION 'plans table not found. Run migrations first.';
  END IF;
  IF to_regclass('public.tenants') IS NULL THEN
    RAISE EXCEPTION 'tenants table not found. Run migrations first.';
  END IF;
END
\$\$;

CREATE SCHEMA IF NOT EXISTS ${TENANT_SCHEMA};

INSERT INTO plans (
  code, plan_type, max_users, max_assets, api_rate_limit_per_minute, max_storage_mb, priority_support, active
)
VALUES (
  '${PLAN_CODE_SQL}', '${PLAN_TYPE_SQL}'::plan_type, ${MAX_USERS}, ${MAX_ASSETS}, ${RATE_LIMIT}, ${MAX_STORAGE_MB}, ${PRIORITY_SUPPORT}, true
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO tenants (
  name, subdomain, tenancy_mode, schema_name, redis_namespace, plan_id, active
)
VALUES (
  '${TENANT_NAME_SQL}', '${TENANT_SUBDOMAIN_SQL}', 'SHARED_SCHEMA', '${TENANT_SCHEMA_SQL}', '${REDIS_NAMESPACE_SQL}',
  (SELECT id FROM plans WHERE code='${PLAN_CODE_SQL}' LIMIT 1), true
)
ON CONFLICT (subdomain) DO UPDATE
SET
  name = EXCLUDED.name,
  tenancy_mode = EXCLUDED.tenancy_mode,
  schema_name = EXCLUDED.schema_name,
  redis_namespace = EXCLUDED.redis_namespace,
  plan_id = EXCLUDED.plan_id,
  active = EXCLUDED.active;

SELECT id, name, subdomain, tenancy_mode, schema_name, active
FROM tenants
WHERE subdomain='${TENANT_SUBDOMAIN_SQL}';
SQL_EOF
)

echo "Provisioning tenant '${TENANT_SUBDOMAIN}' on ${DB_HOST}:${DB_PORT}/${DB_NAME}..."
psql -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "$SQL"

echo "Done."
