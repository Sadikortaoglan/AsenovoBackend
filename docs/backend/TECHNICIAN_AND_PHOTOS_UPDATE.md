# Backend Update: Technician Auto Assignment & Photo Validation

**Date:** 2026-02-10  
**Status:** ✅ All Changes Implemented

---

## CHANGES SUMMARY

### 1. Technician Auto Assignment ✅

**Business Rule:**
- When creating a Maintenance record, technician must be set automatically as the currently authenticated user
- Do NOT accept `technicianId` or `technicianName` from request body
- Technician must NOT be changeable via API

**Implementation:**

**MaintenanceService.createMaintenance()** (UPDATED)
- Removed: `if (dto.getTechnicianUserId() != null)` check
- Added: Auto-assignment from `SecurityContext`
- Gets authenticated user from `SecurityContextHolder.getContext().getAuthentication()`
- Loads `User` entity by username
- Sets technician automatically
- Throws error if user not authenticated

**MaintenanceService.updateMaintenance()** (UPDATED)
- Removed: Technician update logic
- Added: Comment explaining technician cannot be changed
- Technician remains as originally assigned during creation

**MaintenanceDto** (UPDATED)
- Added comment: `technicianUserId` and `technicianUsername` are READ-ONLY (response only)
- They are NOT accepted in request body

---

### 2. Minimum Photo Validation ✅

**Business Rule:**
- Maintenance must include at least 4 photos
- Validate multipart upload count >= 4
- If less than 4 → return validation error
- Store photos normally if validation passes

**Implementation:**

**New Endpoint:**
```
POST /api/maintenances/{id}/photos
Content-Type: multipart/form-data
Parameters:
  - files: MultipartFile[] (at least 4 files required)
```

**MaintenanceController.uploadPhotos()** (NEW)
- Validates maintenance exists
- **Validates minimum 4 photos**: `if (files == null || files.length < 4)`
- Returns error: `"Maintenance must include at least 4 photos. Provided: X"`
- Gets authenticated user from SecurityContext
- Saves each file using `FileStorageService`
- Creates `FileAttachment` records for each photo
- Returns success message with count

**FileStorageService** (NEW)
- Service for handling file uploads
- Saves files to local storage: `uploads/{entityType}/{entityId}/`
- Generates unique filenames using UUID
- Returns storage key (relative path)
- Generates file URL

**FileAttachment Entity** (UPDATED)
- Removed Lombok annotations
- Added manual getters/setters
- Added constructor

---

## CHANGED FILES

### Services
1. **MaintenanceService.java** (UPDATED)
   - `createMaintenance()`: Auto-assigns technician from SecurityContext
   - `updateMaintenance()`: Prevents technician changes

### Controllers
2. **MaintenanceController.java** (UPDATED)
   - Added: `uploadPhotos()` endpoint
   - Added: File upload validation (minimum 4 photos)
   - Added: Dependencies for file handling

### DTOs
3. **MaintenanceDto.java** (UPDATED)
   - Added comment: technician fields are READ-ONLY

### Services (New)
4. **FileStorageService.java** (NEW)
   - Handles file uploads to local storage
   - Generates unique filenames
   - Returns storage keys and URLs

### Entities
5. **FileAttachment.java** (UPDATED)
   - Removed Lombok
   - Added manual getters/setters
   - Added constructor

---

## API ENDPOINTS

### Existing (Updated)

**POST /api/maintenances**
- **Request Body:** `MaintenanceDto` (technicianUserId is ignored)
- **Response:** `MaintenanceDto` (includes auto-assigned technician)
- **Behavior:** Technician automatically set from authenticated user

**PUT /api/maintenances/{id}**
- **Request Body:** `MaintenanceDto` (technicianUserId is ignored)
- **Response:** `MaintenanceDto`
- **Behavior:** Technician cannot be changed

### New Endpoint

**POST /api/maintenances/{id}/photos**
- **Content-Type:** `multipart/form-data`
- **Parameters:**
  - `files`: `MultipartFile[]` (required, minimum 4 files)
- **Response:** 
  ```json
  {
    "success": true,
    "message": "Successfully uploaded X photos",
    "data": null
  }
  ```
- **Validation:**
  - Minimum 4 photos required
  - Error if less than 4: `"Maintenance must include at least 4 photos. Provided: X"`
- **Behavior:**
  - Saves files to `uploads/maintenance/{id}/`
  - Creates `FileAttachment` records
  - Links to authenticated user

---

## VALIDATION RULES

### Technician Assignment
- ✅ Technician is **automatically** assigned from SecurityContext
- ✅ Technician **cannot** be set via request body
- ✅ Technician **cannot** be changed via update endpoint
- ✅ Error if user not authenticated: `"User not authenticated. Cannot assign technician."`

### Photo Upload
- ✅ **Minimum 4 photos** required
- ✅ Validation error: `"Maintenance must include at least 4 photos. Provided: X"`
- ✅ Files saved to: `uploads/maintenance/{maintenanceId}/`
- ✅ Each file gets unique UUID filename
- ✅ `FileAttachment` records created automatically

---

## TESTING CHECKLIST

### Technician Auto Assignment
- [ ] Create maintenance without technicianUserId → Technician auto-assigned
- [ ] Create maintenance with technicianUserId in body → TechnicianUserId ignored, auto-assigned
- [ ] Update maintenance with technicianUserId → TechnicianUserId ignored, unchanged
- [ ] Create maintenance without authentication → Error: "User not authenticated"
- [ ] Verify technician is logged-in user

### Photo Upload
- [ ] Upload 4 photos → Success
- [ ] Upload 5+ photos → Success
- [ ] Upload 3 photos → Error: "Maintenance must include at least 4 photos. Provided: 3"
- [ ] Upload 0 photos → Error: "Maintenance must include at least 4 photos. Provided: 0"
- [ ] Upload to non-existent maintenance → Error: "Maintenance record not found"
- [ ] Verify files saved to correct directory
- [ ] Verify FileAttachment records created
- [ ] Verify uploadedBy is authenticated user

---

## EXAMPLE REQUESTS

### Create Maintenance (Technician Auto-Assigned)

```bash
POST /api/maintenances
Authorization: Bearer {token}
Content-Type: application/json

{
  "elevatorId": 1,
  "date": "2026-02-10",
  "labelType": "BLUE",
  "description": "Regular maintenance",
  "amount": 500.0,
  "isPaid": false
  // technicianUserId is NOT sent - will be auto-assigned
}
```

**Response:**
```json
{
  "success": true,
  "message": "Maintenance record successfully added",
  "data": {
    "id": 1,
    "elevatorId": 1,
    "date": "2026-02-10",
    "labelType": "BLUE",
    "description": "Regular maintenance",
    "technicianUserId": 2,  // Auto-assigned from authenticated user
    "technicianUsername": "john_doe",  // Auto-assigned
    "amount": 500.0,
    "isPaid": false
  }
}
```

### Upload Photos (Minimum 4 Required)

```bash
POST /api/maintenances/1/photos
Authorization: Bearer {token}
Content-Type: multipart/form-data

files: [photo1.jpg, photo2.jpg, photo3.jpg, photo4.jpg]
```

**Success Response:**
```json
{
  "success": true,
  "message": "Successfully uploaded 4 photos",
  "data": null
}
```

**Error Response (Less than 4):**
```json
{
  "success": false,
  "message": "Maintenance must include at least 4 photos. Provided: 3",
  "data": null,
  "errors": null
}
```

---

## FILE STRUCTURE

### Local Storage
```
uploads/
└── maintenance/
    └── {maintenanceId}/
        ├── {uuid1}.jpg
        ├── {uuid2}.jpg
        ├── {uuid3}.jpg
        └── {uuid4}.jpg
```

### Database
**file_attachments** table:
- `entity_type`: 'MAINTENANCE'
- `entity_id`: maintenance ID
- `storage_key`: 'maintenance/{id}/{uuid}.jpg'
- `uploaded_by_user_id`: authenticated user ID

---

## SECURITY NOTES

1. **Technician Assignment:**
   - Uses SecurityContext (JWT authentication)
   - Cannot be spoofed via request body
   - Ensures accountability

2. **Photo Upload:**
   - Requires authentication
   - Validates minimum count
   - Links to authenticated user
   - Files stored with unique names

---

## COMPILATION STATUS

✅ **BUILD SUCCESS**
- All files compile without errors
- No Lombok dependencies
- All manual getters/setters implemented

---

**All changes are backward compatible. Existing endpoints continue to work.**
