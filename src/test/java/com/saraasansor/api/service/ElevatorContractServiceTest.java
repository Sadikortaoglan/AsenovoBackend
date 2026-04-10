package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorContractCreateRequest;
import com.saraasansor.api.dto.ElevatorContractResponse;
import com.saraasansor.api.dto.ElevatorContractUpdateRequest;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorContract;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorContractRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevatorContractServiceTest {

    @Mock
    private ElevatorContractRepository elevatorContractRepository;

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ElevatorContractService elevatorContractService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldPersistContractWithElevatorSelection() {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);
        Elevator elevator = elevator(10L, 5L, "Facility A");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorRepository.findById(10L)).thenReturn(Optional.of(elevator));
        when(elevatorContractRepository.save(any(ElevatorContract.class))).thenAnswer(invocation -> {
            ElevatorContract entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
                entity.setCreatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
                entity.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
            }
            return entity;
        });

        ElevatorContractCreateRequest request = new ElevatorContractCreateRequest();
        request.setElevatorId(10L);
        request.setContractDate("2026-03-30T11:15");
        request.setContractHtml("<p>Contract text</p>");
        request.setStatus("active");

        ElevatorContractResponse response = elevatorContractService.create(request, null);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getElevatorId()).isEqualTo(10L);
        assertThat(response.getFacilityName()).isEqualTo("Facility A");
        assertThat(response.getContractDate()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void updateWithoutNewFileShouldKeepExistingAttachment() {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);
        ElevatorContract existing = contract(1L, elevator(10L, 5L, "Facility A"));
        existing.setAttachmentStorageKey("elevator_contract/1/original.pdf");
        existing.setAttachmentOriginalFileName("original.pdf");
        existing.setAttachmentContentType("application/pdf");
        existing.setAttachmentSize(120L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorContractRepository.findDetailById(1L)).thenReturn(Optional.of(existing));
        when(elevatorContractRepository.save(any(ElevatorContract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorageService.getFileUrl("elevator_contract/1/original.pdf"))
                .thenReturn("/api/files/elevator_contract/1/original.pdf");

        ElevatorContractUpdateRequest request = new ElevatorContractUpdateRequest();
        request.setContractHtml("<p>Updated</p>");

        ElevatorContractResponse response = elevatorContractService.update(1L, request, null);

        assertThat(response.isAttachmentExists()).isTrue();
        assertThat(response.getAttachmentOriginalFileName()).isEqualTo("original.pdf");
        assertThat(response.getAttachmentUrl()).isEqualTo("/api/files/elevator_contract/1/original.pdf");
    }

    @Test
    void updateWithNewFileShouldReplaceAttachmentMetadata() throws Exception {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);
        ElevatorContract existing = contract(1L, elevator(10L, 5L, "Facility A"));
        existing.setAttachmentStorageKey("elevator_contract/1/original.pdf");
        existing.setAttachmentOriginalFileName("original.pdf");
        existing.setAttachmentContentType("application/pdf");
        existing.setAttachmentSize(120L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new-contract.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorContractRepository.findDetailById(1L)).thenReturn(Optional.of(existing));
        when(elevatorContractRepository.save(any(ElevatorContract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorageService.saveFile(eq(file), eq("ELEVATOR_CONTRACT"), eq(1L)))
                .thenReturn("elevator_contract/1/new-contract.pdf");
        when(fileStorageService.getFileUrl("elevator_contract/1/new-contract.pdf"))
                .thenReturn("/api/files/elevator_contract/1/new-contract.pdf");

        ElevatorContractUpdateRequest request = new ElevatorContractUpdateRequest();
        request.setContractDate("2026-03-31");

        ElevatorContractResponse response = elevatorContractService.update(1L, request, file);

        assertThat(response.isAttachmentExists()).isTrue();
        assertThat(response.getAttachmentOriginalFileName()).isEqualTo("new-contract.pdf");
        assertThat(response.getAttachmentUrl()).isEqualTo("/api/files/elevator_contract/1/new-contract.pdf");
        verify(fileStorageService).saveFile(eq(file), eq("ELEVATOR_CONTRACT"), eq(1L));
    }

    @Test
    void getByIdShouldReturnSavedElevatorAndFacilityInfo() {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);
        ElevatorContract existing = contract(3L, elevator(42L, 7L, "Facility 42"));
        existing.setAttachmentStorageKey("elevator_contract/3/file.pdf");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorContractRepository.findDetailById(3L)).thenReturn(Optional.of(existing));
        when(fileStorageService.getFileUrl("elevator_contract/3/file.pdf"))
                .thenReturn("/api/files/elevator_contract/3/file.pdf");

        ElevatorContractResponse response = elevatorContractService.getById(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getElevatorId()).isEqualTo(42L);
        assertThat(response.getFacilityName()).isEqualTo("Facility 42");
        assertThat(response.isAttachmentExists()).isTrue();
    }

    @Test
    void createShouldFailWhenElevatorDoesNotExist() {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());

        ElevatorContractCreateRequest request = new ElevatorContractCreateRequest();
        request.setElevatorId(999L);

        assertThatThrownBy(() -> elevatorContractService.create(request, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Elevator not found");
    }

    @Test
    void createShouldFailForForeignElevatorOfCariUser() {
        authenticateAs("cari");
        User cariUser = user("cari", User.Role.CARI_USER, 10L);
        Elevator foreignElevator = elevator(50L, 99L, "Foreign Facility");

        when(userRepository.findByUsername("cari")).thenReturn(Optional.of(cariUser));
        when(elevatorRepository.findById(50L)).thenReturn(Optional.of(foreignElevator));

        ElevatorContractCreateRequest request = new ElevatorContractCreateRequest();
        request.setElevatorId(50L);

        assertThatThrownBy(() -> elevatorContractService.create(request, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("selected elevator");
    }

    @Test
    void createShouldFailWhenContractDateFormatIsInvalid() {
        authenticateAs("admin");
        User admin = user("admin", User.Role.SYSTEM_ADMIN, null);
        Elevator elevator = elevator(10L, 5L, "Facility A");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(elevatorRepository.findById(10L)).thenReturn(Optional.of(elevator));

        ElevatorContractCreateRequest request = new ElevatorContractCreateRequest();
        request.setElevatorId(10L);
        request.setContractDate("31/31/2026");

        assertThatThrownBy(() -> elevatorContractService.create(request, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid contractDate format");
    }

    @Test
    void listShouldUseCariScopeAndReturnOwnTenantData() {
        authenticateAs("cari");
        User cariUser = user("cari", User.Role.CARI_USER, 7L);
        ElevatorContract ownContract = contract(100L, elevator(200L, 7L, "Own Facility"));
        ownContract.setStatus("ACTIVE");

        when(userRepository.findByUsername("cari")).thenReturn(Optional.of(cariUser));
        when(elevatorContractRepository.searchByB2bUnit(eq(7L), eq("own"), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(ownContract), PageRequest.of(0, 20), 1L));

        Page<?> response = elevatorContractService.list("own", null, PageRequest.of(0, 20));

        assertThat(response.getTotalElements()).isEqualTo(1L);
        verify(elevatorContractRepository).searchByB2bUnit(eq(7L), eq("own"), eq(null), any(PageRequest.class));
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(String username, User.Role role, Long b2bUnitId) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        if (b2bUnitId != null) {
            B2BUnit b2bUnit = new B2BUnit();
            b2bUnit.setId(b2bUnitId);
            user.setB2bUnit(b2bUnit);
        }
        return user;
    }

    private Elevator elevator(Long elevatorId, Long b2bUnitId, String facilityName) {
        Elevator elevator = new Elevator();
        elevator.setId(elevatorId);
        elevator.setElevatorNumber("E-" + elevatorId);
        elevator.setIdentityNumber("ID-" + elevatorId);

        Facility facility = new Facility();
        facility.setId(500L + elevatorId);
        facility.setName(facilityName);
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(b2bUnitId);
        facility.setB2bUnit(b2bUnit);
        elevator.setFacility(facility);
        return elevator;
    }

    private ElevatorContract contract(Long id, Elevator elevator) {
        ElevatorContract contract = new ElevatorContract();
        contract.setId(id);
        contract.setElevator(elevator);
        contract.setContractDate(LocalDate.of(2026, 3, 30));
        contract.setContractHtml("<p>Contract</p>");
        contract.setStatus("ACTIVE");
        contract.setCreatedAt(LocalDateTime.of(2026, 3, 30, 12, 0));
        contract.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 12, 0));
        return contract;
    }
}
