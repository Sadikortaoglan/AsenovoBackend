# Maintenance Template Management - Backend API Specification

**Version:** 1.0  
**Last Updated:** 2026-02-12  
**System:** Sara Asansör Maintenance System

---

## 📋 Table of Contents

1. [Domain Model](#1-domain-model)
2. [API Contract](#2-api-contract)
3. [Business Rules](#3-business-rules)
4. [Transaction Rules](#4-transaction-rules)
5. [Validation Rules](#5-validation-rules)
6. [Response Format Standard](#6-response-format-standard)
7. [Security](#7-security)

---

## 1️⃣ DOMAIN MODEL

### 1.1 Entity Structures

#### **MaintenanceTemplate**

```java
@Entity
@Table(name = "maintenance_templates")
public class MaintenanceTemplate {
    private Long id;                    // Primary Key, Auto-generated
    private String name;                 // Required, VARCHAR(255), NOT NULL
    private TemplateStatus status;       // Enum: ACTIVE, PASSIVE, Default: ACTIVE
    private Integer frequencyDays;       // Optional, INTEGER, nullable
    private List<MaintenanceSection> sections; // One-to-Many, Cascade ALL, Orphan Removal
    private LocalDateTime createdAt;     // Auto-set, NOT NULL, NOT updatable
    private LocalDateTime updatedAt;     // Auto-updated on @PreUpdate, NOT NULL
}
```

**Database Table:**
```sql
CREATE TABLE maintenance_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (status IN ('ACTIVE', 'PASSIVE')),
    frequency_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Enum:**
```java
public enum TemplateStatus {
    ACTIVE,   // Template is active and can be used
    PASSIVE   // Template is inactive and cannot be assigned
}
```

---

#### **MaintenanceSection**

```java
@Entity
@Table(name = "maintenance_sections")
public class MaintenanceSection {
    private Long id;                     // Primary Key, Auto-generated
    private MaintenanceTemplate template; // Many-to-One, FK: template_id, NOT NULL
    private String name;                 // Required, VARCHAR(255), NOT NULL
    private Integer sortOrder;           // Required, INTEGER, NOT NULL, Default: 0
    private Boolean active;              // Required, BOOLEAN, NOT NULL, Default: true
    private List<MaintenanceItem> items; // One-to-Many, Cascade ALL, Orphan Removal
    private LocalDateTime createdAt;     // Auto-set, NOT NULL, NOT updatable
}
```

**Database Table:**
```sql
CREATE TABLE maintenance_sections (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL 
        REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

#### **MaintenanceItem**

```java
@Entity
@Table(name = "maintenance_items")
public class MaintenanceItem {
    private Long id;                     // Primary Key, Auto-generated
    private MaintenanceSection section; // Many-to-One, FK: section_id, NOT NULL
    private String title;                // Required, VARCHAR(255), NOT NULL
    private String description;          // Optional, TEXT, nullable
    private Boolean mandatory;           // Required, BOOLEAN, NOT NULL, Default: false
    private Boolean allowPhoto;          // Required, BOOLEAN, NOT NULL, Default: false
    private Boolean allowNote;           // Required, BOOLEAN, NOT NULL, Default: true
    private Integer sortOrder;           // Required, INTEGER, NOT NULL, Default: 0
    private Boolean isActive;            // Required, BOOLEAN, NOT NULL, Default: true
    private LocalDateTime createdAt;     // Auto-set, NOT NULL, NOT updatable
}
```

**Database Table:**
```sql
CREATE TABLE maintenance_items (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL 
        REFERENCES maintenance_sections(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    mandatory BOOLEAN NOT NULL DEFAULT false,
    allow_photo BOOLEAN NOT NULL DEFAULT false,
    allow_note BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

### 1.2 Relationships

#### **Template → Sections**
- **Type:** One-to-Many
- **Cascade:** `CascadeType.ALL`
- **Orphan Removal:** `true`
- **Fetch Type:** `LAZY`
- **Ordering:** `sortOrder ASC`
- **Foreign Key:** `template_id` → `maintenance_templates(id)`
- **Cascade Delete:** `ON DELETE CASCADE`
- **Behavior:** When template is deleted, all sections are automatically deleted

#### **Section → Items**
- **Type:** One-to-Many
- **Cascade:** `CascadeType.ALL`
- **Orphan Removal:** `true`
- **Fetch Type:** `LAZY`
- **Ordering:** `sortOrder ASC`
- **Foreign Key:** `section_id` → `maintenance_sections(id)`
- **Cascade Delete:** `ON DELETE CASCADE`
- **Behavior:** When section is deleted, all items are automatically deleted

#### **Template → Plans** (Referenced by)
- **Type:** One-to-Many (reverse)
- **Foreign Key:** `template_id` → `maintenance_templates(id)`
- **Cascade Delete:** `ON DELETE CASCADE`
- **Behavior:** When template is deleted, all maintenance plans using it are deleted

---

### 1.3 Database Indexes

```sql
-- No explicit indexes defined for template/section/item tables
-- Consider adding:
CREATE INDEX idx_maintenance_sections_template_id ON maintenance_sections(template_id);
CREATE INDEX idx_maintenance_items_section_id ON maintenance_items(section_id);
CREATE INDEX idx_maintenance_templates_status ON maintenance_templates(status);
```

---

## 2️⃣ API CONTRACT

### Base URL
```
http://localhost:8080/api
```

### Context Path
```
/api
```

---

### 2.1 Maintenance Templates

#### **GET /api/maintenance-templates**

**Description:** Get all maintenance templates

**Request:**
```
GET /api/maintenance-templates
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "Monthly Maintenance",
      "status": "ACTIVE",
      "frequencyDays": 30,
      "sections": [],
      "createdAt": "2026-02-11T10:00:00",
      "updatedAt": "2026-02-11T10:00:00"
    }
  ],
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Insufficient permissions

---

#### **GET /api/maintenance-templates/active**

**Description:** Get only active maintenance templates

**Request:**
```
GET /api/maintenance-templates/active
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "Monthly Maintenance",
      "status": "ACTIVE",
      "frequencyDays": 30,
      "sections": [],
      "createdAt": "2026-02-11T10:00:00",
      "updatedAt": "2026-02-11T10:00:00"
    }
  ],
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `401 Unauthorized` - Missing or invalid JWT token

---

#### **GET /api/maintenance-templates/{id}**

**Description:** Get template by ID with full sections and items

**Request:**
```
GET /api/maintenance-templates/1
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "status": "ACTIVE",
    "frequencyDays": 30,
    "sections": [
      {
        "id": 1,
        "name": "Electrical Check",
        "sortOrder": 0,
        "active": true,
        "items": [
          {
            "id": 1,
            "title": "Check voltage",
            "description": "Verify voltage levels",
            "mandatory": true,
            "allowPhoto": true,
            "allowNote": true,
            "sortOrder": 0,
            "isActive": true,
            "createdAt": "2026-02-11T10:00:00"
          }
        ],
        "createdAt": "2026-02-11T10:00:00"
      }
    ],
    "createdAt": "2026-02-11T10:00:00",
    "updatedAt": "2026-02-11T10:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Template not found
- `401 Unauthorized` - Missing or invalid JWT token

**Error Response:**
```json
{
  "success": false,
  "message": "Maintenance template not found",
  "data": null,
  "errors": null
}
```

---

#### **POST /api/maintenance-templates**

**Description:** Create a new maintenance template

**Request:**
```
POST /api/maintenance-templates
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Quarterly Maintenance",
  "status": "ACTIVE",
  "frequencyDays": 90
}
```

**Validation Rules:**
- `name`: Required, not empty, max 255 characters
- `status`: Optional, enum (ACTIVE, PASSIVE), default: ACTIVE
- `frequencyDays`: Optional, integer, nullable

**Response:**
```json
{
  "success": true,
  "message": "Template created successfully",
  "data": {
    "id": 2,
    "name": "Quarterly Maintenance",
    "status": "ACTIVE",
    "frequencyDays": 90,
    "sections": [],
    "createdAt": "2026-02-12T00:00:00",
    "updatedAt": "2026-02-12T00:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid JWT token

**Error Response:**
```json
{
  "success": false,
  "message": "Template name cannot be empty",
  "data": null,
  "errors": null
}
```

---

#### **PUT /api/maintenance-templates/{id}**

**Description:** Update an existing maintenance template

**Request:**
```
PUT /api/maintenance-templates/1
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Monthly Maintenance Updated",
  "status": "PASSIVE",
  "frequencyDays": 30
}
```

**Validation Rules:**
- `name`: Required, not empty, max 255 characters
- `status`: Required, enum (ACTIVE, PASSIVE)
- `frequencyDays`: Optional, integer, nullable

**Response:**
```json
{
  "success": true,
  "message": "Template updated successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance Updated",
    "status": "PASSIVE",
    "frequencyDays": 30,
    "sections": [],
    "createdAt": "2026-02-11T10:00:00",
    "updatedAt": "2026-02-12T00:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Template not found or validation error
- `401 Unauthorized` - Missing or invalid JWT token

---

#### **POST /api/maintenance-templates/{id}/duplicate**

**Description:** Duplicate a template with all sections and items

**Request:**
```
POST /api/maintenance-templates/1/duplicate
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Template duplicated successfully",
  "data": {
    "id": 3,
    "name": "Monthly Maintenance (Copy)",
    "status": "ACTIVE",
    "frequencyDays": 30,
    "sections": [
      {
        "id": 2,
        "name": "Electrical Check",
        "sortOrder": 0,
        "active": true,
        "items": [
          {
            "id": 2,
            "title": "Check voltage",
            "mandatory": true,
            "allowPhoto": true,
            "allowNote": true,
            "sortOrder": 0,
            "isActive": true
          }
        ]
      }
    ],
    "createdAt": "2026-02-12T00:00:00",
    "updatedAt": "2026-02-12T00:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Template not found
- `401 Unauthorized` - Missing or invalid JWT token

---

#### **DELETE /api/maintenance-templates/{id}**

**Description:** Delete a maintenance template (HARD DELETE - cascades to sections and items)

**Request:**
```
DELETE /api/maintenance-templates/1
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Template deleted successfully",
  "data": null,
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Template not found
- `401 Unauthorized` - Missing or invalid JWT token

**Business Rules:**
- **CASCADE DELETE:** All sections and items are automatically deleted
- **CASCADE DELETE:** All maintenance plans using this template are deleted
- **No Soft Delete:** This is a hard delete operation

---

### 2.2 Maintenance Sections

#### **GET /api/maintenance-templates/{templateId}/sections**

**Description:** Get all sections for a template

**Request:**
```
GET /api/maintenance-templates/1/sections
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "Electrical Check",
      "sortOrder": 0,
      "active": true,
      "items": [],
      "createdAt": "2026-02-11T10:00:00"
    }
  ],
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Template not found
- `401 Unauthorized` - Missing or invalid JWT token

---

#### **POST /api/maintenance-templates/{templateId}/sections**

**Description:** Create a new section for a template

**Request:**
```
POST /api/maintenance-templates/1/sections
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Mechanical Check",
  "sortOrder": 1,
  "active": true
}
```

**Validation Rules:**
- `name`: Required, not empty, max 255 characters
- `sortOrder`: Optional, integer, auto-assigned if null (max + 1)
- `active`: Optional, boolean, default: true

**Response:**
```json
{
  "success": true,
  "message": "Section created successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "status": "ACTIVE",
    "frequencyDays": 30,
    "sections": [
      {
        "id": 2,
        "name": "Mechanical Check",
        "sortOrder": 1,
        "active": true,
        "items": [],
        "createdAt": "2026-02-12T00:00:00"
      }
    ],
    "createdAt": "2026-02-11T10:00:00",
    "updatedAt": "2026-02-11T10:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `201 Created` - Success
- `400 Bad Request` - Validation error or template not found
- `401 Unauthorized` - Missing or invalid JWT token

**Important:** Returns full template with all sections and items (for frontend state refresh)

---

#### **PUT /api/maintenance-sections/{sectionId}**

**Description:** Update an existing section

**Request:**
```
PUT /api/maintenance-sections/1
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Electrical Check Updated",
  "sortOrder": 0,
  "active": false
}
```

**Validation Rules:**
- `name`: Optional, not empty if provided, max 255 characters
- `sortOrder`: Optional, integer
- `active`: Optional, boolean

**Response:**
```json
{
  "success": true,
  "message": "Section updated successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "status": "ACTIVE",
    "sections": [
      {
        "id": 1,
        "name": "Electrical Check Updated",
        "sortOrder": 0,
        "active": false,
        "items": []
      }
    ]
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Section not found or validation error
- `401 Unauthorized` - Missing or invalid JWT token

**Important:** Returns full template with all sections and items

---

#### **DELETE /api/maintenance-sections/{sectionId}**

**Description:** Delete a section (HARD DELETE - cascades to items)

**Request:**
```
DELETE /api/maintenance-sections/1
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Section deleted successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "sections": [],
    "createdAt": "2026-02-11T10:00:00",
    "updatedAt": "2026-02-11T10:00:00"
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Section not found
- `401 Unauthorized` - Missing or invalid JWT token

**Business Rules:**
- **CASCADE DELETE:** All items in the section are automatically deleted
- **No Soft Delete:** This is a hard delete operation
- **Returns:** Full template with updated sections list

---

### 2.3 Maintenance Items

#### **GET /api/maintenance-templates/maintenance-sections/{sectionId}/items**

**Description:** Get all items for a section

**Request:**
```
GET /api/maintenance-templates/maintenance-sections/1/items
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "title": "Check voltage",
      "description": "Verify voltage levels",
      "mandatory": true,
      "allowPhoto": true,
      "allowNote": true,
      "sortOrder": 0,
      "isActive": true,
      "createdAt": "2026-02-11T10:00:00"
    }
  ],
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Section not found
- `401 Unauthorized` - Missing or invalid JWT token

---

#### **POST /api/maintenance-sections/{sectionId}/items**

**Description:** Create a new item for a section

**Request:**
```
POST /api/maintenance-sections/1/items
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "title": "Check motor temperature",
  "description": "Verify motor temperature is within limits",
  "mandatory": true,
  "allowPhoto": true,
  "allowNote": true,
  "sortOrder": 1,
  "isActive": true
}
```

**Validation Rules:**
- `title`: Required, not empty, max 255 characters
- `description`: Optional, TEXT, nullable
- `mandatory`: Optional, boolean, default: false
- `allowPhoto`: Optional, boolean, default: false
- `allowNote`: Optional, boolean, default: true
- `sortOrder`: Optional, integer, auto-assigned if null (max + 1)
- `isActive`: Optional, boolean, default: true

**Response:**
```json
{
  "success": true,
  "message": "Item created successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "sections": [
      {
        "id": 1,
        "name": "Electrical Check",
        "items": [
          {
            "id": 2,
            "title": "Check motor temperature",
            "description": "Verify motor temperature is within limits",
            "mandatory": true,
            "allowPhoto": true,
            "allowNote": true,
            "sortOrder": 1,
            "isActive": true,
            "createdAt": "2026-02-12T00:00:00"
          }
        ]
      }
    ]
  },
  "errors": null
}
```

**Status Codes:**
- `201 Created` - Success
- `400 Bad Request` - Validation error or section not found
- `401 Unauthorized` - Missing or invalid JWT token

**Important:** Returns full template with all sections and items

---

#### **PUT /api/maintenance-items/{itemId}**

**Description:** Update an existing item

**Request:**
```
PUT /api/maintenance-items/1
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "title": "Check voltage updated",
  "description": "Updated description",
  "mandatory": false,
  "allowPhoto": false,
  "allowNote": true,
  "sortOrder": 0,
  "isActive": true
}
```

**Validation Rules:**
- `title`: Optional, not empty if provided, max 255 characters
- `description`: Optional, TEXT, nullable
- `mandatory`: Optional, boolean
- `allowPhoto`: Optional, boolean
- `allowNote`: Optional, boolean
- `sortOrder`: Optional, integer
- `isActive`: Optional, boolean

**Response:**
```json
{
  "success": true,
  "message": "Item updated successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "sections": [
      {
        "id": 1,
        "items": [
          {
            "id": 1,
            "title": "Check voltage updated",
            "description": "Updated description",
            "mandatory": false,
            "allowPhoto": false,
            "allowNote": true,
            "sortOrder": 0,
            "isActive": true
          }
        ]
      }
    ]
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Item not found or validation error
- `401 Unauthorized` - Missing or invalid JWT token

**Important:** Returns full template with all sections and items

---

#### **DELETE /api/maintenance-items/{itemId}**

**Description:** Delete an item (HARD DELETE)

**Request:**
```
DELETE /api/maintenance-items/1
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Item deleted successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "sections": [
      {
        "id": 1,
        "items": []
      }
    ]
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Item not found
- `401 Unauthorized` - Missing or invalid JWT token

**Business Rules:**
- **No Soft Delete:** This is a hard delete operation
- **Returns:** Full template with updated items list

---

#### **PATCH /api/maintenance-items/{itemId}/toggle-active**

**Description:** Toggle item active status

**Request:**
```
PATCH /api/maintenance-items/1/toggle-active
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Item status updated",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "sections": [
      {
        "id": 1,
        "items": [
          {
            "id": 1,
            "isActive": false
          }
        ]
      }
    ]
  },
  "errors": null
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Item not found
- `401 Unauthorized` - Missing or invalid JWT token

**Important:** Returns full template with all sections and items

---

## 3️⃣ BUSINESS RULES

### 3.1 Template Deletion

**Rule:** When a template is deleted:
- ✅ All sections are **automatically deleted** (CASCADE)
- ✅ All items are **automatically deleted** (CASCADE)
- ✅ All maintenance plans using this template are **automatically deleted** (CASCADE)
- ❌ **No soft delete** - this is a hard delete operation
- ⚠️ **Warning:** This operation is irreversible

**Implementation:**
```java
@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
private List<MaintenanceSection> sections;
```

**Database:**
```sql
ON DELETE CASCADE
```

---

### 3.2 Section Deletion

**Rule:** When a section is deleted:
- ✅ All items in the section are **automatically deleted** (CASCADE)
- ❌ **No soft delete** - this is a hard delete operation
- ✅ Section can be deleted even if items exist (items are cascade deleted)

**Implementation:**
```java
@OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
private List<MaintenanceItem> items;
```

**Database:**
```sql
ON DELETE CASCADE
```

---

### 3.3 Item Deletion

**Rule:** When an item is deleted:
- ❌ **No soft delete** - this is a hard delete operation
- ✅ Item can be deleted independently
- ⚠️ **Note:** If item is referenced in maintenance sessions, those references may become invalid

---

### 3.4 Template Status Rules

**Rule:** Inactive templates cannot be assigned to new maintenance plans
- ✅ Only `ACTIVE` templates appear in `/api/maintenance-templates/active`
- ✅ Frontend should filter by `status = ACTIVE` when creating plans
- ⚠️ Existing plans with `PASSIVE` templates remain valid

**Validation:**
- No backend validation prevents assigning `PASSIVE` templates (frontend responsibility)
- Consider adding validation in `MaintenancePlanService.createPlan()`

---

### 3.5 Section Active Status

**Rule:** Section `active` field controls visibility
- ✅ Inactive sections are not hidden from API responses
- ✅ Frontend should filter by `active = true` if needed
- ⚠️ No cascade effect on items when section is deactivated

---

### 3.6 Item Active Status

**Rule:** Item `isActive` field controls visibility
- ✅ Inactive items are not hidden from API responses
- ✅ Frontend should filter by `isActive = true` if needed
- ✅ Can be toggled via `PATCH /api/maintenance-items/{id}/toggle-active`

---

### 3.7 Order Index Logic

**Rule:** `sortOrder` auto-assignment
- ✅ If `sortOrder` is `null` on create, it's auto-assigned as `max(existing) + 1`
- ✅ Manual `sortOrder` values are accepted
- ✅ No uniqueness constraint - multiple items can have same `sortOrder`
- ✅ Ordering is done by `sortOrder ASC` in queries

**Implementation:**
```java
if (section.getSortOrder() == null) {
    List<MaintenanceSection> existing = repository.findByTemplateIdOrderBySortOrderAsc(templateId);
    int maxSortOrder = existing.stream()
        .mapToInt(MaintenanceSection::getSortOrder)
        .max()
        .orElse(-1);
    section.setSortOrder(maxSortOrder + 1);
}
```

---

## 4️⃣ TRANSACTION RULES

### 4.1 @Transactional Operations

**All write operations are `@Transactional`:**

```java
@Service
@Transactional  // Class-level default
public class MaintenanceTemplateService {
    
    @Transactional  // Explicit for clarity
    public MaintenanceTemplate createSection(Long templateId, MaintenanceSection section) {
        // ...
        sectionRepository.save(section);
        sectionRepository.flush(); // Force immediate persistence
        return getTemplateById(templateId);
    }
}
```

**Transactional Methods:**
- ✅ `createTemplate()` - Class-level `@Transactional`
- ✅ `updateTemplate()` - Class-level `@Transactional`
- ✅ `duplicateTemplate()` - Class-level `@Transactional`
- ✅ `deleteTemplate()` - Class-level `@Transactional`
- ✅ `createSection()` - Explicit `@Transactional`
- ✅ `updateSection()` - Explicit `@Transactional`
- ✅ `deleteSection()` - Explicit `@Transactional`
- ✅ `createItem()` - Explicit `@Transactional`
- ✅ `updateItem()` - Explicit `@Transactional`
- ✅ `deleteItem()` - Explicit `@Transactional`
- ✅ `toggleItemActive()` - Explicit `@Transactional`

**Read Operations:**
- ❌ `getAllTemplates()` - No transaction (read-only)
- ❌ `getActiveTemplates()` - No transaction (read-only)
- ❌ `getTemplateById()` - No transaction (read-only)
- ❌ `getSectionsByTemplateId()` - No transaction (read-only)
- ❌ `getItemsBySectionId()` - No transaction (read-only)

---

### 4.2 flush() Usage

**Rule:** `flush()` is called after all write operations to force immediate persistence

**When flush() is used:**
- ✅ After `createSection()` - `sectionRepository.flush()`
- ✅ After `updateSection()` - `sectionRepository.flush()`
- ✅ After `deleteSection()` - `sectionRepository.flush()`
- ✅ After `createItem()` - `itemRepository.flush()`
- ✅ After `updateItem()` - `itemRepository.flush()`
- ✅ After `deleteItem()` - `itemRepository.flush()`
- ✅ After `toggleItemActive()` - `itemRepository.flush()`

**Why flush()?**
- Ensures database state is immediately consistent
- Allows subsequent queries to see the changes
- Prevents stale data in `getTemplateById()` calls

---

### 4.3 Optimistic Locking

**Current Implementation:**
- ❌ **No optimistic locking** implemented
- ❌ No `@Version` field in entities
- ⚠️ Concurrent updates may overwrite each other

**Recommendation:**
```java
@Version
private Long version; // Add to entities for optimistic locking
```

---

### 4.4 Concurrent Update Prevention

**Current Implementation:**
- ❌ **No explicit locking** mechanism
- ⚠️ Last-write-wins behavior
- ⚠️ Race conditions possible in high-concurrency scenarios

**Recommendation:**
- Implement optimistic locking with `@Version`
- Or use pessimistic locking: `@Lock(LockModeType.PESSIMISTIC_WRITE)`

---

## 5️⃣ VALIDATION RULES

### 5.1 Required Fields

#### **MaintenanceTemplate**
- ✅ `name` - Required, not null, not empty
- ❌ `status` - Optional, default: ACTIVE
- ❌ `frequencyDays` - Optional, nullable
- ✅ `createdAt` - Auto-set, not null
- ✅ `updatedAt` - Auto-set, not null

#### **MaintenanceSection**
- ✅ `template` - Required, not null (FK)
- ✅ `name` - Required, not null, not empty
- ✅ `sortOrder` - Required, not null (auto-assigned if null)
- ✅ `active` - Required, not null, default: true
- ✅ `createdAt` - Auto-set, not null

#### **MaintenanceItem**
- ✅ `section` - Required, not null (FK)
- ✅ `title` - Required, not null, not empty
- ❌ `description` - Optional, nullable
- ✅ `mandatory` - Required, not null, default: false
- ✅ `allowPhoto` - Required, not null, default: false
- ✅ `allowNote` - Required, not null, default: true
- ✅ `sortOrder` - Required, not null (auto-assigned if null)
- ✅ `isActive` - Required, not null, default: true
- ✅ `createdAt` - Auto-set, not null

---

### 5.2 Max Length Constraints

- `template.name` - **VARCHAR(255)** - Max 255 characters
- `section.name` - **VARCHAR(255)** - Max 255 characters
- `item.title` - **VARCHAR(255)** - Max 255 characters
- `item.description` - **TEXT** - No explicit limit (PostgreSQL TEXT)

---

### 5.3 Duplicate Name Prevention

**Current Implementation:**
- ❌ **No uniqueness constraint** on template names
- ❌ **No uniqueness constraint** on section names within template
- ❌ **No uniqueness constraint** on item titles within section
- ⚠️ Duplicate names are allowed

**Recommendation:**
```sql
-- Add unique constraint on template name
ALTER TABLE maintenance_templates 
ADD CONSTRAINT uk_template_name UNIQUE (name);

-- Add unique constraint on section name within template
ALTER TABLE maintenance_sections 
ADD CONSTRAINT uk_section_name_template UNIQUE (template_id, name);

-- Add unique constraint on item title within section
ALTER TABLE maintenance_items 
ADD CONSTRAINT uk_item_title_section UNIQUE (section_id, title);
```

---

### 5.4 Order Index Logic

**Auto-Assignment Rules:**
- ✅ If `sortOrder` is `null` on create, it's set to `max(existing) + 1`
- ✅ If `sortOrder` is provided, it's used as-is
- ✅ No validation for duplicate `sortOrder` values
- ✅ No validation for negative `sortOrder` values
- ✅ Ordering is done by `sortOrder ASC` in queries

**Example:**
```java
// Existing sections: [0, 1, 2]
// New section with sortOrder=null → sortOrder=3
// New section with sortOrder=5 → sortOrder=5 (accepted)
```

---

### 5.5 Null Safety

**Validation Checks:**
- ✅ `name` cannot be `null` or empty (trimmed)
- ✅ `title` cannot be `null` or empty (trimmed)
- ✅ Foreign keys (`template`, `section`) are validated to exist
- ✅ Boolean fields default to safe values if `null`

**Implementation:**
```java
if (section.getName() == null || section.getName().trim().isEmpty()) {
    throw new RuntimeException("Section name cannot be empty");
}
```

---

## 6️⃣ RESPONSE FORMAT STANDARD

### 6.1 Success Response

**Standard Format:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "errors": null
}
```

**Examples:**

**List Response:**
```json
{
  "success": true,
  "message": null,
  "data": [
    { "id": 1, "name": "Template 1" },
    { "id": 2, "name": "Template 2" }
  ],
  "errors": null
}
```

**Single Object Response:**
```json
{
  "success": true,
  "message": "Template created successfully",
  "data": {
    "id": 1,
    "name": "Monthly Maintenance",
    "status": "ACTIVE"
  },
  "errors": null
}
```

**Delete Response:**
```json
{
  "success": true,
  "message": "Template deleted successfully",
  "data": null,
  "errors": null
}
```

---

### 6.2 Error Response

**Standard Format:**
```json
{
  "success": false,
  "message": "Error message here",
  "data": null,
  "errors": null
}
```

**Examples:**

**Validation Error:**
```json
{
  "success": false,
  "message": "Section name cannot be empty",
  "data": null,
  "errors": null
}
```

**Not Found Error:**
```json
{
  "success": false,
  "message": "Maintenance template not found",
  "data": null,
  "errors": null
}
```

**Foreign Key Error:**
```json
{
  "success": false,
  "message": "Maintenance template not found: 999",
  "data": null,
  "errors": null
}
```

---

### 6.3 HTTP Status Codes

**Success Codes:**
- `200 OK` - GET, PUT, DELETE operations
- `201 Created` - POST operations (create section/item)

**Error Codes:**
- `400 Bad Request` - Validation errors, not found errors
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Insufficient permissions (if role-based access implemented)

**Note:** All errors return `400 Bad Request` with error message in response body

---

## 7️⃣ SECURITY

### 7.1 Authentication

**All endpoints require JWT authentication:**
- ✅ JWT token must be provided in `Authorization` header
- ✅ Format: `Authorization: Bearer <JWT_TOKEN>`
- ✅ Token validation handled by `JwtAuthenticationFilter`

**Security Config:**
```java
.anyRequest().authenticated()
```

---

### 7.2 Authorization

**Current Implementation:**
- ❌ **No role-based access control** implemented
- ❌ No `@PreAuthorize` annotations
- ✅ All authenticated users can access all endpoints

**Recommendation:**
```java
@PreAuthorize("hasRole('PATRON')")
@PostMapping
public ResponseEntity<ApiResponse<MaintenanceTemplate>> createTemplate(...) {
    // Only PATRON role can create templates
}
```

---

### 7.3 Who Can Create Templates?

**Current Rule:**
- ✅ **Any authenticated user** can create templates
- ⚠️ No role restriction

**Recommended Rule:**
- ✅ Only `PATRON` role should create/edit/delete templates
- ✅ `PERSONEL` role should only view templates

---

### 7.4 Role-Based Access (Recommended)

**Template Operations:**
- `GET /api/maintenance-templates` - All authenticated users
- `GET /api/maintenance-templates/active` - All authenticated users
- `GET /api/maintenance-templates/{id}` - All authenticated users
- `POST /api/maintenance-templates` - **PATRON only**
- `PUT /api/maintenance-templates/{id}` - **PATRON only**
- `DELETE /api/maintenance-templates/{id}` - **PATRON only**
- `POST /api/maintenance-templates/{id}/duplicate` - **PATRON only**

**Section Operations:**
- `GET /api/maintenance-templates/{templateId}/sections` - All authenticated users
- `POST /api/maintenance-templates/{templateId}/sections` - **PATRON only**
- `PUT /api/maintenance-sections/{sectionId}` - **PATRON only**
- `DELETE /api/maintenance-sections/{sectionId}` - **PATRON only**

**Item Operations:**
- `GET /api/maintenance-templates/maintenance-sections/{sectionId}/items` - All authenticated users
- `POST /api/maintenance-sections/{sectionId}/items` - **PATRON only**
- `PUT /api/maintenance-items/{itemId}` - **PATRON only**
- `DELETE /api/maintenance-items/{itemId}` - **PATRON only**
- `PATCH /api/maintenance-items/{itemId}/toggle-active` - **PATRON only**

---

## 📝 Implementation Notes

### Full Template Response Pattern

**All create/update/delete operations return full template:**
- ✅ Frontend doesn't need to make additional GET request
- ✅ State is immediately consistent
- ✅ Reduces API calls

**Example:**
```java
@PostMapping("/{templateId}/sections")
public ResponseEntity<ApiResponse<MaintenanceTemplate>> createSection(...) {
    MaintenanceTemplate updatedTemplate = templateService.createSection(templateId, section);
    return ResponseEntity.status(201).body(ApiResponse.success("Section created successfully", updatedTemplate));
}
```

---

### Eager Loading Strategy

**For full template responses:**
```java
@Query("SELECT t FROM MaintenanceTemplate t " +
       "LEFT JOIN FETCH t.sections s " +
       "LEFT JOIN FETCH s.items " +
       "WHERE t.id = :id " +
       "ORDER BY s.sortOrder ASC")
Optional<MaintenanceTemplate> findByIdWithSectionsAndItems(@Param("id") Long id);
```

**Prevents:**
- LazyInitializationException
- N+1 query problems
- Multiple database round trips

---

## 🔧 Future Enhancements

1. **Add optimistic locking** with `@Version` field
2. **Add unique constraints** on names/titles
3. **Implement role-based access control** with `@PreAuthorize`
4. **Add soft delete** option for templates
5. **Add audit fields** (createdBy, updatedBy)
6. **Add validation annotations** (`@NotNull`, `@NotBlank`, `@Size`)
7. **Add API versioning** (`/api/v1/maintenance-templates`)
8. **Add pagination** for list endpoints
9. **Add filtering** by status, active flags
10. **Add bulk operations** (bulk delete, bulk update)

---

**End of Specification**
