package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockTransferCreateRequest;
import com.saraasansor.api.dto.StockTransferPageResponse;
import com.saraasansor.api.dto.StockTransferResponse;
import com.saraasansor.api.dto.StockTransferUpdateRequest;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.model.StockTransfer;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.PartRepository;
import com.saraasansor.api.repository.StockTransferRepository;
import com.saraasansor.api.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    private StockTransferService service;

    @BeforeEach
    void setUp() {
        service = new StockTransferService(stockTransferRepository, partRepository, warehouseRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateTransfer() {
        StockTransferCreateRequest request = new StockTransferCreateRequest();
        request.setDate(LocalDate.of(2026, 4, 8));
        request.setStockId(1L);
        request.setOutgoingWarehouseId(2L);
        request.setIncomingWarehouseId(3L);
        request.setQuantity(5);
        request.setDescription("  raf degisimi  ");
        request.setActive(true);

        when(partRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(stock(1L, "Motor", 20)));
        when(warehouseRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(warehouse(2L, "Ana Depo")));
        when(warehouseRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(warehouse(3L, "Yedek Depo")));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(invocation -> {
            StockTransfer transfer = invocation.getArgument(0);
            transfer.setId(10L);
            transfer.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
            transfer.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
            return transfer;
        });

        StockTransferResponse response = service.createStockTransfer(request);

        ArgumentCaptor<StockTransfer> captor = ArgumentCaptor.forClass(StockTransfer.class);
        verify(stockTransferRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("raf degisimi");
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("staff");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStockId()).isEqualTo(1L);
        assertThat(response.getOutgoingWarehouseId()).isEqualTo(2L);
        assertThat(response.getIncomingWarehouseId()).isEqualTo(3L);
    }

    @Test
    void shouldUpdateTransfer() {
        StockTransferUpdateRequest request = new StockTransferUpdateRequest();
        request.setDate(LocalDate.of(2026, 4, 9));
        request.setStockId(1L);
        request.setOutgoingWarehouseId(2L);
        request.setIncomingWarehouseId(3L);
        request.setQuantity(4);
        request.setDescription(" Yeni aciklama ");
        request.setActive(true);

        StockTransfer existing = transfer(5L, stock(1L, "Motor", 20), warehouse(2L, "Ana Depo"), warehouse(3L, "Yedek Depo"), 3);
        when(stockTransferRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(partRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(stock(1L, "Motor", 20)));
        when(warehouseRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(warehouse(2L, "Ana Depo")));
        when(warehouseRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(warehouse(3L, "Yedek Depo")));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransferResponse response = service.updateStockTransfer(5L, request);

        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 4, 9));
        assertThat(response.getQuantity()).isEqualTo(4);
        assertThat(response.getDescription()).isEqualTo("Yeni aciklama");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void shouldListTransfers() {
        when(stockTransferRepository.search(eq("motor"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        transfer(1L, stock(1L, "Motor", 20), warehouse(2L, "Ana Depo"), warehouse(3L, "Yedek Depo"), 4),
                        transfer(2L, stock(1L, "Motor", 20), warehouse(2L, "Ana Depo"), warehouse(3L, "Yedek Depo"), 2)
                )));

        StockTransferPageResponse response = service.getStockTransfers("motor", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getStockName()).isEqualTo("Motor");
    }

    @Test
    void shouldSoftDeleteTransfer() {
        StockTransfer existing = transfer(7L, stock(1L, "Motor", 20), warehouse(2L, "Ana Depo"), warehouse(3L, "Yedek Depo"), 3);
        when(stockTransferRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteStockTransfer(7L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectSameWarehouse() {
        StockTransferCreateRequest request = new StockTransferCreateRequest();
        request.setDate(LocalDate.of(2026, 4, 8));
        request.setStockId(1L);
        request.setOutgoingWarehouseId(2L);
        request.setIncomingWarehouseId(2L);
        request.setQuantity(1);

        when(partRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(stock(1L, "Motor", 20)));
        when(warehouseRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(warehouse(2L, "Ana Depo")));

        assertThatThrownBy(() -> service.createStockTransfer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    void shouldRejectQuantityGreaterThanCurrentStock() {
        StockTransferCreateRequest request = new StockTransferCreateRequest();
        request.setDate(LocalDate.of(2026, 4, 8));
        request.setStockId(1L);
        request.setOutgoingWarehouseId(2L);
        request.setIncomingWarehouseId(3L);
        request.setQuantity(25);

        when(partRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(stock(1L, "Motor", 20)));
        when(warehouseRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(warehouse(2L, "Ana Depo")));
        when(warehouseRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(warehouse(3L, "Yedek Depo")));

        assertThatThrownBy(() -> service.createStockTransfer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("exceeds available stock");
    }

    private Part stock(Long id, String name, int stock) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setStock(stock);
        part.setActive(true);
        return part;
    }

    private Warehouse warehouse(Long id, String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setActive(true);
        return warehouse;
    }

    private StockTransfer transfer(Long id, Part stock, Warehouse outgoing, Warehouse incoming, int quantity) {
        StockTransfer transfer = new StockTransfer();
        transfer.setId(id);
        transfer.setDate(LocalDate.of(2026, 4, 8));
        transfer.setStock(stock);
        transfer.setOutgoingWarehouse(outgoing);
        transfer.setIncomingWarehouse(incoming);
        transfer.setQuantity(quantity);
        transfer.setDescription("Transfer");
        transfer.setActive(true);
        transfer.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        transfer.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        return transfer;
    }
}
