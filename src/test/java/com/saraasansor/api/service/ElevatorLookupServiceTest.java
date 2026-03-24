package com.saraasansor.api.service;

import com.saraasansor.api.dto.LookupDto;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorLookupServiceTest {

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
    void lookupWithoutFacilityShouldReturnElevatorAndFacilityInfo() {
        Elevator elevator = new Elevator();
        elevator.setId(55L);
        elevator.setElevatorNumber("E-55");
        elevator.setIdentityNumber("ID-55");
        elevator.setBuildingName("Legacy Building");

        Facility facility = new Facility();
        facility.setId(77L);
        facility.setName("Facility 77");
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(7L);
        facility.setB2bUnit(b2bUnit);
        elevator.setFacility(facility);

        when(elevatorRepository.findAll(any(Sort.class))).thenReturn(List.of(elevator));

        List<LookupDto> response = elevatorService.getLookup(null, "E-55");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(55L);
        assertThat(response.get(0).getName()).isEqualTo("E-55");
        assertThat(response.get(0).getFacilityId()).isEqualTo(77L);
        assertThat(response.get(0).getFacilityName()).isEqualTo("Facility 77");
    }
}
