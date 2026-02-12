# Sara Elevator API - Request & Response Examples

## Base URL
```
http://localhost:8081/api
```

## Authentication

All endpoints (except auth) require Authorization header:
```
Authorization: Bearer {accessToken}
```

---

## 1. Authentication Endpoints

### 1.1 POST /auth/login

**Request:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "patron",
  "password": "password"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUEFUUk9OIiwidXNlcklkIjoxLCJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgwODAxODUsImV4cCI6MTc2ODA4Mzc4NX0...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgwODAxODUsImV4cCI6MTc2ODY4NDk4NX0...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  },
  "errors": null
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Login failed: Bad credentials",
  "data": null,
  "errors": null
}
```

---

### 1.2 POST /auth/register

**Request:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "yeni_kullanici",
  "password": "123456",
  "role": "PERSONEL"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User successfully registered and logged in",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "userId": 2,
    "username": "yeni_kullanici",
    "role": "PERSONEL"
  },
  "errors": null
}
```

---

### 1.3 POST /auth/refresh

**Request:**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  },
  "errors": null
}
```

---

### 1.4 POST /auth/logout

**Request:**
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null,
  "errors": null
}
```

---

## 2. Elevator Endpoints

### 2.1 GET /elevators

**Request:**
```http
GET /api/elevators
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "identityNumber": "ELEV-001",
      "buildingName": "Central Business Center",
      "address": "123 Main Street, Business District",
      "elevatorNumber": "A1",
      "floorCount": 5,
      "capacity": 630,
      "speed": 1.0,
      "technicalNotes": null,
      "driveType": null,
      "machineBrand": null,
      "doorType": null,
      "installationYear": null,
      "serialNumber": null,
      "controlSystem": null,
      "rope": null,
      "modernization": null,
      "inspectionDate": "2024-07-10",
      "expiryDate": "2025-07-10"
    }
  ],
  "errors": null
}
```

---

### 2.2 GET /elevators/{id}

**Request:**
```http
GET /api/elevators/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "identityNumber": "ELEV-001",
    "buildingName": "Central Business Center",
    "address": "123 Main Street, Business District",
    "elevatorNumber": "A1",
    "floorCount": 5,
    "capacity": 630,
    "speed": 1.0,
    "inspectionDate": "2024-07-10",
    "expiryDate": "2025-07-10"
  },
  "errors": null
}
```

---

### 2.3 POST /elevators

**Request:**
```http
POST /api/elevators
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "identityNumber": "ELEV-004",
  "buildingName": "New Business Center",
  "address": "456 Residential Avenue, Block B",
  "elevatorNumber": "A2",
  "floorCount": 4,
  "capacity": 450,
  "speed": 0.75,
  "technicalNotes": "New installation",
  "driveType": "Hydraulic",
  "machineBrand": "Otis",
  "doorType": "Automatic",
  "installationYear": 2023,
  "serialNumber": "SN-12345",
  "controlSystem": "Siemens",
  "rope": "6 ropes",
  "modernization": "2023",
  "inspectionDate": "2024-01-01"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Elevator successfully added",
  "data": {
    "id": 4,
    "identityNumber": "ELEV-004",
    "buildingName": "New Business Center",
    "address": "456 Residential Avenue, Block B",
    "inspectionDate": "2024-01-01",
    "expiryDate": "2025-01-01"
  },
  "errors": null
}
```

**Note:** `expiryDate` is automatically calculated as `inspectionDate + 12 months` is calculated.

---

### 2.4 PUT /elevators/{id}

**Request:**
```http
PUT /api/elevators/1
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "identityNumber": "ELEV-001",
  "buildingName": "Central Business Center Updated",
  "address": "123 Main Street, Business District",
  "floorCount": 6,
  "capacity": 800,
  "inspectionDate": "2024-07-15"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Elevator successfully updated",
  "data": {
    "id": 1,
    "identityNumber": "ELEV-001",
    "buildingName": "Central Business Center Updated",
    "expiryDate": "2025-07-15"
  },
  "errors": null
}
```

---

### 2.5 DELETE /elevators/{id}

**Request:**
```http
DELETE /api/elevators/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Elevator successfully deleted",
  "data": null,
  "errors": null
}
```

---

### 2.6 GET /elevators/{id}/status

**Request:**
```http
GET /api/elevators/1/status
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "status": "OK",
    "daysLeft": 181,
    "identityNumber": "ELEV-001",
    "buildingName": "Central Business Center",
    "expiryDate": "2025-07-10"
  },
  "errors": null
}
```

**Status Values:**
- `OK`: Normal (more than 30 days remaining)
- `WARNING`: Warning (30 days or less remaining)
- `EXPIRED`: Expired (expiryDate has passed)

---

## 3. Maintenance Endpoints

### 3.1 GET /maintenances

**Request (All):**
```http
GET /api/maintenances
Authorization: Bearer {accessToken}
```

**Request (Filtering - Paid):**
```http
GET /api/maintenances?paid=true
Authorization: Bearer {accessToken}
```

**Request (Filtering - Unpaid):**
```http
GET /api/maintenances?paid=false
Authorization: Bearer {accessToken}
```

**Request (Filtering - Date Range):**
```http
GET /api/maintenances?dateFrom=2026-01-01&dateTo=2026-01-31
Authorization: Bearer {accessToken}
```

**Request (Filtering - Karma):**
```http
GET /api/maintenances?paid=false&dateFrom=2026-01-01
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "elevatorId": 1,
      "elevatorBuildingName": "Central Business Center",
      "date": "2026-01-10",
      "description": "Monthly Periodic Maintenance",
      "technicianUserId": 2,
      "technicianUsername": "teknisyen1",
      "amount": 1500.00,
      "isPaid": false,
      "paymentDate": null
    }
  ],
  "errors": null
}
```

---

### 3.2 GET /maintenances/{id}

**Request:**
```http
GET /api/maintenances/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "elevatorId": 1,
    "elevatorBuildingName": "Central Business Center",
    "date": "2026-01-10",
    "description": "Monthly Periodic Maintenance",
    "technicianUserId": 2,
    "technicianUsername": "teknisyen1",
    "amount": 1500.00,
    "isPaid": false,
    "paymentDate": null
  },
  "errors": null
}
```

---

### 3.3 GET /maintenances/elevator/{elevatorId}

**Request:**
```http
GET /api/maintenances/elevator/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Returns array as above, sorted by Date DESC

---

### 3.4 POST /maintenances

**Request:**
```http
POST /api/maintenances
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "elevatorId": 1,
  "date": "2026-01-15",
  "description": "Monthly Periodic Maintenance",
  "technicianUserId": 2,
  "amount": 1500.00,
  "isPaid": false
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Maintenance record successfully added",
  "data": {
    "id": 1,
    "elevatorId": 1,
    "elevatorBuildingName": "Central Business Center",
    "date": "2026-01-15",
    "description": "Monthly Periodic Maintenance",
    "technicianUserId": 2,
    "technicianUsername": "teknisyen1",
    "amount": 1500.00,
    "isPaid": false,
    "paymentDate": null
  },
  "errors": null
}
```

---

### 3.5 PUT /maintenances/{id}

**Request:**
```http
PUT /api/maintenances/1
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "elevatorId": 1,
  "date": "2026-01-15",
  "description": "Monthly Periodic Maintenance - Updated",
  "amount": 2000.00,
  "isPaid": true,
  "paymentDate": "2026-01-20"
}
```

**Response (200 OK):** - Same as above

---

### 3.6 POST /maintenances/{id}/mark-paid

**Request (Mark as Paid):**
```http
POST /api/maintenances/1/mark-paid?paid=true
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Marked as paid",
  "data": {
    "id": 1,
    "isPaid": true,
    "paymentDate": "2026-01-11"
  },
  "errors": null
}
```

**Request (Remove Payment Mark):**
```http
POST /api/maintenances/1/mark-paid?paid=false
Authorization: Bearer {accessToken}
```

---

### 3.7 GET /maintenances/summary

**Request (Bu Ay):**
```http
GET /api/maintenances/summary
Authorization: Bearer {accessToken}
```

**Request (Belirli Ay):**
```http
GET /api/maintenances/summary?month=2026-01
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalCount": 10,
    "paidCount": 7,
    "unpaidCount": 3,
    "totalAmount": 15000.0,
    "paidAmount": 10500.0,
    "unpaidAmount": 4500.0
  },
  "errors": null
}
```

---

### 3.8 DELETE /maintenances/{id}

**Request:**
```http
DELETE /api/maintenances/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Maintenance record successfully deleted",
  "data": null,
  "errors": null
}
```

---

## 4. Fault (Fault) Endpoints

### 4.1 GET /faults

**Request (All):**
```http
GET /api/faults
Authorization: Bearer {accessToken}
```

**Request (Open Faults):**
```http
GET /api/faults?status=ACIK
Authorization: Bearer {accessToken}
```

**Request (Completed Faults):**
```http
GET /api/faults?status=TAMAMLANDI
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "elevatorId": 1,
      "elevatorBuildingName": "Central Business Center",
      "elevatorIdentityNumber": "ELEV-001",
      "faultSubject": "Motor not working",
      "contactPerson": "John Smith",
      "buildingAuthorizedMessage": "Urgent intervention required",
      "description": "Elevator stuck on 2nd floor",
      "status": "OPEN",
      "createdAt": "2026-01-10T20:30:00"
    }
  ],
  "errors": null
}
```

---

### 4.2 GET /faults/{id}

**Request:**
```http
GET /api/faults/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Same as above returns single object

---

### 4.3 POST /faults

**Request:**
```http
POST /api/faults
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "elevatorId": 1,
  "faultSubject": "Motor not working",
  "contactPerson": "John Smith",
  "buildingAuthorizedMessage": "Urgent intervention required",
  "description": "Elevator stuck on 2nd floor"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fault record successfully added",
  "data": {
    "id": 1,
    "elevatorId": 1,
    "elevatorBuildingName": "Central Business Center",
    "faultSubject": "Motor not working",
    "contactPerson": "John Smith",
    "status": "OPEN",
    "createdAt": "2026-01-10T20:30:00"
  },
  "errors": null
}
```

---

### 4.4 PUT /faults/{id}/status

**Request:**
```http
PUT /api/faults/1/status?status=TAMAMLANDI
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Fault status updated",
  "data": {
    "id": 1,
    "status": "COMPLETED",
    "faultSubject": "Motor not working"
  },
  "errors": null
}
```

---

## 5. Inspection (Inspection) Endpoints

### 5.1 GET /inspections

**Request:**
```http
GET /api/inspections
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "elevatorId": 1,
      "elevatorBuildingName": "Central Business Center",
      "elevatorIdentityNumber": "ELEV-001",
      "date": "2026-01-10",
      "result": "SUCCESSFUL",
      "description": "All checks completed, everything is in order",
      "createdAt": "2026-01-10T20:30:00"
    }
  ],
  "errors": null
}
```

---

### 5.2 GET /inspections/{id}

**Request:**
```http
GET /api/inspections/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Same as above returns single object

---

### 5.3 GET /inspections/elevator/{elevatorId}

**Request:**
```http
GET /api/inspections/elevator/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Returns array, sorted by Date DESC

---

### 5.4 POST /inspections

**Request:**
```http
POST /api/inspections
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "elevatorId": 1,
  "date": "2026-01-10",
  "result": "SUCCESSFUL",
  "description": "All checks completed, everything is in order"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Inspection record successfully added",
  "data": {
    "id": 1,
    "elevatorId": 1,
    "elevatorBuildingName": "Central Business Center",
    "date": "2026-01-10",
    "result": "SUCCESSFUL",
    "description": "All checks completed, everything is in order",
    "createdAt": "2026-01-10T20:30:00"
  },
  "errors": null
}
```

---

## 6. Payment Receipt (Receipt) Endpoints

### 6.1 GET /payments

**Request (All):**
```http
GET /api/payments
Authorization: Bearer {accessToken}
```

**Request (Date Range):**
```http
GET /api/payments?dateFrom=2026-01-01&dateTo=2026-01-31
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "maintenanceId": 1,
      "amount": 500.0,
      "payerName": "Building Management",
      "date": "2026-01-10",
      "note": "Nakit Payment",
      "createdAt": "2026-01-10T20:30:00"
    }
  ],
  "errors": null
}
```

---

### 6.2 GET /payments/{id}

**Request:**
```http
GET /api/payments/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Same as above returns single object

---

### 6.3 POST /payments

**Request:**
```http
POST /api/payments
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "maintenanceId": 1,
  "amount": 500.0,
  "payerName": "Building Management",
  "date": "2026-01-10",
  "note": "Nakit Payment"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment receipt successfully added",
  "data": {
    "id": 1,
    "maintenanceId": 1,
    "amount": 500.0,
    "payerName": "Building Management",
    "date": "2026-01-10",
    "note": "Nakit Payment",
    "createdAt": "2026-01-10T20:30:00"
  },
  "errors": null
}
```

**Note:** When a receipt is created, the related maintenance is automatically marked as `isPaid=true`.

---

## 7. Part Endpoints

### 7.1 GET /parts

**Request:**
```http
GET /api/parts
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "Elevator Rope 8mm",
      "unitPrice": 150.00,
      "stock": 10,
      "createdAt": "2026-01-10T20:30:00"
    }
  ],
  "errors": null
}
```

---

### 7.2 GET /parts/{id}

**Request:**
```http
GET /api/parts/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):** - Same as above returns single object

---

### 7.3 POST /parts

**Request:**
```http
POST /api/parts
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Yeni Part",
  "unitPrice": 250.00,
  "stock": 15
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 5,
    "name": "Yeni Part",
    "unitPrice": 250.00,
    "stock": 15,
    "createdAt": "2026-01-10T20:30:00"
  },
  "errors": null
}
```

---

### 7.4 PUT /parts/{id}

**Request:**
```http
PUT /api/parts/1
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "New Part Updated",
  "unitPrice": 300.00,
  "stock": 20
}
```

**Response (200 OK):** - Same as above

---

### 7.5 DELETE /parts/{id}

**Request:**
```http
DELETE /api/parts/1
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": null,
  "errors": null
}
```

---

## 8. Warning Endpoints

### 8.1 GET /warnings

**Request (All):**
```http
GET /api/warnings
Authorization: Bearer {accessToken}
```

**Request (Expired):**
```http
GET /api/warnings?type=EXPIRED
Authorization: Bearer {accessToken}
```

**Request (Warning Veren):**
```http
GET /api/warnings?type=WARNING
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 3,
      "identityNumber": "ELEV-003",
      "buildingName": "Commercial Center",
      "address": "789 Commerce Boulevard, Suite 8",
      "expiryDate": "2025-12-10"
    }
  ],
  "errors": null
}
```

---

## 9. Dashboard Endpoints

### 9.1 GET /dashboard/summary

**Request:**
```http
GET /api/dashboard/summary
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalElevators": 5,
    "totalMaintenances": 10,
    "totalIncome": 5000.0,
    "totalDebt": 2000.0,
    "expiredCount": 1,
    "warningCount": 2
  },
  "errors": null
}
```

---

## 10. Error Responses

### 400 Bad Request - Validation Error

```json
{
  "success": false,
  "message": "Validation error",
  "data": null,
  "errors": {
    "username": "Username cannot be empty",
    "password": "Password cannot be empty"
  }
}
```

### 400 Bad Request - Business Logic Error

```json
{
  "success": false,
  "message": "Elevator not found",
  "data": null,
  "errors": null
}
```

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null,
  "errors": null
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Access Denied",
  "data": null,
  "errors": null
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Maintenance record not found",
  "data": null,
  "errors": null
}
```

---

## 11. Örnek İstek Senaryoları

### Senaryo 1: Tam Akış - Fault ve Maintenance

**1. Login:**
```http
POST /api/auth/login
{"username": "patron", "password": "password"}
→ accessToken al
```

**2. Fault Oluştur:**
```http
POST /api/faults
Authorization: Bearer {accessToken}
{
  "elevatorId": 1,
  "faultSubject": "Motor arızası",
  "contactPerson": "John Smith",
  "description": "Motor not working"
}
```

**3. Maintenance Oluştur:**
```http
POST /api/maintenances
Authorization: Bearer {accessToken}
{
  "elevatorId": 1,
  "date": "2026-01-11",
  "description": "Motor tamiri",
  "amount": 2000.0
}
```

**4. Receipt Fişi Oluştur:**
```http
POST /api/payments
Authorization: Bearer {accessToken}
{
  "maintenanceId": 1,
  "amount": 2000.0,
  "payerName": "Building Management",
  "date": "2026-01-12"
}
```
→ Maintenance is automatically set to `isPaid=true`

**5. Fault Durumu Güncelle:**
```http
PUT /api/faults/1/status?status=TAMAMLANDI
Authorization: Bearer {accessToken}
```

---

### Senaryo 2: Filtering ve Summary

**1. Ödenmeyen Bakımları Listele:**
```http
GET /api/maintenances?paid=false
Authorization: Bearer {accessToken}
```

**2. Ocak 2026 Bakımları:**
```http
GET /api/maintenances?dateFrom=2026-01-01&dateTo=2026-01-31
Authorization: Bearer {accessToken}
```

**3. Ocak 2026 Aylık Summary:**
```http
GET /api/maintenances/summary?month=2026-01
Authorization: Bearer {accessToken}
```

**4. Açık Arızalar:**
```http
GET /api/faults?status=ACIK
Authorization: Bearer {accessToken}
```

---

## 12. Date Format

Tüm Date alanları **ISO 8601** formatında:
- **Date**: `YYYY-MM-DD` (örn: `2026-01-10`)
- **DateTime**: `YYYY-MM-DDTHH:mm:ss` (örn: `2026-01-10T20:30:00`)

---

## 13. Pagination

**Not:** Şu anda pagination yok, tüm kayıtlar dönüyor. Gelecekte eklenebilir.

---

## 14. Environment Variables (Postman)

```json
{
  "baseUrl": "http://localhost:8081/api",
  "username": "patron",
  "password": "password",
  "accessToken": "",
  "refreshToken": "",
  "userId": "",
  "elevatorId": "1",
  "maintenanceId": "1",
  "partId": "1"
}
```

---

**Son Güncelleme**: 2026-01-11
**API Version**: 1.1.0

