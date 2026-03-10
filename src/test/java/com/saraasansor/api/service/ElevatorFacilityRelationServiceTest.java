package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitElevatorCreateRequest;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.util.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorFacilityRelationServiceTest {

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

    @Test
    void createShouldUseFacilityRelationWhenFacilityIdIsValid() {
        Facility facility = facility(11L, "Facility A", "Address A", 5L);
        when(facilityRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.existsByIdentityNumber("ID-001")).thenReturn(false);
        when(elevatorRepository.save(any(Elevator.class))).thenAnswer(invocation -> {
            Elevator saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });

        ElevatorDto dto = validDto(11L, "ID-001");

        ElevatorDto created = elevatorService.createElevator(dto);

        assertThat(created.getId()).isEqualTo(501L);
        assertThat(created.getFacilityId()).isEqualTo(11L);
        assertThat(created.getBuildingName()).isEqualTo("Facility A");

        ArgumentCaptor<Elevator> captor = ArgumentCaptor.forClass(Elevator.class);
        verify(elevatorRepository).save(captor.capture());
        assertThat(captor.getValue().getFacility()).isNotNull();
        assertThat(captor.getValue().getFacility().getId()).isEqualTo(11L);
    }

    @Test
    void updateShouldUseFacilityRelationWhenFacilityIdIsValid() {
        Facility facility = facility(11L, "Facility A", "Address A", 5L);
        when(facilityRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(facility));

        Elevator existing = new Elevator();
        existing.setId(301L);
        existing.setIdentityNumber("ID-001");
        existing.setLabelDate(LocalDate.of(2026, 3, 9));
        existing.setInspectionDate(LocalDate.of(2026, 3, 9));
        existing.setExpiryDate(LocalDate.of(2027, 3, 9));
        when(elevatorRepository.findById(301L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.save(any(Elevator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ElevatorDto dto = validDto(11L, "ID-001");
        ElevatorDto updated = elevatorService.updateElevator(301L, dto);

        assertThat(updated.getFacilityId()).isEqualTo(11L);
        assertThat(updated.getBuildingName()).isEqualTo("Facility A");
    }

    @Test
    void createShouldFailWhenFacilityDoesNotExist() {
        when(facilityRepository.findByIdAndActiveTrue(404L)).thenReturn(Optional.empty());

        ElevatorDto dto = validDto(404L, "ID-404");

        assertThatThrownBy(() -> elevatorService.createElevator(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Facility not found");
    }

    @Test
    void scopedCreateShouldFailWhenFacilityBelongsToAnotherB2BUnit() {
        B2BUnit selected = new B2BUnit();
        selected.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(selected));

        Facility foreignFacility = facility(99L, "Foreign Facility", "Address X", 9L);
        when(facilityRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.of(foreignFacility));

        B2BUnitElevatorCreateRequest request = new B2BUnitElevatorCreateRequest();
        request.setFacilityId(99L);
        request.setIdentityNumber("ID-009");
        request.setName("Elevator 9");
        request.setLabelDate(LocalDate.of(2026, 3, 9));
        request.setLabelType("GREEN");
        request.setExpiryDate(LocalDate.of(2027, 3, 9));
        request.setManagerName("Manager X");
        request.setManagerTcIdentityNo("12345678901");
        request.setManagerPhone("05551112233");

        assertThatThrownBy(() -> elevatorService.createElevatorForB2BUnit(5L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Facility does not belong to selected B2B unit");
    }

    private ElevatorDto validDto(Long facilityId, String identityNumber) {
        ElevatorDto dto = new ElevatorDto();
        dto.setFacilityId(facilityId);
        dto.setIdentityNumber(identityNumber);
        dto.setElevatorNumber("Elevator 1");
        dto.setLabelDate(LocalDate.of(2026, 3, 9));
        dto.setLabelType("GREEN");
        dto.setExpiryDate(LocalDate.of(2027, 3, 9));
        dto.setManagerName("Manager A");
        dto.setManagerTcIdentityNo("12345678901");
        dto.setManagerPhone("05551112233");
        return dto;
    }

    private Facility facility(Long id, String name, String addressText, Long b2bUnitId) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName(name);
        facility.setAddressText(addressText);

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(b2bUnitId);
        facility.setB2bUnit(b2bUnit);
        return facility;
    }
}
