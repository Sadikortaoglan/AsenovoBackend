package com.saraasansor.api.service;

import com.saraasansor.api.dto.ModelCreateRequest;
import com.saraasansor.api.dto.ModelPageResponse;
import com.saraasansor.api.dto.ModelResponse;
import com.saraasansor.api.dto.ModelUpdateRequest;
import com.saraasansor.api.model.Brand;
import com.saraasansor.api.model.StockModel;
import com.saraasansor.api.repository.BrandRepository;
import com.saraasansor.api.repository.StockModelRepository;
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
class ModelServiceTest {

    @Mock
    private StockModelRepository stockModelRepository;

    @Mock
    private BrandRepository brandRepository;

    private ModelService service;

    @BeforeEach
    void setUp() {
        service = new ModelService(stockModelRepository, brandRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateModel() {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName(" Gen2 ");
        request.setBrandId(3L);
        request.setActive(true);

        Brand brand = brand(3L, "Otis");
        when(brandRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(brand));
        when(stockModelRepository.existsByBrandIdAndNameIgnoreCaseAndActiveTrue(3L, "Gen2")).thenReturn(false);
        when(stockModelRepository.save(any(StockModel.class))).thenAnswer(invocation -> {
            StockModel model = invocation.getArgument(0);
            model.setId(7L);
            model.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            model.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            return model;
        });

        ModelResponse response = service.createModel(request);

        ArgumentCaptor<StockModel> captor = ArgumentCaptor.forClass(StockModel.class);
        verify(stockModelRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Gen2");
        assertThat(captor.getValue().getBrand().getId()).isEqualTo(3L);
        assertThat(response.getBrandName()).isEqualTo("Otis");
    }

    @Test
    void shouldUpdateModel() {
        ModelUpdateRequest request = new ModelUpdateRequest();
        request.setName(" MRL ");
        request.setBrandId(5L);
        request.setActive(false);

        StockModel existing = model(2L, "Old", brand(4L, "Kone"), true);
        when(stockModelRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(brandRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(brand(5L, "Schindler")));
        when(stockModelRepository.existsByBrandIdAndNameIgnoreCaseAndIdNotAndActiveTrue(5L, "MRL", 2L)).thenReturn(false);
        when(stockModelRepository.save(any(StockModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelResponse response = service.updateModel(2L, request);

        assertThat(response.getName()).isEqualTo("MRL");
        assertThat(response.getBrandId()).isEqualTo(5L);
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void shouldListModels() {
        when(stockModelRepository.search(eq("gen"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        model(1L, "Gen2", brand(3L, "Otis"), true),
                        model(2L, "MonoSpace", brand(4L, "Kone"), true)
                )));

        ModelPageResponse response = service.getModels("gen", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getBrandName()).isEqualTo("Otis");
    }

    @Test
    void shouldSoftDeleteModel() {
        StockModel existing = model(9L, "NMax", brand(6L, "Mitsubishi"), true);
        when(stockModelRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(stockModelRepository.save(any(StockModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteModel(9L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectBlankName() {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName(" ");
        request.setBrandId(1L);

        assertThatThrownBy(() -> service.createModel(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void shouldRejectInvalidBrand() {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName("Gen2");
        request.setBrandId(99L);

        when(brandRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createModel(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Brand not found");
    }

    @Test
    void shouldRejectDuplicateModelForSameBrand() {
        ModelCreateRequest request = new ModelCreateRequest();
        request.setName("Gen2");
        request.setBrandId(3L);
        when(brandRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(brand(3L, "Otis")));
        when(stockModelRepository.existsByBrandIdAndNameIgnoreCaseAndActiveTrue(3L, "Gen2")).thenReturn(true);

        assertThatThrownBy(() -> service.createModel(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    private StockModel model(Long id, String name, Brand brand, boolean active) {
        StockModel model = new StockModel();
        model.setId(id);
        model.setName(name);
        model.setBrand(brand);
        model.setActive(active);
        model.setCreatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        model.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        return model;
    }

    private Brand brand(Long id, String name) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setActive(true);
        return brand;
    }
}
