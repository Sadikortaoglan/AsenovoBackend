# Frontend QR Implementation Guide

## 📋 Quick Reference

### Entry Points to Update

Find and update ALL "Yeni Bakım Ekle" buttons:

1. **Elevator Detail Page** (`/elevators/:id`)
2. **Maintenance List Page** (`/maintenances/list`)
3. **Any other create-maintenance entry points**

### Required Changes

1. **QR Modal Component** - Create new
2. **Maintenance Create Modal** - Update to accept `qrSessionToken`
3. **Entry Point Handlers** - Update to open QR modal first

---

## 🎯 State Machine (CRITICAL)

```typescript
// State variables
const [showQrModal, setShowQrModal] = useState(false);
const [showCreateModal, setShowCreateModal] = useState(false);
const [qrSessionToken, setQrSessionToken] = useState<string | null>(null);
const [selectedElevatorId, setSelectedElevatorId] = useState<number | null>(null);

// Flow:
// 1. User clicks "Yeni Bakım Ekle"
handleCreateMaintenance(elevatorId) {
  setSelectedElevatorId(elevatorId);
  setShowQrModal(true);        // ✅ Open QR modal
  setShowCreateModal(false);    // ✅ Ensure create modal closed
  setQrSessionToken(null);     // ✅ Clear previous token
}

// 2. QR validated successfully
handleQrValidated(token, elevatorId) {
  setQrSessionToken(token);
  setShowQrModal(false);       // ✅ Close QR modal
  setShowCreateModal(true);     // ✅ Open maintenance modal
}

// 3. User cancels QR modal
handleQrModalCancel() {
  setShowQrModal(false);       // ✅ Close QR modal
  // DO NOT open maintenance modal
  setQrSessionToken(null);     // ✅ Clear token
}
```

**NON-NEGOTIABLE:** Maintenance modal MUST NOT open unless `qrSessionToken` is set.

---

## 📦 Dependencies

### Install QR Scanner Library

```bash
npm install html5-qrcode
# or
npm install @zxing/library @zxing/browser
```

**Recommended:** `html5-qrcode` (simpler API, better mobile support)

---

## 🔧 Component Implementation

### 1. QR Modal Component

**File:** `src/components/MaintenanceQrModal.tsx`

See full implementation in `QR_SESSION_TOKEN_IMPLEMENTATION.md` section "Frontend Implementation > QR Validation Modal Component"

**Key Features:**
- Mobile: Auto-start camera or "Kamerayla Tara" button
- Desktop: Manual input field (no auto-camera)
- ADMIN: "Uzaktan Başlat" button
- TECHNICIAN: QR required, no bypass button
- "Doğrula" button (disabled until QR code present)
- "İptal" button (closes modal, does NOT open maintenance modal)

### 2. Maintenance Create Modal Update

**File:** `src/components/MaintenanceCreateModal.tsx`

**Changes:**

```typescript
// 1. Add qrSessionToken to props
interface MaintenanceCreateModalProps {
  elevatorId: number;
  qrSessionToken: string; // NEW: Required
  onClose: () => void;
  onSuccess?: () => void;
}

// 2. Send token in header
const handleSubmit = async () => {
  const formDataObj = new FormData();
  formDataObj.append('data', JSON.stringify(formData));
  photos.forEach(photo => formDataObj.append('photos', photo));
  
  const response = await apiClient.post('/maintenances', formDataObj, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'X-QR-SESSION-TOKEN': qrSessionToken, // NEW: Add header
    },
  });
  
  // ... rest of logic
};
```

### 3. Entry Point Updates

**File:** `src/pages/ElevatorDetail.tsx`

```typescript
// Add state
const [showQrModal, setShowQrModal] = useState(false);
const [showCreateModal, setShowCreateModal] = useState(false);
const [qrSessionToken, setQrSessionToken] = useState<string | null>(null);
const [selectedElevatorId, setSelectedElevatorId] = useState<number | null>(null);

// Update button handler
const handleCreateMaintenance = (elevatorId: number) => {
  setSelectedElevatorId(elevatorId);
  setShowQrModal(true);
  setShowCreateModal(false);
  setQrSessionToken(null);
};

// QR validated handler
const handleQrValidated = (token: string, elevatorId: number) => {
  setQrSessionToken(token);
  setShowQrModal(false);
  setShowCreateModal(true);
};

// QR modal cancel handler
const handleQrModalCancel = () => {
  setShowQrModal(false);
  setQrSessionToken(null);
  // DO NOT open maintenance modal
};

// Render
return (
  <>
    {/* Existing elevator detail content */}
    
    {/* "Yeni Bakım Ekle" Button */}
    <button onClick={() => handleCreateMaintenance(elevator.id)}>
      Yeni Bakım Ekle
    </button>
    
    {/* QR Modal */}
    {showQrModal && selectedElevatorId && (
      <MaintenanceQrModal
        elevatorId={selectedElevatorId}
        onQrValidated={handleQrValidated}
        onClose={handleQrModalCancel}
      />
    )}
    
    {/* Maintenance Create Modal - ONLY if token exists */}
    {showCreateModal && selectedElevatorId && qrSessionToken && (
      <MaintenanceCreateModal
        elevatorId={selectedElevatorId}
        qrSessionToken={qrSessionToken}
        onClose={() => {
          setShowCreateModal(false);
          setQrSessionToken(null);
        }}
        onSuccess={() => {
          // Refresh data
          refetchMaintenances();
        }}
      />
    )}
  </>
);
```

**File:** `src/pages/MaintenanceList.tsx`

Apply same pattern to "Yeni Bakım Ekle" button in maintenance list.

---

## 🔍 Finding All Entry Points

Search for "Yeni Bakım Ekle" or maintenance create buttons:

```bash
# Search in frontend codebase
grep -r "Yeni Bakım Ekle" src/
grep -r "createMaintenance\|create.*maintenance" src/ --include="*.tsx" --include="*.ts"
grep -r "/maintenances.*create\|maintenance.*create" src/
```

Common locations:
- `pages/ElevatorDetail.tsx`
- `pages/MaintenanceList.tsx`
- `components/ElevatorCard.tsx` (if exists)
- Any maintenance-related pages

---

## 📱 Mobile vs Desktop Detection

```typescript
const [isMobile, setIsMobile] = useState(false);

useEffect(() => {
  const checkMobile = () => {
    const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera;
    const isMobileDevice = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent.toLowerCase());
    setIsMobile(isMobileDevice);
  };
  checkMobile();
}, []);

// Usage
{isMobile && (
  <CameraScanner />
)}
{!isMobile && (
  <ManualInput />
)}
```

---

## 🎥 Camera Integration (Mobile)

### Using html5-qrcode

```typescript
import { Html5Qrcode } from 'html5-qrcode';

const startCamera = async () => {
  const qrCode = new Html5Qrcode("qr-reader");
  
  await qrCode.start(
    { facingMode: "environment" }, // Back camera
    {
      fps: 10,
      qrbox: { width: 250, height: 250 }
    },
    (decodedText) => {
      // QR scanned
      handleQrScanned(decodedText);
      qrCode.stop();
    },
    (errorMessage) => {
      // Ignore scan errors
    }
  );
};

// HTML
<div id="qr-reader"></div>
```

### Camera Permissions

Handle gracefully:

```typescript
try {
  await startCamera();
} catch (err) {
  if (err.name === 'NotAllowedError') {
    setError('Kamera izni reddedildi. Lütfen tarayıcı ayarlarından izin verin.');
  } else if (err.name === 'NotFoundError') {
    setError('Kamera bulunamadı. Manuel giriş kullanın.');
  } else {
    setError('Kamera hatası: ' + err.message);
  }
}
```

---

## 🔐 API Integration

### Validate QR Endpoint

```typescript
// POST /api/qr/validate
const validateQr = async (qrCode: string, elevatorId: number) => {
  const response = await apiClient.post('/qr/validate', {
    qrCode,
    elevatorId,
  });
  
  if (response.data.success) {
    return response.data.data; // { qrSessionToken, elevatorId, expiresAt, startedRemotely }
  } else {
    throw new Error(response.data.message);
  }
};
```

### Remote Start Endpoint (ADMIN)

```typescript
// POST /api/qr/remote-start
const remoteStart = async (elevatorId: number) => {
  const response = await apiClient.post('/qr/remote-start', {
    elevatorId,
  });
  
  if (response.data.success) {
    return response.data.data; // { qrSessionToken, elevatorId, expiresAt, startedRemotely: true }
  } else {
    throw new Error(response.data.message);
  }
};
```

### Create Maintenance with Token

```typescript
// POST /api/maintenances
const createMaintenance = async (formData: FormData, qrSessionToken: string) => {
  const response = await apiClient.post('/maintenances', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'X-QR-SESSION-TOKEN': qrSessionToken, // Required header
    },
  });
  
  return response.data;
};
```

---

## ✅ Testing Checklist

### Mobile Testing
- [ ] Camera opens automatically or via button
- [ ] QR scanning works
- [ ] QR validation succeeds
- [ ] QR modal closes → Maintenance modal opens
- [ ] Maintenance creation succeeds with token

### Desktop Testing
- [ ] Camera does NOT auto-open
- [ ] Manual input field visible
- [ ] QR validation works with manual input
- [ ] QR modal closes → Maintenance modal opens
- [ ] Maintenance creation succeeds with token

### ADMIN Testing
- [ ] "Uzaktan Başlat" button visible
- [ ] Remote start works (no QR required)
- [ ] Maintenance creation succeeds
- [ ] Can also use QR if provided

### TECHNICIAN Testing
- [ ] "Uzaktan Başlat" button NOT visible
- [ ] QR required (cannot bypass)
- [ ] Maintenance creation fails without QR
- [ ] Maintenance creation succeeds with QR

### Edge Cases
- [ ] QR modal cancel does NOT open maintenance modal
- [ ] Expired token returns 403
- [ ] Token reuse returns 403
- [ ] Wrong elevator QR returns 403
- [ ] Planning screen unchanged

---

## 🚨 Common Mistakes to Avoid

1. ❌ **Opening maintenance modal without token**
   - ✅ Always check `qrSessionToken` exists before opening

2. ❌ **Opening maintenance modal on QR cancel**
   - ✅ Only open on `onQrValidated`, never on `onClose`

3. ❌ **Forgetting token header**
   - ✅ Always include `X-QR-SESSION-TOKEN` header

4. ❌ **Auto-opening camera on desktop**
   - ✅ Only show camera button, don't auto-start

5. ❌ **Changing planning screen**
   - ✅ Do NOT touch `/maintenances/plan` routes

---

**End of Guide**
