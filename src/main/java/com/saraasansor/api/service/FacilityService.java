package com.saraasansor.api.service;

import com.saraasansor.api.dto.CreateFacilityRequest;
import com.saraasansor.api.dto.B2BUnitFacilityCreateRequest;
import com.saraasansor.api.dto.FacilityAddressDto;
import com.saraasansor.api.dto.FacilityDto;
import com.saraasansor.api.dto.FacilityImportResultDto;
import com.saraasansor.api.dto.FacilityMovementDto;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.UpdateFacilityRequest;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.City;
import com.saraasansor.api.model.District;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.Fault;
import com.saraasansor.api.model.Maintenance;
import com.saraasansor.api.model.Neighborhood;
import com.saraasansor.api.model.PaymentReceipt;
import com.saraasansor.api.model.Region;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class FacilityService {

    private static final Pattern TAX_NUMBER_PATTERN = Pattern.compile("^(\\d{10}|\\d{11})$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final String HEADER_TESIS_ADI = "TESIS ADI";
    private static final String HEADER_CARI_ADI = "CARI ADI";
    private static final String HEADER_FIRMA_ADI = "FIRMA ADI";
    private static final String HEADER_VERGI_NO = "VERGI NO";
    private static final String HEADER_VERGI_DAIRESI = "VERGI DAIRESI";
    private static final String HEADER_YETKILI_AD = "YETKILI AD";
    private static final String HEADER_YETKILI_SOYAD = "YETKILI SOYAD";
    private static final String HEADER_MAIL_ADRESI = "MAIL ADRESI";
    private static final String HEADER_TELEFON_NUMARASI = "TELEFON NUMARASI";
    private static final String HEADER_IL = "IL";
    private static final String HEADER_ILCE = "ILCE";
    private static final String HEADER_MAHALLE = "MAHALLE";
    private static final String HEADER_BOLGE = "BOLGE";
    private static final String HEADER_ADRES = "ADRES";
    private static final String HEADER_TESIS_TURU = "TESIS TURU";
    private static final String HEADER_GOREVLI = "GOREVLI";
    private static final String HEADER_YONETICI_DAIRE_NO = "YONETICI DAIRE NO";
    private static final String HEADER_KAPI_SIFRESI = "KAPI SIFRESI";
    private static final String HEADER_KAT_SAYISI = "KAT SAYISI";
    private static final String HEADER_ACIKLAMA = "ACIKLAMA";

    private static final List<String> REQUIRED_IMPORT_HEADERS = List.of(
            HEADER_TESIS_ADI,
            HEADER_CARI_ADI,
            HEADER_FIRMA_ADI,
            HEADER_VERGI_NO,
            HEADER_VERGI_DAIRESI,
            HEADER_YETKILI_AD,
            HEADER_YETKILI_SOYAD,
            HEADER_MAIL_ADRESI,
            HEADER_TELEFON_NUMARASI,
            HEADER_IL,
            HEADER_ILCE,
            HEADER_MAHALLE,
            HEADER_BOLGE,
            HEADER_ADRES,
            HEADER_TESIS_TURU,
            HEADER_GOREVLI,
            HEADER_YONETICI_DAIRE_NO,
            HEADER_KAPI_SIFRESI,
            HEADER_KAT_SAYISI,
            HEADER_ACIKLAMA
    );

    private final FacilityRepository facilityRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final UserRepository userRepository;
    private final ElevatorRepository elevatorRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final FaultRepository faultRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final RegionRepository regionRepository;

    public FacilityService(FacilityRepository facilityRepository,
                           B2BUnitRepository b2bUnitRepository,
                           UserRepository userRepository,
                           ElevatorRepository elevatorRepository,
                           MaintenanceRepository maintenanceRepository,
                           PaymentReceiptRepository paymentReceiptRepository,
                           FaultRepository faultRepository,
                           CityRepository cityRepository,
                           DistrictRepository districtRepository,
                           NeighborhoodRepository neighborhoodRepository,
                           RegionRepository regionRepository) {
        this.facilityRepository = facilityRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.userRepository = userRepository;
        this.elevatorRepository = elevatorRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.faultRepository = faultRepository;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.regionRepository = regionRepository;
    }

    @Transactional(readOnly = true)
    public Page<FacilityDto> getFacilities(String query,
                                           Long b2bUnitId,
                                           Facility.FacilityStatus status,
                                           Pageable pageable) {
        User currentUser = getCurrentUser();
        Long effectiveB2bUnitId = b2bUnitId;
        if (isCariUser(currentUser)) {
            effectiveB2bUnitId = getCariB2bUnitId(currentUser);
        }

        Page<Facility> facilities = facilityRepository.search(normalizeNullable(query), effectiveB2bUnitId, status, pageable);
        boolean includeDoorPassword = canViewDoorPassword(currentUser);
        return facilities.map(facility -> FacilityDto.fromEntity(facility, includeDoorPassword));
    }

    @Transactional(readOnly = true)
    public Page<FacilityDto> getFacilitiesByB2BUnit(Long b2bUnitId, String search, Pageable pageable) {
        enforceReadableB2BUnitScopeAccess(b2bUnitId);
        Page<Facility> facilities = facilityRepository.search(normalizeNullable(search), b2bUnitId, null, pageable);
        boolean includeDoorPassword = canViewDoorPassword(getCurrentUser());
        return facilities.map(facility -> FacilityDto.fromEntity(facility, includeDoorPassword));
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(Long b2bUnitId, String query) {
        User currentUser = getCurrentUser();
        Long effectiveB2bUnitId = b2bUnitId;
        if (isCariUser(currentUser)) {
            effectiveB2bUnitId = getCariB2bUnitId(currentUser);
        }
        if (effectiveB2bUnitId == null) {
            throw new RuntimeException("b2bUnitId is required");
        }

        return facilityRepository.findLookupByB2bUnitId(
                        effectiveB2bUnitId,
                        normalizeNullable(query),
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name"))
                ).stream()
                .map(facility -> new LookupDto(facility.getId(), facility.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookupByB2BUnit(Long b2bUnitId, String query) {
        enforceReadableB2BUnitScopeAccess(b2bUnitId);
        return facilityRepository.findLookupByB2bUnitId(
                        b2bUnitId,
                        normalizeNullable(query),
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name"))
                ).stream()
                .map(facility -> new LookupDto(facility.getId(), facility.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FacilityDto getFacilityById(Long id) {
        Facility facility = findAccessibleFacility(id);
        return FacilityDto.fromEntity(facility, canViewDoorPassword(getCurrentUser()));
    }

    @Transactional(readOnly = true)
    public FacilityAddressDto getFacilityAddressById(Long id) {
        Facility facility = findAccessibleFacility(id);

        FacilityAddressDto dto = new FacilityAddressDto();
        if (facility.getCity() != null) {
            dto.setCityId(facility.getCity().getId());
            dto.setCityName(facility.getCity().getName());
        }
        if (facility.getDistrict() != null) {
            dto.setDistrictId(facility.getDistrict().getId());
            dto.setDistrictName(facility.getDistrict().getName());
        }
        if (facility.getNeighborhood() != null) {
            dto.setNeighborhoodId(facility.getNeighborhood().getId());
            dto.setNeighborhoodName(facility.getNeighborhood().getName());
        }
        if (facility.getRegion() != null) {
            dto.setRegionId(facility.getRegion().getId());
            dto.setRegionName(facility.getRegion().getName());
        }
        dto.setAddressText(facility.getAddressText());
        return dto;
    }

    public FacilityDto createFacility(CreateFacilityRequest request) {
        enforceNonCariWrite();

        Facility facility = new Facility();
        B2BUnit b2bUnit = resolveB2BUnit(request.getB2bUnitId());
        validateDuplicateNameForCreate(request.getName(), b2bUnit.getId());
        applyRequest(facility, request, b2bUnit);

        return FacilityDto.fromEntity(facilityRepository.save(facility), canViewDoorPassword(getCurrentUser()));
    }

    public FacilityDto createFacilityForB2BUnit(Long b2bUnitId, B2BUnitFacilityCreateRequest request) {
        enforceNonCariWrite();

        B2BUnit b2bUnit = resolveB2BUnit(b2bUnitId);
        CreateFacilityRequest createRequest = toCreateFacilityRequest(request, b2bUnitId);
        validateDuplicateNameForCreate(createRequest.getName(), b2bUnit.getId());

        Facility facility = new Facility();
        applyRequest(facility, createRequest, b2bUnit);

        return FacilityDto.fromEntity(facilityRepository.save(facility), canViewDoorPassword(getCurrentUser()));
    }

    public FacilityDto updateFacility(Long id, UpdateFacilityRequest request) {
        enforceNonCariWrite();
        Facility existing = findActiveFacility(id);
        enforceObjectAccess(existing);

        B2BUnit b2bUnit = resolveB2BUnit(request.getB2bUnitId());
        validateDuplicateNameForUpdate(request.getName(), b2bUnit.getId(), existing.getId());
        applyRequest(existing, request, b2bUnit);

        return FacilityDto.fromEntity(facilityRepository.save(existing), canViewDoorPassword(getCurrentUser()));
    }

    public void deleteFacility(Long id) {
        enforceNonCariWrite();
        Facility existing = findActiveFacility(id);
        enforceObjectAccess(existing);

        List<Elevator> elevators = elevatorRepository.findByBuildingNameIgnoreCase(existing.getName());
        if (!elevators.isEmpty()) {
            throw new RuntimeException("Facility has related elevators and cannot be deleted");
        }

        existing.setActive(false);
        existing.setStatus(Facility.FacilityStatus.PASSIVE);
        facilityRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public FacilityMovementDto getFacilityMovements(Long id) {
        Facility facility = findAccessibleFacility(id);
        return buildMovements(facility);
    }

    @Transactional(readOnly = true)
    public String buildFacilityReportHtml(Long id) {
        Facility facility = findAccessibleFacility(id);
        FacilityDto facilityDto = FacilityDto.fromEntity(facility, canViewDoorPassword(getCurrentUser()));
        FacilityMovementDto movements = buildMovements(facility);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>")
                .append("<title>Facility Report</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:24px;color:#222;} ")
                .append("h1,h2{margin:0 0 12px 0;} ")
                .append(".meta{margin-bottom:16px;font-size:14px;} ")
                .append(".grid{display:grid;grid-template-columns:1fr 1fr;gap:8px 24px;margin-bottom:20px;} ")
                .append(".label{font-weight:bold;} ")
                .append("table{width:100%;border-collapse:collapse;margin-bottom:20px;} ")
                .append("th,td{border:1px solid #ccc;padding:8px;text-align:left;font-size:13px;} ")
                .append("th{background:#f4f4f4;} ")
                .append("@media print{body{margin:0;} .no-print{display:none;}}")
                .append("</style></head><body>")
                .append("<h1>Bina Asansor Raporu</h1>")
                .append("<div class=\"meta\">Rapor Tarihi: ")
                .append(escapeHtml(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append("</div>")
                .append("<h2>Tesis Bilgileri</h2>")
                .append("<div class=\"grid\">")
                .append(gridCell("Tesis", facilityDto.getName()))
                .append(gridCell("Cari", facilityDto.getB2bUnitName()))
                .append(gridCell("Telefon", facilityDto.getPhone()))
                .append(gridCell("Firma", facilityDto.getCompanyTitle()))
                .append(gridCell("City/District", joinWithSlash(facilityDto.getCityName(), facilityDto.getDistrictName())))
                .append(gridCell("Neighborhood/Region", joinWithSlash(facilityDto.getNeighborhoodName(), facilityDto.getRegionName())))
                .append(gridCell("Tesis Turu", facilityDto.getFacilityType()))
                .append(gridCell("Durum", facilityDto.getStatus() != null ? facilityDto.getStatus().name() : null))
                .append(gridCell("Adres", facilityDto.getAddressText()))
                .append("</div>");

        html.append("<h2>Bina Hareketleri - Asansorler</h2>")
                .append("<table><thead><tr><th>ID</th><th>Kimlik No</th><th>Asansor No</th><th>Durum</th><th>Etiket Bitis</th></tr></thead><tbody>");
        for (FacilityMovementDto.ElevatorItem item : movements.getElevators()) {
            html.append("<tr>")
                    .append(td(item.getId()))
                    .append(td(item.getIdentityNumber()))
                    .append(td(item.getElevatorNumber()))
                    .append(td(item.getStatus()))
                    .append(td(item.getExpiryDate()))
                    .append("</tr>");
        }
        if (movements.getElevators().isEmpty()) {
            html.append("<tr><td colspan=\"5\">Kayit bulunamadi</td></tr>");
        }
        html.append("</tbody></table>");

        html.append("<h2>Bakimlar</h2>")
                .append("<table><thead><tr><th>ID</th><th>Asansor ID</th><th>Tarih</th><th>Etiket</th><th>Tutar</th><th>Odeme</th></tr></thead><tbody>");
        for (FacilityMovementDto.MaintenanceItem item : movements.getMaintenances()) {
            html.append("<tr>")
                    .append(td(item.getId()))
                    .append(td(item.getElevatorId()))
                    .append(td(item.getDate()))
                    .append(td(item.getLabelType()))
                    .append(td(item.getAmount()))
                    .append(td(item.getPaid()))
                    .append("</tr>");
        }
        if (movements.getMaintenances().isEmpty()) {
            html.append("<tr><td colspan=\"6\">Kayit bulunamadi</td></tr>");
        }
        html.append("</tbody></table>");

        html.append("<h2>Tahsilatlar</h2>")
                .append("<table><thead><tr><th>ID</th><th>Bakim ID</th><th>Tutar</th><th>Odeyen</th><th>Tarih</th></tr></thead><tbody>");
        for (FacilityMovementDto.PaymentItem item : movements.getPayments()) {
            html.append("<tr>")
                    .append(td(item.getId()))
                    .append(td(item.getMaintenanceId()))
                    .append(td(item.getAmount()))
                    .append(td(item.getPayerName()))
                    .append(td(item.getDate()))
                    .append("</tr>");
        }
        if (movements.getPayments().isEmpty()) {
            html.append("<tr><td colspan=\"5\">Kayit bulunamadi</td></tr>");
        }
        html.append("</tbody></table>");

        html.append("<h2>Arizalar</h2>")
                .append("<table><thead><tr><th>ID</th><th>Asansor ID</th><th>Konu</th><th>Durum</th><th>Tarih</th></tr></thead><tbody>");
        for (FacilityMovementDto.FaultItem item : movements.getFaults()) {
            html.append("<tr>")
                    .append(td(item.getId()))
                    .append(td(item.getElevatorId()))
                    .append(td(item.getSubject()))
                    .append(td(item.getStatus()))
                    .append(td(item.getCreatedAt()))
                    .append("</tr>");
        }
        if (movements.getFaults().isEmpty()) {
            html.append("<tr><td colspan=\"5\">Kayit bulunamadi</td></tr>");
        }
        html.append("</tbody></table>");

        html.append("</body></html>");
        return html.toString();
    }

    public FacilityImportResultDto importFromExcel(MultipartFile file, boolean createMissingB2BUnit) {
        enforceNonCariWrite();

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Excel file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new RuntimeException("Only .xlsx files are supported");
        }

        FacilityImportResultDto result = new FacilityImportResultDto();

        try (InputStream inputStream = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new RuntimeException("Excel sheet is empty");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel header row is missing");
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow, formatter);
            validateHeaders(headerMap);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                result.setReadRows(result.getReadRows() + 1);
                try {
                    String facilityName = readString(row, headerMap, HEADER_TESIS_ADI, formatter);
                    String cariName = readString(row, headerMap, HEADER_CARI_ADI, formatter);

                    if (!StringUtils.hasText(facilityName)) {
                        throw new RuntimeException("TESIS ADI is required");
                    }
                    if (!StringUtils.hasText(cariName)) {
                        throw new RuntimeException("CARI ADI is required");
                    }

                    B2BUnit b2bUnit = resolveB2BUnitForImport(cariName, createMissingB2BUnit);
                    LocationSelection locationSelection = resolveLocationByNames(
                            readString(row, headerMap, HEADER_IL, formatter),
                            readString(row, headerMap, HEADER_ILCE, formatter),
                            readString(row, headerMap, HEADER_MAHALLE, formatter),
                            readString(row, headerMap, HEADER_BOLGE, formatter)
                    );

                    CreateFacilityRequest request = new CreateFacilityRequest();
                    request.setName(facilityName);
                    request.setB2bUnitId(b2bUnit.getId());
                    request.setCompanyTitle(readString(row, headerMap, HEADER_FIRMA_ADI, formatter));
                    request.setTaxNumber(readString(row, headerMap, HEADER_VERGI_NO, formatter));
                    request.setTaxOffice(readString(row, headerMap, HEADER_VERGI_DAIRESI, formatter));
                    request.setAuthorizedFirstName(readString(row, headerMap, HEADER_YETKILI_AD, formatter));
                    request.setAuthorizedLastName(readString(row, headerMap, HEADER_YETKILI_SOYAD, formatter));
                    request.setEmail(readString(row, headerMap, HEADER_MAIL_ADRESI, formatter));
                    request.setPhone(readString(row, headerMap, HEADER_TELEFON_NUMARASI, formatter));
                    request.setCityId(locationSelection.city != null ? locationSelection.city.getId() : null);
                    request.setDistrictId(locationSelection.district != null ? locationSelection.district.getId() : null);
                    request.setNeighborhoodId(locationSelection.neighborhood != null ? locationSelection.neighborhood.getId() : null);
                    request.setRegionId(locationSelection.region != null ? locationSelection.region.getId() : null);
                    request.setAddressText(readString(row, headerMap, HEADER_ADRES, formatter));
                    request.setFacilityType(readString(row, headerMap, HEADER_TESIS_TURU, formatter));
                    request.setAttendantFullName(readString(row, headerMap, HEADER_GOREVLI, formatter));
                    request.setManagerFlatNo(readString(row, headerMap, HEADER_YONETICI_DAIRE_NO, formatter));
                    request.setDoorPassword(readString(row, headerMap, HEADER_KAPI_SIFRESI, formatter));
                    request.setDescription(readString(row, headerMap, HEADER_ACIKLAMA, formatter));
                    request.setFloorCount(parseInteger(readString(row, headerMap, HEADER_KAT_SAYISI, formatter)));
                    request.setType(Facility.FacilityType.TUZEL_KISI);
                    request.setInvoiceType(Facility.FacilityInvoiceType.TICARI_FATURA);
                    request.setStatus(Facility.FacilityStatus.ACTIVE);

                    validateImportedRequest(request);
                    createFacility(request);
                    result.setSuccessRows(result.getSuccessRows() + 1);
                } catch (Exception ex) {
                    result.setFailedRows(result.getFailedRows() + 1);
                    result.addRowError(rowIndex + 1, ex.getMessage());
                }
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse excel file: " + e.getMessage());
        }
    }

    private FacilityMovementDto buildMovements(Facility facility) {
        FacilityMovementDto movements = new FacilityMovementDto();
        List<Elevator> elevators = elevatorRepository.findByBuildingNameIgnoreCase(facility.getName());
        List<Long> elevatorIds = elevators.stream().map(Elevator::getId).toList();

        List<Maintenance> maintenances = elevatorIds.isEmpty()
                ? List.of()
                : maintenanceRepository.findByElevatorIdInOrderByDateDesc(elevatorIds);
        List<Long> maintenanceIds = maintenances.stream().map(Maintenance::getId).toList();

        List<PaymentReceipt> payments = maintenanceIds.isEmpty()
                ? List.of()
                : paymentReceiptRepository.findByMaintenanceIdInOrderByDateDesc(maintenanceIds);
        List<Fault> faults = elevatorIds.isEmpty()
                ? List.of()
                : faultRepository.findByElevatorIdInOrderByCreatedAtDesc(elevatorIds);

        movements.setElevators(elevators.stream().map(this::toElevatorItem).collect(Collectors.toList()));
        movements.setMaintenances(maintenances.stream().map(this::toMaintenanceItem).collect(Collectors.toList()));
        movements.setPayments(payments.stream().map(this::toPaymentItem).collect(Collectors.toList()));
        movements.setFaults(faults.stream().map(this::toFaultItem).collect(Collectors.toList()));
        return movements;
    }

    private FacilityMovementDto.ElevatorItem toElevatorItem(Elevator elevator) {
        FacilityMovementDto.ElevatorItem item = new FacilityMovementDto.ElevatorItem();
        item.setId(elevator.getId());
        item.setIdentityNumber(elevator.getIdentityNumber());
        item.setElevatorNumber(elevator.getElevatorNumber());
        item.setStatus(elevator.getStatus() != null ? elevator.getStatus().name() : null);
        item.setExpiryDate(elevator.getExpiryDate());
        return item;
    }

    private FacilityMovementDto.MaintenanceItem toMaintenanceItem(Maintenance maintenance) {
        FacilityMovementDto.MaintenanceItem item = new FacilityMovementDto.MaintenanceItem();
        item.setId(maintenance.getId());
        item.setElevatorId(maintenance.getElevator() != null ? maintenance.getElevator().getId() : null);
        item.setDate(maintenance.getDate());
        item.setLabelType(maintenance.getLabelType() != null ? maintenance.getLabelType().name() : null);
        item.setAmount(maintenance.getAmount());
        item.setPaid(maintenance.getIsPaid());
        return item;
    }

    private FacilityMovementDto.PaymentItem toPaymentItem(PaymentReceipt paymentReceipt) {
        FacilityMovementDto.PaymentItem item = new FacilityMovementDto.PaymentItem();
        item.setId(paymentReceipt.getId());
        item.setMaintenanceId(paymentReceipt.getMaintenance() != null ? paymentReceipt.getMaintenance().getId() : null);
        item.setAmount(paymentReceipt.getAmount());
        item.setPayerName(paymentReceipt.getPayerName());
        item.setDate(paymentReceipt.getDate());
        return item;
    }

    private FacilityMovementDto.FaultItem toFaultItem(Fault fault) {
        FacilityMovementDto.FaultItem item = new FacilityMovementDto.FaultItem();
        item.setId(fault.getId());
        item.setElevatorId(fault.getElevator() != null ? fault.getElevator().getId() : null);
        item.setSubject(fault.getFaultSubject());
        item.setStatus(fault.getStatus() != null ? fault.getStatus().name() : null);
        item.setCreatedAt(fault.getCreatedAt());
        return item;
    }

    private Facility findAccessibleFacility(Long id) {
        Facility facility = findActiveFacility(id);
        enforceObjectAccess(facility);
        return facility;
    }

    private Facility findActiveFacility(Long id) {
        return facilityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Facility not found"));
    }

    private void applyRequest(Facility facility, CreateFacilityRequest request, B2BUnit b2bUnit) {
        facility.setName(normalizeRequired(request.getName()));
        facility.setB2bUnit(b2bUnit);
        facility.setTaxNumber(normalizeNullable(request.getTaxNumber()));
        facility.setTaxOffice(normalizeNullable(request.getTaxOffice()));
        facility.setType(request.getType() != null ? request.getType() : Facility.FacilityType.TUZEL_KISI);
        facility.setInvoiceType(request.getInvoiceType() != null ? request.getInvoiceType() : Facility.FacilityInvoiceType.TICARI_FATURA);
        facility.setCompanyTitle(normalizeNullable(request.getCompanyTitle()));
        facility.setAuthorizedFirstName(normalizeNullable(request.getAuthorizedFirstName()));
        facility.setAuthorizedLastName(normalizeNullable(request.getAuthorizedLastName()));
        facility.setEmail(normalizeNullable(request.getEmail()));
        facility.setPhone(normalizeNullable(request.getPhone()));
        facility.setFacilityType(normalizeNullable(request.getFacilityType()));
        facility.setAttendantFullName(normalizeNullable(request.getAttendantFullName()));
        facility.setManagerFlatNo(normalizeNullable(request.getManagerFlatNo()));
        facility.setDoorPassword(normalizeNullable(request.getDoorPassword()));
        facility.setFloorCount(request.getFloorCount());

        LocationSelection locationSelection = resolveLocationSelection(
                request.getCityId(),
                request.getDistrictId(),
                request.getNeighborhoodId(),
                request.getRegionId()
        );
        facility.setCity(locationSelection.city);
        facility.setDistrict(locationSelection.district);
        facility.setNeighborhood(locationSelection.neighborhood);
        facility.setRegion(locationSelection.region);

        facility.setAddressText(normalizeNullable(request.getAddressText()));
        facility.setDescription(normalizeNullable(request.getDescription()));
        facility.setStatus(request.getStatus() != null ? request.getStatus() : Facility.FacilityStatus.ACTIVE);
        facility.setMapLat(request.getMapLat());
        facility.setMapLng(request.getMapLng());
        facility.setMapAddressQuery(normalizeNullable(request.getMapAddressQuery()));
        facility.setAttachmentUrl(normalizeNullable(request.getAttachmentUrl()));
    }

    private void applyRequest(Facility facility, UpdateFacilityRequest request, B2BUnit b2bUnit) {
        CreateFacilityRequest proxy = new CreateFacilityRequest();
        proxy.setName(request.getName());
        proxy.setB2bUnitId(request.getB2bUnitId());
        proxy.setTaxNumber(request.getTaxNumber());
        proxy.setTaxOffice(request.getTaxOffice());
        proxy.setType(request.getType());
        proxy.setInvoiceType(request.getInvoiceType());
        proxy.setCompanyTitle(request.getCompanyTitle());
        proxy.setAuthorizedFirstName(request.getAuthorizedFirstName());
        proxy.setAuthorizedLastName(request.getAuthorizedLastName());
        proxy.setEmail(request.getEmail());
        proxy.setPhone(request.getPhone());
        proxy.setFacilityType(request.getFacilityType());
        proxy.setAttendantFullName(request.getAttendantFullName());
        proxy.setManagerFlatNo(request.getManagerFlatNo());
        proxy.setDoorPassword(request.getDoorPassword());
        proxy.setFloorCount(request.getFloorCount());
        proxy.setCityId(request.getCityId());
        proxy.setDistrictId(request.getDistrictId());
        proxy.setNeighborhoodId(request.getNeighborhoodId());
        proxy.setRegionId(request.getRegionId());
        proxy.setAddressText(request.getAddressText());
        proxy.setDescription(request.getDescription());
        proxy.setStatus(request.getStatus());
        proxy.setMapLat(request.getMapLat());
        proxy.setMapLng(request.getMapLng());
        proxy.setMapAddressQuery(request.getMapAddressQuery());
        proxy.setAttachmentUrl(request.getAttachmentUrl());

        applyRequest(facility, proxy, b2bUnit);
    }

    private void validateDuplicateNameForCreate(String name, Long b2bUnitId) {
        if (facilityRepository.existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrue(b2bUnitId, normalizeRequired(name))) {
            throw new RuntimeException("Facility name already exists for selected B2B unit");
        }
    }

    private void validateDuplicateNameForUpdate(String name, Long b2bUnitId, Long id) {
        if (facilityRepository.existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrueAndIdNot(b2bUnitId, normalizeRequired(name), id)) {
            throw new RuntimeException("Facility name already exists for selected B2B unit");
        }
    }

    private void validateImportedRequest(CreateFacilityRequest request) {
        String taxNumber = normalizeNullable(request.getTaxNumber());
        if (StringUtils.hasText(taxNumber) && !TAX_NUMBER_PATTERN.matcher(taxNumber).matches()) {
            throw new RuntimeException("Tax number must be 10 or 11 digits");
        }

        String phone = normalizeNullable(request.getPhone());
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new RuntimeException("Phone format is invalid");
        }

        String email = normalizeNullable(request.getEmail());
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Email format is invalid");
        }

        Integer floorCount = request.getFloorCount();
        if (floorCount != null && floorCount < 0) {
            throw new RuntimeException("Floor count must be zero or positive");
        }
    }

    private B2BUnit resolveB2BUnit(Long id) {
        return b2bUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
    }

    private void enforceReadableB2BUnitScopeAccess(Long b2bUnitId) {
        B2BUnit b2bUnit = resolveB2BUnit(b2bUnitId);
        User currentUser = getCurrentUser();
        if (!isCariUser(currentUser)) {
            return;
        }

        Long ownB2bUnitId = getCariB2bUnitId(currentUser);
        if (!b2bUnit.getId().equals(ownB2bUnitId)) {
            throw new AccessDeniedException("CARI user can only access own B2B unit facilities");
        }
    }

    private CreateFacilityRequest toCreateFacilityRequest(B2BUnitFacilityCreateRequest source, Long b2bUnitId) {
        CreateFacilityRequest target = new CreateFacilityRequest();
        target.setName(source.getName());
        target.setB2bUnitId(b2bUnitId);
        target.setTaxNumber(source.getTaxNumber());
        target.setTaxOffice(source.getTaxOffice());
        target.setType(source.getType());
        target.setInvoiceType(source.getInvoiceType());
        target.setCompanyTitle(source.getCompanyTitle());
        target.setAuthorizedFirstName(source.getAuthorizedFirstName());
        target.setAuthorizedLastName(source.getAuthorizedLastName());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setFacilityType(source.getFacilityType());
        target.setAttendantFullName(source.getAttendantFullName());
        target.setManagerFlatNo(source.getManagerFlatNo());
        target.setDoorPassword(source.getDoorPassword());
        target.setFloorCount(source.getFloorCount());
        target.setCityId(source.getCityId());
        target.setDistrictId(source.getDistrictId());
        target.setNeighborhoodId(source.getNeighborhoodId());
        target.setRegionId(source.getRegionId());
        target.setAddressText(source.getAddressText());
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus());
        target.setMapLat(source.getMapLat());
        target.setMapLng(source.getMapLng());
        target.setMapAddressQuery(source.getMapAddressQuery());
        target.setAttachmentUrl(source.getAttachmentUrl());
        return target;
    }

    private B2BUnit resolveB2BUnitForImport(String cariName, boolean createMissingB2BUnit) {
        String normalized = normalizeRequired(cariName);
        Optional<B2BUnit> existing = b2bUnitRepository.findFirstByNameIgnoreCaseAndActiveTrue(normalized);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!createMissingB2BUnit) {
            throw new RuntimeException("B2B unit not found: " + normalized);
        }

        B2BUnit created = new B2BUnit();
        created.setName(normalized);
        created.setCurrency(B2BCurrency.TRY);
        created.setRiskLimit(BigDecimal.ZERO);
        created.setActive(true);
        return b2bUnitRepository.save(created);
    }

    private LocationSelection resolveLocationSelection(Long cityId, Long districtId, Long neighborhoodId, Long regionId) {
        City city = cityId != null ? cityRepository.findById(cityId).orElseThrow(() -> new RuntimeException("City not found")) : null;
        District district = districtId != null ? districtRepository.findById(districtId).orElseThrow(() -> new RuntimeException("District not found")) : null;
        Neighborhood neighborhood = neighborhoodId != null ? neighborhoodRepository.findById(neighborhoodId).orElseThrow(() -> new RuntimeException("Neighborhood not found")) : null;
        Region region = regionId != null ? regionRepository.findById(regionId).orElseThrow(() -> new RuntimeException("Region not found")) : null;

        if (district != null) {
            if (city != null && !district.getCity().getId().equals(city.getId())) {
                throw new RuntimeException("District does not belong to selected city");
            }
            city = district.getCity();
        }

        if (neighborhood != null) {
            if (district != null && !neighborhood.getDistrict().getId().equals(district.getId())) {
                throw new RuntimeException("Neighborhood does not belong to selected district");
            }
            district = neighborhood.getDistrict();
            city = district.getCity();
        }

        if (region != null) {
            if (neighborhood != null && !region.getNeighborhood().getId().equals(neighborhood.getId())) {
                throw new RuntimeException("Region does not belong to selected neighborhood");
            }
            neighborhood = region.getNeighborhood();
            district = neighborhood.getDistrict();
            city = district.getCity();
        }

        return new LocationSelection(city, district, neighborhood, region);
    }

    private LocationSelection resolveLocationByNames(String cityName,
                                                     String districtName,
                                                     String neighborhoodName,
                                                     String regionName) {
        City city = null;
        District district = null;
        Neighborhood neighborhood = null;
        Region region = null;

        if (StringUtils.hasText(cityName)) {
            city = findOrCreateCity(cityName);
        }

        if (StringUtils.hasText(districtName)) {
            if (city == null) {
                throw new RuntimeException("IL is required when ILCE is provided");
            }
            district = findOrCreateDistrict(city, districtName);
        }

        if (StringUtils.hasText(neighborhoodName)) {
            if (district == null) {
                throw new RuntimeException("ILCE is required when MAHALLE is provided");
            }
            neighborhood = findOrCreateNeighborhood(district, neighborhoodName);
        }

        if (StringUtils.hasText(regionName)) {
            if (neighborhood == null) {
                throw new RuntimeException("MAHALLE is required when BOLGE is provided");
            }
            region = findOrCreateRegion(neighborhood, regionName);
        }

        return new LocationSelection(city, district, neighborhood, region);
    }

    private City findOrCreateCity(String cityName) {
        String normalized = normalizeRequired(cityName);
        return cityRepository.findFirstByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    City city = new City();
                    city.setName(normalized);
                    return cityRepository.save(city);
                });
    }

    private District findOrCreateDistrict(City city, String districtName) {
        String normalized = normalizeRequired(districtName);
        return districtRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), normalized)
                .orElseGet(() -> {
                    District district = new District();
                    district.setCity(city);
                    district.setName(normalized);
                    return districtRepository.save(district);
                });
    }

    private Neighborhood findOrCreateNeighborhood(District district, String neighborhoodName) {
        String normalized = normalizeRequired(neighborhoodName);
        return neighborhoodRepository.findFirstByDistrictIdAndNameIgnoreCase(district.getId(), normalized)
                .orElseGet(() -> {
                    Neighborhood neighborhood = new Neighborhood();
                    neighborhood.setDistrict(district);
                    neighborhood.setName(normalized);
                    return neighborhoodRepository.save(neighborhood);
                });
    }

    private Region findOrCreateRegion(Neighborhood neighborhood, String regionName) {
        String normalized = normalizeRequired(regionName);
        return regionRepository.findFirstByNeighborhoodIdAndNameIgnoreCase(neighborhood.getId(), normalized)
                .orElseGet(() -> {
                    Region region = new Region();
                    region.setNeighborhood(neighborhood);
                    region.setName(normalized);
                    return regionRepository.save(region);
                });
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (StringUtils.hasText(header)) {
                map.put(header, cell.getColumnIndex());
            }
        }
        return map;
    }

    private void validateHeaders(Map<String, Integer> headerMap) {
        List<String> missing = new ArrayList<>();
        for (String requiredHeader : REQUIRED_IMPORT_HEADERS) {
            if (!headerMap.containsKey(requiredHeader)) {
                missing.add(requiredHeader);
            }
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Missing required headers: " + String.join(", ", missing));
        }
    }

    private String readString(Row row, Map<String, Integer> headerMap, String header, DataFormatter formatter) {
        Integer index = headerMap.get(header);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        return normalizeNullable(formatter.formatCellValue(cell));
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (StringUtils.hasText(normalizeNullable(formatter.formatCellValue(cell)))) {
                return false;
            }
        }
        return true;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("KAT SAYISI must be numeric");
        }
    }

    private void enforceObjectAccess(Facility facility) {
        User currentUser = getCurrentUser();
        if (!isCariUser(currentUser)) {
            return;
        }
        Long ownB2bUnitId = getCariB2bUnitId(currentUser);
        Long targetB2bUnitId = facility.getB2bUnit() != null ? facility.getB2bUnit().getId() : null;
        if (ownB2bUnitId == null || targetB2bUnitId == null || !ownB2bUnitId.equals(targetB2bUnitId)) {
            throw new AccessDeniedException("CARI user can only access own B2B unit facilities");
        }
    }

    private void enforceNonCariWrite() {
        User currentUser = getCurrentUser();
        if (isCariUser(currentUser)) {
            throw new AccessDeniedException("CARI user cannot modify facilities");
        }
    }

    private boolean canViewDoorPassword(User currentUser) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() != User.Role.CARI_USER;
    }

    private boolean isCariUser(User currentUser) {
        return currentUser != null && currentUser.getRole() == User.Role.CARI_USER;
    }

    private Long getCariB2bUnitId(User currentUser) {
        if (currentUser == null || currentUser.getB2bUnit() == null) {
            throw new AccessDeniedException("CARI user is not linked to a B2B unit");
        }
        return currentUser.getB2bUnit().getId();
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
        return userRepository.findByUsername(username).orElse(null);
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String replaced = value.replace('İ', 'I').replace('ı', 'i');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String gridCell(String label, Object value) {
        return "<div><span class=\"label\">" + escapeHtml(label) + ":</span> " + escapeHtml(value) + "</div>";
    }

    private String td(Object value) {
        return "<td>" + escapeHtml(value) + "</td>";
    }

    private String joinWithSlash(String first, String second) {
        if (!StringUtils.hasText(first) && !StringUtils.hasText(second)) {
            return null;
        }
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + " / " + second;
    }

    private String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record LocationSelection(City city, District district, Neighborhood neighborhood, Region region) {
    }
}
