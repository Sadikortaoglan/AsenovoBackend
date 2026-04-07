package com.saraasansor.api.service;

import com.saraasansor.api.dto.BrandCreateRequest;
import com.saraasansor.api.dto.BrandPageResponse;
import com.saraasansor.api.dto.BrandResponse;
import com.saraasansor.api.dto.BrandUpdateRequest;
import com.saraasansor.api.model.Brand;
import com.saraasansor.api.repository.BrandRepository;
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
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandService service;

    @BeforeEach
    void setUp() {
        service = new BrandService(brandRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateBrand() {
        BrandCreateRequest request = new BrandCreateRequest();
        request.setName(" Bosch ");
        request.setActive(true);

        when(brandRepository.existsByNameIgnoreCase("Bosch")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> {
            Brand brand = invocation.getArgument(0);
            brand.setId(5L);
            brand.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            brand.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            return brand;
        });

        BrandResponse response = service.createBrand(request);

        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Bosch");
        assertThat(response.getName()).isEqualTo("Bosch");
    }

    @Test
    void shouldUpdateBrand() {
        BrandUpdateRequest request = new BrandUpdateRequest();
        request.setName("Otis");
        request.setActive(false);

        Brand existing = brand(3L, "Old", true);
        when(brandRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Otis", 3L)).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponse response = service.updateBrand(3L, request);

        assertThat(response.getName()).isEqualTo("Otis");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void shouldListBrands() {
        when(brandRepository.search(eq("ot"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        brand(1L, "Otis", true),
                        brand(2L, "Schindler", true)
                )));

        BrandPageResponse response = service.getBrands("ot", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Otis");
    }

    @Test
    void shouldSoftDeleteBrand() {
        Brand existing = brand(8L, "Kone", true);
        when(brandRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteBrand(8L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectBlankName() {
        BrandCreateRequest request = new BrandCreateRequest();
        request.setName("   ");

        assertThatThrownBy(() -> service.createBrand(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void shouldRejectDuplicateBrand() {
        BrandCreateRequest request = new BrandCreateRequest();
        request.setName("Otis");

        when(brandRepository.existsByNameIgnoreCase("Otis")).thenReturn(true);

        assertThatThrownBy(() -> service.createBrand(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    private Brand brand(Long id, String name, boolean active) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setActive(active);
        brand.setCreatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        brand.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        return brand;
    }
}
