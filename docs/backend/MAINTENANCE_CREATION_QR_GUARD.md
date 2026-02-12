# Maintenance Creation QR Guard - Implementation Guide

## 📋 Overview

Maintenance creation endpoint (`POST /api/maintenances`) now requires QR code validation for TECHNICIAN role, while ADMIN can bypass or use QR optionally.

**Important:** This does NOT affect maintenance planning endpoints (`/api/maintenance-plans`).

---

## 🔒 Security Rules

### TECHNICIAN (PERSONEL)
- ✅ QR token **REQUIRED**
- ❌ Cannot create maintenance without valid QR
- ✅ QR must match elevator ID
- ❌ Cannot use `ADMIN_BYPASS`

### ADMIN (PATRON/ADMIN)
- ✅ Can bypass QR (omit `qrToken` or use `ADMIN_BYPASS`)
- ✅ Can also use QR if provided (validated)
- ✅ Remote creation allowed

---

## 🔧 Backend Implementation

### 1. MaintenanceDto.java

Added `qrToken` field:

```java
/**
 * QR token for maintenance creation validation
 * Required for TECHNICIAN (PERSONEL) role
 * Optional for ADMIN role (can use "ADMIN_BYPASS" or omit)
 * Format: "e={elevatorCode}&s={signature}" or full URL
 */
private String qrToken;
```

### 2. ElevatorQrService.java

Added `validateQrToken()` method:

```java
/**
 * Parse QR token and validate
 * QR token format: "e={elevatorCode}&s={signature}" or full URL
 * 
 * @param qrToken QR token string
 * @param expectedElevatorId Expected elevator ID to match
 * @return Validation result with elevator ID if valid
 */
public QrValidationResult validateQrToken(String qrToken, Long expectedElevatorId)
```

**QR Token Formats Supported:**
- Full URL: `https://app.saraasansor.com/qr-start?e=ELEV-001&s=abc123...`
- Query string: `e=ELEV-001&s=abc123...`
- Admin bypass: `ADMIN_BYPASS` (ADMIN only)

### 3. MaintenanceController.java

Added QR validation guard in `createMaintenance()`:

```java
// Get current user and role
User currentUser = getCurrentUser();
User.Role userRole = currentUser.getRole();

// QR Validation Guard
if (userRole == User.Role.PERSONEL) {
    // TECHNICIAN: QR token is REQUIRED
    if (dto.getQrToken() == null || dto.getQrToken().trim().isEmpty()) {
        return ResponseEntity.status(403)
            .body(ApiResponse.error("QR token is required for TECHNICIAN role"));
    }
    
    // Validate QR token
    QrValidationResult validation = elevatorQrService.validateQrToken(
        dto.getQrToken(), 
        dto.getElevatorId()
    );
    
    if (!validation.isValid()) {
        return ResponseEntity.status(403)
            .body(ApiResponse.error("Invalid or expired QR token: " + validation.getError()));
    }
    
    if (validation.isAdminBypass()) {
        return ResponseEntity.status(403)
            .body(ApiResponse.error("TECHNICIAN role cannot bypass QR validation"));
    }
    
} else if (userRole == User.Role.ADMIN || userRole == User.Role.PATRON) {
    // ADMIN/PATRON: QR token is optional
    if (dto.getQrToken() != null && !dto.getQrToken().trim().isEmpty() && 
        !"ADMIN_BYPASS".equals(dto.getQrToken())) {
        
        // Validate QR if provided
        QrValidationResult validation = elevatorQrService.validateQrToken(
            dto.getQrToken(), 
            dto.getElevatorId()
        );
        
        if (!validation.isValid()) {
            return ResponseEntity.status(403)
                .body(ApiResponse.error("Invalid QR token: " + validation.getError()));
        }
    }
}
```

---

## 🎨 Frontend Flow

### 1. User Clicks "Yeni Bakım Ekle"

```typescript
// User clicks button
const handleCreateMaintenance = () => {
  // Open QR validation modal first
  setShowQrModal(true);
};
```

### 2. QR Validation Modal

```typescript
// components/MaintenanceQrModal.tsx
const MaintenanceQrModal = ({ 
  elevatorId, 
  onQrValidated, 
  onClose 
}) => {
  const [qrToken, setQrToken] = useState<string | null>(null);
  const [validated, setValidated] = useState(false);
  
  const handleQrScan = async (scannedData: string) => {
    try {
      // Parse QR URL: https://app.saraasansor.com/qr-start?e={code}&s={signature}
      const url = new URL(scannedData);
      const elevatorCode = url.searchParams.get('e');
      const signature = url.searchParams.get('s');
      
      // Validate QR with backend
      const response = await apiClient.get('/qr/validate', {
        params: { e: elevatorCode, s: signature }
      });
      
      if (response.data.success && response.data.data.valid) {
        // Verify elevator match
        if (response.data.data.elevatorId !== elevatorId) {
          throw new Error('QR code is for a different elevator');
        }
        
        // Store QR token for maintenance creation
        setQrToken(scannedData);
        setValidated(true);
        
        // Close QR modal → Open maintenance form
        onClose();
        onQrValidated(scannedData);
      }
    } catch (err) {
      setError(err.message);
    }
  };
  
  return (
    <Modal>
      <QrScanner onScanSuccess={handleQrScan} />
      <ManualQrInput onSubmit={handleQrScan} />
      {validated && <SuccessMessage />}
    </Modal>
  );
};
```

### 3. Maintenance Create Modal

```typescript
// components/MaintenanceCreateModal.tsx
const MaintenanceCreateModal = ({ 
  elevatorId, 
  qrToken, // Passed from QR modal
  onClose 
}) => {
  const [formData, setFormData] = useState({
    elevatorId,
    date: new Date().toISOString().split('T')[0],
    labelType: 'GREEN',
    description: '',
    amount: 0,
    qrToken, // Include QR token in form data
  });
  
  const handleSubmit = async () => {
    const formDataObj = new FormData();
    
    // Add JSON data
    formDataObj.append('data', JSON.stringify({
      elevatorId: formData.elevatorId,
      date: formData.date,
      labelType: formData.labelType,
      description: formData.description,
      amount: formData.amount,
      qrToken: formData.qrToken, // Include QR token
    }));
    
    // Add photos
    photos.forEach(photo => {
      formDataObj.append('photos', photo);
    });
    
    // Submit to backend
    await apiClient.post('/maintenances', formDataObj, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  };
  
  return (
    <Modal>
      <Form onSubmit={handleSubmit}>
        {/* Form fields */}
      </Form>
    </Modal>
  );
};
```

### 4. Complete Flow Component

```typescript
// pages/MaintenanceList.tsx
const MaintenanceListPage = () => {
  const [showQrModal, setShowQrModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedElevatorId, setSelectedElevatorId] = useState<number | null>(null);
  const [qrToken, setQrToken] = useState<string | null>(null);
  
  const handleCreateClick = (elevatorId: number) => {
    setSelectedElevatorId(elevatorId);
    setShowQrModal(true);
  };
  
  const handleQrValidated = (token: string) => {
    setQrToken(token);
    setShowQrModal(false);
    setShowCreateModal(true);
  };
  
  return (
    <>
      <button onClick={() => handleCreateClick(elevator.id)}>
        Yeni Bakım Ekle
      </button>
      
      {showQrModal && selectedElevatorId && (
        <MaintenanceQrModal
          elevatorId={selectedElevatorId}
          onQrValidated={handleQrValidated}
          onClose={() => setShowQrModal(false)}
        />
      )}
      
      {showCreateModal && selectedElevatorId && qrToken && (
        <MaintenanceCreateModal
          elevatorId={selectedElevatorId}
          qrToken={qrToken}
          onClose={() => {
            setShowCreateModal(false);
            setQrToken(null);
          }}
        />
      )}
    </>
  );
};
```

---

## 📡 API Contract

### POST `/api/maintenances`

**Request (multipart/form-data):**
```
data: {
  "elevatorId": 1,
  "date": "2026-01-15",
  "labelType": "GREEN",
  "description": "Test maintenance",
  "amount": 1000,
  "qrToken": "e=ELEV-001&s=abc123..." // Required for TECHNICIAN
}
photos: [File, File, File, File] // Minimum 4 photos
```

**Response (Success - 201):**
```json
{
  "success": true,
  "message": "Maintenance created successfully with 4 photos",
  "data": {
    "id": 123,
    "elevatorId": 1,
    "date": "2026-01-15",
    ...
  },
  "errors": null
}
```

**Response (Error - 403 Forbidden):**
```json
{
  "success": false,
  "message": "QR token is required for TECHNICIAN role",
  "data": null,
  "errors": null
}
```

**Response (Error - 403 Invalid QR):**
```json
{
  "success": false,
  "message": "Invalid or expired QR token: Invalid QR signature",
  "data": null,
  "errors": null
}
```

---

## 🧪 Testing

### Test Case 1: TECHNICIAN with valid QR
```bash
POST /api/maintenances
Headers: Authorization: Bearer {technician_token}
Body (multipart/form-data):
  data: {
    "elevatorId": 1,
    "date": "2026-01-15",
    "labelType": "GREEN",
    "description": "Test maintenance",
    "amount": 1000,
    "qrToken": "e=ELEV-001&s=valid_signature"
  }
  photos: [4 files]
Expected: 201 Created
```

### Test Case 2: TECHNICIAN without QR
```bash
POST /api/maintenances
Headers: Authorization: Bearer {technician_token}
Body (multipart/form-data):
  data: {
    "elevatorId": 1,
    "date": "2026-01-15",
    "labelType": "GREEN",
    "description": "Test maintenance",
    "amount": 1000
    // qrToken missing
  }
  photos: [4 files]
Expected: 403 Forbidden - "QR token is required for TECHNICIAN role"
```

### Test Case 3: ADMIN without QR
```bash
POST /api/maintenances
Headers: Authorization: Bearer {admin_token}
Body (multipart/form-data):
  data: {
    "elevatorId": 1,
    "date": "2026-01-15",
    "labelType": "GREEN",
    "description": "Test maintenance",
    "amount": 1000
    // qrToken missing (allowed for ADMIN)
  }
  photos: [4 files]
Expected: 201 Created
```

### Test Case 4: ADMIN with QR
```bash
POST /api/maintenances
Headers: Authorization: Bearer {admin_token}
Body (multipart/form-data):
  data: {
    "elevatorId": 1,
    "date": "2026-01-15",
    "labelType": "GREEN",
    "description": "Test maintenance",
    "amount": 1000,
    "qrToken": "e=ELEV-001&s=valid_signature"
  }
  photos: [4 files]
Expected: 201 Created (QR validated but not required)
```

---

## ⚠️ Important Notes

### 1. Planning Endpoints Unchanged
- ✅ `POST /api/maintenance-plans` → **No QR required**
- ✅ `PUT /api/maintenance-plans/{id}` → **No QR required**
- ✅ `PATCH /api/maintenance-plans/{id}/reschedule` → **No QR required**
- ✅ `DELETE /api/maintenance-plans/{id}` → **No QR required**

### 2. Only Maintenance Creation Requires QR
- ✅ `POST /api/maintenances` → **QR required for TECHNICIAN**

### 3. QR Token Format
- Full URL: `https://app.saraasansor.com/qr-start?e={code}&s={signature}`
- Query string: `e={code}&s={signature}`
- Admin bypass: `ADMIN_BYPASS` (ADMIN only)

### 4. Security Flow
```
1. Frontend: User clicks "Yeni Bakım Ekle"
   ↓
2. Frontend: QR Validation Dialog opens
   ↓
3. User scans/enters QR code
   ↓
4. Frontend: Validates QR with /api/qr/validate
   ↓
5. Frontend: QR validated → Opens MaintenanceFormDialog
   ↓
6. User fills form and submits
   ↓
7. Frontend: POST /api/maintenances with qrToken
   ↓
8. Backend: Validates QR token based on role
   ↓
9. Backend: Creates maintenance record
   ↓
10. Frontend: Shows success message
```

---

## 🔍 Error Handling

### Common Errors

1. **403 Forbidden - No QR Token (TECHNICIAN)**
   - **Cause:** TECHNICIAN attempted to create maintenance without QR
   - **Fix:** Frontend must show QR modal first

2. **403 Forbidden - Invalid QR Signature**
   - **Cause:** QR signature doesn't match
   - **Fix:** User must scan correct QR code

3. **403 Forbidden - Elevator Mismatch**
   - **Cause:** QR code is for different elevator
   - **Fix:** User must scan QR code matching the selected elevator

4. **403 Forbidden - ADMIN_BYPASS (TECHNICIAN)**
   - **Cause:** TECHNICIAN tried to use `ADMIN_BYPASS`
   - **Fix:** TECHNICIAN cannot bypass QR validation

---

## ✅ Checklist

- [x] MaintenanceDto has `qrToken` field
- [x] ElevatorQrService has `validateQrToken()` method
- [x] MaintenanceController validates QR based on role
- [x] TECHNICIAN cannot create without QR
- [x] ADMIN can bypass QR
- [x] QR token format parsing (URL and query string)
- [x] Elevator ID matching validation
- [x] Error responses with clear messages
- [x] Logging for debugging
- [x] Planning endpoints unchanged

---

**End of Documentation**
