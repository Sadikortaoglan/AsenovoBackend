#!/bin/bash

# Backend API Test Script
# Usage: ./test-api.sh [base_url]
# Default: http://localhost:8081/api

BASE_URL="${1:-http://localhost:8080/api}"
TOKEN_FILE="/tmp/sara_api_token.txt"

echo "🔵 Testing Sara Asansör API"
echo "Base URL: $BASE_URL"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS=0
PASSED=0
FAILED=0

# Test function
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    local description=$5
    
    TESTS=$((TESTS + 1))
    
    echo -n "Test $TESTS: $method $endpoint - $description ... "
    
    # Build curl command
    CURL_CMD="curl -s -w '\n%{http_code}' -X $method"
    
    # Add headers
    if [ -f "$TOKEN_FILE" ]; then
        TOKEN=$(cat "$TOKEN_FILE")
        CURL_CMD="$CURL_CMD -H 'Authorization: Bearer $TOKEN'"
    fi
    
    CURL_CMD="$CURL_CMD -H 'Content-Type: application/json'"
    CURL_CMD="$CURL_CMD -H 'Accept: application/json'"
    
    # Add data if provided
    if [ -n "$data" ]; then
        CURL_CMD="$CURL_CMD -d '$data'"
    fi
    
    # Add URL
    CURL_CMD="$CURL_CMD '$BASE_URL$endpoint'"
    
    # Execute
    RESPONSE=$(eval $CURL_CMD)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    # Check status
    if [ "$HTTP_CODE" == "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $HTTP_CODE)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected $expected_status, got $HTTP_CODE)"
        echo -e "${YELLOW}Response:${NC} $BODY"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# 1. Authentication
echo "=== AUTHENTICATION ==="
test_endpoint "POST" "/auth/login" '{"username":"patron","password":"password"}' "200" "Login"
if [ $? -eq 0 ]; then
    TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        echo "$TOKEN" > "$TOKEN_FILE"
        echo "Token saved: ${TOKEN:0:20}..."
    fi
fi

echo ""

# 2. Dashboard
echo "=== DASHBOARD ==="
test_endpoint "GET" "/dashboard/summary" "" "200" "Get dashboard summary"

echo ""

# 3. Elevators
echo "=== ELEVATORS ==="
test_endpoint "GET" "/elevators" "" "200" "Get all elevators"
test_endpoint "GET" "/elevators/1" "" "200" "Get elevator by ID"
test_endpoint "GET" "/elevators/1/status" "" "200" "Get elevator status"

# Create elevator
ELEVATOR_DATA='{"identityNumber":"TEST-001","buildingName":"Test Building","address":"Test Address","elevatorNumber":"E1","inspectionDate":"2025-01-01"}'
test_endpoint "POST" "/elevators" "$ELEVATOR_DATA" "200" "Create elevator"

echo ""

# 4. Maintenances
echo "=== MAINTENANCES ==="
test_endpoint "GET" "/maintenances" "" "200" "Get all maintenances"
test_endpoint "GET" "/maintenances?paid=true" "" "200" "Get paid maintenances"
test_endpoint "GET" "/maintenances?paid=false" "" "200" "Get unpaid maintenances"
test_endpoint "GET" "/maintenances?dateFrom=2025-01-01&dateTo=2025-12-31" "" "200" "Get maintenances by date range"
test_endpoint "GET" "/maintenances/summary" "" "200" "Get maintenance summary"
test_endpoint "GET" "/maintenances/summary?month=2026-01" "" "200" "Get monthly summary"

echo ""

# 5. Faults
echo "=== FAULTS ==="
test_endpoint "GET" "/faults" "" "200" "Get all faults"
test_endpoint "GET" "/faults?status=ACIK" "" "200" "Get open faults (Turkish)"
test_endpoint "GET" "/faults?status=TAMAMLANDI" "" "200" "Get completed faults (Turkish)"
test_endpoint "GET" "/faults?status=OPEN" "" "200" "Get faults (English enum)"
test_endpoint "GET" "/faults?status=COMPLETED" "" "200" "Get faults (English enum)"

# Create fault (assuming elevator ID 1 exists)
FAULT_DATA='{"elevatorId":1,"faultSubject":"Test Fault","contactPerson":"Test Person","buildingAuthorizedMessage":"Test Message","description":"Test Description"}'
test_endpoint "POST" "/faults" "$FAULT_DATA" "200" "Create fault"

echo ""

# 6. Inspections
echo "=== INSPECTIONS ==="
test_endpoint "GET" "/inspections" "" "200" "Get all inspections"

# Create inspection
INSPECTION_DATA='{"elevatorId":1,"date":"2025-01-01","result":"PASS","description":"Test Inspection"}'
test_endpoint "POST" "/inspections" "$INSPECTION_DATA" "200" "Create inspection"

echo ""

# 7. Payments
echo "=== PAYMENTS ==="
test_endpoint "GET" "/payments" "" "200" "Get all payments"
test_endpoint "GET" "/payments?dateFrom=2025-01-01&dateTo=2025-12-31" "" "200" "Get payments by date range"

echo ""

# 8. Parts
echo "=== PARTS ==="
test_endpoint "GET" "/parts" "" "200" "Get all parts"

# Create part
PART_DATA='{"name":"Test Part","unitPrice":100.0,"stock":10}'
test_endpoint "POST" "/parts" "$PART_DATA" "200" "Create part"

echo ""

# 9. Warnings
echo "=== WARNINGS ==="
test_endpoint "GET" "/warnings" "" "200" "Get all warnings"
test_endpoint "GET" "/warnings?type=EXPIRED" "" "200" "Get expired warnings"
test_endpoint "GET" "/warnings?type=WARNING" "" "200" "Get warning warnings"

echo ""

# Summary
echo "=== TEST SUMMARY ==="
echo "Total Tests: $TESTS"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi

