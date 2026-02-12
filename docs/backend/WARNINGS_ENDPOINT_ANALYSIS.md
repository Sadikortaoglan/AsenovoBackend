# Warnings Endpoint Analysis

## Current Implementation

### 1. SQL Query (ElevatorRepository)
```java
@Query("SELECT e FROM Elevator e WHERE e.expiryDate < :now")
List<Elevator> findExpiredElevators(LocalDate now);
```

**Analysis:**
- ❌ **NO GROUP BY clause**
- ❌ **NO DISTINCT clause**
- ✅ Returns **ALL** expired elevators
- ✅ No filtering by buildingName

### 2. Service Layer (ElevatorService)
```java
public List<WarningDto> getExpiredElevatorsAsWarnings() {
    LocalDate now = LocalDate.now();
    return elevatorRepository.findExpiredElevators(now).stream()
            .map(elevator -> WarningDto.fromEntity(elevator, "EXPIRED"))
            .collect(Collectors.toList());
}
```

**Analysis:**
- ❌ **NO grouping logic**
- ❌ **NO distinct() operation**
- ❌ **NO filtering by buildingName**
- ✅ Maps each elevator to WarningDto
- ✅ Should return ALL expired elevators

### 3. WarningDto Mapping
```java
public static WarningDto fromEntity(Elevator elevator, String status) {
    WarningDto dto = new WarningDto();
    dto.setIdentityNo(elevator.getIdentityNumber());
    dto.setBuildingName(elevator.getBuildingName());
    dto.setAddress(elevator.getAddress());
    dto.setMaintenanceEndDate(elevator.getExpiryDate());
    dto.setStatus(status);
    return dto;
}
```

**Analysis:**
- ❌ **NO grouping logic**
- ✅ Simple 1:1 mapping
- ✅ Each elevator becomes one WarningDto

## Conclusion

### ❌ NO GROUPING IN BACKEND

The backend **does NOT group** elevators by buildingName. It should return:
- **ALL expired elevators** (one per elevator)
- **ALL expiring soon elevators** (one per elevator)

### Possible Causes for Only 2 Warnings

1. **Frontend Grouping**: Frontend might be grouping by `buildingName` and showing only one per building
2. **Database Issue**: Only 2 elevators actually have `expiryDate < now`
3. **Filtering Elsewhere**: Some other filter might be applied

## Verification Steps

1. **Check Database**:
   ```sql
   SELECT COUNT(*) FROM elevators WHERE expiry_date < CURRENT_DATE;
   ```

2. **Check API Response**:
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/warnings?type=EXPIRED
   ```
   Count the items in the response.

3. **Check Frontend**: Look for grouping/filtering logic in the frontend code.

## Optional: Add Grouping by Building

If you want to group by building (one warning per building), here are options:

### Option 1: Group by Building (First Elevator Only)
```java
public List<WarningDto> getExpiredElevatorsAsWarnings() {
    LocalDate now = LocalDate.now();
    return elevatorRepository.findExpiredElevators(now).stream()
            .collect(Collectors.toMap(
                Elevator::getBuildingName,
                elevator -> WarningDto.fromEntity(elevator, "EXPIRED"),
                (existing, replacement) -> existing // Keep first
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
}
```

### Option 2: Group by Building (Earliest Expiry)
```java
public List<WarningDto> getExpiredElevatorsAsWarnings() {
    LocalDate now = LocalDate.now();
    return elevatorRepository.findExpiredElevators(now).stream()
            .collect(Collectors.toMap(
                Elevator::getBuildingName,
                elevator -> WarningDto.fromEntity(elevator, "EXPIRED"),
                (existing, replacement) -> 
                    existing.getMaintenanceEndDate().isBefore(replacement.getMaintenanceEndDate()) 
                        ? existing : replacement
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
}
```

### Option 3: Return All with Grouping Flag
Add query parameter to control grouping:
```java
@GetMapping
public ResponseEntity<ApiResponse<List<WarningDto>>> getWarnings(
        @RequestParam(required = false) String type,
        @RequestParam(required = false, defaultValue = "false") boolean groupByBuilding) {
    
    List<WarningDto> warnings;
    
    if ("EXPIRED".equalsIgnoreCase(type)) {
        warnings = elevatorService.getExpiredElevatorsAsWarnings(groupByBuilding);
    } else if ("WARNING".equalsIgnoreCase(type)) {
        warnings = elevatorService.getExpiringSoonElevatorsAsWarnings(groupByBuilding);
    } else {
        warnings = elevatorService.getExpiredElevatorsAsWarnings(groupByBuilding);
        warnings.addAll(elevatorService.getExpiringSoonElevatorsAsWarnings(groupByBuilding));
    }
    
    return ResponseEntity.ok(ApiResponse.success(warnings));
}
```

## Recommendation

1. **Verify Database**: Check how many elevators are actually expired
2. **Check API Response**: Verify backend returns all elevators
3. **Check Frontend**: Look for grouping/filtering in UI code
4. **If Grouping Needed**: Implement Option 3 (optional grouping parameter)
