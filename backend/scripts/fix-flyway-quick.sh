#!/bin/bash
# Quick fix: Delete V5 migration record from database

echo "🔧 Quick Flyway Fix - Delete V5 migration record"
echo ""

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_NAME="${DB_NAME:-sara_asansor}"
DB_USER="${DB_USER:-sara_asansor}"

echo "📋 Connecting to database: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Try Docker exec first
if docker ps | grep -q sara-asansor-postgres; then
    echo "🐳 Using Docker container..."
    docker exec -i sara-asansor-postgres psql -U $DB_USER -d $DB_NAME << SQL
DELETE FROM flyway_schema_history WHERE version = 5;
SELECT 'V5 deleted. Current migrations:' as status;
SELECT version, description, type, installed_on 
FROM flyway_schema_history 
ORDER BY version;
SQL
elif command -v psql > /dev/null 2>&1; then
    echo "💻 Using local psql..."
    PGPASSWORD=$DB_USER psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << SQL
DELETE FROM flyway_schema_history WHERE version = 5;
SELECT 'V5 deleted. Current migrations:' as status;
SELECT version, description, type, installed_on 
FROM flyway_schema_history 
ORDER BY version;
SQL
else
    echo "❌ Neither Docker nor psql found. Please run manually:"
    echo ""
    echo "DELETE FROM flyway_schema_history WHERE version = 5;"
    echo ""
    exit 1
fi

echo ""
echo "✅ Done! Now restart the application."
