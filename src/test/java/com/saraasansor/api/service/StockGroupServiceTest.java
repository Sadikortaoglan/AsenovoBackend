package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockGroupCreateRequest;
import com.saraasansor.api.dto.StockGroupPageResponse;
import com.saraasansor.api.dto.StockGroupResponse;
import com.saraasansor.api.dto.StockGroupUpdateRequest;
import com.saraasansor.api.model.StockGroup;
import com.saraasansor.api.repository.StockGroupRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockGroupServiceTest {

    @Mock
    private StockGroupRepository stockGroupRepository;

    private StockGroupService service;

    @BeforeEach
    void setUp() {
        service = new StockGroupService(stockGroupRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateStockGroup() {
        StockGroupCreateRequest request = new StockGroupCreateRequest();
        request.setName(" Elektrik ");
        request.setActive(true);

        when(stockGroupRepository.existsByNameIgnoreCaseAndActiveTrue("Elektrik")).thenReturn(false);
        when(stockGroupRepository.save(any(StockGroup.class))).thenAnswer(invocation -> {
            StockGroup stockGroup = invocation.getArgument(0);
            stockGroup.setId(5L);
            stockGroup.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            stockGroup.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            return stockGroup;
        });

        StockGroupResponse response = service.createStockGroup(request);

        ArgumentCaptor<StockGroup> captor = ArgumentCaptor.forClass(StockGroup.class);
        verify(stockGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Elektrik");
        assertThat(response.getName()).isEqualTo("Elektrik");
        assertThat(response.getCreatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldAllowInactiveCreateWithoutDuplicateCheck() {
        StockGroupCreateRequest request = new StockGroupCreateRequest();
        request.setName("Elektrik");
        request.setActive(false);

        when(stockGroupRepository.save(any(StockGroup.class))).thenAnswer(invocation -> {
            StockGroup stockGroup = invocation.getArgument(0);
            stockGroup.setId(6L);
            return stockGroup;
        });

        StockGroupResponse response = service.createStockGroup(request);

        assertThat(response.getActive()).isFalse();
        verify(stockGroupRepository, never()).existsByNameIgnoreCaseAndActiveTrue("Elektrik");
    }

    @Test
    void shouldUpdateStockGroup() {
        StockGroupUpdateRequest request = new StockGroupUpdateRequest();
        request.setName(" Mekanik ");
        request.setActive(true);

        StockGroup existing = stockGroup(3L, "Old", true);
        when(stockGroupRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(stockGroupRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue("Mekanik", 3L)).thenReturn(false);
        when(stockGroupRepository.save(any(StockGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockGroupResponse response = service.updateStockGroup(3L, request);

        assertThat(response.getName()).isEqualTo("Mekanik");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void shouldAllowInactiveUpdateWithoutDuplicateCheck() {
        StockGroupUpdateRequest request = new StockGroupUpdateRequest();
        request.setName("Mekanik");
        request.setActive(false);

        StockGroup existing = stockGroup(3L, "Old", true);
        when(stockGroupRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(stockGroupRepository.save(any(StockGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockGroupResponse response = service.updateStockGroup(3L, request);

        assertThat(response.getActive()).isFalse();
        verify(stockGroupRepository, never()).existsByNameIgnoreCaseAndIdNotAndActiveTrue("Mekanik", 3L);
    }

    @Test
    void shouldListStockGroups() {
        when(stockGroupRepository.search(eq("ele"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        stockGroup(1L, "Elektrik", true),
                        stockGroup(2L, "Mekanik", true)
                )));

        StockGroupPageResponse response = service.getStockGroups("ele", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Elektrik");
    }

    @Test
    void shouldSoftDeleteStockGroup() {
        StockGroup existing = stockGroup(8L, "Elektrik", true);
        when(stockGroupRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(stockGroupRepository.save(any(StockGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteStockGroup(8L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectBlankName() {
        StockGroupCreateRequest request = new StockGroupCreateRequest();
        request.setName(" ");

        assertThatThrownBy(() -> service.createStockGroup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void shouldRejectLongName() {
        StockGroupCreateRequest request = new StockGroupCreateRequest();
        request.setName("A".repeat(256));

        assertThatThrownBy(() -> service.createStockGroup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at most 255");
    }

    @Test
    void shouldRejectDuplicateName() {
        StockGroupCreateRequest request = new StockGroupCreateRequest();
        request.setName("Elektrik");

        when(stockGroupRepository.existsByNameIgnoreCaseAndActiveTrue("Elektrik")).thenReturn(true);

        assertThatThrownBy(() -> service.createStockGroup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    private StockGroup stockGroup(Long id, String name, boolean active) {
        StockGroup stockGroup = new StockGroup();
        stockGroup.setId(id);
        stockGroup.setName(name);
        stockGroup.setActive(active);
        stockGroup.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        stockGroup.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        return stockGroup;
    }
}
