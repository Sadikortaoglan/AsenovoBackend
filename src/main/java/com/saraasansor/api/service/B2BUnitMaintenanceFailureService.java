package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitMaintenanceFailureListItemResponse;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.Fault;
import com.saraasansor.api.model.Maintenance;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.FaultRepository;
import com.saraasansor.api.repository.MaintenanceRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class B2BUnitMaintenanceFailureService {

    private final B2BUnitRepository b2bUnitRepository;
    private final FacilityRepository facilityRepository;
    private final ElevatorRepository elevatorRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FaultRepository faultRepository;
    private final UserRepository userRepository;

    public B2BUnitMaintenanceFailureService(B2BUnitRepository b2bUnitRepository,
                                            FacilityRepository facilityRepository,
                                            ElevatorRepository elevatorRepository,
                                            MaintenanceRepository maintenanceRepository,
                                            FaultRepository faultRepository,
                                            UserRepository userRepository) {
        this.b2bUnitRepository = b2bUnitRepository;
        this.facilityRepository = facilityRepository;
        this.elevatorRepository = elevatorRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.faultRepository = faultRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<B2BUnitMaintenanceFailureListItemResponse> getCompletedMaintenanceFailures(Long b2bUnitId,
                                                                                            String search,
                                                                                            Pageable pageable) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceReadableB2BUnitScopeAccess(b2bUnit.getId());

        List<Facility> facilities = facilityRepository.findByB2bUnitIdAndActiveTrue(b2bUnitId);
        if (facilities.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, Facility> facilityByBuildingKey = facilities.stream()
                .collect(Collectors.toMap(
                        facility -> normalizeKey(facility.getName()),
                        facility -> facility,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        List<String> buildingNames = new ArrayList<>(facilityByBuildingKey.keySet());
        List<Elevator> elevators = elevatorRepository.findByBuildingNames(buildingNames);
        if (elevators.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Elevator> elevatorById = elevators.stream()
                .collect(Collectors.toMap(Elevator::getId, elevator -> elevator, (existing, ignored) -> existing));
        List<Long> elevatorIds = new ArrayList<>(elevatorById.keySet());

        List<MaintenanceFailureRow> rows = new ArrayList<>();
        for (Maintenance maintenance : maintenanceRepository.findByElevatorIdInOrderByDateDesc(elevatorIds)) {
            Elevator elevator = elevatorById.get(maintenance.getElevator().getId());
            Facility facility = elevator != null ? facilityByBuildingKey.get(normalizeKey(elevator.getBuildingName())) : null;
            if (elevator == null || facility == null) {
                continue;
            }
            rows.add(MaintenanceFailureRow.fromMaintenance(maintenance, elevator, facility));
        }

        for (Fault fault : faultRepository.findByElevatorIdInOrderByCreatedAtDesc(elevatorIds)) {
            if (fault.getStatus() != Fault.Status.COMPLETED) {
                continue;
            }
            Elevator elevator = elevatorById.get(fault.getElevator().getId());
            Facility facility = elevator != null ? facilityByBuildingKey.get(normalizeKey(elevator.getBuildingName())) : null;
            if (elevator == null || facility == null) {
                continue;
            }
            rows.add(MaintenanceFailureRow.fromFault(fault, elevator, facility));
        }

        String normalizedSearch = normalizeNullable(search);
        List<MaintenanceFailureRow> filteredRows = rows.stream()
                .filter(row -> matchesSearch(row, normalizedSearch))
                .sorted(resolveComparator(pageable))
                .toList();

        if (filteredRows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        int fromIndex = (int) pageable.getOffset();
        if (fromIndex >= filteredRows.size()) {
            return new PageImpl<>(List.of(), pageable, filteredRows.size());
        }
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), filteredRows.size());
        List<B2BUnitMaintenanceFailureListItemResponse> content = filteredRows.subList(fromIndex, toIndex).stream()
                .map(MaintenanceFailureRow::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, filteredRows.size());
    }

    private Comparator<MaintenanceFailureRow> resolveComparator(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Order.desc("operationDate"), Sort.Order.desc("id"));

        Comparator<MaintenanceFailureRow> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<MaintenanceFailureRow> fieldComparator = comparatorForField(order);
            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }

        if (comparator == null) {
            comparator = comparatorForField(Sort.Order.desc("operationDate"));
        }
        return comparator.thenComparing(
                Comparator.comparing(MaintenanceFailureRow::id, Comparator.nullsLast(Long::compareTo)).reversed()
        );
    }

    private Comparator<MaintenanceFailureRow> comparatorForField(Sort.Order order) {
        String property = order.getProperty();
        Comparator<MaintenanceFailureRow> comparator;
        switch (property) {
            case "id" -> comparator = Comparator.comparing(MaintenanceFailureRow::id, Comparator.nullsLast(Long::compareTo));
            case "operationType" -> comparator = Comparator.comparing(MaintenanceFailureRow::operationType,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "sourceType" -> comparator = Comparator.comparing(MaintenanceFailureRow::sourceType,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "elevatorId" -> comparator = Comparator.comparing(MaintenanceFailureRow::elevatorId,
                    Comparator.nullsLast(Long::compareTo));
            case "elevatorName" -> comparator = Comparator.comparing(MaintenanceFailureRow::elevatorName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "facilityId" -> comparator = Comparator.comparing(MaintenanceFailureRow::facilityId,
                    Comparator.nullsLast(Long::compareTo));
            case "facilityName" -> comparator = Comparator.comparing(MaintenanceFailureRow::facilityName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> comparator = Comparator.comparing(MaintenanceFailureRow::status,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "operationDate" -> comparator = Comparator.comparing(MaintenanceFailureRow::operationDate,
                    Comparator.nullsLast(LocalDateTime::compareTo));
            default -> comparator = Comparator.comparing(MaintenanceFailureRow::operationDate,
                    Comparator.nullsLast(LocalDateTime::compareTo));
        }

        return order.isAscending() ? comparator : comparator.reversed();
    }

    private boolean matchesSearch(MaintenanceFailureRow row, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = normalizeSearch(query);
        String operationAliases = "MAINTENANCE".equals(row.sourceType())
                ? "maintenance bakim bakım"
                : "failure fault ariza arıza";

        return contains(row.elevatorName(), normalizedQuery)
                || contains(row.facilityName(), normalizedQuery)
                || contains(row.operationType(), normalizedQuery)
                || contains(row.status(), normalizedQuery)
                || contains(row.searchableDescription(), normalizedQuery)
                || contains(operationAliases, normalizedQuery);
    }

    private boolean contains(String value, String query) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return normalizeSearch(value).contains(query);
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSearch(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ş', 's')
                .replace('ü', 'u')
                .trim();
    }

    private String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void enforceReadableB2BUnitScopeAccess(Long b2bUnitId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.Role.CARI_USER) {
            return;
        }

        Long ownB2bUnitId = currentUser.getB2bUnit() != null ? currentUser.getB2bUnit().getId() : null;
        if (ownB2bUnitId == null || !ownB2bUnitId.equals(b2bUnitId)) {
            throw new AccessDeniedException("CARI user can only access own B2B unit maintenance-failure records");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
            username = principalName;
        }

        if (!StringUtils.hasText(username)) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }

    private record MaintenanceFailureRow(Long id,
                                         LocalDateTime operationDate,
                                         String operationType,
                                         String sourceType,
                                         Long elevatorId,
                                         String elevatorName,
                                         Long facilityId,
                                         String facilityName,
                                         String status,
                                         String searchableDescription) {
        static MaintenanceFailureRow fromMaintenance(Maintenance maintenance, Elevator elevator, Facility facility) {
            String elevatorName = StringUtils.hasText(elevator.getElevatorNumber())
                    ? elevator.getElevatorNumber()
                    : elevator.getIdentityNumber();
            return new MaintenanceFailureRow(
                    maintenance.getId(),
                    maintenance.getDate() != null ? maintenance.getDate().atStartOfDay() : maintenance.getCreatedAt(),
                    "MAINTENANCE",
                    "MAINTENANCE",
                    elevator.getId(),
                    elevatorName,
                    facility.getId(),
                    facility.getName(),
                    "COMPLETED",
                    maintenance.getDescription()
            );
        }

        static MaintenanceFailureRow fromFault(Fault fault, Elevator elevator, Facility facility) {
            String elevatorName = StringUtils.hasText(elevator.getElevatorNumber())
                    ? elevator.getElevatorNumber()
                    : elevator.getIdentityNumber();
            String searchableDescription = StringUtils.hasText(fault.getDescription())
                    ? fault.getDescription()
                    : fault.getFaultSubject();
            return new MaintenanceFailureRow(
                    fault.getId(),
                    fault.getCreatedAt(),
                    "FAILURE",
                    "FAILURE",
                    elevator.getId(),
                    elevatorName,
                    facility.getId(),
                    facility.getName(),
                    fault.getStatus() != null ? fault.getStatus().name() : null,
                    searchableDescription
            );
        }

        B2BUnitMaintenanceFailureListItemResponse toResponse() {
            B2BUnitMaintenanceFailureListItemResponse response = new B2BUnitMaintenanceFailureListItemResponse();
            response.setId(id);
            response.setOperationDate(operationDate);
            response.setOperationType(operationType);
            response.setSourceType(sourceType);
            response.setElevatorId(elevatorId);
            response.setElevatorName(elevatorName);
            response.setFacilityId(facilityId);
            response.setFacilityName(facilityName);
            response.setStatus(status);
            return response;
        }
    }
}
