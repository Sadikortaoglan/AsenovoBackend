package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockItemDto;
import com.saraasansor.api.dto.StockTransferDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.StockItem;
import com.saraasansor.api.model.StockTransfer;
import com.saraasansor.api.repository.StockItemRepository;
import com.saraasansor.api.repository.StockTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class StockService {
    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    public Page<StockItemDto> listStocks(String q, Pageable pageable) {
        String query = q == null ? "" : q;
        return stockItemRepository.findByProductNameContainingIgnoreCase(query, pageable).map(StockItemDto::fromEntity);
    }

    public StockItemDto createStock(StockItemDto dto) {
        StockItem entity = new StockItem();
        map(entity, dto);
        return StockItemDto.fromEntity(stockItemRepository.save(entity));
    }

    public StockItemDto updateStock(Long id, StockItemDto dto) {
        StockItem entity = stockItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock item not found"));
        map(entity, dto);
        return StockItemDto.fromEntity(stockItemRepository.save(entity));
    }

    public void deleteStock(Long id) {
        if (!stockItemRepository.existsById(id)) {
            throw new NotFoundException("Stock item not found");
        }
        stockItemRepository.deleteById(id);
    }

    public Page<StockTransferDto> listTransfers(Pageable pageable) {
        return stockTransferRepository.findAllByOrderByTransferDateDesc(pageable).map(StockTransferDto::fromEntity);
    }

    public StockTransferDto transfer(StockTransferDto dto) {
        StockItem from = stockItemRepository.findById(dto.getFromStockId())
                .orElseThrow(() -> new NotFoundException("From stock not found"));
        StockItem to = stockItemRepository.findById(dto.getToStockId())
                .orElseThrow(() -> new NotFoundException("To stock not found"));

        if (from.getCurrentStock().compareTo(dto.getQuantity()) < 0) {
            throw new RuntimeException("Insufficient stock for transfer");
        }

        from.setStockOut(from.getStockOut().add(dto.getQuantity()));
        from.recalculateCurrentStock();
        to.setStockIn(to.getStockIn().add(dto.getQuantity()));
        to.recalculateCurrentStock();

        stockItemRepository.save(from);
        stockItemRepository.save(to);

        StockTransfer transfer = new StockTransfer();
        transfer.setFromStock(from);
        transfer.setToStock(to);
        transfer.setQuantity(dto.getQuantity());
        transfer.setTransferDate(dto.getTransferDate());
        transfer.setNote(dto.getNote());

        return StockTransferDto.fromEntity(stockTransferRepository.save(transfer));
    }

    public java.util.List<String> listModels() {
        return stockItemRepository.findDistinctModelNames();
    }

    public java.util.List<BigDecimal> listVatRates() {
        return stockItemRepository.findDistinctVatRates();
    }

    private void map(StockItem entity, StockItemDto dto) {
        entity.setProductName(dto.getProductName());
        entity.setStockGroup(dto.getStockGroup());
        entity.setModelName(dto.getModelName());
        entity.setUnit(dto.getUnit());
        entity.setVatRate(dto.getVatRate());
        entity.setPurchasePrice(dto.getPurchasePrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setStockIn(dto.getStockIn());
        entity.setStockOut(dto.getStockOut());
        entity.recalculateCurrentStock();
    }
}
