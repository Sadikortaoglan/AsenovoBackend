package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorImportResultResponse;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.util.AuditLogger;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorExcelImportServiceTest {

    private static final String[] HEADERS = {
            "ASANSÖR ADI",
            "TESİS ADI",
            "BÖLGE",
            "ASANSÖR KİMLİK NUMARASI",
            "ASANSÖR TÜRÜ/CİNSİ",
            "KAPI TİPİ",
            "TAHRİK TÜRÜ",
            "MARKA",
            "YAPIM YILI",
            "DURAK SAYISI",
            "KAPASİTE KİŞİ/KG",
            "HIZ M/S",
            "GARANTİ",
            "GARANTİ BİTİŞ TARİHİ",
            "AÇIKLAMA",
            "BAKIM TÜRÜ",
            "BAKIM PERSONELİ AD",
            "BAKIM PERSONELİ SOYAD",
            "ARIZA PERSONELİ AD",
            "ARIZA PERSONELİ SOYAD",
            "ADRES"
    };

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

    private ElevatorService elevatorService;

    @BeforeEach
    void setUp() {
        elevatorService = new ElevatorService();
        setField(elevatorService, "elevatorRepository", elevatorRepository);
        setField(elevatorService, "facilityRepository", facilityRepository);
        setField(elevatorService, "b2bUnitRepository", b2bUnitRepository);
        setField(elevatorService, "userRepository", userRepository);
        setField(elevatorService, "auditLogger", auditLogger);

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
    void importShouldSucceedForValidRow() throws Exception {
        Facility facility = facility(11L, "Facility A", 5L);
        when(facilityRepository.search(eq("Facility A"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(facility)));
        when(facilityRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.existsByIdentityNumber("ID-001")).thenReturn(false);
        when(elevatorRepository.save(any(Elevator.class))).thenAnswer(invocation -> {
            Elevator elevator = invocation.getArgument(0);
            elevator.setId(101L);
            return elevator;
        });

        List<String[]> rowList = new ArrayList<>();
        String[] rowValues = new String[]{"Asansör A", "Facility A", "Merkez", "ID-001", "İnsan", "Otomatik", "Elektrikli",
                "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"};
        rowList.add(rowValues);
        MockMultipartFile file = buildExcel(rowList);

        ElevatorImportResultResponse result = elevatorService.importFromExcel(file);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("SUCCESS");

        ArgumentCaptor<Elevator> captor = ArgumentCaptor.forClass(Elevator.class);
        verify(elevatorRepository).save(captor.capture());
        assertThat(captor.getValue().getFacility()).isNotNull();
        assertThat(captor.getValue().getFacility().getId()).isEqualTo(11L);
    }

    @Test
    void importShouldFailRowWhenFacilityNotFound() throws Exception {
        when(facilityRepository.search(eq("Missing Facility"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<String[]> rowList = new ArrayList<>();
        String[] rowValues = new String[]{"Asansör A", "Missing Facility", "Merkez", "ID-002", "İnsan", "Otomatik", "Elektrikli",
                "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"};
        rowList.add(rowValues);
        MockMultipartFile file = buildExcel(rowList);

        ElevatorImportResultResponse result = elevatorService.importFromExcel(file);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(result.getItems().get(0).getMessage()).contains("Facility not found");
    }

    @Test
    void importShouldFailRowWhenRequiredDataMissing() throws Exception {

        List<String[]> rowList = new ArrayList<>();
        String[] rowValues = new String[]{"", "Facility A", "Merkez", "ID-003", "İnsan", "Otomatik", "Elektrikli",
                "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"};
        rowList.add(rowValues);
        MockMultipartFile file = buildExcel(rowList);

        ElevatorImportResultResponse result = elevatorService.importFromExcel(file);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getMessage()).contains("ASANSÖR ADI is required");
    }

    @Test
    void importShouldSupportPartialSuccess() throws Exception {
        Facility facility = facility(11L, "Facility A", 5L);
        when(facilityRepository.search(eq("Facility A"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(facility)));
        when(facilityRepository.search(eq("Missing Facility"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(facilityRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(facility));
        when(elevatorRepository.existsByIdentityNumber(anyString())).thenReturn(false);
        when(elevatorRepository.save(any(Elevator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = buildExcel(List.of(
                new String[]{"Asansör A", "Facility A", "Merkez", "ID-004", "İnsan", "Otomatik", "Elektrikli",
                        "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                        "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"},
                new String[]{"Asansör B", "Missing Facility", "Merkez", "ID-005", "İnsan", "Otomatik", "Elektrikli",
                        "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                        "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres B"}
        ));

        ElevatorImportResultResponse result = elevatorService.importFromExcel(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
    }

    @Test
    void scopedImportShouldFailWhenFacilityBelongsToAnotherB2BUnit() throws Exception {
        B2BUnit selectedB2B = new B2BUnit();
        selectedB2B.setId(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(selectedB2B));

        Facility foreignFacility = facility(99L, "Facility X", 9L);
        when(facilityRepository.search(eq("Facility X"), eq(5L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(foreignFacility)));

        List<String[]> rowList = new ArrayList<>();
        String[] rowValues = new String[]{"Asansör A", "Facility X", "Merkez", "ID-006", "İnsan", "Otomatik", "Elektrikli",
                "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"};
        rowList.add(rowValues);
        MockMultipartFile file = buildExcel(rowList);

        ElevatorImportResultResponse result = elevatorService.importFromExcelForB2BUnit(5L, file);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getMessage()).contains("does not belong to selected B2B unit");
    }

    @Test
    void importShouldBeForbiddenForCariUser() throws Exception {
        authenticateAs("cari-user");

        B2BUnit own = new B2BUnit();
        own.setId(10L);
        User cari = new User();
        cari.setUsername("cari-user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(own);
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cari));

        List<String[]> rowList = new ArrayList<>();
        String[] rowValues = new String[]{"Asansör A", "Facility A", "Merkez", "ID-007", "İnsan", "Otomatik", "Elektrikli",
                "MarkaX", "2020", "10", "8", "1.6", "Var", "2028-01-01",
                "Açıklama", "Aylık", "Ali", "Yılmaz", "Mehmet", "Kaya", "Adres A"};
        rowList.add(rowValues);
        MockMultipartFile file = buildExcel(rowList);

        assertThatThrownBy(() -> elevatorService.importFromExcel(file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("CARI user cannot modify elevators");
    }

    private Facility facility(Long id, String name, Long b2bUnitId) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName(name);
        facility.setAddressText("Address " + name);
        facility.setActive(true);
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(b2bUnitId);
        facility.setB2bUnit(b2bUnit);
        return facility;
    }

    private MockMultipartFile buildExcel(List<String[]> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Elevators");
            var header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowIndex = 1;
            for (String[] rowValues : rows) {
                var row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowValues.length; i++) {
                    row.createCell(i).setCellValue(rowValues[i]);
                }
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "elevators.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
