# Backend Update & Fix Summary
## Elevator Maintenance System - Feature Updates

**Date:** 2026-01-22  
**Status:** ✅ All Changes Implemented

---

## CHANGED FILES

### 1. Migrations (Database Schema)

**V9__add_label_system_and_manager_fields.sql**
- Added `label_type` enum (GREEN, BLUE, YELLOW, RED)
- Added `label_date` column to elevators
- Added `label_type` column to elevators and maintenances
- Added `status` enum (ACTIVE, EXPIRED) to elevators
- Added manager fields: `manager_name`, `manager_tc_identity_no`, `manager_phone`, `manager_email`
- Backfilled existing data

**V10__create_building_and_current_account.sql**
- Created `buildings` table
- Created `current_accounts` table
- Added `building_id` foreign key to elevators

**V11__create_revision_offers.sql**
- Created `revision_offers` table
- Created `revision_offer_status` enum (DRAFT, SENT, APPROVED, REJECTED, CONVERTED_TO_SALE)

### 2. Entities

**LabelType.java** (NEW)
- Enum: GREEN, BLUE, YELLOW, RED

**Elevator.java** (UPDATED)
- Added: `labelDate`, `labelType`, `status` (enum)
- Added: `managerName`, `managerTcIdentityNo`, `managerPhone`, `managerEmail`
- Updated constructor and getters/setters

**Maintenance.java** (UPDATED)
- Added: `labelType` field

**Building.java** (NEW)
- Fields: id, name, address, city, district

**CurrentAccount.java** (NEW)
- Fields: id, building, name, authorizedPerson, phone, debt, credit, balance
- Auto-calculates balance = credit - debt

**RevisionOffer.java** (NEW)
- Fields: id, elevator, building, currentAccount, partsTotal, laborTotal, totalPrice, status
- Auto-calculates totalPrice = partsTotal + laborTotal

### 3. Repositories

**BuildingRepository.java** (NEW)
- JpaRepository<Building, Long>

**CurrentAccountRepository.java** (NEW)
- JpaRepository<CurrentAccount, Long>
- Custom: `findByBuilding`, `findByBuildingId`

**RevisionOfferRepository.java** (NEW)
- JpaRepository<RevisionOffer, Long>
- Custom: `findByElevatorId`, `findByBuildingId`, `findByCurrentAccountId`, `findByStatus`

### 4. Services

**LabelDurationCalculator.java** (NEW - Utility)
- `getLabelDurationMonths(LabelType)`: Returns months (BLUE=12, GREEN=24, YELLOW=6, RED=1)
- `calculateEndDate(labelDate, labelType)`: endDate = labelDate + duration
- `calculateStatus(endDate)`: if (today > endDate) → EXPIRED, else → ACTIVE

**ElevatorService.java** (UPDATED)
- `createElevator`: Now uses `labelDate` + `labelType` to calculate `endDate` and `status`
- `updateElevator`: Recalculates `endDate` and `status` if `labelDate` or `labelType` changed
- `mapDtoToEntity`: Maps new fields (labelDate, labelType, status, manager fields)

**MaintenanceService.java** (UPDATED)
- `createMaintenance`: 
  - Accepts `labelType` in DTO
  - **AUTO-UPDATES elevator**: Sets elevator's `labelType`, `labelDate`, `endDate`, and `status`

**BuildingService.java** (NEW)
- `createBuilding`: Auto-creates CurrentAccount
- `updateBuilding`: Ensures CurrentAccount exists

**CurrentAccountService.java** (NEW)
- `createForBuilding`: Auto-creates account when building is created
- `updateBalance`: Updates debt/credit and recalculates balance

**RevisionOfferService.java** (NEW)
- `createRevisionOffer`: Creates offer, validates relationships
- `updateRevisionOffer`: Updates (only DRAFT/SENT)
- `convertToSale`: Converts APPROVED offer to sale, updates current account debt

### 5. DTOs

**ElevatorDto.java** (UPDATED)
- Added: `labelDate`, `labelType`, `status`
- Added: `managerName`, `managerTcIdentityNo`, `managerPhone`, `managerEmail`
- Updated `fromEntity` to map new fields

**MaintenanceDto.java** (UPDATED)
- Added: `labelType` field
- Updated `fromEntity` to map labelType

**BuildingDto.java** (NEW)
- Fields: id, name, address, city, district

**CurrentAccountDto.java** (NEW)
- Fields: id, buildingId, buildingName, name, authorizedPerson, phone, debt, credit, balance

**RevisionOfferDto.java** (NEW)
- Fields: id, elevatorId, buildingId, currentAccountId, partsTotal, laborTotal, totalPrice, status

### 6. Controllers

**BuildingController.java** (NEW)
- GET /buildings
- GET /buildings/{id}
- POST /buildings
- PUT /buildings/{id}
- DELETE /buildings/{id}

**CurrentAccountController.java** (NEW)
- GET /current-accounts
- GET /current-accounts/{id}
- GET /current-accounts/building/{buildingId}
- PUT /current-accounts/{id}
- PATCH /current-accounts/{id}/balance

**RevisionOfferController.java** (NEW)
- GET /revision-offers
- GET /revision-offers/{id}
- GET /revision-offers/elevator/{elevatorId}
- POST /revision-offers
- PUT /revision-offers/{id}
- POST /revision-offers/{id}/convert-to-sale
- DELETE /revision-offers/{id}

### 7. Scheduled Jobs

**ElevatorStatusScheduler.java** (NEW)
- `@Scheduled(cron = "0 0 3 * * *")`: Daily at 3:00 AM
  - Recalculates elevator statuses based on endDate
- `@Scheduled(cron = "0 30 3 * * *")`: Daily at 3:30 AM
  - Recalculates endDate from labelDate + labelDuration

**SaraAsansorApiApplication.java** (UPDATED)
- Added `@EnableScheduling`

---

## BUSINESS RULES IMPLEMENTED

### 1. Label Options in Maintenance ✅
- Maintenance now has `labelType` field
- Options: GREEN, BLUE, YELLOW, RED
- Enum: `LabelType`

### 2. Blue Label Duration Bug Fix ✅
- **Rule**: Blue label duration = 12 months
- **Status Logic**: 
  ```java
  if (today > labelDate + 12 months) → EXPIRED
  else → ACTIVE
  ```
- **Fixed in**: `LabelDurationCalculator.calculateStatus()`
- **Applied in**: `ElevatorService.createElevator()`, `ElevatorService.updateElevator()`
- **Scheduled Job**: `ElevatorStatusScheduler.recalculateElevatorStatuses()`

### 3. End Date Not Being Saved ✅
- **Rule**: `endDate = labelDate + labelDuration`
- **Implementation**:
  - Added `label_date` column (migration V9)
  - Backfilled: `label_date = inspection_date` for existing records
  - Calculation: `LabelDurationCalculator.calculateEndDate()`
  - Auto-saved in `ElevatorService.createElevator()` and `updateElevator()`

### 4. Manager Information to Elevator ✅
- Added fields:
  - `managerName`
  - `managerTcIdentityNo`
  - `managerPhone`
  - `managerEmail` (optional)
- Updated: Entity, DTO, Mapper, API responses

### 5. Maintenance Auto-Update Elevator ✅
- **Rule**: When maintenance is created with a label:
  - Automatically update elevator's `labelType`
  - Automatically update elevator's `labelDate` = maintenance date
  - Automatically recalculate `endDate` and `status`
- **Implementation**: `MaintenanceService.createMaintenance()`

### 6. Auto-Create Current Account per Building ✅
- **Rule**: Each building must automatically have a CurrentAccount
- **Implementation**:
  - `BuildingService.createBuilding()` → calls `CurrentAccountService.createForBuilding()`
  - `BuildingService.updateBuilding()` → ensures account exists
  - Unique constraint: `UNIQUE (building_id)` in database

### 7. Revision Offer Module ✅
- **Entity**: `RevisionOffer`
- **Fields**: elevatorId, buildingId, currentAccountId, partsTotal, laborTotal, totalPrice, status
- **Status**: DRAFT, SENT, APPROVED, REJECTED, CONVERTED_TO_SALE
- **APIs**:
  - `POST /revision-offers` - create
  - `PUT /revision-offers/{id}` - update
  - `GET /revision-offers` - list
  - `POST /revision-offers/{id}/convert-to-sale` - convert to sale
- **Business Logic**:
  - `totalPrice = partsTotal + laborTotal` (auto-calculated)
  - `convertToSale`: Only APPROVED offers can be converted
  - On conversion: Updates current account debt

---

## STATUS CALCULATION ALGORITHM

### Final Implementation

```java
// Label Duration (months)
BLUE   → 12 months
GREEN  → 24 months
YELLOW → 6 months
RED    → 1 month

// End Date Calculation
endDate = labelDate + labelDurationMonths

// Status Calculation
if (today > endDate) {
    status = EXPIRED
} else {
    status = ACTIVE
}
```

### Where Applied

1. **ElevatorService.createElevator()**: On create
2. **ElevatorService.updateElevator()**: On update (if labelDate/labelType changed)
3. **MaintenanceService.createMaintenance()**: Auto-updates elevator
4. **ElevatorStatusScheduler**: Daily cron job (3:00 AM)
5. **ElevatorStatusScheduler.recalculateEndDates()**: Daily cron job (3:30 AM)

---

## CRON JOB DESIGN

### Job 1: Status Recalculation
- **Schedule**: `0 0 3 * * *` (Daily at 3:00 AM)
- **Purpose**: Update elevator statuses based on endDate
- **Logic**: 
  ```sql
  UPDATE elevators
  SET status = 'EXPIRED'
  WHERE status = 'ACTIVE' AND expiry_date < CURRENT_DATE
  ```

### Job 2: End Date Recalculation
- **Schedule**: `0 30 3 * * *` (Daily at 3:30 AM)
- **Purpose**: Recalculate endDate from labelDate + labelDuration
- **Logic**: For each elevator, recalculate if labelDate/labelType changed

---

## API ENDPOINTS SUMMARY

### Existing (Updated)
- `POST /elevators` - Now accepts labelDate, labelType, manager fields
- `PUT /elevators/{id}` - Now updates labelDate, labelType, manager fields
- `POST /maintenances` - Now accepts labelType, auto-updates elevator

### New Endpoints

**Buildings:**
- `GET /buildings`
- `GET /buildings/{id}`
- `POST /buildings` (auto-creates CurrentAccount)
- `PUT /buildings/{id}`
- `DELETE /buildings/{id}`

**Current Accounts:**
- `GET /current-accounts`
- `GET /current-accounts/{id}`
- `GET /current-accounts/building/{buildingId}`
- `PUT /current-accounts/{id}`
- `PATCH /current-accounts/{id}/balance`

**Revision Offers:**
- `GET /revision-offers`
- `GET /revision-offers/{id}`
- `GET /revision-offers/elevator/{elevatorId}`
- `POST /revision-offers`
- `PUT /revision-offers/{id}`
- `POST /revision-offers/{id}/convert-to-sale`
- `DELETE /revision-offers/{id}`

---

## TESTING CHECKLIST

### Elevator
- [ ] Create elevator with labelType BLUE → endDate = labelDate + 12 months
- [ ] Create elevator with labelType GREEN → endDate = labelDate + 24 months
- [ ] Update elevator labelDate → endDate recalculated
- [ ] Update elevator labelType → endDate recalculated
- [ ] Status = EXPIRED when today > endDate
- [ ] Status = ACTIVE when today <= endDate
- [ ] Manager fields saved and returned

### Maintenance
- [ ] Create maintenance with labelType BLUE → elevator updated
- [ ] Create maintenance with labelType GREEN → elevator updated
- [ ] Maintenance labelType saved correctly
- [ ] Elevator labelDate = maintenance date after maintenance created
- [ ] Elevator endDate recalculated after maintenance

### Building & Current Account
- [ ] Create building → CurrentAccount auto-created
- [ ] Update building → CurrentAccount exists (or created)
- [ ] Get current account by building ID
- [ ] Update current account balance (debt/credit)

### Revision Offer
- [ ] Create revision offer
- [ ] Update revision offer (DRAFT/SENT only)
- [ ] Convert APPROVED offer to sale
- [ ] Convert fails if status != APPROVED
- [ ] Current account debt updated on conversion

### Scheduled Jobs
- [ ] Status recalculation runs daily
- [ ] End date recalculation runs daily
- [ ] Status updates correctly based on endDate

---

## MIGRATION NOTES

### Running Migrations

```bash
# Migrations will run automatically on startup
# Or manually:
mvn flyway:migrate
```

### Data Backfill

Migration V9 automatically:
- Sets `label_date = inspection_date` for existing elevators
- Sets `label_type = 'BLUE'` if `blue_label = true`, else 'GREEN'
- Calculates `status` based on `expiry_date`

### Breaking Changes

⚠️ **Note**: Existing code using `inspectionDate` for calculations will continue to work, but new code should use `labelDate`.

---

## FINAL STATUS CALCULATION METHOD

**File**: `LabelDurationCalculator.java`

```java
public static StatusResult calculateStatusAndEndDate(LocalDate labelDate, LabelType labelType) {
    LocalDate endDate = calculateEndDate(labelDate, labelType);
    Elevator.Status status = calculateStatus(endDate);
    return new StatusResult(endDate, status);
}
```

**Usage**:
```java
LabelDurationCalculator.StatusResult result = 
    LabelDurationCalculator.calculateStatusAndEndDate(labelDate, labelType);
elevator.setExpiryDate(result.getEndDate());
elevator.setStatus(result.getStatus());
```

---

## NEXT STEPS

1. **Test migrations** on development database
2. **Verify** existing elevators have correct labelDate and status after migration
3. **Test** maintenance auto-update functionality
4. **Test** building → current account auto-creation
5. **Test** revision offer conversion to sale
6. **Monitor** scheduled jobs in production logs

---

**All changes are backward compatible. Existing endpoints continue to work.**
