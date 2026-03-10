package com.saraasansor.api.service;

import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.FaultRepository;
import com.saraasansor.api.repository.MaintenanceRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import com.saraasansor.api.repository.PaymentReceiptRepository;
import com.saraasansor.api.repository.RegionRepository;
import com.saraasansor.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityLookupAccessServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private PaymentReceiptRepository paymentReceiptRepository;

    @Mock
    private FaultRepository faultRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private NeighborhoodRepository neighborhoodRepository;

    @Mock
    private RegionRepository regionRepository;

    private FacilityService facilityService;

    @BeforeEach
    void setUp() {
        facilityService = new FacilityService(
                facilityRepository,
                b2bUnitRepository,
                userRepository,
                elevatorRepository,
                maintenanceRepository,
                paymentReceiptRepository,
                faultRepository,
                cityRepository,
                districtRepository,
                neighborhoodRepository,
                regionRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemAdminLookupWithoutB2BUnitShouldReturnTenantFacilities() {
        authenticateAs("system-admin");

        User systemAdmin = new User();
        systemAdmin.setUsername("system-admin");
        systemAdmin.setRole(User.Role.SYSTEM_ADMIN);
        when(userRepository.findByUsername("system-admin")).thenReturn(Optional.of(systemAdmin));

        Facility facilityA = new Facility();
        facilityA.setId(1L);
        facilityA.setName("Facility A");
        facilityA.setActive(true);

        Facility facilityB = new Facility();
        facilityB.setId(2L);
        facilityB.setName("Facility B");
        facilityB.setActive(true);

        when(facilityRepository.search(eq("fac"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(facilityA, facilityB)));

        List<LookupDto> result = facilityService.getLookup(null, "fac");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LookupDto::getId).containsExactly(1L, 2L);
        verify(facilityRepository).search(eq("fac"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void staffLookupWithoutB2BUnitShouldFail() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> facilityService.getLookup(null, "fac"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("b2bUnitId is required for this user role");
    }

    @Test
    void cariLookupShouldUseOwnB2BUnit() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(55L);

        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));

        Facility ownFacility = new Facility();
        ownFacility.setId(10L);
        ownFacility.setName("Own Facility");
        ownFacility.setActive(true);

        when(facilityRepository.findLookupByB2bUnitId(eq(55L), eq("own"), any(Pageable.class)))
                .thenReturn(List.of(ownFacility));

        List<LookupDto> result = facilityService.getLookup(null, "own");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        verify(facilityRepository).findLookupByB2bUnitId(eq(55L), eq("own"), any(Pageable.class));
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
