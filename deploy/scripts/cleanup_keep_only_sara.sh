#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGPASSWORD='<db_password>' ./deploy/scripts/cleanup_keep_only_sara.sh \
    --db-host <host> \
    --db-port <port> \
    --db-name <db_name> \
    --db-user <db_user> \
    [--keep-tenant <subdomain>] \
    [--apply] \
    [--platform-username <username>] \
    [--platform-password <password>] \
    [--tenant-admin-username <username>] \
    [--tenant-admin-password <password>] \
    [--staff-username <username>] \
    [--staff-password <password>] \
    [--cari-username <username>] \
    [--cari-password <password>] \
    [--output <csv_path>]

Description:
  Keeps only the selected tenant data plus public control-plane data.
  Default kept tenant is `sara`.
  Deletes all other tenant rows and drops their shared schemas / dedicated databases.
  Preserves imported public revision standard data:
    - public.revision_standards
    - public.revision_standard_sets
  Recreates/resets the canonical kept users with known passwords:
    - public PLATFORM_ADMIN
    - tenant_sara TENANT_ADMIN
    - tenant_sara STAFF_USER
    - tenant_sara CARI_USER

Safety:
  - Dry-run by default. Nothing is deleted unless --apply is provided.
  - Physical uploaded files are NOT pruned from disk by this script.
  - If the kept tenant maps to `public` (for example `default`), public schema data and
    users are preserved as-is; only the other tenant rows/schemas/databases are deleted.
USAGE
}

DB_HOST=""
DB_PORT="5432"
DB_NAME=""
DB_USER=""
APPLY="false"
OUTPUT_PATH=""
KEEP_TENANT="sara"

PLATFORM_USERNAME="platformadmin"
PLATFORM_PASSWORD="Platform123!"
TENANT_ADMIN_USERNAME="sara_tenant_admin"
TENANT_ADMIN_PASSWORD="Tenant123!"
STAFF_USERNAME="sara_staff_user"
STAFF_PASSWORD="Staff123!"
CARI_USERNAME="sara_cari_user"
CARI_PASSWORD="Cari123!"

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

psql_base() {
  psql -q -X -v ON_ERROR_STOP=1 -A -t -F '|' -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" "$@"
}

psql_shared() {
  psql -q -X -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" "$@"
}

psql_db() {
  local target_db="$1"
  shift
  psql -q -X -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$target_db" "$@"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    --keep-tenant) KEEP_TENANT="$2"; shift 2 ;;
    --apply) APPLY="true"; shift ;;
    --platform-username) PLATFORM_USERNAME="$2"; shift 2 ;;
    --platform-password) PLATFORM_PASSWORD="$2"; shift 2 ;;
    --tenant-admin-username) TENANT_ADMIN_USERNAME="$2"; shift 2 ;;
    --tenant-admin-password) TENANT_ADMIN_PASSWORD="$2"; shift 2 ;;
    --staff-username) STAFF_USERNAME="$2"; shift 2 ;;
    --staff-password) STAFF_PASSWORD="$2"; shift 2 ;;
    --cari-username) CARI_USERNAME="$2"; shift 2 ;;
    --cari-password) CARI_PASSWORD="$2"; shift 2 ;;
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
  OUTPUT_PATH="tmp/admin-exports/keep-only-sara-credentials-$(date +%Y%m%d-%H%M%S).csv"
fi

KEEP_TENANT_SQL="$(sql_literal "$KEEP_TENANT")"
SARA_ROW="$(psql_base -c "SELECT id, tenancy_mode, COALESCE(schema_name, ''), COALESCE(db_name, ''), COALESCE(status::text, '') FROM public.tenants WHERE subdomain = '${KEEP_TENANT_SQL}' LIMIT 1")"
if [[ -z "$SARA_ROW" ]]; then
  echo "ERROR: keep tenant not found: ${KEEP_TENANT}" >&2
  echo "Available tenants:" >&2
  psql_shared -c "SELECT id, subdomain, tenancy_mode, COALESCE(schema_name, '') AS schema_name, COALESCE(db_name, '') AS db_name, COALESCE(status::text, '') AS status FROM public.tenants ORDER BY id;" >&2
  exit 1
fi

IFS='|' read -r SARA_TENANT_ID SARA_TENANCY_MODE SARA_SCHEMA_NAME SARA_DB_NAME SARA_STATUS <<< "$SARA_ROW"
if [[ "$SARA_TENANCY_MODE" != "SHARED_SCHEMA" ]]; then
  echo "ERROR: expected sara tenant to be SHARED_SCHEMA, got $SARA_TENANCY_MODE" >&2
  exit 1
fi
require_identifier "$SARA_SCHEMA_NAME" "sara schema_name"
KEEP_TENANT_IS_PUBLIC="false"
if [[ "$SARA_SCHEMA_NAME" == "public" ]]; then
  KEEP_TENANT_IS_PUBLIC="true"
fi

TENANTS_TO_DELETE_RAW="$(psql_base -c "SELECT id, subdomain, tenancy_mode, COALESCE(schema_name, ''), COALESCE(db_name, '') FROM public.tenants WHERE subdomain <> '${KEEP_TENANT_SQL}' ORDER BY id")"
PUBLIC_KEEP_TABLES="'flyway_schema_history','plans','tenants','subscriptions','features','tenant_feature_overrides','tenant_provisioning_jobs','tenant_provisioning_audit_logs','platform_users','revision_standards','revision_standard_sets','users'"
PUBLIC_TRUNCATE_TABLES="$(psql_base -c "SELECT COALESCE(string_agg(format('%I.%I', schemaname, tablename), ', ' ORDER BY tablename), '') FROM pg_tables WHERE schemaname = 'public' AND tablename NOT IN (${PUBLIC_KEEP_TABLES})")"
PUBLIC_USER_COUNT="$(psql_base -c "SELECT COUNT(*) FROM public.users")"
SARA_USER_COUNT="$(psql_base -c "SELECT COUNT(*) FROM ${SARA_SCHEMA_NAME}.users")"
SARA_B2B_COUNT="$(psql_base -c "SELECT COUNT(*) FROM ${SARA_SCHEMA_NAME}.b2b_units WHERE active = true")"

echo "Mode: $([[ "$APPLY" == "true" ]] && echo APPLY || echo DRY-RUN)"
echo "Keep tenant: ${KEEP_TENANT} (${SARA_SCHEMA_NAME})"
echo "Public users currently: ${PUBLIC_USER_COUNT}"
echo "Sara users currently: ${SARA_USER_COUNT}"
echo "Sara active B2B units: ${SARA_B2B_COUNT}"
echo
echo "Tenants to delete:"
if [[ -n "$TENANTS_TO_DELETE_RAW" ]]; then
  while IFS='|' read -r tenant_id subdomain tenancy_mode schema_name db_name; do
    [[ -z "$tenant_id" ]] && continue
    echo "  - id=${tenant_id} subdomain=${subdomain} mode=${tenancy_mode} schema=${schema_name:-<none>} db=${db_name:-<none>}"
  done <<< "$TENANTS_TO_DELETE_RAW"
else
  echo "  - none"
fi
echo
echo "Public business tables to purge (control-plane + imported revision standards preserved):"
if [[ "$KEEP_TENANT_IS_PUBLIC" == "true" ]]; then
  echo "  - none (kept tenant uses public schema; preserving public data as-is)"
elif [[ -n "$PUBLIC_TRUNCATE_TABLES" ]]; then
  printf '  - %s\n' "${PUBLIC_TRUNCATE_TABLES//, /$'\n  - '}"
else
  echo "  - none"
fi
echo
echo "Target kept credentials:"
if [[ "$KEEP_TENANT_IS_PUBLIC" == "true" ]]; then
  echo "  - existing public/default users preserved as-is"
else
  echo "  - public PLATFORM_ADMIN: ${PLATFORM_USERNAME}"
  echo "  - ${KEEP_TENANT} TENANT_ADMIN: ${TENANT_ADMIN_USERNAME}"
  echo "  - ${KEEP_TENANT} STAFF_USER: ${STAFF_USERNAME}"
  echo "  - ${KEEP_TENANT} CARI_USER: ${CARI_USERNAME}"
fi
echo "Sensitive credential output will be written to: ${OUTPUT_PATH}"

if [[ "$APPLY" != "true" ]]; then
  echo
  echo "Dry-run only. Re-run with --apply to execute destructive cleanup."
  exit 0
fi

psql_shared -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null

platform_username_sql="$(sql_literal "$PLATFORM_USERNAME")"
platform_password_sql="$(sql_literal "$PLATFORM_PASSWORD")"
tenant_admin_username_sql="$(sql_literal "$TENANT_ADMIN_USERNAME")"
tenant_admin_password_sql="$(sql_literal "$TENANT_ADMIN_PASSWORD")"
staff_username_sql="$(sql_literal "$STAFF_USERNAME")"
staff_password_sql="$(sql_literal "$STAFF_PASSWORD")"
cari_username_sql="$(sql_literal "$CARI_USERNAME")"
cari_password_sql="$(sql_literal "$CARI_PASSWORD")"

if [[ "$KEEP_TENANT_IS_PUBLIC" != "true" ]]; then
  if [[ -n "$PUBLIC_TRUNCATE_TABLES" ]]; then
    psql_shared -c "TRUNCATE TABLE ${PUBLIC_TRUNCATE_TABLES}, public.users RESTART IDENTITY CASCADE;"
  fi

  PLATFORM_USER_ID="$(psql_base -c "
    INSERT INTO public.users (username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at)
    VALUES ('${platform_username_sql}', crypt('${platform_password_sql}', gen_salt('bf', 10)), 'SYSTEM_ADMIN', 'SYSTEM_ADMIN', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (username) DO UPDATE
    SET password_hash = crypt('${platform_password_sql}', gen_salt('bf', 10)),
        role = 'SYSTEM_ADMIN',
        user_type = 'SYSTEM_ADMIN',
        active = true,
        enabled = true,
        locked = false,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id
  ")"
  PLATFORM_USER_ID="${PLATFORM_USER_ID//[[:space:]]/}"

  PLATFORM_USERS_EXISTS="$(psql_base -c "SELECT to_regclass('public.platform_users')")"
  if [[ "$PLATFORM_USERS_EXISTS" == "public.platform_users" ]]; then
    psql_shared <<SQL
INSERT INTO public.platform_users (username, password_hash, enabled, locked, role, created_at, updated_at, last_login_at)
SELECT username, password_hash, COALESCE(enabled, true), COALESCE(locked, false), 'ROLE_PLATFORM_ADMIN',
       COALESCE(created_at, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, last_login_at
FROM public.users
WHERE id = ${PLATFORM_USER_ID}
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    locked = EXCLUDED.locked,
    role = EXCLUDED.role,
    updated_at = CURRENT_TIMESTAMP,
    last_login_at = EXCLUDED.last_login_at;

DELETE FROM public.platform_users WHERE username <> '${platform_username_sql}';
SQL
  fi

  psql_shared <<SQL
DELETE FROM public.refresh_tokens WHERE user_id <> ${PLATFORM_USER_ID};
DELETE FROM public.users WHERE id <> ${PLATFORM_USER_ID};
SQL

  SARA_B2B_UNIT_ID="$(psql_base -c "SELECT id FROM ${SARA_SCHEMA_NAME}.b2b_units WHERE active = true ORDER BY id LIMIT 1")"
  SARA_B2B_UNIT_ID="${SARA_B2B_UNIT_ID//[[:space:]]/}"
  if [[ -z "$SARA_B2B_UNIT_ID" ]]; then
    SARA_B2B_UNIT_ID="$(psql_base -c "
      INSERT INTO ${SARA_SCHEMA_NAME}.b2b_units (name, currency, risk_limit, active, created_at, updated_at)
      VALUES ('SARA Cari Hesabi', 'TRY', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      RETURNING id
    ")"
    SARA_B2B_UNIT_ID="${SARA_B2B_UNIT_ID//[[:space:]]/}"
  fi

  psql_shared <<SQL
UPDATE ${SARA_SCHEMA_NAME}.users
SET b2b_unit_id = NULL
WHERE COALESCE(b2b_unit_id, 0) = ${SARA_B2B_UNIT_ID}
  AND username <> '${cari_username_sql}';

INSERT INTO ${SARA_SCHEMA_NAME}.users (username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at)
VALUES ('${tenant_admin_username_sql}', crypt('${tenant_admin_password_sql}', gen_salt('bf', 10)), 'STAFF_ADMIN', 'STAFF', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO UPDATE
SET password_hash = crypt('${tenant_admin_password_sql}', gen_salt('bf', 10)),
    role = 'STAFF_ADMIN',
    user_type = 'STAFF',
    active = true,
    enabled = true,
    locked = false,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO ${SARA_SCHEMA_NAME}.users (username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at)
VALUES ('${staff_username_sql}', crypt('${staff_password_sql}', gen_salt('bf', 10)), 'STAFF_USER', 'STAFF', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO UPDATE
SET password_hash = crypt('${staff_password_sql}', gen_salt('bf', 10)),
    role = 'STAFF_USER',
    user_type = 'STAFF',
    active = true,
    enabled = true,
    locked = false,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO ${SARA_SCHEMA_NAME}.users (username, password_hash, role, user_type, active, enabled, locked, b2b_unit_id, created_at, updated_at)
VALUES ('${cari_username_sql}', crypt('${cari_password_sql}', gen_salt('bf', 10)), 'CARI_USER', 'CARI', true, true, false, ${SARA_B2B_UNIT_ID}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO UPDATE
SET password_hash = crypt('${cari_password_sql}', gen_salt('bf', 10)),
    role = 'CARI_USER',
    user_type = 'CARI',
    active = true,
    enabled = true,
    locked = false,
    b2b_unit_id = ${SARA_B2B_UNIT_ID},
    updated_at = CURRENT_TIMESTAMP;

UPDATE ${SARA_SCHEMA_NAME}.b2b_units
SET portal_username = '${cari_username_sql}',
    portal_password_hash = crypt('${cari_password_sql}', gen_salt('bf', 10)),
    active = true,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ${SARA_B2B_UNIT_ID};
SQL

  SARA_TENANT_ADMIN_ID="$(psql_base -c "SELECT id FROM ${SARA_SCHEMA_NAME}.users WHERE username = '${tenant_admin_username_sql}' LIMIT 1")"
  SARA_STAFF_ID="$(psql_base -c "SELECT id FROM ${SARA_SCHEMA_NAME}.users WHERE username = '${staff_username_sql}' LIMIT 1")"
  SARA_CARI_ID="$(psql_base -c "SELECT id FROM ${SARA_SCHEMA_NAME}.users WHERE username = '${cari_username_sql}' LIMIT 1")"
  SARA_TENANT_ADMIN_ID="${SARA_TENANT_ADMIN_ID//[[:space:]]/}"
  SARA_STAFF_ID="${SARA_STAFF_ID//[[:space:]]/}"
  SARA_CARI_ID="${SARA_CARI_ID//[[:space:]]/}"

  psql_shared <<SQL
DELETE FROM ${SARA_SCHEMA_NAME}.refresh_tokens
WHERE user_id IN (
  SELECT id FROM ${SARA_SCHEMA_NAME}.users
  WHERE username NOT IN ('${tenant_admin_username_sql}', '${staff_username_sql}', '${cari_username_sql}')
);

UPDATE ${SARA_SCHEMA_NAME}.maintenances
SET technician_user_id = ${SARA_STAFF_ID}
WHERE technician_user_id IS NOT NULL
  AND technician_user_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.maintenance_plans
SET assigned_technician_id = CASE
        WHEN assigned_technician_id IS NOT NULL
         AND assigned_technician_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID})
        THEN ${SARA_STAFF_ID}
        ELSE assigned_technician_id
    END,
    updated_by = CASE
        WHEN updated_by IS NOT NULL
         AND updated_by NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID})
        THEN ${SARA_TENANT_ADMIN_ID}
        ELSE updated_by
    END,
    cancelled_by = CASE
        WHEN cancelled_by IS NOT NULL
         AND cancelled_by NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID})
        THEN ${SARA_TENANT_ADMIN_ID}
        ELSE cancelled_by
    END,
    started_by_user_id = CASE
        WHEN started_by_user_id IS NOT NULL
         AND started_by_user_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID})
        THEN ${SARA_TENANT_ADMIN_ID}
        ELSE started_by_user_id
    END;

UPDATE ${SARA_SCHEMA_NAME}.qr_proofs
SET used_by = NULL
WHERE used_by IS NOT NULL
  AND used_by NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.maintenance_sessions
SET technician_id = ${SARA_STAFF_ID}
WHERE technician_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.qr_scan_logs
SET technician_id = ${SARA_STAFF_ID}
WHERE technician_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.maintenance_plan_photos
SET uploaded_by = ${SARA_TENANT_ADMIN_ID}
WHERE uploaded_by IS NOT NULL
  AND uploaded_by NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.file_attachments
SET uploaded_by_user_id = ${SARA_TENANT_ADMIN_ID}
WHERE uploaded_by_user_id IS NOT NULL
  AND uploaded_by_user_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

UPDATE ${SARA_SCHEMA_NAME}.audit_logs
SET user_id = NULL
WHERE user_id IS NOT NULL
  AND user_id NOT IN (${SARA_TENANT_ADMIN_ID}, ${SARA_STAFF_ID}, ${SARA_CARI_ID});

DELETE FROM ${SARA_SCHEMA_NAME}.users
WHERE username NOT IN ('${tenant_admin_username_sql}', '${staff_username_sql}', '${cari_username_sql}');
SQL
fi

if [[ -n "$TENANTS_TO_DELETE_RAW" ]]; then
  while IFS='|' read -r tenant_id subdomain tenancy_mode schema_name db_name; do
    [[ -z "$tenant_id" ]] && continue

    if [[ "$tenancy_mode" == "SHARED_SCHEMA" && -n "$schema_name" && "$schema_name" != "public" ]]; then
      require_identifier "$schema_name" "schema_name"
      psql_shared -c "DROP SCHEMA IF EXISTS \"${schema_name}\" CASCADE;"
    fi

    if [[ "$tenancy_mode" == "DEDICATED_DB" && -n "$db_name" ]]; then
      require_identifier "$db_name" "db_name"
      psql_db postgres <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${db_name}'
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS "${db_name}";
SQL
    fi

    psql_shared <<SQL
DELETE FROM public.subscriptions WHERE tenant_id = ${tenant_id};
DELETE FROM public.tenant_feature_overrides WHERE tenant_id = ${tenant_id};
DELETE FROM public.tenants WHERE id = ${tenant_id};
SQL
  done <<< "$TENANTS_TO_DELETE_RAW"
fi

printf "scope,username,password\n" > "$OUTPUT_PATH"
if [[ "$KEEP_TENANT_IS_PUBLIC" == "true" ]]; then
  printf '"%s","%s","%s"\n' "$KEEP_TENANT" "UNCHANGED" "UNCHANGED" >> "$OUTPUT_PATH"
else
  printf '"public","%s","%s"\n' "$PLATFORM_USERNAME" "$PLATFORM_PASSWORD" >> "$OUTPUT_PATH"
  printf '"%s","%s","%s"\n' "$KEEP_TENANT" "$TENANT_ADMIN_USERNAME" "$TENANT_ADMIN_PASSWORD" >> "$OUTPUT_PATH"
  printf '"%s","%s","%s"\n' "$KEEP_TENANT" "$STAFF_USERNAME" "$STAFF_PASSWORD" >> "$OUTPUT_PATH"
  printf '"%s","%s","%s"\n' "$KEEP_TENANT" "$CARI_USERNAME" "$CARI_PASSWORD" >> "$OUTPUT_PATH"
fi

echo
echo "Cleanup completed."
echo "Credential output written to: $OUTPUT_PATH"
