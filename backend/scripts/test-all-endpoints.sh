#!/bin/bash

BASE_URL="http://localhost:8080/api"
echo "🧪 Testing all POST, PUT, DELETE endpoints..."
echo ""

# Login and get token
echo "1. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"username":"patron","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('data', {}).get('accessToken', ''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed!"
  echo $LOGIN_RESPONSE
  exit 1
fi

echo "✅ Token obtained"
echo ""

# Test function
test_endpoint() {
  local method=$1
  local endpoint=$2
  local data=$3
  local description=$4
  
  echo "Testing: $method $endpoint - $description"
  
  if [ "$method" = "POST" ] || [ "$method" = "PUT" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "$data")
  else
    RESPONSE=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer $TOKEN")
  fi
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✅ $method $endpoint - HTTP $HTTP_CODE"
    # Extract ID if present
    ID=$(echo "$BODY" | python3 -c "import sys, json; d=json.load(sys.stdin); data=d.get('data', {}); print(data.get('id', ''))" 2>/dev/null)
    if [ ! -z "$ID" ]; then
      echo "   Created/Updated ID: $ID"
    fi
  else
    echo "❌ $method $endpoint - HTTP $HTTP_CODE"
    echo "   Response: $BODY"
  fi
  echo ""
}

# Test ELEVATORS
echo "=== ELEVATORS ==="
test_endpoint "POST" "/elevators" '{
  "identityNumber": "TEST-ELEV-001",
  "buildingName": "Test Building",
  "address": "Test Address",
  "elevatorNumber": "T1",
  "floorCount": 5,
  "capacity": 630,
  "speed": 1.0,
  "inspectionDate": "2024-01-01",
  "expiryDate": "2025-01-01"
}' "Create elevator"

ELEVATOR_ID=$(curl -s "$BASE_URL/elevators" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; elevators=json.load(sys.stdin).get('data', []); print(elevators[0].get('id', '') if elevators else '')" 2>/dev/null)

if [ ! -z "$ELEVATOR_ID" ]; then
  test_endpoint "PUT" "/elevators/$ELEVATOR_ID" '{
    "identityNumber": "TEST-ELEV-001-UPDATED",
    "buildingName": "Updated Test Building",
    "address": "Updated Test Address",
    "elevatorNumber": "T1",
    "floorCount": 6,
    "capacity": 800,
    "speed": 1.5,
    "inspectionDate": "2024-01-01",
    "expiryDate": "2025-01-01"
  }' "Update elevator"
  
  # Don't delete test elevator, keep it for other tests
  # test_endpoint "DELETE" "/elevators/$ELEVATOR_ID" "" "Delete elevator"
fi

# Test MAINTENANCES
echo "=== MAINTENANCES ==="
if [ ! -z "$ELEVATOR_ID" ]; then
  test_endpoint "POST" "/maintenances" "{
    \"elevatorId\": $ELEVATOR_ID,
    \"date\": \"2024-12-15\",
    \"description\": \"Test maintenance\",
    \"amount\": 1500.0,
    \"isPaid\": false
  }" "Create maintenance"
  
  MAINTENANCE_ID=$(curl -s "$BASE_URL/maintenances" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; items=json.load(sys.stdin).get('data', []); print(items[0].get('id', '') if items else '')" 2>/dev/null)
  
  if [ ! -z "$MAINTENANCE_ID" ]; then
    test_endpoint "PUT" "/maintenances/$MAINTENANCE_ID" "{
      \"elevatorId\": $ELEVATOR_ID,
      \"date\": \"2024-12-16\",
      \"description\": \"Updated test maintenance\",
      \"amount\": 2000.0,
      \"isPaid\": true,
      \"paymentDate\": \"2024-12-16\"
    }" "Update maintenance"
  fi
fi

# Test FAULTS
echo "=== FAULTS ==="
if [ ! -z "$ELEVATOR_ID" ]; then
  test_endpoint "POST" "/faults" "{
    \"elevatorId\": $ELEVATOR_ID,
    \"faultSubject\": \"Test Fault\",
    \"contactPerson\": \"Test Person\",
    \"buildingAuthorizedMessage\": \"Test message\",
    \"description\": \"Test fault description\",
    \"status\": \"OPEN\"
  }" "Create fault"
  
  FAULT_ID=$(curl -s "$BASE_URL/faults" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; items=json.load(sys.stdin).get('data', []); print(items[0].get('id', '') if items else '')" 2>/dev/null)
  
  if [ ! -z "$FAULT_ID" ]; then
    test_endpoint "PUT" "/faults/$FAULT_ID" "{
      \"elevatorId\": $ELEVATOR_ID,
      \"faultSubject\": \"Updated Test Fault\",
      \"contactPerson\": \"Updated Person\",
      \"buildingAuthorizedMessage\": \"Updated message\",
      \"description\": \"Updated fault description\",
      \"status\": \"COMPLETED\"
    }" "Update fault"
  fi
fi

# Test INSPECTIONS
echo "=== INSPECTIONS ==="
if [ ! -z "$ELEVATOR_ID" ]; then
  test_endpoint "POST" "/inspections" "{
    \"elevatorId\": $ELEVATOR_ID,
    \"date\": \"2024-12-15\",
    \"result\": \"PASSED\",
    \"description\": \"Test inspection\"
  }" "Create inspection"
  
  INSPECTION_ID=$(curl -s "$BASE_URL/inspections" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; items=json.load(sys.stdin).get('data', []); print(items[0].get('id', '') if items else '')" 2>/dev/null)
  
  if [ ! -z "$INSPECTION_ID" ]; then
    test_endpoint "PUT" "/inspections/$INSPECTION_ID" "{
      \"elevatorId\": $ELEVATOR_ID,
      \"date\": \"2024-12-16\",
      \"result\": \"FAILED\",
      \"description\": \"Updated test inspection\"
    }" "Update inspection"
  fi
fi

# Test PAYMENT RECEIPTS
echo "=== PAYMENT RECEIPTS ==="
if [ ! -z "$MAINTENANCE_ID" ]; then
  test_endpoint "POST" "/payments" "{
    \"maintenanceId\": $MAINTENANCE_ID,
    \"amount\": 1500.0,
    \"payerName\": \"Test Payer\",
    \"date\": \"2024-12-15\",
    \"note\": \"Test payment\"
  }" "Create payment receipt"
  
  PAYMENT_ID=$(curl -s "$BASE_URL/payments" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; items=json.load(sys.stdin).get('data', []); print(items[0].get('id', '') if items else '')" 2>/dev/null)
  
  if [ ! -z "$PAYMENT_ID" ]; then
    test_endpoint "PUT" "/payments/$PAYMENT_ID" "{
      \"maintenanceId\": $MAINTENANCE_ID,
      \"amount\": 2000.0,
      \"payerName\": \"Updated Payer\",
      \"date\": \"2024-12-16\",
      \"note\": \"Updated test payment\"
    }" "Update payment receipt"
  fi
fi

# Test PARTS
echo "=== PARTS ==="
test_endpoint "POST" "/parts" '{
  "name": "Test Part",
  "unitPrice": 100.0,
  "stock": 10
}' "Create part"

PART_ID=$(curl -s "$BASE_URL/parts" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; items=json.load(sys.stdin).get('data', []); print(items[0].get('id', '') if items else '')" 2>/dev/null)

if [ ! -z "$PART_ID" ]; then
  test_endpoint "PUT" "/parts/$PART_ID" '{
    "name": "Updated Test Part",
    "unitPrice": 150.0,
    "stock": 20
  }' "Update part"
fi

# Test USERS
echo "=== USERS ==="
USERNAME="testuser_$(date +%s)"
test_endpoint "POST" "/users" "{
  \"username\": \"$USERNAME\",
  \"passwordHash\": \"testpass123\",
  \"role\": \"PERSONEL\",
  \"active\": true
}" "Create user"

USER_ID=$(curl -s "$BASE_URL/users" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys, json; users=json.load(sys.stdin).get('data', []); print([u.get('id') for u in users if u.get('username', '').startswith('testuser_')][-1] if users else '')" 2>/dev/null)

if [ ! -z "$USER_ID" ]; then
  test_endpoint "PUT" "/users/$USER_ID" '{
    "username": "testuser_'$(date +%s)'_updated",
    "role": "PERSONEL",
    "active": true
  }' "Update user"
fi

echo "=== TEST SUMMARY ==="
echo "✅ All endpoint tests completed!"
echo "Note: DELETE endpoints were not tested to preserve test data"

