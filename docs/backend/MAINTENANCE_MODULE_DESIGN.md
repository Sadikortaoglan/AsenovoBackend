# Maintenance Module - Backend Design Document

## Overview
This document describes the complete backend implementation for the Maintenance Module with QR anti-fraud, planning, and execution tracking.

## 1. Domain Model

### Entities Created

#### A) MaintenanceTemplate
- **Purpose**: Defines reusable maintenance checklists
- **Fields**: id, name, status (ACTIVE/PASSIVE), frequencyDays (optional), createdAt, updatedAt
- **Relations**: One-to-Many with MaintenanceSection

#### B) MaintenanceSection
- **Purpose**: Groups related maintenance items (e.g., "Machine Room", "Cabin", "Shaft")
- **Fields**: id, templateId, name, sortOrder, createdAt
- **Relations**: Many-to-One with MaintenanceTemplate, One-to-Many with MaintenanceItem

#### C) MaintenanceItem
- **Purpose**: Individual checklist items under a section
- **Fields**: id, sectionId, title, description (optional), mandatory (boolean), allowPhoto, allowNote, sortOrder, isActive
- **Relations**: Many-to-One with MaintenanceSection

#### D) MaintenancePlan
- **Purpose**: Calendar assignment of maintenance tasks
- **Fields**: id, elevatorId, templateId, plannedDate, assignedTechnicianId (optional), status (PLANNED/IN_PROGRESS/COMPLETED/CANCELLED), createdAt
- **Relations**: Many-to-One with Elevator, MaintenanceTemplate, User (technician)

#### E) MaintenanceSession
- **Purpose**: Actual execution instance (created on QR start)
- **Fields**: id, planId (optional), elevatorId, templateId, technicianId, startAt, endAt (optional), status (IN_PROGRESS/COMPLETED/ABORTED), startedByQrScan, qrProofId, gpsLat/gpsLng (optional), deviceInfo, overallNote, signatureUrl
- **Relations**: Many-to-One with MaintenancePlan, Elevator, MaintenanceTemplate, User, QrProof

#### F) MaintenanceStepResult
- **Purpose**: Result for each maintenance item in a session
- **Fields**: id, sessionId, itemId, result (COMPLETED/ISSUE_FOUND/NOT_APPLICABLE), note (optional), createdAt, updatedAt
- **Relations**: Many-to-One with MaintenanceSession, MaintenanceItem
- **Unique Constraint**: One result per session-item pair

#### G) MaintenanceAttachment
- **Purpose**: Photos/videos attached to maintenance items or session
- **Fields**: id, sessionId, itemId (optional), fileUrl, fileType, createdAt
- **Relations**: Many-to-One with MaintenanceSession, MaintenanceItem (optional)

#### H) QrProof
- **Purpose**: Anti-fraud token validation
- **Fields**: id, elevatorId, tokenHash (unique), issuedAt, expiresAt, usedAt (optional), usedBy (technicianId), nonce, ip (optional), createdAt
- **Relations**: Many-to-One with Elevator, User

## 2. QR Anti-Fraud Flow

### Flow Sequence

1. **QR Code Generation** (Static, per elevator)
   - Format: `https://yourdomain/m/qr?e=ELEV-002&sig=<signature>`
   - Signature: HMAC-SHA256(elevatorPublicCode + secretKey)

2. **Token Issue** (POST /api/qr/issue-session-token)
   ```
   Request: {
     elevatorPublicCode: "ELEV-002",
     sig: "<signature>",
     deviceMeta: { deviceInfo, gpsLat, gpsLng }
   }
   Response: {
     qrToken: "<short-lived-token>",
     expiresAt: "<timestamp>"
   }
   ```
   - Validates signature
   - Issues JWT or random token (TTL: 2-3 minutes)
   - Stores as QrProof with expiresAt

3. **Session Start** (POST /api/maintenances/sessions/start)
   ```
   Request: {
     elevatorId: 123,
     templateId: 456,
     qrToken: "<token>",
     plannedDate: "2026-02-15" (optional)
   }
   ```
   - Validates qrToken is fresh & unused
   - Binds to technicianId (from SecurityContext)
   - Marks QrProof.usedAt = now()
   - Creates MaintenanceSession with startedByQrScan = true

4. **Session Completion** (POST /api/maintenances/sessions/{id}/finalize)
   - **MUST verify**: session has valid qrProofId
   - **MUST verify**: minimum duration (e.g., >= 3 minutes) or configurable
   - **MUST verify**: all mandatory items completed (unless explicitly justified)
   - Sets endAt, status = COMPLETED
   - If linked to plan, marks plan.status = COMPLETED

## 3. API Endpoints

### 3.1 Maintenance Items Management

#### Templates
- `GET /api/maintenance/templates` - List all templates
- `POST /api/maintenance/templates` - Create template
- `PUT /api/maintenance/templates/{id}` - Update template
- `POST /api/maintenance/templates/{id}/duplicate` - Duplicate template
- `DELETE /api/maintenance/templates/{id}` - Delete template (soft delete if needed)

#### Sections
- `GET /api/maintenance/templates/{id}/sections` - Get sections for template
- `POST /api/maintenance/templates/{id}/sections` - Create section
- `PUT /api/maintenance/sections/{id}` - Update section
- `DELETE /api/maintenance/sections/{id}` - Delete section

#### Items
- `POST /api/maintenance/sections/{id}/items` - Create item
- `PUT /api/maintenance/items/{id}` - Update item
- `PATCH /api/maintenance/items/{id}/toggle-active` - Toggle active status
- `POST /api/maintenance/templates/{id}/reorder` - Bulk reorder sections & items

### 3.2 Maintenance Planning

- `GET /api/maintenances/planning?from=YYYY-MM-DD&to=YYYY-MM-DD&regionId=optional`
  - Returns: Planned items grouped by date
  - Per elevator: buildingName, elevatorCode, templateName, assignedTechnicianName, status

- `POST /api/maintenances/plans/bulk`
  ```
  Body: [
    { elevatorId: 1, templateId: 2, plannedDate: "2026-02-15", assignedTechnicianId: 3 },
    ...
  ]
  ```

- `PATCH /api/maintenances/plans/{id}`
  - Move date, assign technician, cancel

- `GET /api/elevators/compact`
  - For left list in planner (id, identityNumber, buildingName)

### 3.3 Completed & Upcoming Lists

- `GET /api/maintenances/completed?from=&to=&q=&page=&size=`
  - Returns paginated list of completed maintenances
  - Filters: date range, search query (elevator code, building name)

- `GET /api/maintenances/upcoming?from=&to=&q=&page=&size=`
  - Returns paginated list of upcoming/planned maintenances

**Response DTO:**
```json
{
  "plannedDate": "2026-02-15",
  "elevatorCode": "ELEV-002",
  "buildingName": "Residential Complex",
  "templateName": "Monthly Maintenance",
  "assignedTechnician": "Ahmet Yılmaz",
  "statusChip": "PLANNED",
  "actionsAllowed": ["start", "cancel"]
}
```

### 3.4 Session Execution

- `GET /api/maintenances/sessions/{id}`
  - Returns: session + sections + items + existing results
  - Full execution context

- `PUT /api/maintenances/sessions/{id}/steps/{itemId}`
  ```
  Body: {
    result: "COMPLETED" | "ISSUE_FOUND" | "NOT_APPLICABLE",
    note: "optional note"
  }
  ```

- `POST /api/maintenances/sessions/{id}/attachments` (multipart)
  - Upload photo/video for item or session

- `GET /api/maintenances/sessions/{id}/report`
  - Detailed report for modal (all steps, attachments, notes)

- `POST /api/maintenances/sessions/{id}/finalize`
  ```
  Body: {
    overallNote: "optional",
    signature: "base64-encoded-signature" (optional)
  }
  ```
  - Validates mandatory items
  - Sets endAt, status = COMPLETED
  - Updates linked plan if exists

### 3.5 QR Anti-Fraud

- `POST /api/qr/issue-session-token`
  ```
  Request: {
    elevatorPublicCode: "ELEV-002",
    sig: "<signature>",
    deviceMeta: { deviceInfo, gpsLat, gpsLng }
  }
  Response: {
    qrToken: "<token>",
    expiresAt: "<timestamp>"
  }
  ```

## 4. Validation & Error Format

### Consistent ApiResponse
```json
{
  "success": true,
  "message": null,
  "data": { ... },
  "errors": {
    "fieldName": "Error message"
  }
}
```

### Validation Rules
- Phone patterns: `^(0?[0-9]{10})$` (10-11 digits, Turkish format)
- Required fields: Use `@NotBlank` or `@NotNull` with clear messages
- Enum mapping: Use `@Enumerated(EnumType.STRING)` with PostgreSQL enum types or VARCHAR + CHECK constraint

## 5. Performance & Indexes

### Indexes Created
- `maintenance_templates(status)`
- `maintenance_sections(template_id, sort_order)`
- `maintenance_items(section_id, sort_order)`
- `maintenance_plans(planned_date, status)`
- `maintenance_plans(elevator_id, planned_date)`
- `maintenance_plans(assigned_technician_id, planned_date)`
- `maintenance_sessions(technician_id, start_at)`
- `maintenance_sessions(qr_proof_id)`
- `qr_proofs(token_hash)` (unique)
- `qr_proofs(expires_at)`
- `maintenance_step_results(session_id, item_id)` (unique)

### Pagination
- All list endpoints support `page` and `size` parameters
- Default: page=0, size=20
- Use Spring Data `Pageable`

## 6. Implementation Status

### ✅ Completed
- [x] Domain Model (8 entities)
- [x] Database Migration (V15)
- [x] Indexes and constraints

### 🔄 In Progress
- [ ] Repository interfaces
- [ ] Service layer
- [ ] Controller endpoints
- [ ] DTOs (Request/Response)
- [ ] QR Anti-Fraud service
- [ ] Validation logic

### 📋 Next Steps
1. Create Repository interfaces for all entities
2. Implement QR Anti-Fraud service (QrProofService)
3. Implement MaintenanceTemplateService (CRUD + duplicate)
4. Implement MaintenancePlanService (planning + bulk operations)
5. Implement MaintenanceSessionService (execution flow)
6. Create DTOs for all endpoints
7. Create Controllers with proper validation
8. Add unit tests for critical flows

## 7. Sequence Flows

### A) Template Creation
1. POST /api/maintenance/templates → Create template
2. POST /api/maintenance/templates/{id}/sections → Add sections
3. POST /api/maintenance/sections/{id}/items → Add items to sections
4. POST /api/maintenance/templates/{id}/reorder → Reorder if needed

### B) Planning
1. GET /api/maintenances/planning?from=&to= → View calendar
2. GET /api/elevators/compact → Select elevators
3. POST /api/maintenances/plans/bulk → Assign multiple plans

### C) QR Start
1. Technician scans QR code
2. POST /api/qr/issue-session-token → Get token
3. POST /api/maintenances/sessions/start → Start session

### D) Execution
1. GET /api/maintenances/sessions/{id} → Load session context
2. PUT /api/maintenances/sessions/{id}/steps/{itemId} → Complete each step
3. POST /api/maintenances/sessions/{id}/attachments → Upload photos
4. POST /api/maintenances/sessions/{id}/finalize → Complete session

### E) Completed List
1. GET /api/maintenances/completed?from=&to= → List completed
2. GET /api/maintenances/sessions/{id}/report → View details

## 8. Notes for Frontend

### Field Mapping
- Use exact field names from DTOs (camelCase)
- Date format: ISO 8601 (YYYY-MM-DD for dates, YYYY-MM-DDTHH:mm:ss for timestamps)
- Enum values: UPPERCASE strings (e.g., "ACTIVE", "COMPLETED")

### Common Issues to Avoid
- **"Backend returns but FE doesn't show"**: Ensure DTO field names match exactly
- **Date format mismatch**: Always use ISO 8601
- **Enum mapping**: Use string values, not numeric
- **Null handling**: Check for null before accessing nested objects

### Required Headers
- `Content-Type: application/json` for JSON requests
- `Authorization: Bearer <token>` for authenticated endpoints
- `Content-Type: multipart/form-data` for file uploads

## 9. Security Considerations

- QR tokens expire in 2-3 minutes
- QR tokens can only be used once
- Session must have valid qrProofId to finalize
- Minimum session duration enforced (configurable, default 3 minutes)
- All endpoints require authentication (except QR token issue, which validates signature)
- Technician auto-assigned from SecurityContext (cannot be overridden)
