package com.saraasansor.api.service;

import com.saraasansor.api.dto.WarehouseCreateRequest;
import com.saraasansor.api.dto.WarehousePageResponse;
import com.saraasansor.api.dto.WarehouseResponse;
import com.saraasansor.api.dto.WarehouseUpdateRequest;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    private WarehouseService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseService(warehouseRepository);
    }

    @Test
    void shouldCreateWarehouse() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setName(" Ana Depo ");
        request.setActive(true);

        when(warehouseRepository.existsByNameIgnoreCaseAndActiveTrue("Ana Depo")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse warehouse = invocation.getArgument(0);
            warehouse.setId(5L);
            warehouse.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            warehouse.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            return warehouse;
        });

        WarehouseResponse response = service.createWarehouse(request);

        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana Depo");
        assertThat(response.getName()).isEqualTo("Ana Depo");
    }

    @Test
    void shouldAllowInactiveCreateWithoutDuplicateCheck() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setName("Ana Depo");
        request.setActive(false);

        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse warehouse = invocation.getArgument(0);
            warehouse.setId(6L);
            return warehouse;
        });

        WarehouseResponse response = service.createWarehouse(request);

        assertThat(response.getActive()).isFalse();
        verify(warehouseRepository, never()).existsByNameIgnoreCaseAndActiveTrue("Ana Depo");
    }

    @Test
    void shouldUpdateWarehouse() {
        WarehouseUpdateRequest request = new WarehouseUpdateRequest();
        request.setName(" Merkez Depo ");
        request.setActive(true);

        Warehouse existing = warehouse(3L, "Old", true);
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue("Merkez Depo", 3L)).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = service.updateWarehouse(3L, request);

        assertThat(response.getName()).isEqualTo("Merkez Depo");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void shouldAllowInactiveUpdateWithoutDuplicateCheck() {
        WarehouseUpdateRequest request = new WarehouseUpdateRequest();
        request.setName("Merkez Depo");
        request.setActive(false);

        Warehouse existing = warehouse(3L, "Old", true);
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = service.updateWarehouse(3L, request);

        assertThat(response.getActive()).isFalse();
        verify(warehouseRepository, never()).existsByNameIgnoreCaseAndIdNotAndActiveTrue("Merkez Depo", 3L);
    }

    @Test
    void shouldListWarehouses() {
        when(warehouseRepository.search(eq("depo"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        warehouse(1L, "Ana Depo", true),
                        warehouse(2L, "Yedek Depo", true)
                )));

        WarehousePageResponse response = service.getWarehouses("depo", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Ana Depo");
    }

    @Test
    void shouldSoftDeleteWarehouse() {
        Warehouse existing = warehouse(8L, "Ana Depo", true);
        when(warehouseRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteWarehouse(8L);

        assertThat(existing.getActive()).isFalse();
    }

    @Test
    void shouldRejectBlankName() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setName(" ");

        assertThatThrownBy(() -> service.createWarehouse(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void shouldRejectLongName() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setName("A".repeat(256));

        assertThatThrownBy(() -> service.createWarehouse(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at most 255");
    }

    @Test
    void shouldRejectDuplicateName() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setName("Ana Depo");

        when(warehouseRepository.existsByNameIgnoreCaseAndActiveTrue("Ana Depo")).thenReturn(true);

        assertThatThrownBy(() -> service.createWarehouse(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    private Warehouse warehouse(Long id, String name, boolean active) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setActive(active);
        warehouse.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        warehouse.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        return warehouse;
    }
}
