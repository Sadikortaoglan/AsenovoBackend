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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitMaintenanceFailureServiceTest {

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private FaultRepository faultRepository;

    @Mock
    private UserRepository userRepository;

    private B2BUnitMaintenanceFailureService service;

    @BeforeEach
    void setUp() {
        service = new B2BUnitMaintenanceFailureService(
                b2bUnitRepository,
                facilityRepository,
                elevatorRepository,
                maintenanceRepository,
                faultRepository,
                userRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnOnlyCompletedRecordsOfSelectedB2BUnit() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));

        B2BUnit b2bUnit = activeB2BUnit(5L);
        Facility facility = facility(11L, "Facility A", b2bUnit);
        Elevator elevator = elevator(101L, "Facility A", "E-1", "ID-1");

        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.findByB2bUnitIdAndActiveTrue(5L)).thenReturn(List.of(facility));
        when(elevatorRepository.findByBuildingNames(List.of("facility a"))).thenReturn(List.of(elevator));

        Maintenance maintenance = maintenance(201L, elevator, LocalDate.of(2026, 3, 8), "monthly maintenance");
        Fault completedFault = fault(301L, elevator, Fault.Status.COMPLETED, LocalDateTime.of(2026, 3, 9, 9, 30), "door issue");
        Fault openFault = fault(302L, elevator, Fault.Status.OPEN, LocalDateTime.of(2026, 3, 9, 10, 0), "still open");

        when(maintenanceRepository.findByElevatorIdInOrderByDateDesc(List.of(101L))).thenReturn(List.of(maintenance));
        when(faultRepository.findByElevatorIdInOrderByCreatedAtDesc(List.of(101L))).thenReturn(List.of(openFault, completedFault));

        Page<B2BUnitMaintenanceFailureListItemResponse> page = service.getCompletedMaintenanceFailures(
                5L,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(B2BUnitMaintenanceFailureListItemResponse::getSourceType)
                .containsExactly("FAILURE", "MAINTENANCE");
        assertThat(page.getContent())
                .extracting(B2BUnitMaintenanceFailureListItemResponse::getStatus)
                .allMatch("COMPLETED"::equals);
    }

    @Test
    void listShouldExcludeRecordsOutsideSelectedB2BUnitOwnershipChain() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));

        B2BUnit b2bUnit = activeB2BUnit(5L);
        Facility facility = facility(11L, "Facility A", b2bUnit);
        Elevator ownElevator = elevator(101L, "Facility A", "E-1", "ID-1");
        Elevator foreignElevator = elevator(202L, "Foreign Facility", "E-2", "ID-2");

        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.findByB2bUnitIdAndActiveTrue(5L)).thenReturn(List.of(facility));
        when(elevatorRepository.findByBuildingNames(List.of("facility a"))).thenReturn(List.of(ownElevator, foreignElevator));

        Maintenance ownMaintenance = maintenance(201L, ownElevator, LocalDate.of(2026, 3, 8), "own");
        Maintenance foreignMaintenance = maintenance(202L, foreignElevator, LocalDate.of(2026, 3, 8), "foreign");
        when(maintenanceRepository.findByElevatorIdInOrderByDateDesc(List.of(101L, 202L)))
                .thenReturn(List.of(foreignMaintenance, ownMaintenance));
        when(faultRepository.findByElevatorIdInOrderByCreatedAtDesc(List.of(101L, 202L)))
                .thenReturn(List.of());

        Page<B2BUnitMaintenanceFailureListItemResponse> page = service.getCompletedMaintenanceFailures(
                5L,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getElevatorId()).isEqualTo(101L);
        assertThat(page.getContent().get(0).getFacilityName()).isEqualTo("Facility A");
    }

    @Test
    void listShouldSupportPaginationAndSearch() {
        authenticateAs("staff-admin");
        when(userRepository.findByUsername("staff-admin")).thenReturn(Optional.of(staffUser("staff-admin")));

        B2BUnit b2bUnit = activeB2BUnit(7L);
        Facility facility = facility(77L, "Facility Search", b2bUnit);
        Elevator elevator = elevator(707L, "Facility Search", "SearchElevator", "ID-707");

        when(b2bUnitRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.findByB2bUnitIdAndActiveTrue(7L)).thenReturn(List.of(facility));
        when(elevatorRepository.findByBuildingNames(List.of("facility search"))).thenReturn(List.of(elevator));

        Maintenance m1 = maintenance(1L, elevator, LocalDate.of(2026, 1, 1), "alpha");
        Maintenance m2 = maintenance(2L, elevator, LocalDate.of(2026, 1, 2), "beta");
        Maintenance m3 = maintenance(3L, elevator, LocalDate.of(2026, 1, 3), "gamma");
        when(maintenanceRepository.findByElevatorIdInOrderByDateDesc(List.of(707L))).thenReturn(List.of(m3, m2, m1));
        when(faultRepository.findByElevatorIdInOrderByCreatedAtDesc(List.of(707L))).thenReturn(List.of());

        Page<B2BUnitMaintenanceFailureListItemResponse> searchPage = service.getCompletedMaintenanceFailures(
                7L,
                "beta",
                PageRequest.of(0, 20)
        );
        assertThat(searchPage.getContent()).hasSize(1);
        assertThat(searchPage.getContent().get(0).getId()).isEqualTo(2L);

        Page<B2BUnitMaintenanceFailureListItemResponse> paged = service.getCompletedMaintenanceFailures(
                7L,
                null,
                PageRequest.of(1, 2)
        );
        assertThat(paged.getContent()).hasSize(1);
        assertThat(paged.getTotalElements()).isEqualTo(3);
        assertThat(paged.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldForbidCariUserForAnotherB2BUnit() {
        authenticateAs("cari-user");
        User cariUser = new User();
        cariUser.setUsername("cari-user");
        cariUser.setRole(User.Role.CARI_USER);
        B2BUnit ownB2B = activeB2BUnit(10L);
        cariUser.setB2bUnit(ownB2B);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cariUser));

        when(b2bUnitRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(activeB2BUnit(11L)));

        assertThatThrownBy(() -> service.getCompletedMaintenanceFailures(11L, null, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit");

        verify(facilityRepository, never()).findByB2bUnitIdAndActiveTrue(11L);
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User staffUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(User.Role.STAFF_USER);
        user.setUserType(User.UserType.STAFF);
        return user;
    }

    private B2BUnit activeB2BUnit(Long id) {
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(id);
        b2bUnit.setActive(true);
        return b2bUnit;
    }

    private Facility facility(Long id, String name, B2BUnit b2bUnit) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName(name);
        facility.setB2bUnit(b2bUnit);
        facility.setActive(true);
        return facility;
    }

    private Elevator elevator(Long id, String buildingName, String elevatorNumber, String identityNumber) {
        Elevator elevator = new Elevator();
        elevator.setId(id);
        elevator.setBuildingName(buildingName);
        elevator.setElevatorNumber(elevatorNumber);
        elevator.setIdentityNumber(identityNumber);
        return elevator;
    }

    private Maintenance maintenance(Long id, Elevator elevator, LocalDate date, String description) {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(id);
        maintenance.setElevator(elevator);
        maintenance.setDate(date);
        maintenance.setDescription(description);
        maintenance.setCreatedAt(date.atStartOfDay());
        return maintenance;
    }

    private Fault fault(Long id, Elevator elevator, Fault.Status status, LocalDateTime createdAt, String description) {
        Fault fault = new Fault();
        fault.setId(id);
        fault.setElevator(elevator);
        fault.setStatus(status);
        fault.setCreatedAt(createdAt);
        fault.setDescription(description);
        fault.setFaultSubject(description);
        fault.setContactPerson("contact");
        return fault;
    }
}
