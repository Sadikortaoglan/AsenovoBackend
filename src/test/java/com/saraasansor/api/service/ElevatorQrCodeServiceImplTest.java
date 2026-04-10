package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.exception.QrCodeNotFoundException;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorQrCode;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.repository.ElevatorQrCodeRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.service.impl.ElevatorQrCodeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorQrCodeServiceImplTest {

    @Mock
    private ElevatorQrCodeRepository qrCodeRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private ElevatorQrService elevatorQrService;

    @InjectMocks
    private ElevatorQrCodeServiceImpl elevatorQrCodeService;

    @Test
    void createShouldSucceedWithValidElevatorId() {
        Elevator elevator = elevator(10L, "E-10", "Building A", 101L, "Facility A");
        when(elevatorRepository.findById(10L)).thenReturn(Optional.of(elevator));
        when(qrCodeRepository.existsByQrValue(anyString())).thenReturn(false);
        when(qrCodeRepository.save(any(ElevatorQrCode.class))).thenAnswer(invocation -> {
            ElevatorQrCode entity = invocation.getArgument(0);
            entity.setId(501L);
            entity.setCreatedAt(LocalDateTime.of(2026, 3, 24, 10, 0));
            return entity;
        });
        when(elevatorQrService.generateQrUrl(10L)).thenReturn("https://qr/e/10");

        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(10L);

        QrCodeResponseDTO response = elevatorQrCodeService.create(request, 5L);

        assertThat(response.getId()).isEqualTo(501L);
        assertThat(response.getElevatorId()).isEqualTo(10L);
        assertThat(response.getFacilityId()).isEqualTo(101L);
        assertThat(response.getFacilityName()).isEqualTo("Facility A");
    }

    @Test
    void updateShouldSucceedWithValidElevatorSelection() {
        Elevator oldElevator = elevator(10L, "E-10", "Building A", 101L, "Facility A");
        Elevator newElevator = elevator(11L, "E-11", "Building B", 102L, "Facility B");

        ElevatorQrCode existing = new ElevatorQrCode();
        existing.setId(901L);
        existing.setCompanyId(7L);
        existing.setUuid(UUID.randomUUID());
        existing.setQrValue("QR-901");
        existing.setElevator(oldElevator);
        existing.setCreatedAt(LocalDateTime.of(2026, 3, 24, 11, 0));

        when(qrCodeRepository.findByIdAndCompanyId(901L, 7L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(newElevator));
        when(qrCodeRepository.save(any(ElevatorQrCode.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(elevatorQrService.generateQrUrl(11L)).thenReturn("https://qr/e/11");

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(11L);

        QrCodeResponseDTO response = elevatorQrCodeService.update(901L, request, 7L);

        assertThat(response.getId()).isEqualTo(901L);
        assertThat(response.getElevatorId()).isEqualTo(11L);
        assertThat(response.getFacilityId()).isEqualTo(102L);
        assertThat(response.getFacilityName()).isEqualTo("Facility B");
    }

    @Test
    void createShouldFailWhenElevatorDoesNotExist() {
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());

        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(999L);

        assertThatThrownBy(() -> elevatorQrCodeService.create(request, 5L))
                .isInstanceOf(QrCodeNotFoundException.class)
                .hasMessageContaining("Elevator not found");
    }

    @Test
    void updateShouldFailWhenQrCodeNotInCompanyScope() {
        when(qrCodeRepository.findByIdAndCompanyId(1001L, 5L)).thenReturn(Optional.empty());

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(10L);

        assertThatThrownBy(() -> elevatorQrCodeService.update(1001L, request, 5L))
                .isInstanceOf(QrCodeNotFoundException.class)
                .hasMessageContaining("QR code not found");
    }

    @Test
    void listShouldIncludeElevatorAndFacilityInformation() {
        ElevatorQrCodeRepository.ElevatorQrListProjection projection = new ElevatorQrCodeRepository.ElevatorQrListProjection() {
            @Override
            public Long getQrId() {
                return 700L;
            }

            @Override
            public UUID getUuid() {
                return UUID.fromString("11111111-1111-1111-1111-111111111111");
            }

            @Override
            public Long getElevatorId() {
                return 10L;
            }

            @Override
            public String getElevatorName() {
                return "E-10";
            }

            @Override
            public String getBuildingName() {
                return "Building A";
            }

            @Override
            public Long getFacilityId() {
                return 101L;
            }

            @Override
            public String getFacilityName() {
                return "Facility A";
            }

            @Override
            public Long getB2bUnitId() {
                return 1001L;
            }

            @Override
            public String getB2bUnitName() {
                return "Cari A";
            }

            @Override
            public String getCustomerName() {
                return "Customer A";
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return LocalDateTime.of(2026, 3, 24, 12, 0);
            }

            @Override
            public LocalDateTime getUpdatedAt() {
                return LocalDateTime.of(2026, 3, 24, 12, 30);
            }

            @Override
            public String getQrValue() {
                return "QR-700";
            }
        };

        when(qrCodeRepository.findAllBySearchAndCompanyId(eq("E-10"), eq(5L), eq(false), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));
        when(elevatorQrService.generateQrUrl(10L)).thenReturn("https://qr/e/10");

        Page<QrCodeResponseDTO> page = elevatorQrCodeService.list(PageRequest.of(0, 20), "E-10", 5L);

        assertThat(page.getTotalElements()).isEqualTo(1L);
        QrCodeResponseDTO item = page.getContent().get(0);
        assertThat(item.getElevatorId()).isEqualTo(10L);
        assertThat(item.getElevatorName()).isEqualTo("E-10");
        assertThat(item.getFacilityId()).isEqualTo(101L);
        assertThat(item.getFacilityName()).isEqualTo("Facility A");
        assertThat(item.getB2bUnitId()).isEqualTo(1001L);
        assertThat(item.getB2bUnitName()).isEqualTo("Cari A");
        assertThat(item.getQrImageUrl()).isEqualTo("/api/elevators/10/qr");
        assertThat(item.getQrPrintUrl()).isEqualTo("/api/elevator-qrcodes/700/print");
    }

    @Test
    void getByIdShouldReturnCurrentElevatorSelection() {
        Elevator elevator = elevator(21L, "E-21", "Building Z", 301L, "Facility Z");
        ElevatorQrCode qrCode = new ElevatorQrCode();
        qrCode.setId(321L);
        qrCode.setCompanyId(9L);
        qrCode.setUuid(UUID.randomUUID());
        qrCode.setQrValue("QR-321");
        qrCode.setElevator(elevator);
        qrCode.setCreatedAt(LocalDateTime.of(2026, 3, 24, 13, 0));

        when(qrCodeRepository.findByIdAndCompanyId(321L, 9L)).thenReturn(Optional.of(qrCode));
        when(elevatorQrService.generateQrUrl(21L)).thenReturn("https://qr/e/21");

        QrCodeResponseDTO response = elevatorQrCodeService.getById(321L, 9L);

        assertThat(response.getId()).isEqualTo(321L);
        assertThat(response.getElevatorId()).isEqualTo(21L);
        assertThat(response.getElevatorName()).isEqualTo("E-21");
        assertThat(response.getFacilityName()).isEqualTo("Facility Z");
    }

    private Elevator elevator(Long id, String elevatorNumber, String buildingName, Long facilityId, String facilityName) {
        Elevator elevator = new Elevator();
        elevator.setId(id);
        elevator.setElevatorNumber(elevatorNumber);
        elevator.setBuildingName(buildingName);
        elevator.setManagerName("Manager");

        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName(facilityName);
        elevator.setFacility(facility);
        return elevator;
    }
}
