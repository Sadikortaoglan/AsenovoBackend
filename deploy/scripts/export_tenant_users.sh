#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGPASSWORD='<db_password>' ./deploy/scripts/export_tenant_users.sh \
    --db-host <host> \
    --db-port <port> \
    --db-name <db_name> \
    --db-user <db_user> \
    [--output <csv_path>]

Description:
  Exports platform users and tenant users into a single CSV with these columns:
  tenant,schema_db,username,role,active,enabled,locked,password_reset_required

Notes:
  - Plaintext passwords are NOT exported.
  - password_reset_required is exported as 'unknown' because this system does not
    persist that state explicitly.
USAGE
}

DB_HOST=""
DB_PORT="5432"
DB_NAME=""
DB_USER=""
OUTPUT_PATH=""

sql_literal() {
  printf "%s" "$1" | sed "s/'/''/g"
}

require_identifier() {
  local value="$1"
  local label="$2"
  if [[ ! "$value" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
    echo "ERROR: invalid ${label}: ${value}" >&2
    exit 1
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    --output) OUTPUT_PATH="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: PGPASSWORD is required in environment." >&2
  exit 1
fi

if [[ -z "$DB_HOST" || -z "$DB_NAME" || -z "$DB_USER" ]]; then
  echo "ERROR: --db-host, --db-name and --db-user are required." >&2
  usage
  exit 1
fi

mkdir -p tmp/admin-exports
if [[ -z "$OUTPUT_PATH" ]]; then
  OUTPUT_PATH="tmp/admin-exports/tenant-users-$(date +%Y%m%d-%H%M%S).csv"
fi

printf "tenant,schema_db,username,role,active,enabled,locked,password_reset_required\n" > "$OUTPUT_PATH"

export_platform_users() {
  local platform_exists
  platform_exists="$(psql -X -A -t -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -c "SELECT to_regclass('public.platform_users');" | tr -d '[:space:]')"
  if [[ "$platform_exists" == *"platform_users" ]]; then
    psql -X -q -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
      -c "\copy (
        SELECT
          '__platform__' AS tenant,
          'public.platform_users' AS schema_db,
          username,
          role,
          enabled AS active,
          enabled,
          locked,
          'unknown' AS password_reset_required
        FROM public.platform_users
        ORDER BY id
      ) TO STDOUT WITH CSV" >> "$OUTPUT_PATH"
  fi
}

export_shared_schema_users() {
  local tenant="$1"
  local schema_name="$2"
  local tenant_sql schema_sql
  tenant_sql="$(sql_literal "$tenant")"
  require_identifier "$schema_name" "schema_name"
  psql -X -q -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -c "\copy (
      SELECT
        '${tenant_sql}' AS tenant,
        '${schema_name}' AS schema_db,
        username,
        role,
        COALESCE(active, true) AS active,
        COALESCE(enabled, true) AS enabled,
        COALESCE(locked, false) AS locked,
        'unknown' AS password_reset_required
      FROM ${schema_name}.users
      ORDER BY id
    ) TO STDOUT WITH CSV" >> "$OUTPUT_PATH"
}

export_dedicated_db_users() {
  local tenant="$1"
  local database_name="$2"
  local tenant_sql db_sql
  tenant_sql="$(sql_literal "$tenant")"
  db_sql="$(sql_literal "$database_name")"
  psql -X -q -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database_name" \
    -c "\copy (
      SELECT
        '${tenant_sql}' AS tenant,
        '${db_sql}' AS schema_db,
        username,
        role,
        COALESCE(active, true) AS active,
        COALESCE(enabled, true) AS enabled,
        COALESCE(locked, false) AS locked,
        'unknown' AS password_reset_required
      FROM public.users
      ORDER BY id
    ) TO STDOUT WITH CSV" >> "$OUTPUT_PATH"
}

export_platform_users

while IFS='|' read -r tenant_subdomain tenancy_mode schema_name database_name; do
  [[ -z "$tenant_subdomain" ]] && continue
  if [[ "$tenancy_mode" == "SHARED_SCHEMA" && -n "$schema_name" ]]; then
    export_shared_schema_users "$tenant_subdomain" "$schema_name"
  elif [[ "$tenancy_mode" == "DEDICATED_DB" && -n "$database_name" ]]; then
    export_dedicated_db_users "$tenant_subdomain" "$database_name"
  fi
done < <(
  psql -X -A -F '|' -t -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -c "SELECT subdomain, tenancy_mode, COALESCE(schema_name, ''), COALESCE(db_name, '') FROM public.tenants ORDER BY id"
)

echo "Export written to: $OUTPUT_PATH"
