package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitElevatorCreateRequest;
import com.saraasansor.api.dto.B2BUnitElevatorListItemResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.dto.ElevatorStatusDto;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.WarningDto;
import com.saraasansor.api.dto.WarningElevatorDto;
import com.saraasansor.api.dto.WarningGroupDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.LabelType;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.util.AuditLogger;
import com.saraasansor.api.util.LabelDurationCalculator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ElevatorService {
    
    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private B2BUnitRepository b2bUnitRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogger auditLogger;
    
    public List<ElevatorDto> getAllElevators() {
        return elevatorRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<B2BUnitElevatorListItemResponse> getElevatorsByB2BUnit(Long b2bUnitId, String search, Pageable pageable) {
        enforceReadableB2BUnitScopeAccess(b2bUnitId);

        List<Facility> facilities = facilityRepository.findByB2bUnitIdAndActiveTrue(b2bUnitId);
        if (facilities.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Facility> facilityById = facilities.stream()
                .collect(Collectors.toMap(Facility::getId, facility -> facility));
        Map<String, Facility> facilityByName = facilities.stream()
                .collect(Collectors.toMap(
                        facility -> normalizeKey(facility.getName()),
                        facility -> facility,
                        (existing, ignored) -> existing
                ));
        List<String> buildingNames = new ArrayList<>(facilityByName.keySet());
        List<Long> facilityIds = new ArrayList<>(facilityById.keySet());

        Page<Elevator> elevators = elevatorRepository.searchByFacilityIdsOrLegacyBuildingNames(
                facilityIds,
                buildingNames,
                normalizeNullable(search),
                pageable
        );
        if (elevators == null) {
            elevators = elevatorRepository.searchByBuildingNames(
                    buildingNames,
                    normalizeNullable(search),
                    pageable
            );
        }

        List<B2BUnitElevatorListItemResponse> content = elevators.getContent().stream()
                .map(elevator -> toListItem(elevator, facilityById, facilityByName))
                .toList();

        return new PageImpl<>(content, pageable, elevators.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(Long facilityId, String query) {
        if (facilityId == null) {
            throw new RuntimeException("facilityId is required");
        }

        Facility facility = resolveFacility(facilityId);

        String normalizedQuery = normalizeNullable(query);
        return collectFacilityElevators(facility).stream()
                .filter(elevator -> matchLookupQuery(elevator, normalizedQuery))
                .sorted(Comparator
                        .comparing((Elevator elevator) -> normalizeNullable(elevator.getElevatorNumber()),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(elevator -> normalizeNullable(elevator.getIdentityNumber()),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toLookupDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] generateElevatorReportPdf(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Elevator not found"));
        String tenantDisplayName = resolveTenantDisplayName();
        String tenantHeader = tenantDisplayName.toUpperCase(Locale.ROOT);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();
                float margin = 42f;
                float contentWidth = pageWidth - (margin * 2);

                // Header band
                content.setNonStrokingColor(new Color(25, 48, 93));
                content.addRect(0, pageHeight - 112, pageWidth, 112);
                content.fill();
                content.setNonStrokingColor(Color.WHITE);
                drawText(content, PDType1Font.HELVETICA_BOLD, 16, margin, pageHeight - 34, tenantHeader);
                drawText(content, PDType1Font.HELVETICA_BOLD, 26, margin, pageHeight - 66, "Asansor Durum Raporu");
                drawText(content, PDType1Font.HELVETICA, 11, margin, pageHeight - 86, "Asenovo Kurumsal Teknik Raporu");
                drawText(content, PDType1Font.HELVETICA, 10, margin, pageHeight - 102, "Raporlanan Firma: " + tenantDisplayName);

                // Metadata row
                float metaTop = pageHeight - 135;
                content.setStrokingColor(new Color(210, 215, 224));
                content.setNonStrokingColor(new Color(245, 247, 251));
                content.addRect(margin, metaTop - 42, contentWidth, 38);
                content.fill();
                content.setNonStrokingColor(Color.BLACK);
                content.addRect(margin, metaTop - 42, contentWidth, 38);
                content.stroke();
                drawText(content, PDType1Font.HELVETICA_BOLD, 10, margin + 10, metaTop - 20,
                        "Rapor No: REP-" + elevator.getId());
                drawText(content, PDType1Font.HELVETICA_BOLD, 10, margin + 190, metaTop - 20,
                        "Asansor ID: " + safe(elevator.getId()));
                drawText(content, PDType1Font.HELVETICA_BOLD, 10, margin + 350, metaTop - 20,
                        "Tarih: " + safe(LocalDate.now()));
                drawText(content, PDType1Font.HELVETICA, 9, margin + 10, metaTop - 34,
                        "Platform: Asenovo");

                // Body sections
                float sectionY = metaTop - 70;
                drawSectionTitle(content, margin, sectionY, "Asansor Bilgileri");
                sectionY -= 18;
                sectionY = drawInfoGrid(content, margin, sectionY, contentWidth,
                        "Kimlik No", safe(elevator.getIdentityNumber()),
                        "Asansor No", safe(elevator.getElevatorNumber()),
                        "Bina", safe(elevator.getBuildingName()),
                        "Adres", safe(elevator.getAddress()),
                        "Kapasite", safe(elevator.getCapacity()),
                        "Hiz", safe(elevator.getSpeed()) + " m/s");

                sectionY -= 14;
                drawSectionTitle(content, margin, sectionY, "Etiket ve Muayene");
                sectionY -= 18;
                sectionY = drawInfoGrid(content, margin, sectionY, contentWidth,
                        "Etiket Tipi", toTurkishLabelType(elevator.getLabelType()),
                        "Durum", toTurkishStatus(elevator.getStatus()),
                        "Muayene Tarihi", safe(elevator.getInspectionDate()),
                        "Etiket Baslangic", safe(elevator.getLabelDate()),
                        "Etiket Bitis", safe(elevator.getExpiryDate()),
                        "Mavi Etiket", toTurkishBoolean(elevator.getBlueLabel()));

                sectionY -= 14;
                drawSectionTitle(content, margin, sectionY, "Sorumlu Kisi");
                sectionY -= 18;
                sectionY = drawInfoGrid(content, margin, sectionY, contentWidth,
                        "Ad Soyad", safe(elevator.getManagerName()),
                        "Telefon", safe(elevator.getManagerPhone()),
                        "TC Kimlik", safe(elevator.getManagerTcIdentityNo()),
                        "E-Posta", safe(elevator.getManagerEmail()),
                        "Makine Marka", safe(elevator.getMachineBrand()),
                        "Kontrol Sistemi", safe(elevator.getControlSystem()));

                // Footer
                content.setStrokingColor(new Color(210, 215, 224));
                content.moveTo(margin, 56);
                content.lineTo(pageWidth - margin, 56);
                content.stroke();
                drawText(content, PDType1Font.HELVETICA_OBLIQUE, 9, margin, 42,
                        "Bu rapor Asenovo platformu tarafindan otomatik olusturulmustur.");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate elevator PDF report", ex);
        }
    }
    
    public ElevatorDto getElevatorById(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        ElevatorDto dto = ElevatorDto.fromEntity(elevator);
        
        // Temporary log: Verify manager info is returned
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.info("Returning manager info: tc={}, name={}, phone={}", 
            dto.getManagerTcIdentityNo(), 
            dto.getManagerName(), 
            dto.getManagerPhone());
        
        return dto;
    }

    private LookupDto toLookupDto(Elevator elevator) {
        String name;
        if (StringUtils.hasText(elevator.getElevatorNumber())) {
            name = elevator.getElevatorNumber();
        } else if (StringUtils.hasText(elevator.getIdentityNumber())) {
            name = elevator.getIdentityNumber();
        } else {
            name = "Elevator #" + elevator.getId();
        }
        return new LookupDto(elevator.getId(), name);
    }

    private boolean matchLookupQuery(Elevator elevator, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return (StringUtils.hasText(elevator.getElevatorNumber()) && elevator.getElevatorNumber().toLowerCase(Locale.ROOT).contains(normalized))
                || (StringUtils.hasText(elevator.getIdentityNumber()) && elevator.getIdentityNumber().toLowerCase(Locale.ROOT).contains(normalized))
                || (StringUtils.hasText(elevator.getBuildingName()) && elevator.getBuildingName().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
    
    public ElevatorDto createElevator(ElevatorDto dto) {
        // VALIDATION: Required fields
        validateElevatorDto(dto);

        Facility facility = resolveFacility(dto.getFacilityId());
        
        if (elevatorRepository.existsByIdentityNumber(dto.getIdentityNumber())) {
            throw new RuntimeException("This identity number is already in use");
        }
        
        Elevator elevator = new Elevator();
        mapDtoToEntity(dto, elevator, facility);
        
        // VALIDATION: Label date is required
        if (elevator.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Label type is required
        if (elevator.getLabelType() == null) {
            throw new RuntimeException("Label type is required");
        }
        
        // VALIDATION: Expiry date is REQUIRED
        if (elevator.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (elevator.getLabelDate() != null && !elevator.getExpiryDate().isAfter(elevator.getLabelDate())) {
            throw new RuntimeException("End date must be after label date");
        }
        
        // Calculate status from expiryDate (duration calculation removed - expiryDate must be provided)
        elevator.setStatus(LabelDurationCalculator.calculateStatus(elevator.getExpiryDate()));
        
        Elevator saved = elevatorRepository.save(elevator);
        
        // Log periodic date update
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("labelDate", saved.getLabelDate());
        metadata.put("labelType", saved.getLabelType() != null ? saved.getLabelType().name() : null);
        metadata.put("expiryDate", saved.getExpiryDate());
        metadata.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        auditLogger.log("ELEVATOR_CREATED", "ELEVATOR", saved.getId(), metadata);
        
        return ElevatorDto.fromEntity(saved);
    }

    public ElevatorDto createElevatorForB2BUnit(Long b2bUnitId, B2BUnitElevatorCreateRequest request) {
        enforceNonCariWrite();
        enforceReadableB2BUnitScopeAccess(b2bUnitId);

        Facility facility = resolveFacility(request.getFacilityId());
        Long facilityB2bUnitId = facility.getB2bUnit() != null ? facility.getB2bUnit().getId() : null;
        if (facilityB2bUnitId == null || !facilityB2bUnitId.equals(b2bUnitId)) {
            throw new AccessDeniedException("Facility does not belong to selected B2B unit");
        }

        ElevatorDto dto = mapScopedCreateRequestToElevatorDto(request, facility);
        return createElevator(dto);
    }
    
    public ElevatorDto updateElevator(Long id, ElevatorDto dto) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));

        Facility facility = resolveFacility(dto.getFacilityId());
        
        // Check identityNumber uniqueness if changed
        if (!elevator.getIdentityNumber().equals(dto.getIdentityNumber()) && 
            elevatorRepository.existsByIdentityNumber(dto.getIdentityNumber())) {
            throw new RuntimeException("This identity number is already in use");
        }
        
        LocalDate oldInspectionDate = elevator.getInspectionDate();
        LocalDate oldExpiryDate = elevator.getExpiryDate();
        
        // Log before mapping
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.info(
            "Updating elevator: floorCount={}, capacity={}, speed={}, inspectionDate={}, labelType={}, labelDate={}",
            dto.getFloorCount(),
            dto.getCapacity(),
            dto.getSpeed(),
            dto.getInspectionDate(),
            dto.getLabelType(),
            dto.getLabelDate()
        );
        
        // VALIDATION: Required fields
        validateElevatorDto(dto);
        
        mapDtoToEntity(dto, elevator, facility);
        
        // VALIDATION: Label date is required
        if (elevator.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Label type is required
        if (elevator.getLabelType() == null) {
            throw new RuntimeException("Label type is required");
        }
        
        // VALIDATION: Expiry date is REQUIRED
        if (elevator.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (elevator.getLabelDate() != null && !elevator.getExpiryDate().isAfter(elevator.getLabelDate())) {
            throw new RuntimeException("End date must be after label date");
        }
        
        // Calculate status from expiryDate (duration calculation removed - expiryDate must be provided)
        elevator.setStatus(LabelDurationCalculator.calculateStatus(elevator.getExpiryDate()));
        
        Elevator saved = elevatorRepository.save(elevator);
        
        // Log periodic date update if changed
        if (oldInspectionDate == null || !oldInspectionDate.equals(saved.getInspectionDate()) ||
            oldExpiryDate == null || !oldExpiryDate.equals(saved.getExpiryDate())) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("inspectionDate", saved.getInspectionDate());
            metadata.put("expiryDate", saved.getExpiryDate());
            auditLogger.log("PERIODIC_DATE_UPDATED", "ELEVATOR", saved.getId(), metadata);
        }
        
        return ElevatorDto.fromEntity(saved);
    }
    
    public void deleteElevator(Long id) {
        if (!elevatorRepository.existsById(id)) {
            throw new RuntimeException("Elevator not found");
        }
        elevatorRepository.deleteById(id);
    }
    
    public ElevatorStatusDto getElevatorStatus(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        LocalDate now = LocalDate.now();
        LocalDate expiryDate = elevator.getExpiryDate();
        
        ElevatorStatusDto status = new ElevatorStatusDto();
        status.setIdentityNumber(elevator.getIdentityNumber());
        status.setBuildingName(elevator.getBuildingName());
        status.setExpiryDate(expiryDate);
        
        long daysLeft = ChronoUnit.DAYS.between(now, expiryDate);
        status.setDaysLeft(daysLeft);
        
        if (now.isAfter(expiryDate)) {
            status.setStatus("EXPIRED");
        } else if (daysLeft <= 30) {
            status.setStatus("WARNING");
        } else {
            status.setStatus("OK");
        }
        
        return status;
    }
    
    public List<ElevatorDto> getExpiredElevators() {
        return elevatorRepository.findExpiredElevators(LocalDate.now()).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<ElevatorDto> getExpiringSoonElevators() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get expired elevators as WarningDto with complete information
     * Returns elevators with identityNo, buildingName, address, maintenanceEndDate, and status
     */
    public List<WarningDto> getExpiredElevatorsAsWarnings() {
        LocalDate now = LocalDate.now();
        return elevatorRepository.findExpiredElevators(now).stream()
                .map(elevator -> WarningDto.fromEntity(elevator, "EXPIRED"))
                .collect(Collectors.toList());
    }
    
    /**
     * Get expiring soon elevators as WarningDto with complete information
     * Returns elevators with identityNo, buildingName, address, maintenanceEndDate, and status
     */
    public List<WarningDto> getExpiringSoonElevatorsAsWarnings() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).stream()
                .map(elevator -> WarningDto.fromEntity(elevator, "WARNING"))
                .collect(Collectors.toList());
    }
    
    /**
     * Get warnings grouped by building (buildingName + address)
     * Groups elevators by building and returns structured data for frontend expansion
     * 
     * Sorting:
     * - EXPIRED buildings first
     * - Then WARNING buildings
     * - Within each group, elevators sorted by maintenanceEndDate ASC
     * 
     * @param type Optional: "EXPIRED", "WARNING", or null (both)
     * @return List of WarningGroupDto grouped by building
     */
    public List<WarningGroupDto> getGroupedWarnings(String type) {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        
        List<Elevator> expiredElevators = new ArrayList<>();
        List<Elevator> warningElevators = new ArrayList<>();
        
        if (type == null || "EXPIRED".equalsIgnoreCase(type)) {
            expiredElevators = elevatorRepository.findExpiredElevators(now);
        }
        
        if (type == null || "WARNING".equalsIgnoreCase(type)) {
            warningElevators = elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater);
        }
        
        // Group expired elevators by building (buildingName + address)
        Map<String, WarningGroupDto> expiredGroups = expiredElevators.stream()
                .collect(Collectors.groupingBy(
                    elevator -> buildGroupKey(elevator.getBuildingName(), elevator.getAddress()),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        elevators -> {
                            Elevator first = elevators.get(0);
                            List<WarningElevatorDto> elevatorDtos = elevators.stream()
                                    .sorted(Comparator.comparing(Elevator::getExpiryDate))
                                    .map(e -> new WarningElevatorDto(
                                        e.getIdentityNumber() != null ? e.getIdentityNumber() : "",
                                        e.getExpiryDate(),
                                        "EXPIRED"
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new WarningGroupDto(
                                first.getBuildingName() != null ? first.getBuildingName() : "",
                                first.getAddress() != null ? first.getAddress() : "",
                                "EXPIRED",
                                elevatorDtos
                            );
                        }
                    )
                ));
        
        // Group warning elevators by building (buildingName + address)
        Map<String, WarningGroupDto> warningGroups = warningElevators.stream()
                .collect(Collectors.groupingBy(
                    elevator -> buildGroupKey(elevator.getBuildingName(), elevator.getAddress()),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        elevators -> {
                            Elevator first = elevators.get(0);
                            List<WarningElevatorDto> elevatorDtos = elevators.stream()
                                    .sorted(Comparator.comparing(Elevator::getExpiryDate))
                                    .map(e -> new WarningElevatorDto(
                                        e.getIdentityNumber() != null ? e.getIdentityNumber() : "",
                                        e.getExpiryDate(),
                                        "WARNING"
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new WarningGroupDto(
                                first.getBuildingName() != null ? first.getBuildingName() : "",
                                first.getAddress() != null ? first.getAddress() : "",
                                "WARNING",
                                elevatorDtos
                            );
                        }
                    )
                ));
        
        // Combine and sort: EXPIRED first, then WARNING
        List<WarningGroupDto> result = new ArrayList<>();
        result.addAll(expiredGroups.values());
        result.addAll(warningGroups.values());
        
        return result;
    }
    
    /**
     * Build a unique key for grouping by buildingName and address
     */
    private String buildGroupKey(String buildingName, String address) {
        String name = buildingName != null ? buildingName : "";
        String addr = address != null ? address : "";
        return name + "|" + addr; // Use pipe separator to combine
    }
    
    private void mapDtoToEntity(ElevatorDto dto, Elevator entity, Facility facility) {
        entity.setIdentityNumber(dto.getIdentityNumber());
        entity.setFacility(facility);
        entity.setBuildingName(facility.getName());
        String resolvedAddress = StringUtils.hasText(dto.getAddress()) ? dto.getAddress() : facility.getAddressText();
        if (!StringUtils.hasText(resolvedAddress)) {
            throw new RuntimeException("Address is required");
        }
        entity.setAddress(resolvedAddress);
        entity.setElevatorNumber(dto.getElevatorNumber());
        entity.setFloorCount(dto.getFloorCount());
        entity.setCapacity(dto.getCapacity());
        entity.setSpeed(dto.getSpeed());
        entity.setTechnicalNotes(dto.getTechnicalNotes());
        entity.setDriveType(dto.getDriveType());
        entity.setMachineBrand(dto.getMachineBrand());
        entity.setDoorType(dto.getDoorType());
        entity.setInstallationYear(dto.getInstallationYear());
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setControlSystem(dto.getControlSystem());
        entity.setRope(dto.getRope());
        entity.setModernization(dto.getModernization());
        // Map inspectionDate - if not provided, use labelDate as fallback (inspection_date is NOT NULL)
        if (dto.getInspectionDate() != null) {
            entity.setInspectionDate(dto.getInspectionDate());
        } else if (dto.getLabelDate() != null) {
            // Fallback: use labelDate as inspectionDate (inspection_date is NOT NULL)
            entity.setInspectionDate(dto.getLabelDate());
        } else {
            // Both are null - this should not happen due to validation, but set a default
            throw new RuntimeException("Inspection date is required. Please provide inspectionDate or labelDate.");
        }
        
        entity.setBlueLabel(dto.getBlueLabel());
        
        // Map label fields
        if (dto.getLabelDate() != null) {
            entity.setLabelDate(dto.getLabelDate());
        } else if (dto.getInspectionDate() != null) {
            // Fallback: use inspectionDate as labelDate
            entity.setLabelDate(dto.getInspectionDate());
        } else {
            // This should not happen due to validation
            throw new RuntimeException("Label date is required");
        }
        
        // Label type is REQUIRED - parse from DTO
        if (dto.getLabelType() != null && !dto.getLabelType().trim().isEmpty()) {
            try {
                entity.setLabelType(LabelType.valueOf(dto.getLabelType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid label type: " + dto.getLabelType() + ". Valid values: GREEN, YELLOW, RED, ORANGE, BLUE");
            }
        } else {
            // Label type is required - cannot be null
            throw new RuntimeException("Label type is required");
        }
        
        // Map manager fields
        entity.setManagerName(dto.getManagerName());
        entity.setManagerTcIdentityNo(dto.getManagerTcIdentityNo());
        entity.setManagerPhone(dto.getManagerPhone());
        entity.setManagerEmail(dto.getManagerEmail());
        
        // Map expiryDate if provided (otherwise will be calculated from labelDate + duration)
        if (dto.getExpiryDate() != null) {
            entity.setExpiryDate(dto.getExpiryDate());
        }
        
        // Status will be calculated from expiryDate (either provided or calculated)
    }
    
    /**
     * Validate ElevatorDto for required fields and business rules
     */
    private void validateElevatorDto(ElevatorDto dto) {
        // Required fields validation
        if (dto.getIdentityNumber() == null || dto.getIdentityNumber().trim().isEmpty()) {
            throw new RuntimeException("Identity number is required");
        }

        if (dto.getFacilityId() == null) {
            throw new RuntimeException("facilityId is required");
        }
        
        if (dto.getLabelType() == null || dto.getLabelType().trim().isEmpty()) {
            throw new RuntimeException("Label type is required");
        }
        
        if (dto.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Inspection date is REQUIRED (or use labelDate as fallback)
        // Note: inspection_date column is NOT NULL in database
        if (dto.getInspectionDate() == null && dto.getLabelDate() == null) {
            throw new RuntimeException("Inspection date is required. Please provide inspectionDate or labelDate.");
        }
        
        // VALIDATION: Expiry date is REQUIRED (must be provided by frontend)
        if (dto.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (dto.getLabelDate() != null && dto.getExpiryDate() != null) {
            if (!dto.getExpiryDate().isAfter(dto.getLabelDate())) {
                throw new RuntimeException("End date must be after label date");
            }
        }
        
        // Manager validation: Name (mandatory)
        if (dto.getManagerName() == null || dto.getManagerName().trim().isEmpty()) {
            throw new RuntimeException("Manager name is required");
        }
        
        // Manager validation: TC Identity Number (mandatory)
        if (dto.getManagerTcIdentityNo() == null || dto.getManagerTcIdentityNo().trim().isEmpty()) {
            throw new RuntimeException("Manager TC Identity Number is required");
        }
        
        // TC Identity Number: Exactly 11 digits, numeric only
        String tcIdentityNo = dto.getManagerTcIdentityNo().trim();
        if (!tcIdentityNo.matches("^[0-9]{11}$")) {
            throw new RuntimeException("Manager TC Identity Number must be exactly 11 digits");
        }
        
        // Manager validation: Phone Number (mandatory)
        // Note: Phone number is already normalized in DTO setter
        if (dto.getManagerPhone() == null || dto.getManagerPhone().trim().isEmpty()) {
            throw new RuntimeException("Manager phone number is required");
        }
        
        // Phone Number: 10 or 11 digits (Turkish format, digits only)
        // Normalization already done in DTO setter (spaces, dashes, parentheses removed)
        String phone = dto.getManagerPhone();
        
        // Temporary debug logging
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.debug("Validating manager phone number: '{}' (length: {})", phone, phone != null ? phone.length() : 0);
        
        if (!phone.matches("^(0?[0-9]{10})$")) {
            throw new RuntimeException("Phone number must be 10 or 11 digits (Turkish format, digits only)");
        }
        
        // Validate endDate > labelDate
        if (dto.getExpiryDate() != null && dto.getLabelDate() != null) {
            if (!dto.getExpiryDate().isAfter(dto.getLabelDate())) {
                throw new RuntimeException("End date must be after label date");
            }
        }
    }

    private ElevatorDto mapScopedCreateRequestToElevatorDto(B2BUnitElevatorCreateRequest request, Facility facility) {
        ElevatorDto dto = new ElevatorDto();
        dto.setIdentityNumber(request.getIdentityNumber());
        dto.setFacilityId(facility.getId());
        dto.setElevatorNumber(request.getName());
        dto.setBuildingName(facility.getName());
        dto.setAddress(StringUtils.hasText(request.getAddressText()) ? request.getAddressText() : facility.getAddressText());
        dto.setDriveType(request.getMaintenanceType());
        dto.setMachineBrand(request.getBrand());
        dto.setDoorType(request.getDoorType());
        dto.setInstallationYear(request.getConstructionYear());
        dto.setFloorCount(request.getStopCount());
        dto.setCapacity(request.getCapacity());
        dto.setSpeed(request.getSpeed());
        dto.setTechnicalNotes(request.getDescription());
        dto.setLabelDate(request.getLabelDate());
        dto.setInspectionDate(request.getLabelDate());
        dto.setLabelType(request.getLabelType());
        dto.setExpiryDate(request.getExpiryDate() != null ? request.getExpiryDate() : request.getWarrantyEndDate());
        dto.setManagerName(request.getManagerName());
        dto.setManagerTcIdentityNo(request.getManagerTcIdentityNo());
        dto.setManagerPhone(request.getManagerPhone());
        dto.setManagerEmail(request.getManagerEmail());
        return dto;
    }

    private B2BUnitElevatorListItemResponse toListItem(Elevator elevator,
                                                       Map<Long, Facility> facilityById,
                                                       Map<String, Facility> facilityByName) {
        Facility facility = elevator.getFacility() != null
                ? facilityById.get(elevator.getFacility().getId())
                : facilityByName.get(normalizeKey(elevator.getBuildingName()));
        B2BUnitElevatorListItemResponse response = new B2BUnitElevatorListItemResponse();
        response.setId(elevator.getId());
        response.setName(StringUtils.hasText(elevator.getElevatorNumber()) ? elevator.getElevatorNumber() : elevator.getIdentityNumber());
        response.setFacilityId(facility != null ? facility.getId() : null);
        response.setFacilityName(facility != null ? facility.getName() : elevator.getBuildingName());
        response.setIdentityNumber(elevator.getIdentityNumber());
        response.setBrand(elevator.getMachineBrand());
        response.setStatus(elevator.getStatus() != null ? elevator.getStatus().name() : null);
        response.setCreatedAt(elevator.getCreatedAt());
        return response;
    }

    private String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Facility resolveFacility(Long facilityId) {
        if (facilityId == null) {
            throw new RuntimeException("facilityId is required");
        }
        return facilityRepository.findByIdAndActiveTrue(facilityId)
                .orElseThrow(() -> new RuntimeException("Facility not found"));
    }

    private List<Elevator> collectFacilityElevators(Facility facility) {
        List<Elevator> relationElevators = elevatorRepository.findByFacilityId(facility.getId());
        if (relationElevators == null) {
            relationElevators = List.of();
        }
        List<Elevator> legacyCandidates = elevatorRepository.findByBuildingNameIgnoreCase(facility.getName());
        if (legacyCandidates == null) {
            legacyCandidates = List.of();
        }
        List<Elevator> legacyElevators = legacyCandidates.stream()
                .filter(elevator -> elevator.getFacility() == null)
                .toList();

        if (legacyElevators.isEmpty()) {
            return relationElevators;
        }

        Map<Long, Elevator> merged = new LinkedHashMap<>();
        relationElevators.forEach(elevator -> merged.put(elevator.getId(), elevator));
        legacyElevators.forEach(elevator -> merged.put(elevator.getId(), elevator));
        return new ArrayList<>(merged.values());
    }

    private void enforceReadableB2BUnitScopeAccess(Long b2bUnitId) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));

        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.Role.CARI_USER) {
            return;
        }

        Long ownB2bUnitId = currentUser.getB2bUnit() != null ? currentUser.getB2bUnit().getId() : null;
        if (ownB2bUnitId == null || !ownB2bUnitId.equals(b2bUnit.getId())) {
            throw new AccessDeniedException("CARI user can only access own B2B unit elevators");
        }
    }

    private void enforceNonCariWrite() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException("CARI user cannot modify elevators");
        }
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
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }

    private float drawInfoGrid(PDPageContentStream content,
                               float x,
                               float y,
                               float width,
                               String k1, String v1,
                               String k2, String v2,
                               String k3, String v3,
                               String k4, String v4,
                               String k5, String v5,
                               String k6, String v6) throws IOException {
        float colSplit = x + (width * 0.34f);
        float valueWidth = width - (colSplit - x) - 16f;
        String[] keys = {k1, k2, k3, k4, k5, k6};
        String[] vals = {v1, v2, v3, v4, v5, v6};
        float currentTop = y + 6f;

        for (int i = 0; i < 6; i++) {
            List<String> wrappedLines = wrapText(vals[i], PDType1Font.HELVETICA, 9, valueWidth);
            if (wrappedLines.isEmpty()) {
                wrappedLines = List.of("-");
            }
            float rowHeight = Math.max(24f, 10f + (wrappedLines.size() * 11f));
            float rowBottom = currentTop - rowHeight;

            if (i % 2 == 0) {
                content.setNonStrokingColor(new Color(248, 250, 253));
                content.addRect(x, rowBottom, width, rowHeight);
                content.fill();
                content.setNonStrokingColor(Color.BLACK);
            }

            content.setStrokingColor(new Color(223, 227, 235));
            content.addRect(x, rowBottom, width, rowHeight);
            content.stroke();
            content.moveTo(colSplit, rowBottom);
            content.lineTo(colSplit, currentTop);
            content.stroke();

            float textY = currentTop - 14f;
            drawText(content, PDType1Font.HELVETICA_BOLD, 9, x + 8, textY, keys[i]);
            for (int lineIndex = 0; lineIndex < wrappedLines.size(); lineIndex++) {
                drawText(content, PDType1Font.HELVETICA, 9, colSplit + 8, textY - (lineIndex * 11f), wrappedLines.get(lineIndex));
            }
            currentTop = rowBottom;
        }
        return currentTop - 6f;
    }

    private void drawSectionTitle(PDPageContentStream content, float x, float y, String title) throws IOException {
        content.setNonStrokingColor(new Color(32, 64, 123));
        drawText(content, PDType1Font.HELVETICA_BOLD, 12, x, y, title);
        content.setNonStrokingColor(Color.BLACK);
    }

    private void drawText(PDPageContentStream content,
                          PDType1Font font,
                          float size,
                          float x,
                          float y,
                          String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(toPdfSafeText(text));
        content.endText();
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String resolveTenantDisplayName() {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return "Asenovo";
        }
        if (tenant.getSubdomain() != null && !tenant.getSubdomain().isBlank()) {
            return tenant.getSubdomain();
        }
        if (tenant.getName() != null && !tenant.getName().isBlank()) {
            return tenant.getName();
        }
        return "Asenovo";
    }

    private String toTurkishLabelType(LabelType labelType) {
        if (labelType == null) {
            return "-";
        }
        return switch (labelType) {
            case BLUE -> "Mavi";
            case GREEN -> "Yesil";
            case YELLOW -> "Sari";
            case RED -> "Kirmizi";
            case ORANGE -> "Turuncu";
        };
    }

    private String toTurkishStatus(Elevator.Status status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case ACTIVE -> "Aktif";
            case EXPIRED -> "Suresi Dolmus";
        };
    }

    private String toTurkishBoolean(Boolean value) {
        if (value == null) {
            return "-";
        }
        return value ? "Var" : "Yok";
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        String normalized = safe(text);
        List<String> lines = new ArrayList<>();
        String[] words = normalized.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = font.getStringWidth(toPdfSafeText(candidate)) / 1000f * fontSize;
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.isEmpty()) {
            lines.add("-");
        }
        return lines;
    }

    private String toPdfSafeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('ç', 'c').replace('Ç', 'C')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ü', 'u').replace('Ü', 'U');
    }
}
