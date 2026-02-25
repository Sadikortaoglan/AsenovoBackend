#!/bin/bash
# Flyway repair script to fix migration validation issues

echo "🔧 Flyway Repair Script"
echo "This script will repair Flyway schema history to fix validation errors"
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Get database connection info from application.yml
DB_HOST="localhost"
DB_PORT="5433"
DB_NAME="sara_asansor"
DB_USER="sara_asansor"
DB_PASS="sara_asansor"

echo "📋 Database connection:"
echo "   Host: $DB_HOST:$DB_PORT"
echo "   Database: $DB_NAME"
echo "   User: $DB_USER"
echo ""

# Run Flyway repair via Maven
echo "🔄 Running Flyway repair..."
mvn flyway:repair -Dflyway.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
                  -Dflyway.user=${DB_USER} \
                  -Dflyway.password=${DB_PASS} \
                  -Dflyway.locations=classpath:db/migration

echo ""
echo "✅ Flyway repair completed!"
echo "You can now restart the application."
