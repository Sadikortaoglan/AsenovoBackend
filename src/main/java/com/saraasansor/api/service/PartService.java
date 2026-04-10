package com.saraasansor.api.service;

import com.saraasansor.api.dto.LookupDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final StockGroupRepository stockGroupRepository;
    private final StockUnitRepository stockUnitRepository;
    private final BrandRepository brandRepository;
    private final StockModelRepository stockModelRepository;
    private final VatRateRepository vatRateRepository;

    public PartService(PartRepository partRepository,
                       StockGroupRepository stockGroupRepository,
                       StockUnitRepository stockUnitRepository,
                       BrandRepository brandRepository,
                       StockModelRepository stockModelRepository,
                       VatRateRepository vatRateRepository) {
        this.partRepository = partRepository;
        this.stockGroupRepository = stockGroupRepository;
        this.stockUnitRepository = stockUnitRepository;
        this.brandRepository = brandRepository;
        this.stockModelRepository = stockModelRepository;
        this.vatRateRepository = vatRateRepository;
    }

    @Transactional(readOnly = true)
    public PartPageResponse getParts(String query, Boolean active, Pageable pageable) {
        Page<Part> page = partRepository.search(normalizeNullable(query), active, pageable);
        PartPageResponse response = new PartPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.PartListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public PartResponse getPartById(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found"));
        return PartResponse.fromEntity(part);
    }

    public PartResponse createPart(PartCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeRequired(request.getName(), "name is required");
        String normalizedCode = normalizeOptional(request.getCode());
        String normalizedBarcode = normalizeOptional(request.getBarcode());
        String normalizedDescription = normalizeOptional(request.getDescription());
        String normalizedImagePath = normalizeOptional(request.getImagePath());
        boolean targetActive = request.getActive() == null || request.getActive();

        validateNumericValues(
                request.getPurchasePrice(),
                request.getSalePrice(),
                request.getStock(),
                request.getStockEntry(),
                request.getStockExit()
        );

        if (targetActive && StringUtils.hasText(normalizedCode)
                && partRepository.existsByCodeIgnoreCaseAndActiveTrue(normalizedCode)) {
            throw new RuntimeException("Part code already exists");
        }
        if (targetActive && StringUtils.hasText(normalizedBarcode)
                && partRepository.existsByBarcodeIgnoreCaseAndActiveTrue(normalizedBarcode)) {
            throw new RuntimeException("Part barcode already exists");
        }

        StockGroup stockGroup = stockGroupRepository.findByIdAndActiveTrue(request.getStockGroupId())
                .orElseThrow(() -> new RuntimeException("Stock group not found"));
        StockUnit unit = stockUnitRepository.findByIdAndActiveTrue(request.getUnitId())
                .orElseThrow(() -> new RuntimeException("Stock unit not found"));
        VatRate vatRate = resolveVatRate(request.getVatRateId());
        Brand brand = resolveBrand(request.getBrandId());
        StockModel model = resolveModel(request.getModelId());
        validateBrandModelRelation(brand, model);
        StockSnapshot stockSnapshot = resolveStockSnapshotForCreate(request);

        Part part = new Part();
        part.setName(normalizedName);
        part.setCode(normalizedCode);
        part.setBarcode(normalizedBarcode);
        part.setVatRate(vatRate);
        part.setStockGroup(stockGroup);
        part.setUnit(unit);
        part.setBrand(brand != null ? brand : (model != null ? model.getBrand() : null));
        part.setModel(model);
        part.setPurchasePrice(request.getPurchasePrice());
        part.setUnitPrice(request.getSalePrice());
        part.setDescription(normalizedDescription);
        part.setImagePath(normalizedImagePath);
        part.setStock(stockSnapshot.stock());
        part.setStockEntry(stockSnapshot.stockEntry());
        part.setStockExit(stockSnapshot.stockExit());
        part.setActive(targetActive);
        part.setCreatedBy(resolveCurrentUsername());
        part.setUpdatedBy(resolveCurrentUsername());

        return PartResponse.fromEntity(partRepository.save(part));
    }

    public PartResponse updatePart(Long id, PartUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeRequired(request.getName(), "name is required");
        String normalizedCode = normalizeOptional(request.getCode());
        String normalizedBarcode = normalizeOptional(request.getBarcode());
        String normalizedDescription = normalizeOptional(request.getDescription());
        String normalizedImagePath = normalizeOptional(request.getImagePath());

        Part part = partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found"));
        boolean targetActive = request.getActive() != null ? request.getActive() : part.getActive();

        validateNumericValues(
                request.getPurchasePrice(),
                request.getSalePrice(),
                request.getStock(),
                request.getStockEntry(),
                request.getStockExit()
        );

        if (targetActive && StringUtils.hasText(normalizedCode)
                && partRepository.existsByCodeIgnoreCaseAndIdNotAndActiveTrue(normalizedCode, id)) {
            throw new RuntimeException("Part code already exists");
        }
        if (targetActive && StringUtils.hasText(normalizedBarcode)
                && partRepository.existsByBarcodeIgnoreCaseAndIdNotAndActiveTrue(normalizedBarcode, id)) {
            throw new RuntimeException("Part barcode already exists");
        }

        StockGroup stockGroup = stockGroupRepository.findByIdAndActiveTrue(request.getStockGroupId())
                .orElseThrow(() -> new RuntimeException("Stock group not found"));
        StockUnit unit = stockUnitRepository.findByIdAndActiveTrue(request.getUnitId())
                .orElseThrow(() -> new RuntimeException("Stock unit not found"));
        VatRate vatRate = resolveVatRate(request.getVatRateId());
        Brand brand = resolveBrand(request.getBrandId());
        StockModel model = resolveModel(request.getModelId());
        validateBrandModelRelation(brand, model);
        StockSnapshot stockSnapshot = resolveStockSnapshotForUpdate(request, part);

        part.setName(normalizedName);
        part.setCode(normalizedCode);
        part.setBarcode(normalizedBarcode);
        part.setVatRate(vatRate);
        part.setStockGroup(stockGroup);
        part.setUnit(unit);
        part.setBrand(brand != null ? brand : (model != null ? model.getBrand() : null));
        part.setModel(model);
        part.setPurchasePrice(request.getPurchasePrice());
        part.setUnitPrice(request.getSalePrice());
        part.setDescription(normalizedDescription);
        part.setImagePath(normalizedImagePath);
        part.setStock(stockSnapshot.stock());
        part.setStockEntry(stockSnapshot.stockEntry());
        part.setStockExit(stockSnapshot.stockExit());
        part.setActive(targetActive);
        part.setUpdatedBy(resolveCurrentUsername());

        return PartResponse.fromEntity(partRepository.save(part));
    }

    public void deletePart(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found"));
        part.setActive(false);
        part.setUpdatedBy(resolveCurrentUsername());
        partRepository.save(part);
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(String query) {
        return partRepository.lookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(part -> new LookupDto(part.getId(), part.getName()))
                .toList();
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findByIdAndActiveTrue(brandId)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
    }

    private StockModel resolveModel(Long modelId) {
        if (modelId == null) {
            return null;
        }
        return stockModelRepository.findByIdAndActiveTrue(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found"));
    }

    private VatRate resolveVatRate(Long vatRateId) {
        if (vatRateId == null) {
            return null;
        }
        return vatRateRepository.findByIdAndActiveTrue(vatRateId)
                .orElseThrow(() -> new RuntimeException("VAT rate not found"));
    }

    private void validateBrandModelRelation(Brand brand, StockModel model) {
        if (model == null) {
            return;
        }
        if (brand != null && !model.getBrand().getId().equals(brand.getId())) {
            throw new RuntimeException("Selected model does not belong to selected brand");
        }
    }

    private void validateCreateRequest(PartCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (request.getStockGroupId() == null) {
            throw new RuntimeException("stockGroupId is required");
        }
        if (request.getUnitId() == null) {
            throw new RuntimeException("unitId is required");
        }
        if (request.getVatRateId() == null) {
            throw new RuntimeException("vatRateId is required");
        }
        if (request.getSalePrice() == null) {
            throw new RuntimeException("salePrice is required");
        }
    }

    private void validateUpdateRequest(PartUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (request.getStockGroupId() == null) {
            throw new RuntimeException("stockGroupId is required");
        }
        if (request.getUnitId() == null) {
            throw new RuntimeException("unitId is required");
        }
        if (request.getVatRateId() == null) {
            throw new RuntimeException("vatRateId is required");
        }
        if (request.getSalePrice() == null) {
            throw new RuntimeException("salePrice is required");
        }
    }

    private void validateNumericValues(Double purchasePrice, Double salePrice, Integer stock, Integer stockEntry, Integer stockExit) {
        if (purchasePrice != null && purchasePrice < 0) {
            throw new RuntimeException("purchasePrice must be greater than or equal to 0");
        }
        if (salePrice != null && salePrice < 0) {
            throw new RuntimeException("salePrice must be greater than or equal to 0");
        }
        if (stock != null && stock < 0) {
            throw new RuntimeException("stock must be greater than or equal to 0");
        }
        if (stockEntry != null && stockEntry < 0) {
            throw new RuntimeException("stockEntry must be greater than or equal to 0");
        }
        if (stockExit != null && stockExit < 0) {
            throw new RuntimeException("stockExit must be greater than or equal to 0");
        }
    }

    private StockSnapshot resolveStockSnapshotForCreate(PartCreateRequest request) {
        if (request.getStockEntry() != null || request.getStockExit() != null) {
            return buildSnapshotFromStockEntryExit(request.getStockEntry(), request.getStockExit());
        }
        int stock = request.getStock() != null ? request.getStock() : 0;
        return new StockSnapshot(stock, stock, 0);
    }

    private StockSnapshot resolveStockSnapshotForUpdate(PartUpdateRequest request, Part part) {
        if (request.getStockEntry() != null || request.getStockExit() != null) {
            return buildSnapshotFromStockEntryExit(request.getStockEntry(), request.getStockExit());
        }
        if (request.getStock() != null) {
            int stock = request.getStock();
            return new StockSnapshot(stock, stock, 0);
        }

        int existingStock = part.getStock() != null ? part.getStock() : 0;
        int existingStockEntry = part.getStockEntry() != null ? part.getStockEntry() : existingStock;
        int existingStockExit = part.getStockExit() != null ? part.getStockExit() : 0;
        return new StockSnapshot(existingStock, existingStockEntry, existingStockExit);
    }

    private StockSnapshot buildSnapshotFromStockEntryExit(Integer stockEntry, Integer stockExit) {
        int normalizedStockEntry = stockEntry != null ? stockEntry : 0;
        int normalizedStockExit = stockExit != null ? stockExit : 0;
        int stock = normalizedStockEntry - normalizedStockExit;
        if (stock < 0) {
            throw new RuntimeException("stock cannot be negative");
        }
        return new StockSnapshot(stock, normalizedStockEntry, normalizedStockExit);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
            return principalName;
        }
        return null;
    }

    private record StockSnapshot(int stock, int stockEntry, int stockExit) {
    }
}
