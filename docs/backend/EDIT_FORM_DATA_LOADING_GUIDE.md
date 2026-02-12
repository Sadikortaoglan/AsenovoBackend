# Edit Form Data Loading Guide

## Problem
When clicking "Edit" for an elevator/offer, the edit form should load ALL existing data. This guide ensures proper data loading.

## Backend Verification ✅

### GET Endpoints
Both endpoints return complete data:

1. **GET /api/elevators/{id}** → Returns `ElevatorDto` with ALL fields
2. **GET /api/offers/{id}** → Returns `OfferDto` with ALL fields including `items[]`

### Response Format

#### Elevator Response:
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "identityNumber": "ELEV-001",
    "buildingName": "Example Building",
    "address": "123 Main St",
    "elevatorNumber": "A1",
    "floorCount": 5,
    "capacity": 630,
    "speed": 1.0,
    "technicalNotes": "Notes here",
    "driveType": "Geared",
    "machineBrand": "Brand",
    "doorType": "Center opening",
    "installationYear": 2020,
    "serialNumber": "SN123",
    "controlSystem": "Microprocessor",
    "rope": "Steel",
    "modernization": "2020",
    "inspectionDate": "2024-01-01",
    "expiryDate": "2025-01-01"
  }
}
```

#### Offer Response:
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "elevatorId": 1,
    "elevatorBuildingName": "Example Building",
    "elevatorIdentityNumber": "ELEV-001",
    "date": "2024-01-15",
    "vatRate": 20.0,
    "discountAmount": 0.0,
    "subtotal": 1000.0,
    "totalAmount": 1200.0,
    "status": "PENDING",
    "createdAt": "2024-01-15T10:00:00",
    "items": [
      {
        "id": 1,
        "partId": 1,
        "partName": "Part Name",
        "quantity": 2,
        "unitPrice": 500.0,
        "lineTotal": 1000.0
      }
    ]
  }
}
```

## Frontend Implementation

### 1. When Clicking "Edit" Button

```javascript
// Example for Elevator Edit
const handleEditElevator = async (elevatorId) => {
  try {
    // Step 1: Fetch complete data
    const response = await fetch(`${API_BASE_URL}/elevators/${elevatorId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });
    
    const apiResponse = await response.json();
    
    if (!apiResponse.success) {
      console.error('Failed to load elevator:', apiResponse.message);
      return;
    }
    
    // Step 2: Extract data from ApiResponse wrapper
    const elevatorData = apiResponse.data;
    
    // Step 3: Log for debugging
    console.log('Loaded elevator data:', elevatorData);
    
    // Step 4: Map to form state (field names must match API keys)
    setFormData({
      id: elevatorData.id,
      identityNumber: elevatorData.identityNumber || '',
      buildingName: elevatorData.buildingName || '',
      address: elevatorData.address || '',
      elevatorNumber: elevatorData.elevatorNumber || '',
      floorCount: elevatorData.floorCount || null,
      capacity: elevatorData.capacity || null,
      speed: elevatorData.speed || null,
      technicalNotes: elevatorData.technicalNotes || '',
      driveType: elevatorData.driveType || '',
      machineBrand: elevatorData.machineBrand || '',
      doorType: elevatorData.doorType || '',
      installationYear: elevatorData.installationYear || null,
      serialNumber: elevatorData.serialNumber || '',
      controlSystem: elevatorData.controlSystem || '',
      rope: elevatorData.rope || '',
      modernization: elevatorData.modernization || '',
      inspectionDate: elevatorData.inspectionDate || '', // Format: YYYY-MM-DD
      expiryDate: elevatorData.expiryDate || '' // Format: YYYY-MM-DD
    });
    
    // Step 5: Log form state after mapping
    console.log('Form data after mapping:', formData);
    
    // Step 6: Open edit modal/dialog
    setEditMode(true);
    setEditingId(elevatorData.id);
    
  } catch (error) {
    console.error('Error loading elevator:', error);
  }
};
```

### 2. Offer Edit (with items array)

```javascript
const handleEditOffer = async (offerId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/offers/${offerId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });
    
    const apiResponse = await response.json();
    const offerData = apiResponse.data;
    
    console.log('Loaded offer data:', offerData);
    
    // Map to form state
    setFormData({
      id: offerData.id,
      elevatorId: offerData.elevatorId || null,
      date: offerData.date || '', // Format: YYYY-MM-DD
      vatRate: offerData.vatRate || 20.0,
      discountAmount: offerData.discountAmount || 0.0,
      subtotal: offerData.subtotal || 0.0,
      totalAmount: offerData.totalAmount || 0.0,
      status: offerData.status || 'PENDING',
      items: offerData.items || [] // IMPORTANT: Include items array
    });
    
    // If using separate items state
    setOfferItems(offerData.items || []);
    
    console.log('Form data after mapping:', formData);
    setEditMode(true);
    
  } catch (error) {
    console.error('Error loading offer:', error);
  }
};
```

## Critical Mapping Rules

### Field Name Matching
**⚠️ IMPORTANT**: Frontend form field names MUST exactly match API response keys:

| API Key | Form Field Name |
|---------|----------------|
| `identityNumber` | `identityNumber` |
| `buildingName` | `buildingName` |
| `elevatorNumber` | `elevatorNumber` |
| `floorCount` | `floorCount` |
| `inspectionDate` | `inspectionDate` |
| `vatRate` | `vatRate` |
| `discountAmount` | `discountAmount` |
| `items` | `items` |

### Date Format Handling

**Backend returns**: `"2024-01-15"` (ISO 8601 date string)

**HTML date input expects**: `"2024-01-15"` (YYYY-MM-DD)

```javascript
// ✅ CORRECT - Direct assignment works
formData.inspectionDate = elevatorData.inspectionDate;

// ❌ WRONG - Don't convert if already in YYYY-MM-DD format
// formData.inspectionDate = new Date(elevatorData.inspectionDate).toISOString().split('T')[0];
```

### Null/Undefined Handling

Always provide defaults for nullable fields:

```javascript
// ✅ CORRECT - Handle null/undefined
buildingName: elevatorData.buildingName || '',
floorCount: elevatorData.floorCount ?? null, // Use ?? for null, not ||
capacity: elevatorData.capacity ?? null,
```

**Why `??` vs `||`?**
- `||` treats `0`, `false`, `''` as falsy → replaces with default
- `??` only treats `null` and `undefined` → preserves `0` and `false`

### Array Fields (Offer Items)

```javascript
// ✅ CORRECT - Preserve array or use empty array
items: offerData.items || []

// ❌ WRONG - Don't skip items
// if (offerData.items) { setItems(offerData.items); }
```

## Debugging Checklist

1. ✅ **Log API Response**
   ```javascript
   console.log('API Response:', apiResponse);
   ```

2. ✅ **Log Extracted Data**
   ```javascript
   console.log('Extracted data:', apiResponse.data);
   ```

3. ✅ **Log Form State After Mapping**
   ```javascript
   console.log('Form state:', formData);
   ```

4. ✅ **Verify Field Names Match**
   - Compare API response keys with form field names
   - Use browser DevTools → Network → Response tab

5. ✅ **Check Date Format**
   ```javascript
   console.log('Date value:', typeof formData.inspectionDate, formData.inspectionDate);
   // Should be: string "2024-01-15"
   ```

6. ✅ **Verify Array Fields**
   ```javascript
   console.log('Items array:', Array.isArray(formData.items), formData.items);
   ```

## Common Issues & Fixes

### Issue 1: Some fields are empty
**Cause**: Field names don't match API keys
**Fix**: Use exact API key names in form state

### Issue 2: Dates not showing
**Cause**: Date format conversion issue
**Fix**: Use date as-is from API (already YYYY-MM-DD)

### Issue 3: Items not loading (Offers)
**Cause**: Not mapping `items` array from response
**Fix**: Include `items: offerData.items || []` in form state

### Issue 4: Nullable fields showing as empty
**Cause**: Using `||` instead of `??`
**Fix**: Use `??` for numbers that can be 0

## Testing

### Test GET Endpoint Directly
```bash
# Test Elevator
curl -X GET "http://localhost:8080/api/elevators/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Test Offer
curl -X GET "http://localhost:8080/api/offers/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### Verify Response Structure
- Check that `data` object contains ALL expected fields
- Verify `items` array exists for offers
- Ensure dates are in `YYYY-MM-DD` format
- Confirm nullable fields can be `null` (not missing)

## Summary

✅ **Backend is correct** - Returns all fields in DTOs
✅ **API format is correct** - Wrapped in `ApiResponse<T>`
⚠️ **Frontend must**:
1. Extract `apiResponse.data`
2. Map ALL fields to form state
3. Use exact API key names
4. Handle null/undefined with `??` or `||`
5. Include array fields (items)
6. Log for debugging
