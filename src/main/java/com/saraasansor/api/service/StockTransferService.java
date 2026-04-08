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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class StockTransferService {

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private final StockTransferRepository stockTransferRepository;
    private final PartRepository partRepository;
    private final WarehouseRepository warehouseRepository;

    public StockTransferService(StockTransferRepository stockTransferRepository,
                                PartRepository partRepository,
                                WarehouseRepository warehouseRepository) {
        this.stockTransferRepository = stockTransferRepository;
        this.partRepository = partRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public StockTransferPageResponse getStockTransfers(String query, Boolean active, Pageable pageable) {
        Page<StockTransfer> page = stockTransferRepository.search(normalizeNullable(query), active, pageable);
        StockTransferPageResponse response = new StockTransferPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.StockTransferListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getStockTransferById(Long id) {
        StockTransfer stockTransfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock transfer not found"));
        return StockTransferResponse.fromEntity(stockTransfer);
    }

    public StockTransferResponse createStockTransfer(StockTransferCreateRequest request) {
        validateCreateRequest(request);
        String normalizedDescription = normalizeNullable(request.getDescription());
        validateDescriptionLength(normalizedDescription);
        boolean targetActive = request.getActive() == null || request.getActive();

        Part stock = resolveStock(request.getStockId());
        Warehouse outgoingWarehouse = resolveWarehouse(request.getOutgoingWarehouseId(), "Outgoing warehouse not found");
        Warehouse incomingWarehouse = resolveWarehouse(request.getIncomingWarehouseId(), "Incoming warehouse not found");
        validateWarehouses(outgoingWarehouse, incomingWarehouse);
        validateTransferQuantity(stock, request.getQuantity());

        StockTransfer stockTransfer = new StockTransfer();
        stockTransfer.setDate(request.getDate());
        stockTransfer.setStock(stock);
        stockTransfer.setOutgoingWarehouse(outgoingWarehouse);
        stockTransfer.setIncomingWarehouse(incomingWarehouse);
        stockTransfer.setQuantity(request.getQuantity());
        stockTransfer.setDescription(normalizedDescription);
        stockTransfer.setActive(targetActive);
        stockTransfer.setCreatedBy(resolveCurrentUsername());
        stockTransfer.setUpdatedBy(resolveCurrentUsername());

        return StockTransferResponse.fromEntity(stockTransferRepository.save(stockTransfer));
    }

    public StockTransferResponse updateStockTransfer(Long id, StockTransferUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedDescription = normalizeNullable(request.getDescription());
        validateDescriptionLength(normalizedDescription);

        StockTransfer stockTransfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock transfer not found"));
        boolean targetActive = request.getActive() != null ? request.getActive() : stockTransfer.getActive();

        Part stock = resolveStock(request.getStockId());
        Warehouse outgoingWarehouse = resolveWarehouse(request.getOutgoingWarehouseId(), "Outgoing warehouse not found");
        Warehouse incomingWarehouse = resolveWarehouse(request.getIncomingWarehouseId(), "Incoming warehouse not found");
        validateWarehouses(outgoingWarehouse, incomingWarehouse);
        validateTransferQuantity(stock, request.getQuantity());

        stockTransfer.setDate(request.getDate());
        stockTransfer.setStock(stock);
        stockTransfer.setOutgoingWarehouse(outgoingWarehouse);
        stockTransfer.setIncomingWarehouse(incomingWarehouse);
        stockTransfer.setQuantity(request.getQuantity());
        stockTransfer.setDescription(normalizedDescription);
        stockTransfer.setActive(targetActive);
        stockTransfer.setUpdatedBy(resolveCurrentUsername());

        return StockTransferResponse.fromEntity(stockTransferRepository.save(stockTransfer));
    }

    public void deleteStockTransfer(Long id) {
        StockTransfer stockTransfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock transfer not found"));
        stockTransfer.setActive(false);
        stockTransfer.setUpdatedBy(resolveCurrentUsername());
        stockTransferRepository.save(stockTransfer);
    }

    private Part resolveStock(Long stockId) {
        return partRepository.findByIdAndActiveTrue(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));
    }

    private Warehouse resolveWarehouse(Long id, String notFoundMessage) {
        return warehouseRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException(notFoundMessage));
    }

    private void validateCreateRequest(StockTransferCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        validateCommonRequest(request.getDate(),
                request.getStockId(),
                request.getOutgoingWarehouseId(),
                request.getIncomingWarehouseId(),
                request.getQuantity());
    }

    private void validateUpdateRequest(StockTransferUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        validateCommonRequest(request.getDate(),
                request.getStockId(),
                request.getOutgoingWarehouseId(),
                request.getIncomingWarehouseId(),
                request.getQuantity());
    }

    private void validateCommonRequest(java.time.LocalDate date,
                                       Long stockId,
                                       Long outgoingWarehouseId,
                                       Long incomingWarehouseId,
                                       Integer quantity) {
        if (date == null) {
            throw new RuntimeException("date is required");
        }
        if (stockId == null) {
            throw new RuntimeException("stockId is required");
        }
        if (outgoingWarehouseId == null) {
            throw new RuntimeException("outgoingWarehouseId is required");
        }
        if (incomingWarehouseId == null) {
            throw new RuntimeException("incomingWarehouseId is required");
        }
        if (quantity == null) {
            throw new RuntimeException("quantity is required");
        }
        if (quantity <= 0) {
            throw new RuntimeException("quantity must be greater than zero");
        }
    }

    private void validateDescriptionLength(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RuntimeException("description must be at most 5000 characters");
        }
    }

    private void validateWarehouses(Warehouse outgoingWarehouse, Warehouse incomingWarehouse) {
        if (outgoingWarehouse.getId().equals(incomingWarehouse.getId())) {
            throw new RuntimeException("Outgoing and incoming warehouse cannot be the same");
        }
    }

    private void validateTransferQuantity(Part stock, Integer quantity) {
        Integer currentStock = stock.getStock();
        if (currentStock != null && quantity > currentStock) {
            throw new RuntimeException("Transfer quantity exceeds available stock");
        }
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
}
