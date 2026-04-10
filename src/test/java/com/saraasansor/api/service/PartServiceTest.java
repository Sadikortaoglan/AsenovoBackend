package com.saraasansor.api.service;

import com.saraasansor.api.dto.PartCreateRequest;
import com.saraasansor.api.dto.PartPageResponse;
import com.saraasansor.api.dto.PartResponse;
import com.saraasansor.api.dto.PartUpdateRequest;
import com.saraasansor.api.model.Brand;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.model.StockGroup;
import com.saraasansor.api.model.StockModel;
import com.saraasansor.api.model.StockUnit;
import com.saraasansor.api.model.VatRate;
import com.saraasansor.api.repository.BrandRepository;
import com.saraasansor.api.repository.PartRepository;
import com.saraasansor.api.repository.StockGroupRepository;
import com.saraasansor.api.repository.StockModelRepository;
import com.saraasansor.api.repository.StockUnitRepository;
import com.saraasansor.api.repository.VatRateRepository;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private StockGroupRepository stockGroupRepository;

    @Mock
    private StockUnitRepository stockUnitRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private StockModelRepository stockModelRepository;

    @Mock
    private VatRateRepository vatRateRepository;

    private PartService service;

    @BeforeEach
    void setUp() {
        service = new PartService(partRepository, stockGroupRepository, stockUnitRepository, brandRepository, stockModelRepository, vatRateRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreatePart() {
        PartCreateRequest request = new PartCreateRequest();
        request.setName(" Motor ");
        request.setCode(" MTR-001 ");
        request.setBarcode(" 869000000001 ");
        request.setVatRateId(9L);
        request.setStockGroupId(3L);
        request.setUnitId(4L);
        request.setBrandId(5L);
        request.setModelId(6L);
        request.setPurchasePrice(100.0);
        request.setSalePrice(150.0);
        request.setStockEntry(14);
        request.setStockExit(4);
        request.setActive(true);

        Brand brand = brand(5L, "Otis");
        StockModel model = model(6L, "Gen2", brand);
        when(partRepository.existsByCodeIgnoreCaseAndActiveTrue("MTR-001")).thenReturn(false);
        when(partRepository.existsByBarcodeIgnoreCaseAndActiveTrue("869000000001")).thenReturn(false);
        when(vatRateRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(vatRate(9L, "20.00")));
        when(stockGroupRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(stockGroup(3L, "Mekanik")));
        when(stockUnitRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(stockUnit(4L, "Adet")));
        when(brandRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(brand));
        when(stockModelRepository.findByIdAndActiveTrue(6L)).thenReturn(Optional.of(model));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> {
            Part part = invocation.getArgument(0);
            part.setId(7L);
            part.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            part.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
            return part;
        });

        PartResponse response = service.createPart(request);

        ArgumentCaptor<Part> captor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Motor");
        assertThat(captor.getValue().getCode()).isEqualTo("MTR-001");
        assertThat(captor.getValue().getBarcode()).isEqualTo("869000000001");
        assertThat(captor.getValue().getStock()).isEqualTo(10);
        assertThat(captor.getValue().getStockEntry()).isEqualTo(14);
        assertThat(captor.getValue().getStockExit()).isEqualTo(4);
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getBrandId()).isEqualTo(5L);
        assertThat(response.getModelId()).isEqualTo(6L);
        assertThat(response.getVatRateId()).isEqualTo(9L);
        assertThat(response.getVatRate()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldUpdatePart() {
        PartUpdateRequest request = new PartUpdateRequest();
        request.setName(" Motor X ");
        request.setCode("MTR-001");
        request.setBarcode("869000000001");
        request.setVatRateId(10L);
        request.setStockGroupId(3L);
        request.setUnitId(4L);
        request.setPurchasePrice(110.0);
        request.setSalePrice(170.0);
        request.setStockEntry(11);
        request.setStockExit(3);
        request.setActive(true);

        Part existing = part(2L, "Old Motor", true);
        when(partRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(partRepository.existsByCodeIgnoreCaseAndIdNotAndActiveTrue("MTR-001", 2L)).thenReturn(false);
        when(partRepository.existsByBarcodeIgnoreCaseAndIdNotAndActiveTrue("869000000001", 2L)).thenReturn(false);
        when(vatRateRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(vatRate(10L, "10.00")));
        when(stockGroupRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(stockGroup(3L, "Mekanik")));
        when(stockUnitRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(stockUnit(4L, "Adet")));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartResponse response = service.updatePart(2L, request);

        assertThat(response.getName()).isEqualTo("Motor X");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getSalePrice()).isEqualTo(170.0);
        assertThat(response.getStock()).isEqualTo(8);
        assertThat(response.getStockEntry()).isEqualTo(11);
        assertThat(response.getStockExit()).isEqualTo(3);
        assertThat(response.getVatRateId()).isEqualTo(10L);
        assertThat(response.getVatRate()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldListParts() {
        when(partRepository.search(eq("motor"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        part(1L, "Motor", true),
                        part(2L, "Fren", true)
                )));

        PartPageResponse response = service.getParts("motor", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Motor");
        assertThat(response.getContent().get(0).getPurchasePrice()).isEqualTo(90.0);
    }

    @Test
    void shouldSoftDeletePart() {
        Part existing = part(8L, "Motor", true);
        when(partRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deletePart(8L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectDuplicateCode() {
        PartCreateRequest request = baseCreateRequest();
        request.setCode("MTR-001");

        when(partRepository.existsByCodeIgnoreCaseAndActiveTrue("MTR-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createPart(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("code already exists");
    }

    @Test
    void shouldRejectModelBrandMismatch() {
        PartCreateRequest request = baseCreateRequest();
        request.setBrandId(5L);
        request.setModelId(6L);

        Brand brand = brand(5L, "Otis");
        Brand otherBrand = brand(9L, "Kone");
        StockModel model = model(6L, "MonoSpace", otherBrand);
        when(partRepository.existsByCodeIgnoreCaseAndActiveTrue("MTR-001")).thenReturn(false);
        when(partRepository.existsByBarcodeIgnoreCaseAndActiveTrue("869000000001")).thenReturn(false);
        when(vatRateRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(vatRate(9L, "20.00")));
        when(stockGroupRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(stockGroup(3L, "Mekanik")));
        when(stockUnitRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(stockUnit(4L, "Adet")));
        when(brandRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(brand));
        when(stockModelRepository.findByIdAndActiveTrue(6L)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.createPart(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");
    }

    private PartCreateRequest baseCreateRequest() {
        PartCreateRequest request = new PartCreateRequest();
        request.setName("Motor");
        request.setCode("MTR-001");
        request.setBarcode("869000000001");
        request.setVatRateId(9L);
        request.setStockGroupId(3L);
        request.setUnitId(4L);
        request.setSalePrice(150.0);
        request.setStock(5);
        return request;
    }

    private Part part(Long id, String name, boolean active) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setCode("MTR-" + id);
        part.setBarcode("86900000000" + id);
        part.setPurchasePrice(90.0);
        part.setUnitPrice(100.0);
        part.setStock(4);
        part.setStockEntry(9);
        part.setStockExit(5);
        part.setActive(active);
        part.setCreatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        part.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 9, 0));
        return part;
    }

    private StockGroup stockGroup(Long id, String name) {
        StockGroup stockGroup = new StockGroup();
        stockGroup.setId(id);
        stockGroup.setName(name);
        stockGroup.setActive(true);
        return stockGroup;
    }

    private StockUnit stockUnit(Long id, String name) {
        StockUnit stockUnit = new StockUnit();
        stockUnit.setId(id);
        stockUnit.setName(name);
        stockUnit.setAbbreviation("AD");
        stockUnit.setActive(true);
        return stockUnit;
    }

    private Brand brand(Long id, String name) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setActive(true);
        return brand;
    }

    private StockModel model(Long id, String name, Brand brand) {
        StockModel model = new StockModel();
        model.setId(id);
        model.setName(name);
        model.setBrand(brand);
        model.setActive(true);
        return model;
    }

    private VatRate vatRate(Long id, String rate) {
        VatRate vatRate = new VatRate();
        vatRate.setId(id);
        vatRate.setRate(new BigDecimal(rate));
        vatRate.setActive(true);
        return vatRate;
    }
}
