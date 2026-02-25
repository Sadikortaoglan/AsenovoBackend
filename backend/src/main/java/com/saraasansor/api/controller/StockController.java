package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.StockItemDto;
import com.saraasansor.api.dto.StockTransferDto;
import com.saraasansor.api.service.StockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {
    @Autowired
    private StockService stockService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockItemDto>>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(stockService.listStocks(q, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockItemDto>> create(@Valid @RequestBody StockItemDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Stock created", stockService.createStock(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockItemDto>> update(@PathVariable Long id, @Valid @RequestBody StockItemDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated", stockService.updateStock(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.ok(ApiResponse.success("Stock deleted", null));
    }

    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<Page<StockTransferDto>>> listTransfers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(stockService.listTransfers(pageable)));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<StockTransferDto>> transfer(@Valid @RequestBody StockTransferDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Stock transfer completed", stockService.transfer(dto)));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<String>>> listModels() {
        return ResponseEntity.ok(ApiResponse.success(stockService.listModels()));
    }

    @GetMapping("/vat-rates")
    public ResponseEntity<ApiResponse<List<BigDecimal>>> listVatRates() {
        return ResponseEntity.ok(ApiResponse.success(stockService.listVatRates()));
    }
}
