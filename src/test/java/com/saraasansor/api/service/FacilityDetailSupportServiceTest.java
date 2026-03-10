package com.saraasansor.api.service;

import com.saraasansor.api.dto.FacilityDetailResponse;
import com.saraasansor.api.dto.FacilityElevatorSummaryResponse;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityDetailSupportServiceTest {

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

    @Mock
    private FileStorageService fileStorageService;

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
                regionRepository,
                fileStorageService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void detailShouldLoadFacilitySnapshot() throws Exception {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(15L);
        b2bUnit.setName("Main Cari");

        Facility facility = new Facility();
        facility.setId(55L);
        facility.setName("Facility Detail");
        facility.setB2bUnit(b2bUnit);
        facility.setTaxNumber("1234567890");
        facility.setTaxOffice("Tax Office");
        facility.setType(Facility.FacilityType.TUZEL_KISI);
        facility.setInvoiceType(Facility.FacilityInvoiceType.TICARI_FATURA);
        facility.setCompanyTitle("Company X");
        facility.setAuthorizedFirstName("Ali");
        facility.setAuthorizedLastName("Yildiz");
        facility.setEmail("ali@example.com");
        facility.setPhone("+90 555 111 2233");
        facility.setFacilityType("Site");
        facility.setAttendantFullName("Duty");
        facility.setManagerFlatNo("12");
        facility.setFloorCount(6);
        facility.setAddressText("Sample Address");
        facility.setDescription("Detailed facility");
        facility.setStatus(Facility.FacilityStatus.ACTIVE);
        facility.setMapLat(BigDecimal.valueOf(39.9));
        facility.setMapLng(BigDecimal.valueOf(32.8));
        facility.setAttachmentUrl("facility/55/doc.pdf");
        facility.setActive(true);

        Elevator elevatorOne = new Elevator();
        elevatorOne.setId(201L);
        elevatorOne.setFacility(facility);
        elevatorOne.setBuildingName("Facility Detail");
        elevatorOne.setElevatorNumber("E-201");

        Elevator elevatorTwo = new Elevator();
        elevatorTwo.setId(202L);
        elevatorTwo.setFacility(facility);
        elevatorTwo.setBuildingName("facility detail");
        elevatorTwo.setIdentityNumber("ID-202");

        Path attachmentPath = Files.createTempFile("facility-55", ".pdf");
        attachmentPath.toFile().deleteOnExit();
        Files.writeString(attachmentPath, "facility attachment content");

        when(facilityRepository.findByIdAndActiveTrue(55L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.findByFacilityId(55L)).thenReturn(List.of(elevatorOne, elevatorTwo));
        when(elevatorRepository.findByBuildingNameIgnoreCase("Facility Detail")).thenReturn(List.of());
        when(fileStorageService.resolveStoredPath("facility/55/doc.pdf")).thenReturn(attachmentPath);

        FacilityDetailResponse result = facilityService.getFacilityDetail(55L);

        assertThat(result.getId()).isEqualTo(55L);
        assertThat(result.getName()).isEqualTo("Facility Detail");
        assertThat(result.getB2bUnitName()).isEqualTo("Main Cari");
        assertThat(result.getAttachmentPreviewUrl()).isEqualTo("/facilities/55/attachment");
        assertThat(result.getAttachmentName()).isEqualTo(attachmentPath.getFileName().toString());
        assertThat(result.getReportUrl()).isEqualTo("/facilities/55/report");
        assertThat(result.getElevators()).hasSize(2)
                .extracting(FacilityElevatorSummaryResponse::getId)
                .containsExactly(201L, 202L);
    }

    @Test
    void detailShouldThrowWhenFacilityNotFound() {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));
        when(facilityRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getFacilityDetail(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Facility not found");
    }

    @Test
    void detailShouldRejectCariOwnershipViolation() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);

        B2BUnit other = new B2BUnit();
        other.setId(11L);
        Facility facility = new Facility();
        facility.setId(66L);
        facility.setName("Foreign Facility");
        facility.setB2bUnit(other);
        facility.setActive(true);

        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));
        when(facilityRepository.findByIdAndActiveTrue(66L)).thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.getFacilityDetail(66L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit facilities");
    }

    @Test
    void detailShouldFilterOnlyFacilityElevators() throws Exception {
        authenticateAs("staff-user");

        User staff = new User();
        staff.setUsername("staff-user");
        staff.setRole(User.Role.STAFF_USER);
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staff));

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(15L);

        Facility facility = new Facility();
        facility.setId(77L);
        facility.setName("Facility Gamma");
        facility.setB2bUnit(b2bUnit);
        facility.setActive(true);

        Elevator matching = new Elevator();
        matching.setId(401L);
        matching.setFacility(facility);
        matching.setBuildingName("Facility Gamma");

        when(facilityRepository.findByIdAndActiveTrue(77L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.findByFacilityId(77L)).thenReturn(List.of(matching));
        when(elevatorRepository.findByBuildingNameIgnoreCase("Facility Gamma")).thenReturn(List.of());
        Path attachment = Files.createTempFile("facility-other", ".txt");
        when(fileStorageService.resolveStoredPath(anyString())).thenReturn(attachment);

        FacilityDetailResponse result = facilityService.getFacilityDetail(77L);

        assertThat(result.getElevators()).extracting(FacilityElevatorSummaryResponse::getId)
                .containsExactly(401L);
    }

    @Test
    void attachmentAccessShouldRespectOwnershipForCariUsers() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);

        B2BUnit other = new B2BUnit();
        other.setId(11L);

        Facility facility = new Facility();
        facility.setId(88L);
        facility.setName("Private Facility");
        facility.setB2bUnit(other);
        facility.setAttachmentUrl("private/file.txt");
        facility.setActive(true);

        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));
        when(facilityRepository.findByIdAndActiveTrue(88L)).thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.getFacilityAttachment(88L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit facilities");
    }

    @Test
    void cariUserFacilitiesQueryShouldUseOwnB2BUnit() {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);

        Facility facility = new Facility();
        facility.setId(90L);
        facility.setName("Facility Query");
        facility.setB2bUnit(own);
        facility.setActive(true);

        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));
        when(facilityRepository.search("search", 10L, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(facility), PageRequest.of(0, 20), 1));

        var result = facilityService.getFacilities("search", 999L, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        verify(facilityRepository).search("search", 10L, null, PageRequest.of(0, 20));
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", List.of());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
