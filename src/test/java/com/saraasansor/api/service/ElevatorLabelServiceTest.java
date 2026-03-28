package com.saraasansor.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelListItemResponse;
import com.saraasansor.api.dto.ElevatorLabelResponse;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorLabel;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.repository.ElevatorLabelRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.service.impl.ElevatorLabelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorLabelServiceTest {

    @Mock
    private ElevatorLabelRepository elevatorLabelRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private FileStorageService fileStorageService;

    private ElevatorLabelService elevatorLabelService;

    @BeforeEach
    void setUp() {
        elevatorLabelService = new ElevatorLabelServiceImpl(
                elevatorLabelRepository,
                elevatorRepository,
                fileStorageService,
                new ObjectMapper()
        );
    }

    @Test
    void createShouldPersistElevatorLabelEntity() {
        Elevator elevator = elevator(11L);
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(elevator));
        when(elevatorLabelRepository.save(any(ElevatorLabel.class))).thenAnswer(invocation -> {
            ElevatorLabel saved = invocation.getArgument(0);
            saved.setId(101L);
            saved.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));
            return saved;
        });

        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(11L);
        request.setLabelName("Aylık Etiket");
        request.setLabelStartDate(LocalDate.of(2026, 3, 1));
        request.setLabelEndDate(LocalDate.of(2026, 4, 1));
        request.setLabelIssueDate(LocalDate.of(2026, 3, 5));
        request.setLabelType("GREEN");
        request.setSerialNumber("SN-101");
        request.setDescription("Deneme");
        request.setStatus("ACTIVE");
        request.setAdditionalFields(Map.of("customField", "customValue"));

        ElevatorLabelResponse response = elevatorLabelService.create(request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getElevatorId()).isEqualTo(11L);
        assertThat(response.getLabelName()).isEqualTo("Aylık Etiket");
        assertThat(response.getLabelType()).isEqualTo("GREEN");
        assertThat(response.getAdditionalFields()).containsEntry("customField", "customValue");
    }

    @Test
    void updateShouldPersistChangedDateFields() {
        Elevator elevator = elevator(11L);
        ElevatorLabel existing = new ElevatorLabel();
        existing.setId(88L);
        existing.setElevator(elevator);
        existing.setLabelStartDate(LocalDate.of(2026, 1, 1));
        existing.setLabelEndDate(LocalDate.of(2026, 2, 1));
        existing.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        existing.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(elevatorLabelRepository.findById(88L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(elevator));
        when(elevatorLabelRepository.save(any(ElevatorLabel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(11L);
        request.setLabelStartDate(LocalDate.of(2026, 5, 1));
        request.setLabelEndDate(LocalDate.of(2026, 6, 1));
        request.setLabelIssueDate(LocalDate.of(2026, 5, 2));
        request.setLabelName("Güncel Etiket");
        request.setStatus("ACTIVE");

        ElevatorLabelResponse response = elevatorLabelService.update(88L, request);

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getLabelStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.getLabelEndDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.getLabelIssueDate()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(response.getLabelName()).isEqualTo("Güncel Etiket");
    }

    @Test
    void detailShouldReturnSavedValues() {
        ElevatorLabel existing = sampleLabel(55L, 11L);
        existing.setLabelName("Detay Etiketi");
        existing.setSerialNumber("SER-55");
        existing.setLabelType("RED");

        when(elevatorLabelRepository.findById(55L)).thenReturn(Optional.of(existing));

        ElevatorLabelResponse response = elevatorLabelService.getById(55L);

        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getElevatorId()).isEqualTo(11L);
        assertThat(response.getLabelName()).isEqualTo("Detay Etiketi");
        assertThat(response.getSerialNumber()).isEqualTo("SER-55");
        assertThat(response.getLabelType()).isEqualTo("RED");
    }

    @Test
    void listShouldIncludeSavedLabelData() {
        ElevatorLabel item = sampleLabel(1L, 11L);
        item.setLabelName("Liste Etiketi");
        item.setStatus("ACTIVE");
        item.setSerialNumber("LST-1");

        when(elevatorLabelRepository.search(eq("liste"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<ElevatorLabelListItemResponse> result = elevatorLabelService.list(PageRequest.of(0, 20), "liste");

        assertThat(result.getTotalElements()).isEqualTo(1L);
        ElevatorLabelListItemResponse first = result.getContent().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getLabelName()).isEqualTo("Liste Etiketi");
        assertThat(first.getSerialNumber()).isEqualTo("LST-1");
        assertThat(first.getFacilityName()).isEqualTo("Facility A");
    }

    @Test
    void createShouldFailForInvalidElevatorId() {
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());
        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(999L);

        assertThatThrownBy(() -> elevatorLabelService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Elevator not found");
    }

    @Test
    void updateShouldFailForInvalidElevatorId() {
        ElevatorLabel existing = sampleLabel(9L, 11L);
        when(elevatorLabelRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(999L);

        assertThatThrownBy(() -> elevatorLabelService.update(9L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Elevator not found");
    }

    @Test
    void createShouldPersistAttachmentMetadataWhenFileUploaded() throws Exception {
        Elevator elevator = elevator(11L);
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(elevator));
        when(elevatorLabelRepository.save(any(ElevatorLabel.class))).thenAnswer(invocation -> {
            ElevatorLabel saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(303L);
            }
            return saved;
        });
        when(fileStorageService.saveFile(any(), eq("ELEVATOR_LABEL"), eq(303L)))
                .thenReturn("elevator_label/303/test.pdf");
        when(fileStorageService.getFileUrl("elevator_label/303/test.pdf"))
                .thenReturn("/api/files/elevator_label/303/test.pdf");

        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(11L);
        request.setLabelName("File Label");
        request.setFile(new MockMultipartFile("file", "test.pdf", "application/pdf", "test-content".getBytes()));

        ElevatorLabelResponse response = elevatorLabelService.create(request);

        assertThat(response.getId()).isEqualTo(303L);
        assertThat(response.getAttachmentName()).isEqualTo("test.pdf");
        assertThat(response.getAttachmentContentType()).isEqualTo("application/pdf");
        assertThat(response.getAttachmentUrl()).isEqualTo("/api/files/elevator_label/303/test.pdf");
        assertThat(response.isAttachmentExists()).isTrue();
    }

    @Test
    void updateShouldKeepExistingAttachmentWhenNoNewFileProvided() {
        Elevator elevator = elevator(11L);
        ElevatorLabel existing = sampleLabel(77L, 11L);
        existing.setAttachmentName("existing.pdf");
        existing.setAttachmentUrl("/api/files/elevator_label/77/existing.pdf");
        existing.setAttachmentContentType("application/pdf");
        existing.setAttachmentSize(123L);

        when(elevatorLabelRepository.findById(77L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(elevator));
        when(elevatorLabelRepository.save(any(ElevatorLabel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(11L);
        request.setLabelName("Updated but no new file");

        ElevatorLabelResponse response = elevatorLabelService.update(77L, request);

        assertThat(response.getAttachmentName()).isEqualTo("existing.pdf");
        assertThat(response.getAttachmentUrl()).isEqualTo("/api/files/elevator_label/77/existing.pdf");
        assertThat(response.isAttachmentExists()).isTrue();
    }

    @Test
    void updateShouldReplaceAttachmentWhenNewFileProvided() throws Exception {
        Elevator elevator = elevator(11L);
        ElevatorLabel existing = sampleLabel(88L, 11L);
        existing.setAttachmentName("old.pdf");
        existing.setAttachmentUrl("/api/files/elevator_label/88/old.pdf");
        existing.setAttachmentContentType("application/pdf");
        existing.setAttachmentSize(10L);

        when(elevatorLabelRepository.findById(88L)).thenReturn(Optional.of(existing));
        when(elevatorRepository.findById(11L)).thenReturn(Optional.of(elevator));
        when(elevatorLabelRepository.save(any(ElevatorLabel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorageService.saveFile(any(), eq("ELEVATOR_LABEL"), eq(88L)))
                .thenReturn("elevator_label/88/new.pdf");
        when(fileStorageService.getFileUrl("elevator_label/88/new.pdf"))
                .thenReturn("/api/files/elevator_label/88/new.pdf");

        ElevatorLabelUpdateRequest request = new ElevatorLabelUpdateRequest();
        request.setElevatorId(11L);
        request.setFile(new MockMultipartFile("file", "new.pdf", "application/pdf", "new-content".getBytes()));

        ElevatorLabelResponse response = elevatorLabelService.update(88L, request);

        assertThat(response.getAttachmentName()).isEqualTo("new.pdf");
        assertThat(response.getAttachmentUrl()).isEqualTo("/api/files/elevator_label/88/new.pdf");
        assertThat(response.isAttachmentExists()).isTrue();
    }

    private Elevator elevator(Long id) {
        Elevator elevator = new Elevator();
        elevator.setId(id);
        elevator.setElevatorNumber("E-" + id);
        elevator.setIdentityNumber("ID-" + id);
        elevator.setBuildingName("Building A");
        elevator.setManagerName("Manager A");
        Facility facility = new Facility();
        facility.setId(201L);
        facility.setName("Facility A");
        elevator.setFacility(facility);
        return elevator;
    }

    private ElevatorLabel sampleLabel(Long labelId, Long elevatorId) {
        ElevatorLabel label = new ElevatorLabel();
        label.setId(labelId);
        label.setElevator(elevator(elevatorId));
        label.setCreatedAt(LocalDateTime.of(2026, 3, 26, 12, 0));
        label.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 12, 0));
        return label;
    }
}
