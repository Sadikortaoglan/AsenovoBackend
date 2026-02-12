# Elevator QR Code Feature - Complete Implementation

## 📋 Table of Contents
1. [Overview](#overview)
2. [Backend Implementation](#backend-implementation)
3. [Frontend Implementation](#frontend-implementation)
4. [API Endpoints](#api-endpoints)
5. [Security](#security)
6. [Usage Examples](#usage-examples)

---

## 🎯 Overview

### Features
- **Static QR codes** per elevator (permanently printed)
- **QR display** on elevator detail page
- **Download as PNG** image
- **Download as PDF** with company logo, elevator info, QR image, and instructions
- **QR validation** when scanning

### QR Format
```
https://app.saraasansor.com/qr-start?e={elevatorCode}&s={signature}
```

Where:
- `e` = Elevator public code (`identityNumber` or `elevatorNumber`)
- `s` = HMAC-SHA256 signature (Base64 URL-encoded)

---

## 🔧 Backend Implementation

### 1. Dependencies

**pom.xml:**
```xml
<!-- QR Code Generation (ZXing) -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

### 2. Service: `ElevatorQrService`

**Key Methods:**
- `generateQrUrl(Long elevatorId)`: Generates QR URL with signature
- `generateSignature(String elevatorCode)`: HMAC-SHA256 signature
- `validateSignature(String elevatorCode, String signature)`: Validates QR
- `generateQrImage(Long elevatorId)`: Returns `BufferedImage`
- `generateQrImagePng(Long elevatorId)`: Returns PNG byte array
- `generateQrPdf(Long elevatorId)`: Returns PDF byte array with logo, info, instructions

**HMAC Signature Generation:**
```java
public String generateSignature(String elevatorCode) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec = new SecretKeySpec(
            qrSecretKey.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
    );
    mac.init(secretKeySpec);
    byte[] signatureBytes = mac.doFinal(elevatorCode.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
}
```

### 3. Configuration

**application.yml:**
```yaml
app:
  qr:
    secret-key: ${QR_SECRET_KEY:default-secret-key-change-in-production}
    base-url: ${QR_BASE_URL:https://app.saraasansor.com}
    size: 300 # QR code image size in pixels
```

---

## 📡 API Endpoints

### 1. GET `/api/elevators/{id}/qr`

**Description:** Returns QR code image as PNG

**Response:**
- Content-Type: `image/png`
- Body: PNG image bytes

**Example:**
```bash
curl -X GET "http://localhost:8080/api/elevators/1/qr" \
  -H "Authorization: Bearer {token}" \
  --output qr-code.png
```

### 2. GET `/api/elevators/{id}/qr/download?format=png|pdf`

**Description:** Downloads QR code as PNG or PDF

**Query Parameters:**
- `format` (optional, default: `png`): `png` or `pdf`

**Response:**
- Content-Type: `image/png` or `application/pdf`
- Content-Disposition: `attachment; filename="elevator-{id}-qr.{format}"`
- Body: File bytes

**Examples:**
```bash
# Download PNG
curl -X GET "http://localhost:8080/api/elevators/1/qr/download?format=png" \
  -H "Authorization: Bearer {token}" \
  --output qr-code.png

# Download PDF
curl -X GET "http://localhost:8080/api/elevators/1/qr/download?format=pdf" \
  -H "Authorization: Bearer {token}" \
  --output qr-code.pdf
```

### 3. GET `/api/qr/validate?e={elevatorCode}&s={signature}`

**Description:** Validates QR code signature when scanning

**Query Parameters:**
- `e` (required): Elevator public code
- `s` (required): HMAC signature

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "QR code is valid",
  "data": {
    "valid": true,
    "elevatorId": 123,
    "elevatorCode": "ELEV-002",
    "buildingName": "Example Building",
    "address": "123 Main St"
  },
  "errors": null
}
```

**Response (Error - 403):**
```json
{
  "success": false,
  "message": "Invalid QR signature",
  "data": null,
  "errors": null
}
```

---

## 🎨 Frontend Implementation

### 1. React Component: `ElevatorQrDisplay.tsx`

```typescript
// components/ElevatorQrDisplay.tsx
import React, { useState } from 'react';
import { useElevatorQr } from '@/hooks/useElevatorQr';

interface ElevatorQrDisplayProps {
  elevatorId: number;
  elevatorName?: string;
}

export const ElevatorQrDisplay: React.FC<ElevatorQrDisplayProps> = ({
  elevatorId,
  elevatorName,
}) => {
  const { qrImageUrl, loading, error, downloadQrPng, downloadQrPdf } = useElevatorQr(elevatorId);
  const [downloading, setDownloading] = useState<'png' | 'pdf' | null>(null);

  const handleDownloadPng = async () => {
    try {
      setDownloading('png');
      await downloadQrPng();
    } catch (err) {
      console.error('Failed to download PNG:', err);
    } finally {
      setDownloading(null);
    }
  };

  const handleDownloadPdf = async () => {
    try {
      setDownloading('pdf');
      await downloadQrPdf();
    } catch (err) {
      console.error('Failed to download PDF:', err);
    } finally {
      setDownloading(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <span className="ml-2">QR kodu yükleniyor...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-100 text-red-700 rounded">
        <p>QR kodu yüklenirken hata oluştu: {error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">
        {elevatorName ? `${elevatorName} - QR Kodu` : 'QR Kodu'}
      </h3>
      
      {/* QR Code Image */}
      <div className="flex justify-center mb-4">
        {qrImageUrl ? (
          <img
            src={qrImageUrl}
            alt="QR Code"
            className="border-2 border-gray-300 rounded"
            style={{ width: '300px', height: '300px' }}
          />
        ) : (
          <div className="w-[300px] h-[300px] bg-gray-200 rounded flex items-center justify-center">
            <span className="text-gray-500">QR kodu yüklenemedi</span>
          </div>
        )}
      </div>
      
      {/* Download Buttons */}
      <div className="flex gap-2 justify-center">
        <button
          onClick={handleDownloadPng}
          disabled={downloading === 'png'}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {downloading === 'png' ? 'İndiriliyor...' : 'PNG İndir'}
        </button>
        
        <button
          onClick={handleDownloadPdf}
          disabled={downloading === 'pdf'}
          className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {downloading === 'pdf' ? 'İndiriliyor...' : 'PDF İndir'}
        </button>
      </div>
      
      {/* Instructions */}
      <div className="mt-4 text-sm text-gray-600">
        <p className="font-semibold mb-2">Kullanım:</p>
        <ul className="list-disc list-inside space-y-1">
          <li>Bu QR kodu asansör üzerine yapıştırın</li>
          <li>Bakım başlatmak için QR kodu mobil uygulama ile tarayın</li>
          <li>QR kod hasarlı veya okunamaz durumda ise yenisini yazdırın</li>
        </ul>
      </div>
    </div>
  );
};
```

### 2. Custom Hook: `useElevatorQr.ts`

```typescript
// hooks/useElevatorQr.ts
import { useState, useEffect } from 'react';
import apiClient from '@/config/apiClient';

export function useElevatorQr(elevatorId: number) {
  const [qrImageUrl, setQrImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadQrImage = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Fetch QR image as blob
        const response = await apiClient.get(`/elevators/${elevatorId}/qr`, {
          responseType: 'blob',
        });
        
        // Create object URL for image display
        const imageUrl = URL.createObjectURL(response.data);
        setQrImageUrl(imageUrl);
      } catch (err: any) {
        setError(err.message || 'QR kodu yüklenemedi');
      } finally {
        setLoading(false);
      }
    };

    if (elevatorId) {
      loadQrImage();
    }

    // Cleanup object URL on unmount
    return () => {
      if (qrImageUrl) {
        URL.revokeObjectURL(qrImageUrl);
      }
    };
  }, [elevatorId]);

  const downloadQrPng = async () => {
    try {
      const response = await apiClient.get(
        `/elevators/${elevatorId}/qr/download?format=png`,
        { responseType: 'blob' }
      );
      
      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `elevator-${elevatorId}-qr.png`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      throw new Error(err.message || 'PNG indirme başarısız');
    }
  };

  const downloadQrPdf = async () => {
    try {
      const response = await apiClient.get(
        `/elevators/${elevatorId}/qr/download?format=pdf`,
        { responseType: 'blob' }
      );
      
      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `elevator-${elevatorId}-qr.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      throw new Error(err.message || 'PDF indirme başarısız');
    }
  };

  return {
    qrImageUrl,
    loading,
    error,
    downloadQrPng,
    downloadQrPdf,
  };
}
```

### 3. Usage in Elevator Detail Page

```typescript
// pages/ElevatorDetail.tsx
import { ElevatorQrDisplay } from '@/components/ElevatorQrDisplay';

export const ElevatorDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const elevatorId = parseInt(id || '0');
  
  // ... fetch elevator data ...
  
  return (
    <div className="container mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Asansör Detayı</h1>
      
      {/* Elevator Info */}
      <div className="mb-6">
        {/* ... elevator details ... */}
      </div>
      
      {/* QR Code Section */}
      <ElevatorQrDisplay
        elevatorId={elevatorId}
        elevatorName={elevator?.buildingName}
      />
    </div>
  );
};
```

### 4. QR Scanning Flow (Mobile)

```typescript
// components/QrScanner.tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import QrScannerLib from 'qr-scanner';

export const QrScanner: React.FC = () => {
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleScan = async (scannedData: string) => {
    try {
      setError(null);
      
      // Parse QR URL: https://app.saraasansor.com/qr-start?e={code}&s={signature}
      const url = new URL(scannedData);
      const elevatorCode = url.searchParams.get('e');
      const signature = url.searchParams.get('s');
      
      if (!elevatorCode || !signature) {
        throw new Error('Geçersiz QR kod formatı');
      }
      
      // Validate QR signature
      const response = await apiClient.get('/qr/validate', {
        params: { e: elevatorCode, s: signature },
      });
      
      if (!response.data.success || !response.data.data.valid) {
        throw new Error('QR kodu geçersiz');
      }
      
      const elevatorId = response.data.data.elevatorId;
      
      // Close QR modal → Open MaintenanceCreateModal
      // Pre-fill elevatorId
      navigate(`/maintenances/create?elevatorId=${elevatorId}`);
      
    } catch (err: any) {
      setError(err.message || 'QR kod doğrulanamadı');
    }
  };

  return (
    <div>
      {/* QR Scanner UI */}
      {/* ... */}
    </div>
  );
};
```

---

## 🔒 Security

### HMAC Signature

**Generation:**
```java
HMAC-SHA256(elevatorCode, secretKey) → Base64 URL-encoded
```

**Validation:**
- Backend recalculates signature
- Compares with provided signature
- Returns 403 if mismatch

### Best Practices

1. **Secret Key:** Store in environment variable (`QR_SECRET_KEY`)
2. **QR Base URL:** Configurable via `QR_BASE_URL`
3. **Signature Validation:** Always validate on backend
4. **Error Messages:** Don't reveal signature details in errors

---

## 📝 Usage Examples

### Backend: Generate QR URL

```java
@Autowired
private ElevatorQrService elevatorQrService;

String qrUrl = elevatorQrService.generateQrUrl(elevatorId);
// Returns: https://app.saraasansor.com/qr-start?e=ELEV-002&s=abc123...
```

### Backend: Validate QR

```java
boolean isValid = elevatorQrService.validateSignature(elevatorCode, signature);
if (isValid) {
    Elevator elevator = elevatorQrService.getElevatorByCode(elevatorCode);
    // Proceed with maintenance start
}
```

### Frontend: Display QR

```tsx
<ElevatorQrDisplay elevatorId={123} elevatorName="Example Building" />
```

### Frontend: Download QR

```typescript
const { downloadQrPng, downloadQrPdf } = useElevatorQr(elevatorId);

// Download PNG
await downloadQrPng();

// Download PDF
await downloadQrPdf();
```

---

## ✅ Testing Checklist

- [ ] QR image displays correctly on elevator detail page
- [ ] PNG download works
- [ ] PDF download works (with logo, info, instructions)
- [ ] QR signature validation works
- [ ] Invalid signature returns 403
- [ ] QR scanning flow works (mobile)
- [ ] QR modal closes → MaintenanceCreateModal opens
- [ ] ElevatorId is pre-filled in maintenance form
- [ ] Error handling works gracefully
- [ ] Loading states display correctly

---

## 🚀 Deployment Notes

1. **Environment Variables:**
   ```bash
   QR_SECRET_KEY=your-production-secret-key
   QR_BASE_URL=https://app.saraasansor.com
   ```

2. **QR Code Printing:**
   - Generate PDF for each elevator
   - Print and attach to elevators
   - QR codes are static (never expire)

3. **Frontend Dependencies:**
   ```bash
   npm install qr-scanner  # For mobile scanning
   ```

---

**End of Documentation**
