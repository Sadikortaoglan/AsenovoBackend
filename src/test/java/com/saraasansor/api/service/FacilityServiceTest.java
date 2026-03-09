package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitFacilityCreateRequest;
import com.saraasansor.api.dto.CreateFacilityRequest;
import com.saraasansor.api.dto.FacilityDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

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
    void cariUserShouldNotAccessFacilityOfAnotherB2BUnit() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User currentUser = new User();
        currentUser.setUsername("cari-user");
        currentUser.setRole(User.Role.CARI_USER);
        currentUser.setB2bUnit(own);

        B2BUnit another = new B2BUnit();
        another.setId(11L);
        Facility facility = new Facility();
        facility.setId(100L);
        facility.setB2bUnit(another);
        facility.setActive(true);

        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(currentUser));
        when(facilityRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.getFacilityById(100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit facilities");
    }

    @Test
    void staffUserShouldCreateFacility() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(20L);
        b2bUnit.setName("Cari A");
        when(b2bUnitRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrue(20L, "Facility A")).thenReturn(false);
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> {
            Facility saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        CreateFacilityRequest request = new CreateFacilityRequest();
        request.setName("Facility A");
        request.setB2bUnitId(20L);
        request.setStatus(Facility.FacilityStatus.ACTIVE);
        request.setType(Facility.FacilityType.TUZEL_KISI);
        request.setInvoiceType(Facility.FacilityInvoiceType.TICARI_FATURA);

        assertThat(facilityService.createFacility(request).getId()).isEqualTo(200L);
    }

    @Test
    void shouldListFacilitiesOnlyForSelectedB2BUnit() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(20L);
        b2bUnit.setName("Cari A");
        when(b2bUnitRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(b2bUnit));

        Facility facility = new Facility();
        facility.setId(901L);
        facility.setName("Facility A");
        facility.setB2bUnit(b2bUnit);
        facility.setActive(true);
        when(facilityRepository.search(eq("a"), eq(20L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(facility), PageRequest.of(0, 20), 1));

        var result = facilityService.getFacilitiesByB2BUnit(20L, "a", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(901L);
        assertThat(result.getContent().get(0).getB2bUnitId()).isEqualTo(20L);
        verify(facilityRepository).search(eq("a"), eq(20L), isNull(), any(Pageable.class));
    }

    @Test
    void shouldCreateFacilityUnderPathB2BUnit() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(30L);
        b2bUnit.setName("Cari B");
        when(b2bUnitRepository.findByIdAndActiveTrue(30L)).thenReturn(Optional.of(b2bUnit));
        when(facilityRepository.existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrue(30L, "Scoped Facility")).thenReturn(false);
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> {
            Facility saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        B2BUnitFacilityCreateRequest request = new B2BUnitFacilityCreateRequest();
        request.setName("Scoped Facility");
        request.setStatus(Facility.FacilityStatus.ACTIVE);
        request.setType(Facility.FacilityType.TUZEL_KISI);
        request.setInvoiceType(Facility.FacilityInvoiceType.TICARI_FATURA);

        FacilityDto created = facilityService.createFacilityForB2BUnit(30L, request);

        assertThat(created.getId()).isEqualTo(300L);
        assertThat(created.getB2bUnitId()).isEqualTo(30L);

        ArgumentCaptor<Facility> captor = ArgumentCaptor.forClass(Facility.class);
        verify(facilityRepository).save(captor.capture());
        assertThat(captor.getValue().getB2bUnit().getId()).isEqualTo(30L);
    }

    @Test
    void cariUserShouldNotAccessAnotherB2BUnitScopedList() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User currentUser = new User();
        currentUser.setUsername("cari-user");
        currentUser.setRole(User.Role.CARI_USER);
        currentUser.setB2bUnit(own);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(currentUser));

        B2BUnit another = new B2BUnit();
        another.setId(11L);
        when(b2bUnitRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(another));

        assertThatThrownBy(() -> facilityService.getFacilitiesByB2BUnit(11L, null, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit facilities");
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
