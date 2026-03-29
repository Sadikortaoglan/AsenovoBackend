#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  PGPASSWORD='<db_password>' ./deploy/scripts/reset_tenant_user_passwords.sh \
    --db-host <host> \
    --db-port <port> \
    --db-name <db_name> \
    --db-user <db_user> \
    --tenant-subdomain <subdomain> \
    --user <username[:newPassword]> \
    [--user <username[:newPassword]> ...] \
    [--output <csv_path>]

Description:
  Resets selected tenant users in the target tenant.
  If a password is omitted, a random one is generated.
  Results are written to a sensitive CSV output file:
  tenant,schema_db,username,new_password
USAGE
}

DB_HOST=""
DB_PORT="5432"
DB_NAME=""
DB_USER=""
TENANT_SUBDOMAIN=""
OUTPUT_PATH=""
declare -a USER_SPECS=()

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

generate_password() {
  openssl rand -base64 18 | tr -d '\n' | tr '/+' 'AB' | cut -c1-16
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    --tenant-subdomain) TENANT_SUBDOMAIN="$2"; shift 2 ;;
    --user) USER_SPECS+=("$2"); shift 2 ;;
    --output) OUTPUT_PATH="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: PGPASSWORD is required in environment." >&2
  exit 1
fi

if [[ -z "$DB_HOST" || -z "$DB_NAME" || -z "$DB_USER" || -z "$TENANT_SUBDOMAIN" || "${#USER_SPECS[@]}" -eq 0 ]]; then
  echo "ERROR: required arguments are missing." >&2
  usage
  exit 1
fi

mkdir -p tmp/password-resets
if [[ -z "$OUTPUT_PATH" ]]; then
  OUTPUT_PATH="tmp/password-resets/${TENANT_SUBDOMAIN}-password-resets-$(date +%Y%m%d-%H%M%S).csv"
fi

TENANT_ROW="$(psql -X -A -F '|' -t -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c \
  "SELECT tenancy_mode, COALESCE(schema_name, ''), COALESCE(db_name, ''), COALESCE(status::text, '') FROM public.tenants WHERE subdomain = '$(sql_literal "$TENANT_SUBDOMAIN")' LIMIT 1")"

if [[ -z "$TENANT_ROW" ]]; then
  echo "ERROR: tenant not found: $TENANT_SUBDOMAIN" >&2
  exit 1
fi

IFS='|' read -r TENANCY_MODE TENANT_SCHEMA TENANT_DB TENANT_STATUS <<< "$TENANT_ROW"
if [[ "$TENANT_STATUS" == "PENDING" || "$TENANT_STATUS" == "PROVISIONING_FAILED" ]]; then
  echo "ERROR: tenant $TENANT_SUBDOMAIN is not ready (status=$TENANT_STATUS)." >&2
  exit 1
fi

if [[ "$TENANCY_MODE" == "SHARED_SCHEMA" ]]; then
  require_identifier "$TENANT_SCHEMA" "schema_name"
  TARGET_DB="$DB_NAME"
  TARGET_REF="$TENANT_SCHEMA"
  USER_TABLE="${TENANT_SCHEMA}.users"
elif [[ "$TENANCY_MODE" == "DEDICATED_DB" ]]; then
  TARGET_DB="$TENANT_DB"
  TARGET_REF="$TENANT_DB"
  USER_TABLE="public.users"
else
  echo "ERROR: unsupported tenancy_mode: $TENANCY_MODE" >&2
  exit 1
fi

psql -X -q -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null

printf "tenant,schema_db,username,new_password\n" > "$OUTPUT_PATH"

for spec in "${USER_SPECS[@]}"; do
  username="${spec%%:*}"
  password=""
  if [[ "$spec" == *:* ]]; then
    password="${spec#*:}"
  fi
  if [[ -z "$password" ]]; then
    password="$(generate_password)"
  fi

  username_sql="$(sql_literal "$username")"
  password_sql="$(sql_literal "$password")"

  updated_username="$(psql -X -A -t -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB" -c "
    UPDATE ${USER_TABLE}
    SET password_hash = crypt('${password_sql}', gen_salt('bf', 10)),
        active = true,
        enabled = true,
        locked = false
    WHERE username = '${username_sql}'
    RETURNING username;
  " | tr -d '[:space:]')"

  if [[ -z "$updated_username" ]]; then
    echo "ERROR: user not found in tenant ${TENANT_SUBDOMAIN}: ${username}" >&2
    exit 1
  fi

  printf '"%s","%s","%s","%s"\n' "$TENANT_SUBDOMAIN" "$TARGET_REF" "$updated_username" "$password" >> "$OUTPUT_PATH"
done

echo "Password reset output written to: $OUTPUT_PATH"
