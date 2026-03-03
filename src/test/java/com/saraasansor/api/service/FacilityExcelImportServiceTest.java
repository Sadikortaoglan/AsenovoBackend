package com.saraasansor.api.service;

import com.saraasansor.api.dto.FacilityImportResultDto;
import com.saraasansor.api.model.B2BCurrency;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityExcelImportServiceTest {

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
        authenticateAs("staff-admin");

        User staff = new User();
        staff.setUsername("staff-admin");
        staff.setRole(User.Role.STAFF_ADMIN);
        when(userRepository.findByUsername("staff-admin")).thenReturn(Optional.of(staff));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importShouldCreateMissingB2BUnitWhenEnabled() throws Exception {
        MockMultipartFile file = buildExcel("New Facility", "New Cari");

        when(b2bUnitRepository.findFirstByNameIgnoreCaseAndActiveTrue("New Cari")).thenReturn(Optional.empty());
        when(b2bUnitRepository.save(any(B2BUnit.class))).thenAnswer(invocation -> {
            B2BUnit unit = invocation.getArgument(0);
            unit.setId(90L);
            if (unit.getCurrency() == null) {
                unit.setCurrency(B2BCurrency.TRY);
            }
            return unit;
        });
        when(b2bUnitRepository.findByIdAndActiveTrue(90L)).thenAnswer(invocation -> {
            B2BUnit unit = new B2BUnit();
            unit.setId(90L);
            unit.setName("New Cari");
            unit.setActive(true);
            unit.setCurrency(B2BCurrency.TRY);
            return Optional.of(unit);
        });
        when(facilityRepository.existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrue(90L, "New Facility")).thenReturn(false);
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> {
            Facility facility = invocation.getArgument(0);
            facility.setId(1000L);
            return facility;
        });

        FacilityImportResultDto result = facilityService.importFromExcel(file, true);

        assertThat(result.getReadRows()).isEqualTo(1);
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(0);
        verify(b2bUnitRepository, times(1)).save(any(B2BUnit.class));
    }

    @Test
    void importShouldReturnFailedRowWhenCariMissingAndCreationDisabled() throws Exception {
        MockMultipartFile file = buildExcel("Another Facility", "Missing Cari");
        when(b2bUnitRepository.findFirstByNameIgnoreCaseAndActiveTrue("Missing Cari")).thenReturn(Optional.empty());

        FacilityImportResultDto result = facilityService.importFromExcel(file, false);

        assertThat(result.getReadRows()).isEqualTo(1);
        assertThat(result.getSuccessRows()).isEqualTo(0);
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getRowErrors()).hasSize(1);
        assertThat(result.getRowErrors().get(0).getReason()).contains("B2B unit not found");
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MockMultipartFile buildExcel(String facilityName, String cariName) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Facilities");
            var header = sheet.createRow(0);
            String[] headers = {
                    "TESIS ADI",
                    "CARI ADI",
                    "FIRMA ADI",
                    "VERGI NO",
                    "VERGI DAIRESI",
                    "YETKILI AD",
                    "YETKILI SOYAD",
                    "MAIL ADRESI",
                    "TELEFON NUMARASI",
                    "IL",
                    "ILCE",
                    "MAHALLE",
                    "BOLGE",
                    "ADRES",
                    "TESIS TURU",
                    "GOREVLI",
                    "YONETICI DAIRE NO",
                    "KAPI SIFRESI",
                    "KAT SAYISI",
                    "ACIKLAMA"
            };

            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(facilityName);
            row.createCell(1).setCellValue(cariName);
            row.createCell(2).setCellValue("Company");
            row.createCell(3).setCellValue("1234567890");
            row.createCell(4).setCellValue("Tax Office");
            row.createCell(5).setCellValue("Ali");
            row.createCell(6).setCellValue("Veli");
            row.createCell(7).setCellValue("test@example.com");
            row.createCell(8).setCellValue("+905551112233");
            row.createCell(9).setCellValue("");
            row.createCell(10).setCellValue("");
            row.createCell(11).setCellValue("");
            row.createCell(12).setCellValue("");
            row.createCell(13).setCellValue("Address");
            row.createCell(14).setCellValue("Site");
            row.createCell(15).setCellValue("Attendant");
            row.createCell(16).setCellValue("10");
            row.createCell(17).setCellValue("1234");
            row.createCell(18).setCellValue("8");
            row.createCell(19).setCellValue("Desc");

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "facilities.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
