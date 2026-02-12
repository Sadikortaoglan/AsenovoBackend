# QR-Based Maintenance Start System - Complete Architecture

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Security Architecture](#security-architecture)
3. [Backend Implementation](#backend-implementation)
4. [Frontend Implementation](#frontend-implementation)
5. [API Contract](#api-contract)
6. [Business Rules](#business-rules)
7. [Audit Logging](#audit-logging)

---

## 🎯 System Overview

### Roles
- **TECHNICIAN** (`PERSONEL`): Must scan QR to start maintenance
- **ADMIN**: Can start maintenance remotely without QR (or with QR)

### Flow
1. **TECHNICIAN Flow:**
   - Scan QR code → Validate → Start maintenance → Open "Yeni Bakım Ekle" modal
   
2. **ADMIN Flow:**
   - Option 1: Scan QR (same as TECHNICIAN)
   - Option 2: Click "Uzaktan Başlat" → Start without QR → Open "Yeni Bakım Ekle" modal

---

## 🔒 Security Architecture

### QR Token Structure

QR codes are **NOT static text**. They contain:
- `elevatorId` (public code: `identityNumber` or `elevatorNumber`)
- `signature` (HMAC-SHA256)
- `expirationTimestamp` (implicit in token TTL)

### Token Generation Flow

```
1. Static QR Code (printed on elevator):
   Format: https://yourdomain.com/qr?e=ELEV-002&sig=<HMAC_SIGNATURE>
   
2. Frontend scans QR → Extracts elevatorPublicCode + signature
   
3. POST /api/qr/issue-session-token
   Request: {
     elevatorPublicCode: "ELEV-002",
     sig: "<HMAC_SIGNATURE>",
     deviceMeta: {...}
   }
   
4. Backend validates signature → Issues short-lived JWT token (TTL: 3 minutes)
   
5. Response: {
     qrToken: "<JWT_TOKEN>",
     expiresAt: "2026-02-15T10:03:00",
     elevatorId: 123
   }
   
6. Frontend stores token → Uses for POST /api/maintenance-plans/{id}/start
```

### HMAC Signature Generation

**Backend (`QrProofService.generateSignature`):**
```java
Mac mac = Mac.getInstance("HmacSHA256");
SecretKeySpec secretKeySpec = new SecretKeySpec(
    qrSecretKey.getBytes(StandardCharsets.UTF_8), 
    "HmacSHA256"
);
mac.init(secretKeySpec);
byte[] signatureBytes = mac.doFinal(elevatorPublicCode.getBytes(StandardCharsets.UTF_8));
return Base64.getEncoder().encodeToString(signatureBytes);
```

**Frontend (for QR generation):**
```javascript
import CryptoJS from 'crypto-js';

function generateQrSignature(elevatorPublicCode, secretKey) {
  const hmac = CryptoJS.HmacSHA256(elevatorPublicCode, secretKey);
  return CryptoJS.enc.Base64.stringify(hmac);
}

// QR URL format
const qrUrl = `https://yourdomain.com/qr?e=${elevatorCode}&sig=${signature}`;
```

### Token Validation

**Backend validates:**
1. ✅ Signature matches (HMAC verification)
2. ✅ Token is not expired (TTL: 3 minutes)
3. ✅ Token is not already used (`usedAt IS NULL`)
4. ✅ Token matches elevatorId of maintenance plan

**If invalid → HTTP 403 Forbidden**

---

## 🔧 Backend Implementation

### 1. Entity Updates

#### `User.java`
```java
public enum Role {
    PATRON, PERSONEL, ADMIN  // ADMIN added
}
```

#### `MaintenancePlan.java`
```java
// Audit fields for maintenance start
@Column(name = "started_remotely", nullable = false)
private Boolean startedRemotely = false;

@Column(name = "started_by_role", length = 50)
private String startedByRole; // TECHNICIAN, ADMIN

@Column(name = "started_at")
private LocalDateTime startedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "started_by_user_id")
private User startedBy;

@Column(name = "started_from_ip", length = 45)
private String startedFromIp; // IPv4 or IPv6
```

### 2. DTO Updates

#### `StartMaintenancePlanRequest.java`
```java
public class StartMaintenancePlanRequest {
    /**
     * QR token from scanned QR code
     * Required for TECHNICIAN role, optional for ADMIN when remoteStart = true
     */
    private String qrToken;
    
    /**
     * Remote start flag
     * true = ADMIN can start without QR (remote start)
     * false = Normal start with QR validation
     */
    private Boolean remoteStart = false;
}
```

### 3. Service Logic

#### `MaintenancePlanService.startPlan()`

**Business Rules:**
- **TECHNICIAN (`PERSONEL`):** QR token is **REQUIRED**
- **ADMIN:** 
  - If `remoteStart = true` → No QR required
  - If `remoteStart = false` → QR token is required (same as TECHNICIAN)

**Validation:**
1. Plan status must be `PLANNED`
2. Role-based QR validation
3. QR token must match elevatorId (if provided)
4. Audit logging (startedRemotely, startedByRole, startedAt, startedByUserId, ipAddress)

**Code:**
```java
public MaintenancePlanResponseDto startPlan(Long id, String qrToken, Boolean remoteStart, String ipAddress) {
    MaintenancePlan plan = getPlanById(id);
    
    // Validation: Only PLANNED can be started
    if (plan.getStatus() != MaintenancePlan.PlanStatus.PLANNED) {
        throw new RuntimeException("Only PLANNED maintenance plans can be started");
    }
    
    User currentUser = getCurrentUser();
    User.Role userRole = currentUser.getRole();
    
    com.saraasansor.api.model.QrProof qrProof = null;
    Boolean isRemoteStart = false;
    
    // Role-based validation
    if (userRole == User.Role.ADMIN && remoteStart != null && remoteStart) {
        // ADMIN remote start: No QR required
        isRemoteStart = true;
    } else {
        // TECHNICIAN or ADMIN with QR: QR token is REQUIRED
        if (qrToken == null || qrToken.trim().isEmpty()) {
            throw new RuntimeException("QR token is required. Role: " + userRole);
        }
        
        // Validate and use QR token
        qrProof = qrProofService.validateAndUseToken(qrToken, currentUser.getId());
        
        // Verify QR proof is for the same elevator
        if (!qrProof.getElevator().getId().equals(plan.getElevator().getId())) {
            throw new RuntimeException("QR token is for a different elevator");
        }
    }
    
    // Update plan status
    plan.setStatus(MaintenancePlan.PlanStatus.IN_PROGRESS);
    plan.setQrProof(qrProof);
    
    // Audit logging
    plan.setStartedRemotely(isRemoteStart);
    plan.setStartedByRole(userRole.name());
    plan.setStartedAt(LocalDateTime.now());
    plan.setStartedBy(currentUser);
    plan.setStartedFromIp(ipAddress);
    plan.setUpdatedBy(currentUser);
    plan.setUpdatedAt(LocalDateTime.now());
    
    return MaintenancePlanResponseDto.fromEntity(planRepository.save(plan));
}
```

### 4. Controller Updates

#### `MaintenancePlanController.startPlan()`

```java
@PostMapping("/{id}/start")
public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> startPlan(
        @PathVariable Long id,
        @RequestBody StartMaintenancePlanRequest request,
        HttpServletRequest httpRequest) {
    try {
        // Extract IP address
        String ipAddress = httpRequest.getRemoteAddr();
        
        // Handle X-Forwarded-For header (if behind proxy)
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            ipAddress = xForwardedFor.split(",")[0].trim();
        }
        
        MaintenancePlanResponseDto started = planService.startPlan(
            id, 
            request.getQrToken(), 
            request.getRemoteStart(), 
            ipAddress
        );
        
        return ResponseEntity.ok(ApiResponse.success("Maintenance started successfully", started));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }
}
```

---

## 🎨 Frontend Implementation

### 1. QR Modal Component

```typescript
// components/MaintenanceStartModal.tsx
import React, { useState, useEffect } from 'react';
import { useUser } from '@/hooks/useUser';
import { startMaintenancePlan } from '@/services/maintenanceService';
import QrScanner from '@/components/QrScanner';

interface MaintenanceStartModalProps {
  planId: number;
  elevatorId: number;
  onSuccess: () => void;
  onClose: () => void;
}

export const MaintenanceStartModal: React.FC<MaintenanceStartModalProps> = ({
  planId,
  elevatorId,
  onSuccess,
  onClose,
}) => {
  const { user } = useUser();
  const [qrToken, setQrToken] = useState<string | null>(null);
  const [qrValidated, setQrValidated] = useState(false);
  const [manualQrInput, setManualQrInput] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState(false);

  // Detect mobile device
  useEffect(() => {
    const checkMobile = () => {
      const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera;
      const isMobileDevice = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent.toLowerCase());
      setIsMobile(isMobileDevice);
    };
    checkMobile();
  }, []);

  const isAdmin = user?.role === 'ADMIN';
  const isTechnician = user?.role === 'PERSONEL';

  // Handle QR scan success
  const handleQrScanSuccess = async (scannedData: string) => {
    try {
      setError(null);
      
      // Parse QR data (format: https://yourdomain.com/qr?e=ELEV-002&sig=<signature>)
      const url = new URL(scannedData);
      const elevatorCode = url.searchParams.get('e');
      const signature = url.searchParams.get('sig');
      
      if (!elevatorCode || !signature) {
        throw new Error('Invalid QR code format');
      }
      
      // Issue session token
      const tokenResponse = await issueSessionToken(elevatorCode, signature);
      
      // Validate token matches elevator
      if (tokenResponse.elevatorId !== elevatorId) {
        throw new Error('QR code is for a different elevator');
      }
      
      setQrToken(tokenResponse.qrToken);
      setQrValidated(true);
    } catch (err: any) {
      setError(err.message || 'QR validation failed');
      setQrValidated(false);
    }
  };

  // Handle manual QR input
  const handleManualQrSubmit = async () => {
    if (!manualQrInput.trim()) {
      setError('Please enter QR code');
      return;
    }
    await handleQrScanSuccess(manualQrInput);
  };

  // Handle remote start (ADMIN only)
  const handleRemoteStart = async () => {
    try {
      setError(null);
      
      await startMaintenancePlan(planId, {
        qrToken: null,
        remoteStart: true,
      });
      
      // Success: Close QR modal → Open maintenance create modal
      onClose();
      onSuccess(); // Opens "Yeni Bakım Ekle" modal
    } catch (err: any) {
      setError(err.message || 'Remote start failed');
    }
  };

  // Handle start with QR
  const handleStartWithQr = async () => {
    if (!qrToken) {
      setError('QR token is required');
      return;
    }
    
    try {
      setError(null);
      
      await startMaintenancePlan(planId, {
        qrToken,
        remoteStart: false,
      });
      
      // Success: Close QR modal → Open maintenance create modal
      onClose();
      onSuccess(); // Opens "Yeni Bakım Ekle" modal
    } catch (err: any) {
      setError(err.message || 'Start failed');
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h2 className="text-xl font-bold mb-4">Bakım Başlat</h2>
        
        {/* Error message */}
        {error && (
          <div className="mb-4 p-3 bg-red-100 text-red-700 rounded">
            {error}
          </div>
        )}
        
        {/* ADMIN: Remote Start Button */}
        {isAdmin && (
          <div className="mb-4">
            <button
              onClick={handleRemoteStart}
              className="w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700"
            >
              Uzaktan Başlat (QR Gerekmez)
            </button>
          </div>
        )}
        
        {/* QR Scanner (Mobile only) */}
        {isMobile && (
          <div className="mb-4">
            <QrScanner
              onScanSuccess={handleQrScanSuccess}
              onError={(err) => setError(err.message)}
            />
          </div>
        )}
        
        {/* Manual QR Input (Desktop or fallback) */}
        <div className="mb-4">
          <label className="block text-sm font-medium mb-2">
            QR Kodu (Manuel Giriş)
          </label>
          <input
            type="text"
            value={manualQrInput}
            onChange={(e) => setManualQrInput(e.target.value)}
            placeholder="QR kodunu buraya yapıştırın"
            className="w-full border rounded px-3 py-2"
          />
          <button
            onClick={handleManualQrSubmit}
            className="mt-2 w-full bg-gray-600 text-white py-2 px-4 rounded hover:bg-gray-700"
          >
            QR Doğrula
          </button>
        </div>
        
        {/* QR Validation Status */}
        {qrValidated && (
          <div className="mb-4 p-3 bg-green-100 text-green-700 rounded">
            ✅ QR kodu doğrulandı
          </div>
        )}
        
        {/* Start Button (Disabled until QR validated for TECHNICIAN) */}
        <button
          onClick={handleStartWithQr}
          disabled={isTechnician && !qrValidated}
          className={`w-full py-2 px-4 rounded ${
            isTechnician && !qrValidated
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-green-600 hover:bg-green-700 text-white'
          }`}
        >
          {isTechnician && !qrValidated
            ? 'QR Kodu Gerekli'
            : 'Bakımı Başlat'}
        </button>
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="mt-2 w-full bg-gray-300 text-gray-700 py-2 px-4 rounded hover:bg-gray-400"
        >
          İptal
        </button>
      </div>
    </div>
  );
};
```

### 2. QR Scanner Component (Mobile)

```typescript
// components/QrScanner.tsx
import React, { useEffect, useRef } from 'react';
import QrScannerLib from 'qr-scanner';

interface QrScannerProps {
  onScanSuccess: (data: string) => void;
  onError: (error: Error) => void;
}

export const QrScanner: React.FC<QrScannerProps> = ({ onScanSuccess, onError }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const qrScannerRef = useRef<QrScannerLib | null>(null);

  useEffect(() => {
    if (!videoRef.current) return;

    const qrScanner = new QrScannerLib(
      videoRef.current,
      (result) => {
        onScanSuccess(result.data);
        qrScanner.stop();
      },
      {
        onDecodeError: (error) => {
          // Ignore decode errors (normal during scanning)
        },
      }
    );

    qrScannerRef.current = qrScanner;

    // Start scanning
    qrScanner.start().catch((err) => {
      onError(new Error('Camera access denied or not available'));
    });

    return () => {
      qrScanner.stop();
      qrScanner.destroy();
    };
  }, [onScanSuccess, onError]);

  return (
    <div className="relative">
      <video
        ref={videoRef}
        className="w-full h-64 bg-black rounded"
        playsInline
      />
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="border-2 border-white border-dashed w-48 h-48 rounded" />
      </div>
    </div>
  );
};
```

### 3. API Service

```typescript
// services/maintenanceService.ts
import apiClient from '@/config/apiClient';

export interface StartMaintenancePlanRequest {
  qrToken?: string | null;
  remoteStart?: boolean;
}

export interface QrTokenResponse {
  qrToken: string;
  expiresAt: string;
  elevatorId: number;
}

// Issue session token from QR scan
export async function issueSessionToken(
  elevatorPublicCode: string,
  signature: string
): Promise<QrTokenResponse> {
  const response = await apiClient.post<ApiResponse<QrTokenResponse>>(
    '/qr/issue-session-token',
    {
      elevatorPublicCode,
      sig: signature,
      deviceMeta: {
        userAgent: navigator.userAgent,
        platform: navigator.platform,
      },
    }
  );
  return response.data.data;
}

// Start maintenance plan
export async function startMaintenancePlan(
  planId: number,
  request: StartMaintenancePlanRequest
): Promise<void> {
  await apiClient.post(`/maintenance-plans/${planId}/start`, request);
}
```

### 4. Usage in Maintenance List

```typescript
// pages/MaintenanceList.tsx
const [showStartModal, setShowStartModal] = useState(false);
const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
const [showCreateModal, setShowCreateModal] = useState(false);

const handleStartClick = (planId: number) => {
  setSelectedPlanId(planId);
  setShowStartModal(true);
};

const handleStartSuccess = () => {
  // Close start modal, open create modal
  setShowStartModal(false);
  setShowCreateModal(true);
  // Pre-fill elevatorId and date in create modal
};

return (
  <>
    {/* Start Button */}
    <button onClick={() => handleStartClick(plan.id)}>
      Bakımı Başlat
    </button>
    
    {/* QR Start Modal */}
    {showStartModal && selectedPlanId && (
      <MaintenanceStartModal
        planId={selectedPlanId}
        elevatorId={plan.elevatorId}
        onSuccess={handleStartSuccess}
        onClose={() => setShowStartModal(false)}
      />
    )}
    
    {/* Create Maintenance Modal */}
    {showCreateModal && (
      <MaintenanceCreateModal
        elevatorId={plan.elevatorId}
        plannedDate={plan.plannedDate}
        onClose={() => setShowCreateModal(false)}
      />
    )}
  </>
);
```

---

## 📡 API Contract

### POST `/api/maintenance-plans/{id}/start`

**Request:**
```json
{
  "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // Optional for ADMIN remote start
  "remoteStart": false  // true = ADMIN remote start (no QR)
}
```

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "Maintenance started successfully",
  "data": {
    "id": 123,
    "status": "IN_PROGRESS",
    "startedRemotely": false,
    "startedByRole": "PERSONEL",
    "startedAt": "2026-02-15T10:00:00",
    "startedFromIp": "192.168.1.100",
    ...
  },
  "errors": null
}
```

**Response (Error - 400):**
```json
{
  "success": false,
  "message": "QR token is required. Role: PERSONEL",
  "data": null,
  "errors": null
}
```

**Response (Error - 403):**
```json
{
  "success": false,
  "message": "Invalid or expired QR token",
  "data": null,
  "errors": null
}
```

---

## 📋 Business Rules

### Rule 1: TECHNICIAN QR Requirement
- **TECHNICIAN (`PERSONEL`) role:** QR token is **MANDATORY**
- "Start" button is **disabled** until QR validation succeeds
- QR must match `elevatorId` of maintenance plan

### Rule 2: ADMIN Remote Start
- **ADMIN role:** Can start without QR if `remoteStart = true`
- "Uzaktan Başlat" button is **only visible** for ADMIN
- Audit log: `startedRemotely = true`, `startedByRole = "ADMIN"`

### Rule 3: Desktop Behavior
- Camera button is **only visible** on mobile devices
- Desktop: Manual QR input only
- Mobile: Camera opens automatically

### Rule 4: State Transition
- After successful start:
  1. Close QR modal
  2. Open "Yeni Bakım Ekle" modal
  3. Pre-fill `elevatorId` and `plannedDate`

---

## 📊 Audit Logging

### Database Fields

| Field | Type | Description |
|-------|------|-------------|
| `started_remotely` | BOOLEAN | `true` if ADMIN remote start, `false` if QR start |
| `started_by_role` | VARCHAR(50) | `"PERSONEL"` or `"ADMIN"` |
| `started_at` | TIMESTAMP | When maintenance was started |
| `started_by_user_id` | BIGINT | FK to `users.id` |
| `started_from_ip` | VARCHAR(45) | IP address (IPv4 or IPv6) |

### Query Examples

**Find all remote starts:**
```sql
SELECT * FROM maintenance_plans 
WHERE started_remotely = true 
ORDER BY started_at DESC;
```

**Find starts by role:**
```sql
SELECT * FROM maintenance_plans 
WHERE started_by_role = 'ADMIN' 
ORDER BY started_at DESC;
```

**Find starts by IP:**
```sql
SELECT * FROM maintenance_plans 
WHERE started_from_ip = '192.168.1.100';
```

---

## 🔐 Security Best Practices

1. **QR Secret Key:** Store in `application.yml` (environment variable in production)
   ```yaml
   app:
     qr:
       secret-key: ${QR_SECRET_KEY:default-secret-key-change-in-production}
       token-ttl-minutes: 3
   ```

2. **Token TTL:** Short-lived tokens (3 minutes) prevent replay attacks

3. **One-Time Use:** Tokens are marked as `used` after first use

4. **IP Logging:** Track IP addresses for audit and security monitoring

5. **Role-Based Access:** Strict role validation prevents privilege escalation

6. **Elevator Matching:** QR token must match maintenance plan's elevator

---

## ✅ Testing Checklist

- [ ] TECHNICIAN cannot start without QR
- [ ] ADMIN can start remotely (no QR)
- [ ] ADMIN can start with QR (same as TECHNICIAN)
- [ ] QR validation fails for wrong elevator
- [ ] QR validation fails for expired token
- [ ] QR validation fails for already-used token
- [ ] Desktop shows manual input only
- [ ] Mobile shows camera scanner
- [ ] "Start" button disabled until QR validated (TECHNICIAN)
- [ ] Audit fields are correctly saved
- [ ] IP address is correctly logged
- [ ] State transition: QR modal → Create modal

---

## 🚀 Deployment Notes

1. **Database Migration:** Run `V9__add_admin_role_and_maintenance_start_audit.sql`
2. **Environment Variables:** Set `QR_SECRET_KEY` in production
3. **QR Code Generation:** Generate static QR codes for each elevator
4. **Frontend Dependencies:** Install `qr-scanner` package
5. **Camera Permissions:** Request camera access on mobile devices

---

**End of Documentation**
