# Multipart/Form-Data Architecture - Complete Backend Specification

**Version:** 1.0  
**Last Updated:** 2026-02-12  
**System:** Sara Asansör Maintenance System

---

## 1️⃣ PROBLEM ANALYSIS

### Why @RequestBody Fails with multipart/form-data

**Root Cause:**
- `@RequestBody` expects `Content-Type: application/json`
- Spring's `HttpMessageConverter` chain includes `MappingJackson2HttpMessageConverter`
- This converter **only** handles `application/json` and `application/*+json`
- When `Content-Type: multipart/form-data` is received, Spring cannot find a matching converter
- Result: `HttpMediaTypeNotSupportedException` → HTTP 500

**Technical Flow:**
```
1. Frontend sends: Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
2. Spring dispatcher receives request
3. Spring checks @RequestBody annotation
4. Spring looks for HttpMessageConverter that accepts multipart/form-data
5. MappingJackson2HttpMessageConverter rejects (only accepts application/json)
6. No converter found → Exception thrown
```

### Why Spring Throws 500

**Exception Chain:**
```java
HttpMediaTypeNotSupportedException: 
  Content-Type 'multipart/form-data; boundary=...; charset=UTF-8' is not supported
    → No matching HttpMessageConverter
      → @RequestBody cannot deserialize multipart
        → HandlerMethodArgumentResolver fails
          → HTTP 500 Internal Server Error
```

### Difference Between Annotations

#### **@RequestBody**
- **Purpose:** Deserialize HTTP request body into Java object
- **Content-Type:** `application/json`, `application/xml`
- **Converter:** `MappingJackson2HttpMessageConverter` (for JSON)
- **Use Case:** JSON-only requests
- **Limitation:** Cannot handle multipart/form-data

#### **@RequestPart**
- **Purpose:** Extract individual parts from multipart/form-data
- **Content-Type:** `multipart/form-data`
- **Converter:** `MultipartHttpMessageConverter`
- **Use Case:** Multipart requests with named parts
- **Advantage:** Can handle JSON part + file parts together
- **Example:**
  ```java
  @RequestPart("data") MaintenanceDto dto,  // JSON part
  @RequestPart("photos") MultipartFile[] files  // File parts
  ```

#### **@ModelAttribute**
- **Purpose:** Bind form data (key-value pairs) to object
- **Content-Type:** `application/x-www-form-urlencoded`, `multipart/form-data`
- **Converter:** `ServletModelAttributeMethodProcessor`
- **Use Case:** HTML form submissions
- **Limitation:** Cannot handle complex nested JSON in multipart
- **Note:** Works for simple key-value pairs, not JSON blobs

---

## 2️⃣ CORRECT CONTROLLER CONTRACT

### POST /api/maintenances

**Exact Method Signature:**
```java
@PostMapping(
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Transactional
public ResponseEntity<ApiResponse<MaintenanceDto>> createMaintenance(
    @RequestPart(value = "data", required = true) @Valid MaintenanceDto dto,
    @RequestPart(value = "photos", required = true) MultipartFile[] photos) {
    
    // Validation: Minimum 4 photos
    if (photos == null || photos.length < 4) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "Maintenance must include at least 4 photos. Provided: " + 
                (photos != null ? photos.length : 0)
            ));
    }
    
    // Validate non-empty files
    long validPhotoCount = Arrays.stream(photos)
        .filter(file -> file != null && !file.isEmpty())
        .count();
    
    if (validPhotoCount < 4) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "Maintenance must include at least 4 valid photos. Valid photos: " + validPhotoCount
            ));
    }
    
    // Business logic: Create maintenance + save photos
    MaintenanceDto created = maintenanceService.createMaintenanceWithPhotos(dto, photos);
    
    return ResponseEntity.status(201)
        .body(ApiResponse.success("Maintenance created successfully with " + validPhotoCount + " photos", created));
}
```

**Key Annotations:**
- `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` - Accepts multipart
- `@RequestPart("data")` - Extracts JSON part named "data"
- `@RequestPart("photos")` - Extracts file array named "photos"
- `@Valid` - Validates MaintenanceDto
- `@Transactional` - Ensures atomicity

**Content-Type Expectations:**
- Request: `multipart/form-data; boundary=----WebKitFormBoundary...`
- Response: `application/json`

**Validation Behavior:**
1. **JSON Part Validation:**
   - `@Valid` triggers `MaintenanceDto` validation
   - Required fields checked
   - Type validation

2. **Photo Validation:**
   - Minimum 4 photos (business rule)
   - Non-empty file check
   - File size validation (application.yml: max-file-size)
   - File type validation (if implemented)

3. **Transaction Rollback:**
   - If photo upload fails → entire transaction rolls back
   - If maintenance creation fails → photos not saved
   - Atomic operation: all or nothing

**Error Response Structure:**
```json
{
  "success": false,
  "message": "Maintenance must include at least 4 photos. Provided: 2",
  "data": null,
  "errors": null
}
```

---

## 3️⃣ FRONTEND CONTRACT

### FormData Structure

**Exact Implementation:**
```javascript
const formData = new FormData();

// 1. JSON Part (REQUIRED - name must be "data")
const maintenanceData = {
  elevatorId: 1,
  date: "2026-02-12",
  amount: 1500.00,
  description: "Monthly maintenance",
  // ... other fields
};
formData.append("data", new Blob([JSON.stringify(maintenanceData)], {
  type: "application/json"
}));

// 2. Photo Parts (REQUIRED - name must be "photos", minimum 4)
photos.forEach((photoFile) => {
  formData.append("photos", photoFile); // Same key "photos" for all files
});

// 3. Send Request
fetch("/api/maintenances", {
  method: "POST",
  headers: {
    "Authorization": "Bearer " + token
    // DO NOT set Content-Type - browser sets it automatically with boundary
  },
  body: formData
});
```

**Key Names (CRITICAL):**
- JSON part: **`"data"`** (exact match required)
- Photo parts: **`"photos"`** (exact match required, array)

**Why Naming Matters:**
- `@RequestPart("data")` expects part name "data"
- `@RequestPart("photos")` expects part name "photos"
- Mismatch → `MissingServletRequestPartException`
- Example: If frontend sends `"maintenance"` instead of `"data"` → 400 Bad Request

**JSON Blob Format:**
```javascript
// CORRECT
formData.append("data", new Blob([JSON.stringify(obj)], {
  type: "application/json"
}));

// WRONG - This sends as text/plain
formData.append("data", JSON.stringify(obj));
```

**File Array Naming:**
```javascript
// CORRECT - All files use same key "photos"
formData.append("photos", file1);
formData.append("photos", file2);
formData.append("photos", file3);
formData.append("photos", file4);

// WRONG - Different keys
formData.append("photo1", file1);
formData.append("photo2", file2);
```

**Why Boundary Matters:**
- Browser automatically generates boundary: `----WebKitFormBoundary7MA4YWxkTrZu0gW`
- Boundary separates parts in multipart body
- **DO NOT** set `Content-Type` header manually
- Browser sets: `Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...`
- Manual boundary → parsing fails

**Why Charset Must Not Break Parsing:**
- Default: `multipart/form-data; boundary=...`
- With charset: `multipart/form-data; boundary=...; charset=UTF-8`
- Spring's `MultipartHttpMessageConverter` handles both
- **Issue:** Some frameworks add charset incorrectly
- **Solution:** Let browser set Content-Type automatically

**What Breaks if Naming Mismatches:**
- Wrong JSON part name → `MissingServletRequestPartException: Required part 'data' is not present`
- Wrong photo part name → `MissingServletRequestPartException: Required part 'photos' is not present`
- Missing parts → 400 Bad Request

---

## 4️⃣ VALIDATION RULES

### Minimum Photo Count Rule

**Business Rule:**
- Maintenance must include **minimum 4 photos**
- Validation occurs **before** any database operations
- Transaction rollback if validation fails

**Implementation:**
```java
// 1. Null check
if (photos == null || photos.length < 4) {
    throw new ValidationException("Minimum 4 photos required");
}

// 2. Non-empty check
long validCount = Arrays.stream(photos)
    .filter(file -> file != null && !file.isEmpty())
    .count();

if (validCount < 4) {
    throw new ValidationException("Minimum 4 valid photos required");
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Maintenance must include at least 4 photos. Provided: 2",
  "data": null,
  "errors": null
}
```

### Maximum File Size

**Configuration (application.yml):**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB      # Per file limit
      max-request-size: 50MB   # Total request limit (4 photos × 10MB = 40MB max)
```

**Validation:**
- Spring validates automatically before controller method executes
- If exceeded → `MaxUploadSizeExceededException`
- Error response: HTTP 413 Payload Too Large (or 400 Bad Request)

**Error Response:**
```json
{
  "success": false,
  "message": "File size exceeds maximum allowed size of 10MB",
  "data": null,
  "errors": null
}
```

### Allowed File Types

**Current Implementation:**
- No explicit file type validation
- Accepts any file type

**Recommended Validation:**
```java
private static final List<String> ALLOWED_TYPES = Arrays.asList(
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp"
);

for (MultipartFile photo : photos) {
    String contentType = photo.getContentType();
    if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
        throw new ValidationException(
            "Invalid file type: " + contentType + ". Allowed: JPEG, PNG, WEBP"
        );
    }
}
```

### Null Safety

**Validation Checks:**
1. **JSON Part:**
   - `@RequestPart(value = "data", required = true)` - Enforces presence
   - `@Valid` on `MaintenanceDto` - Validates fields

2. **Photo Array:**
   - `@RequestPart(value = "photos", required = true)` - Enforces presence
   - Null check: `if (photos == null)`
   - Empty check: `file.isEmpty()`
   - Individual file null check: `file != null`

**Null Safety Chain:**
```java
// 1. Part presence (Spring handles)
@RequestPart(value = "photos", required = true)

// 2. Array null check
if (photos == null) { ... }

// 3. Array length check
if (photos.length < 4) { ... }

// 4. Individual file checks
for (MultipartFile file : photos) {
    if (file == null || file.isEmpty()) {
        // Skip or error
    }
}
```

### Transaction Rollback Behavior

**Atomic Operation:**
```java
@Transactional
public MaintenanceDto createMaintenanceWithPhotos(MaintenanceDto dto, MultipartFile[] photos) {
    // 1. Create maintenance record
    Maintenance maintenance = maintenanceRepository.save(convertToEntity(dto));
    
    // 2. Save photos (if any fails, entire transaction rolls back)
    for (MultipartFile photo : photos) {
        String storageKey = fileStorageService.saveFile(photo, "MAINTENANCE", maintenance.getId());
        FileAttachment attachment = createAttachment(maintenance.getId(), photo, storageKey);
        fileAttachmentRepository.save(attachment);
    }
    
    // 3. Return DTO
    return convertToDto(maintenance);
}
```

**Rollback Scenarios:**
- Photo upload fails → Maintenance not created
- Maintenance creation fails → Photos not saved
- Database constraint violation → All changes rolled back
- File storage failure → Transaction rolled back

---

## 5️⃣ CONFIGURATION REQUIREMENTS

### Required Spring Boot Properties

**application.yml:**
```yaml
spring:
  servlet:
    multipart:
      enabled: true                    # Enable multipart support
      max-file-size: 10MB              # Per file limit
      max-request-size: 50MB          # Total request limit (4 photos × 10MB = 40MB)
      file-size-threshold: 2KB        # In-memory threshold (files larger written to disk)
      location: ${java.io.tmpdir}     # Temporary directory for file uploads
      resolve-lazily: false           # Resolve multipart immediately
```

**Explanation:**
- `enabled: true` - Required for multipart support
- `max-file-size` - Maximum size per file (10MB per photo)
- `max-request-size` - Maximum total request size (50MB for 4 photos + JSON)
- `file-size-threshold` - Files smaller than this kept in memory
- `location` - Temporary directory for file uploads
- `resolve-lazily: false` - Parse multipart immediately (better error handling)

### Tomcat Limits

**If using embedded Tomcat:**
```yaml
server:
  tomcat:
    max-http-post-size: 50MB  # Tomcat-specific limit (should match max-request-size)
```

**Note:** Not required if using external Tomcat (configured in server.xml)

---

## 6️⃣ RESPONSE FORMAT

### Success Response

**Standard Format:**
```json
{
  "success": true,
  "message": "Maintenance created successfully with 4 photos",
  "data": {
    "id": 1,
    "elevatorId": 1,
    "date": "2026-02-12",
    "amount": 1500.00,
    "description": "Monthly maintenance",
    "technicianUserId": 2,
    "technicianUsername": "technician1",
    "photos": [
      {
        "id": 1,
        "fileName": "photo1.jpg",
        "url": "/uploads/MAINTENANCE/1/photo1.jpg",
        "contentType": "image/jpeg",
        "size": 1024000
      },
      // ... 3 more photos
    ]
  },
  "errors": null
}
```

**HTTP Status:** `201 Created`

---

### Failure Examples

#### **Less Than 4 Photos Uploaded**

**Request:**
```
POST /api/maintenances
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

----WebKitFormBoundary...
Content-Disposition: form-data; name="data"
Content-Type: application/json

{"elevatorId": 1, "date": "2026-02-12", ...}
----WebKitFormBoundary...
Content-Disposition: form-data; name="photos"; filename="photo1.jpg"
Content-Type: image/jpeg

[binary data]
----WebKitFormBoundary...
Content-Disposition: form-data; name="photos"; filename="photo2.jpg"
Content-Type: image/jpeg

[binary data]
----WebKitFormBoundary----
```

**Response:**
```json
{
  "success": false,
  "message": "Maintenance must include at least 4 photos. Provided: 2",
  "data": null,
  "errors": null
}
```

**HTTP Status:** `400 Bad Request`

---

#### **Unsupported Content-Type**

**Request:**
```
POST /api/maintenances
Content-Type: application/json

{"elevatorId": 1, ...}
```

**Response:**
```json
{
  "success": false,
  "message": "Content-Type 'application/json' is not supported. Expected: multipart/form-data",
  "data": null,
  "errors": null
}
```

**HTTP Status:** `415 Unsupported Media Type` (or 400 Bad Request)

**Note:** Spring throws `HttpMediaTypeNotSupportedException`, but we catch and return 400

---

#### **Missing JSON Part**

**Request:**
```
POST /api/maintenances
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

----WebKitFormBoundary...
Content-Disposition: form-data; name="photos"; filename="photo1.jpg"
Content-Type: image/jpeg

[binary data]
----WebKitFormBoundary----
```

**Response:**
```json
{
  "success": false,
  "message": "Required part 'data' is not present",
  "data": null,
  "errors": null
}
```

**HTTP Status:** `400 Bad Request`

**Exception:** `MissingServletRequestPartException`

---

#### **Missing Photos Part**

**Request:**
```
POST /api/maintenances
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

----WebKitFormBoundary...
Content-Disposition: form-data; name="data"
Content-Type: application/json

{"elevatorId": 1, ...}
----WebKitFormBoundary----
```

**Response:**
```json
{
  "success": false,
  "message": "Required part 'photos' is not present",
  "data": null,
  "errors": null
}
```

**HTTP Status:** `400 Bad Request`

---

#### **File Size Exceeded**

**Request:**
```
POST /api/maintenances
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

[Request with file > 10MB]
```

**Response:**
```json
{
  "success": false,
  "message": "File size exceeds maximum allowed size of 10MB",
  "data": null,
  "errors": null
}
```

**HTTP Status:** `413 Payload Too Large` (or 400 Bad Request)

---

## 7️⃣ IMPLEMENTATION CHECKLIST

### Controller Changes

- [ ] Change `@PostMapping` to `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`
- [ ] Replace `@RequestBody` with `@RequestPart("data")`
- [ ] Add `@RequestPart("photos") MultipartFile[] photos`
- [ ] Add minimum 4 photos validation
- [ ] Add `@Transactional` annotation
- [ ] Update service method to accept photos

### Service Changes

- [ ] Create `createMaintenanceWithPhotos(MaintenanceDto dto, MultipartFile[] photos)` method
- [ ] Implement atomic transaction (maintenance + photos)
- [ ] Add file validation (size, type)
- [ ] Handle file storage errors

### Configuration

- [ ] Verify `spring.servlet.multipart.enabled: true`
- [ ] Set `max-file-size: 10MB`
- [ ] Set `max-request-size: 50MB`
- [ ] Configure file storage service

### Error Handling

- [ ] Catch `MissingServletRequestPartException`
- [ ] Catch `MaxUploadSizeExceededException`
- [ ] Catch `MultipartException`
- [ ] Return standardized error responses

---

## 8️⃣ COMPLETE IMPLEMENTATION

### Updated Controller Method

```java
@PostMapping(
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Transactional
public ResponseEntity<ApiResponse<MaintenanceDto>> createMaintenance(
    @RequestPart(value = "data", required = true) @Valid MaintenanceDto dto,
    @RequestPart(value = "photos", required = true) MultipartFile[] photos) {
    
    try {
        // Validation: Minimum 4 photos
        if (photos == null || photos.length < 4) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                    "Maintenance must include at least 4 photos. Provided: " + 
                    (photos != null ? photos.length : 0)
                ));
        }
        
        // Validate non-empty files
        long validPhotoCount = Arrays.stream(photos)
            .filter(file -> file != null && !file.isEmpty())
            .count();
        
        if (validPhotoCount < 4) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                    "Maintenance must include at least 4 valid photos. Valid photos: " + validPhotoCount
                ));
        }
        
        // Create maintenance with photos
        MaintenanceDto created = maintenanceService.createMaintenanceWithPhotos(dto, photos);
        
        return ResponseEntity.status(201)
            .body(ApiResponse.success(
                "Maintenance created successfully with " + validPhotoCount + " photos", 
                created
            ));
            
    } catch (MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Missing required part: " + e.getRequestPartName()));
    } catch (MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413)
            .body(ApiResponse.error("File size exceeds maximum allowed size of 10MB"));
    } catch (MultipartException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Multipart request processing failed: " + e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

### Required Imports

```java
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.bind.MissingServletRequestPartException;
import java.util.Arrays;
```

---

**End of Specification**
