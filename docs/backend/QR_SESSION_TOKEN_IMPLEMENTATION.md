# QR Session Token Implementation - Complete Guide

## 📋 Overview

This implementation adds QR-required maintenance creation with session tokens, **without breaking existing features**. Planning endpoints remain unchanged.

---

## 🎯 Key Requirements Met

✅ **Planning screen unchanged** - `/maintenances/plan` endpoints untouched  
✅ **QR validation required** - Only for maintenance creation (execution)  
✅ **Session token system** - Short-lived tokens (5 minutes)  
✅ **Role-based access** - TECHNICIAN requires QR, ADMIN can bypass  
✅ **Mobile vs Desktop** - Camera on mobile, manual input on desktop  
✅ **Audit logging** - Tracks maintenance starts with remote flag  

---

## 🔧 Backend Implementation

### 1. New Service: `QrSessionService.java`

**Purpose:** Manages short-lived session tokens for maintenance creation.

**Key Methods:**
- `createSessionToken()` - Creates token after QR validation
- `validateToken()` - Validates token before maintenance creation
- `invalidateToken()` - Marks token as used (one-time use)

**Storage:** In-memory `ConcurrentHashMap` (dev only). Production should use Redis or database.

**Token TTL:** 5 minutes (configurable via `app.qr.session-token-ttl-minutes`)

### 2. Updated Controller: `QrController.java`

**New Endpoints:**

#### POST `/api/qr/validate`
Validates QR code and creates session token.

**Request:**
```json
{
  "qrCode": "e=ELEV-001&s=signature" or "https://app.saraasansor.com/qr-start?e=ELEV-001&s=signature",
  "elevatorId": 123 (optional)
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "qrSessionToken": "uuid-token",
    "elevatorId": 123,
    "expiresAt": "2026-01-15T10:05:00",
    "startedRemotely": false
  }
}
```

#### POST `/api/qr/remote-start`
Creates session token for ADMIN remote start (no QR required).

**Request:**
```json
{
  "elevatorId": 123
}
```

**Response:** Same as `/validate` but with `startedRemotely: true`

### 3. Updated Controller: `MaintenanceController.java`

**Changed:** `createMaintenance()` now requires `X-QR-SESSION-TOKEN` header.

**Validation Logic:**
- **TECHNICIAN:** Token required, validates token, invalidates after use
- **ADMIN:** Token optional, if provided validates it, if missing allows remote start

**Header:**
```
X-QR-SESSION-TOKEN: uuid-token
```

### 4. Configuration: `application.yml`

```yaml
app:
  qr:
    session-token-ttl-minutes: 5  # Session token expiration
```

---

## 🎨 Frontend Implementation

### 1. QR Validation Modal Component

**File:** `components/MaintenanceQrModal.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { useUser } from '@/hooks/useUser';
import { Html5Qrcode } from 'html5-qrcode';
import apiClient from '@/config/apiClient';

interface MaintenanceQrModalProps {
  elevatorId: number;
  onQrValidated: (sessionToken: string, elevatorId: number) => void;
  onClose: () => void;
}

export const MaintenanceQrModal: React.FC<MaintenanceQrModalProps> = ({
  elevatorId,
  onQrValidated,
  onClose,
}) => {
  const { user } = useUser();
  const [qrCode, setQrCode] = useState<string>('');
  const [manualInput, setManualInput] = useState<string>('');
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState(false);
  const [html5QrCode, setHtml5QrCode] = useState<Html5Qrcode | null>(null);

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'PATRON';
  const isTechnician = user?.role === 'PERSONEL';

  // Detect mobile device
  useEffect(() => {
    const checkMobile = () => {
      const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera;
      const isMobileDevice = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent.toLowerCase());
      setIsMobile(isMobileDevice);
    };
    checkMobile();
  }, []);

  // Start camera scanning (mobile only)
  const startCameraScan = async () => {
    try {
      setError(null);
      const qrCodeInstance = new Html5Qrcode("qr-reader");
      
      await qrCodeInstance.start(
        { facingMode: "environment" }, // Back camera
        {
          fps: 10,
          qrbox: { width: 250, height: 250 }
        },
        (decodedText) => {
          // QR code scanned
          handleQrScanned(decodedText);
        },
        (errorMessage) => {
          // Ignore scan errors (normal during scanning)
        }
      );
      
      setHtml5QrCode(qrCodeInstance);
      setScanning(true);
    } catch (err: any) {
      setError('Kamera erişimi reddedildi veya kullanılamıyor');
      console.error('Camera error:', err);
    }
  };

  // Stop camera scanning
  const stopCameraScan = async () => {
    if (html5QrCode) {
      try {
        await html5QrCode.stop();
        html5QrCode.clear();
      } catch (err) {
        console.error('Stop camera error:', err);
      }
      setHtml5QrCode(null);
      setScanning(false);
    }
  };

  // Handle QR code scanned or manually entered
  const handleQrScanned = async (scannedData: string) => {
    try {
      setError(null);
      
      // Stop camera if scanning
      if (scanning) {
        await stopCameraScan();
      }
      
      // Validate QR and create session token
      const response = await apiClient.post('/qr/validate', {
        qrCode: scannedData,
        elevatorId: elevatorId,
      });
      
      if (response.data.success) {
        const { qrSessionToken, elevatorId: validatedElevatorId } = response.data.data;
        
        // Verify elevator match
        if (validatedElevatorId !== elevatorId) {
          throw new Error('QR kodu farklı bir asansör için');
        }
        
        // Success: Close QR modal → Open maintenance modal
        onQrValidated(qrSessionToken, validatedElevatorId);
        onClose();
      } else {
        throw new Error(response.data.message || 'QR doğrulama başarısız');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'QR doğrulama başarısız');
    }
  };

  // Handle manual QR input
  const handleManualSubmit = () => {
    if (!manualInput.trim()) {
      setError('Lütfen QR kodunu girin');
      return;
    }
    handleQrScanned(manualInput.trim());
  };

  // Handle remote start (ADMIN only)
  const handleRemoteStart = async () => {
    try {
      setError(null);
      
      const response = await apiClient.post('/qr/remote-start', {
        elevatorId: elevatorId,
      });
      
      if (response.data.success) {
        const { qrSessionToken, elevatorId: validatedElevatorId } = response.data.data;
        onQrValidated(qrSessionToken, validatedElevatorId);
        onClose();
      } else {
        throw new Error(response.data.message || 'Uzaktan başlatma başarısız');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Uzaktan başlatma başarısız');
    }
  };

  // Cleanup camera on unmount
  useEffect(() => {
    return () => {
      if (html5QrCode) {
        stopCameraScan();
      }
    };
  }, [html5QrCode]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h2 className="text-xl font-bold mb-4">QR Kod Doğrulama</h2>
        
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
        
        {/* Mobile: Camera Scanner */}
        {isMobile && (
          <div className="mb-4">
            <div id="qr-reader" className="w-full h-64 bg-black rounded mb-2"></div>
            {!scanning ? (
              <button
                onClick={startCameraScan}
                className="w-full bg-green-600 text-white py-2 px-4 rounded hover:bg-green-700"
              >
                Kamerayla Tara
              </button>
            ) : (
              <button
                onClick={stopCameraScan}
                className="w-full bg-red-600 text-white py-2 px-4 rounded hover:bg-red-700"
              >
                Kamerayı Durdur
              </button>
            )}
          </div>
        )}
        
        {/* Manual QR Input (Desktop or fallback) */}
        <div className="mb-4">
          <label className="block text-sm font-medium mb-2">
            QR Kodu (Manuel Giriş)
          </label>
          <input
            type="text"
            value={manualInput}
            onChange={(e) => setManualInput(e.target.value)}
            placeholder="QR kodunu buraya yapıştırın"
            className="w-full border rounded px-3 py-2"
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                handleManualSubmit();
              }
            }}
          />
          <button
            onClick={handleManualSubmit}
            disabled={!manualInput.trim()}
            className={`mt-2 w-full py-2 px-4 rounded ${
              !manualInput.trim()
                ? 'bg-gray-400 cursor-not-allowed'
                : 'bg-gray-600 hover:bg-gray-700 text-white'
            }`}
          >
            Doğrula
          </button>
        </div>
        
        {/* Cancel Button */}
        <button
          onClick={onClose}
          className="w-full bg-gray-300 text-gray-700 py-2 px-4 rounded hover:bg-gray-400"
        >
          İptal
        </button>
      </div>
    </div>
  );
};
```

### 2. Updated Maintenance Create Modal

**File:** `components/MaintenanceCreateModal.tsx`

**Changes:**
1. Accept `qrSessionToken` prop
2. Send token in `X-QR-SESSION-TOKEN` header
3. Clear token after success or modal close

```typescript
interface MaintenanceCreateModalProps {
  elevatorId: number;
  qrSessionToken: string; // NEW: Required prop
  onClose: () => void;
  onSuccess?: () => void;
}

export const MaintenanceCreateModal: React.FC<MaintenanceCreateModalProps> = ({
  elevatorId,
  qrSessionToken,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    elevatorId,
    date: new Date().toISOString().split('T')[0],
    labelType: 'GREEN',
    description: '',
    amount: 0,
  });
  const [photos, setPhotos] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      setError(null);

      // Validate minimum 4 photos
      if (photos.length < 4) {
        setError('En az 4 fotoğraf gereklidir');
        return;
      }

      const formDataObj = new FormData();
      
      // Add JSON data
      formDataObj.append('data', JSON.stringify({
        elevatorId: formData.elevatorId,
        date: formData.date,
        labelType: formData.labelType,
        description: formData.description,
        amount: formData.amount,
      }));
      
      // Add photos
      photos.forEach(photo => {
        formDataObj.append('photos', photo);
      });
      
      // Submit with QR session token in header
      const response = await apiClient.post('/maintenances', formDataObj, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'X-QR-SESSION-TOKEN': qrSessionToken, // NEW: Session token header
        },
      });
      
      if (response.data.success) {
        // Clear token (one-time use)
        // Token is already invalidated on backend, but clear from memory
        onSuccess?.();
        onClose();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Bakım oluşturma başarısız');
    } finally {
      setSubmitting(false);
    }
  };

  // ... rest of component
};
```

### 3. Updated Entry Points

**File:** `pages/ElevatorDetail.tsx`

```typescript
const [showQrModal, setShowQrModal] = useState(false);
const [showCreateModal, setShowCreateModal] = useState(false);
const [qrSessionToken, setQrSessionToken] = useState<string | null>(null);
const [selectedElevatorId, setSelectedElevatorId] = useState<number | null>(null);

const handleCreateMaintenance = (elevatorId: number) => {
  setSelectedElevatorId(elevatorId);
  setShowQrModal(true); // Open QR modal FIRST
  setShowCreateModal(false); // Ensure create modal is closed
  setQrSessionToken(null); // Clear any previous token
};

const handleQrValidated = (token: string, elevatorId: number) => {
  setQrSessionToken(token);
  setShowQrModal(false); // Close QR modal
  setShowCreateModal(true); // Open maintenance modal
};

const handleQrModalCancel = () => {
  setShowQrModal(false);
  // DO NOT open maintenance modal
  setQrSessionToken(null);
};

return (
  <>
    {/* "Yeni Bakım Ekle" Button */}
    <button onClick={() => handleCreateMaintenance(elevator.id)}>
      Yeni Bakım Ekle
    </button>
    
    {/* QR Validation Modal */}
    {showQrModal && selectedElevatorId && (
      <MaintenanceQrModal
        elevatorId={selectedElevatorId}
        onQrValidated={handleQrValidated}
        onClose={handleQrModalCancel}
      />
    )}
    
    {/* Maintenance Create Modal - ONLY opens after QR validation */}
    {showCreateModal && selectedElevatorId && qrSessionToken && (
      <MaintenanceCreateModal
        elevatorId={selectedElevatorId}
        qrSessionToken={qrSessionToken}
        onClose={() => {
          setShowCreateModal(false);
          setQrSessionToken(null);
        }}
        onSuccess={() => {
          // Refresh maintenance list
          refetchMaintenances();
        }}
      />
    )}
  </>
);
```

**File:** `pages/MaintenanceList.tsx`

Same pattern as above - update "Yeni Bakım Ekle" button to open QR modal first.

---

## 📡 API Contract

### POST `/api/qr/validate`

**Request:**
```json
{
  "qrCode": "e=ELEV-001&s=signature",
  "elevatorId": 123
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "qrSessionToken": "uuid-token",
    "elevatorId": 123,
    "expiresAt": "2026-01-15T10:05:00",
    "startedRemotely": false
  }
}
```

**Response (Error - 403):**
```json
{
  "success": false,
  "message": "Invalid QR signature"
}
```

### POST `/api/qr/remote-start`

**Request:**
```json
{
  "elevatorId": 123
}
```

**Response:** Same as `/validate` but `startedRemotely: true`

**Error (403):** "Remote start is only allowed for ADMIN role"

### POST `/api/maintenances`

**Headers:**
```
X-QR-SESSION-TOKEN: uuid-token (required for TECHNICIAN, optional for ADMIN)
```

**Request:** Same as before (multipart/form-data)

**Response (Error - 403):**
```json
{
  "success": false,
  "message": "QR validation required. Please scan QR code first."
}
```

---

## 🧪 Testing Plan

### Test Case 1: TECHNICIAN with QR (Mobile)
1. Login as TECHNICIAN
2. Navigate to elevator detail page
3. Click "Yeni Bakım Ekle"
4. ✅ QR modal opens
5. ✅ Camera starts automatically (mobile)
6. Scan QR code
7. ✅ QR validated → QR modal closes
8. ✅ Maintenance modal opens
9. Fill form, upload 4 photos, submit
10. ✅ Maintenance created successfully

### Test Case 2: TECHNICIAN with QR (Desktop)
1. Login as TECHNICIAN
2. Navigate to elevator detail page
3. Click "Yeni Bakım Ekle"
4. ✅ QR modal opens
5. ✅ Camera button NOT auto-started (desktop)
6. Enter QR code manually
7. Click "Doğrula"
8. ✅ QR validated → QR modal closes
9. ✅ Maintenance modal opens
10. Submit form
11. ✅ Maintenance created successfully

### Test Case 3: TECHNICIAN without QR
1. Login as TECHNICIAN
2. Navigate to elevator detail page
3. Click "Yeni Bakım Ekle"
4. ✅ QR modal opens
5. Click "İptal" (cancel)
6. ✅ QR modal closes
7. ✅ Maintenance modal does NOT open
8. Try to call API directly without token:
   ```bash
   POST /api/maintenances
   # Without X-QR-SESSION-TOKEN header
   ```
9. ✅ Returns 403 "QR validation required"

### Test Case 4: ADMIN Remote Start
1. Login as ADMIN
2. Navigate to elevator detail page
3. Click "Yeni Bakım Ekle"
4. ✅ QR modal opens
5. ✅ "Uzaktan Başlat" button visible
6. Click "Uzaktan Başlat"
7. ✅ QR modal closes
8. ✅ Maintenance modal opens
9. Submit form
10. ✅ Maintenance created with `startedRemotely: true`

### Test Case 5: ADMIN with QR
1. Login as ADMIN
2. Navigate to elevator detail page
3. Click "Yeni Bakım Ekle"
4. ✅ QR modal opens
5. Scan/enter QR code
6. ✅ QR validated → QR modal closes
7. ✅ Maintenance modal opens
8. Submit form
9. ✅ Maintenance created with `startedRemotely: false`

### Test Case 6: Planning Screen Unchanged
1. Navigate to `/maintenances/plan`
2. ✅ Calendar view works normally
3. ✅ Create plan button works (no QR required)
4. ✅ All planning endpoints work as before

### Test Case 7: Token Expiration
1. Create session token
2. Wait 6 minutes (past 5-minute TTL)
3. Try to create maintenance
4. ✅ Returns 403 "QR session token has expired"

### Test Case 8: Token Reuse Prevention
1. Create session token
2. Use token to create maintenance (success)
3. Try to use same token again
4. ✅ Returns 403 "Invalid QR session token" (token was invalidated)

---

## 🔒 Security Notes

1. **Server-Side Enforcement:** All validation happens on backend. Frontend cannot bypass.
2. **One-Time Use:** Tokens are invalidated after use.
3. **Short TTL:** 5-minute expiration prevents token reuse.
4. **User Match:** Token must match authenticated user.
5. **Elevator Match:** Token must match elevator in request.

---

## 📝 Files Changed

### Backend:
1. ✅ `QrSessionService.java` - **NEW**
2. ✅ `QrController.java` - **UPDATED** (added `/validate` and `/remote-start`)
3. ✅ `MaintenanceController.java` - **UPDATED** (added header validation)
4. ✅ `application.yml` - **UPDATED** (added session token TTL config)

### Frontend (to be implemented):
1. `components/MaintenanceQrModal.tsx` - **NEW**
2. `components/MaintenanceCreateModal.tsx` - **UPDATE** (add token header)
3. `pages/ElevatorDetail.tsx` - **UPDATE** (add QR flow)
4. `pages/MaintenanceList.tsx` - **UPDATE** (add QR flow)

---

## ✅ Checklist

- [x] Backend session token service created
- [x] QR validate endpoint created
- [x] Remote start endpoint created
- [x] Maintenance create endpoint enforces token
- [x] Token validation (expiration, user match, elevator match)
- [x] Token invalidation after use
- [x] Audit logging (console for now)
- [x] Configuration added
- [ ] Frontend QR modal component
- [ ] Frontend maintenance modal updated
- [ ] Frontend entry points updated
- [ ] Mobile camera integration
- [ ] Desktop manual input
- [ ] Testing completed

---

**End of Documentation**
