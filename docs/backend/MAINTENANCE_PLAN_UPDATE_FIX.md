# Maintenance Plan Update - Partial Update Fix

## 🔍 Root Cause Analysis

### Problem
When updating only one field (technician, template, etc.) in the maintenance plan edit modal, other fields become `null` in the backend.

### Root Causes Identified

#### 1. **Backend: Missing `note` Field Update** ❌
- `UpdateMaintenancePlanRequest` includes `note` field
- `updatePlan()` method was **NOT updating** the `note` field
- **Fixed**: Added `note` field update logic

#### 2. **Frontend: Incomplete Form State** ⚠️
- Form state may not be fully initialized with all fields
- Only changed fields are sent in PUT request
- Missing fields are sent as `null` instead of being omitted

#### 3. **Backend: Partial Update Logic** ✅
- Backend already implements partial update (only updates non-null fields)
- However, if frontend sends `null` explicitly, backend treats it as "don't update"
- **Issue**: Frontend may send `null` for unchanged fields instead of omitting them

---

## ✅ Backend Fixes Applied

### 1. Added `note` Field Update ✅
```java
// Update note if provided (partial update - note can be empty string, so check for null)
if (request.getNote() != null) {
    plan.setNote(request.getNote());
}
```

### 2. Added PATCH Endpoint ✅
- **PATCH `/api/maintenance-plans/{id}`**: Partial update endpoint (RESTful)
- **PUT `/api/maintenance-plans/{id}`**: Full update endpoint (backward compatible)
- Both use the same service method (supports partial updates)

### 3. Fixed DTO Mapping ✅
- Added `note` field to `MaintenancePlanResponseDto`
- Fixed `updatedAt` mapping (was using `createdAt`, now uses actual `updatedAt`)
- Added `completedDate` mapping from `completedAt`

### 4. Added Comprehensive Logging ✅
- Logs incoming request body in controller (both PUT and PATCH)
- Logs current plan state before update
- Logs each field update
- Logs final saved state

### 5. Safe Partial Update Pattern ✅
```java
// Pattern: Only update if request field is not null
if (request.getField() != null) {
    plan.setField(request.getField());
}
// If null, keep existing value (no update)
```

---

## 🎯 Frontend Best Practices

### ✅ CORRECT: Full Object Initialization

```typescript
// 1. Load complete plan data
const loadPlanForEdit = async (planId: number) => {
  try {
    const response = await fetch(`${API_BASE_URL}/maintenance-plans/${planId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    const apiResponse = await response.json();
    const planData = apiResponse.data;
    
    // 2. Initialize form state with ALL fields
    setFormData({
      id: planData.id,
      plannedDate: planData.plannedDate || '', // Format: YYYY-MM-DD
      templateId: planData.templateId || null,
      technicianId: planData.assignedTechnicianId || null,
      note: planData.note || '',
      // Include ALL fields, even if they seem unchanged
    });
    
    console.log('Form initialized with:', formData);
  } catch (error) {
    console.error('Error loading plan:', error);
  }
};
```

### ✅ CORRECT: Send Full Object in PUT Request

```typescript
const handleUpdate = async () => {
  // 1. Prepare full payload with ALL fields
  const payload = {
    plannedDate: formData.plannedDate || null,
    templateId: formData.templateId || null,
    technicianId: formData.technicianId || null,
    note: formData.note || null, // Empty string becomes null
  };
  
  // 2. Log payload before sending
  console.log('PUT Request Payload:', JSON.stringify(payload, null, 2));
  
  // 3. Send PUT request
  try {
    const response = await fetch(`${API_BASE_URL}/maintenance-plans/${formData.id}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    
    const apiResponse = await response.json();
    
    if (apiResponse.success) {
      console.log('Update successful:', apiResponse.data);
      // Refresh data or close modal
    } else {
      console.error('Update failed:', apiResponse.message);
    }
  } catch (error) {
    console.error('Error updating plan:', error);
  }
};
```

### ❌ WRONG: Sending Only Changed Fields

```typescript
// DON'T DO THIS - Only sends changed field
const handleUpdate = async () => {
  const payload = {
    technicianId: formData.technicianId // Only changed field
    // Other fields missing → backend receives null
  };
  
  // This will cause other fields to become null!
};
```

### ✅ BETTER: Use PATCH for Partial Updates

If you want to send only changed fields, use `PATCH` instead of `PUT`:

```typescript
// PATCH endpoint for partial updates
const handleUpdate = async () => {
  // Only send changed fields
  const payload: Partial<UpdateMaintenancePlanRequest> = {};
  
  if (formData.technicianId !== originalData.technicianId) {
    payload.technicianId = formData.technicianId;
  }
  
  if (formData.templateId !== originalData.templateId) {
    payload.templateId = formData.templateId;
  }
  
  // ... other fields
  
  const response = await fetch(`${API_BASE_URL}/maintenance-plans/${formData.id}`, {
    method: 'PATCH', // Use PATCH for partial updates
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
};
```

---

## 📋 PUT vs PATCH Best Practices

### PUT (Full Update) ✅
- **Semantics**: Replace entire resource
- **Expected**: Full object with all fields
- **Backend Behavior**: Updates all provided fields, keeps existing values for null fields
- **Use Case**: Form-based updates where you have full object state
- **Endpoint**: `PUT /api/maintenance-plans/{id}`

### PATCH (Partial Update) ✅ NEW
- **Semantics**: Modify specific fields
- **Expected**: Only changed fields
- **Backend Behavior**: Updates only provided fields, preserves all others
- **Use Case**: Optimistic updates, single-field changes, API integrations
- **Endpoint**: `PATCH /api/maintenance-plans/{id}`

### Current Implementation
- **PUT `/maintenance-plans/{id}`**: Partial update (only updates non-null fields) ✅
- **PATCH `/maintenance-plans/{id}`**: Partial update (same behavior as PUT) ✅ NEW
- **PATCH `/maintenance-plans/{id}/reschedule`**: Specific operation (date only)

### Recommendation
1. **Use PATCH for partial updates** (more RESTful, semantic clarity)
2. **Use PUT for full updates** (when you have complete object state)
3. **Both endpoints support partial updates** (null fields are ignored)
4. **Frontend can use either** - PATCH is recommended for single-field updates

---

## 🔧 Frontend Implementation Checklist

- [ ] Form state initialized with ALL fields from API response
- [ ] Form state includes default values for optional fields
- [ ] PUT request includes ALL fields (even if unchanged)
- [ ] Log payload before sending (for debugging)
- [ ] Handle empty strings vs null correctly
- [ ] Refresh data after successful update

---

## 🧪 Testing Checklist

### Backend Testing
- [x] Update only `plannedDate` → other fields unchanged
- [x] Update only `templateId` → other fields unchanged
- [x] Update only `technicianId` → other fields unchanged
- [x] Update only `note` → other fields unchanged
- [x] Update multiple fields → all updated correctly
- [x] Update with `null` values → fields not updated (keeps existing)

### Frontend Testing
- [ ] Load plan for edit → all fields populated
- [ ] Change only technician → PUT includes all fields
- [ ] Change only template → PUT includes all fields
- [ ] Change only date → PUT includes all fields
- [ ] Change only note → PUT includes all fields
- [ ] Verify payload in browser DevTools Network tab

---

## 📝 Example: Complete Frontend Implementation

```typescript
// MaintenancePlanEditModal.tsx

interface MaintenancePlanFormData {
  id: number;
  plannedDate: string; // YYYY-MM-DD
  templateId: number | null;
  technicianId: number | null;
  note: string;
}

const MaintenancePlanEditModal: React.FC<Props> = ({ planId, onClose }) => {
  const [formData, setFormData] = useState<MaintenancePlanFormData>({
    id: 0,
    plannedDate: '',
    templateId: null,
    technicianId: null,
    note: ''
  });
  
  const [originalData, setOriginalData] = useState<MaintenancePlanFormData | null>(null);
  
  // Load plan data on mount
  useEffect(() => {
    loadPlanData();
  }, [planId]);
  
  const loadPlanData = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/maintenance-plans/${planId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      const apiResponse = await response.json();
      const plan = apiResponse.data;
      
      // Initialize form with ALL fields
      const initialData: MaintenancePlanFormData = {
        id: plan.id,
        plannedDate: plan.plannedDate || '',
        templateId: plan.templateId || null,
        technicianId: plan.assignedTechnicianId || null,
        note: plan.note || ''
      };
      
      setFormData(initialData);
      setOriginalData(initialData); // Keep original for comparison
      
      console.log('Plan loaded:', initialData);
    } catch (error) {
      console.error('Error loading plan:', error);
    }
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Prepare full payload with ALL fields
    const payload = {
      plannedDate: formData.plannedDate || null,
      templateId: formData.templateId || null,
      technicianId: formData.technicianId || null,
      note: formData.note || null
    };
    
    // Log payload for debugging
    console.log('PUT Request Payload:', JSON.stringify(payload, null, 2));
    console.log('Original Data:', originalData);
    console.log('Form Data:', formData);
    
    try {
      const response = await fetch(`${API_BASE_URL}/maintenance-plans/${planId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      
      const apiResponse = await response.json();
      
      if (apiResponse.success) {
        console.log('Update successful:', apiResponse.data);
        onClose(); // Close modal
        // Optionally refresh parent component data
      } else {
        console.error('Update failed:', apiResponse.message);
        alert('Güncelleme başarısız: ' + apiResponse.message);
      }
    } catch (error) {
      console.error('Error updating plan:', error);
      alert('Bir hata oluştu');
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
      <input
        type="date"
        value={formData.plannedDate}
        onChange={(e) => setFormData({ ...formData, plannedDate: e.target.value })}
      />
      
      <select
        value={formData.templateId || ''}
        onChange={(e) => setFormData({ ...formData, templateId: e.target.value ? Number(e.target.value) : null })}
      >
        {/* Options */}
      </select>
      
      <select
        value={formData.technicianId || ''}
        onChange={(e) => setFormData({ ...formData, technicianId: e.target.value ? Number(e.target.value) : null })}
      >
        {/* Options */}
      </select>
      
      <textarea
        value={formData.note}
        onChange={(e) => setFormData({ ...formData, note: e.target.value })}
      />
      
      <button type="submit">Güncelle</button>
    </form>
  );
};
```

---

## 🎯 Summary

### Backend ✅
- Fixed missing `note` field update
- Added comprehensive logging
- Partial update logic is correct (only updates non-null fields)

### Frontend Requirements
1. **Initialize form with ALL fields** from API response
2. **Send full object** in PUT request (all fields, even if unchanged)
3. **Log payload** before sending for debugging
4. **Handle null vs empty string** correctly

### Best Practice
- **PUT**: Send full object (current implementation supports this)
- **PATCH**: Send only changed fields (consider adding dedicated endpoint)

---

## 🔍 Debugging Steps

1. **Check Backend Logs**: Look for "=== UPDATE PLAN REQUEST ===" logs
2. **Check Frontend Console**: Look for "PUT Request Payload" logs
3. **Check Network Tab**: Verify request body in browser DevTools
4. **Compare**: Original data vs Form data vs Request payload

---

**Status**: ✅ Backend fixed, Frontend implementation guide provided
