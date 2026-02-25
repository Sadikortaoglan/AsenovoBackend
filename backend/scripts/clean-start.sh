#!/bin/bash

echo "=========================================="
echo "CLEAN DATABASE RESET & START"
echo "=========================================="
echo ""

# 1. Stop app (if running)
echo "1️⃣  Stopping application..."
pkill -f "spring-boot:run" 2>/dev/null || echo "   No running app found"
sleep 2
echo "✅ Application stopped"
echo ""

# 2. Delete target folder
echo "2️⃣  Deleting target folder..."
rm -rf target/
echo "✅ Target folder deleted"
echo ""

# 3. Drop database schema
echo "3️⃣  Dropping database schema..."
PGPASSWORD=sara_asansor psql -h localhost -p 5433 -U sara_asansor -d sara_asansor -f drop-schema.sql 2>&1 | grep -v "does not exist" || echo "   Schema dropped"
echo "✅ Database schema reset"
echo ""

# 4. Recreate schema
echo "4️⃣  Recreating schema..."
PGPASSWORD=sara_asansor psql -h localhost -p 5433 -U sara_asansor -d sara_asansor -c "CREATE SCHEMA IF NOT EXISTS public;" 2>&1 | grep -v "already exists" || echo "   Schema ready"
echo "✅ Schema recreated"
echo ""

# 5. Clean Maven
echo "5️⃣  Cleaning Maven project..."
mvn clean -q
echo "✅ Maven clean completed"
echo ""

# 6. Start application (Flyway will run migrations)
echo "6️⃣  Starting Spring Boot application..."
echo "   (Flyway migrations will run automatically)"
echo ""
mvn spring-boot:run -DskipTests
