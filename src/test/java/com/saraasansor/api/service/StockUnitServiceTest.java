package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockUnitCreateRequest;
import com.saraasansor.api.dto.StockUnitPageResponse;
import com.saraasansor.api.dto.StockUnitResponse;
import com.saraasansor.api.dto.StockUnitUpdateRequest;
import com.saraasansor.api.model.StockUnit;
import com.saraasansor.api.repository.StockUnitRepository;
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
class StockUnitServiceTest {

    @Mock
    private StockUnitRepository stockUnitRepository;

    private StockUnitService service;

    @BeforeEach
    void setUp() {
        service = new StockUnitService(stockUnitRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateStockUnit() {
        StockUnitCreateRequest request = new StockUnitCreateRequest();
        request.setName(" Adet ");
        request.setAbbreviation(" Ad ");
        request.setActive(true);

        when(stockUnitRepository.existsByNameIgnoreCaseAndActiveTrue("Adet")).thenReturn(false);
        when(stockUnitRepository.existsByAbbreviationIgnoreCaseAndActiveTrue("Ad")).thenReturn(false);
        when(stockUnitRepository.save(any(StockUnit.class))).thenAnswer(invocation -> {
            StockUnit stockUnit = invocation.getArgument(0);
            stockUnit.setId(5L);
            stockUnit.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            stockUnit.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            return stockUnit;
        });

        StockUnitResponse response = service.createStockUnit(request);

        ArgumentCaptor<StockUnit> captor = ArgumentCaptor.forClass(StockUnit.class);
        verify(stockUnitRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Adet");
        assertThat(captor.getValue().getAbbreviation()).isEqualTo("Ad");
        assertThat(response.getName()).isEqualTo("Adet");
    }

    @Test
    void shouldUpdateStockUnit() {
        StockUnitUpdateRequest request = new StockUnitUpdateRequest();
        request.setName(" Metre ");
        request.setAbbreviation(" M ");
        request.setActive(false);

        StockUnit existing = stockUnit(3L, "Old", "OLD", true);
        when(stockUnitRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(stockUnitRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue("Metre", 3L)).thenReturn(false);
        when(stockUnitRepository.existsByAbbreviationIgnoreCaseAndIdNotAndActiveTrue("M", 3L)).thenReturn(false);
        when(stockUnitRepository.save(any(StockUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockUnitResponse response = service.updateStockUnit(3L, request);

        assertThat(response.getName()).isEqualTo("Metre");
        assertThat(response.getAbbreviation()).isEqualTo("M");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void shouldListStockUnits() {
        when(stockUnitRepository.search(eq("ad"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        stockUnit(1L, "Adet", "AD", true),
                        stockUnit(2L, "Metre", "M", true)
                )));

        StockUnitPageResponse response = service.getStockUnits("ad", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getAbbreviation()).isEqualTo("AD");
    }

    @Test
    void shouldSoftDeleteStockUnit() {
        StockUnit existing = stockUnit(8L, "Litre", "LT", true);
        when(stockUnitRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(stockUnitRepository.save(any(StockUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteStockUnit(8L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectBlankValues() {
        StockUnitCreateRequest request = new StockUnitCreateRequest();
        request.setName(" ");
        request.setAbbreviation(" ");

        assertThatThrownBy(() -> service.createStockUnit(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void shouldRejectDuplicateName() {
        StockUnitCreateRequest request = new StockUnitCreateRequest();
        request.setName("Adet");
        request.setAbbreviation("AD");

        when(stockUnitRepository.existsByNameIgnoreCaseAndActiveTrue("Adet")).thenReturn(true);

        assertThatThrownBy(() -> service.createStockUnit(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name already exists");
    }

    @Test
    void shouldRejectDuplicateAbbreviation() {
        StockUnitCreateRequest request = new StockUnitCreateRequest();
        request.setName("Adet");
        request.setAbbreviation("AD");

        when(stockUnitRepository.existsByNameIgnoreCaseAndActiveTrue("Adet")).thenReturn(false);
        when(stockUnitRepository.existsByAbbreviationIgnoreCaseAndActiveTrue("AD")).thenReturn(true);

        assertThatThrownBy(() -> service.createStockUnit(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("abbreviation already exists");
    }

    private StockUnit stockUnit(Long id, String name, String abbreviation, boolean active) {
        StockUnit stockUnit = new StockUnit();
        stockUnit.setId(id);
        stockUnit.setName(name);
        stockUnit.setAbbreviation(abbreviation);
        stockUnit.setActive(active);
        stockUnit.setCreatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        stockUnit.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        return stockUnit;
    }
}
