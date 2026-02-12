# API Endpoints - Desktop Application Guide

Bu dokümantasyon, desktop uygulaması için tüm API endpoint'lerinin request ve response örneklerini içerir.

**Base URL**: `http://localhost:8081/api`

**Önemli Notlar**:
- Tüm request'lerde `Content-Type: application/json` header'ı olmalı
- Tüm request'lerde `Accept: application/json` header'ı olmalı
- Authentication gereken endpoint'lerde `Authorization: Bearer <token>` header'ı olmalı
- Login endpoint'i dışında tüm endpoint'ler authentication gerektirir

---

## 1. Authentication Endpoints

### 1.1. Login
**Endpoint**: `POST /api/auth/login`  
**Authentication**: Gereksiz (public)

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
  "username": "patron",
  "password": "password"
}
```

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUEFUUk9OIiwidXNlcklkIjoxLCJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgxNjM2MDYsImV4cCI6MTc2ODE2NzIwNn0.SwXrJ9qSFhnoD8iPpGbAkBxRk2VfHnwDfjvRpKI8gjPD8ksqh4-lFdqgwsgMNxJNhFic4YR2g9KkCqSalw0S0A",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgxNjM2MDYsImV4cCI6MTc2ODc2ODQwNn0.m-lXAWiUo1smKq47wIq3FRwEMKMJHDmF0g--7wNi4ApFBHBm428NqrPYrU9PFsStiutOXDsphx8Pp6J5Qnzuow",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  },
  "errors": null
}
```

**Error Response (400)**:
```json
{
  "success": false,
  "message": "Login failed: Bad credentials",
  "data": null,
  "errors": null
}
```

---

### 1.2. Refresh Token
**Endpoint**: `POST /api/auth/refresh`  
**Authentication**: Gereksiz (public)

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgxNjM2MDYsImV4cCI6MTc2ODc2ODQwNn0.m-lXAWiUo1smKq47wIq3FRwEMKMJHDmF0g--7wNi4ApFBHBm428NqrPYrU9PFsStiutOXDsphx8Pp6J5Qnzuow"
}
```

**Success Response (200)**: Login response ile aynı format

---

### 1.3. Register
**Endpoint**: `POST /api/auth/register`  
**Authentication**: Gereksiz (public)

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
  "username": "newuser",
  "password": "password123",
  "role": "PERSONEL"
}
```

**Success Response (200)**: Login response ile aynı format

---

### 1.4. Logout
**Endpoint**: `POST /api/auth/logout`  
**Authentication**: Gerekli

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**: Boş (body göndermeyebilirsiniz)

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null,
  "errors": null
}
```

---

## 2. Elevators Endpoints

### 2.1. Get All Elevators
**Endpoint**: `GET /api/elevators`  
**Authentication**: Gerekli (PATRON veya PERSONEL)

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**: Yok

**Success Response (200)**:
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
      "technicalNotes": "Regular maintenance required",
      "driveType": "Hydraulic",
      "machineBrand": "Otis",
      "doorType": "Automatic",
      "installationYear": 2020,
      "serialNumber": "SN-001",
      "controlSystem": "Siemens",
      "rope": "6 ropes",
      "modernization": "2020",
      "inspectionDate": "2024-07-11",
      "expiryDate": "2025-07-11"
    }
  ],
  "errors": null
}
```

---

### 2.2. Get Elevator by ID
**Endpoint**: `GET /api/elevators/{id}`  
**Authentication**: Gerekli

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Success Response (200)**: Tek bir elevator objesi (yukarıdaki gibi)

**Error Response (400)**:
```json
{
  "success": false,
  "message": "Elevator not found",
  "data": null,
  "errors": null
}
```

---

### 2.3. Create Elevator
**Endpoint**: `POST /api/elevators`  
**Authentication**: Gerekli

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "identityNumber": "ELEV-011",
  "buildingName": "New Building",
  "address": "456 New Street",
  "elevatorNumber": "N1",
  "floorCount": 6,
  "capacity": 800,
  "speed": 1.5,
  "technicalNotes": "New installation",
  "driveType": "Traction",
  "machineBrand": "Schindler",
  "doorType": "Automatic",
  "installationYear": 2023,
  "serialNumber": "SN-011",
  "controlSystem": "Mitsubishi",
  "rope": "6 ropes",
  "modernization": null,
  "inspectionDate": "2024-01-01",
  "expiryDate": "2025-01-01"
}
```

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Elevator successfully added",
  "data": {
    "id": 11,
    "identityNumber": "ELEV-011",
    ...
  },
  "errors": null
}
```

---

### 2.4. Update Elevator
**Endpoint**: `PUT /api/elevators/{id}`  
**Authentication**: Gerekli

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**: Create ile aynı format (id hariç)

**Success Response (200)**: Güncellenmiş elevator objesi

---

### 2.5. Delete Elevator
**Endpoint**: `DELETE /api/elevators/{id}`  
**Authentication**: Gerekli

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Elevator successfully deleted",
  "data": null,
  "errors": null
}
```

---

## 3. Maintenances Endpoints

### 3.1. Get All Maintenances
**Endpoint**: `GET /api/maintenances`  
**Query Parameters**: 
- `paid` (optional): `true` veya `false`
- `dateFrom` (optional): `YYYY-MM-DD`
- `dateTo` (optional): `YYYY-MM-DD`

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Example**: `GET /api/maintenances?paid=true&dateFrom=2024-01-01&dateTo=2024-12-31`

**Success Response (200)**:
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "elevatorId": 1,
      "elevatorIdentityNumber": "ELEV-001",
      "elevatorBuildingName": "Central Business Center",
      "date": "2024-12-12",
      "description": "Monthly periodic maintenance",
      "technicianUserId": 2,
      "technicianUsername": "technician1",
      "amount": 1500.00,
      "isPaid": true,
      "paymentDate": "2024-12-07"
    }
  ],
  "errors": null
}
```

---

### 3.2. Get Maintenance by ID
**Endpoint**: `GET /api/maintenances/{id}`

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Success Response (200)**: Tek bir maintenance objesi

---

### 3.3. Create Maintenance
**Endpoint**: `POST /api/maintenances`

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "elevatorId": 1,
  "date": "2024-12-15",
  "description": "Monthly maintenance check",
  "technicianUserId": 2,
  "amount": 1500.00,
  "isPaid": false,
  "paymentDate": null
}
```

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Maintenance record successfully added",
  "data": {
    "id": 16,
    "elevatorId": 1,
    ...
  },
  "errors": null
}
```

---

### 3.4. Update Maintenance
**Endpoint**: `PUT /api/maintenances/{id}`

**Request Body**: Create ile aynı format

---

### 3.5. Delete Maintenance
**Endpoint**: `DELETE /api/maintenances/{id}`

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Maintenance record successfully deleted",
  "data": null,
  "errors": null
}
```

---

### 3.6. Get Maintenance Summary
**Endpoint**: `GET /api/maintenances/summary?month=2024-12`

**Success Response (200)**:
```json
{
  "success": true,
  "data": {
    "totalMaintenances": 5,
    "paidCount": 3,
    "unpaidCount": 2,
    "totalAmount": 7500.00,
    "paidAmount": 4500.00,
    "unpaidAmount": 3000.00
  }
}
```

---

## 4. Faults Endpoints

### 4.1. Get All Faults
**Endpoint**: `GET /api/faults`  
**Query Parameters**: 
- `status` (optional): `OPEN` veya `COMPLETED`

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Example**: `GET /api/faults?status=OPEN`

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "elevatorId": 3,
      "elevatorIdentityNumber": "ELEV-003",
      "elevatorBuildingName": "Commercial Center",
      "faultSubject": "Motor not working",
      "contactPerson": "John Smith",
      "buildingAuthorizedMessage": "Urgent intervention required",
      "description": "Elevator stuck on 2nd floor",
      "status": "OPEN",
      "createdAt": "2024-12-09T22:00:00"
    }
  ]
}
```

---

### 4.2. Get Fault by ID
**Endpoint**: `GET /api/faults/{id}`

---

### 4.3. Create Fault
**Endpoint**: `POST /api/faults`

**Request Headers**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

**Request Body**:
```json
{
  "elevatorId": 1,
  "faultSubject": "Door not closing",
  "contactPerson": "Jane Doe",
  "buildingAuthorizedMessage": "Please fix ASAP",
  "description": "Door mechanism malfunction",
  "status": "OPEN"
}
```

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Fault record successfully added",
  "data": {
    "id": 11,
    ...
  }
}
```

---

### 4.4. Update Fault
**Endpoint**: `PUT /api/faults/{id}`

**Request Body**: Create ile aynı format

---

### 4.5. Update Fault Status
**Endpoint**: `PUT /api/faults/{id}/status`

**Request Body**:
```json
{
  "status": "COMPLETED"
}
```

---

### 4.6. Delete Fault
**Endpoint**: `DELETE /api/faults/{id}`

---

## 5. Inspections Endpoints

### 5.1. Get All Inspections
**Endpoint**: `GET /api/inspections`

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "elevatorId": 1,
      "elevatorIdentityNumber": "ELEV-001",
      "elevatorBuildingName": "Central Business Center",
      "date": "2024-06-11",
      "result": "PASSED",
      "description": "All checks completed, everything is in order"
    }
  ]
}
```

---

### 5.2. Get Inspection by ID
**Endpoint**: `GET /api/inspections/{id}`

---

### 5.3. Get Inspections by Elevator ID
**Endpoint**: `GET /api/inspections/elevator/{elevatorId}`

---

### 5.4. Create Inspection
**Endpoint**: `POST /api/inspections`

**Request Body**:
```json
{
  "elevatorId": 1,
  "date": "2024-12-15",
  "result": "PASSED",
  "description": "Regular inspection completed"
}
```

---

### 5.5. Update Inspection
**Endpoint**: `PUT /api/inspections/{id}`

---

### 5.6. Delete Inspection
**Endpoint**: `DELETE /api/inspections/{id}`

---

## 6. Payment Receipts Endpoints

### 6.1. Get All Payment Receipts
**Endpoint**: `GET /api/payments`  
**Query Parameters**:
- `dateFrom` (optional): `YYYY-MM-DD`
- `dateTo` (optional): `YYYY-MM-DD`

**Example**: `GET /api/payments?dateFrom=2024-01-01&dateTo=2024-12-31`

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "maintenanceId": 1,
      "amount": 1500.00,
      "payerName": "Building Management",
      "date": "2024-12-07",
      "note": "Payment for maintenance #1",
      "createdAt": "2024-12-07T10:00:00"
    }
  ]
}
```

---

### 6.2. Get Payment Receipt by ID
**Endpoint**: `GET /api/payments/{id}`

---

### 6.3. Create Payment Receipt
**Endpoint**: `POST /api/payments`

**Request Body**:
```json
{
  "maintenanceId": 1,
  "amount": 1500.00,
  "payerName": "Building Management",
  "date": "2024-12-15",
  "note": "Payment received"
}
```

---

### 6.4. Update Payment Receipt
**Endpoint**: `PUT /api/payments/{id}`

---

### 6.5. Delete Payment Receipt
**Endpoint**: `DELETE /api/payments/{id}`

---

## 7. Parts Endpoints

### 7.1. Get All Parts
**Endpoint**: `GET /api/parts`

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Elevator Rope 8mm",
      "unitPrice": 150.00,
      "stock": 10,
      "createdAt": "2024-12-11T10:00:00"
    }
  ]
}
```

---

### 7.2. Get Part by ID
**Endpoint**: `GET /api/parts/{id}`

---

### 7.3. Create Part
**Endpoint**: `POST /api/parts`

**Request Body**:
```json
{
  "name": "New Part",
  "unitPrice": 200.00,
  "stock": 5
}
```

---

### 7.4. Update Part
**Endpoint**: `PUT /api/parts/{id}`

---

### 7.5. Delete Part
**Endpoint**: `DELETE /api/parts/{id}`

---

## 8. Dashboard Endpoint

### 8.1. Get Dashboard Summary
**Endpoint**: `GET /api/dashboard/summary`

**Success Response (200)**:
```json
{
  "success": true,
  "data": {
    "totalElevators": 10,
    "totalMaintenances": 15,
    "totalFaults": 10,
    "openFaults": 5,
    "totalInspections": 10,
    "expiredElevators": 1,
    "expiringSoonElevators": 2
  }
}
```

---

## 9. Warnings Endpoint

### 9.1. Get Warnings
**Endpoint**: `GET /api/warnings`

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "status": "EXPIRED",
      "daysLeft": -30,
      "identityNumber": "ELEV-003",
      "buildingName": "Commercial Center",
      "expiryDate": "2024-11-11"
    }
  ]
}
```

---

## 10. Users Endpoint (PATRON only)

### 10.1. Get All Users
**Endpoint**: `GET /api/users`  
**Authentication**: Sadece PATRON rolü

**Request Headers**:
```
Accept: application/json
Authorization: Bearer <token>
```

**Success Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "patron",
      "role": "PATRON",
      "active": true,
      "createdAt": "2024-12-11T10:00:00"
    }
  ]
}
```

---

## Hata Kodları ve Çözümleri

### 403 Forbidden
**Neden**: 
- Token eksik veya geçersiz
- Yetki yetersiz (ör: PERSONEL rolü users endpoint'ine erişemez)

**Çözüm**:
- Token'ı kontrol edin
- Login yapıp yeni token alın
- Rolünüzün endpoint'e erişim yetkisi olduğundan emin olun

### 401 Unauthorized
**Neden**: Token geçersiz veya süresi dolmuş

**Çözüm**:
- Yeni token alın (login endpoint'i)

### 400 Bad Request
**Neden**: 
- Request body formatı hatalı
- Zorunlu alanlar eksik
- Validation hatası

**Çözüm**:
- Request body'yi kontrol edin
- Tüm zorunlu alanların dolu olduğundan emin olun
- Date formatlarının `YYYY-MM-DD` olduğundan emin olun

### 500 Internal Server Error
**Neden**: 
- Server-side hata
- Database bağlantı sorunu

**Çözüm**:
- Backend loglarını kontrol edin
- Database'in çalıştığından emin olun

---

## Örnek Desktop Uygulaması Kodu (Java)

```java
// HTTP Client örneği
HttpClient client = HttpClient.newHttpClient();

// Login
HttpRequest loginRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8081/api/auth/login"))
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"username\":\"patron\",\"password\":\"password\"}"
    ))
    .build();

HttpResponse<String> loginResponse = client.send(loginRequest, 
    HttpResponse.BodyHandlers.ofString());

// Token'ı parse et
JSONObject loginJson = new JSONObject(loginResponse.body());
String token = loginJson.getJSONObject("data").getString("accessToken");

// Authenticated request
HttpRequest apiRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8081/api/elevators"))
    .header("Accept", "application/json")
    .header("Authorization", "Bearer " + token)
    .GET()
    .build();

HttpResponse<String> apiResponse = client.send(apiRequest, 
    HttpResponse.BodyHandlers.ofString());
```

---

## Önemli Notlar

1. **Tüm tarihler ISO 8601 formatında**: `YYYY-MM-DD` veya `YYYY-MM-DDTHH:mm:ss`
2. **Decimal sayılar**: `.` (nokta) ile, örnek: `1500.00`
3. **Boolean değerler**: `true` veya `false` (küçük harf)
4. **Enum değerler**: Büyük harf, örnek: `OPEN`, `COMPLETED`, `PATRON`, `PERSONEL`
5. **Token kullanımı**: Token'ı `Authorization: Bearer <token>` formatında gönderin
6. **Content-Type**: POST/PUT request'lerde mutlaka `application/json` olmalı
7. **Accept**: Tüm request'lerde `application/json` önerilir

