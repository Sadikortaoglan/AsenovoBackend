package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitElevatorCreateRequest;
import com.saraasansor.api.dto.B2BUnitElevatorListItemResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.util.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitElevatorServiceTest {

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ElevatorService elevatorService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnOnlyElevatorsOfSelectedB2BUnitFacilities() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));

        Facility facility = new Facility();
        facility.setId(11L);
        facility.setName("Facility A");
        facility.setB2bUnit(b2bUnit);
        when(facilityRepository.findByB2bUnitIdAndActiveTrue(5L)).thenReturn(List.of(facility));

        Elevator elevator = new Elevator();
        elevator.setId(101L);
        elevator.setBuildingName("Facility A");
        elevator.setElevatorNumber("Elevator 1");
        elevator.setIdentityNumber("ID-001");
        elevator.setMachineBrand("BrandX");
        elevator.setStatus(Elevator.Status.ACTIVE);
        elevator.setCreatedAt(LocalDateTime.of(2026, 3, 9, 12, 0));
        when(elevatorRepository.searchByBuildingNames(eq(List.of("facility a")), eq("id-001"), any()))
                .thenReturn(new PageImpl<>(List.of(elevator), PageRequest.of(0, 20), 1));

        var response = elevatorService.getElevatorsByB2BUnit(5L, "id-001", PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        B2BUnitElevatorListItemResponse item = response.getContent().get(0);
        assertThat(item.getId()).isEqualTo(101L);
        assertThat(item.getFacilityId()).isEqualTo(11L);
        assertThat(item.getFacilityName()).isEqualTo("Facility A");
    }

    @Test
    void createShouldPersistElevatorUnderFacilityOfSelectedB2BUnit() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));

        Facility facility = new Facility();
        facility.setId(11L);
        facility.setName("Facility A");
        facility.setAddressText("Address A");
        facility.setB2bUnit(b2bUnit);
        when(facilityRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(facility));

        when(elevatorRepository.existsByIdentityNumber("ID-001")).thenReturn(false);
        when(elevatorRepository.save(any(Elevator.class))).thenAnswer(invocation -> {
            Elevator toSave = invocation.getArgument(0);
            toSave.setId(201L);
            return toSave;
        });

        B2BUnitElevatorCreateRequest request = buildValidRequest();
        request.setFacilityId(11L);

        ElevatorDto created = elevatorService.createElevatorForB2BUnit(5L, request);

        assertThat(created.getId()).isEqualTo(201L);
        assertThat(created.getBuildingName()).isEqualTo("Facility A");
        assertThat(created.getElevatorNumber()).isEqualTo("Elevator 1");

        ArgumentCaptor<Elevator> captor = ArgumentCaptor.forClass(Elevator.class);
        verify(elevatorRepository).save(captor.capture());
        assertThat(captor.getValue().getBuildingName()).isEqualTo("Facility A");
    }

    @Test
    void createShouldRejectCrossB2BUnitFacility() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit selected = new B2BUnit();
        selected.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(selected));

        B2BUnit other = new B2BUnit();
        other.setId(9L);
        Facility foreignFacility = new Facility();
        foreignFacility.setId(99L);
        foreignFacility.setName("Foreign Facility");
        foreignFacility.setB2bUnit(other);
        when(facilityRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.of(foreignFacility));

        B2BUnitElevatorCreateRequest request = buildValidRequest();
        request.setFacilityId(99L);

        assertThatThrownBy(() -> elevatorService.createElevatorForB2BUnit(5L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Facility does not belong to selected B2B unit");
    }

    @Test
    void cariUserShouldNotAccessAnotherB2BUnitElevatorList() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(7L);
        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));

        B2BUnit selected = new B2BUnit();
        selected.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(selected));

        assertThatThrownBy(() -> elevatorService.getElevatorsByB2BUnit(5L, null, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit elevators");
    }

    private B2BUnitElevatorCreateRequest buildValidRequest() {
        B2BUnitElevatorCreateRequest request = new B2BUnitElevatorCreateRequest();
        request.setIdentityNumber("ID-001");
        request.setName("Elevator 1");
        request.setLabelDate(LocalDate.of(2026, 3, 9));
        request.setLabelType("GREEN");
        request.setExpiryDate(LocalDate.of(2027, 3, 9));
        request.setManagerName("Manager A");
        request.setManagerTcIdentityNo("12345678901");
        request.setManagerPhone("05551112233");
        request.setAddressText("Address A");
        return request;
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
